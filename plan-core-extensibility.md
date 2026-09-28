# Entwicklungsvorschlag: Kern erweiterbar machen (2026-09-28)

Auftrag für einen Entwicklungs-Chat. Ziel: Der Kern (`core/`, `villageroverhaul`) soll so gebaut sein, dass fremde Programmierer ihn holen und eigene Trades, Bedingungen und Ideen umsetzen können, **ohne den Kern anzufassen**. Dafür fliegt Berufswissen der Extension aus dem Kern, und drei feste Sonderfälle werden zu registrierbaren Typen. Dazu kommt ein neues Kern-Feature: Trades in Raten.

Entstanden in einer Analyse-Sitzung. Befunde und Begründungen stehen kurz unter „Warum“ in jeder Phase.

---

## Vor dem Start lesen

- `villager-overhaul-projektrahmen.md`: Arbeitsweise **Verstehen → Planen (Plan freigeben lassen) → Umsetzen → Einordnen**, Regeln, „Zwei Mods: Kern + Extension“, Vanilla-Erhalt.
- `README.md`, `active-development.md`
- Die Kern-Pakete `api/`, `data/`, `trade/`, `menu/`, `section/`
- Extension: `TradeReworkSections`, `quest/QuestLogic`, `trade/ExplorerMaps`, `veteran/MobWeapons`, `librarian/ExperienceBottles`

**Zielversion ist MC/NeoForge 26.1.** Jede Vanilla-Klasse, die hier genannt wird, **vor der Nutzung in den 26.1-Quellen nachsehen**. Nichts aus dem Gedächtnis annehmen. Das gilt besonders für die Punkte, die unten mit ⚠ markiert sind.

**Kompatibilität:**
- Weltspeicherstände dürfen **nicht** brechen. Neue Attachments sind neu, bestehende bleiben lesbar.
- Das JSON-Format der Einträge (`data/*/villageroverhaul/exchange/`) darf sich ändern. Alle betroffenen JSONs liegen im Repo und werden im selben Schritt migriert, ohne Kompatibilitäts-Code für das alte Format.

**Pro Phase:** ein Commit, danach README / active-development nachziehen. Neue einstellbare Zahlen kommen in Obsidian `Tweak-Werte.md` + `tweaks/general.md`, fehlende Grafiken in Obsidian `Fehlende Texturen und Modelle.md`.

---

## Leitprinzip

> **Der Kern kennt keine konkreten Dinge, sondern registrierte Typen mit Codec.**
> Eintrags-Zusatzdaten → Erweiterungstypen. Item-Modifikationen → Component-Prüfung. Bedingungen außerhalb der Items → `TradeRequirement`-Typen.

Danach entfallen alle drei globalen Setter in `api/ExtensionHooks`:
- `setMobWeaponComponent`
- `setPlayerXpCost`
- `setExplorerMaps`

Übrig bleiben nur `registerSection`, `registerStation` und die neuen `register…`-Methoden. Das „der letzte Setter gewinnt“-Problem, das zwei Extensions heute still gegeneinander ausspielen würde, verschwindet damit.

---

## Phase 1: Sortierung (reines Verschieben, kein Verhalten)

**Warum:** In `station/` stecken zwei Themen. Einige Klassen liegen im falschen Paket, und die Grafiken der Extension liegen im Kern.

- `station/` teilen. Neues Paket `interaction/` (Name frei wählbar) für die Interaktion zwischen Spieler und Villager: `TradingBlock`, `TradingBlocks`, `TradingBlockCall`, `GoToTradingBlock`, `EmeraldClaimTool`, `ManualClaims`, `EmeraldLure`. In `station/` bleiben: `StationClaims`, `StationFocus`, `ProfessionStations`, `WorkAtStation`, `PausedAtStation`, `UnemployedPriority`.
- `ProfessionLock`: prüfen, ob es besser zu `trade/` oder `progression/` passt (es wird von `TradeEvents` benutzt und betrifft den Beruf, nicht Stationen).
- `trade/RequiredEnchantmentCost` wird in Phase 3 ohnehin ersetzt, also hier nicht anfassen.
- **Grafiken:** `master_productivity_*`, `passive_productivity_*` wandern von `core/…/textures/gui/villager/` in die Extension (Namespace `vo_trade_rework`). `TradeReworkSections` verweist dann nicht mehr auf `VillagerOverhaulMod.MODID`.
- `passive_ability_group.png` (Badge-Rahmen, `VillagerGuiTextures.BADGE`): ansehen. Ist es ein neutraler Rahmen, bleibt er im Kern und wird in `badge_frame` umbenannt. Enthält es Passiv-Grafik, bekommt `SectionDefinition` ein optionales Badge-Sprite, und die Datei wandert in die Extension.
- Mixin-Config und `package-info.java` nachziehen, Build + `runClient` beider Mods kurz starten.

