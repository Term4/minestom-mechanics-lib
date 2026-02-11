package com.minestom.mechanics.systems.armor;

import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

// TODO: Rewrite to use tags / components, also leave a placeholder for enchants

// All this should basically do is take a protection value / calculation and pass it to the damage system with the damage.
// i.e. item has property armor = 4, item has 2 armor bars (4/2), + enchants (protection, feather falling, etc)
// all of this gets sent to the damage system which accounts for various protection.
// Should be accessed from damage calculator

/**
 * Armor system that reduces damage based on armor points and toughness.
 * Integrates with DamageSystem via EntityDamageEvent.
 */
public class ArmorSystem extends InitializableSystem {
    private static ArmorSystem instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("ArmorSystem");

    private ArmorSystem() {}

    public static ArmorSystem initialize() {
        if (instance != null) {
            LogUtil.logAlreadyInitialized("ArmorSystem");
            return instance;
        }

        instance = new ArmorSystem();
        instance.setup();
        instance.markInitialized();

        LogUtil.logInit("ArmorSystem");
        return instance;
    }

    private void setup() {
        var handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(EntityDamageEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;

            float originalDamage = event.getDamage().getAmount();
            float reducedDamage = calculateDamageAfterArmor(player, originalDamage,
                    event.getDamage().getType());

            if (reducedDamage != originalDamage) {
                event.getDamage().setAmount(reducedDamage);

                float blocked = originalDamage - reducedDamage;
                log.debug("{} blocked {:.2f} damage (from {:.2f} to {:.2f})",
                        player.getUsername(),
                        blocked,
                        originalDamage,
                        reducedDamage);
            }
        });
    }

    /**
     * Calculate damage after armor reduction.
     * Uses vanilla Minecraft formula with constants from CombatConstants.
     */
    public float calculateDamageAfterArmor(Player player, float damage,
                                           RegistryKey<DamageType> damageType) {

        if (bypassesArmor(damageType)) {
            return damage;
        }

        double armor = player.getAttributeValue(Attribute.ARMOR);
        double toughness = player.getAttributeValue(Attribute.ARMOR_TOUGHNESS);

        if (armor <= 0) {
            return damage;
        }

        // Vanilla formula with constants
        double effectiveArmor = Math.max(
                armor / ARMOR_EFFECTIVENESS_DIVISOR,
                armor - damage / (ARMOR_BASE_DIVISOR + toughness / TOUGHNESS_DIVISOR)
        );
        effectiveArmor = Math.min(MAX_EFFECTIVE_ARMOR, effectiveArmor);

        float reduction = (float) (effectiveArmor / ARMOR_REDUCTION_DIVISOR);
        float reducedDamage = damage * (1.0f - reduction);

        return Math.max(reducedDamage, 0);
    }

    private boolean bypassesArmor(RegistryKey<DamageType> damageType) {
        return damageType.equals(DamageType.OUT_OF_WORLD) ||
                damageType.equals(DamageType.STARVE) ||
                damageType.equals(DamageType.MAGIC) ||
                damageType.equals(DamageType.WITHER) ||
                damageType.equals(DamageType.SONIC_BOOM);
    }

    /**
     * Get total armor points from equipped armor.
     */
    public int getArmorPoints(Player player) {
        int total = 0;

        total += getArmorValue(player.getEquipment(EquipmentSlot.HELMET));
        total += getArmorValue(player.getEquipment(EquipmentSlot.CHESTPLATE));
        total += getArmorValue(player.getEquipment(EquipmentSlot.LEGGINGS));
        total += getArmorValue(player.getEquipment(EquipmentSlot.BOOTS));

        return total;
    }

    /**
     * Get armor value for a single item.
     */
    private int getArmorValue(ItemStack item) {
        if (item.isAir()) return 0;

        // TODO: This way of doing things seems extremely inefficient, find a better way
        Material material = item.material();

        // Check material using Material enum (can't static import Materials class)
        if (material == Material.LEATHER_HELMET) return ARMOR_LEATHER_HELMET;
        if (material == Material.LEATHER_CHESTPLATE) return ARMOR_LEATHER_CHESTPLATE;
        if (material == Material.LEATHER_LEGGINGS) return ARMOR_LEATHER_LEGGINGS;
        if (material == Material.LEATHER_BOOTS) return ARMOR_LEATHER_BOOTS;

        if (material == Material.CHAINMAIL_HELMET) return ARMOR_CHAINMAIL_HELMET;
        if (material == Material.CHAINMAIL_CHESTPLATE) return ARMOR_CHAINMAIL_CHESTPLATE;
        if (material == Material.CHAINMAIL_LEGGINGS) return ARMOR_CHAINMAIL_LEGGINGS;
        if (material == Material.CHAINMAIL_BOOTS) return ARMOR_CHAINMAIL_BOOTS;

        if (material == Material.IRON_HELMET) return ARMOR_IRON_HELMET;
        if (material == Material.IRON_CHESTPLATE) return ARMOR_IRON_CHESTPLATE;
        if (material == Material.IRON_LEGGINGS) return ARMOR_IRON_LEGGINGS;
        if (material == Material.IRON_BOOTS) return ARMOR_IRON_BOOTS;

        if (material == Material.GOLDEN_HELMET) return ARMOR_GOLDEN_HELMET;
        if (material == Material.GOLDEN_CHESTPLATE) return ARMOR_GOLDEN_CHESTPLATE;
        if (material == Material.GOLDEN_LEGGINGS) return ARMOR_GOLDEN_LEGGINGS;
        if (material == Material.GOLDEN_BOOTS) return ARMOR_GOLDEN_BOOTS;

        if (material == Material.DIAMOND_HELMET) return ARMOR_DIAMOND_HELMET;
        if (material == Material.DIAMOND_CHESTPLATE) return ARMOR_DIAMOND_CHESTPLATE;
        if (material == Material.DIAMOND_LEGGINGS) return ARMOR_DIAMOND_LEGGINGS;
        if (material == Material.DIAMOND_BOOTS) return ARMOR_DIAMOND_BOOTS;

        if (material == Material.NETHERITE_HELMET) return ARMOR_NETHERITE_HELMET;
        if (material == Material.NETHERITE_CHESTPLATE) return ARMOR_NETHERITE_CHESTPLATE;
        if (material == Material.NETHERITE_LEGGINGS) return ARMOR_NETHERITE_LEGGINGS;
        if (material == Material.NETHERITE_BOOTS) return ARMOR_NETHERITE_BOOTS;

        if (material == Material.TURTLE_HELMET) return ARMOR_TURTLE_HELMET;

        return 0;
    }

    public static ArmorSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Armor system not initialized!");
        }
        return instance;
    }
}
