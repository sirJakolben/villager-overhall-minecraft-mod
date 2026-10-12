# Tweaks – Farmer / Bauer (teilweise umgesetzt)

Zwilling von Obsidian `Farmer.md`, kompakt. Im Code seit 2026-09-30: Passive (Erntezähler + Gold-Chance), Masteries 1–4, Stations-Kauf-Trades; Basic Trades 0–3, 5, 6 und Quests noch offen (leere Zeilen). Fehlende Werte = 1, außer den Platzhaltern unten. Bei Abweichung gilt Obsidian. Stand: 2026-09-30.

## Stationen (Namen Vorschlag, frei änderbar)
| Station | Id | Rolle | Kauf |
|---|---|---|---|
| Komposter (Vanilla) | – | Basic-Arbeit | Vanilla |
| Glasurstation | `glazing_station` | Master (Placeholder-Würfel) | Basic 4, 1 → 1 Smaragd |
| Erntekorbstation | `harvest_basket_station` | Passive (Korb, 5 Slots) | Basic 4, 1 → 1 Smaragd |

## Basic Trades
- Rang 0–3, 5, 6: leere Zeilen (`farmer_todo_basic_*.json`); Wunsch: die ersten zwei einfaches Essen, letzter die Vogelscheuche (nur Deko)

## Passive – Erntekorb — `passive/CarryingWork.java`, `passive/HarvestWork.java`, `farmer/HarvestCounting.java`, `passive/PassiveCounters.java`, `basket/*`
- Umgebaut 2026-10-07: Der Farmer arbeitet wirklich am Feld. Jede reife Pflanze, die er erntet (Weizen, Karotte, Kartoffel, Rote Bete – Vanilla-Ernte, gezählt über das Drop-Ereignis), ist **eine Einheit auf seiner Last**; nur mit Erntekorb und nur solange die Last nicht voll ist. Die **Passive-Leiste zeigt die Last** (voll = Kapazität); die Leiste füllt sich nicht mehr durch Arbeit an der Station
- Voll → er muss zum Korb; er geht auch bei nicht voller Leiste, wenn eine Minute nichts Neues dazukam (und Last > 0); einmal unterwegs bleibt er, bis alles abgelegt ist; Korb ohne Platz → er geht nicht hin
- Am Korb (Arbeitszeit, nahe am Block) nimmt er **alle 3 Sekunden eine Einheit** heraus und würfelt sie; die Leiste sinkt jedes Mal um eine Einheit, Gewinne gehen direkt in den Korb, Redstone-Puls + Arbeitsgeräusch je Einheit
| Wert | Aktuell | Einheit | Wirkung | Code |
|---|---|---|---|---|
| Kapazität, Passive-Rang 0–6 | 3, 4, 5, 6, 7, 8, 9 | Einheiten | wie viele Ernten er tragen kann (Vorgabe: Rang 0 = 3, Max = 9) | `PassiveCounters.capacity` |
| Entleeren | 540 | Ticks (9 s) | Zeit für einen vollen Korb-Gang, bei jedem Rang gleich (Einheit alle 540 / Kapazität Ticks) | `CarryingWork.DRAIN_TOTAL_TICKS` |
| Morgens leeren | - | - | was nach Arbeitsende noch getragen wird, bringt er morgens zuerst zum Korb | `CarryingWork.apply` |
| Rückkehr bei Stillstand | 1200 | Ticks (1 min) | so lange nichts Neues geerntet → er bringt, was er hat | `CarryingWork.IDLE_TICKS` |
| Gold-Chance je Einheit, Passive-Rang 0–6 | 0, 3,3, 6,7, 10, 13,3, 16,7, 20 | % | goldene Sorte der Pflanze (Weizen → goldenes Brot, Rote Bete → goldene Rote Bete, Kartoffel → goldene Kartoffel, Karotte → goldene Karotte); Rang 0 = 0 %, Rang 6 = 20 %, gleichmäßige Schritte dazwischen (Vorgabe). **Kein goldener Apfel mehr** – der Farmer erzeugt ihn nicht | `HarvestWork.goldChancePercent`, `MAX_GOLD_CHANCE_PERCENT` |
| Reichweite zum Korb | 2 | Blöcke | wie die anderen Passiven | `CarryingWork.WORK_REACH` |
- Korb: 5 Slots (Vanilla-Trichterfenster wie die Verbesserungsstation), Füllstand 0–5 = belegte Slots (Blockzustand `fill`, 6 Modelle), Hitbox 14 × 14 × 9 px, Trichter unten zieht, oben/seitlich füllt nichts, Komparator, Spieler dürfen von Hand rein und raus; passt nicht alles hinein, fällt der Rest vor den Korb
- Kern: `SectionLogic.fillsFromWork` – eine Passive, die ihre Leiste selbst führt, füllt sich nicht durch Arbeit an der Station und wird vom Fokus nur gewählt, wenn Arbeit wartet (`isPending`), nie wegen ihrer Leiste
- Die Ernte selbst bleibt beim Farmer wie in Vanilla; Rezept der goldenen Karotte entfernt (`data/minecraft/recipe/golden_carrot.json` mit `neoforge:never`)
- Golden-Items: goldenes Brot (Nahrung 8), goldene Rote Bete (4), goldene Kartoffel (6), Sättigung wie die goldene Karotte (14,4) – Platzhalter


