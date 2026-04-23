# 137 Project

This is a simple Java project that displays a working graphical screen using JavaFX.

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

## How to Run

### Prerequisites
Make sure you have the Java Development Kit (JDK) 17 or higher installed on your system.
You will also need **Maven** installed to manage the JavaFX dependencies. You can verify this by running `mvn -version`.

### Compilation and Execution

Since JavaFX requires Maven to download its dependencies, and you are using the portable Maven we downloaded, you can compile and start the game by running this exact command in your terminal:

```bash
.\apache-maven-3.9.6\bin\mvn.cmd clean javafx:run
```

A window titled "137 Project - Networked Game" should pop up on your screen!