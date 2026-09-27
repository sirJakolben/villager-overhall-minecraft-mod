# Tweaks – Cartographer / Kartograph (teilweise umgesetzt)

Zwilling von Obsidian `Cartographer.md`, kompakt. Fehlende Werte = 1. Bei Abweichung gilt Obsidian. Stand: 2026-09-27.

## Basic Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 5 | Abenteuerkarte (neues Item) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 6 | Herausforderungskarte (neues Item) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
- 4: Erkundungsstation (Master-Workstation), 1 → 1 Smaragd; Rang 0–3 offen (im Spiel leere Zeile)
- Im Code seit 2026-09-27 (`trade/cartographer_*.json`); Dorfkarten über die Erkundungsstation

## Master Trades – Dorfkarten (Smaragde + 1 Abenteuerkarte)
| Rang | Karte zu |
|---|---|
| 0 | Ebenen-Dorf (ohne Mastery-Aufwertung) |
| 1 | Wüsten-Dorf (Vorschlag) |
| 2 | Savannen-Dorf (Vorschlag) |
| 3 | Taiga-Dorf (Vorschlag) |
| 4 | Schnee-Dorf (Vorschlag) |
- alle Werte 1

## Abnehmer der Karten
- Abenteuerkarte: Veteran (Außenposten, Verlies), Mason (Geode), Salvager (Kupfer-/Eisenader), Dorfkarten hier
- Herausforderungskarte: Veteran (Trial Chamber, Anwesen, Monument, Antike Stadt)
