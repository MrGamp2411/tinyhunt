# Repository Guidelines
- This repository hosts a PaperMC plugin project built with Maven.
- Keep Java source under `src/main/java`, organized by package (`com.example.tinyhunt` unless the scope requires otherwise).
- Resource files (e.g., `plugin.yml`, `config.yml`) belong in `src/main/resources`.
- Prefer descriptive class names and JavaDoc for new public APIs.
- Avoid embedding CSS or JavaScript directly in HTML templates within this repository.
- The `com.example.tinyhunt` package is organized into three sub-packages:
  - `command` for Bukkit command executors and tab completers.
  - `game` for game flow controllers, state, and listeners.
  - `model` for serializable representations such as areas or player roles.
- When adding new commands, wire them through the central `TinyHuntCommand` class and keep permission keys consistent with the pattern `tinyhunt.<action>`.
- Persist game configuration (areas, spawns, numeric settings) via the plugin config to allow administrators to tweak values without rebuilding.

# Notes
- Il README nella root documenta flusso di gioco, comandi e parametri del plugin TinyHunt.
