# 137 Project

Networked Game Project in Java.

## Team Members
- Erin Reiley Amistoso
- Mary Eunice Magnaye
- Edgar Alan Emmanuel Tiamzon III

## How to Run

### Prerequisites
Make sure you have the Java Development Kit (JDK) 17 or higher installed on your system.
You will also need **Maven** installed to manage the JavaFX dependencies. You can verify this by running `mvn -version`.

### Compilation and Execution

1. Open your terminal.
2. Navigate into the project folder:
   ```bash
   cd 137_Project
   ```
3. Since JavaFX requires Maven to download its dependencies, and you are using the portable Maven, you can compile and start the game by running this exact command in your terminal:

```bash
.\apache-maven-3.9.6\bin\mvn.cmd clean javafx:run
```

## Compilation using jar file inside the repo
```bash
java -jar target\project137-game-1.0-SNAPSHOT-fat.jar
```

## Compilation using jar file outside the repo
```bash
java -jar project137-game-1.0-SNAPSHOT-fat.jar
```


## Game Mechanics

### Core Rules
- *Leave a Trail*: Your empanada always moves and leaves a dough trail behind as you roam outside your territory.
- *Close a Loop*: Return to your own territory to enclose an area — all tiles inside become yours!

### Items and Hazards
- *Rolling Pin*: -30% speed
- *Ice Spill*: Freeze
- *Rotten Egg*: Reverse controls
- *Spilled Oil*: 1.5x speed
- *Dough*: Wider trail
- *Flour*: Invisible trail to enemies

### How to Win
When the timer runs out, the player with the most territory (%) wins. If only one player remains before time's up, they win immediately. In timed mode, eliminated players respawn after 3 seconds.

## Main Menu & Controls

### Controls
- *Move*: W, A, S, D or Arrow Keys (↑, ↓, ←, →)
- *Mouse Cursor*: Used for navigating the UI and menus.

## Folder Organization

```text
137_Project/
├── assets/               # Game assets (images, sounds, levels, etc.)
│   ├── images/           # Sprites, backgrounds, UI elements
│   ├── sounds/           # Sound effects and background music
│   └── levels/           # Level data (e.g., text files or JSON)
├── src/                  # Source code directory
│   └── app/              # Main application package
│       ├── core/         # Main game loop, window setup, state management
│       ├── entities/     # Game objects (Player, Enemies, Items)
│       ├── graphics/     # Rendering logic, animations, UI components
│       ├── input/        # Keyboard and mouse listeners
│       ├── screens/      # UI screens (Main Menu, Multiplayer, etc.)
│       ├── utils/        # Helper classes, constants, math functions
│       └── Main.java     # The main entry point of the program
└── README.md             # Project documentation (this file)
```
