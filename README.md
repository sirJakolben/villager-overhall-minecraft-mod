# Villager Overhaul + Trade Rework

Kompakter Überblick: was die zwei Mods können und wo es im Code steht. Zum Wiederfinden, nicht zum Durchlesen.
Seit 2026-09-28 zwei Mods in einem Gradle-Projekt: **Villager Overhaul** (Kern, `core/`, Mod-ID `villageroverhaul`) und **Villager Overhaul: Trade Rework** (Extension, `trade-rework/`, Mod-ID `vo_trade_rework`, braucht den Kern).
Offenes steht in [active-development.md](active-development.md), Zahlen in [tweaks/](tweaks/), Arbeitsregeln in [villager-overhaul-projektrahmen.md](villager-overhaul-projektrahmen.md).
Pfade stehen am Anfang der beiden „Wo steht was“-Teile. Stand: 2026-09-28.

## Features

### Kern (Villager Overhaul)

- **Jeder Beruf läuft mit seinen Vanilla-Trades auf unserem System** (Nicht-Implementierungsregel): Smaragd-Trades → Dauer-Quests, letzte 4 → Masteries, Rest → Basic Trades, jedes Upgrade schaltet den nächsten Eintrag frei, danach 4 Preis-Upgrades; keine Level-Grenze; nur der Vanilla-Arbeitsblock als Station, Passive ausgeblendet
- **Villager leveln durch Arbeit**, nicht durch Handel: Arbeits-XP je nach Glücklichkeit, ein Punkt pro Level (Level 0–20 für Berufe der Extension)
- **Vier Upgrade-Gruppen** (Quests, Basic Trades, Master Trades, Passive): Punkte schalten frei und verbessern Preise, Mengen, Bestand
- **Eigenes Handelsfenster** mit Quests, Trades, Masteries, Leisten und Upgrade-Buttons, aber Vanilla-Handelsmechanik darunter
- **Quests**: Items abgeben gegen Smaragde, Easy/Hard/Dauer-Plätze, Tageslimit, Reroll mit Wartezeit
- **Restock über Produktivitätsleisten** statt Vanilla-Restock, abhängig von Arbeit und Glücklichkeit
- **Glücklichkeit** aus Bett, Dorfzentrum, Villager-Kontakt, Begleitern; Angst/Held überlagern
- **Bis zu drei Arbeitsplätze** pro Villager (Basic, Master, Passive), die er selbst sucht und besetzt
- **Claim per Smaragd**, Anlocken mit Smaragd, **Handelsblock** ruft den Villager per Redstone herbei
- **Beruf wird fest**, sobald gehandelt wurde oder XP da ist

### Extension (Trade Rework)

- Eigene Trades, Quests, Master- und Passive-Stationen für sechs Berufe; alle anderen Berufe bleiben auf der Nicht-Implementierungsregel des Kerns
- **Librarian** komplett: Laterne, Namensschild, Bücherstapel, XP abfüllen, Villager-XP-Flasche, Schreib- und Verbesserungsstation, vier Spezialbücher, Buch-Upgrades als Passive, Lore-Schriftrollen
- **Mason** (Grundgerüst, Placeholder-Grafik): Sandsteine inkl. neuem braunem Sandstein, Steinchen, Wegsteine, Dorfbewohner-Statue (Deko), Veredelung mit Kohle/Quarz statt Smaragden, Metamorph- und Brechstation, Brech-Passive (Varianten eine Stufe zurück), Quests, Geoden-Karte
- **Runesmith** (Vanilla-Toolsmith, Teil-Gerüst, Placeholder-Grafik): Bücher Haltbarkeit I + Reparatur, eine Upgrade-Vorlage (Vanilla-Schmiede-Rezepte, 1 Block der Zielstufe), Upgrade- und Reparaturstation, Reparieren + Polster als Passive
- **Veteran** (Vanilla-Weaponsmith, Teil-Gerüst, Placeholder-Grafik): Mob-Waffen (markierte Vanilla-Items mit Namen und Treffer-Effekt, 25 % Drop; Diener-Axt schlägt, Piglin-Armbrust spannt 20 % schneller), Quests auf Mob-Waffen, Fernglas, Rahmen, Waffenständer, Challengestation, Karten zu Außenposten, Verlies, Trial Chamber, Anwesen, Monument, Antiker Stadt
- **Cartographer** (Vanilla, Teil-Gerüst): Abenteuer- und Herausforderungskarte (Vanilla-Kartentextur), Erkundungsstation, Dorfkarten als Masteries; Entdeckerkarten als Trade-Output für alle Berufe (Strukturen, Geoden, Erzadern – die letzten zwei aus dem Weltseed)
- **Salvager** (Vanilla-Armorer, Teil-Gerüst): Eisenketten, Eisengitter, Kupfer- und Eisenader-Karte, Schmelzstation als Passive (Metall-Ausrüstung → Barren), Veredelungsstation (noch ohne Funktion)

