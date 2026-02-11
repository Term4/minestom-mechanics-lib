# Damage System Registry Refactor — Final State

Registry-based damage system architecture for scalable type registration,
clean config management, and easy addition of new types.

---

## Architecture

```
com.minestom.mechanics.systems.health/
├── HealthSystem.java              # Orchestrator; registers types, wires listeners
├── InvulnerabilityTracker.java
├── damage/
│   ├── DamageType.java            # Core: name, properties, tag resolution, damage calc
│   ├── DamageTypeProperties.java  # Shared properties record (enabled, blockable, etc.)
│   ├── DamageOverride.java        # Tag-based runtime overrides (mult, modify, custom)
│   ├── DamageApplicator.java      # Pipeline: creative check, i-frames, replacement
│   ├── DamageTypeDefinition.java  # Interface: defines a damage type + config + tracker
│   ├── DamageTypeRegistry.java    # Static registry: O(1) lookup by id / RegistryKey
│   ├── DamageTracker.java         # Interface: tick-based environmental damage
│   ├── FallTracker.java           # Extension of DamageTracker with fall-specific API
│   └── types/                     # One class per damage type
│       ├── Fall.java              # Fall damage definition + tracker
│       ├── Fire.java              # Fire/lava/burning definition + tracker
│       └── Cactus.java            # Cactus contact definition + tracker
```

## How It Works

1. **HealthSystem.initialize()** registers built-in definitions (`Fall`, `Fire`, `Cactus`)
2. **DamageTypeRegistry.register()** creates the `DamageType`, sets config, indexes by id + RegistryKey
3. **Tick handler** iterates `DamageTypeRegistry.getTrackers()` and calls `tick()` on each
4. **Damage event** looks up type via `DamageTypeRegistry.find(registryKey)`, runs pipeline
5. **Cleanup/shutdown** calls `DamageTypeRegistry.clear()`

## Adding a New Damage Type

1. Create `damage/types/MyType.java` implementing `DamageTypeDefinition<MyType.Config>`
2. Define a `Config` record with defaults and `withX()` methods
3. Implement `createTracker()` (return tracker for tick-based, or `null` for event-only)
4. Register in `HealthSystem` constructor: `DamageTypeRegistry.register(new MyType())`

No edits needed in DamageApplicator, DamageTypeRegistry, or config classes.

## Config

Each type carries its own typed `Config` record:

```java
// Fall config
new Fall.Config(3.0f)                        // safeFallDistance

// Fire config
new Fire.Config(2.0f, 4.0f, 1.0f, 20, 300)  // fireDamage, lavaDamage, onFireDamage, interval, burn

// Cactus config
new Cactus.Config(1.0f)                      // damage
```

Override at runtime:
```java
DamageType fallType = HealthSystem.getInstance().getDamageType("fall");
fallType.setConfig(new Fall.Config(5.0f));  // Change safe fall distance
```

## Tag Overrides (unchanged)

```java
player.setTag(HealthSystem.FIRE_DAMAGE, DamageOverride.mult(0.5));
world.setTag(HealthSystem.FALL_DAMAGE, DamageOverride.DISABLED);
item.setTag(HealthSystem.CACTUS_DAMAGE, DamageOverride.override(
    DamageTypeProperties.ENVIRONMENTAL_DEFAULT.withBlockable(true)
));
```

## Future Types

| Type | Matched Keys | Tracker? |
|------|-------------|----------|
| Melee | PLAYER_ATTACK | No (event-driven) |
| Arrow | ARROW | No (event-driven) |
| Drowning | DROWN | Yes (tick-based) |
| Suffocation | IN_WALL | Yes (tick-based) |
| Poison | MAGIC | No (event-driven) |
