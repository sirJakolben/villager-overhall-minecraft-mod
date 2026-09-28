# Villager Overhaul – Active Development

Alles, was noch aussteht. Erledigtes wandert (kompakt) nach [README.md](README.md) und wird hier gelöscht.
Zahlen: [tweaks/](tweaks/) · fehlende Grafiken: Obsidian `Fehlende Texturen und Modelle.md`. Stand: 2026-09-28. Seit 2026-09-28 zwei Mods: Kern `core/` (Villager Overhaul) + Extension `trade-rework/` (Trade Rework) – siehe README.

## 1. Im Spiel testen (umgesetzt, noch nie getestet)

- [ ] **Sektions-Umbau** (2026-09-28, ersetzt den Test „Aufteilung Kern + Extension“): alte Welten laden, Villager starten dabei bewusst neu (Level, Ränge, Bestand weg – kein Absturz); IntelliJ-Gradle-Projekt neu laden. `:core:runClient` (`run-core/`, neue Welt): jeder Beruf zeigt nur **Quests** (Smaragd-Trades) + **Trades**, eine sichtbare Leiste (Trades), keine Masteries, keine Plakette; Quests restocken wie Trades (kein Tageslimit, kein Reroll), jedes Upgrade ein Eintrag mehr, danach 4 Preis-Upgrades, keine Level-Grenze; Berufsnamen Vanilla. `:trade-rework:runClient`: Librarian zeigt Quests (Reroll-Button + Tage-Timer, Tageslimit, Pause nach Abgabe) + Trades + Masteries (ab Schreibstation) + Plakette „Work“ oben links (ab Verbesserungsstation), Leisten Masteries/Trades/Passive rechts neben der Glücksleiste; Veteran/Kartograph ohne Plakette; Morgens zuerst zur Master-Station, Fokus wechselt zwischen Sektionen, Passive läuft wie vorher; `/vo invest <sektion>` (Tab-Vorschläge), `/vo state sections`, `/vo trade <eintrag>` auch für Quests, `/vo action vo_trade_rework:quests <slot> 0` = Reroll
- [ ] **Level, Ertrag, Reset** (2026-09-28): Extension-Berufe leveln über 20 hinaus (nur Punkte-Puffer 5 bremst, XP-Kurve ab 20 flach); höherer Ertrag (Vanilla-Beruf stapelbar nach allen Freischaltungen, Extension-Einträge mit `max_output`) zeigt oben rechts am Ergebnis die Base-Menge rot durchgestrichen, unten die neue – auch in Quest-Zeilen neben dem Reroll-Button lesbar; `/vo reset` am angeschauten Villager: Level 0, keine Punkte, alle Ränge 0, Bestand/Leisten frisch, Quest-Slots neu (kein Reroll-Timer), Glück + Stationen bleiben
- [ ] **Zombie, Heilen, Brauen, Lizenz** (2026-09-28): Villager mit Level/Rängen von Zombie beißen lassen (Schwierigkeit Normal/Schwer), heilen → Level, Punkte, Ränge, Bestand, Quest-Slots sind wieder da, Arbeitsplätze sucht er neu (kein doppelter Besitzer), Nebenstationen werden wieder frei; mehr Zombie-Villager nachts (grob jeder vierte Zombie); **neu generiertes** Dorf mit Tempel: Braustand hat 1–3 Lohenstaub (alte Chunks nicht); Mod-Liste zeigt Lizenz CC0-1.0, Autor, neue Beschreibung
- [ ] **Ein Besitzer pro Station** (Bug 2026-09-28, zwei Masons an einer Brechstation): in der betroffenen Welt geht einer der beiden innerhalb von ~5 s weg und sucht sich eine andere Brechstation (oder bleibt ohne); per Smaragd eine besetzte Station einem anderen Mason zuweisen → nur der neue arbeitet dort; Station abbauen und sofort neu setzen → trotzdem nur ein Besitzer
- [ ] **Claim-Partikel + Handelsblock-Partikel** (2026-09-28): Smaragd-Claim an Handelsblock oder Arbeitsplatz → grüne Sterne über dem Villager **und** über dem Block; Handelsblock abbauen/drauf laufen → Fichtenholz-Partikel statt Stücke des UV-Blatts
- [ ] **Handelsblock-Pathfinding** (2026-09-28): Villager während Arbeitszeit (~Tick 1400–9600) und Treffpunkt-Zeit (9600–11000) per Redstone rufen → läuft direkt hin und bleibt stehen, kein Hin-und-Her-Zuckeln; Rechtsklick-Besuch genauso; nach dem Ruf wieder normales Schlendern um Arbeitsblock/Glocke
- [ ] **Handelsblock + Stationen nicht mehr begehbar** (2026-09-28): Villager laufen um Handelsblock, Schreib-, Verbesserungs-, Brech- und Schmelzstation herum oder springen drüber, statt daran hängen zu bleiben
- [ ] Zweiter Bezahl-Slot (`second_input`)
- [ ] Spezialbücher am Zaubertisch: nur Kategorie-Verzauberungen, Ergebnis = verzaubertes Buch, eine von mehreren wird gestrichen, Vorschau stimmt
- [ ] Verbesserungsstation: Trichter/Spender rein, nur fertige Bücher raus, Komparator, Redstone-Puls + Arbeitsgeräusch, Abbauen droppt Bücher
- [ ] Buch-Upgrade: niedrigste Stufe über alle Bücher, nur Arbeitszeit, Gang zur Station bei voller Leiste, Leistengröße nach Passiv-Rang
- [ ] Hard-Quest „verzaubertes Buch mit Seelenläufer/Huschen/Windstoß“ (jede Stufe zählt)
- [ ] Reroll-Button in Quest-Zeilen + Tage-Timer im Tooltip, Quest-Zeile nach links gerückt
- [ ] Kreativ-Mittelklick auf Items im Handelsfenster
- [ ] Lore-Schriftrollen: Fundorte (`/loot give @s loot minecraft:chests/...`), Buch-Fenster, festes Fragment, nicht stapelbar, neue Lore-Quests
- [ ] Claim-System + Handelsblock komplett durchspielen
- [ ] Restock-Leisten (aus alter Testliste): Fokus auf größte Lücke, bleibt dabei bis voll/neuer Tag, +25 % pro volle Leiste, nie über Max, volle Leiste hält bei vollem Lager und gibt 2 s nach Trade frei (nur eigene Kategorie), Morgen-Rückfall nach Rang-Aufstieg
- [ ] Glück wirkt auf Restock: ohne Bett ~halb so schnell, Panik langsamer aber nie null
- [ ] **Mason** (umgesetzt 2026-09-26): Dorfbewohner-Statuen (2026-09-28): Wüsten-Mason verkauft Wüsten-Statue usw.; Setzen braucht 2 Blöcke Platz, Figur zufällig (Beruf/arbeitslos/Nitwit/Kind), grau, auf Bodenplatte, schaut zum Spieler; oberen oder unteren Teil abbauen → beide weg, 1 Statue droppt (Kreativ: keiner); Item zeigt grauen Arbeitslosen; Trades Rang 0–6 + Master 1–4 (Katalysator statt Smaragd), Steinchen + Wegsteine stapeln (je 1–4, gedreht, Schleichen setzt daneben, Drop = Anzahl; Wegsteine liegen passend auf Trampelpfad), Metamorph- + Brechstation als Arbeitsplätze (Claim, Bereiche sichtbar), Quests Easy/Hard/Dauer; Dauer-Quest-Item (glatter Stein) erscheint in keinem anderen Pool
- [ ] **Veteran Basic 0–2** (2026-09-27): Fernglas, Rahmen, Waffenständer; Waffenständer an eine Wand setzen (nur an festen Wänden, dreht sich zum Spieler), Rechtsklick mit Schwert/Axt/Speer/Dreizack/Streitkolben/Bogen/Armbrust hängt sie auf, andere Items nicht, leere Hand nimmt sie ab; Waffe sitzt sichtbar vor dem Brett (Lage/Größe prüfen – ungetestet!), Abbauen droppt Waffe + Ständer, Wand weg → Ständer fällt ab
- [ ] **Piglin-Armbrust**: spannt 20 % schneller (auch mit Schnellladen), Tooltip „+ 20 % schneller spannen“, bleibt nach Upgrade; normale Armbrust unverändert
- [ ] **Runenschmied Master 3 + 4**: Smaragd + Buch → Buch Haltbarkeit I bzw. Reparatur (Stufe I)
- [ ] **Verwerter Basic 4**: Veredelungs- + Schmelzstation kaufen, besetzen, Masteries/Passive-Bereich öffnen sich (Masteries noch leere Zeilen)
- [ ] **Schmelzstation** (Verwerter-Passive, 2026-09-27): Fenster wie die Brechstation (3 links, Pfeil, 3 rechts); nimmt nur Eisen-/Gold-/Kupfer-/Ketten-/Netherite-Ausrüstung (Diamant, Leder, Holz nicht – auch nicht per Trichter); Verwerter läuft bei voller Passive-Leiste hin und schmilzt ein Teil: neues Eisen-Brustpanzer auf Rang 0 → 2 Barren, halb kaputt → 1; Kette → Eisen-Nuggets; Netherite-Schwert → Diamantschwert (Verzauberungen, Name, Haltbarkeit anteilig) + 1 Scrap; Output voll → Teil wartet; Hochofen-Knistern beim Füllen, Redstone-Puls + Geräusch beim Schritt
- [ ] **Verbesserungsstation** (umbenannt, ID jetzt `enhancement_station`): alte Stationen in der Test-Welt sind weg – neu kaufen/setzen, Librarian nimmt sie an, Buch-Upgrades laufen wie vorher
- [ ] **Leere Zeilen**: Ränge ohne Trade von Veteran (Basic 3), Runenschmied (Basic 0–3, 5–6, Master 2), Verwerter (Basic 2–3, Master 1–4), Kartograph (Basic 0–3) zeigen eine komplett leere, deaktivierte Zeile (kein Smaragd, kein Text), nicht anklickbar
- [ ] **Geoden-, Verlies- + Erzader-Karten** (2026-09-27): erst `/vo locate geode`, `dungeon`, `copper_vein`, `iron_vein` an mehreren Stellen (auch in frisch erkundetem Gebiet) – die Zahl der passenden Blöcke sollte fast immer über 0 liegen; dann Mason Basic 6 (Smaragd + Abenteuerkarte → Geoden-Karte, Dorfbewohner-Statue jetzt auf Basic 5), Verwerter (Armorer) Basic 5/6 (Kupfer-/Eisenader-Karte), Basic 0/1 Eisenketten/Eisengitter; Karte zeigt rotes X und im Tooltip „Höhe: Y …“; hingehen und graben; Veteran Basic 6: Smaragd + Abenteuerkarte → Verlies-Karte (X auf dem Spawner; erster Kauf kann kurz ruckeln, weil Kandidaten-Chunks geladen werden)
- [ ] **Erkundungsstation**: Kartograph besetzt sie, Masteries-Bereich zeigt Dorfkarten ab Master-Rang 0 (Ebenen-Dorf), dann Wüste/Savanne/Taiga/Schnee
- [ ] **Karten-Trades** (2026-09-27): Kartograph verkauft auf Basic 5/6 Abenteuer- und Herausforderungskarte (Kartensymbol); Veteran Basic 5: Smaragd + Abenteuerkarte → Außenposten-Karte (rotes X); Veteran Master 1–4 (Challengestation): Diamant + Herausforderungskarte → Trial Chamber / Anwesen / Monument / Antike Stadt; vor dem ersten Handeln kurz eine leere Karte im Angebot, beim Öffnen des Fensters wird gesucht (kleiner Ruckler möglich) und danach die echte Karte angezeigt; jeder Kauf = dieselbe Karte; nicht gefunden → Angebot ausverkauft; nach Neustart bleibt die Karte beim Villager
- [ ] **Veteran + Runesmith** (Umbau 2026-09-27): Weaponsmith heißt „Veteran“, Toolsmith „Runenschmied“; Runesmith: Upgradestation + Reparaturstation als Arbeitsplätze (Basic 4, Claim, Bereiche sichtbar), Upgrade-Vorlage auf Master 1; Veteran: Challengestation (Basic 4, Masteries-Bereich noch leer), Steinstatue ist aus dem Spiel; Veteran-Quests Easy/Hard (Mob-Waffen, Plünderer-Armbrust in Easy, Dreizack + Totem der Unsterblichkeit in Hard); ab Quest-Rang 6 zweiter Hard-Platz statt Dauer-Quest (nie dieselbe Quest doppelt), Tageslimit auf Quest-Rang 9 = 10; Dauer-Quest Eisenbarren jetzt beim Runenschmied (ab Quest-Rang 6)
- [ ] **Upgrade-Vorlage** (eine für alle Stufen): am Schmiedetisch Vorlage + Teil + 1 **Block** der Zielstufe → nächste Stufe (Holz→Stein mit Bruchstein, →Kupfer mit Kupferblock, →Eisen mit Eisenblock, →Diamant mit Diamantblock; Leder→Kupfer, Kette/Gold→Eisen, Speere auch); Verzauberungen, Name, Markierung bleiben, Haltbarkeit prozentual gleich (20 % Eisen → 20 % Diamant); Vorlage verbraucht
- [ ] **Mob-Waffen**: Zombie/Skelett/Piglin … droppen ihre Waffe mit 25 % nur bei Spieler-Tötung; Name „Zombie-Eisenschwert“ in Gelb, nach Upgrade „Zombie-Diamantschwert“; aufgehobene Spieler-Items bleiben unmarkiert; Quest nimmt nur die richtige Mob-Waffe; Witherskelett-Schwert gibt mit 1/5 Wither (5 s), Eiswanderer-/Sumpfskelett-Bogen Pfeile mit Langsamkeit/Vergiftung; Diener-Axt schlägt 20 % schneller (Tooltip + auch nach Upgrade); Tooltip „+ Verursacht …-Effekt“ bei Witherskelett-, Wüstenzombie-, Eiswanderer-, Sumpfskelett-Waffen
- [ ] **Reparaturstation**: nur beschädigte Teile rein, Trichter wie Brechstation; 25 Punkte pro Schritt, 8 Schritte/Tag auf Rang 0; heile Teile wandern in den Output; Schleifstein-Geräusch beim Füllen, Runenschmied selbst stumm
- [ ] **Polster** (Runesmith Passive-Rang 6): voll reparierte Teile bekommen +10 % Polster, erst dann in den Output; heile Teile ohne volles Polster werden angenommen (unter Rang 6 wandern sie durch); Tooltip „+X Polster-Haltbarkeit“; Schaden (Werkzeug, Waffe, Rüstung) frisst zuerst das Polster, Unbreaking wirkt davor; Amboss/Mending füllen es nicht
- [ ] **Brechstation**: Trichter oben/seitlich → nur Brechbares in den Input, unten ← nur Output; Output-Slots für Spieler normale Slots (alles rein/raus); Fenster (3 links, Pfeil, 3 rechts); Mason läuft bei voller Passive-Leiste hin und bricht einen Batch (max. 4 Ergebnisse einer Sorte, Passive-Rang 5: 6, Rang 6: 8; 9 Stein + 1 glatter Basalt → 4, 4, 1, dann Basalt; Rang 0: 8 Batches/Tag, Max 32); Steinmetz-Varianten zurück (Polished-Granite-Treppe → Granit, 2 Stufen → 1 Block, auch über zwei Slots und gemischt: polierte Andesit-Stufe + Andesit-Stufe → Andesit), Stein-/Tiefenschiefer-Varianten → Bruchstein/Bruchtiefenschiefer, bemooste Steinziegel → bemooster Bruchstein, glatter Basalt → Basalt, Quarzblock/Schwarzstein bleiben, Beton → Pulver, glasierte → normale Terrakotta; Tuff-Abbaugeräusch (Takt wie Abbauen per Hand), während er an der Station die Passive-Leiste füllt (Geräusche nur an der Station, an der er gerade arbeitet – auch wenn Stationen nebeneinander stehen; der Villager selbst ist an der Passive-Station stumm, auch der Librarian; eigenes Arbeitsgeräusch allgemein nur, während sich eine Trade-Leiste füllt); Redstone-Puls + Geräusch beim Batch; Librarian-Buch-Upgrade läuft unverändert (Passive-Weiche)
- [ ] Quest-Tageslimit (aus alter Testliste): nur abgeschlossener Platz pausiert, nach Limit keine neuen Quests, angezeigte bleiben abschließbar, `/vo state quests` zeigt Limit, nächster Tag alles wieder da

