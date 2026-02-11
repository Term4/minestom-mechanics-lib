# Damage System — Final Architecture

Registry-based damage system with unified item component storage.

## Package Structure

```
health/
├── HealthSystem.java              # Orchestrator, event listeners
├── InvulnerabilityTracker.java    # I-frame state (transient tags on entities)
└── damage/
    ├── DamageCalculator.java      # Damage math (multipliers, modifiers, future enchants)
    ├── DamageResult.java          # Pipeline result (for knockback integration)
    ├── DamageTracker.java         # Abstract: definition + optional tick tracking
    ├── DamageType.java            # Core + registry + unified pipeline
    ├── DamageTypeProperties.java  # Properties record (enabled, blockable, etc.)
    ├── util/
    │   └── DamageOverride.java    # Tag override (mult, modify, custom, configOverride)
    └── types/
        ├── Fall.java, Fire.java, Cactus.java   # Environmental (tick-based)
        ├── Melee.java, Arrow.java              # Combat (event-only)
        ├── Thrown.java, Generic.java           # Projectile (event-only)

components/
├── Mechanics.java                 # Public API: builder, static getters, TAG
├── MechanicsData.java             # Record: all item overrides
└── MechanicsDataSerializer.java   # Single serializer for item data
```

## Item Data: Mechanics Component

All item overrides use one component (`Mechanics.TAG`):

```java
Mechanics.builder()
    .damage("melee", DamageOverride.override(props.withBypassCreative(true)))
    .knockback(kbMult(5.0, 2.0))
    .blockable(BlockableTagValue.blockable(true, 0.5, 0.5, 0.5))
    .velocity(velMult(1.5))
    .build()
```

## Entity/World Data: Transient Tags

```java
player.setTag(HealthSystem.tag("fire"), DamageOverride.mult(0.5));
world.setTag(HealthSystem.tag("fall"), DamageOverride.DISABLED);
```

## Adding a New Damage Type

1. Create `types/MyType.java` extending `DamageTracker`
2. Register in `HealthSystem`: `DamageType.register(new MyType())`
3. Done — tags, component resolution, and pipeline work automatically
