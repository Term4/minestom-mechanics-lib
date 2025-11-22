package com.minestom.mechanics.systems.projectile.features;

import com.minestom.mechanics.systems.projectile.entities.CustomEntityProjectile;
import com.minestom.mechanics.systems.projectile.entities.Snowball;
import com.minestom.mechanics.systems.projectile.entities.ThrownEgg;
import com.minestom.mechanics.systems.projectile.entities.ThrownEnderpearl;
import com.minestom.mechanics.systems.projectile.utils.ProjectileData;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.systems.projectile.components.ProjectileCreator;
import com.minestom.mechanics.systems.projectile.utils.ProjectileMaterials;
import com.minestom.mechanics.systems.projectile.utils.ProjectileRegistry;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

// TODO: Find a way to make them invisible with a tag
//  ALSO add lookweight like we have in kbsystem, ALSO FIX THE DAMN LOOK DIRECTION!!!

/**
 * Misc projectile feature using unified ProjectileCreator and registry.
 */
public class MiscProjectileFeature extends InitializableSystem implements ProjectileFeature {

    private static MiscProjectileFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("MiscProjectileFeature");

    // Unified components
    private final ProjectileCreator creator;

    private MiscProjectileFeature() {
        this.creator = new ProjectileCreator();
    }

    public static MiscProjectileFeature initialize() {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("MiscProjectileFeature");
            return instance;
        }

        instance = new MiscProjectileFeature();
        instance.registerListeners();
        instance.markInitialized();

        LogUtil.logInit("MiscProjectileFeature");
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(PlayerUseItemEvent.class, event -> {
            Material material = event.getItemStack().material();

            // Use ProjectileMaterials for validation
            if (!ProjectileMaterials.isThrowable(material)) {
                return;
            }

            handleThrowProjectile(event.getPlayer(), event.getItemStack(), event.getHand());
        });
    }

    private void handleThrowProjectile(Player player, ItemStack stack, PlayerHand hand) {
        Material material = stack.material();

        // 1. Get entity type and data from registry
        EntityType entityType = getRegistry().getEntityType(material);
        if (entityType == null) {
            log.warn("No entity type registered for: {}", material);
            return;
        }

        ProjectileData data = getRegistry().getData(entityType);
        if (data == null) {
            log.warn("No projectile data for: {}", entityType);
            return;
        }

        // 2. Create entity
        CustomEntityProjectile projectile = createEntity(entityType, player);

        // 3. Configure from ProjectileData
        configureFromData(projectile, data);

        // 4. Spawn using unified creator
        ProjectileVelocityConfig velocityConfig = data.getVelocityConfig();
        creator.spawn(projectile, player, stack, velocityConfig);

        // 5. Consume item
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setItemInHand(hand, stack.withAmount(stack.amount() - 1));
        }

        log.debug("Spawned {} for {}", material, player.getUsername());
    }

    private CustomEntityProjectile createEntity(EntityType type, Player player) {
        // EntityType constants work here
        if (type == EntityType.SNOWBALL) {
            return new Snowball(player);
        } else if (type == EntityType.EGG) {
            return new ThrownEgg(player);
        } else if (type == EntityType.ENDER_PEARL) {
            return new ThrownEnderpearl(player);
        }
        throw new IllegalArgumentException("Unsupported: " + type);
    }

    private void configureFromData(CustomEntityProjectile projectile, ProjectileData data) {
        // Just enable knockback handler if the projectile has knockback enabled
        if (data.hasKnockback()) {
            if (projectile instanceof Snowball s) {
                s.setUseKnockbackHandler(true);
            } else if (projectile instanceof ThrownEgg e) {
                e.setUseKnockbackHandler(true);
            }
        }
    }

    private ProjectileRegistry getRegistry() {
        return com.minestom.mechanics.manager.ProjectileManager.getInstance()
                .getProjectileRegistry();
    }

    public static MiscProjectileFeature getInstance() {
        return requireInstance(instance, "MiscProjectileFeature");
    }
}