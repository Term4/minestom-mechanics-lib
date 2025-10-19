# Minestom Test Server

A test server demonstrating the Minestom Mechanics Library capabilities. This server showcases various mechanics modes, features, and provides a testing environment for the mechanics system.

## Features

- **Multiple Mechanics Modes**: Demonstrates all available mechanics presets
- **Cross-Version Support**: Works with ViaVersion and Velocity proxy
- **Command System**: Admin and player commands for testing
- **GUI Integration**: Mechanics settings GUI
- **Legacy Mechanics**: 1.8 compatibility features
- **Client Version Detection**: Automatic optimization for different client versions
- **Projectile Testing**: Full projectile system demonstration
- **Debug Tools**: Memory monitoring and entity visibility testing

## Quick Start

### 1. Build and Run

```bash
# Build the project
mvn clean package

# Run the test server
java -jar target/minestom-test-server-1.0-SNAPSHOT.jar
```

### 2. Connect

Connect to `localhost:25566` with any Minecraft client (1.8.9+ recommended).

## Mechanics Modes

The test server demonstrates several mechanics modes:

- **MinemanClub**: Fast combo combat (default)
- **Vanilla**: Classic 1.8 PvP
- **Hypixel**: Hypixel-style combat
- **Lunar**: Lunar Network style
- **Intave/Grim**: Anticheat-friendly combat
- **Balanced**: General-purpose combat
- **Debug**: Testing with extreme values

## Commands

### Player Commands

- `/block` or `/blocking` - Open blocking settings GUI
- `/kb` - Knockback settings and information

### Admin Commands

- `/blockingconfig` - Configure blocking system
- `/memcheck` - Memory usage monitoring
- `/entitytest` - Entity visibility testing

## Configuration

The server can be configured by modifying `Main.java`:

TODO add updated methods for configuring mechanics

## Server Features

### Starter Kit
- Diamond sword with blocking capability
- Bow with arrows
- Fishing rod
- Throwable items (snowballs, eggs)
- Building blocks
- Food

### World Setup
- Flat world generation
- Spawn protection
- Speed and resistance effects
- Inventory sync fixes

### Compatibility
- 1.8 mechanics enforcement
- Legacy movement handling
- Client version detection
- Modern client optimization

## Testing

### Combat Testing
1. Connect with multiple clients
2. Test different combat modes
3. Verify hit detection and reach
4. Test knockback mechanics
5. Validate blocking system

### Projectile Testing
1. Test bow mechanics
2. Verify fishing rod physics
3. Test throwable items
4. Check projectile knockback

### GUI Testing
1. Open blocking settings
2. Configure preferences
3. Test real-time changes
4. Verify persistence

## Development

### Project Structure

```
src/main/java/com/test/minestom/
├── Main.java                    # Server entry point
├── commands/                    # Command system
│   ├── combat/                  # Combat commands
│   └── debug/                   # Debug commands
├── compat/                      # Legacy compatibility
│   └── legacymechanics/         # 1.8 mechanics
├── config/                      # Server configuration
│   └── server/                  # Server settings
└── servers/                     # Game modes
    └── games/                   # Game implementations
```

### Adding New Features

1. **New Commands**: Add to `commands/` package
2. **New Game Modes**: Add to `servers/games/`
3. **New Compatibility**: Add to `compat/`
4. **Configuration**: Update `Main.java`

### Testing New Combat Modes

2. Test with multiple clients
3. Verify all systems work correctly
4. Document any issues

## Requirements

- Java 25+
- Minestom 2025.10.11-1.21.10+
- Maven 3.6+
- Minestom Combat Library

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For support and questions, please open an issue on the GitHub repository.
