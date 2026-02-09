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
import com.minestom.mechanics.CombatManager;
import com.minestom.mechanics.config.combat.CombatPresets;

public class MyServer {
    public static void main(String[] args) {
        // Initialize server
        MinecraftServer server = MinecraftServer.init();
        
        // Initialize combat system with MinemanClub preset
        CombatManager.getInstance().initialize(CombatPresets.Modes.MINEMEN);
        
        // Start server
        server.start("0.0.0.0", 25566);
    }
}
```

### 3. Custom Configuration

```java
// Create custom combat configuration
CombatManager.getInstance()
    .configure()
    .fromPreset(CombatPresets.Modes.MINEMEN)
    .withHitDetection(CombatPresets.STRICT_HIT_DETECTION)
    .withName("Custom Minemen", "Minemen with strict hit detection")
    .apply();
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
CombatManager manager = CombatManager.getInstance();

// Enable/disable blocking
manager.setBlockingEnabled(true);

// Configure knockback sync
manager.setKnockbackSyncEnabled(true);

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

### CombatManager

The main entry point for the combat system.

```java
// Initialize with preset
CombatManager.getInstance().initialize(CombatPresets.Modes.MINEMEN);

// Initialize with custom configuration
CombatManager.getInstance().configure()
    .fromPreset(CombatPresets.Modes.MINEMEN)
    .withHitDetection(customHitDetection)
    .apply();

// Runtime configuration
manager.setBlockingEnabled(true);
manager.setKnockbackSyncEnabled(false);

// Cleanup
manager.cleanupPlayer(player);
manager.shutdown();
```

### Configuration Classes

- `CombatRulesConfig`: Combat mechanics configuration
- `HitDetectionConfig`: Hit detection settings
- `DamageConfig`: Damage system configuration
- `BlockingConfig`: Blocking system settings
- `ProjectileConfig`: Projectile system configuration

### Presets

- `CombatPresets.Modes.*`: Complete combat mode presets
- `CombatPresets.STANDARD_COMBAT_RULES`: Individual config presets
- `CombatPresets.STANDARD_HIT_DETECTION`: Hit detection presets
- `CombatPresets.STANDARD_DAMAGE`: Damage system presets

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
