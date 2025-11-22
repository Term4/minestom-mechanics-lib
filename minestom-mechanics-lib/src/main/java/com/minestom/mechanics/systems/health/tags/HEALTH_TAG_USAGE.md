# Health System Tag Usage Guide

The health system now fully supports the `ConfigurableSystem` pattern, allowing runtime configuration via tags on items, players, and worlds.

## Damage Type Tags

Each damage type (fall, fire, cactus) supports:
- **MULTIPLIER**: Multiplicative scaling (item × player × world)
- **MODIFY**: Additive changes (item + player + world)
- **CUSTOM**: Complete config override (item > player > world > server)

### Priority Chain
For damage types: **Item > Attacker > Player > Victim > World > Server Default**

### Usage Examples

```java
import static com.minestom.mechanics.systems.health.tags.HealthTagWrapper.*;
import com.minestom.mechanics.systems.health.HealthSystem;

// ===========================
// PER-ITEM CONFIGURATION
// ===========================

// Boots that reduce fall damage by 50%
ItemStack boots = ItemStack.of(Material.LEATHER_BOOTS);
boots.setTag(HealthSystem.FALL_DAMAGE, healthMult(0.5));

// Weapon that doubles fire damage
ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
sword.setTag(HealthSystem.FIRE_DAMAGE, DOUBLE_DAMAGE);

// Armor that disables cactus damage
ItemStack chestplate = ItemStack.of(Material.LEATHER_CHESTPLATE);
chestplate.setTag(HealthSystem.CACTUS_DAMAGE, DISABLED);

// ===========================
// PER-PLAYER CONFIGURATION
// ===========================

// Player immune to fire
player.setTag(HealthSystem.FIRE_DAMAGE, DISABLED);

// Player takes double fall damage
player.setTag(HealthSystem.FALL_DAMAGE, DOUBLE_DAMAGE);

// Player takes half cactus damage
player.setTag(HealthSystem.CACTUS_DAMAGE, HALF_DAMAGE);

// Combined multiplier and modify
player.setTag(HealthSystem.FALL_DAMAGE, healthMult(2.0).thenAdd(5.0));
// Result: (baseDamage * 2.0) + 5.0

// ===========================
// PER-WORLD CONFIGURATION
// ===========================

// World with double fall damage for everyone
world.setTag(HealthSystem.FALL_DAMAGE, DOUBLE_DAMAGE);

// World with no fire damage
world.setTag(HealthSystem.FIRE_DAMAGE, DISABLED);

// World with custom fall damage config
world.setTag(HealthSystem.FALL_DAMAGE, healthSet(
    HealthTagValue.healthConfig(1.5f, 0.0f, true)
));

// ===========================
// PRESETS
// ===========================

// Available presets:
HealthTagWrapper.DOUBLE_DAMAGE  // 2x multiplier
HealthTagWrapper.HALF_DAMAGE    // 0.5x multiplier
HealthTagWrapper.NO_DAMAGE      // 0x multiplier
HealthTagWrapper.DISABLED       // Custom: enabled=false
HealthTagWrapper.ENABLED        // Custom: enabled=true
```

## Invulnerability Tags

Invulnerability supports:
- **CUSTOM**: Complete config override (item > player > world > server)

### Priority Chain
For invulnerability: **Item > Attacker > Player > Victim > World > Server Default**

### Usage Examples

```java
import static com.minestom.mechanics.systems.health.tags.InvulnerabilityTagWrapper.*;
import static com.minestom.mechanics.systems.health.tags.InvulnerabilityTagValue.*;
import com.minestom.mechanics.systems.health.HealthSystem;

// ===========================
// PER-PLAYER CONFIGURATION
// ===========================

// Player with 20 ticks of invulnerability
player.setTag(HealthSystem.INVULNERABILITY, 
    invulnSet(invulnTicks(20)));

// Player with no invulnerability
player.setTag(HealthSystem.INVULNERABILITY, NO_INVULNERABILITY);

// Player with damage replacement enabled
player.setTag(HealthSystem.INVULNERABILITY, REPLACEMENT_ENABLED);

// Combined configuration
player.setTag(HealthSystem.INVULNERABILITY, 
    invulnSet(invulnConfig(15, true, false)));

// ===========================
// PER-WORLD CONFIGURATION
// ===========================

// World with long invulnerability
world.setTag(HealthSystem.INVULNERABILITY, LONG_INVULNERABILITY);

// World with damage replacement enabled
world.setTag(HealthSystem.INVULNERABILITY, REPLACEMENT_ENABLED);

// ===========================
// PER-ITEM CONFIGURATION
// ===========================

// Armor that grants extra invulnerability
ItemStack armor = ItemStack.of(Material.DIAMOND_CHESTPLATE);
armor.setTag(HealthSystem.INVULNERABILITY, 
    invulnSet(invulnTicks(25)));

// ===========================
// PRESETS
// ===========================

// Available presets:
InvulnerabilityTagWrapper.NO_INVULNERABILITY      // 0 ticks
InvulnerabilityTagWrapper.STANDARD                // 10 ticks
InvulnerabilityTagWrapper.LONG_INVULNERABILITY    // 20 ticks
InvulnerabilityTagWrapper.REPLACEMENT_ENABLED      // Custom: replacement=true
InvulnerabilityTagWrapper.REPLACEMENT_DISABLED     // Custom: replacement=false
```

## How It Works

### Multiplier Stacking
Multipliers are applied multiplicatively:
- Item: 2.0x
- Player: 0.5x
- World: 1.5x
- **Result**: baseDamage × 2.0 × 0.5 × 1.5 = baseDamage × 1.5

### Modify Stacking
Modifies are applied additively:
- Item: +5.0
- Player: -2.0
- World: +1.0
- **Result**: baseDamage + 5.0 - 2.0 + 1.0 = baseDamage + 4.0

### Custom Config Priority
Custom configs override everything and use priority:
- If item has custom config → use it
- Else if player has custom config → use it
- Else if world has custom config → use it
- Else → use server default

### Complete Formula
```
finalDamage = ((baseDamage × itemMult × playerMult × worldMult) + itemMod + playerMod + worldMod)
if (custom config exists) {
    finalDamage = customConfig.calculate(customConfig)
}
```