## 2. Librarian – Reste

- [ ] **Notizzettel / Papierstapel als Block**: an die Wand oder auf Blöcke, stapelt wie Blütenblätter; Item `paper_pile` existiert; Textur + Modell später
- [ ] **Passiv-Bereich im Villager-Fenster**: Villager mit seiner Station zeigen – Design offen
- [ ] **Lore-Fragment-Texte**: 36 Texte (4 Herkünfte × 3 Stufen × 3), Platzhalter in `lang/*.json`, Schlüssel `lore.vo_trade_rework.<herkunft>_<stufe>.<1-3>` (Extension)
- [ ] **Balancing**: alle Trade-/Quest-Werte stehen noch auf 1 (außer Lore-Quests) → [tweaks/librarian.md](tweaks/librarian.md)
- [ ] Texturen: Fernkampfbuch, Lore-Schriftrollen, finale Stationen (Liste in Obsidian)

## 3. Weitere Berufe

Muster pro Beruf: Obsidian-Datei nach Vorbild `Librarian.md` · Basic Trades (Rang 0–6, Workstations auf Basic-Rang 4) · Master Trades · Quests (Easy-, Hard-Pool, Dauer-Quest) · Passive (letzter Rang Sonderwirkung) · Stationen enden auf „Station“ (`…_station`) · Items/Blöcke/Texturen · Stationen + Passive in `TradeReworkSections.java` (Extension) · Tweak-Datei in `tweaks/` · im Spiel getestet

