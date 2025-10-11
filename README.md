# Arkanoid Game - Enhanced Edition

A Java implementation of the classic Arkanoid/Breakout game with improved code organization and enhanced physics.

## Project Structure

```
src/
├── ArkanoidGame.java          # Main entry point
├── entities/                  # Game objects
│   ├── Ball.java             # Ball entity with physics
│   ├── Paddle.java           # Player-controlled paddle
│   └── Block.java            # Destructible blocks
├── game/                     # Core game logic
│   ├── GameFrame.java        # Main window setup
│   └── GamePanel.java        # Game loop and rendering
├── levels/                   # Level management
│   ├── LevelManager.java     # Level progression logic
│   └── LevelBuilder.java     # Level layout creation
└── utils/                    # Utility classes
    ├── GameConfig.java       # Configuration constants
    └── Velocity.java         # 2D velocity calculations
```

## Features

### Enhanced Physics
- **Smooth Movement**: Floating-point positions for fluid ball movement
- **Paddle Angle Variation**: Ball direction changes based on where it hits the paddle
- **Smart Collision Detection**: Proper side-based bouncing for blocks
- **Speed Control**: Automatic speed limiting to prevent extreme velocities
- **Bounds Checking**: Prevents ball from getting stuck on screen edges

### Improved Game Design
- **Three Levels**: Each with unique block patterns
- **Progressive Difficulty**: Blocks require different numbers of hits
- **Visual Feedback**: Color-coded blocks based on remaining hits
- **Multiple Controls**: Arrow keys or WASD for paddle movement

### Code Organization
- **Separation of Concerns**: Each class has a single, well-defined purpose
- **Configuration Management**: All constants centralized in GameConfig
- **Modular Design**: Easy to add new levels, entities, or features
- **Clean Architecture**: Clear separation between entities, game logic, and utilities

## How to Play

1. **Movement**: Use Left/Right arrow keys or A/D keys to move the paddle
2. **Objective**: Break all blocks to advance to the next level
3. **Block Types**:
   - Red blocks: 1 hit to destroy
   - Orange blocks: 2 hits to destroy  
   - Magenta blocks: 3 hits to destroy
4. **Controls**:
   - Space: Resume paused game
   - Escape: Quit game (with confirmation)

## Level Descriptions

1. **Level 1**: Rectangular grid (5×8 blocks) - Good for learning the basics
2. **Level 2**: Pyramid pattern - More strategic gameplay
3. **Level 3**: Diamond formation - Challenging layout with varied block strengths

## Technical Improvements

### From Original Code
- ✅ Organized into logical packages
- ✅ Centralized configuration constants
- ✅ Enhanced collision detection
- ✅ Better physics with floating-point precision
- ✅ Improved input handling (multiple key options)
- ✅ Proper bounds checking
- ✅ Modular level creation system
- ✅ Comprehensive documentation

### Physics Enhancements
- **Velocity Variation**: Small random changes prevent predictable patterns
- **Paddle Angle Control**: Hit position affects ball direction (up to 60°)
- **Speed Limiting**: Maintains playable speeds (2.0 - 6.0 units)
- **Edge Prevention**: Smart bounds checking prevents edge-sticking bugs

## Building and Running

To compile and run the game:

```bash
# Navigate to the project directory
cd Arkanoid

# Compile all Java files
javac -d bin src/**/*.java

# Run the game
java -cp bin ArkanoidGame
```

Or using an IDE like VS Code, Eclipse, or IntelliJ IDEA:
1. Open the project folder
2. Run `ArkanoidGame.java`

## Future Enhancement Ideas

- Sound effects and background music
- Power-ups (multi-ball, larger paddle, etc.)
- More level patterns
- High score system
- Particle effects
- Customizable controls
- Save/load game state