package com.minestom.mechanics.systems.compatibility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Also move this to player package

/**
 * Detects client version for version-aware behavior (e.g. legacy 1.8 vs modern).
 * Version is determined from ViaVersion – plugin messages on {@value #VIA_MOD_DETAILS_CHANNEL}
 * (client mods) or {@value #VIA_PROXY_DETAILS_CHANNEL} (Velocity/Bungee proxy with send-player-details).
 * Payload is JSON with {@code version} and {@code versionName}. When no protocol version has been
 * received, {@link #getClientVersion(Player)} returns {@link ClientVersion#UNKNOWN}.
 *
 * @see <a href="https://github.com/ViaVersion/ViaVersion/wiki/Server-and-Player-Details-Protocol">ViaVersion Server and Player Details Protocol</a>
 */
public class ClientVersionDetector {

    /** Plugin message channel from client mods (ViaFabric, ViaFabricPlus, ViaForge). */
    public static final String VIA_MOD_DETAILS_CHANNEL = "vv:mod_details";

    /** Plugin message channel from proxies (Velocity, BungeeCord) when send-player-details is enabled. */
    public static final String VIA_PROXY_DETAILS_CHANNEL = "vv:proxy_details";

    private static final Gson GSON = new Gson();

    /** Protocol version below this is treated as legacy (1.8.x = 47; 1.9 = 107). */
    private static final int LEGACY_PROTOCOL_THRESHOLD = 107;

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
            String channel = event.getIdentifier();
            if (channel.equals(VIA_MOD_DETAILS_CHANNEL) || channel.equals(VIA_PROXY_DETAILS_CHANNEL)) {
                handleViaVersionMessage(event.getPlayer(), event.getMessage());
            }
        });

        handler.addListener(PlayerDisconnectEvent.class, event ->
                proxyProtocolVersions.remove(event.getPlayer().getUuid()));

        log.debug("Client version detector initialized");
    }

    /**
     * Handle protocol version from ViaVersion (vv:mod_details or vv:proxy_details).
     * Payload: JSON with {@code version} (int) and {@code versionName} (string).
     */
    private void handleViaVersionMessage(Player player, byte[] data) {
        if (data == null || data.length == 0) {
            log.debug("Invalid ViaVersion message from {}: empty payload", player.getUsername());
            return;
        }
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            JsonObject payload = GSON.fromJson(json, JsonObject.class);
            if (payload == null || !payload.has("version")) {
                log.debug("Invalid ViaVersion message from {}: missing version", player.getUsername());
                return;
            }
            int protocolVersion = payload.get("version").getAsInt();
            String versionName = payload.has("versionName") ? sanitizeVersionName(payload.get("versionName").getAsString()) : null;

            proxyProtocolVersions.put(player.getUuid(), protocolVersion);
            ClientVersion version = protocolVersion < LEGACY_PROTOCOL_THRESHOLD ? ClientVersion.LEGACY : ClientVersion.MODERN;
            String label = versionName != null ? versionName : ("protocol " + protocolVersion);
            log.debug("{} | client version: {} (protocol {}, {})", player.getUsername(), label, protocolVersion, version);
        } catch (Exception e) {
            log.warn("Failed to parse ViaVersion message from {}: {}", player.getUsername(), e.getMessage());
        }
    }

    /** Trim and strip trailing non-alphanumeric/dot characters from version name for display (e.g. "1.8.x]" -> "1.8.x"). */
    private static String sanitizeVersionName(String versionName) {
        if (versionName == null) return null;
        String s = versionName.trim();
        int end = s.length();
        while (end > 0 && !Character.isLetterOrDigit(s.charAt(end - 1)) && s.charAt(end - 1) != '.') {
            end--;
        }
        return end == s.length() ? s : s.substring(0, end);
    }

    /**
     * Get client version for a player. Only protocol version from ViaVersion is used.
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
     * Raw protocol version when provided by ViaVersion.
     * e.g. 47 = 1.8.x, 107 = 1.9. Returns null if ViaVersion has not sent details yet.
     */
    public Integer getProtocolVersion(Player player) {
        return proxyProtocolVersions.get(player.getUuid());
    }

    /**
     * Whether the player needs frequent animation updates (legacy clients only).
     * False when version is unknown (no protocol from ViaVersion).
     */
    public boolean needsFrequentUpdates(Player player) {
        return getClientVersion(player) == ClientVersion.LEGACY;
    }

    public enum ClientVersion {
        LEGACY,   // protocol < 107 (e.g. 1.7.10, 1.8.x)
        MODERN,   // protocol >= 107 (e.g. 1.21.x)
        UNKNOWN   // No protocol version received from ViaVersion
    }

    /**
     * Statistics for players with a known protocol version from ViaVersion.
     */
    public String getStatistics() {
        int total = proxyProtocolVersions.size();
        long legacy = proxyProtocolVersions.values().stream()
                .filter(p -> p < LEGACY_PROTOCOL_THRESHOLD).count();
        long modern = total - legacy;
        return String.format("Clients: %d total (%d legacy, %d modern)", total, legacy, modern);
    }
}
