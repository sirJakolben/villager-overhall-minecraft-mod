# Tweaks – allgemein (alle Berufe)

Zwilling von Obsidian `Tweak-Werte.md` (allgemeiner Teil), kompakt. Beide werden gemeinsam geändert; bei Abweichung gilt Obsidian.
Berufs-Werte: [librarian.md](librarian.md) · [mason.md](mason.md), [veteran.md](veteran.md) · geplant: [runesmith.md](runesmith.md), [salvager.md](salvager.md), [cartographer.md](cartographer.md), [fisherman.md](fisherman.md), [leatherworker.md](leatherworker.md). Stand: 2026-09-28. Code-Stellen: Kern `core/…`, Berufe `trade-rework/…` (siehe README).

## Level & XP — `progression/ProgressionService.java`
| Wert | Aktuell |
|---|---|
| Maximallevel | keins (alle Berufe, seit 2026-09-28) |
| XP für Level L → L+1 | 400 + 9 × L² (400 … 3649), ab Level 20 konstant 3649 |
| Punkte-Puffer (Leveln stoppt) | 5 |
| Level pro Titelstufe | 5 |

## Sektionen (Upgrade-Kosten je Sektion)
| Sektion | Kosten je Rang | Max-Rang | alles frei ab | Code |
|---|---|---|---|---|
| Quests (Kern) | 1,1,1,2,2,2,3,3,5 | 9 | Rang 6 | `section/CoreSections.java` |
| Trades (Kern) | 1,1,1,2,2,2,3,3,5 | 9 | Rang 6 | `section/CoreSections.java` |
| Quests (Extension) | 1,1,1,2,2,2,3,3,5 | 9 | Rang 6 | `TradeReworkSections.java` |
| Masteries (Extension) | 2,2,2,3,3,3,5 | 7 | Rang 4 | `TradeReworkSections.java` |
| Passive (Extension) | 3,3,3,3,3,5 | 6 | Rang 3 | `TradeReworkSections.java` |
- Bis „alles frei“: Werte auf Base · danach linear bis Max (Preis als Vanilla-Rabatt) · Workstations auf Trades-Rang 4
- Berufe mit Vanilla-Trades (`vanilla/`): statt der Tabelle eine gemeinsame Kostenlinie 1 → 5 über alle Ränge, je Sektion letzter Freischalt-Rang + 4 Preis-Stufen

## Arbeit — `work/VillagerWorkScan.java`
| Wert | Aktuell |
|---|---|
| Scan-Intervall | 100 Ticks (+ Leisten-Zwischenbuchung nach 50 Ticks: halbe Punkte + eigene Arbeit der Sektionen) |
| Arbeits-XP pro Scan | 10 × (1 + 2 × Glück) → 10 … 30 |
| Arbeits-Radius | 10 Blöcke (wie Vanilla-Arbeitsbereich) |
| Glocke / Begleiter-Radius | 8 / 8 Blöcke |

## Stationen — `station/`
| Wert | Aktuell | Code |
|---|---|---|
| Suche + Vorrang Arbeitslose | 48 Blöcke | `UnemployedPriority.RADIUS` |
| Ankunft an Station | 1,73 Blöcke | `WorkAtStation.ARRIVED_DISTANCE` |
| Arbeitsgeräusch | 300–600 Ticks | `WorkAtStation.WORK_SOUND_INTERVAL` |

## Glücklichkeit — `happiness/HappinessCalculator.java`
| Element | Anteil | Timer |
|---|---|---|
| Bett | 30 % | 2 Tage |
| Dorfzentrum (Glocke) | 20 % | 2 Tage |
| Villager-Kontakt | 20 + 10 + 10 % | je 3 Tage |
| Begleiter (Tag `happiness_companions`) | 10 + 10 % | je 3 Tage |
- Je Element auf 5 % aufgerundet, Summe max. 100 % · Angst → 0 %, Held des Dorfes → 100 % (vorübergehend)

## Handel & Restock — `work/RestockService.java`
| Wert | Aktuell |
|---|---|
| Startbestand neuer Trade | 50 % (aufgerundet) |
| Nachschub pro volle Leiste | +25 % vom Max, mind. +1 |
| Arbeits-Checks pro vollem Tag | 60 |
| Leistengröße (jede Listen-Sektion, eine Leiste pro Sektion) | 1500 Punkte |
| Punkte pro Check | 50 (0 % Glück) … 100 (100 %) |
| Freigabe voller Leiste nach Trade | 40 Ticks |
| Passiv-Leiste nach Passiv-Rang 0–6 | Librarian 6000 · 6000 · 6000 · 6000 · 3000 · 2000 · 1500 (= 1,1,1,1,2,3,4 Upgrades/Tag, `BookUpgradeWork`) · Mason 750 … 187 (= 8 … 32 Batches/Tag, `CrushingWork`) (Extension, `passive/PassiveLogic.meterPoints`) |
- Fokus (`station/StationFocus.choose`, zwischen Sektionen): fällige Arbeit (Passive) → größte Lücke → leerste Leiste → höchster Rang · Sektionen an derselben Station füllen ihre Leisten gemeinsam · Ruf-Rabatt/Nachfrage aus · Held-Rabatt Vanilla

## Quests (Extension) — `quest/QuestSlots.java`, `quest/QuestLogic.java`
| Wert | Aktuell |
|---|---|
| 2. Easy-Platz / Hard-Platz / Dauer-Quest ab Rang | 3 / 5 / 6 |
| Tageslimit nach Rang 0–9 | 1,2,3,3,4,4,4,5,6,7 |
| Pause bis nächste Quest | 40 Ticks |
| Reroll-Wartezeit | 5 Tage (Rang 0) → 1 Tag (Max-Rang) |

## Claim & Handelsblock — `station/`
| Wert | Aktuell |
|---|---|
| Claim-Zeitfenster | 600 Ticks |
| Suche alter Besitzer | 64 Blöcke |
| Anlocken: Reichweite / Abstand / Tempo | 10 / 2,5 Blöcke / 0,6 |
| Handelsblock: Tempo | 0,6 |
| Besuch: Wartezeit / Abbruch / angekommen ab | 100 / 1200 Ticks / 2,5 Blöcke |
| Handelsblock max. Abstand zum Arbeitsplatz | 48 Blöcke |

## Debug — `debug/DebugCommands.java`
| Wert | Aktuell |
|---|---|
| Reichweite / Ziel-Toleranz / Ziel-Kegel | 20 Blöcke / 0,5 / 6° |
