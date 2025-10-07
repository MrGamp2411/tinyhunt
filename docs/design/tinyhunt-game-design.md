# TinyHunt Game Design Specification

Questo documento riassume e dettaglia tutte le meccaniche previste per la versione completa di TinyHunt. Ogni sezione può essere utilizzata come checklist durante lo sviluppo: quando un blocco viene consegnato, spunta le voci o collega il commit relativo.

## 1. Core di gioco
- **Ruoli**
  - Fuggitivi (miniaturizzati) con modello/hitbox ridotta.
  - Cacciatori nelle dimensioni normali.
- **Fasi**
  - Lobby di attesa con coda e map vote.
  - Pre-partita: countdown con eventuale shrink progressivo dell'area di gioco.
  - Match attivo con obiettivi e timer globale.
  - Fine partita con riepilogo MVP e ricompense.
- **Condizioni di vittoria**
  - Cacciatori: eliminano o "convertono" tutti i fuggitivi.
  - Fuggitivi: fanno scadere il tempo o completano obiettivi opzionali.
- **Respawn / Conversione**
  - Un fuggitivo colpito respawna come cacciatore dopo X secondi.
  - Breve finestra di invulnerabilità al rientro.
- **Timer & Sudden Death**
  - Negli ultimi N minuti: visibilità ridotta dei fuggitivi, ping periodici o potenziamenti ai cacciatori.
- **Collisioni / Hitbox**
  - I fuggitivi devono poter attraversare corridoi 1×1.
  - Hitbox, reach e velocità calibrate per entrambi i ruoli.
- **Danni**
  - Un singolo colpo è sufficiente a convertire un fuggitivo (configurabile).

## 2. Meccaniche speciali
- **Abilità Fuggitivi (con cooldown server-side)**
  - Scatto breve.
  - Rampino / ender pearl limitata.
  - Smoke / cecità locale.
  - Nascondiglio temporaneo.
- **Abilità Cacciatori**
  - Ping del fuggitivo più vicino.
  - Bussola / compass tracking.
  - Reveal periodici dell'area di un fuggitivo.
- **Power-up sulla mappa**
  - Invisibilità 5s.
  - Salto potenziato.
  - Stun 1s.
  - Cure.
  - Decoy (esca).
- **Progressione leggera**
  - Livelli account → sblocchi cosmetici (no pay-to-win).
- **Cosmetici**
  - Trail, kill-effect, gadget lobby, particelle.
  - Nessun impatto sul gameplay.
- **Obiettivi opzionali**
  - Micro-missioni in partita (es. raccogli 3 token per un boost temporaneo).

## 3. Mappe & rotazioni
- Pool mappe con tag (piccola/media/grande, indoor/outdoor, verticalità).
- Zone vietate / safe spot bloccati.
- Spawn dei power-up con probabilità e cooldown configurabili.
- Rotazione automatica + veto / voto mappa in lobby.

## 4. UX & onboarding
- Tutorial rapido (book o GUI) per spiegare ruoli e obiettivi.
- Scoreboard chiaro con tempo, giocatori vivi, numero cacciatori, timer prossimo reveal.
- Bossbar / actionbar per countdown ed eventi.
- Title / subtitle per transizioni di fase.
- Hologram in lobby con statistiche globali, top player, mappa in votazione.
- Menu GUI per join/leave, guardaroba cosmetici, statistiche personali.

## 5. Comandi & permessi
- Giocatori: `/tinyhunt join`, `/tinyhunt leave`, `/tinyhunt stats [player]`, `/tinyhunt top`, `/tinyhunt vote`, `/tinyhunt map`.
- Admin: `/tinyhunt start`, `/tinyhunt stop`, `/tinyhunt setlobby`, `/tinyhunt addmap <nome>`, `/tinyhunt setspawn <team>`.
- Permessi: `tinyhunt.play`, `tinyhunt.vote`, `tinyhunt.cosmetics.*`, `tinyhunt.admin.*`.

## 6. Dati & statistiche
- **Per partita**: durata, mappa, vincitore, tempo medio di cattura, kill per cacciatore, power-up usati.
- **Per giocatore**: partite, win rate, K/D come cacciatore, tempo medio di sopravvivenza, abilità più usate.
- **Leaderboard**: Elo semplice o punteggio ponderato (win + sopravvivenza + contributi).

## 7. Anti-abuso & qualità server
- Anti-camping: penalità o marcatori se un ruolo resta troppo fermo in un raggio ristretto.
- Anti-ghosting: nascondi nickname ai non partecipanti, spettatori invisibili, chat separata per team.
- AFK detector con auto-kick in lobby e sostituzione in partita.
- Compatibilità anti-cheat: whitelist per abilà e knockback personalizzati.
- Performance: budget di tick per effetti, particelle limitate, pooling degli oggetti e task asincroni.
- Rientro rapido: se un giocatore si disconnette può tornare entro N secondi mantenendo il ruolo.

## 8. Accessibilità & localizzazione
- Traduzioni via file `messages_xx.yml` (IT/EN/DE) con placeholder chiari.
- Supporto daltonismo: icone oltre ai colori; suoni distinti per ping/reveal.
- Cue audio per eventi chiave.

## 9. Monetizzazione (opzionale, solo cosmetica)
- Rank cosmetici con più slot trail, emote, animazioni di vittoria.
- Battle pass cosmetico stagionale (sfide solo estetiche).
- Store in-game con valuta soft guadagnata giocando.

## 10. Admin tools & integrazioni
- PlaceholderAPI per scoreboard/tablist.
- Vault per permessi/economia.
- ProtocolLib per effetti lato client.
- Supporto BungeeCord / Velocity con ingresso da hub e più istanze in parallelo.
- Logging con file rotanti e livelli DEBUG/INFO/WARN.
- Comandi rapidi per forzare mappa, settare spawn, ricaricare config live.

## 11. Scelta tecnica per il ridimensionamento dei fuggitivi
- Opzione `attribute` (Minecraft 1.20.5+ / 1.21.x): usa l'attributo `generic.scale`.
- Opzione `disguise`: integra LibsDisguises per trasformare i fuggitivi in baby-mob.
- Opzione `visual`: gestita via ProtocolLib con sola modifica estetica.
- Config `scaleMode` che consente di cambiare approccio senza refactor.

## 12. Test & QA
- Casi base: 1vN, mappe piccole/grandi, pochi giocatori.
- Verifica che reveal/ping rispettino il tick-rate anche sotto lag.
- Cooldown abilità gestiti lato server (nessuna fiducia al client).
- Compatibilità con anti-cheat, plugin scoreboard e proxy.
- Stress test con bot o istanze multiple.

## 13. Roadmap suggerita
1. **MVP**: lobby, ruoli, shrinking/disguise, timer, condizioni di vittoria, almeno 2 mappe.
2. **Quality of Life**: scoreboard, bossbar, map vote, statistiche base, GUI cosmetici.
3. **Contenuti**: 6–10 power-up, 5–8 mappe, leaderboard web.
4. **Stagioni & Progressi**: progressione cosmetica e API per overlay.

## 14. Tracciamento implementazione
- Aggiungi qui sotto una tabella con stato (TODO / In corso / Completo) per ogni feature mano a mano che viene sviluppata.

| Feature | Stato | Note |
| --- | --- | --- |
| Lobby & queue | TODO | |
| Map vote | TODO | |
| Ability dash | TODO | |
| Power-up invisibilità | TODO | |
| ... | TODO | |

Aggiorna la tabella con i riferimenti ai commit e mantieni il documento sincronizzato con lo stato reale del plugin.
