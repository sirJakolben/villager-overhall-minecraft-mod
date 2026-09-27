# Tweaks – Mason

Zwilling der Mason-Werte aus Obsidian (`Mason.md` Wertetabellen + Brechstation-Teil von `Tweak-Werte.md`), kompakt. Bei Abweichung gilt Obsidian.
Base = Rang 0 der Gruppe, Max = Max-Rang, dazwischen linear. Fehlt ein Wert → 1. Stand: 2026-09-27.

## Basic Trades — `data/vo_trade_rework/villageroverhaul/trade/mason_*.json`
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 0 | Sandstein | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 1 | Brauner Sandstein | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 2 | Roter Sandstein | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 3 | Steinchen (`rock_pile`) | 1 → 1 Smaragd | 1 Bruchstein | 1 → 1 | 1 → 1 |
| 3 | Wegsteine (`rock_path`) | 1 → 1 Smaragd | 1 Kies | 1 → 1 | 1 → 1 |
| 4 | Brechstation | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Metamorphstation | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 5 | Dorfbewohner-Statue | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 6 | Geoden-Karte | 1 → 1 Smaragd | 1 Abenteuerkarte | 1 → 1 | 1 → 1 |

## Master Trades (ohne Smaragde, Katalysator im 1. Slot, Gestein im 2. Slot)
| Rang | Gestein (2. Slot) | Katalysator (1. Slot, skaliert) | Output | Werte |
|---|---|---|---|---|
| 1 | Bruchtiefenschiefer | 1 Kohle | Schwarzstein | alle 1 |
| 2 | Andesit | 1 Kohle | Tuff | alle 1 |
| 3 | Diorit | 1 Quarz | Calcit | alle 1 |
| 4 | Sandstein | 1 Quarz | Quarzblock | alle 1 (Vorschlag: 2 Quarz → 1 Quarz auf Max) |

## Quests — `.../quest/mason_*.json` (alle Werte 1)
- Easy (Oberwelt): gemeißelte Blöcke, Ziegel, andere Varianten, polierte Blöcke, Kohle, Stein
- Hard (Nether, Prismarin, Endstein, Purpur): gemeißelte Nether-Blöcke, Nether-Ziegel, polierte Nether-Blöcke, Quarz, Prismarin, Endstein, Purpur
- Dauer: glatter Stein

## Brechstation (Passive) — `mason/CrushingStationBlockEntity.java`, `passive/CrushingWork.java`
| Wert | Aktuell |
|---|---|
| Slots | 3 Input + 3 Output |
| Batch-Größe | 4 Ergebnisse, nur eine Sorte pro Batch |
| Batches pro Tag, Rang 0–6 | 8, 12, 16, 20, 24, 28, 32 |
| Reichweite | 2 Blöcke |
| Redstone-Puls | 2 Ticks |
| Tuff-Abbaugeräusch beim Füllen / Nachlauf | alle 4 Ticks (Vanilla-Abbautakt), Lautstärke 0,75 / 20 Ticks |
| Verhältnis | 1 : 1 (Steinmetz-Varianten im Steinmetz-Verhältnis) |
- Regeln: Steinmetz-Rezepte rückwärts (automatisch) + `data/vo_trade_rework/vo_trade_rework/crushing/*.json` (crushing, cobbled_forms, smooth_blocks, concrete, glazed_terracotta); jede Steinart → Bruch-Form samt Varianten