## Technische Basis

- NeoForge 26.1.0.19-beta, Minecraft 26.1, Java 25, ModDevGradle 2.x, Mojang-Namen (kein Parchment), keine Fremd-Libs
- **Gradle-Multiprojekt**: `core/` und `trade-rework/`, gemeinsame `gradle.properties` im Wurzelordner (Extension-Werte `ext_mod_*`); die Extension kompiliert gegen den Kern (`compileOnly`) und lädt ihn im Entwicklungsstart aus dessen Source-Set (mods-Block)
- **Starts**: `:trade-rework:runClient` = Kern + Extension im alten `run/` (Test-Welten); `:core:runClient` = Kern allein in `run-core/`; `runGameTestServer` in beiden = Ladetest ohne Fenster (`run-gametest*/`)
- 26.1-Eigenheiten: `ResourceLocation` heißt `Identifier`; Screens rendern über `GuiGraphicsExtractor` / `extractBackground`
- **Falle:** Datapack-Einträge liegen unter `data/<Namespace der Datei>/<Namespace der Registry>/<registry>/` – Trades der Extension also in `data/vo_trade_rework/villageroverhaul/trade/` (Registry gehört dem Kern), Brech-Regeln in `data/vo_trade_rework/vo_trade_rework/crushing/`

## Grundentscheidungen

- **Kern + Extension** (2026-09-28): der Kern kennt keinen Beruf und kein Extension-Item; die Extension hängt sich über `api/ExtensionHooks` ein (Stationen, Passive, zweiter Hard-Platz, Mob-Waffen-Markierung, XP-Preis, Entdeckerkarten). Ohne Einträge dort gilt für jeden Beruf die Nicht-Implementierungsregel
- **Kein eigener Entity-Typ**: Zustand als Data Attachment am Vanilla-Villager → Spawning, Zucht, Raids, Heilen bleiben Vanilla
- **Vanilla-Erhalt**: so spät und so eng wie möglich eingreifen (Daten > Events > Mixin, `@WrapOperation` statt Methoden-Abbruch) – Regeln im Projektrahmen
- **Leitprinzip**: keine Vanilla-Spielweise funktionslos machen, nur ineffizienter (eingesperrter Villager arbeitet schwächer, nie null)
- **Keine Zufallsmechaniken** bei Villager-Entscheidungen (Fokus, Quests deterministisch)
- **Werte als Datapack**: Trades/Quests als JSON, Balancing ohne Neukompilieren
- **Nicht-Implementierungsregel** (2026-09-27): jeder Beruf ohne eigenes Design (Vanilla oder andere Mods) läuft trotzdem auf unserem System – Vanilla-Trades: Smaragd-Trades → Dauer-Quests, letzte 4 → Masteries, Rest → Basic Trades, jedes Upgrade = nächster Eintrag, keine Level-Caps, Masteries ohne Station sichtbar, Passive ausgeblendet (Projektrahmen)
- **Handelsliste = Ansicht** des eigenen Zustands: Vanillas `MerchantOffers` wird aus `VillagerState` neu gebaut
- **Sichtbarkeit der Bereiche**: Trades / Masteries / Passive nur, solange die zugehörige Station (Lesepult / Schreib- / Verbesserungsstation) besetzt ist oder schon Ränge da sind; Quests immer (`ProgressionService.isGroupOpen`)
- **Claim**: verdrängter Villager behält Level, Punkte und Beruf; **kein Teleport** zum Handelsblock (Panik, Raid, Schlaf haben Vorrang)
- Verworfen: generisches Skill-/Modifier-System, Countdown-Glücklichkeitszähler, morgendlicher Tages-Restock, „erste Benutzung“-Flag für Stationen, SmartBrainLib

