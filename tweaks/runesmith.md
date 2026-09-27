# Tweaks – Runesmith / Runenschmied (ersetzt Toolsmith)

Zwilling von Obsidian `Runesmith.md` + Reparaturstation-Teil von `Tweak-Werte.md`, kompakt. Fehlende Werte = 1. Bei Abweichung gilt Obsidian. Stand: 2026-09-27.
Vorlage, Upgradestation und Reparaturstation im Code seit dem Umbau 2026-09-27 (`runesmith/`, `trade/runesmith_*.json`). Unbinding Station gestrichen.

## Basic Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 4 | Upgradestation | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Reparaturstation | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
- Rang 0–3, 5–6 offen (im Spiel leere Zeile)

## Master Trades
- Bücher, immer Stufe I (Smaragd + Buch → verzaubertes Buch, alle Werte 1): Master 2 offen (leere Zeile), Master 3 Haltbarkeit I, Master 4 Reparatur (Mending, später teuer); Mod-Verzauberungen = weitere Trade-Datei
- Upgrade-Vorlage: **eine** Vorlage für alle Stufen, Master-Rang 1, 1 → 1 Smaragd, Output 1, Stock 1

### Upgrade am Schmiedetisch (Vorlage + Teil + 1 Block der Zielstufe, Vorlage wird verbraucht)
| Stufe | Block | Teile |
|---|---|---|
| Stein | Bruchstein | Holz → Stein |
| Kupfer | Kupferblock | Stein → Kupfer, Leder → Kupfer |
| Eisen | Eisenblock | Kupfer/Gold → Eisen, Kette → Eisen |
| Diamant | Diamantblock | Eisen → Diamant |
- Haltbarkeit + Polster werden prozentual übernommen

## Quests — `.../quest/runesmith_*.json`
- Dauer: Eisenbarren (vom Veteran übernommen, alle Werte 1); Easy + Hard offen

## Reparaturstation (Passive) — `runesmith/RepairStationBlockEntity.java`, `passive/RepairWork.java`
| Wert | Aktuell |
|---|---|
| Slots | 3 Input + 3 Output |
| Punkte pro Schritt | 25 |
| Schritte pro Tag, Rang 0–6 | 8, 12, 16, 20, 24, 28, 32 |
| Reichweite | 2 Blöcke |
| Schleifstein-Geräusch beim Füllen | alle 20 Ticks, Lautstärke 0,5 |
| Bonus-Haltbarkeit (Polster) | +10 % der Max-Haltbarkeit ab Passive-Rang 6, nimmt Schaden zuerst auf |
| Verteidigungs-Bonus | vorerst ausgeklammert (falls, dann nur Rüstung) |
