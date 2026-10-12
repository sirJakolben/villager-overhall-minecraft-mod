# Smart Villager (`smart-villager/`, `smartvillager`)

Unterste Stufe der Leiter (Vanilla → Smart Villager → Progress → Professions): kleine Spieler-Werkzeuge und Welt-Änderungen für Vanilla-Villager, die **nichts anderes** brauchen. Progress und Professions bauen darauf auf und docken über `api/SmartVillagerApi` an; Smart Villager importiert nie etwas daraus. Zahlen: Obsidian `02_Tweak-Werte`.
Pfade: Java `smart-villager/src/main/java/com/smartvillager/base/`, Daten `smart-villager/src/main/resources/`.

## Vorhanden

| Feature | Was | Wo |
|---|---|---|
| Claim per Smaragd | Smaragd-Rechtsklick wählt den Villager, Klick auf einen Arbeitsblock weist ihn zu; ein vorheriger Besitzer wird verdrängt und behält Level, Punkte, Beruf. Läuft auf Vanillas Job-Site; Progress meldet sich als `ClaimHandler` für seine Stationen an | `claim/EmeraldClaimTool`, `ManualClaims`, `api/ClaimHandler` |
| Anlocken mit Smaragd | Villager folgt dem Smaragd in der Hand wie die Kuh dem Weizen (Reichweite 10) | `lure/EmeraldLure`, `mixin/VillagerLureMixin` |
| Handelsblock | ruft den Villager per Redstone; Rechtsklick = kurzer Besuch; beim Setzen zum Spieler gedreht; max. 48 Blöcke vom Arbeitsplatz; Panik, Raid und Schlaf haben Vorrang; kein Teleport. Position als eigenes Attachment am Villager, übersteht Zombie-Umwandlung; kaputter oder zu weit entfernter Block wird vergessen | `trading/TradingBlock`, `TradingBlocks`, `TradingBlockCall`, `GoToTradingBlock`, `TradingBlockLinks`, `TradingBlockUpkeep`, Rezept `data/smartvillager/recipe/trading_block.json` |
| Pause der Vanilla-Wege | Arbeits- und Glockenwege pausieren beim Anruf oder wenn eine Mod „bleib stehen“ meldet (`registerStayPut`, `registerStayPutWhileWorking`) | `trading/PausedWhileAway`, `mixin/VillagerGoalPackagesMixin`, `api/SmartVillagerApi` |
| Doppelt so viele Zombie-Villager | 5/95 der natürlich gespawnten Zombies werden Zombie-Villager (Anteil 5 % → 10 %); Husks, Drowned, Spawner unberührt | `world/ZombieVillagerSpawns` |
| Heilen mit goldenem Essen | jedes Item im Tag `smartvillager:golden_food` heilt, kein Schwäche-Trank nötig; Dauer und Ablauf wie Vanilla | `world/ZombieVillagerCure`, `mixin/ZombieVillagerInvoker`, Tag `data/smartvillager/tags/item/golden_food.json` (Professions ergänzt seine goldenen Gerichte) |
| Brauständer mit Brennstoff | jeder bei der Weltgenerierung erzeugte Brauständer: 50–100 % Brennstoff (10–20 von 20), 50 % Chance auf 1 Lohenstaub im Fuel-Slot; nur neue Chunks | `world/BrewingStandSpawns`, `mixin/BrewingStandAccessor` |
| Gametests | Heilen, Claim, Handelsblock-Anruf, Brauständer-Brennstoff | `test/VillagerGameTests` |

## Geplant

- Chunk-Generierung und natürliche Zombie-Spawns sind nur im Spiel prüfbar (Testliste)
- Ideen: Eisengolems laufen bevorzugt zu Villagern (gehört evtl. zu [smart-golem.md](smart-golem.md))