## Kern – wo steht was

Java relativ zu `core/src/main/java/com/villageroverhaul/`, Daten relativ zu `core/src/main/resources/`.

### Zustand, Sync, Registries
| Was | Dateien |
|---|---|
| Villager-Zustand (Level, XP, Punkte, Ränge, Bestände, Leisten, Glück, Quest-Log, Stationen) | `core/state/VillagerState.java` + Records in `core/state/` |
| Attachment + automatischer Sync | `core/ModAttachments.java`, `core/AttachmentVillagerStateAccess.java`, `core/VillagerStateAccess.java` |
| Datapack-Registries `trade`, `quest`, `passive` | `data/ModDataPackRegistries.java`, `data/ItemExchange.java`, `data/ItemAmount.java`, `data/PassiveAbilityDefinition.java` |
| Trade-/Quest-Dateien | keine im Kern – liefert die Extension (`data/vo_trade_rework/villageroverhaul/trade/`, `.../quest/`) |
| Spieltag | `core/DayClock.java` |

### Handel und Fenster
| Was | Dateien |
|---|---|
| Villager-Klick öffnet eigenes Fenster (Vanilla-Schritte bleiben) | `mixin/VillagerTradingMixin.java`, `mixin/VillagerAccessor.java` |
| Handelsliste aus Zustand, Rang-Rabatt als Vanilla-Sonderpreis | `trade/VillagerOffers.java`, `trade/TradeProviderImpl.java`, `trade/ExchangeScaling.java` |
| Menü (Vanilla-Merchant-Slots, XP-Preis per Haken `ExtensionHooks.playerXpCost`, Kreativ-Mittelklick) | `client/ui/VillagerMenu.java` |
| Fenster (Abschnitte, Leisten, Upgrade-Buttons, Scrollen, Reroll-Button + Tage-Timer) | `client/ui/VillagerScreen.java`, `client/ui/TradeLevelButtonWidget.java`, `client/ui/RerollButtonWidget.java`, `client/ui/VillagerGuiTextures.java` |
| Netzwerk | `network/*Payload.java`, `network/ModPayloads.java` |
| Trade gebucht (Bestand, Quest-Rotation, Berufsbindung) | `trade/TradeEvents.java`, `trade/TradeActions.java`, `trade/ExchangeExecutor.java` |
| Kosten „Item mit Verzauberung, jede Stufe“ | `trade/RequiredEnchantmentCost.java`, `mixin/ItemCostMixin.java` |
| Zweiter Bezahl-Slot | Feld `second_input` in `ItemExchange` |

### Quests
| Was | Dateien |
|---|---|
| Plätze, Tageslimit, Hard/Dauer-Quest (per Haken zweiter Hard-Platz statt Dauer-Quest, Slot 4, +3 Limit auf Rang 9 – Extension: Veteran) | `quest/QuestSlots.java` |
| Angebot, Varianten (`input_variants`, `input_enchantment_variants`), Dauer-Quest-Item nie in anderen Pools | `quest/QuestProviderImpl.java` |
| Abschluss, Pause, Reroll + Wartezeit | `quest/QuestActions.java` |