Bis ein Beruf designt ist, läuft er mit seinen Vanilla-Trades auf dem Kern (Quests + Trades, README „Vanilla-Trades“).

- [ ] Farmer
- [ ] **Fisherman** – geplant 2026-09-27, Obsidian `Fisherman.md`: besondere Fische nur in bestimmten Biomen (Spieler angelt sie), Master-Station macht daraus Essen mit Effekten (Reichweiten-Sorbet, Eile …), Passive = selbst fischen, höherer Rang = seltenere Fänge (Schätze nur selten, sonst kannibalisiert es Angeln); Basic Trades, Quests, Stationen offen
- [ ] Shepherd
- [ ] Fletcher
- [ ] **Cartographer** – Obsidian `Cartographer.md`; im Code seit 2026-09-27: Basic 4 Erkundungsstation (Master-Workstation), Basic 5 Abenteuerkarte, Basic 6 Herausforderungskarte, Master 0–4 Dorfkarten; offen: Basic 0–3, Passive + Passive-Station, Tonscherben als Quest (vom Librarian verschoben)
- [ ] Cleric
- [ ] **Salvager** (ersetzt Armorer) – geplant 2026-09-26, Obsidian `Salvager.md`: Passive Smelting Station (nur Metall-Ausrüstung → Barren, nie Nuggets außer Kette, Diamant nicht, Netherite → Diamant-Teil + Scraps); Master-Workstation **Refinement Station**: Glas/Redstone + Eisen, Lapis/Schwarzstein + Gold → Grund-Variante, Rest per Steinmetzblock; Lapis-Texturen aus Abyssal Decor (starrysock, CC0) – Antwort auf Anfrage abwarten, crediten; Basic Trades im Code seit 2026-09-27: 0 Eisenketten, 1 Eisengitter, 4 Veredelungs- + Schmelzstation (Placeholder ohne Funktion), 5 Kupferader-Karte, 6 Eisenader-Karte, Name „Verwerter“; Schmelzstation als Passive umgesetzt 2026-09-27; offen: Basic 2–3, Master 1–4 (Veredelung), Quests, Balancing der Rang-Anteile
- [ ] **Veteran – Reste** (Obsidian `Veteran.md`): Basic Trades Rang 3, 6; Passive + Passive-Station offen; Waffenständer: Trichter/Komparator?
- [ ] **Runesmith / Runenschmied** (ersetzt Toolsmith, Obsidian `Runesmith.md`): Master 2 offen; Passive = Reparaturstation; Unbinding Station gestrichen (2026-09-27, entwertet Beute); Basic Trades 0–3, 5–6 + Easy/Hard-Quests offen; Balancing; Grafiken (Stationen, Vorlage); Pferde-/Nautilus-Rüstung upgradebar?
- [ ] Butcher
- [ ] **Leatherworker** – Ideensammlung 2026-09-27, Obsidian `Leatherworker.md`: Thema Reittiere; Master-Station = Reittier-Gadgets: Gleit-Pferderüstung (Elytra an Pferderüstung, Pferd gleitet), Happy-Ghast-Gondel (Boot dauerhaft unter dem Ghast, trägt Villager, kaputtmachen wie ein Vanilla-Boot → Boden klappt auf, Mobs fallen raus, nach ca. 5 s wieder da, kein Drop); Rest offen
- [ ] **Mason – Reste** (Grundgerüst umgesetzt 2026-09-26, Obsidian `Mason.md`): echte Grafiken statt Placeholder (Steinchen + Wegsteine fertig 2026-09-28); Balancing (alle Werte 1); Steinstatue entfernt (2026-09-27, kannibalisiert Rüstungsständer); Dorfbewohner-Statue Glücks-Wirkung (Aussehen fertig 2026-09-28: graue Villager-Figur je Typ); brauner Sandstein ganze Familie?; eigene Brechstation-GUI-Textur
- [ ] **Vanilla-Berufsnamen ersetzen** (erledigt 2026-09-27: Armorer → Verwerter, Toolsmith → Runenschmied, Weaponsmith → Veteran, über `entity.minecraft.villager.<beruf>` in den Sprachdateien): Anzeigename per Sprachdatei, Profession-ID bleibt Vanilla (Weltkompatibilität) – beim Bau prüfen

