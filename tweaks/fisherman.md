# Tweaks – Fisherman / Fischer (teilweise umgesetzt)

Zwilling von Obsidian `Fisherman.md`, kompakt. Im Code seit 2026-09-30: Passive (Fischkorb mit Zählern, Biom-Fische), Stations-Kauf-Trades; Basic Trades 0–3, 5, 6 und Quests noch offen (leere Zeilen). Fehlende Werte = 1, außer den Platzhaltern unten. Bei Abweichung gilt Obsidian. Stand: 2026-09-30. Die Fischgerichte sind beim Butcher.

## Stationen (Namen Vorschlag, frei änderbar)
| Station | Id | Rolle | Kauf |
|---|---|---|---|
| Fass (Vanilla) | – | Basic-Arbeit | Vanilla |
| Werftstation | `shipyard_station` | Master (Placeholder-Würfel) | Basic 4, 1 → 1 Smaragd |
| Fischkorbstation | `fish_basket_station` | Passive (Korb, 5 Slots) | Basic 4, 1 → 1 Smaragd |

## Basic Trades
- Rang 0–3, 5, 6: leere Zeilen (`fisherman_todo_basic_*.json`); Wunsch: die ersten zwei einfaches Essen; Smaragdfisch → Smaragde als permanenter Trade (Quest, noch nicht gebaut)

## Passive – Fischkorb — `passive/CarryingWork.java`, `passive/FishingWork.java`, `fisherman/VillagerBobber.java`, `client/render/VillagerBobberRenderer.java`, `basket/*`
- Umgebaut 2026-10-07: Der Fischer **fischt wirklich**. Ist Wasser in der Nähe, nimmt er eine Angel in die Hand und wirft einen Schwimmer aufs Wasser (Bogenflug, Vanilla-Schwimmer-Textur und -Schnur von seiner Hand, Wurf-/Platsch-/Einholgeräusch); nach 5–15 s taucht der Schwimmer wie bei einem Biss, dann holt er ein: **kein Fisch wird erzeugt**, die Last steigt um eine Einheit („Fisch-Zähler“). Pause 2 s bis zum nächsten Wurf
- Die Passive-Leiste zeigt die Last; Kapazität wie beim Farmer: Rang 0 = 3 Fische, Rang 6 = 9
- **Wurf nur mit freier Sicht** (2026-10-07): vor jedem Wurf braucht er eine **Line of Sight** von der Hand zum Wasser (nichts Festes dazwischen, Wasser höchstens 9 Blöcke entfernt). Hat er sie, **dreht er sich zum Wasser** (Körper und Kopf) und wirft – nie nach hinten, nie gegen Wand oder Korb. Hat er keine, läuft er (Kern: `SectionLogic.workPlace` + `isPending`, die Vanilla-Arbeit pausiert unterwegs) zu einem Standplatz in höchstens 6 Blöcken Abstand vom Wasser, von dem aus er es sieht; mehrere Gewässer werden probiert, ein eingemauertes übersprungen. Nach dem Wurf ist er frei; für den nächsten Wurf braucht er wieder freie Sicht. Kommt er in 30 s nicht hin, wartet er eine Minute und sucht neu
- **Fester Fischplatz** (2026-10-08): bis der erste Wurf im Wasser landet, wirft er von dort, wo er freie Sicht hat; **der Block, in dem er dabei stand, wird sein Fischplatz** – für jeden weiteren Wurf läuft er genau dorthin (Kern führt ihn bis ~1,7 Blöcke heran, die letzten Schritte macht er selbst) und wirft von dort. Ist die Sicht vom Platz weg oder geht ein Wurf daneben, sucht er einen neuen. Gemerkt in `PassiveCounters.fixedSpot`
- **Beim Wurf hält er still und schaut hin**: ab dem Wurf 1,5 s und nach dem Aufprall noch 1 s dreht er Körper und Kopf jeden Tick zum Schwimmer und bleibt stehen – über denselben Weg wie beim Handelsblock: der Kern pausiert die ortsgebundenen Vanilla-Verhalten (Herumschlendern, Zurück zum Arbeitsblock), solange `FishingWork.isHeld` gilt (`ExtensionHooks.registerStayPut` → `PausedWhileAway`); ein schon laufender Weg wird einmal beim Wurf gestoppt – kein Wegwandern, das die Schnur zurückzieht; nach dem Wurf wird der Fokus sofort geleert, damit er nicht zum Korb abbiegt. Die Angel bleibt vom Wurf bis zum Einholen durchgehend in der Hand (wird nur noch bei Wurf und Ende gesetzt/entfernt)
- Voll → zum Korb; auch bei nicht voller Leiste nach einer Minute ohne Fang; einmal unterwegs bleibt er, bis alles abgelegt ist; am Korb fischt er nicht; Angel verschwindet beim Wegbringen
- Am Korb nimmt er **alle 3 s eine Einheit** heraus; **eine Einheit = genau ein Fisch** (umgestellt 2026-10-07). Jeder Fisch ist mit **(Rang + 1) %** der **Biom-Fisch** des Korb-Bioms (1 % bis 7 %), sonst Kabeljau (60 %) oder Lachs (40 %); alles geht direkt in den Korb
- **Fänge pro Arbeitstag** sind vorgegeben, die Angelzeit ist darauf abgestimmt: **Rang 0 = 10, Rang 6 = 30** Fische, gleichmäßig dazwischen (13,3 / 16,7 / 20 / 23,3 / 26,7). Nur auf **Rang 6** zählt ein Fang mit **50 %** doppelt (Zähler +2) – diese Bonus-Fische sind in den 30 **nicht** eingerechnet (real im Schnitt 45). Die Wartezeit bis zum Biss ist je Rang so gewählt, dass ein Fang-Zyklus (Wartezeit + Wurf + Biss + Pause + Gang zum Ufer + Entleeren) ungefähr 8200 ÷ Tagesfänge Ticks dauert; wo die festen Zeiten schon zu viel sind (Rang 6), bleibt es bei der Mindestwartezeit
| Wert | Aktuell | Einheit | Code |
|---|---|---|---|
| Kapazität, Passive-Rang 0–6 | 3, 4, 5, 6, 7, 8, 9 | Einheiten | `PassiveCounters.capacity` |
| Fänge pro Arbeitstag, Rang 0–6 | 10, 13,3, 16,7, 20, 23,3, 26,7, 30 | Fische (ohne Bonus) | `FishingWork.dailyFish` |
| Bonus auf Rang 6 | 50 | % | ein Fang zählt 2 Einheiten (`FishingWork.BONUS_PERCENT`) |
| Mittlere Wartezeit bis zum Biss | je Rang aus den Tagesfängen (Rang 0 ≈ 620, Rang 6 ≈ 73) | Ticks | `FishingWork.biteWaitTicks`, Streuung 0,5–1,5× in `VillagerBobber` |
| Biom-Fisch-Chance je Fisch | Rang + 1 (1–7) | % | `FishingWork.biomeFishPercent` |
| Kabeljau-Anteil der übrigen | 60 (Lachs 40) | % | `FishingWork.COD_PERCENT` |
| Wartezeit bis zum Biss | 100–300 | Ticks (5–15 s) | `VillagerBobber.BITE_MIN_TICKS`, `BITE_RANDOM_TICKS` |
| Biss-Tauchen vor dem Einholen | 20 | Ticks | `VillagerBobber.BITE_TICKS` |
| Pause nach Fang / verlorenem Wurf | 40 | Ticks | `FishingWork.CAST_PAUSE_TICKS` |
| Wasser in der Nähe (um den Arbeitsplatz, Seerosen über dem Wasser gelten als offen) | 14 seitlich, 5 hoch/runter | Blöcke; Standplatz höchstens 6 Blöcke vom Wasser, Wurf höchstens 9 | `FishingWork.WATER_REACH`, `WATER_HEIGHT` |
| Entleeren, Rückkehr bei Stillstand, Reichweite zum Korb | 60 Ticks (3 s), 1200 Ticks, 2 Blöcke | | `CarryingWork.*` |
- Korb: wie beim Farmer (5 Slots, Füllstand 0–5, Hitbox 14 × 14 × 9 px, Trichter unten); der Fischer steht dort, wo Vanilla ihn arbeiten lässt, und läuft nicht zum Wasser – das Wasser muss im Umkreis sein
- Die Angel in der Hand ist eine echte Fischangel im Hauptslot (Drop-Chance 0), sie wird nach dem Fang oder bei jeder Prüfung ohne Schwimmer wieder weggenommen


