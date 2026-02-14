# Minestom Mechanics Library

A production-ready mechanics library for Minestom servers, providing comprehensive game mechanics with cross-version support and extensive customization. Perfect for servers using ViaVersion with Velocity proxy or standalone Minestom servers.

## Features

- **Cross-Version Support**: Works with ViaVersion and Velocity proxy for multi-version servers
- **Comprehensive Combat System**: Full 1.8-style PvP with modern enhancements
- **Multiple Combat Modes**: Pre-configured modes for different server types (MinemanClub, Vanilla, Hypixel, etc.)
- **Advanced Hit Detection**: Server-side raycasting with configurable reach and hitbox expansion
- **Knockback System**: Multiple knockback profiles with sync support
- **Blocking System**: Complete blocking mechanics with damage reduction and visual effects
- **Projectile Support**: Full projectile system with bow, fishing rod, and throwable items
- **Damage System**: Environmental damage handling with invulnerability frames
- **Client Version Detection**: Via ViaVersion channels `vv:mod_details` (client mods) or `vv:proxy_details` (Velocity/Bungee with send-player-details); requires ViaVersion in the pipeline for version-aware behavior
- **Legacy Mechanics**: Support for older Minecraft mechanics (1.8, 1.9, etc.)
- **GUI System**: Built-in GUI framework for mechanics settings
- **Extensive Configuration**: Highly configurable with preset support

## Quick Start

### 1. Add Dependency

Add the library to your Maven project:

```xml
<dependency>
  <groupId>com.minestom</groupId>
  <artifactId>minestom-mechanics-lib</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Basic Usage

```java
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.config.MechanicsPresets;

public class MyServer {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        // Initialize with preset (Minemen, Vanilla, Hypixel)
        MechanicsManager.getInstance().withPreset(MechanicsPresets.MINEMEN);

        server.start("0.0.0.0", 25566);
    }
}
```

### 3. Custom Configuration

```java
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.config.combat.CombatPresets;
import com.minestom.mechanics.config.gameplay.GameplayPresets;
import com.minestom.mechanics.config.gameplay.DamagePresets;

// Configure systems individually
MechanicsManager.getInstance()
    .configure()
    .withCombat(CombatPresets.MINEMEN)
    .withGameplay(GameplayPresets.MINEMEN)
    .withDamage(DamagePresets.MINEMEN)
    .withKnockback(CombatPresets.MINEMEN.knockbackConfig())
    .initialize();
```

## Combat Modes

The library includes several pre-configured combat modes:

- **MinemanClub**: Fast combo combat with 50ms invulnerability
- **Vanilla**: Classic 1.8 PvP mechanics
- **Hypixel**: Hypixel network combat style
- **Lunar**: Lunar Network combat style
- **Intave/Grim**: Modern anticheat compatible combat
- **Balanced**: Middle ground for general play
- **Debug**: Testing mode with extreme values

## Configuration

### Combat Rules
- Attack cooldown settings
- Critical hit multipliers
- Sprint bonus windows
- Knockback profiles

### Hit Detection
- Server-side reach
- Attack packet reach
- Hitbox expansion
- Angle validation

### Damage System
- Invulnerability frames
- Environmental damage
- Damage replacement
- Fire and fall damage

### Blocking System
- Damage reduction
- Knockback reduction
- Visual effects
- Player preferences

### Projectiles
- Bow mechanics
- Fishing rod physics
- Throwable items
- Knockback integration

## Advanced Usage

### Custom Combat Mode

TODO

### Runtime Configuration

```java
MechanicsManager manager = MechanicsManager.getInstance();

// Enable/disable blocking
manager.getCombatManager().setBlockingEnabled(true);

// Get system status
String status = manager.getStatus();
```

### GUI Integration

```java
import com.minestom.mechanics.gui.GuiManager;

// Open blocking settings GUI for player
GuiManager.getInstance().openBlockingSettings(player);
```

## API Reference

### MechanicsManager

The main entry point for initializing all mechanics systems.

```java
// Initialize with preset (MINEMEN, VANILLA, HYPIXEL)
MechanicsManager.getInstance().withPreset(MechanicsPresets.MINEMEN);

// Or configure systems individually
MechanicsManager.getInstance()
    .configure()
    .withCombat(CombatPresets.MINEMEN)
    .withGameplay(GameplayPresets.MINEMEN)
    .withDamage(DamagePresets.MINEMEN)
    .withKnockback(combatConfig.knockbackConfig())
    .initialize();

// Runtime: get CombatManager for blocking, etc.
MechanicsManager.getInstance().getCombatManager().setBlockingEnabled(true);
```

### Configuration Classes

- `CombatConfig`: Combat mechanics (attack cooldown, blocking, knockback profile)
- `HitDetectionConfig`: Hit detection settings
- `DamageConfig`: Damage system (invulnerability, replacement, buffers)
- `GameplayConfig`: Gameplay (eye height, movement)
- `KnockbackConfig`: Knockback profiles

### Presets

- `MechanicsPresets.*`: Full preset (MINEMEN, VANILLA, HYPIXEL)
- `CombatPresets.*`: Combat config only
- `GameplayPresets.*`, `DamagePresets.*`, `KnockbackPresets.*`: Per-system presets

## Requirements

- Java 25+
- Minestom 2025.10.11-1.21.10+
- Maven 3.6+

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For support and questions, please open an issue on the GitHub repository.
