# Tweaks – Cartographer / Kartograph (teilweise umgesetzt)

Zwilling von Obsidian `Cartographer.md`, kompakt. Fehlende Werte = 1. Bei Abweichung gilt Obsidian. Stand: 2026-10-10.

## Basic Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 4 | Kartenstation (Master-Workstation) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Abenteuerstation (Passive-Station) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
- Rang 0–3 und 5–6 offen (im Spiel leere Zeile); Dateien `exchange/cartographer_*.json`
- Die Karten-Basic-Trades (Abenteuer- und Herausforderungskarte, Rang 5/6) sind seit 2026-10-10 Masteries

## Master Trades (Kartenstation) – 1 Smaragd → leere Karte
| Rang | Karte |
|---|---|
| 1 | Abenteuerkarte (Eingabe der Abenteuerstation, Zahlung für Mason-/Goldsmith-Karten) |
| 2 | Herausforderungskarte (Eingabe der Challenge Station des Veteran) |
| 3 | Höhlenkarte (leer, wird per Rechtsklick gefüllt) |
| 4 | Netherkarte (leer) |
- Die Dorfkarten-Masteries sind gestrichen (2026-10-10)

## Passive – Abenteuerstation (`mapping/*`, `passive/MappingWork.java`)
| Wert | Aktuell |
|---|---|
| Karte aus | 1 Abenteuerkarte (Station nimmt nur diese) |
| Dauer je Karte (ganze Arbeitstage) nach Passive-Rang 0–6 | 3, 3, 2, 2, 2, 1, 1 (`TradeReworkSections`, `daysPerStep`) |
| Ziel | zufällig, nach Gewicht aus dem Abenteuer-Pool (unten), erst beim Fertigwerden gewürfelt |
| Suche | Radius 100 Chunks, nur Strukturen ohne Karte, bis zu 3 Würfe je Versuch (`MapTargets.MAX_SEARCHES`) |
| Nichts gefunden | Meter bleibt voll, nächster Versuch nach 1200 Ticks (`MapStationBlockEntity.RETRY_TICKS`) |
| Name der Karte | „Gezeichnete Abenteuerkarte“, Ziel-X ohne Hinweis auf die Struktur |

## Abenteuer-Pool – `data/vo_trade_rework/vo_trade_rework/map_pool/adventure.json`
| Gewicht | Struktur |
|---|---|
| 10 | Dörfer (Tag `minecraft:village`) |
| 6 | Außenposten, Ruinenportale |
| 5 | Wüstentempel, Dschungeltempel, Schiffswracks, Ozeanruinen |
| 4 | Iglu, Sumpfhütte |
| 3 | Vergrabener Schatz, Pfadruinen |
- Weitere Dateien (auch von anderen Mods) mit `"pool": "adventure"` ergänzen den Pool. Eine Struktur ohne Eintrag in irgendeiner Datei kommt mit Gewicht 1 dazu: Untergrund- und Festungs-Schritt → Herausforderungs-Pool, sonst Abenteuer-Pool (`MapTargets.fallbackKind`)
- Dungeon-Karte (Spawner-Verlies) fällt weg: Verliese sind Features, keine Strukturen, und stehen in keinem Pool