---

## Phase 2: Offene Eintrags-Erweiterungen

**Warum:** `data/ItemExchange` ist ein geschlossener Record. Die Felder `pool`, `input_variants` und `input_enchantment_variants` liest nur `QuestLogic` der Extension. `explorer_map` (samt `ExplorerMap`, `VeinType` mit fest eingebauten Y-Bändern) kann der Kern nicht einmal erfüllen, das macht der Hook der Extension. Jede neue Extension-Idee bräuchte heute ein neues Kern-Feld.

**Ziel-Form im JSON:**
```json
{
  "profession": "minecraft:librarian",
  "section": "vo_trade_rework:quests",
  "base_input": { … }, "max_input": { … }, "base_output": { … }, "max_output": { … },
  "base_max_uses": 1, "max_max_uses": 1,
  "extensions": {
    "vo_trade_rework:quest": { "pool": "hard", "input_variants": ["minecraft:oak_sign", "…"] },
    "vo_trade_rework:explorer_map": { "destination": "#minecraft:village", "decoration": "…", "name": "…" }
  }
}
```

**Kern:**
- `api/ExchangeExtensionType<T>(Identifier id, Codec<T> codec)` wird über `ExtensionHooks.registerExchangeExtension(type)` registriert. Die Registrierung ist thread-safe wie `Sections`.
- `ItemExchange` bekommt `ExchangeExtensions extensions` (eine Map Typ → Wert). Der Codec dispatcht über die Map-Schlüssel. Ein unbekannter Schlüssel ist ein **Ladefehler mit klarer Meldung**, kein stilles Ignorieren (so fallen Tippfehler im Datapack sofort auf). Zugriff: `exchange.extension(TYPE)` → `Optional<T>`.
- **Ausgabe ersetzen:** ein Interface `api/ResultOverride` mit `Output resultFor(Villager, Identifier entryId)`, wobei `Output` aus Stack und `soldOut` besteht (heute `ExtensionHooks.MapOutput`). Implementiert es ein Erweiterungswert, benutzt `VillagerOffers.toOffer` (heute Zeile ~147) dessen Ergebnis statt `base_output`.
- Aus dem Kern entfernen: `ItemExchange.pool/inputVariants/inputEnchantmentVariants/explorerMap`, `data/ExplorerMap` (mit `VeinType`), `ExtensionHooks.ExplorerMapBuilder/MapOutput/setExplorerMaps/explorerMap`.
- ⚠ Prüfen: Wird die Exchange-Registry zum Client synchronisiert (`ModDataPackRegistries`, `SectionEntries` spricht von Client- und Server-Kopie)? Dann braucht der Erweiterungs-Codec auch auf dem Client alle Typen. Das ist gegeben, solange beide Seiten die Extension laden.

**Extension:**
- `quest/QuestEntry` (Record: `pool`, `inputVariants`, `inputEnchantmentVariants`) als Erweiterungstyp `vo_trade_rework:quest`. `QuestLogic` liest `exchange.extension(QuestEntry.TYPE)`.
- `ExplorerMap` + `VeinType` ziehen nach `trade/` bzw. `explore/` der Extension, als Typ `vo_trade_rework:explorer_map`, der `ResultOverride` implementiert und intern `ExplorerMaps.outputFor` aufruft.
- **JSON-Migration:** alle `exchange/*.json` mit `pool`, `input_variants`, `input_enchantment_variants` oder `explorer_map`. Das betrifft etwa 50 Dateien, also ein Skript benutzen und nicht von Hand arbeiten.

---

## Phase 3: Item-Modifikationen allgemein (Components)

