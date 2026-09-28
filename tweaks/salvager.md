# Tweaks – Salvager / Verwerter (teilweise umgesetzt, ersetzt Armorer)

Zwilling von Obsidian `Salvager.md`, kompakt. Im Code: Basic 0, 1, 4, 5, 6 und die Schmelzstation; fehlende Werte = 1. Bei Abweichung gilt Obsidian. Stand: 2026-09-27.

## Basic Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 0 | Eisenkette (`iron_chain`) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 1 | Eisengitter (`iron_bars`) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Veredelungsstation (Refinement, Placeholder) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Schmelzstation (Smelting, Placeholder) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 5 | Kupferader-Karte | 1 → 1 Smaragd | 1 Abenteuerkarte  | 1 → 1 | 1 → 1 |
| 6 | Eisenader-Karte | 1 → 1 Smaragd | 1 Abenteuerkarte  | 1 → 1 | 1 → 1 |
- Rang 2–3 offen; Erzader-Karten im Code (Ader-Rauschen aus dem Weltseed, 32 Chunks); Basic 0, 1, 4, 5, 6 umgesetzt; 2–3 und Master 1–4 im Spiel leere Zeilen

## Master Trades – Refinement Station (Master-Workstation)
- Block + Metall → Grund-Variante, viele Varianten per Steinmetzblock oder Werkbank; ohne Smaragde (Metall = Bezahlung, 1. Slot; Block im 2. Slot)
| Rang (Vorschlag) | Block (2. Slot) | Metall (1. Slot) | Output |
|---|---|---|---|
| 1 | Glas | Eisen | eisengerahmtes Glas |
| 2 | Redstone-Block | Eisen | eisengerahmter Redstone |
| 3 | Lapisblock | Gold | vergoldeter Lapis |
| 4 | Schwarzstein | Gold | Vergoldeter Schwarzstein (Vanilla) |
- Lapis-Texturen: Mod Abyssal Decor (starrysock), Lizenz CC0; Erlaubnis angefragt, Credit geplant

## Schmelzstation (Passive) — `salvager/SmeltingStationBlockEntity.java`, `passive/SmeltingWork.java`, `.../salvage/*.json`
| Wert | Aktuell |
|---|---|
| Slots | 3 Input + 3 Output (Fenster wie Brechstation) |
| Angenommen | Eisen, Gold, Kupfer, Kette, Netherite – Rüstung + Werkzeuge/Waffen inkl. Speer; **kein Diamant** |
| Teile pro Tag, Rang 0–6 | 8, 12, 16, 20, 24, 28, 32 |
| Rang-Anteil, Rang 0–6 | 25, 35, 45, 55, 70, 85, 100 % (Vorschlag) |
| Ausbeute | Rezeptmenge × Rang-Anteil × Zustand, abgerundet, min. 1 |
| Rezeptmengen | Helm 5, Brust 8, Hose 7, Stiefel 4, Schwert 2, Spitzhacke 3, Axt 3, Schaufel 1, Hacke 2, Speer 1 |
| Kette | 2 × Rezeptmenge als Eisen-Nuggets (Brustpanzer 16) |
| Netherite | Diamant-Teil (behält alles, Haltbarkeit anteilig; bleibt im Input-Slot) + Scraps in den Output, volle Menge 4 |
| Geräusch beim Füllen | Hochofen-Knistern alle 20 Ticks, Lautstärke 0,6 |
| Licht beim Arbeiten | 13, sonst 0 (`SmeltingStationBlock.WORKING_LIGHT`); Metall im Tiegel leuchtet voll hell |
| Lava-Blasen beim Arbeiten | 1 von 5 Animations-Ticks (`SmeltingStationBlock.BUBBLE_CHANCE`), Rate wie Lagerfeuer; je Blase Partikel + Lava-Plopp (Lautstärke 0,2–0,4) |
| Lava-Blasen: Flugweite / Größe | 40 % der Vanilla-Weite (`MoltenBubbleParticle.FLIGHT_SHARE`); Größe 0,2–1,6 statt 0,2–2,2 (`MAX_SIZE_FACTOR`) |
