# Villager Overhaul – Arbeitsregeln

Wie an diesem Projekt gearbeitet wird. Überblick: [README.md](README.md); Features und Geplantes je Baustein: [docs/](docs/); Zahlen, Tests, Grafik-Bedarf: Obsidian (siehe „Dokumente“).

**Leitidee:** Jeder Villager bekommt ein Vanilla-artiges Rework, das sich anfühlt wie das nächste Minecraft-Update. Neue Blöcke und Items erweitern, sie greifen nicht ins Vanilla-Erlebnis ein. Game-Design entscheidet der Nutzer; Beratung nur bei technischen Folgen oder Widersprüchen.

## Festgelegt

- NeoForge 26.1.0.19-beta, Minecraft 26.1, Java 25. Bewusst nicht die neueste Version; ein Upgrade nur auf ausdrücklichen Wunsch.
- Alles Versionsabhängige (Registries, Mixins, Brain/AI, Datapacks, Rendering) in den 26.1-Quellen nachsehen, nie aus dem Gedächtnis. Keine erfundenen APIs.
- Code, Bezeichner, Kommentare, Commits, Translation-Keys: Englisch. Erklärungen und Rückfragen: Deutsch. UI-Texte nur über Translation-Keys.
- Kommentare im Code nur für das nicht offensichtliche Warum.
- Repo = Git (Branch pro Aufgabe, Commit pro Schritt, Push nur auf Wunsch).

## Drei Mods (plus Smart Villager)

Ein Gradle-Projekt, Abhängigkeit nur in eine Richtung:
- **Kern** `core/` (`villageroverhaul`): alles, was für jeden Villager gleich gilt – Leveln durch Arbeit, Sektionen, Handelsfenster, Restock, Glück, Arbeitsplätze, Claim, Handelsblock. Ohne eigene Einträge laufen Berufe (auch fremder Mods) mit ihren Vanilla-Trades.
- **Trade Rework** `trade-rework/` (`vo_trade_rework`): die designten Berufe samt Items, Blöcken, Stationen, Quests/Masteries/Passive. Hängt sich über `api/ExtensionHooks` in den Kern ein; der Kern importiert nie etwas daraus (fehlt ihm Berufswissen: neutraler Haken in `api/`).
- **Smart Iron Golem** `smart-golem/` (`smartgolem`): Eisengolem-KI, braucht weder Kern noch Extension.
- Einordnen: gilt es für jeden Villager → Kern; gehört es zu einem designten Beruf → Extension; betrifft es den Golem → smart-golem.
- Registry-IDs: Kern `villageroverhaul`, Extension `vo_trade_rework`; Einträge der Extension liegen in Kern-Registries (`data/vo_trade_rework/villageroverhaul/exchange/`).

## Vanilla-Erhalt: so spät und so eng wie möglich

Jedes Vanilla-Feature, das wir nicht ausdrücklich ersetzen, bleibt.
- **Rangfolge:** erst Daten/neutral geschaltete Werte, dann NeoForge-Event/API, erst dann ein Mixin – und nie eines, das eine ganze Methode abbricht, wenn ein einzelner Aufruf per `@WrapOperation` reicht.
- **Spät:** der Eingriff sitzt an der letzten Stelle, an der sich das Verhalten noch ändern lässt (Beispiel: beim Handelsöffnen laufen alle Vanilla-Schritte, nur das geöffnete Fenster wird getauscht).
- **Eng:** ersetzt wird nur, was anders sein muss (Beispiel: beim Restock wird nur das Auffüllen übersprungen, Gang zum Arbeitsblock bleibt).
- **Nachfragen in Spielersprache**, wenn unklar ist, ob ein Vanilla-Feature bleibt, wegfällt oder ersetzt wird: was bleibt, was wird abgeschaltet, was ersetzt es, welche Nebenwirkung spürt der Spieler. Keine Klassen- oder Methodennamen als Entscheidungsgrundlage.
- **Design-Test „kein Kannibalisieren“:** Trades nehmen Fleißarbeit ab und dürfen Chores ersetzen, nie ein Vanilla-Feature, das selbst Gameplay ist. Der Villager multipliziert die Arbeit des Spielers.

## Arbeitsweise pro Aufgabe

1. **Verstehen:** Aufgabe in eigenen Worten, offene technische Entscheidungen benennen.
2. **Planen:** Weg als zusammenhängenden Ablauf erzählen (Startpunkt, Reihenfolge nach Abhängigkeit, Vanilla-Eingriffe). Bei mehreren Dateien Freigabe abwarten.
3. **Umsetzen:** in sich lauffähige Schritte, vollständige Dateien statt Fragmente.
4. **Einordnen:** erklären, wie es im laufenden Spiel arbeitet, was testbar ist, welche Nebenwirkungen, was als Nächstes folgt.
5. **Nachziehen:** `docs/`, Obsidian (Tweak-Werte, Fehlende Texturen, Testliste, Berufs-Tabelle im Entwicklungsplan), Drive-Spiegel (siehe unten). Dann nach einem Entwicklungsplan fragen (siehe unten).

Verbindlich:
- Der Plan ist Richtschnur; ist eine Abweichung sauberer, hat sie Vorrang – kurz begründen.
- Kein Scope-Creep: nur das Geforderte, Zusatzideen am Ende nennen.
- Performance ist Pflicht: Villager-Logik läuft pro Tick und Entity; Pathfinding, Entity-Suche, Block-Scans benennen und Alternativen nennen.
- Client/Server strikt getrennt; Zustand serverseitig, UI-Daten per Netzwerkpaket.
- Neuer Villager-Zustand wird serialisiert; Weltkompatibilität nicht stillschweigend brechen (Ausnahme nur nach Absprache, zuletzt 2026-09-28).