## Biom-Fische — `fisherman/BiomeFish.java`, `data/vo_trade_rework/vo_trade_rework/biome_fish/*.json`, Biom-Listen `tags/worldgen/biome/fish_*.json`
| Biomgruppe | Fisch (Arbeitstitel) | Biome |
|---|---|---|
| Berge | Smaragdfisch `emerald_fish` | Gipfel (3), Verschneite Hänge, Hain, Wiese, Kirschhain, Windgepeitschte Hügel/Kies/Wald |
| Wüste | Dünenfisch `dune_fish` | Wüste |
| Savanne | Abendfisch `sunset_fish` | Savanne, Plateau, Windgepeitschte Savanne |
| Tiefsee | Abgrundfisch `abyss_fish` | Tiefer / lauwarmer / kalter Ozean |
| Taiga | Nadelfisch `needle_fish` | Taiga, Alte Kiefern-/Fichtentaiga |
| Schnee | Frostfisch `frost_fish` | Schneeebene, Eiszapfen, Verschneite Taiga/Strand, gefrorener Fluss/Ozean/tiefer Ozean |
| Dschungel | Dschungelfisch `canopy_fish` | Dschungel, Lichter Dschungel, Bambusdschungel |
| Pilzinsel | Sporenfisch `spore_fish` | Pilzinsel |
- Die Biom-Listen überschneiden sich nicht (bei Überschneidung gewinnt der erste Treffer, die Reihenfolge ist Zufall); Biome außerhalb (Ebene, Wald, Fluss …) haben keinen Fisch
- Spieler: Loot-Modifier ersetzt einen gefangenen Fisch mit 8 % durch den Biom-Fisch (nie zusätzlich; Schrott und Schätze bleiben) — `fisherman/BiomeFishModifier`, `loot_modifiers/biome_fish.json` (Platzhalter `chance`)
- Eigenschaften der einzelnen Fische offen; bisher schlichte Items (Tag `vo_trade_rework:biome_fish`)

## Masteries – Werftstation
- Offen: Die Boote (Bannerboot, Kanonenboot, Piratenbanner, Kanonenkugel, Kanone) wurden am 2026-10-07 komplett entfernt; Jakob beschreibt das Konzept neu. Die Werftstation (`shipyard_station`, Basic 4) steht weiter, ohne Einträge.

## Offen
- Quests (Ankauf Lore-Scrolls aus dem Meer), permanenter Trade Smaragdfisch, Basic Trades, Eigenschaften der Biom-Fische, Balancing, Aussehen der Stationen,
