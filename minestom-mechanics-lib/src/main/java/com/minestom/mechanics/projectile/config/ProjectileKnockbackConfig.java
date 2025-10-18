package com.minestom.mechanics.projectile.config;

/**
 * Configuration for projectile knockback settings.
 * Controls how much knockback projectiles apply to entities they hit.
 * 
 * @param enabled whether knockback is enabled for this projectile type
 * @param horizontalKnockback the horizontal knockback multiplier
 * @param verticalKnockback the vertical knockback multiplier
 * @param verticalLimit the maximum vertical knockback limit
 */
public record ProjectileKnockbackConfig(boolean enabled, double horizontalKnockback, double verticalKnockback,
                                        double verticalLimit) {

    // Default configurations for different projectile types (1.8.9 vanilla values)
    public static ProjectileKnockbackConfig defaultArrowKnockback() {
        return new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45); // 1.8.9 vanilla: h=0.4, v=0.1
    }

    public static ProjectileKnockbackConfig defaultSnowballKnockback() {
        return new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45); // 1.8.9 vanilla: h=0.3, v=0.05
    }

    public static ProjectileKnockbackConfig defaultEggKnockback() {
        return new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45); // 1.8.9 vanilla: h=0.2, v=0.05
    }

    public static ProjectileKnockbackConfig defaultEnderPearlKnockback() {
        return new ProjectileKnockbackConfig(false, 0.0, 0.0, 0.0); // No knockback for ender pearls
    }

    public static ProjectileKnockbackConfig disabled() {
        return new ProjectileKnockbackConfig(false, 0.0, 0.0, 0.0);
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private double horizontalKnockback = 0.55;
        private double verticalKnockback = 0.365;
        private double verticalLimit = 0.45;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder horizontalKnockback(double knockback) {
            this.horizontalKnockback = knockback;
            return this;
        }

        public Builder verticalKnockback(double knockback) {
            this.verticalKnockback = knockback;
            return this;
        }

        public Builder verticalLimit(double limit) {
            this.verticalLimit = limit;
            return this;
        }

        public ProjectileKnockbackConfig build() {
            return new ProjectileKnockbackConfig(enabled, horizontalKnockback,
                    verticalKnockback, verticalLimit);
        }
    }
}