**Warum:** `ItemAmount` kennt zwei fest eingebaute Modifikationen: `enchantment` (beliebige Stufe) und `mob` (Mobwaffe der Extension, über einen globalen Hook). Namen, Tränke, Coatings eines fremden Mods usw. gehen nicht. Seit 1.20.5 ist jede Modifikation eines Items ein **Data Component**. Wer Components allgemein prüfen kann, kann jede Modifikation prüfen, auch die von Mods, die es noch nicht gibt.

**Zwei Arten zu vergleichen:**
- **exakt**: „hat genau diesen Wert“, z. B. Mobwaffe = Zombie, Gold-Coating, Name = „Bob“. Das kann Vanilla schon (`ItemCost` + `DataComponentExactPredicate`).
- **teilweise**: „erfüllt eine Bedingung“, z. B. Seelenläufer in beliebiger Stufe, irgendein Name, irgendein Coating. Dafür nimmt man Vanillas Item-Prädikat-System (das der Advancements) mit seinen registrierbaren Prädikat-Typen. ⚠ Klassennamen in 26.1 nachsehen (in 1.21.x: `DataComponentMatchers`, `DataComponentPredicate`, Registry `DATA_COMPONENT_PREDICATE_TYPE`). Prüfen, welche Prädikat-Typen Vanilla mitbringt (Verzauberungen mit Stufenbereich, Trank, Haltbarkeit, Custom Data …) und ob Mods eigene registrieren dürfen.

**Neue Form von `ItemAmount`:**
```json
{
  "item": "minecraft:enchanted_book",
  "count": 1,
  "components": { "vo_trade_rework:mob_weapon": "minecraft:zombie" },   // exakt – auch angezeigt
  "match":      { "minecraft:enchantments": [ { "enchantments": "minecraft:soul_speed" } ] },  // teilweise
  "display":    { "minecraft:stored_enchantments": { "minecraft:soul_speed": 1 } }         // optional: Beispiel für die Anzeige
}
```
(Die Feldnamen der Prädikate richten sich nach dem, was 26.1 wirklich hat, siehe ⚠.)

**Kern:**
- `ItemAmount`: die Felder `enchantment` und `mob` fallen weg, dazu kommen `components` (exakt), `match` (teilweise) und `display` (Beispielwerte, nur für die Anzeige).
- `RequiredEnchantmentCost` wird durch `ComponentCost` ersetzt (Paket `data/` oder `trade/cost/`):
  - `of(ItemAmount, registries)` baut die Vanilla-`ItemCost`. Ihr angezeigtes Item trägt `components` + `display` sowie **ein** Marker-Component `villageroverhaul:cost_match`, das die eigentliche Prüfung enthält (exakter Teil + teilweiser Teil, mit Codec und StreamCodec, weil das Angebot zum Client geht).
  - `matches(cost, actual)`: exakt(`components`) UND teilweise(`match`). Die `display`-Werte zählen **nicht**.
- `ItemCostMixin` bleibt ein Mixin an derselben Stelle: markiert → `ComponentCost.matches`, sonst Vanilla.
- `VillagerMenu.cloneOfferItem` entfernt den neuen Marker statt `REQUIRES_ENCHANTMENT`.
- Optional, klein: ein eigener Prädikat-Typ `villageroverhaul:has_component` („hat dieses Component, egal welcher Wert“) für „irgendein Name / irgendein Coating“, falls Vanilla keinen hat.
- Aus dem Kern entfernen: `ExtensionHooks.setMobWeaponComponent/mobWeaponComponent`, Marker `REQUIRES_ENCHANTMENT`.
- `ExchangeScaling.scale` reicht die neuen Felder unverändert durch (heute: `enchantment()`, `mob()`).

**Extension:**
- Veteran: `"mob": "minecraft:zombie"` wird zu `"components": { "vo_trade_rework:mob_weapon": "minecraft:zombie", "minecraft:rarity": "uncommon" }`. Heute setzt `RequiredEnchantmentCost` die Seltenheit UNCOMMON automatisch dazu, künftig steht sie im JSON.
- Librarian / Runesmith: `"enchantment": "…"` wird zu `match` + `display`.
- `QuestLogic`: `input_enchantment_variants` baut künftig ein `match` pro gewählter Verzauberung statt `ItemAmount(…, enchantment)`.
- **Tooltip:** Bei einer teilweisen Bedingung zeigt die Zeile ein Beispiel-Item. Eine Tooltip-Zeile „beliebige Stufe“ bzw. allgemein „erfüllt: …“ ergänzen, sonst glaubt der Spieler, es muss genau Stufe I sein. Translation-Keys benutzen.