## Masteries – Glasurstation — `exchange/farmer_*.json`, `farmer/FarmerItems.java`
- Ohne Smaragde; Gold-Essen im 1. Slot, alle Mengen 1; Zutat im 2. Slot bewusst weggelassen (offen)
| Rang | Eingabe | Ergebnis | Effekt (Platzhalter) |
|---|---|---|---|
| 1 | Goldener Apfel | Kandierter Apfel (`candied_apple`) | Regeneration II 30 s, danach Geschwindigkeit I 60 s (über den unsichtbaren Effekt Zuckerschub, `SugarRushEffect`) |
| 2 | Goldene Rote Bete | Goldene Rote-Bete-Suppe (`golden_beetroot_soup`) | Eile II 2 min; Suppe: stapelt 16, gibt die Schüssel zurück |
| 3 | Goldene Karotte | Glasierte Karotte (`glazed_carrot`) | Nachtsicht 3 min |
| 4 | Goldenes Brot | Goldtoast (`gilded_toast`) | Absorption II 2 min – **Effekt aus der Luft, Obsidian sagt nur „offen“** |
- Hot Potato (goldene Kartoffel, Kettensprung) fehlt noch; die goldene Kartoffel fällt schon aus dem Korb
- Suppen stapeln bis 16: global (`farmer/SoupStacking`), erkennt jedes Item mit Stapel 1 und Schüssel als Rückgabe – Vanilla und Mods

## Offen
- Quests (Ankauf alles Pflanzliche außer Crops), Permanente Trades (Kürbis-/Melonenblock), Basic Trades, Balancing, Zutaten der Masteries, Aussehen der Stationen

## Vogelscheuche (letzter Basic Trade, Rang 6)

- Trade: 1 Smaragd + 2 Heuballen → Vogelscheuche (1 Nutzung, max. 2); Dekoblock, eigenes Modell (28 Pixel hoch), zwei Blöcke hoch wie eine Tür (untere Hälfte zeichnet die Figur und droppt), Südseite zeigt beim Platzieren zum Spieler
- Zombies (auch Husk, Drowned, Zombie Villager; nicht Zombifizierter Piglin) laufen zur nächsten Vogelscheuche, wenn sie keinen Spieler als Ziel haben – die Scheuche hat immer Vorrang vor Villagern, egal wie nah diese sind
- Auf Hard brechen sie sie wie eine Tür (240 Ticks, Riss-Animation); sonst stehen sie nur dran
- Suche per POI-Abfrage (Typ `scarecrow`), alle 20 Ticks pro Zombie, kein Block-Scan

| Wert | Standard | Wo |
|---|---|---|
| Reichweite | 24 Blöcke | `ScarecrowGoal.RANGE` |
| Prüf-Intervall | 20 Ticks | `ScarecrowGoal.CHECK_INTERVAL` |
| Zuschlag-Abstand | 1,0 Blöcke (waagrecht zur Blockmitte) | `ScarecrowGoal.HIT_SQR` |
| Brechzeit (Hard) | 240 Ticks | `ScarecrowGoal.BREAK_TICKS` |