### Progression
| Was | Dateien |
|---|---|
| Level-Kurve, Punkte-Puffer, Investieren, Gruppe offen? | `progression/ProgressionService.java` |
| Upgrade-Kosten, Voll-Freischalt-Rang | `progression/UpgradeGroup.java` |
| Beruf fest nach Handel/XP | `core/ProfessionLock.java` |

### Arbeit, Restock, Glücklichkeit
| Was | Dateien |
|---|---|
| Periodischer Scan (alle 100 Ticks): Glück, Stationen, Arbeits-XP, Leisten, Passive | `freedom/VillagerWorkScan.java` |
| Passive je Beruf (fragt die Extension-Haken; ohne Eintrag keine Passive, Leistengröße 1 Schritt/Tag) | `passive/PassiveWork.java` |
| Eigenes Arbeitsgeräusch nur, während eine Trade-Leiste sich füllt; an der Passive-Station stumm (die Station macht die Geräusche) | `mixin/VillagerWorkSoundMixin.java`, `RestockService.isFillingTradeMeter` |
| Leisten, Nachschub, Fokus-Wahl, Passiv-Leistengröße | `trade/RestockService.java`, `core/state/DailyProductivity.java` |
| Glücklichkeit berechnen / Villager-Kontakt erkennen | `freedom/HappinessCalculator.java`, `freedom/HappinessTracker.java`, `mixin/VillagerGossipMixin.java` |
| Begleiter-Liste | `data/villageroverhaul/tags/entity_type/happiness_companions.json` |

### Arbeitsplätze, Claim, Handelsblock
| Was | Dateien |
|---|---|
| Stationen als Vanilla-Arbeitsort (POI) je Beruf – Einträge kommen aus der Extension | `claim/ProfessionStations.java` |
| Stationen suchen/prüfen/besetzen, Arbeitslose zuerst | `claim/StationClaims.java`, `claim/UnemployedPriority.java`, `mixin/VillagerGoalPackagesMixin.java` |
| Wo er arbeitet, Hinlaufen, Lesepult pausieren | `claim/StationFocus.java`, `claim/WorkAtStation.java`, `claim/PausedAtStation.java` |
| Claim per Smaragd, Verdrängen | `claim/EmeraldClaimTool.java`, `claim/ManualClaims.java` |
| Anlocken mit Smaragd | `claim/EmeraldLure.java`, `mixin/VillagerLureMixin.java` |
| Handelsblock (Redstone-Ruf, Besuch per Rechtsklick) | `claim/TradingBlock.java`, `claim/TradingBlockCall.java`, `claim/GoToTradingBlock.java`, `claim/ClaimBlocks.java`, Rezept `data/villageroverhaul/recipe/trading_block.json` |

### Nicht-Implementierungsregel (nicht designte Berufe, Mod-Kompatibilität)
| Was | Dateien |
|---|---|
| Erkennen (keine eigenen Stationen, Trades, Quests; hat Vanilla-Trade-Sets), Vanilla-Trades würfeln (Seed je Villager + Trade), Farbvarianten > 3 → eine, Aufteilung Quests / Basic / Masteries, im Speicher gecacht (Datapack-Reload leert) | `fallback/FallbackCatalog.java` |
| Rang-Obergrenzen je Gruppe (letzter Freischalt-Rang + 4 Preis-Upgrades), Punkte-Kosten auf einer gemeinsamen Linie 1 → 5 über alle Upgrades, an den Client geschickt; Preis-Stufen (Quest-Menge, Smaragd-Preis unstapelbar, Menge stapelbar, Max-Bestand 50 → 300 %) in `fallback/FallbackScaling.java` | `fallback/RankCaps.java`, `network/VillagerOffersPayload.java`, `progression/ProgressionService.investPoint/isGroupOpen(villager, …)` |
| Einbau: Trades, Bestand, Quests (Dauer-Quest-Slots ab 100), exaktes Vanilla-Ergebnis, Basic-Station = Vanilla-Arbeitsblock, Masteries-Bereich immer offen, keine Level-Grenze (`ProgressionService.maxLevel`) | `trade/TradeProviderImpl.java`, `trade/RestockService.java`, `trade/TradeActions.java`, `quest/QuestProviderImpl.java`, `quest/QuestSlots.java`, `core/ResolvedExchange.resultStack`, `claim/StationClaims.java`, `client/ui/VillagerScreen.java` |

