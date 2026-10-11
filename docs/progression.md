# Smart Villager Progression (`smart-villager-progression/`, `smartvillager_progression`)

Zweite Stufe der Leiter (braucht Smart Villager): alles, was für jeden Villager gleich gilt; kennt keinen Beruf. Ohne Professions läuft jeder Beruf (auch fremder Mods) mit seinen Vanilla-Trades. Claim, Anlocken, Handelsblock, Zombie-Heilen und Brauständer liegen eine Stufe tiefer in Smart Villager: [smart-villager.md](smart-villager.md). Zahlen: Obsidian `02_Tweak-Werte`.
Pfade: Java `smart-villager-progression/src/main/java/com/smartvillager/progression/`, Daten `smart-villager-progression/src/main/resources/`.

## Vorhanden

### Sektionen und Handel
| Feature | Was | Wo |
|---|---|---|
| Sektionen | Gruppe im Fenster mit eigenem Rang, Upgrade-Button, Leiste, optional Station; Kern bringt Quests + Trades | `section/Sections`, `CoreSections`, `SectionEntries`, `StandardSection`, `VillagerSections` (sichtbar/offen), `SectionView` |
| Erweiterungs-API | Sektion, Station, Logik, Zeile, Haken, Glücks-Elemente, Eintrags-Erweiterungen | `api/` (`ExtensionHooks`, `SectionDefinition`, `StationDefinition`, `SectionLogic`, `SectionOffer`, `HappinessElement`, `ExchangeExtensionType`, `ResultOverride`) |
| Vanilla-Trades für alle Berufe | Smaragd-Trades → Quests, Rest → Trades, je Villager gewürfelt, 4 Preis-Upgrades nach dem Freischalten | `vanilla/VanillaCatalog`, `VanillaScaling`, `VanillaRankCaps` |
| Einträge als Datapack | Registry `exchange` (Trades + Quests aller Sektionen), zweiter Bezahl-Slot `second_input` | `data/ModDataPackRegistries`, `ItemExchange`, `ExchangeExtensions`, `ItemAmount` |
| Handelsliste = Ansicht | Vanilla-`MerchantOffers` wird aus Zustand + Sektions-Logik neu gebaut; Rang-Rabatt als Sonderpreis, Mehrertrag durchgestrichen | `trade/VillagerOffers`, `ExchangeScaling`, `ResolvedExchange`, `section/RowView` |
| Eigenes Fenster | öffnet am letzten Vanilla-Schritt; Sektionen, Plakette, Leisten, Rang-Buttons, Scrollen | `mixin/VillagerTradingMixin`, `menu/VillagerMenu`, `client/ui/VillagerScreen`, `RankButtonWidget`, `SectionClientLogic`, `network/*Payload` |
| Kosten mit Verzauberung | „Item mit Verzauberung, jede Stufe“ | `trade/RequiredEnchantmentCost`, `mixin/ItemCostMixin` |
| Zahlung nach Item-Tag | `tag` (+ `without`: verbotene Komponenten) statt eines festen Items, z. B. „beliebiges Boot ohne Kanone“; Marker auf dem Anzeige-Stack, nur markierte Kosten werden anders verglichen | `trade/TagCost`, `data/ItemAmount`, `mixin/ItemCostMixin` |
| Ergebnis aus der Zahlung | Haken: das Ergebnisfeld wird aus den Zahlungs-Slots abgeleitet (beide Seiten, Vorhersage stimmt); neutral, solange keine Erweiterung ihn setzt | `api/ResultTransform`, `ExtensionHooks.setResultTransform`, `menu/VillagerMenu.transformResult` |
| Leere Zeilen | Platzhalter für fehlende Trades: leer, nicht klickbar | Item `trade/MissingTrade`, `VillagerScreen.isEmptyRow` |
| Trade-Ablauf | Buchen über die Sektions-Logik, `/vo trade` | `trade/TradeEvents`, `TradeActions`, `ExchangeExecutor` |

