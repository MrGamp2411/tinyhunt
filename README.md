# TinyHunt

TinyHunt è un plugin Paper per Minecraft 1.21 che introduce un minigioco Hunter vs Runner completamente gestito via comandi e configurazione. I giocatori si mettono in coda, vengono teletrasportati in arena e un hunter viene scelto casualmente dopo pochi secondi: se tutti i runner vengono convertiti gli hunter vincono, altrimenti vincono i runner resistendo fino allo scadere del tempo di gioco.

## Funzionalità principali
- Coda automatica con avvio del match quando viene raggiunto il numero minimo di giocatori configurato.
- Gestione completa delle fasi del gioco (attesa, conto alla rovescia, partita, chiusura) con messaggi personalizzabili.
- Configurazione della lobby, dell'arena e degli spawn direttamente in gioco tramite `/tinyhunt`.
- Selezione casuale dell'hunter, conversione differita dei runner colpiti con respawn temporizzato e breve invulnerabilità.
- Modalità Sudden Death automatica negli ultimi minuti con ping periodico dei runner e potenziamento degli hunter.
- HUD dedicato con bossbar del tempo rimanente e scoreboard live (runner/hunter attivi, prossimo ping).
- Possibilità di ricaricare la configurazione senza riavviare il server.

Per una lista completa delle feature pianificate (inclusi ruoli avanzati, abilità, power-up, statistiche e strumenti amministrativi) fai riferimento alla [TinyHunt Game Design Specification](docs/design/tinyhunt-game-design.md). Il documento funge da checklist ufficiale: ogni sezione corrisponde a un blocco di sviluppo e indica anche le dipendenze tecniche suggerite.

## Requisiti
- Server Paper o compatibile con API 1.21 (testato fino alla build 1.21.8).
- Permessi Bukkit per gestire i comandi (`tinyhunt.play`, `tinyhunt.admin`).

## Installazione rapida
1. Compila il progetto con `mvn package` oppure scarica la build già fornita.
2. Copia `target/TinyHunt-0.1.0.jar` nella cartella `plugins/` del server Paper.
3. Avvia il server per generare `plugins/TinyHunt/config.yml` e caricare il plugin.

## Configurazione in gioco
1. **Definisci la lobby**: posizionati sul primo angolo dell'area lobby e usa `/tinyhunt lobby setpos1`, poi ripeti con `/tinyhunt lobby setpos2` per l'angolo opposto.
2. **Definisci l'arena**: usa `/tinyhunt arena setpos1` e `/tinyhunt arena setpos2` per delimitare l'area di gioco.
3. **Aggiungi gli spawn dell'arena**: posizionati nei punti di spawn desiderati e ripeti `/tinyhunt arena addspawn` per ciascuno.
4. (Opzionale) Modifica `config.yml` per personalizzare numero di giocatori, timer e messaggi.

Perché un match possa iniziare sono necessari:
- Lobby e arena configurate con due posizioni ciascuna.
- Almeno uno spawn in arena.
- Numero di giocatori in coda maggiore o uguale a `players.min`.

## Comandi
| Comando | Permesso | Descrizione |
| --- | --- | --- |
| `/tinyhunt join` | `tinyhunt.play` | Entra nella coda del minigioco.
| `/tinyhunt leave` | `tinyhunt.play` | Esce dalla coda o dalla partita in corso.
| `/tinyhunt start` | `tinyhunt.admin` | Avvia forzatamente una partita se la configurazione è completa.
| `/tinyhunt stop` | `tinyhunt.admin` | Arresta la partita in corso o il conto alla rovescia.
| `/tinyhunt reload` | `tinyhunt.admin` | Ricarica `config.yml` e le impostazioni salvate.
| `/tinyhunt lobby setpos1|setpos2` | `tinyhunt.admin` | Salva gli angoli della lobby nel file di configurazione.
| `/tinyhunt arena setpos1|setpos2|addspawn` | `tinyhunt.admin` | Salva gli angoli e gli spawn dell'arena.

## Permessi
- `tinyhunt.play`: accesso ai comandi di coda, assegnato di default a tutti i giocatori.
- `tinyhunt.admin`: accesso ai comandi di gestione, assegnato di default agli operatori.

## Parametri configurabili
All'interno di `config.yml` puoi regolare:
- `players.min` / `players.max`: minimo e massimo di giocatori ammessi.
- `timers.auto-start-seconds`: tempo di attesa prima dell'avvio automatico dopo aver raggiunto il minimo di giocatori.
- `timers.hunter-selection-seconds`: ritardo prima della scelta casuale dell'hunter.
- `timers.game-duration-seconds`: durata totale del match.
- `timers.runner-respawn-seconds`: quanti secondi passano prima che un runner eliminato torni come hunter.
- `timers.respawn-invulnerability-seconds`: invulnerabilità concessa al nuovo hunter dopo il respawn.
- `scales.runner` / `scales.hunter`: scala del modello dei giocatori runner/hunter (richiede server con attributo `GENERIC_SCALE`).
- Sezione `sudden-death.*`: impostazioni per i ping finali (momento di attivazione, intervallo, durata reveal, speed degli hunter).
- Sezione `hud.*`: testi mostrati in bossbar/scoreboard durante la partita.
- Sezioni `messages.*`: testi mostrati al giocatore, con supporto ai codici colore `&` e placeholder come `%player%` o `%seconds%`.

## Build dal sorgente
1. Assicurati di avere Java 17+ e Maven installati.
2. Esegui `mvn clean package` nella root del progetto.
3. Recupera il file generato in `target/TinyHunt-0.1.0.jar` e distribuiscilo come indicato sopra.

## Supporto
Durante il gioco puoi monitorare lo stato attuale con i messaggi automatici inviati ai partecipanti. In caso di problemi di configurazione il plugin avviserà i giocatori e impedirà l'avvio della partita finché lobby, arena e spawn non saranno definiti.
