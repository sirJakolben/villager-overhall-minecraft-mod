# Plan: Farmer + Fisherman – Passive und Masteries

Stand: 2026-09-30. Ziel der Session: die komplette Passive-Logik und Mastery-Logik beider Berufe (Extension `trade-rework/`). Quests und Basic Trades bleiben offen, außer den Kauf-Trades der Stationen. Design-Quelle: Obsidian `Farmer.md`, `Fisherman.md`; Zahlen: [tweaks/farmer.md](tweaks/farmer.md), [tweaks/fisherman.md](tweaks/fisherman.md).

## Entscheidungen (2026-09-30)

- **Fisherman-Mastery = nur Boote** (Bannerboot, Kanonenboot, Piratenbanner, Kanonenkugeln). Die Fischgerichte wandern zum **Butcher** – **2026-10-07: alle Boote wieder entfernt, Konzept folgt**
- **Farmer-Mastery = Apfel, Rote Bete, Karotte, Brot** (Master 1–4); die Hot Potato (Kettensprung) folgt später als eigener Eintrag
- **Körbe = Ausgabe**: die Zähler liegen am Villager, der Korb ist ein Lager-Block, in den beim Leeren das Ergebnis fällt
- Namen der Stationen (Vorschlag, frei änderbar): Farmer Master **Glasurstation** (`glazing_station`), Farmer Passive **Erntekorbstation** (`harvest_basket_station`), Fisherman Master **Werftstation** (`shipyard_station`), Fisherman Passive **Fischkorbstation** (`fish_basket_station`)
- Werte ohne Vorgabe sind Platzhalter (Liste in den Tweak-Dateien) – Trade-Mengen bleiben 1

## Ablauf (Phasen 1–5 im Code umgesetzt 2026-09-30, kompiliert, Ladetest ok, noch nicht im Spiel getestet – Testliste in active-development.md §1)

### Phase 1 – Fundament (beide Berufe)
- Zähler-Attachment am Villager (`passive/PassiveCounters`): Punkte + Fangzahl je Sorte, nur Server, übersteht Zombie-Umwandlung
- Korb-Block + Block-Entity (9 Slots, Trichter unten zieht, oben/seitlich füllt nichts, Komparator, Fenster = Vanilla-Werfer-Fenster), ein Typ für beide Körbe
- Stationen registrieren (Master + Passive je Beruf), Kauf-Trades Basic 4, leere Zeilen für die übrigen Ränge
- Suppen stapeln bis 16 (global, erkennt jede Suppe an Stapel 1 + Schüssel als Rückgabe)

### Phase 2 – Farmer Passive
- Ernte zählen: NeoForge-Drop-Ereignis (Breaker = Farmer, reife Pflanze), kein Mixin; Punkte pro Ernte = Passive-Rang + 1
- Leeren am Korb (`passive/HarvestWork`): je Punkt Chance auf die goldene Sorte (Weizen → goldenes Brot, Rote Bete, Kartoffel, Karotte), dazu eine kleine Chance auf goldenen Apfel; Ergebnis in den Korb; passt es nicht, wartet er
- Neue Items: goldenes Brot, goldene Rote Bete, goldene Kartoffel; Rezept der goldenen Karotte entfällt

### Phase 3 – Farmer Masteries
- Glasurstation als Master-Station; vier Masteries ohne Smaragde (Gold-Essen + Zutat): Candied Apple (Regeneration, danach Speed), goldene Rote-Bete-Suppe (Eile), Nachtsicht-Karotte, Brot-Gericht
- Effekt „danach Speed“ über einen eigenen Effekt (Zuckerschub), der beim Ablaufen Speed gibt

### Phase 4 – Fisherman Passive
- Fang-Takt (`passive/FishingWork`): steht er zur Arbeitszeit mit Wasser in der Nähe (gemerkt, kein Block-Scan je Prüfung), fängt er im festen Takt einen Fisch aus Vanillas Fisch-Beutetabelle (nie Schätze/Schrott)
- Zähler je Fischsorte; Leeren am Fischkorb: die Fische in den Korb, je Zählerpunkt Chance auf den Biom-Fisch seines Bioms
- Acht Biom-Fische (Datapack `biome_fish`, Item je Biomgruppe), der Spieler fängt sie per Loot-Modifier mit der Angel ebenfalls

### Phase 5 – Fisherman Masteries (Boote) – ENTFERNT 2026-10-07
- Alle Boote, Kanone, Kanonenkugel und Piratenbanner wurden wieder gelöscht; Konzept folgt von Jakob

### Phase 6 – Einordnen (erledigt)
- README, active-development.md, Tweak-Dateien, Obsidian (Fischgerichte → Butcher), Drive-Spiegel


### Umbau der Passiven 2026-10-07
- Beide Passiven arbeiten als Tragen: Farmer erntet wirklich, Fisherman fischt wirklich (Angel + Schwimmer, `VillagerBobber`); die Passive-Leiste zeigt die Last (Kapazität 3–9 je Rang), am Korb sinkt sie alle 5 s um eine Einheit und jede Einheit wird gewürfelt – siehe tweaks/farmer.md und tweaks/fisherman.md
- Offene Grafik: Gemüse-Schichten – Stufe 1 nutzt `vegetables_1`, Stufen 2–5 nur `vegetables`
