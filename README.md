# BotMod

**Never have to manually AFK farm again.**

BotMod is a [NeoForge](https://neoforged.net/) mod for Minecraft **1.21.1** that lets you spawn a stationary "bot" at your location. The bot wears your skin, force-loads its chunk, and — crucially — fools the game's spawning engine into treating the spot as player-occupied. The result: your mob and passive farms keep running while you wander off, log out of the area, or do something else entirely.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.172-F16436)
![Java](https://img.shields.io/badge/Java-21-007396)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## How it works

A vanilla farm stops the moment no real player is nearby — the chunk unloads and the natural spawner has no "player" to anchor spawns to. BotMod solves both problems at once.

When you run `/bot spawn`, the mod:

1. **Spawns a `BotEntity`** at your position, rotated to match your facing and rendered with your own skin (downloaded asynchronously from Mojang's session servers, falling back to the default skin while it loads).
2. **Force-loads the bot's chunk** via `setChunkForced(...)`, so the farm keeps ticking after you leave render distance.
3. **Injects a `FakePlayer`** into the level's player list. The natural spawner's `getNearestPlayer()` check now finds a "player" at that location, so hostile and passive mobs spawn around the bot exactly as if you were standing there.

The bot itself is deliberately inert: no AI, no movement, invulnerable, non-pushable, and immune to attacks. It's a presence anchor, not an autonomous agent — it doesn't move, fight, or collect drops. That's by design: it provides the two things a farm needs (a loaded chunk and a nearby player) and nothing else.

## Features

- Spawn named bots that persist in the world while the server runs
- Each bot renders with the spawning player's actual skin and a `[Bot] <name>` nametag
- Force-loaded chunks keep farms running when you're away
- Mob spawning works around the bot via fake-player injection
- Bots are invulnerable and can't be pushed, hit, or interacted with
- Tab-completion for bot names on removal
- Bots survive a server restart — they're restored automatically on startup, no need to respawn them

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.172+ |
| Java | 21 |
| Side | Client **and** server (required on both) |

> Because the mod registers a custom entity and renderer, it must be installed on both the client and the server. Vanilla clients can't connect to a server running BotMod.

## Installation

Download `botmod-1.1.0.jar` from the [latest release](https://github.com/juliuspleunes4/BotMod/releases/latest) and drop it into your `mods/` folder on both the client and server.

```
.minecraft/mods/botmod-1.1.0.jar
```

Alternatively, build from source (see [Building](#building-from-source)).

## Usage

All commands are available to every player (permission level 0). On a multiplayer server you may want to restrict the `/bot` command via your permissions setup.

| Command | Description |
|---|---|
| `/bot spawn <name>` | Spawns a bot with your skin at your current position. Must be run by a player. Fails if the name is already taken. |
| `/bot remove <name>` | Removes the named bot, releases its forced chunk, and removes its fake player. Supports tab-completion. |
| `/bot list` | Lists all active bots. |

**Example workflow**

```
/bot spawn ironfarm     # stand at your farm's AFK spot and spawn a bot
                        # walk away — the farm keeps producing
/bot list               # check what's running
/bot remove ironfarm    # tear it down when you're done
```

## Notes & limitations

- **Bots survive a server restart.** They're normal persisted entities and their forced-chunk ticket is reinstated automatically; on startup the mod re-scans the world and rebuilds `/bot list` from what it finds.
- **The bot is stationary.** It won't kill mobs, route items, or trigger anything that needs movement. Pair it with a conventional farm design (water streams, fall damage, hoppers, etc.).
- **One bot per chunk anchor.** Each bot force-loads its own chunk; spawn them where the farm actually needs presence.
- Bot names are unique per server session and capped at 16 characters internally for the fake-player profile.

## Building from source

You'll need **JDK 21**.

```bash
git clone https://github.com/juliuspleunes4/BotMod.git
cd BotMod

# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

The compiled mod lands in `build/libs/`. To launch a dev client with the mod loaded:

```bash
gradlew.bat runClient      # Windows
./gradlew runClient        # macOS / Linux
```

The project uses [ModDevGradle](https://github.com/neoforged/ModDevGradle) with [Parchment](https://parchmentmc.org/) mappings for readable parameter names in the IDE.

## Project structure

```
src/main/java/com/julius/botmod/
├── BotMod.java                   # Mod entry point; command + lifecycle wiring
├── bot/
│   ├── BotManager.java           # Spawn/remove logic, chunk forcing, fake-player injection
│   └── BotEventHandler.java      # Cancels attacks targeting bots
├── command/
│   └── BotCommand.java           # /bot spawn | remove | list
├── entity/
│   ├── BotEntity.java            # The bot mob: no AI, invulnerable, stores owner skin
│   └── ModEntities.java          # Entity type registration
└── client/
    ├── ClientSetup.java          # Registers the renderer
    └── BotEntityRenderer.java    # Renders the bot with the owner's player skin
```

## License

Released under the [MIT License](LICENSE). © 2026 Julius Pleunes.