### Platzhalter für fehlende Trades
| Was | Dateien |
|---|---|
| Leere Ränge zeigen eine komplett leere, deaktivierte Zeile – kein Preis, kein Pfeil, kein Ergebnis, kein Tooltip, nicht anklickbar (`client/ui/VillagerScreen.isEmptyRow`) – Datei löschen, sobald der echte Trade steht | Item `trade/MissingTrade.java` (`villageroverhaul:missing_trade`); die `*_todo_basic_<rang>.json` / `*_todo_master_<rang>.json` liegen in der Extension |

### Andockstellen für die Extension
| Was | Dateien |
|---|---|
| Alle Haken an einer Stelle (Stationen, Passive, zweiter Hard-Platz, Mob-Waffen-Markierung, XP-Preis, Entdeckerkarten); ohne Extension neutral | `api/ExtensionHooks.java` |
| Datenformat der Entdeckerkarten (inkl. Erzader-Arten) bleibt im Kern, die Suche macht die Extension | `data/ExplorerMap.java`, Feld `explorer_map` in `data/ItemExchange.java` |

### Debug (`/vo`, angeschauter Villager)
- `core/DebugCommands.java`: `state` (`progression`, `productivity`, `happiness`, `quests`, `trades`, `raw`), `grant_xp`, `grant_points`, `invest <gruppe>`, `list <registry>`, `trade <id>`, `quest_complete`, `quest_reroll`, `restock`

## Trade Rework – wo steht was

Java relativ zu `trade-rework/src/main/java/com/villageroverhaul/traderework/`, Daten relativ zu `trade-rework/src/main/resources/`. Einstieg: `TradeReworkMod.java` (Registrierung), `TradeReworkProfessions.java` (alle Haken in den Kern), `TradeReworkRegistries.java` (`crushing`, `salvage`, `mob_weapon`), Mixins in `vo_trade_rework.mixins.json`.

