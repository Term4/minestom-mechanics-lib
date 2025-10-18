package com.test.minestom.config.server;

import net.minestom.server.coordinate.Pos;

/**
 * Server configuration - all server-level settings in one place.
 * Separates server config from combat config for better organization.
 */
public class ServerConfig {

    // Network settings
    private final String serverIp;
    private final int serverPort;

    // Spawn settings
    private final Pos spawnPosition;

    // Velocity proxy settings
    private final boolean velocityEnabled;
    private final String velocitySecret;

    // World generation settings
    private final boolean flatWorld;
    private final int worldHeight;

    private ServerConfig(Builder builder) {
        this.serverIp = builder.serverIp;
        this.serverPort = builder.serverPort;
        this.spawnPosition = builder.spawnPosition;
        this.velocityEnabled = builder.velocityEnabled;
        this.velocitySecret = builder.velocitySecret;
        this.flatWorld = builder.flatWorld;
        this.worldHeight = builder.worldHeight;
    }

    // ===========================
    // PRESET CONFIGS
    // ===========================

    /**
     * Default local development server config
     */
    public static ServerConfig local() {
        return builder()
                .network("0.0.0.0", 25566)
                .spawn(new Pos(0, 42, 0))
                .velocity(false, "")
                .flatWorld(true, 42)
                .build();
    }

    /**
     * Production server with Velocity proxy
     */
    public static ServerConfig production(String velocitySecret) {
        return builder()
                .network("0.0.0.0", 25566)
                .spawn(new Pos(0, 42, 0))
                .velocity(true, velocitySecret)
                .flatWorld(true, 42)
                .build();
    }

    /**
     * Testing server (separate port to avoid conflicts)
     */
    public static ServerConfig testing() {
        return builder()
                .network("0.0.0.0", 25567)
                .spawn(new Pos(0, 42, 0))
                .velocity(false, "")
                .flatWorld(true, 42)
                .build();
    }

    // ===========================
    // BUILDER
    // ===========================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serverIp = "0.0.0.0";
        private int serverPort = 25566;
        private Pos spawnPosition = new Pos(0, 42, 0);
        private boolean velocityEnabled = false;
        private String velocitySecret = "";
        private boolean flatWorld = true;
        private int worldHeight = 42;

        public Builder network(String ip, int port) {
            this.serverIp = ip;
            this.serverPort = port;
            return this;
        }

        public Builder spawn(Pos position) {
            this.spawnPosition = position;
            return this;
        }

        public Builder velocity(boolean enabled, String secret) {
            this.velocityEnabled = enabled;
            this.velocitySecret = secret;
            return this;
        }

        public Builder flatWorld(boolean enabled, int height) {
            this.flatWorld = enabled;
            this.worldHeight = height;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }

    // ===========================
    // GETTERS
    // ===========================

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public Pos getSpawnPosition() {
        return spawnPosition;
    }

    public boolean isVelocityEnabled() {
        return velocityEnabled;
    }

    public String getVelocitySecret() {
        return velocitySecret;
    }

    public boolean isFlatWorld() {
        return flatWorld;
    }

    public int getWorldHeight() {
        return worldHeight;
    }
}