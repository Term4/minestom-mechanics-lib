package com.minestom.mechanics.systems.compatibility;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Also move this to player package

/**
 * Detects client version for version-aware behavior (e.g. legacy 1.8 vs modern).
 * Version is determined only from the Velocity protocol forwarder – plugin message on
 * {@value #PROTOCOL_VERSION_CHANNEL} (UUID + VarInt protocol version). When no protocol
 * version has been received (e.g. not behind proxy or message not yet sent),
 * {@link #getClientVersion(Player)} returns {@link ClientVersion#UNKNOWN}.
 */
public class ClientVersionDetector {

    /** Plugin message channel used by Velocity protocol forwarder (mechanics:protocol_version). */
    public static final String PROTOCOL_VERSION_CHANNEL = "mechanics:protocol_version";

    /** Protocol version below this is treated as legacy (1.8.x = 47; 1.9 = 107). */
    private static final int LEGACY_PROTOCOL_THRESHOLD = 107;

    /** Protocol number → game version string for logging (supported range: 1.7.10, 1.8.x, 1.21.x). */
    private static final Map<Integer, String> PROTOCOL_TO_GAME_VERSION = new TreeMap<>();

    static {
        PROTOCOL_TO_GAME_VERSION.put(5, "1.7.10");
        PROTOCOL_TO_GAME_VERSION.put(47, "1.8.x");
        PROTOCOL_TO_GAME_VERSION.put(767, "1.21");
        PROTOCOL_TO_GAME_VERSION.put(768, "1.21.1");
        PROTOCOL_TO_GAME_VERSION.put(769, "1.21.2");
        PROTOCOL_TO_GAME_VERSION.put(770, "1.21.3");
    }

    private static ClientVersionDetector instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("ClientVersionDetector");

    private final Map<UUID, Integer> proxyProtocolVersions = new ConcurrentHashMap<>();

    private ClientVersionDetector() {}

    public static ClientVersionDetector getInstance() {
        if (instance == null) {
            instance = new ClientVersionDetector();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(PlayerPluginMessageEvent.class, event -> {
            if (event.getIdentifier().equals(PROTOCOL_VERSION_CHANNEL)) {
                handleProtocolVersionMessage(event.getPlayer(), event.getMessage());
            }
        });

        handler.addListener(PlayerDisconnectEvent.class, event ->
                proxyProtocolVersions.remove(event.getPlayer().getUuid()));

        log.debug("Client version detector initialized");
    }

    /**
     * Handle protocol version from Velocity protocol forwarder.
     * Payload: 16 bytes UUID (MSB first) + VarInt protocol version (e.g. 47 = 1.8.x, 107 = 1.9+).
     * Only the proxy should send this; accept only when UUID matches the player (consistency check).
     */
    private void handleProtocolVersionMessage(Player player, byte[] data) {
        if (data == null || data.length < 17) { // 16 UUID + at least 1 byte VarInt
            log.debug("Invalid protocol version message from {}: payload too short", player.getUsername());
            return;
        }
        try {
            long msb = ByteBuffer.wrap(data, 0, 8).getLong();
            long lsb = ByteBuffer.wrap(data, 8, 8).getLong();
            UUID payloadUuid = new UUID(msb, lsb);
            if (!payloadUuid.equals(player.getUuid())) {
                log.warn("Protocol version message UUID mismatch for {} – ignoring (possible spoof)", player.getUsername());
                log.warn("Payload ID: " +  payloadUuid + " | Player ID: " +  player.getUuid());
                return;
            }
            int[] result = readVarInt(data, 16);
            int protocolVersion = result[0];
            proxyProtocolVersions.put(player.getUuid(), protocolVersion);
            ClientVersion version = protocolVersion < LEGACY_PROTOCOL_THRESHOLD ? ClientVersion.LEGACY : ClientVersion.MODERN;
            String versionLabel = protocolVersionToGameVersion(protocolVersion);
            log.info("{} protocol version from proxy: {} ({}) – {}", player.getUsername(), protocolVersion, version, versionLabel);
        } catch (Exception e) {
            log.warn("Failed to parse protocol version message from {}: {}", player.getUsername(), e.getMessage());
        }
    }

    /** Returns human-readable game version for a protocol number (e.g. 47 → "1.8.x"), or "protocol N" if unknown. */
    private static String protocolVersionToGameVersion(int protocolVersion) {
        String known = PROTOCOL_TO_GAME_VERSION.get(protocolVersion);
        return known != null ? known : "protocol " + protocolVersion;
    }

    /** Reads a VarInt from {@code data} starting at {@code offset}. Returns [value, nextOffset]. */
    private static int[] readVarInt(byte[] data, int offset) {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            if (offset >= data.length) throw new IllegalArgumentException("VarInt extends past buffer");
            b = data[offset++];
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return new int[]{ value, offset };
    }

    /**
     * Get client version for a player. Only protocol version from the proxy is used.
     * Returns {@link ClientVersion#UNKNOWN} if no protocol version has been received.
     */
    public ClientVersion getClientVersion(Player player) {
        Integer protocol = proxyProtocolVersions.get(player.getUuid());
        if (protocol == null) {
            return ClientVersion.UNKNOWN;
        }
        return protocol < LEGACY_PROTOCOL_THRESHOLD ? ClientVersion.LEGACY : ClientVersion.MODERN;
    }

    /**
     * Raw protocol version when provided by the proxy (Velocity protocol forwarder).
     * e.g. 47 = 1.8.x, 107 = 1.9. Returns null if not behind proxy or not yet received.
     */
    public Integer getProtocolVersion(Player player) {
        return proxyProtocolVersions.get(player.getUuid());
    }

    /**
     * Whether the player needs frequent animation updates (legacy clients only).
     * False when version is unknown (no protocol from proxy).
     */
    public boolean needsFrequentUpdates(Player player) {
        return getClientVersion(player) == ClientVersion.LEGACY;
    }

    public enum ClientVersion {
        LEGACY,   // protocol < 107 (e.g. 1.7.10, 1.8.x)
        MODERN,   // protocol >= 107 (e.g. 1.21.x)
        UNKNOWN   // No protocol version received from proxy
    }

    /**
     * Statistics for players with a known protocol version from the proxy.
     */
    public String getStatistics() {
        int total = proxyProtocolVersions.size();
        long legacy = proxyProtocolVersions.values().stream()
                .filter(p -> p < LEGACY_PROTOCOL_THRESHOLD).count();
        long modern = total - legacy;
        return String.format("Clients: %d total (%d legacy, %d modern)", total, legacy, modern);
    }
}