### Librarian
| Was | Dateien |
|---|---|
| Items, Blöcke, Block-Entities | `librarian/LibrarianItems.java`, `librarian/LibrarianBlocks.java`, `librarian/LibrarianBlockEntities.java` |
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/trade/librarian_*.json`, `.../quest/librarian_*.json` |
| Bücherstapel (bis 4 Bücher) | `librarian/BookPileBlock.java` |
| Stations-Blöcke mit Modell-Hitbox | `librarian/ModelShapedBlock.java`, `librarian/EnchantmentStationBlock.java` |
| Erfahrungsfläschchen = 55 XP, Abfüll-Trade | `librarian/ExperienceBottles.java`, `mixin/ThrownExperienceBottleMixin.java` |
| Villager-XP-Flasche + Kugeln | `librarian/VillagerExperienceBottleItem.java`, `librarian/ThrownVillagerExperienceBottle.java`, `librarian/VillagerExperienceOrb.java`, `librarian/LibrarianEntities.java`, `client/render/` |
| Spezialbücher (Zaubertisch nur Kategorie, Ergebnis = verzaubertes Buch, Vanilla-Buchkürzung) | `librarian/SpecialBookItem.java`, `mixin/EnchantmentMenuMixin.java`, Listen `data/vo_trade_rework/tags/enchantment/*_book.json` |
| Verbesserungsstation (5 Slots, Trichter, Komparator, Redstone-Puls) | `librarian/EnchantmentStationBlock.java` (Block-ID `enhancement_station`), `librarian/EnchantmentStationBlockEntity.java`, `librarian/EnchantmentStationMenu.java`, `client/ui/EnchantmentStationScreen.java` |
| Passive: Buch-Upgrade (niedrigste Stufe zuerst, Stufen-Obergrenze, Gang zur Station) | `passive/BookUpgradeWork.java`, `librarian/BookUpgrades.java` |
| Lore-Schriftrollen (12 Items, Buch-Fenster, festes Fragment) | `librarian/LoreScrollItem.java`, `network/OpenLoreScrollPayload.java`, `client/ui/LoreScrollScreens.java` |
| Lore-Fundorte (Global Loot Modifier) | `data/vo_trade_rework/loot_modifiers/`, `data/vo_trade_rework/loot_table/lore_scroll/`, Liste `data/neoforge/loot_modifiers/global_loot_modifiers.json` |
| Texte (Namen, Tooltips, Lore-Fragmente) | `assets/vo_trade_rework/lang/de_de.json`, `en_us.json` |

### Mason
| Was | Dateien |
|---|---|
| Blöcke, Block-Entity, Items | `mason/MasonBlocks.java`, `mason/MasonBlockEntities.java`, `mason/MasonItems.java` |
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/trade/mason_*.json`, `.../quest/mason_*.json` |
| Steinchen `rock_pile` + Wegsteine `rock_path` (je bis 4, wie Bücherstapel; Wegsteine 1 px tiefer für Trampelpfade, Teppich-Halt) | `mason/RockPileBlock.java`, `mason/RockPathBlock.java` |
| Brechstation (3 + 3 Slots, Trichter wie Ofen, Komparator, Redstone-Puls) | `mason/CrushingStationBlock.java`, `mason/CrushingStationBlockEntity.java`, Fenster `station/ThreeInThreeOutMenu.java` + `client/ui/ThreeInThreeOutScreen.java` |
| Brech-Regeln (Steinmetz-Rezepte rückwärts + Datapack-Regeln) | `mason/CrushingRecipes.java`, `mason/CrushingRule.java`, `data/vo_trade_rework/vo_trade_rework/crushing/*.json` |
| Passive: Brechen (Batches à 4 einer Sorte, 8–32 pro Tag, Gang zur Station) | `passive/CrushingWork.java` |
| Placeholder für Blöcke ohne Grafik | `models/block/placeholder_block.json`, `textures/block/placeholder_texture.png` (Quelle `assets/placeholder_block/`) |

### Runesmith (Vanilla-Toolsmith; bis 2026-09-27 beim Repair Smith)
| Was | Dateien |
|---|---|
| Blöcke, Block-Entity, Items (Upgrade- + Reparaturstation, eine Upgrade-Vorlage) | `runesmith/RunesmithBlocks.java`, `runesmith/RunesmithBlockEntities.java`, `runesmith/RunesmithItems.java` |
| Trades (Stationen Basic 4, Vorlage Master 1, Bücher Haltbarkeit I Master 3 + Reparatur Master 4 – Smaragd + Buch → Buch Stufe I) | `data/vo_trade_rework/villageroverhaul/trade/runesmith_*.json`; Buch-Output über Feld `enchantment` im Output (Kern: `data/ItemAmount.toStack`) |
| Upgrade-Rezepte (Vanilla `smithing_transform`, Vorlage + Teil + 1 Block der Zielstufe), Haltbarkeit relativ übernehmen | `data/vo_trade_rework/recipe/*_smithing.json`, `runesmith/UpgradeTemplates.java`, `mixin/SmithingUpgradeDurabilityMixin.java` |
| Reparaturstation (3 + 3, Trichter, Komparator, Redstone-Puls, Schleifstein-Geräusch) | `runesmith/RepairStationBlock.java`, `runesmith/RepairStationBlockEntity.java` |
| Passive: Reparieren (Schritte je Rang) | `passive/RepairWork.java` |
| Polster (+10 % Haltbarkeit auf Passive-Rang 6, frisst Schaden zuerst, Tooltip) | `runesmith/BonusDurability.java`, `mixin/ItemStackBonusDurabilityMixin.java` |
| Berufsname „Runenschmied“ | `entity.minecraft.villager.toolsmith` in `lang/*.json` |
| Gemeinsames 3 + 3-Stationsfenster (auch Brechstation) | `station/ThreeInThreeOutMenu.java`, `client/ui/ThreeInThreeOutScreen.java` |

### Veteran (Vanilla-Weaponsmith; bis 2026-09-27 „Repair Smith“)
| Was | Dateien |
|---|---|
| Challengestation (Master-Workstation, noch keine Passive-Station) | `veteran/VeteranBlocks.java`, `veteran/VeteranItems.java`, `TradeReworkProfessions.java` (`Entry.NO_STATION`) |
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/trade/veteran_*.json`, `.../quest/veteran_*.json` |
| Mob-Waffen: Markierung, Drop-Chance, Treffer-/Pfeil-Effekt, kürzere Abklingzeit (Liste an Client synchronisiert) | `veteran/MobWeapons.java`, `veteran/MobWeaponEvents.java`, `veteran/MobWeaponDefinition.java`, Liste `data/vo_trade_rework/vo_trade_rework/mob_weapon/*.json` |
| Mob-Waffen-Name („Zombie-Eisenschwert“) + Tooltip „+ Applies … effect“ | `mixin/ItemStackMobWeaponNameMixin.java`, `veteran/MobWeaponTooltip.java`, Sprachschlüssel `item.vo_trade_rework.mob_weapon(.effect)` |
| Quest verlangt Mob-Waffe | Kern: Feld `mob` in `data/ItemAmount.java`, `trade/RequiredEnchantmentCost.of`; Markierung über `ExtensionHooks.setMobWeaponComponent` |
| Berufsname „Veteran“ | `entity.minecraft.villager.weaponsmith` in `lang/*.json` |
| Karten-Trades: Basic 5 Außenposten, Basic 6 Verlies (beide Abenteuerkarte; Verlies aus dem Weltseed + Spawner-Nachprüfung), Master 1–4 Trial Chamber / Anwesen / Monument / Antike Stadt (Diamant + Herausforderungskarte) | `trade/veteran_*_map.json`, eigene Struktur-Tags `data/vo_trade_rework/tags/worldgen/structure/on_*_maps.json` |
| Basic 0 Fernglas, 1 Rahmen, 2 Waffenständer | `trade/veteran_*.json` |
| Waffenständer: Wand-Block, eine Waffe (Tag `weapon_rack_weapons`), Rechtsklick aufhängen/abnehmen, Waffe gezeichnet | `veteran/WeaponRackBlock.java`, `WeaponRackBlockEntity.java`, `VeteranBlockEntities.java`, `client/render/WeaponRackRenderer.java` |
| Piglin-Armbrust spannt 20 % schneller (Feld `charge_reduction`, Tooltip) | `mixin/CrossbowChargeMixin.java`, `mob_weapon/piglin.json`, `veteran/MobWeaponTooltip.java` |

### Cartographer (Vanilla, Teil-Gerüst)
| Was | Dateien |
|---|---|
| Erkundungsstation (Master-Workstation, noch keine Passive-Station) | `cartographer/CartographerBlocks.java`, `TradeReworkProfessions.java` |
| Abenteuer- + Herausforderungskarte (Items, Textur = Vanilla-Karte) | `cartographer/CartographerItems.java`, Modelle `models/item/adventure_map.json`, `challenge_map.json` |
| Trades: Basic 4 Erkundungsstation, Basic 5/6 die zwei Karten, Master 0–4 Dorfkarten Ebene/Wüste/Savanne/Taiga/Schnee | `trade/cartographer_*.json` |

### Salvager (Vanilla-Armorer, Basic Trades + Schmelz-Passive)
| Was | Dateien |
|---|---|
| Basic 0 Eisenketten, 1 Eisengitter, 4 Veredelungs- + Schmelzstation, 5 Kupferader-Karte, 6 Eisenader-Karte | `trade/salvager_*.json` |
| Veredelungsstation (Master, noch ohne Funktion) + Schmelzstation (Passive) | `salvager/SalvagerBlocks.java`, `TradeReworkProfessions.java` |
| Schmelzstation (3 + 3 wie die Brechstation, Trichter, Komparator, Redstone-Puls, Hochofen-Knistern) | `salvager/SmeltingStationBlock.java`, `salvager/SmeltingStationBlockEntity.java`, `salvager/SalvagerBlockEntities.java` |
| Passive: Einschmelzen (1 Teil pro Schritt, 8–32 pro Tag; Rezeptmenge × Rang-Anteil × Zustand, min. 1; Kette → Nuggets; Netherite → Diamant-Teil + Scraps) | `passive/SmeltingWork.java`, Regeln `data/vo_trade_rework/vo_trade_rework/salvage/*.json` (`salvager/SalvageRule.java`) |
| Berufsname „Verwerter“ | `entity.minecraft.villager.armorer` in `lang/*.json` |

### Entdeckerkarten als Trade-Output (gemeinsam für alle Berufe)
| Was | Dateien |
|---|---|
| Feld `explorer_map` im Trade: genau eins von `destination` (Struktur-Tag), `feature` (Platziertes Feature), `ore_vein` (`copper`/`iron`), dazu Markierung + Name | Kern: `data/ExplorerMap.java`, `data/ItemExchange.java` |
| Suche einmal pro Villager + Trade beim ersten Handeln, fertige Karte am Villager gespeichert; vorher Platzhalter, nichts gefunden = ausverkauft (1 Tag Pause bis zur nächsten Suche); Geoden-/Aderkarten zeigen die Höhe im Tooltip | `trade/ExplorerMaps.java` (Attachment `explorer_maps`), eingehängt über `ExtensionHooks.setExplorerMaps` (Kern: `trade/VillagerOffers.build`) |
| Strukturen: Vanilla-Suche (100 Chunks, noch nicht verwendete Struktur) | `ServerLevel.findNearestMapStructure` |
| Geoden (Mason) + Verliese (Veteran): Platzierung aus dem Weltseed nachgerechnet, ohne Chunks zu laden (Deko-Seed, Feature-Index, Platzierungsregeln, Biom aus der Biom-Quelle); Verliese zusätzlich in der Welt nachgeprüft (`verify_block` Spawner, max. 64 Kandidaten, lädt den Chunk) | `explore/FeatureLocator.java`, `mixin/ChunkGeneratorAccessor.java` |
| Erzadern (Salvager): Ader-Rauschen (`OreVeinifier`) aus dem Weltseed ausgewertet | `explore/OreVeinLocator.java` |
| Ring-Suche um den Villager (32 Chunks) | `explore/ChunkSearch.java` |
### Debug der Extension
- `explore/LocateCommand.java`: `/vo locate geode|copper_vein|iron_vein|dungeon` – Vorhersage ab der eigenen Position, lädt die Stelle und zählt passende Blöcke (0 = daneben)

## Externe Quellen

- Obsidian-Vault `Jakobs Vault/Improved Villagers/`: Berufs-Dateien (z. B. `Librarian.md`, Wertetabellen = Quelle der Datapack-Werte), `Tweak-Werte.md` (Quelle aller Zahlen), `Fehlende Texturen und Modelle.md`
- Rohe Grafiken: `assets/` (Blockbench, Pixelorama, PNG)
- NeoForge-Doku: Attachments https://docs.neoforged.net/docs/datastorage/attachments/ · Registries https://docs.neoforged.net/docs/concepts/registries/