### Fortschritt, Arbeit, Glück
| Feature | Was | Wo |
|---|---|---|
| Leveln durch Arbeit | Arbeits-XP je Glück, 1 Punkt/Level, kein Maximallevel, Punkte-Puffer | `progression/ProgressionService`, `work/VillagerWorkScan` |
| Beruf wird fest | nach Handel oder XP | `progression/ProfessionLock` |
| Restock über Leisten | eine Leiste je Sektion statt Vanilla-Restock, Glück wirkt | `work/RestockService`, `state/Productivity`, `mixin/VillagerWorkSoundMixin` |
| Glücklichkeit | Bett, Dorfzentrum, Kontakt, Begleiter; Angst/Held überlagern | `happiness/HappinessCalculator`, `HappinessTracker`, `HappinessElements`, `mixin/VillagerGossipMixin`, Tag `happiness_companions` |
| Arbeitsplätze | Vanilla-Block + registrierte Stationen, ein Besitzer je Station, Fokus zwischen Sektionen | `station/ProfessionStations`, `StationClaims`, `StationOwners`, `StationFocus`, `UnemployedPriority`, `WorkAtStation`, `StationClaimHandler` (Claim-Anschluss an Smart Villager), `mixin/VillagerGoalPackagesMixin` |
| Arbeitszeit | Tick 1400–9600, `/time set work` | `data/minecraft/timeline/villager_schedule.json`, `data/smartvillager_progression/timeline/work_time_marker.json`, `work/DayClock` |

### Zustand und Welt
| Feature | Was | Wo |
|---|---|---|
| Zustand am Villager | Attachment (kein eigener Entity-Typ), automatisch synchronisiert | `state/VillagerState`, `ModAttachments`, `AttachmentVillagerStateAccess`, `Stations`, `Happiness` |
| Zombie-Umwandlung | Level, Punkte, Ränge, Bestand überstehen Zombie + Heilen | `state/VillagerConversion` |
| Debug `/vo` | `state`, `grant_xp`, `grant_points`, `invest`, `list`, `trade`, `action`, `restock`, `reset` | `debug/DebugCommands` |

## Geplant

- **Kern-API für fremde Programmierer** (je ein Commit; JSON-Format darf sich ändern, Weltstände nicht brechen; Vanilla-Klassen vorher in den 26.1-Quellen prüfen):
  - *Phase 3 – Components:* `ItemAmount` bekommt `components` (exakt), `match` (teilweise, Vanillas Item-Prädikate), `display`; `RequiredEnchantmentCost` → `ComponentCost`; raus: `enchantment`, `mob`, `ExtensionHooks.setMobWeaponComponent`; Extension migriert Veteran/Librarian/Runesmith/`QuestLogic`
  - *Phase 4 – `TradeRequirement`:* Interface `test`/`apply`/`describe` + Typ-Registry, Feld `requirements`; Kern liefert nur `player_xp`; `PlayerXpCostResultSlot` → `RequirementResultSlot`; raus: `ExtensionHooks.setPlayerXpCost`; unerfüllte Zeile ausgegraut
  - *Phase 5 – Trades in Raten:* automatisch ab Menge > Stapelgröße; Fortschritt je Spieler als Attachment `TradeProgress`; Pfeil füllt sich (Grafik `trade_arrow_progress` fehlt); Falle: Stackgröße der gespeicherten `MerchantOffers` (1.21.x: 99) → Angebot gedeckelt bauen
  - Danach: API von Interna entkoppeln (`SectionLogic`/`SectionOffer`, `SectionClientLogic` → `api/client`), mehr als eine BADGE-Sektion, Anleitung „Wie schreibe ich eine Extension“
- Punkte-Ökonomie und Leisten-Platz im Fenster prüfen
- Performance mit 100+ Villagern profilen
- Idee: Villager individualisierbar (gefärbte Lederrüstung), Denkblase mit dem Wunsch-Item, Kupfergolem als Begleiter, Urnen-System (eher eigene Mod)
- Risiken: Versionsdrift 26.1 → 26.2 (Mixins flach halten), andere Villager-Mods hooken dieselben Stellen
