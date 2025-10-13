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
- Il documento `docs/design/tinyhunt-game-design.md` contiene la checklist completa delle feature richieste (ruoli, abilità, power-up, QoL, roadmap). Aggiornalo quando implementi nuove parti del gioco.
- Mantieni questo file aggiornato con link utili o convenzioni scoperte durante lo sviluppo per velocizzare il lavoro futuro.
- Il `GameManager` gestisce ora respawn ritardati (stato `CONVERTING`), la Sudden Death (ping glowing + speed ai cacciatori) e l'HUD condiviso (`MatchHud` con scoreboard e bossbar). Consulta `config.yml` per i nuovi parametri (`timers.runner-respawn-*`, sezione `sudden-death`, sezione `hud`).
- Target API aggiornato a Paper 1.21.x (testato su 1.21.8); assicurati che le dipendenze Maven e `plugin.yml` restino allineati.
- Moduli MMO/party sperimentali vivono sotto `com.kjaza.tinymmo`. Qui trovi `skill` (gestione abilità, cooldown sia logici che visivi) e `party` (party manager + chat rapida `/p` con listener `@`). Le classi chiave vengono istanziate in `TinyHuntPlugin` e usano `LegacyComponentSerializer` per i messaggi in actionbar.
- Il sistema arena supporta più definizioni: `ArenaDefinition` vive in `model`, mentre `ArenaSetupManager`, `ArenaSetupListener` e `ArenaBoundaryListener` gestiscono creazione, wand e controllo confini. Le arene sono serializzate in `config.yml` sotto `arenas` con chiave `active-arena`.
- Il comando `/tinyhunt join` apre ora `JoinMenu`, inventario 1x9 che mostra stato lobby/arena e consente join/leave. Gli item e i testi derivano da `messages.menu-*` in `config.yml`.
