# Smart Villager – Komfort-Funktionen (derzeit noch im Kern `core/`)

Kleine Spieler-Werkzeuge rund um Villager. Liegen heute im Kern (`villageroverhaul`) und könnten später eine eigene Mod „Smart Villager“ werden. Zahlen: Obsidian `02_Tweak-Werte`, Abschnitt „Claim & Handelsblock“.
Pfade: Java `core/src/main/java/com/villageroverhaul/`.

## Vorhanden

| Feature | Was | Wo |
|---|---|---|
| Claim per Smaragd | Smaragd-Rechtsklick wählt den Villager, Klick auf einen Arbeitsblock weist ihn zu; ein vorheriger Besitzer wird verdrängt und behält Level, Punkte, Beruf | `interaction/EmeraldClaimTool`, `ManualClaims`, `station/StationOwners` |
| Anlocken mit Smaragd | Villager folgt dem Smaragd in der Hand wie die Kuh dem Weizen (Reichweite 10) | `interaction/EmeraldLure`, `mixin/VillagerLureMixin` |
| Handelsblock | ruft den Villager per Redstone; Rechtsklick = kurzer Besuch; beim Setzen zum Spieler gedreht; max. 48 Blöcke vom Arbeitsplatz; Panik, Raid und Schlaf haben Vorrang; kein Teleport | `interaction/TradingBlock`, `TradingBlockCall`, `GoToTradingBlock`, `TradingBlocks`, Rezept `data/villageroverhaul/recipe/trading_block.json` |

## Geplant

- Eigene Mod abspalten (Entscheidung offen; Kern darf sie nie importieren)
- Ideen: Eisengolems laufen bevorzugt zu Villagern (gehört evtl. zu [smart-golem.md](smart-golem.md))
