# TinyHunt

TinyHunt è un plugin Paper per Minecraft 1.21.x che introduce un minigioco Hunter vs Runner completamente gestito via comandi e configurazione. I giocatori si mettono in coda, vengono teletrasportati in arena e dopo un conto alla rovescia uno di loro diventa Hunter: se tutti i Runner vengono convertiti gli Hunter vincono, altrimenti vincono i Runner resistendo fino allo scadere del tempo di gioco.

## Indice rapido
- [Caratteristiche principali](#caratteristiche-principali)
- [Flusso di gioco](#flusso-di-gioco)
- [HUD e feedback visivi](#hud-e-feedback-visivi)
- [Sistema skill e risorse MMO](#sistema-skill-e-risorse-mmo)
- [Chat di party integrata](#chat-di-party-integrata)
- [Configurazione passo dopo passo](#configurazione-passo-dopo-passo)
- [File di configurazione](#file-di-configurazione)
- [Messaggi personalizzabili e placeholder](#messaggi-personalizzabili-e-placeholder)
- [Comandi e permessi](#comandi-e-permessi)
- [Build dal sorgente](#build-dal-sorgente)
- [Suggerimenti operativi](#suggerimenti-operativi)
- [Risorse aggiuntive](#risorse-aggiuntive)

## Caratteristiche principali
- **Coda automatica e gestione stati** – Il `GameManager` coordina le fasi `WAITING`, `COUNTDOWN`, `RUNNING` ed `ENDING`, scegliendo automaticamente quando avviare il conto alla rovescia, confermare l'inizio partita o annullare se i giocatori scendono sotto il minimo configurato.
- **Conversione progressiva dei Runner** – Quando un Hunter colpisce un Runner l'evento viene annullato e parte un timer di conversione; allo scadere il giocatore rientra in arena come Hunter con invulnerabilità temporanea, preservando l'equilibrio della partita.
- **Sudden Death dinamica** – Negli ultimi minuti viene attivata una modalità finale con ping periodico dei Runner (glowing) e buff di velocità configurabili per gli Hunter, tenendo informati i partecipanti via messaggi e scoreboard.
- **HUD condiviso** – Bossbar, scoreboard laterale e actionbar mostrano tempo residuo, conteggio Runner/Hunter, stato del ping e messaggi contestuali, aggiornati ogni secondo tramite `MatchHud` e `VisualCooldowns`.
- **Configurazione persistente in-game** – Lobby, arena e punti di spawn si salvano direttamente via comandi `/tinyhunt`, con persistenza immediata su `config.yml` grazie agli helper `ConfiguredArea` e `ConfigLocationUtil`.
- **Compatibilità con Paper 1.21.8** – Il plugin è costruito contro l'API 1.21, include il supporto facoltativo all'attributo `GENERIC_SCALE` per ridimensionare modelli Hunter/Runner e registra automaticamente listeners e task ciclici all'avvio.

## Flusso di gioco
1. **Attesa in lobby** – I giocatori usano `/tinyhunt join` per entrare in coda. Se la configurazione non è completa il gioco lo segnala immediatamente, evitando avvii errati.
2. **Countdown automatico** – Al raggiungimento di `players.min` parte un conto alla rovescia configurabile (`timers.auto-start-seconds`) con broadcast periodici; se la coda scende sotto il minimo il countdown si interrompe da solo.
3. **Selezione Hunter** – Tutti i partecipanti vengono teletrasportati negli spawn dell'arena come Runner; dopo `timers.hunter-selection-seconds` un Runner casuale viene promosso Hunter e informato insieme agli altri.
4. **Partita attiva** – Gli Hunter devono colpire i Runner per convertirli; i Runner puntano a sopravvivere fino allo scadere di `timers.game-duration-seconds`. Tutti i progressi, ruoli e conversioni vengono tracciati in memoria per la durata del match.
5. **Chiusura e reset** – Al termine (timeout, eliminazione totale o stop manuale) i giocatori vengono riportati in lobby, gli stati ripristinati e la HUD smontata prima di tornare allo stato `WAITING`.

### Ruoli e stati speciali
- **RUNNER** – Stato iniziale, mantiene la scala configurata, è soggetto a conversione se colpito da Hunter.
- **CONVERTING** – Stato temporaneo durante il respawn; il giocatore viene messo in spettatore e riacquista il controllo solo al termine del timer.
- **HUNTER** – Stato offensivo, applica scala e buff (anche extra in Sudden Death). Gli Hunter vengono riportati in arena a ogni promozione o conversione completata.

## HUD e feedback visivi
- **Bossbar** – Mostra il tempo rimanente formattato (`hud.bossbar-title`) e progredisce linearmente in base alla durata totale del match.
- **Scoreboard laterale** – Titolo configurabile (`hud.scoreboard-title`) e linee dinamiche: tempo, conteggio Runner/Hunter, stato del prossimo ping e messaggi extra (es. “Sudden death”).
- **Actionbar cooldown** – Le abilità registrate dal modulo MMO impostano tempi di ricarica visibili in actionbar e come cooldown visivo sugli oggetti, garantendo feedback immediato ai giocatori.
- **Messaggistica** – Ogni fase chiave invia messaggi configurabili, supportando codici colore `&` e placeholder `%player%`, `%seconds%`, `%position%`, `%time%` ecc.

## Sistema skill e risorse MMO
TinyHunt include un modulo sperimentale `com.kjaza.tinymmo.skill` per abilità attive collegabili a oggetti custom:
- **Registrazione skill** – `SkillManager` conserva abilità identificate da `NamespacedKey`, collegate a un materiale, costo risorsa e cooldown; i metadati dell'oggetto determinano l'abilità eseguita e l'eventuale proprietario.
- **Gestione risorse** – `ResourceManager` gestisce pool di risorse (Mana, Stamina, Energy) per giocatore, permettendo di configurare costi dinamici da `config.yml` (`skills.costs.<id>.resource` / `amount`).
- **Cooldown logici e visivi** – `CooldownManager` traccia i tempi rimanenti lato server, mentre `VisualCooldowns` aggiorna actionbar e cooldown degli item ogni 10 tick grazie a un task schedulato in `TinyHuntPlugin`.
- **Hook evento di utilizzo** – `SkillListener` intercetta l'interazione primaria del giocatore, valida il possesso dell'oggetto, controlla risorse e cooldown e infine esegue l'azione definita, bloccando l'uso se i requisiti non sono soddisfatti.

Per creare nuove abilità è sufficiente registrarle in fase di avvio (es. in `TinyHuntPlugin#onEnable`) e popolare eventuali risorse iniziali tramite `ResourceManager#setResource`.

## Chat di party integrata
Il namespace `com.kjaza.tinymmo.party` fornisce una gestione semplice dei party:
- **Creazione e membership** – `PartyManager` tiene traccia dei party per owner e membri, consentendo di aggiungere/rimuovere giocatori e ottenere il party corrente di un utente.
- **Chat rapida con @** – `PartyChatListener` cattura i messaggi in chat che iniziano con `@` e li inoltra solo ai membri del party del mittente, mantenendo il formato colorato `[Party]`.
- **Comando /p** – Consente di inviare messaggi di party espliciti (`/p <testo>`), disponibile solo ai membri attivi; se il giocatore non è in un party riceve un messaggio d'errore dedicato.

L'integrazione nativa di party e skill rende più semplice espandere TinyHunt verso modalità RPG o cooperative senza modificare la logica core di coda e match.

## Configurazione passo dopo passo
1. **Compilazione o download** – Usa `mvn clean package` per generare `target/TinyHunt-0.1.0.jar` con Java 17+ (vedi [Build dal sorgente](#build-dal-sorgente)).
2. **Installazione** – Copia la JAR nella cartella `plugins/` di un server Paper 1.21.8 e avvia il server per generare `plugins/TinyHunt/config.yml` e registrare i comandi.
3. **Definizione aree** – Posizionati negli angoli opposti della lobby e dell'arena eseguendo rispettivamente `/tinyhunt lobby setpos1|setpos2` e `/tinyhunt arena setpos1|setpos2`; aggiungi almeno uno spawn con `/tinyhunt arena addspawn`.
4. **Verifica configurazione** – Il comando `/tinyhunt start` richiede lobby, arena e spawn validi; in caso contrario invia il messaggio `configuration-missing`. Usa `/tinyhunt reload` dopo modifiche manuali al config.
5. **Personalizzazioni avanzate** – Modifica timers, parametri Sudden Death, scale dei modelli, messaggi HUD e costi delle skill direttamente in `config.yml` senza riavviare il server (basta `/tinyhunt reload`).

## File di configurazione
`plugins/TinyHunt/config.yml` espone tutte le opzioni principali:

| Sezione | Chiavi | Descrizione |
| --- | --- | --- |
| `players` | `min`, `max` | Numero minimo/massimo di giocatori in coda per avviare la partita. |
| `timers` | `auto-start-seconds`, `hunter-selection-seconds`, `game-duration-seconds`, `runner-respawn-seconds`, `respawn-invulnerability-seconds` | Controlla i tempi di countdown, durata partita, conversioni e invulnerabilità post-conversione. |
| `sudden-death` | `enabled`, `start-seconds`, `reveal-interval-seconds`, `reveal-duration-seconds`, `hunter-speed-amplifier` | Attiva la modalità finale con ping periodici e buff Hunter. |
| `scales` | `runner`, `hunter` | Scala modello dei giocatori per ruoli diversi; richiede attributo `GENERIC_SCALE`. |
| `areas` | `lobby`, `arena` | Coordinate salvate dai comandi `/tinyhunt`; devono avere `pos1` e `pos2` per risultare valide. |
| `arena-spawns` | Lista di location | Teletrasporto iniziale e durante le conversioni; puoi definirne quanti ne vuoi. |
| `messages` | chiavi varie | Localizzazione completa dei messaggi inviati dal plugin, con supporto colori e placeholder. |
| `hud` | `scoreboard-title`, `bossbar-title`, `extra-match`, `extra-sudden-death` | Testi mostrati nell'HUD durante le partite. |
| `skills.costs` | `<id>.resource`, `<id>.amount` | Override opzionale dei costi per ciascuna skill registrata. |

## Messaggi personalizzabili e placeholder
Utilizza i placeholder disponibili nelle sezioni `messages` e `hud` per inserire dati dinamici:
- `%player%` – Nome del giocatore coinvolto (es. Hunter selezionato, Runner convertito).
- `%seconds%` – Valore numerico per countdown, respawn o durata ping.
- `%position%` – Posizione nella coda mostrata al join.
- `%time%` – Formattazione `mm:ss` del tempo rimanente usata nella bossbar.

Il metodo `TinyHuntPlugin#getMessage` sostituisce automaticamente questi placeholder, così puoi aggiungerne di nuovi nelle sezioni config senza scrivere codice.

## Comandi e permessi
| Comando | Permesso | Descrizione |
| --- | --- | --- |
| `/tinyhunt` | `tinyhunt.play` | Mostra l'help generale con tutte le sottocomandi disponibili. |
| `/tinyhunt join` | `tinyhunt.play` | Entra in coda; attiva il countdown se viene raggiunto il minimo partecipanti. |
| `/tinyhunt leave` | `tinyhunt.play` | Esce dalla coda o dalla partita; in countdown può annullare l'avvio automatico. |
| `/tinyhunt start` | `tinyhunt.admin` | Forza l'avvio di un match se configurazione e giocatori sono sufficienti. |
| `/tinyhunt stop` | `tinyhunt.admin` | Ferma immediatamente countdown o partita attiva, annunciandolo ai giocatori. |
| `/tinyhunt reload` | `tinyhunt.admin` | Ricarica `config.yml`, aggiornando timers, messaggi e parametri HUD al volo. |
| `/tinyhunt lobby setpos1|setpos2` | `tinyhunt.admin` | Salva gli angoli dell'area lobby nel config, usati per teletrasporti post-partita. |
| `/tinyhunt arena setpos1|setpos2|addspawn` | `tinyhunt.admin` | Definisce area arena e aggiunge spawn multipli per gestire teletrasporti casuali. |
| `/p <messaggio>` | *(nessun permesso dedicato)* | Invia un messaggio alla chat del party di appartenenza, se presente. |

Permessi definiti in `plugin.yml`:
- `tinyhunt.play` – Assegnato di default a tutti i giocatori, consente azioni di queueing.
- `tinyhunt.admin` – Assegnato di default agli operatori (`op`), abilita setup e gestione partite.

## Build dal sorgente
1. **Prerequisiti** – Java 17 (o superiore) e Maven installati localmente.
2. **Compilazione** – Esegui `mvn clean package` nella root del progetto per generare la JAR in `target/` con dipendenze incluse.
3. **Distribuzione** – Copia `target/TinyHunt-0.1.0.jar` nella cartella `plugins/` del server Paper e riavvia/ricarica il server.

## Suggerimenti operativi
- Usa `/tinyhunt reload` dopo ogni modifica manuale a `config.yml` per applicare i cambiamenti senza riavviare il server.
- Monitora la chat per messaggi `configuration-missing` o `not-enough-players`, indicano che manca qualcosa per avviare la partita.
- Se il server non supporta l'attributo `GENERIC_SCALE`, il plugin logga un warning e ignora la modifica delle dimensioni dei giocatori senza causare errori.
- Configura eventuali abilità personalizzate aggiungendo i relativi item nel tuo plugin companion o nello stesso `TinyHuntPlugin` durante `onEnable`, sfruttando `SkillManager#register` e i costi configurabili.

## Risorse aggiuntive
- [Documentazione di design TinyHunt](docs/design/tinyhunt-game-design.md) – Checklist completa delle feature pianificate (ruoli avanzati, power-up, roadmap).
- `AGENTS.md` – Appunti tecnici aggiornati sul progetto, inclusi dettagli sulle convenzioni di package e sulle ultime implementazioni.

Con questo README hai una panoramica completa del plugin, delle sue estensioni MMO e degli strumenti a disposizione per configurarlo e ampliarlo rapidamente.
