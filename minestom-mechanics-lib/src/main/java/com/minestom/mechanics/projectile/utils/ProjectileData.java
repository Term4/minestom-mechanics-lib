package com.minestom.mechanics.projectile.utils;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Holds all configuration data for a projectile type.
 * Extensible - add new properties as needed.
 */
public class ProjectileData {

    private final KnockbackConfig knockbackConfig;
    private final ProjectileVelocityConfig velocityConfig;
    // Future: DamageConfig, ParticleConfig, SoundConfig, etc.

    public ProjectileData(
            @Nullable KnockbackConfig knockbackConfig,
            @Nullable ProjectileVelocityConfig velocityConfig
    ) {
        this.knockbackConfig = knockbackConfig;
        this.velocityConfig = velocityConfig;
    }

    /**
     * Get knockback config for this projectile.
     * Returns null if not configured (use system defaults).
     */
    @Nullable
    public KnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }

    /**
     * Get velocity config for this projectile.
     * Returns null if not configured (use system defaults).
     */
    @Nullable
    public ProjectileVelocityConfig getVelocityConfig() {
        return velocityConfig;
    }

    /**
     * Check if this projectile has custom knockback.
     */
    public boolean hasKnockback() {
        return knockbackConfig != null;
    }

    /**
     * Check if this projectile has custom velocity.
     */
    public boolean hasVelocity() {
        return velocityConfig != null;
    }

    /**
     * Check if this data is empty (no configs set).
     */
    public boolean isEmpty() {
        return knockbackConfig == null && velocityConfig == null;
    }
}