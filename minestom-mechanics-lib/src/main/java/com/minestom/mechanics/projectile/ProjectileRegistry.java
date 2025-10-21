package com.minestom.mechanics.projectile;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.projectiles.ProjectileConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.manager.ProjectileData;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unified registry for all projectile data.
 *
 * Handles:
 * - Material → EntityType mapping (what to spawn)
 * - EntityType → ProjectileData mapping (entity properties)
 *
 * Usage:
 * <pre>
 * // Get entity to spawn
 * EntityType type = registry.getEntityType(Material.SNOWBALL);
 *
 * // Get entity properties
 * ProjectileData data = registry.getData(EntityType.SNOWBALL);
 * KnockbackConfig kb = data.getKnockbackConfig();
 * </pre>
 */
public class ProjectileRegistry {

    private static final LogUtil.SystemLogger log = LogUtil.system("ProjectileRegistry");

    private final Map<Material, EntityType> materialToEntity = new HashMap<>();
    private final Map<EntityType, ProjectileData> entityData = new HashMap<>();

    /**
     * Initialize registry from ProjectileConfig.
     * Registers all vanilla projectiles with their configs.
     */
    public void initialize(ProjectileConfig config) {
        log.debug("Initializing ProjectileRegistry...");

        // Arrows (multiple materials → same entity)
        registerArrows(config);

        // Throwables (1:1 mapping)
        register(
                Material.SNOWBALL,
                EntityType.SNOWBALL,
                convertKnockback(config.getSnowballKnockbackConfig()),
                config.getSnowballVelocityConfig()
        );

        register(
                Material.EGG,
                EntityType.EGG,
                convertKnockback(config.getEggKnockbackConfig()),
                config.getEggVelocityConfig()
        );

        register(
                Material.ENDER_PEARL,
                EntityType.ENDER_PEARL,
                convertKnockback(config.getEnderPearlKnockbackConfig()),
                config.getEnderPearlVelocityConfig()
        );

        // Fishing rod (special case - no material mapping, only entity data)
        registerEntityOnly(
                EntityType.FISHING_BOBBER,
                convertKnockback(config.getFishingRodKnockbackConfig()),
                config.getFishingRodVelocityConfig()
        );

        log.info("Registered {} material mappings and {} entity configs",
                materialToEntity.size(), entityData.size());
    }

    private void registerArrows(ProjectileConfig config) {
        KnockbackConfig kbConfig = convertKnockback(config.getArrowKnockbackConfig());
        ProjectileVelocityConfig velConfig = config.getArrowVelocityConfig();

        // All arrow materials → ARROW entity
        materialToEntity.put(Material.ARROW, EntityType.ARROW);
        materialToEntity.put(Material.TIPPED_ARROW, EntityType.ARROW);
        materialToEntity.put(Material.SPECTRAL_ARROW, EntityType.SPECTRAL_ARROW);

        // Register entity data for both arrow types
        ProjectileData arrowData = new ProjectileData(kbConfig, velConfig);
        entityData.put(EntityType.ARROW, arrowData);
        entityData.put(EntityType.SPECTRAL_ARROW, arrowData); // Same configs
    }

    /**
     * Register a projectile with material → entity mapping and configs.
     */
    public void register(Material material, EntityType entityType,
                         @Nullable KnockbackConfig knockback,
                         @Nullable ProjectileVelocityConfig velocity) {
        materialToEntity.put(material, entityType);

        ProjectileData data = new ProjectileData(knockback, velocity);
        if (!data.isEmpty()) {
            entityData.put(entityType, data);
        }
    }

    /**
     * Register entity data without material mapping.
     * Used for entities spawned by other means (fishing bobber, etc).
     */
    public void registerEntityOnly(EntityType entityType,
                                   @Nullable KnockbackConfig knockback,
                                   @Nullable ProjectileVelocityConfig velocity) {
        ProjectileData data = new ProjectileData(knockback, velocity);
        if (!data.isEmpty()) {
            entityData.put(entityType, data);
        }
    }

    // ===========================
    // LOOKUPS
    // ===========================

    /**
     * Get entity type to spawn for a material.
     * Returns null if not registered (not a projectile).
     */
    @Nullable
    public EntityType getEntityType(Material material) {
        return materialToEntity.get(material);
    }

    /**
     * Get projectile data for an entity type.
     * Returns null if not registered (use system defaults).
     */
    @Nullable
    public ProjectileData getData(EntityType entityType) {
        return entityData.get(entityType);
    }

    /**
     * Get knockback config for an entity type.
     * Returns null if not configured.
     */
    @Nullable
    public KnockbackConfig getKnockbackConfig(EntityType entityType) {
        ProjectileData data = entityData.get(entityType);
        return data != null ? data.getKnockbackConfig() : null;
    }

    /**
     * Get velocity config for an entity type.
     * Returns null if not configured.
     */
    @Nullable
    public ProjectileVelocityConfig getVelocityConfig(EntityType entityType) {
        ProjectileData data = entityData.get(entityType);
        return data != null ? data.getVelocityConfig() : null;
    }

    /**
     * Check if material is registered as a projectile.
     */
    public boolean hasEntityType(Material material) {
        return materialToEntity.containsKey(material);
    }

    /**
     * Check if entity type has registered data.
     */
    public boolean hasData(EntityType entityType) {
        return entityData.containsKey(entityType);
    }

    /**
     * Get all registered materials.
     */
    public Set<Material> getRegisteredMaterials() {
        return materialToEntity.keySet();
    }

    /**
     * Get all registered entity types.
     */
    public Set<EntityType> getRegisteredEntityTypes() {
        return entityData.keySet();
    }

    // ===========================
    // HELPERS
    // ===========================

    /**
     * Convert ProjectileKnockbackConfig to full KnockbackConfig.
     */
    @Nullable
    private KnockbackConfig convertKnockback(@Nullable ProjectileKnockbackConfig pkbConfig) {
        if (pkbConfig == null || !pkbConfig.enabled()) {
            return null;
        }

        return KnockbackConfig.validated(
                pkbConfig.horizontalKnockback(),
                pkbConfig.verticalKnockback(),
                pkbConfig.verticalLimit(),
                0.0, 0.0, 1.0, 1.0, 0.0, false, false
        );
    }
}