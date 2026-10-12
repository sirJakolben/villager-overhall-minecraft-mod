# Tweaks – Goldsmith / Goldschmied (teilweise umgesetzt, ersetzt Armorer; bis 2026-09-29 „Salvager / Verwerter“)

Zwilling von Obsidian `Goldsmith.md`, kompakt. Im Code: Basic 0, 1, 4, 5, 6, Master 1–4 und die Verwertungsstation; fehlende Werte = 1. Bei Abweichung gilt Obsidian. Stand: 2026-09-29.

## Basic Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 0 | Vergoldete Eisenkette (`gilded_iron_chain`) | 1 → 1 Smaragd | 1 Eisenkette | 1 → 1 | 1 → 1 |
| 1 | Vergoldetes Eisengitter (`gilded_iron_bars`) | 1 → 1 Smaragd | 1 Eisengitter | 1 → 1 | 1 → 1 |
| 4 | Vergoldungsstation (Gilding, Placeholder) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Verwertungsstation (Salvage Station) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 5 | Kupferader-Karte | 1 → 1 Smaragd | 1 Abenteuerkarte  | 1 → 1 | 1 → 1 |
| 6 | Eisenader-Karte | 1 → 1 Smaragd | 1 Abenteuerkarte  | 1 → 1 | 1 → 1 |
- Rang 2–3 offen; Erzader-Karten im Code (Ader-Rauschen aus dem Weltseed, 32 Chunks); Basic 0, 1, 4, 5, 6 umgesetzt; 2–3 im Spiel leere Zeilen

## Master Trades – Gilding Station (Master-Workstation) — `exchange/goldsmith_*.json`
- Umgesetzt 2026-09-29: Block + Metall → Grund-Variante; ohne Smaragde (Metall = Bezahlung, 1. Slot; Block im 2. Slot); alle Mengen 1 (Werte fehlen)
| Rang | Block (2. Slot) | Metall (1. Slot) | Output | Stock |
|---|---|---|---|---|
| 1 | Glas | 1 → 1 Eisenbarren | Verstärktes Glas (`reinforced_glass`) | 1 → 1 |
| 2 | Redstone-Block | 1 → 1 Eisenbarren | Eisengerahmter Redstone (`iron_framed_redstone`) | 1 → 1 |
| 3 | Lapisblock | 1 → 1 Goldbarren | Goldgerahmter Lapis (`gold_framed_lapis`) | 1 → 1 |
| 4 | Schwarzstein | 1 → 1 Goldbarren | Vergoldeter Schwarzstein (Vanilla) | 1 → 1 |
- Steinmetz, je 1 : 1 (Stufe 1 : 2): eisengerahmter Redstone → eisengerahmte Redstone-Treppe (L-Textur, Modelle wie die Lapis-Treppe), -Stufe ×2, eisenverzierter Redstone (Ornated Redstone, setzt sich wie ein Kolben), eisenverzierte Redstone-Säule (Ornated Redstone Pillar); nur der Grundblock gibt Signal (entschieden 2026-09-30), Texturen selbst gezeichnet (`assets/Goldsmith/redstone variants`); goldgerahmter Lapis → goldgerahmte Stufe ×2 / Treppe, goldverzierter Lapis, goldverzierte Lapissäule; für Glas, Redstone und Schwarzstein noch keine (Grafiken fehlen)
- Verstärktes Glas: durchsichtig, droppt sich selbst (Spitzhacke), Härte 1,5, Explosionsfestigkeit 6 (wie Stein); Rahmen verbindet sich mit angrenzendem verstärktem Glas (Connected Textures: jede Seite wählt eine von 16 Texturen nach den 4 Nachbarn in ihrer Ebene, `ReinforcedGlassBlock`, eigene Texturen in `assets/Goldsmith/reinforced_glass`)
- Ornated Redstone als Deko-Leitung (2026-09-30): Vorderseite top, Rückseite bottom wie beim Kolben-Setzen, beide drehen sich zum Ornated-Redstone-Nachbarn in ihrer Ebene – top-Leitung unten→links, freies Ende nach oben/links; bottom-Leitung rechts→oben, freies Ende nach unten/rechts; ohne Nachbar top wie mit Nachbar links, bottom wie mit Nachbar rechts; bei mehreren Nachbarn zählt der erste von links, rechts, oben, unten (`OrnatedRedstoneBlock`)
- Eisengerahmter Redstone: gibt Signal 15 wie der Redstone-Block, Härte 5 / 6 wie der Redstone-Block; Varianten gleiche Härte, aber ohne Signal
- Gold-Lapis-Blöcke: wie Stein (Härte 1,5 / 6), Kartenfarbe Lapis; goldverzierter Lapis (`ornated_lapis`) wird gesetzt wie ein Kolben (Vorderseite zeigt zum Spieler, alle 6 Richtungen; Rückseite eigene Textur) – beim Setzen nach oben/unten umgekehrt: Blick nach unten → Rückseite oben (2026-09-30), goldverzierte Lapissäule drehbar wie Holzstämme; goldgerahmte Treppe mit eigenen Modellen (gerade + Außenecke selbst in Blockbench gebaut, Innenecke im selben Stil ergänzt): L-förmige Seitentextur `gold_framed_lapis_stair` auf den Flächen mit Stufenprofil, sonst Goldrahmen; Textur dreht sich mit der Treppe (kein uvlock); die schlichten Lapis-Varianten (Abyssal Decors Sternstein) wurden 2026-09-29 entfernt
- Texturen: Gold-Lapis-Blöcke selbst gezeichnet (`assets/Goldsmith/lapis variants`); Eisenrahmen, Seabrass-Kette/-Gitter aus Abyssal Decor (starrysock), CC0, Erlaubnis erteilt 2026-09-29, Credit in README + mods.toml

## Verwertungsstation (Passive) — `goldsmith/SalvageStationBlockEntity.java`, `passive/SalvageWork.java`, `.../salvage/*.json`
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
| Licht beim Arbeiten | 13, sonst 0 (`SalvageStationBlock.WORKING_LIGHT`); Metall im Tiegel leuchtet voll hell |
| Lava-Blasen beim Arbeiten | 1 von 5 Animations-Ticks (`SalvageStationBlock.BUBBLE_CHANCE`), Rate wie Lagerfeuer; je Blase Partikel + Lava-Plopp (Lautstärke 0,2–0,4) |
| Lava-Blasen: Flugweite / Größe | 40 % der Vanilla-Weite (`MoltenBubbleParticle.FLIGHT_SHARE`); Größe 0,2–1,6 statt 0,2–2,2 (`MAX_SIZE_FACTOR`) |
