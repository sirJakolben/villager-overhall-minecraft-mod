# Villager Overhaul – Active Development

Was noch aussteht. Erledigtes steht in [README.md](README.md). Zahlen: [tweaks/](tweaks/), fehlende Grafiken: Obsidian `Fehlende Texturen und Modelle.md`. Stand: 2026-09-30.

## 1. Im Spiel testen (umgesetzt, noch nie getestet)

- **Kern allein** (`:core:runClient`): Quests + Trades, Leisten, Level durch Arbeit, Ertrag-Anzeige, `/vo reset`
- **Kern + Extension** (`:trade-rework:runClient`): Sektionen, Quests (Reroll, Tageslimit), Masteries, Passive, Fokus zwischen Sektionen, alte Welten laden
- **Zombie, Heilen**, Lohenstaub in neuen Dorf-Brauständen, Lizenz in der Mod-Liste
- **Stationen**: ein Besitzer pro Station, Claim per Smaragd, nicht begehbar; alle fünf Passive-Stationen auf der gemeinsamen Basis (Fenster, Redstone-Puls, Komparator, Trichter)
- **Handelsblock**: Ruf per Redstone, Pathfinding, Ausrichtung, Partikel
- Zweiter Bezahl-Slot, Restock-Leisten, Glück wirkt auf Restock, Kreativ-Mittelklick, leere Zeilen
- **Librarian**: Spezialbücher, Verbesserungsstation, Buch-Upgrade, Hard-Quest, Lore-Schriftrollen
- **Mason**: brauner Sandstein, Dorfbewohner-Statuen, Steinchen + Wegsteine, Brechstation, Quests
- **Zombie Villager Cage** (Gametests laufen: Tragen, Freilassen, Ablenkung, Verschonen): im Spiel Optik prüfen (Zombie quer in den Armen, Cage-Gitter, Zombie im Cage), Heilung per Schwäche-Trank + goldenem Apfel bis zum Villager, Sonne beim Tragen
- **Runesmith**: Upgrade-Vorlage, Reparaturstation, Polster, Master 3 + 4
- **Veteran**: Waffenständer, Mob-Waffen, Piglin-Armbrust, Challengestation (Passive)
- **Goldsmith**: Basic 0 + 1 + 4, Masteries 1–4 (Blöcke, Texturen, Ornated Redstone als Leitung), Verwertungsstation
- **Cartographer**: Kartenstation (vier Masteries), Abenteuerstation (Passive, Abenteuer-/Herausforderungs-Pools), Höhlen-/Netherkarte
- **Entdeckerkarten** zu Geoden, Erzadern, Verliesen (`/vo locate` zum Prüfen)
- **Farmer**: Erntekorb, Glasurstation, vier Masteries
- **Fisherman**: Fischkorb, Biom-Fische (Boote entfernt 2026-10-07, Werftstation ohne Masteries)

## 2. Berufe – Offenes

- **Librarian**: Papierstapel-Block, 36 Lore-Fragment-Texte, Balancing, Texturen
- **Mason**: Grafiken, Balancing
- **Runesmith**: Basic 0–3 + 5–6, Master 2, Quests, Balancing, Grafiken
- **Veteran**: Challengestation im Spiel prüfen (Dauer, Pool-Gewichte), Modell/Textur, düstere Master-Items (rostige Eisenbarren, flackernde Laterne, Skelett-Deko)
- **Goldsmith**: Basic 2–3, Quests, Balancing, Grafiken
- **Cartographer**: Basic 0–3, Abenteuerstation + Höhlen-/Netherkarte im Spiel prüfen (Reichweite, Ebenen-Grenzen, Optik), Modelle/Texturen, Quests
- **Farmer**: Basic 0–3 + 5–6, Quests, permanente Trades, Hot Potato, Balancing, Grafiken
- **Fisherman**: Basic 0–3 + 5–6, Quests, Smaragdfisch, Eigenschaften der Biom-Fische, Balancing, Grafiken
- **Noch nicht designt**: Butcher, Shepherd, Fletcher, Cleric, Leatherworker (wird Ranger: Brutstation für Mob-Eier, reitbare Spinnen)

## 3. Gemeinsame Systeme

- Kern erweiterbar machen: [plan-core-extensibility.md](plan-core-extensibility.md)
- Punkte-Ökonomie und Leisten-Platz im Kern-Fenster prüfen
- Performance mit 100+ Villagern profilen

## 4. Ideen

- Urnen-System (könnte eigene Mod werden)
- Kupfergolem als Begleiter des Villagers
- Eisengolems laufen bevorzugt zu Villagern
- Villager individualisierbar (gefärbte Lederrüstung)
- Denkblase mit dem Item, das der Villager will

## Risiken

- Versionsdrift 26.1 → 26.2 (bewusst auf 26.1): Mixins flach halten
- Andere Villager-Mods hooken dieselben Stellen