## Höhlenkarte + Netherkarte (`cavemap/*`)
| Wert | Aktuell |
|---|---|
| Ebenen Overworld (Y, untere Grenze inklusive) | unten −64 bis 0 (Bedrock bis Ende des Tiefenschiefers), oben 0 bis Oberfläche (`CaveMapKind.CAVE`) |
| Ebenen Nether | unten 0 bis 32 (Lava), oben 32 bis 128 (`CaveMapKind.NETHER`) |
| Machen | Rechtsklick mit der leeren Karte; Höhlenkarte: Overworld und kein offener Himmel über dem Kopf (Höhenkarte `MOTION_BLOCKING_NO_LEAVES`); Netherkarte: nur Nether; sonst passiert nichts und die Karte bleibt |
| Name | leer „Höhlenkarte“ / „Netherkarte“; gefüllt je nach Ebene „Flache/Tiefe Höhlenkarte“ (EN Shallow/Deep Cave Map), „Flache/Tiefe Netherkarte“ (oben = flach, unten = tief) |
| Bindung | an Dimension und Ebene beim Machen, Mittelpunkt auf dem Spieler (Kachelraster wie Vanilla), 1 Block pro Pixel |
| Gezeichnet | nur beim Halten in der eigenen Ebene (Dimension, Höhe), alle 10 Ticks (`DRAW_INTERVAL_TICKS`) |
| Sichtweite | 32 Blöcke (`CaveMapDrawer.RADIUS`), 32 Strahlen (`RAYS`), Höhen Auge / +2 / +4 (`RAY_HEIGHTS`) |
| Höhen | untere zuerst; obere Strahlen nur für Pixel ohne Luft darunter und nur, wenn zwischen Kopf und dieser Höhe nichts Festes hängt |
| Falsche Ebene | ein Boden innerhalb der Höhenspanne der Karte ersetzt aktiv einen Pixel, der einen Boden von außerhalb zeigt (tiefe Höhle auf der flachen Karte), ohne Rücksicht auf die Pixel-Höhe |
| Struktur-Blöcke | Pixel, die einen Block des Block-Tags `vo_trade_rework:cave_map_structure_blocks` zeigen (Minenschacht-Holz und -Schienen, Dungeon-Bruchstein, Trial-Chamber-Tuffziegel und Kupfer, Festung, Antike Stadt, Nether-Festung u. a.; Datei `tags/block/cave_map_structure_blocks.json`, für andere Mods erweiterbar), werden nach dem Zeichnen nie wieder verändert (nur im Speicher). Sie überschreiben auch aktiv alles: die Höhen-Regel (Pixel-Höhe), die Höhenspannen-Regel und jede vorhandene Farbe, auch von höheren Strahlen; nur Oberflächen-Schwarz und ein schon gesperrter Strukturpixel sind stärker. Dafür werden auch bereits gezeichnete Pixel jedes Mal neu gelesen |
| Pixel-Höhe | jeder gefärbte Pixel merkt die Y-Höhe seines Bodenblocks (nur im Speicher); ein Strahl auf dieser Höhe oder darüber berechnet ihn nicht und überschreibt ihn nie, nur ein Strahl darunter kann ihn neu färben |
| Oberfläche | eine vorhandene Karte wird auch an der Oberfläche weitergezeichnet (Spieler in der Höhenspanne der Karte); Böden über Y 60 mit offenem Himmel darüber (`SURFACE_BLACK_ABOVE`, nur Overworld) werden schwarz. Eine neue Karte lässt sich dort weiter nicht machen |
| Boden | erster Block mit Kartenfarbe von der Strahlenhöhe nach unten, bis zum Weltboden; seine normale Kartenfarbe ist die Standardfarbe (keine Gefälle- oder Wassertiefen-Schattierung mehr) |
| Höhen-Schattierung | der tiefste Block einer Ebene wird 40 % dunkler als seine normale Kartenfarbe, der höchste 40 % heller, dazwischen linear (`HEIGHT_SHADE`); obere Overworld-Ebene zählt bis Y 64 (`CaveMapKind.shadeTopY`). Ein Pixel kann nur eine der Vanilla-Palettenfarben sein: genommen wird die dem verschobenen RGB-Wert nächste Palettenfarbe, kein Dithering. Lava und Holz (Klang Holz/Nether-Holz/Kirsch/Bambus) sind ausgenommen und behalten ihre normale Farbe |
| Tiefer als die Ebene | der Boden unter der Untergrenze (tiefe Höhle auf der flachen Karte) wird gezeichnet: 40 % plus 5 % je Block darunter dunkler (`DARKEN_PER_BLOCK`) bis höchstens 90 % dunkler (`MIN_BRIGHTNESS` 10 %), dann bleibt es die dunkelste Palettenfarbe; so ein Pixel füllt nur leere oder schwarze Pixel und überschreibt nie einen entdeckten |
| Wände | schwarz (Umriss); schwarz überschreibt nie einen entdeckten Pixel |
| Schwarzer Umkreis | jeder noch unentdeckte Pixel im Umkreis von 32 Blöcken um den Spieler wird schwarz, auch hinter dem Ende der Strahlen (`BLACKOUT_RADIUS`); eine Farbe ersetzt Schwarz sofort |
| Nicht gesehen | außerhalb des schwarzen Umkreises bleiben Pixel unverändert (kein Sehen durch Wände) |

## Abnehmer der Abenteuerkarte
- Abenteuerstation hier, Mason (Geode), Goldsmith (Kupfer-/Eisenader)
- Herausforderungskarte: nur die Challenge Station des Veteran (`tweaks/veteran.md`)