---

## Phase 4: Handelsbedingungen (`TradeRequirement`)

**Warum:** Der XP-Preis der Erfahrungsflaschen ist schon eine Bedingung plus Wirkung (`VillagerMenu.PlayerXpCostResultSlot`: `mayPickup` = genug XP?, `onTake` = XP abziehen). Er ist aber fest auf XP verdrahtet, läuft über einen globalen Setter, und die Regel „55 XP pro Flasche“ steht als Code in der Extension.

**Konzept:**
```java
public interface TradeRequirement {
    boolean test(TradeContext ctx);                 // darf gehandelt werden?
    default void apply(TradeContext ctx) {}         // was passiert beim Handel (nur Server)
    Component describe(TradeContext ctx);           // Hinweis im Fenster
    TradeRequirementType<?> type();
}
// TradeContext: Player, Villager, das Angebot, Level, wie oft gerade gehandelt wird / wie viele Ergebnisse
```
- Die Registry `villageroverhaul:trade_requirement_type` wird befüllt über `ExtensionHooks.registerTradeRequirement(type)` (oder eine echte NeoForge-Registry, ⚠ prüfen, was sauberer ist). Jeder Typ hat einen `MapCodec` (für JSON) und einen `StreamCodec` (für den Client).
- Neues Kern-Feld in `ItemExchange`: `"requirements": [ { "type": "…", … } ]` (optional, leer als Standard).
- `ResolvedExchange` trägt die Requirements weiter. `RowView` bekommt sie für den Client. Das Menü muss zum aktiven Angebot die Requirements finden: auf dem Server über eine Parallel-Liste in `VillagerOffers`, auf dem Client über `RowView`.
- **Mitgelieferte Typen im Kern:** `villageroverhaul:player_xp` (`points`, optional `per_result: true`). XP ist allgemein, also gehört der Typ in den Kern. Health, Hunger, „Tiere in der Nähe“ usw. **nicht** bauen (kein Scope Creep). Sie sind die Beispiele, für die das System da ist.
- `PlayerXpCostResultSlot` wird zu `RequirementResultSlot`:
  - `mayPickup`: alle `test`.
  - `onTake`: alle `apply`.
  - Die Neuprüfung pro Iteration in `quickMoveStack` bleibt.
- `trade/TradeActions` (`/vo trade`) prüft und wendet ebenfalls an.
- **Anzeige:** Eine Zeile mit unerfüllter Bedingung wird ausgegraut. Tooltip mit allen `describe`-Texten. Eine kompakte Anzeige in der Zeile (z. B. „55 XP“) nur, wenn es ins Layout passt. Sonst reicht der Tooltip, und die Frage wird in active-development notiert.
- Aus dem Kern entfernen: `ExtensionHooks.setPlayerXpCost/playerXpCost`.
- **Extension:** `ExperienceBottles.playerXpCost` entfällt. Die Flaschen-Trades bekommen `"requirements": [ { "type": "villageroverhaul:player_xp", "points": 55, "per_result": true } ]`. Die 55 steht dann im JSON und in `tweaks/`, nicht mehr als Konstante (`XP_POINTS` bleibt für den Flaschenwurf-Mixin, beide Stellen müssen übereinstimmen, Kommentar setzen).

**Entscheidungen (Empfehlung, vom Nutzer bestätigen lassen):**
1. **Client/Server:** `test` läuft auf beiden Seiten (für die Shift-Klick-Vorhersage), `apply` nur auf dem Server. Kann ein Typ auf dem Client nicht sicher prüfen, gibt er dort `true` zurück, der Server lehnt ab, und Vanilla synchronisiert den Slot zurück.
2. **Rang-Skalierung von Requirements:** vorerst nein.
3. **Reihenfolge:** alle `test` vor dem ersten `apply`. Kein Teil-Anwenden.

