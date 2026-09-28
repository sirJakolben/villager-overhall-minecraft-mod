# Tweaks – Veteran (ersetzt Weaponsmith, bis 2026-09-27 „Repair Smith“)

Zwilling der Veteran-Werte aus Obsidian (`Veteran.md` Wertetabellen + Mob-Waffen-Teil von `Tweak-Werte.md`), kompakt. Bei Abweichung gilt Obsidian.
Base = Rang 0 der Gruppe, Max = Max-Rang, dazwischen linear. Fehlt ein Wert → 1. Stand: 2026-09-27.
Umbau umgesetzt 2026-09-27: Dateien `veteran_*`, Code `veteran/`; Vorlage + Reparaturstation beim Runesmith (`runesmith.md`).

## Basic Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 0 | Fernglas | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 1 | Rahmen | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 2 | Waffenständer (neuer Wand-Block, eine Waffe) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Challengestation | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Passive-Station | offen | – | – | – |
| 5 | Außenposten-Karte | 1 → 1 Smaragd | 1 Abenteuerkarte | 1 → 1 | 1 → 1 |
| 6 | Verlies-Karte (Spawner-Verlies, aus dem Weltseed + nachgeprüft) | 1 → 1 Smaragd | 1 Abenteuerkarte | 1 → 1 | 1 → 1 |
- Rang 3 offen (im Spiel leere Zeile); Fernglas, Rahmen, Waffenständer im Code seit 2026-09-27

## Master Trades – Herausforderungskarten (Diamanten im 1. Slot, Herausforderungskarte im 2. Slot)
| Rang | Karte zu | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 1 | Trial Chamber | 1 → 1 Diamant | 1 Herausforderungskarte | 1 → 1 | 1 → 1 |
| 2 | Woodland Mansion | 1 → 1 Diamant | 1 Herausforderungskarte | 1 → 1 | 1 → 1 |
| 3 | Ozeanmonument | 1 → 1 Diamant | 1 Herausforderungskarte | 1 → 1 | 1 → 1 |
| 4 | Antike Stadt | 1 → 1 Diamant | 1 Herausforderungskarte | 1 → 1 | 1 → 1 |

## Quests — `.../exchange/veteran_*.json` (Sektion `vo_trade_rework:quests`) (alle Werte 1)
- Easy: Zombie-, Wüstenzombie-Waffe (Eisenschwert/-schaufel), Skelett-, Eiswanderer-, Sumpfskelett-Bogen, Plünderer-Armbrust
- Hard: Piglin-Waffe (Goldschwert/Armbrust), Piglin-Barbar-Goldaxt, Witherskelett-Steinschwert, Diener-Eisenaxt, Ertrunkener-Dreizack, Totem der Unsterblichkeit (normales Item)
- keine Dauer-Quest: ab Quest-Rang 6 ein zweiter Hard-Platz (2026-09-27), Tageslimit auf Rang 9: 10 statt 7 (+3)

## Mob-Waffen — `veteran/MobWeapons.java`, `.../mob_weapon/*.json`
| Wert | Aktuell |
|---|---|
| Drop-Chance | 0,25 (Vanilla 0,085) |
| Name | gelb (Seltenheit ungewöhnlich) |
| Effekt-Chance | 0,2 pro Treffer / Pfeil |
| Effekt-Dauer | 0,5 × Mob-Dauer |
| Effekte (Mob-Dauer) | Wither 200, Hunger 280, Langsamkeit 600, Vergiftung 100 Ticks |
| Tooltip | blau „+ Verursacht X-Effekt“ |
| Diener-Axt | 20 % kürzere Abklingzeit (Angriffsgeschwindigkeit × 1,25) |
| Piglin-Armbrust | 20 % schneller spannen (`charge_reduction` 0,2) |

## Passive
- offen
