# AI Terror Mod (Project-NULL)

This repository contains the starting scaffold for a **Minecraft Java Edition mod** powered by AI. The goal is to have models analyze the game state and decide the best way to terrorize the player, with the ability to interact with the world.

## Getting Started

1. **Install prerequisites**:
   - Java Development Kit (JDK) 17 or newer
   - [Gradle](https://gradle.org/) (or use the included wrapper)
   - Minecraft launcher with a Fabric installation for development

2. **Generate the Gradle wrapper** (if you haven't already):
   ```sh
   gradle wrapper --gradle-version 8.2
   ```
   After this you can run `./gradlew` instead of a system Gradle.

3. **Build and run**:
   - `./gradlew build` compiles the mod and produces a JAR in `build/libs`.
   - `./gradlew runClient` starts a development instance of Minecraft.

4. **IDE setup**:
   - Import the project as a Gradle project in IntelliJ IDEA, Eclipse, or VS Code.
   - The `src/main/java` and `src/main/resources` folders are already configured.

## Project Structure

```
Project-NULL/
├── build.gradle          # Fabric/Gradle configuration
├── settings.gradle
├── src/main/java         # Java source code
│   └── com/rjwebb134/aiterror
│       ├── MainMod.java  # Mod entrypoint
│       └── AIManager.java# stub for AI logic
└── src/main/resources
    ├── fabric.mod.json   # Fabric metadata
    └── assets/aiterror   # placeholder for textures/models
```

## Next Steps

* Hook `AIManager` into game events (ticks, player moves, mob spawns).
* Integrate a machine learning library (TensorFlow, PyTorch via JNI, etc.).
* Create neural nets/behaviors that observe the world and choose terrifying actions.
* Allow the AI to manipulate blocks, spawn creatures, send chat messages, or affect player status.

Happy modding! 🎮