## Erklärstil

Erzählend wie am Whiteboard: vom Auslöser dem Ablauf folgend bis zum sichtbaren Ergebnis, in der Reihenfolge, in der die Teile im laufenden Spiel zusammenspielen. Meta-Sicht statt Code-Nacherzählung (Rolle jeder Datei, warum Java/JSON/Datapack/Mixin-Config). Neues mit Bestehendem und Geplantem verbinden. Bezeichner nur, wo sie zum Wiederfinden nötig sind. Prosa mit Zwischenüberschriften, Listen nur für echte Aufzählungen. Code in Blöcken mit vollem Pfad als Überschrift, Einordnung vor dem Code.

## Dokumente: jede Info genau einmal, jede Datei nur für ihren Zweck

**Repo** (wenige, gebündelte Dateien):
- `README.md`: Master-Übersicht, ganz knapp – Bausteine, wo was liegt, Befehle, Grundentscheidungen/Fallen. Keine Feature-Details.
- `docs/core.md`, `docs/trade-rework.md`, `docs/smart-golem.md`, `docs/smart-villager.md`: je Baustein Tabellen des Vorhandenen (Feature | Was | Wo im Code, nach Themen/Berufen sortiert) und **Geplant** (Offenes, Ideen, Risiken; in `trade-rework.md` unter jedem Beruf, nicht gesammelt). Hier – und nur hier – steht, wo etwas im Code liegt. Neues Feature → Zeile in „Vorhanden“ (und aus „Geplant“ streichen); Aufgeschobenes → sofort in „Geplant“.
- `arbeitsregeln.md`: diese Datei (über `CLAUDE.md` in jeder Session geladen).
- `assets/`: rohe Grafiken, nicht in `src`.

**Obsidian** (`C:\Users\(09) Obsidian\(02) Jakobs Obsidian\Improved Villagers\`, Vault „Jakobs Vault“) – Quelle für Design, Zahlen, Tests, Grafik-Bedarf:
- `<Beruf>.md`: Design und Trade-Tabellen (Base Rang 0, Max Maximalrang; Preis, Output, Stock; fehlt ein Wert, gilt 1), Werte 1:1 in die Datapack-JSONs. Vor Arbeit an einem Beruf lesen.
- `02_Tweak-Werte.md`: nur Zahlen für Leute, die die Features kennen, aber nicht den Code – nach Themen/Berufen sortiert, so kompakt wie möglich, **ohne** Erklärungen, Wirkungs-Prosa, Code-Spalte oder Änderungshistorie (die Tabellen: Name | Wert | Einheit). Neuer Wert im Code → sofort Zeile; Änderung → nachziehen. Die Zahlen stehen nur dort.
- `03_Fehlende Texturen und Modelle.md`: nur eine Liste, was an Grafik fehlt oder provisorisch ist, nach Kategorien (Beruf/Art) sortiert, Stichwort + Dateiname/Größe, keine Erklärungen (was das Feature ist, steht in `docs/`). Neues Asset ohne Grafik → sofort Zeile; Grafik da → Zeile löschen.
- `04_Testliste.md`: siehe unten.
- `01_Entwicklungsplan.md`: Plan des Nutzers; nur die Tabelle „Stand je Beruf“ pflegt die KI selbst (siehe unten), sonst ändert die KI dort nichts.
- Stil: Stichpunkte statt Fließtext, so kurz wie möglich, keine Implementierungs-Tagebücher („Umbau …, Stand im Code“) und keine Code-Orte (die stehen in `docs/`), Wiki-Links zwischen Notizen sind ok.

**Testliste führen (jede Session, die Features baut):** Code analysieren und `04_Testliste.md` ergänzen – kurz, nur das Wichtigste.
- Pro Feature eine Zeile Grundfunktion (ja/nein), nicht „funktioniert es schön“ – das sieht der Nutzer selbst.
- Dazu die Edge Cases und größten Unsicherheiten, die der Code verrät: Stellen, an denen Optik/Verhalten im Spiel am ehesten falsch ist (Hitboxen, Ausrichtung, Pathfinding, Multiplayer/Client-Server, Weltladen/Persistenz, leere/volle Zustände, Zombie-Umwandlung, Rangwechsel mitten drin, Mod-Kollisionen).
- Getestetes streichen, Veraltetes löschen. Liste kurz halten.

**Drive-Spiegel:** nach jeder Aufgabe Projekt (ohne Build/Run/.gradle/.idea) und Obsidian-Notizen einseitig nach `G:\Meine Ablage\Projekte\Improved Villagers` spiegeln (PowerShell, `robocopy /MIR`).

**Entwicklungsplan:**
- Tabelle „Stand je Beruf“ (Beruf × Quests/Trades/Master/Passive, je Zelle vier Slots in zwei Zeilen à zwei: oben links geplant, oben rechts implementiert, unten links getweaked, unten rechts texturiert (alle Texturen final); 🟩 fertig, 🟨 teilweise, ⬜ offen, – nicht zutreffend): die KI aktualisiert sie **automatisch bei jedem Feature**, das sie hinzufügt, ändert oder entfernt (Slot setzen); „texturiert“ folgt `03_Fehlende Texturen und Modelle` (nur wenn nichts mehr fehlt oder provisorisch ist, ganz fertig). „Getweaked“ setzt sie nur, wenn der Nutzer Zahlen freigegeben hat; Neues in der Tabelle als geplant eintragen, sobald es im Design steht.
- Alles übrige in der Datei führt der Nutzer. Am Anfang jeder Session und nach Abschluss einer Aufgabe (Commit) fragt die KI, ob er einen Entwicklungsplan für die anstehende Arbeit will; bei Ja kompakt im bestehenden Stil (abhakbare Blöcke, Stichpunkte) eintragen.
