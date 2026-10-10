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
| 4 | Trappings-Station | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
| 4 | Challengestation (Passive-Station) | 1 → 1 Smaragd | – | 1 → 1 | 1 → 1 |
- Rang 3 offen (im Spiel leere Zeile); Fernglas, Rahmen, Waffenständer im Code seit 2026-09-27

## Master Trades
- keine Karten-Trades mehr (gestrichen 2026-10-10: Trial Chamber, Anwesen, Monument, Antike Stadt, Außenposten, Verlies) – Karten zeichnet die Passive
- die Mastery-Sektion ist leer, bis die düsteren Items (rostige Eisenbarren, flackernde Laternen, Skelett-Deko) kommen

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

## Passive – Challengestation (`mapping/*`, `passive/MappingWork.java`)
| Wert | Aktuell |
|---|---|
| Karte aus | 1 Herausforderungskarte (Station nimmt nur diese) |
| Dauer je Karte (ganze Arbeitstage) nach Passive-Rang 0–6 | 6, 5, 4, 4, 3, 3, 2 (halb so schnell wie der Kartograf) |
| Ziel | zufällig, nach Gewicht aus dem Herausforderungs-Pool, erst beim Fertigwerden gewürfelt; Suche und Wiederholung wie beim Kartograf (`tweaks/cartographer.md`) |
| Name der Karte | „Gezeichnete Herausforderungskarte“, Ziel-X ohne Hinweis |

### Herausforderungs-Pool – `map_pool/challenge.json`
| Gewicht | Struktur |
|---|---|
| 8 | Minenschächte |
| 3 | Festung (Stronghold), Trial Chambers |
| 2 | Antike Stadt, Anwesen, Ozeanmonument |
- Weitere Dateien (auch von Mods) mit `"pool": "challenge"` ergänzen; Strukturen ohne Eintrag: Untergrund-/Festungs-Schritt → hier, sonst Abenteuer-Pool

## Zombie Villager Cage — `cage/*`, Basic 3 (8 Smaragde, `exchange/veteran_zombie_villager_cage.json`)
| Wert | Aktuell |
|---|---|
| Cage-Suchradius Golem | 48 Blöcke (`ZombieCaptives.CAGE_RANGE`) |
| Zombie-Villager-Suchradius | 16 Blöcke (`SCAN_RANGE`) |
| Betäubung nach Fallenlassen | 60–80 Ticks (`DAZE_MIN_TICKS` / `DAZE_EXTRA_TICKS`) |
| Aufgeben, dann Vanilla-Angriff | 1200 Ticks (`GIVE_UP_TICKS`) |
| Tempo hin / mit Last | 1,1 / 0,8 (`CarryZombieVillagerGoal`) |
| Greifweite | 2,3 Blöcke |
| Zeitlimit Anlaufen / Tragen | 200 / 600 Ticks |
| Pause nach Ablenkung / sonst | 100 / 40 Ticks |
| Cage-Reservierung | 400 Ticks (`ZombieVillagerCageBlockEntity.RESERVATION_TICKS`) |
| Schwäche-Trank im Cage | 2400 Ticks (`WEAKNESS_TICKS`) |
| Heilung (goldener Apfel) | 3600 + 0–2400 Ticks (`CURE_MIN_TICKS` / `CURE_EXTRA_TICKS`) |
| Trageposition | 1,1 vor, Höhe 1,55 (`CarryPose`) |