---

## Phase 5: Trades in Raten (neues Kern-Feature)

**Warum:** Ein Eintrag kann heute nicht mehr verlangen oder geben, als in einen Slot passt. 90 Smaragde als Ausgabe oder 3 Eisenschwerter (nicht stapelbar) als Preis funktionieren nicht. Die Extension braucht es vorerst nicht. Es ist Kern-Infrastruktur für fremde Programmierer, und der Vanilla-Katalog erzeugt so etwas nie (`VanillaScaling` deckelt auf einen Stapel).

**Verhalten (Wunsch des Nutzers):**
- **Eingabe in Raten:** Braucht ein Trade mehr, als in den Bezahl-Slot passt (Anzahl > max. Stapelgröße des Items, z. B. 3 Eisenschwerter oder 150 Kopfsteinpflaster), legt der Spieler nacheinander hinein. Jede Rate wird verbucht, und der **Handelspfeil zwischen Bezahl- und Ergebnis-Slot füllt sich wie ein Ofen-Fortschrittsbalken**. Ist alles bezahlt, erscheint das Ergebnis.
- **Ausgabe in Raten:** Gibt ein Trade mehr aus, als auf einen Stapel passt (z. B. 90 Smaragde), erscheinen zuerst 64. Nimmt der Spieler sie heraus, erscheint der Rest (26).
- **Fortschritt bleibt erhalten:** Schließt der Spieler das Fenster oder klickt einen anderen Trade an, geht nichts verloren. Beim nächsten Öffnen oder Anklicken steht der Balken dort, wo er war, und ausstehende Ausgaben liegen wieder bereit.
- Ein- und Ausgabe in Raten sind kombinierbar (3 Schwerter → 90 Smaragde).
- **Aktivierung automatisch**, sobald eine Menge die Stapelgröße übersteigt, ohne JSON-Schalter.

**Umsetzungsskizze:**
- **Zustand:** neues Attachment am Villager, `TradeProgress`: pro Spieler-UUID pro Eintrags-ID die bereits gelieferte Menge (Eingabe A, Eingabe B) und die noch ausstehende Ausgabe. NBT-gespeichert. Getrennt von `VillagerState`, weil es pro Spieler ist und nicht an alle Clients synchronisiert werden soll. `/vo reset` und `SectionLogic.onReset` löschen es mit.
- **Container:** Vanillas `MerchantContainer.updateSellItem` zeigt ein Ergebnis nur, wenn die Bezahl-Slots die ganzen Kosten decken, und das geht bei Raten nie. Deshalb: eine Unterklasse von `MerchantContainer` (⚠ prüfen, ob sie erweiterbar ist und welche Methoden sie hat), die sich für Raten-Angebote anders verhält. Nicht-Raten-Angebote bleiben **exakt Vanilla**.
- **Einzahlen:** Liegt passende Ware im Bezahl-Slot, während ein Raten-Angebot gewählt ist, nimmt der Server sie (höchstens bis zum Restbetrag) heraus und bucht sie in `TradeProgress`. Die Prüfung läuft über `ComponentCost.matches` aus Phase 3, damit Modifikationen auch bei Raten gelten. Nur auf dem Server, der Client sieht es per Sync.
- **Verbuchen:** Der Handel zählt als *ein* Handel (Bestand −1, `SectionLogic.onUse`, `TradeWithVillagerEvent` genau einmal) in dem Moment, in dem die letzte Rate eingezahlt ist. Ab dann ist die Ausgabe „geschuldet“ und liegt in `TradeProgress`. So kann das Herausnehmen der Ausgaben nichts doppelt auslösen. ⚠ Prüfen, wie `MerchantResultSlot.onTake` → `notifyTrade` bei Teil-Ausgaben unterdrückt bzw. nur einmal ausgelöst wird.
- **Requirements (Phase 4):** `test` vor der ersten Rate und vor dem Verbuchen, `apply` beim Verbuchen, mit der Gesamtmenge.
- **Fortschritt zum Client:** für das gewählte Angebot per `DataSlot` im Menü (wie der Ofen seinen Brennfortschritt synchronisiert), für die Zeilenliste über `RowView` (angefangene Trades in der Liste markieren).
- **Anzeige:** Der große Pfeil zwischen den Slots bekommt eine Füllgrafik (`trade_arrow_progress`). Die fehlt noch, also Platzhalter nehmen und in Obsidian `Fehlende Texturen und Modelle.md` eintragen. In der Zeile steht die volle Menge (90, 3).
- ⚠ **Speicherfalle:** Vanilla speichert die `MerchantOffers` des Villagers in dessen NBT. Der persistente `ItemStack`-Codec begrenzt die Anzahl (in 1.21.x auf 1–99). Ein Angebot mit 150 als Kosten oder Ergebnis könnte beim Speichern scheitern. Prüfen, und dann den `MerchantOffer` mit auf eine Rate gedeckelten Stacks bauen, während die echten Gesamtmengen in `ResolvedExchange`/`RowView` stehen. `TradeEvents` ersetzt die Liste beim Laden ohnehin.
- `/vo trade` (`TradeActions`): zahlt alles auf einmal, wenn es im Inventar liegt.