## 4. Gemeinsame Systeme

- [ ] **Kern erweiterbar machen** (Vorschlag 2026-09-28, ausführlich in [plan-core-extensibility.md](plan-core-extensibility.md)): Paket-Sortierung + Extension-Grafiken raus; offene Eintrags-Erweiterungen (Quest-Felder + Entdeckerkarten in die Extension); Item-Modifikationen allgemein über Components (ersetzt `enchantment`/`mob`); Handelsbedingungen `TradeRequirement` (ersetzt XP-Preis-Hook); neues Feature Trades in Raten (Eingabe/Ausgabe über Stapelgröße, Fortschritt bleibt)

- [ ] **Offen aus dem Sektions-Umbau** (2026-09-28): Punkte-Ökonomie im Kern mit nur zwei Sektionen (Upgrade-Kosten, Freischalt-Ränge) im Spiel prüfen; Leisten-Platz im Fenster, sobald mehr als drei Leisten sichtbar sind (Stat-Gruppe wächst heute einfach nach links – Pixel-Art-Entscheidung); Leistengröße der Kern-Sektionen (beide 1500 Punkte, füllen sich gemeinsam)
- [ ] **Smaragd-Ökonomie**: Smaragde als Raid-Belohnung (Villager werfen sie dem Spieler zu), Goldäpfel leichter beschaffbar (erledigt 2026-09-28: Zombie-Villager häufiger, Lohenstaub in Dorf-Brauständen; Heilen gibt keinen Handelsrabatt mehr = schon so)
- [ ] **Freizeit / aktive Glücklichkeitssuche** (alte Arbeitsroutine §3, nie gebaut): nach dem Aufwachen sucht der Villager gezielt sein schwächstes Glücks-Element, bevor er arbeitet – noch gewollt?
- [ ] **Wenn alles voll ist**: Freizeit o. ä. statt wirkungslosem Weiterarbeiten – vertagt
- [ ] Bestand pro Zeile im Handelsfenster anzeigen (bewusst ausgelassen seit 2026-09-23)
- [ ] Verbesserungsstation auch für Rohre anderer Mods (Item-Capability)
- [ ] Performance: mit 100+ Villagern profilen (Scan, Stationssuche)
- [ ] Offene Designfrage aus altem Plan: Block-Trades nur als Block→Block-Umwandlung? – prüfen, ob noch gewollt

## 5. Eigenständig: Urnen-System (könnte eigene Mod werden)

- [ ] Stirbt ein benannter Mob: Urne mit Inventar, Drops und Wiederherstellungsdaten statt normaler Drops
- [ ] Orchid of Resurrection belebt den Mob aus der Urne
- [ ] Hexen werden neutral durch Lohenruten und wandeln Wither-Rosen in Orchids um
- Ohne Abhängigkeit zum Villager-System bauen

## 6. Ideen (noch nicht entschieden, aus Obsidian „Improved Villagers Mod.md“)

- Kupfergolem als „Hund“ des Villagers
- Eisengolems laufen bevorzugt zu Villagern
- Villager individualisierbar: gefärbte Lederrüstung ausrüsten wie beim Rüstungsständer
- Denkblase: Villager zeigt das Item oder den Block, den er gerade will

## Risiken im Blick

- Versionsdrift 26.1 → 26.2 (bewusst auf 26.1): Mixins flach halten
- Andere Villager-Mods hooken dieselben Stellen – Kompatibilität nicht garantiert