**Entscheidungen (Empfehlung, vom Nutzer bestätigen lassen):**
1. **Fortschritt pro Spieler** (nicht geteilt): Zwei Spieler zahlen getrennt ein.
2. **Einzahlen automatisch** beim Hineinlegen (kein Knopf). Alternative: ein „Einzahlen“-Knopf, der versehentliches Einzahlen verhindert.
3. **Rückgabe:** Eingezahltes ist weg und nicht zurückholbar (wie bei einem bezahlten Trade). Stirbt der Villager, verfällt der Fortschritt.
4. **Bestand leer während des Einzahlens** (z. B. nach einem Rang-Wechsel): Angefangenes darf zu Ende gezahlt werden, und das Verbuchen wartet notfalls auf Restock. Neu *anfangen* geht nur mit Bestand > 0.
5. **Rang-Wechsel mitten drin:** Die Gesamtkosten werden neu berechnet, das bereits Gelieferte zählt weiter. Ist das Gelieferte ≥ die neuen Kosten, gilt der Trade als bezahlt.
6. **Eintrag verschwindet** (Datapack geändert, Sektion verloren): Der Fortschritt wird beim nächsten Neuaufbau still verworfen.

**Test (als Datapack-Eintrag im Kern-Testlauf oder als Extension-Test-JSON, danach wieder entfernen):** 3 Eisenschwerter → 1 Smaragd, 1 Smaragd → 90 Smaragde (nur zum Testen), 3 Schwerter → 90 Smaragde. Dabei: Fenster schließen, anderen Trade wählen, Shift-Klick, zwei Spieler.

---

## Nicht in diesem Auftrag (nur notieren, nicht bauen)

- **API von internen Klassen entkoppeln:** `api/SectionLogic` zieht `StandardSection`, `RestockService`, `VillagerState` mit, `SectionOffer` zieht `trade/ResolvedExchange` mit, und `SectionClientLogic` liegt in `client/ui/` statt `api/client/`. Großer Umbau, sinnvoll erst nach diesen Phasen.
- **Höchstens eine BADGE-Sektion** (`Sections.register` wirft sonst): Eine zweite Extension mit eigenem Passiv würde beim Start abstürzen.
- **Weitere Requirement-Typen** (Leben, Hunger, Tiere in der Nähe …): Beispiele für fremde Programmierer, nicht bauen.
- Eine Beispiel- oder Dokumentationsseite „Wie schreibe ich eine Extension“.

## Was bewusst im Kern bleibt (nicht verschieben)

- Der ganze Stations-Stapel (`StationDefinition`, `StationClaims`, `StationFocus`, `WorkAtStation`, `PausedAtStation`, `heldByStation`): Der Kern allein benutzt ihn nicht, aber er ist die API, an die jede Extension andockt.
- Die Hooks von `SectionLogic` (`isPending`, `onWorkScan`, `refresh`, `onAction`, `onReset`, `describe`), `SectionOffer.slot`, `SectionActionPayload`, `SectionClientLogic`, `Display.BADGE`, `hiddenMeter`.
- `second_input`: Der Vanilla-Katalog benutzt es selbst.
- `MissingTrade`: ein Design-Werkzeug für jeden Extension-Autor.
