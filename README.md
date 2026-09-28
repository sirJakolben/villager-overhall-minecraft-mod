# Villager Overhaul + Trade Rework

Kompakter Überblick: was die zwei Mods können und wo es im Code steht. Zum Wiederfinden, nicht zum Durchlesen.
Seit 2026-09-28 zwei Mods in einem Gradle-Projekt: **Villager Overhaul** (Kern, `core/`, Mod-ID `villageroverhaul`) und **Villager Overhaul: Trade Rework** (Extension, `trade-rework/`, Mod-ID `vo_trade_rework`, braucht den Kern).
Offenes steht in [active-development.md](active-development.md), Zahlen in [tweaks/](tweaks/), Arbeitsregeln in [villager-overhaul-projektrahmen.md](villager-overhaul-projektrahmen.md).
Pfade stehen am Anfang der beiden „Wo steht was“-Teile. Stand: 2026-09-28 (Sektions-Umbau).

## Features

### Kern (Villager Overhaul)

- **Jeder Beruf läuft mit seinen Vanilla-Trades auf unserem System** (auch Berufe anderer Mods): Smaragd-Trades → Sektion **Quests**, alles andere → Sektion **Trades**; beide restocken normal am Vanilla-Arbeitsblock; jedes Upgrade schaltet den nächsten Eintrag frei, danach 4 Preis-Upgrades; keine Level-Grenze
- **Villager leveln durch Arbeit**, nicht durch Handel: Arbeits-XP je nach Glücklichkeit, ein Punkt pro Level, keine Level-Grenze (auch nicht für Berufe der Extension)
- **Sektionen**: jede Gruppe im Handelsfenster hat eigenen Rang, eigenen Upgrade-Button, eigene Leiste und optional eine Station; Punkte schalten frei und verbessern Preise, Mengen, Bestand. Der Kern bringt Quests + Trades mit, Extensions registrieren weitere
- **Eigenes Handelsfenster** mit den Sektionen, Leisten und Upgrade-Buttons, aber Vanilla-Handelsmechanik darunter
- **Restock über Leisten** (eine pro Sektion) statt Vanilla-Restock, abhängig von Arbeit und Glücklichkeit
- **Glücklichkeit** aus Bett, Dorfzentrum, Villager-Kontakt, Begleitern; Angst/Held überlagern
- **Mehrere Arbeitsplätze** pro Villager (Vanilla-Arbeitsblock + registrierte Stationen), die er selbst sucht und besetzt
- **Claim per Smaragd**, Anlocken mit Smaragd, **Handelsblock** ruft den Villager per Redstone herbei
- **Beruf wird fest**, sobald gehandelt wurde oder XP da ist

### Extension (Trade Rework)

- Eigene Sektionen **Quests** (Easy/Hard/Dauer-Plätze, Tageslimit, Reroll mit Wartezeit), **Masteries** (Master-Station) und **Passive** (Plakette oben links, Passiv-Station) plus eigene Einträge für sechs Berufe; alle anderen Berufe laufen mit ihren Vanilla-Trades auf dem Kern
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
- **Falle:** Datapack-Einträge liegen unter `data/<Namespace der Datei>/<Namespace der Registry>/<registry>/` – Einträge der Extension also in `data/vo_trade_rework/villageroverhaul/exchange/` (Registry gehört dem Kern), Brech-Regeln in `data/vo_trade_rework/vo_trade_rework/crushing/`

## Grundentscheidungen

- **Kern + Extension** (2026-09-28): der Kern kennt keinen Beruf und kein Extension-Item; die Extension hängt sich über `api/ExtensionHooks` ein – Sektionen und Stationen registrieren, dazu Mob-Waffen-Markierung, XP-Preis, Entdeckerkarten. Der Kern registriert seine zwei Sektionen auf demselben Weg
- **Sektionen statt fester Gruppen** (Umbau 2026-09-28): eine Sektion = Id, Titel, Reihenfolge, Upgrade-Kosten, Darstellung (`LIST` | `BADGE`), optional Station, eigene Leiste (sichtbar oder versteckt), optional eigene Logik (`api/SectionLogic`, Standard = normaler Trade mit Restock). Einträge nennen ihre Sektion (`section`), Stationen werden je Beruf registriert, mehrere Sektionen dürfen dieselbe Station haben (ihre Leisten füllen sich gemeinsam). Alte Saves wurden dabei bewusst nicht migriert (Villager starten neu)
- **Sichtbarkeit**: eine Sektion gehört zum Villager, wenn sie für seinen Beruf Einträge oder eine registrierte Station hat; sie ist offen, wenn sie keine Station braucht, die Station besetzt ist oder schon Ränge da sind (`section/VillagerSections`)
- **Kein eigener Entity-Typ**: Zustand als Data Attachment am Vanilla-Villager → Spawning, Zucht, Raids, Heilen bleiben Vanilla
- **Vanilla-Erhalt**: so spät und so eng wie möglich eingreifen (Daten > Events > Mixin, `@WrapOperation` statt Methoden-Abbruch) – Regeln im Projektrahmen
- **Leitprinzip**: keine Vanilla-Spielweise funktionslos machen, nur ineffizienter (eingesperrter Villager arbeitet schwächer, nie null)
- **Keine Zufallsmechaniken** bei Villager-Entscheidungen (Fokus, Quests deterministisch)
- **Werte als Datapack**: Einträge als JSON, Balancing ohne Neukompilieren
- **Handelsliste = Ansicht** des eigenen Zustands: Vanillas `MerchantOffers` wird aus `VillagerState` und den Sektions-Logiken neu gebaut
- **Claim**: verdrängter Villager behält Level, Punkte und Beruf; **kein Teleport** zum Handelsblock (Panik, Raid, Schlaf haben Vorrang)
- Verworfen: generisches Skill-/Modifier-System, Countdown-Glücklichkeitszähler, morgendlicher Tages-Restock, „erste Benutzung“-Flag für Stationen, SmartBrainLib, geteilte Leisten zwischen Sektionen

## Kern – wo steht was

Java relativ zu `core/src/main/java/com/villageroverhaul/`, Daten relativ zu `core/src/main/resources/`.

### Sektionen und Stationen
| Was | Dateien |
|---|---|
| Schnittstelle für Extensions: Sektion, Station, Sektions-Logik, Zeile | `api/SectionDefinition.java`, `api/StationDefinition.java`, `api/SectionLogic.java`, `api/SectionOffer.java`, `api/ExtensionHooks.java` |
| Registrierte Sektionen/Stationen, Einträge je Beruf + Sektion (Cache je Registry) | `section/Sections.java`, `section/SectionEntries.java` |
| Die zwei Kern-Sektionen Quests + Trades | `section/CoreSections.java` |
| Welche Sektionen ein Villager hat, offen, bearbeitbar | `section/VillagerSections.java` |
| Standard-Verhalten (alle freigeschalteten Einträge, Bestand, Nachfüllen) | `section/StandardSection.java` |
| Was das Fenster über eine Sektion erfährt | `section/SectionView.java` |

### Zustand, Sync, Registries
| Was | Dateien |
|---|---|
| Villager-Zustand (Level, XP, Punkte, Ränge je Sektion, Bestand je Eintrag, Leisten, Glück, Stationen) | `state/VillagerState.java`, `state/Productivity.java`, `state/Stations.java`, `state/Happiness.java` |
| Attachment + automatischer Sync | `state/ModAttachments.java`, `state/AttachmentVillagerStateAccess.java`, `state/VillagerStateAccess.java` |
| Datapack-Registry `exchange` (alle Trades und Quests aller Sektionen) | `data/ModDataPackRegistries.java`, `data/ItemExchange.java` (`section`, optional `pool`), `data/ItemAmount.java` |
| Einträge | keine im Kern – liefert die Extension (`data/vo_trade_rework/villageroverhaul/exchange/`) |
| Spieltag | `work/DayClock.java` |

### Handel und Fenster
| Was | Dateien |
|---|---|
| Villager-Klick öffnet eigenes Fenster (Vanilla-Schritte bleiben) | `mixin/VillagerTradingMixin.java`, `mixin/VillagerAccessor.java` |
| Handelsliste aus Zustand + Sektions-Logiken, Rang-Rabatt als Vanilla-Sonderpreis, höherer Ertrag: Base-Menge durchgestrichen oben rechts am Ergebnis (`section/RowView.java`) | `trade/VillagerOffers.java`, `trade/ExchangeScaling.java`, `trade/ResolvedExchange.java` |
| Menü (Vanilla-Merchant-Slots, XP-Preis per Haken `ExtensionHooks.playerXpCost`, Kreativ-Mittelklick) | `menu/VillagerMenu.java`, `menu/ModMenuTypes.java` |
| Fenster (Sektionen in einer Schleife, Plakette, Leisten für n Sektionen, Rang-Buttons, Scrollen) | `client/ui/VillagerScreen.java`, `client/ui/RankButtonWidget.java`, `client/ui/VillagerGuiTextures.java` |
| Zeilen-Aussehen je Sektion (Pfeil/Ergebnis-Position, Zusatz-Widget wie Reroll) | `client/ui/SectionClientLogic.java` (nur Client) |
| Netzwerk (u. a. Sektions-Aktion für Zeilen-Buttons) | `network/*Payload.java`, `network/ModPayloads.java` |
| Trade gebucht (über die Sektions-Logik, Berufsbindung), Debug-Handel | `trade/TradeEvents.java`, `trade/TradeActions.java`, `trade/ExchangeExecutor.java` |
| Kosten „Item mit Verzauberung, jede Stufe“ | `trade/RequiredEnchantmentCost.java`, `mixin/ItemCostMixin.java` |
| Zweiter Bezahl-Slot | Feld `second_input` in `ItemExchange` |

### Progression
| Was | Dateien |
|---|---|
| Level-Kurve, Punkte-Puffer, Upgrade-Kosten, Investieren in eine Sektion | `progression/ProgressionService.java` |
| Beruf fest nach Handel/XP | `station/ProfessionLock.java` |

### Arbeit, Restock, Glücklichkeit
| Was | Dateien |
|---|---|
| Periodischer Scan (alle 100 Ticks): Glück, Stationen, Arbeits-XP, Leisten, eigene Arbeit der Sektionen | `work/VillagerWorkScan.java` |
| Leisten je Sektion, Nachschub, Tagesstart (vergessene Einträge aus dem Bestand), Freigabe nach Handel | `work/RestockService.java`, `state/Productivity.java` |
| Fokus-Wahl zwischen Sektionen, Morgen-Station | `station/StationFocus.java` |
| Eigenes Arbeitsgeräusch nur, während eine Listen-Sektion ihre Leiste füllt | `mixin/VillagerWorkSoundMixin.java`, `RestockService.makesWorkSound` |
| Glücklichkeit berechnen / Villager-Kontakt erkennen | `happiness/HappinessCalculator.java`, `happiness/HappinessTracker.java`, `mixin/VillagerGossipMixin.java` |
| Begleiter-Liste | `data/villageroverhaul/tags/entity_type/happiness_companions.json` |

### Arbeitsplätze, Claim, Handelsblock
| Was | Dateien |
|---|---|
| Stationen als Vanilla-Arbeitsort (POI) je Beruf, welche Station ein Block ist, Morgen-Priorität | `station/ProfessionStations.java` |
| Stationen suchen/prüfen/besetzen, Arbeitslose zuerst | `station/StationClaims.java`, `station/UnemployedPriority.java`, `mixin/VillagerGoalPackagesMixin.java` |
| Hinlaufen, Vanilla-Arbeitsblock-Verhalten pausieren | `station/WorkAtStation.java`, `station/PausedAtStation.java` |
| Claim per Smaragd, Verdrängen | `station/EmeraldClaimTool.java`, `station/ManualClaims.java` |
| Anlocken mit Smaragd | `station/EmeraldLure.java`, `mixin/VillagerLureMixin.java` |
| Handelsblock (Redstone-Ruf, Besuch per Rechtsklick) | `station/TradingBlock.java`, `station/TradingBlockCall.java`, `station/GoToTradingBlock.java`, `station/TradingBlocks.java`, Rezept `data/villageroverhaul/recipe/trading_block.json` |

### Vanilla-Trades (Berufe ohne eigene Einträge, Mod-Kompatibilität)
| Was | Dateien |
|---|---|
| Erkennen (keine eigenen Stationen oder Einträge, hat Vanilla-Trade-Sets), Vanilla-Trades würfeln (Seed je Villager + Trade), Farbvarianten > 3 → eine, Aufteilung Quests / Trades, im Speicher gecacht (Datapack-Reload leert) | `vanilla/VanillaCatalog.java` |
| Preis-Stufen (Smaragd-Belohnung: verlangte Menge, unstapelbar: Smaragd-Preis, stapelbar: Menge, Max-Bestand 50 → 300 %) | `vanilla/VanillaScaling.java` |
| Rang-Obergrenzen je Sektion (letzter Freischalt-Rang + 4 Preis-Upgrades), Punkte-Kosten auf einer gemeinsamen Linie 1 → 5 | `vanilla/VanillaRankCaps.java` |

### Platzhalter für fehlende Trades
| Was | Dateien |
|---|---|
| Leere Ränge zeigen eine komplett leere, deaktivierte Zeile – kein Preis, kein Pfeil, kein Ergebnis, kein Tooltip, nicht anklickbar (`client/ui/VillagerScreen.isEmptyRow`) – Datei löschen, sobald der echte Trade steht | Item `trade/MissingTrade.java` (`villageroverhaul:missing_trade`); die `*_todo_basic_<rang>.json` / `*_todo_master_<rang>.json` liegen in der Extension |

### Debug (`/vo`, angeschauter Villager)
- `debug/DebugCommands.java`: `state` (`progression`, `productivity`, `happiness`, `sections`, `raw`), `grant_xp`, `grant_points`, `invest <sektion>`, `list`, `trade <eintrag>` (Trade oder Quest), `action <sektion> <slot> <aktion>` (z. B. Quest-Reroll = Aktion 0), `restock`, `reset` (Level, Punkte, Ränge, Bestand, Leisten, Quest-Slots zurück; Glück + Stationen bleiben)

## Trade Rework – wo steht was

Java relativ zu `trade-rework/src/main/java/com/villageroverhaul/traderework/`, Daten relativ zu `trade-rework/src/main/resources/`. Einstieg: `TradeReworkMod.java` (Registrierung), `TradeReworkSections.java` (Stationen, Sektionen Quests/Masteries/Passive, übrige Haken), `TradeReworkRegistries.java` (`crushing`, `salvage`, `mob_weapon`), Mixins in `vo_trade_rework.mixins.json`.

### Sektionen der Extension
| Was | Dateien |
|---|---|
| Stationen (Master-, Passiv-Station je Beruf), Sektionen Quests / Masteries / Passive, übrige Haken | `TradeReworkSections.java` |
| Quests: Plätze, Tageslimit, Hard/Dauer-Quest (Veteran: zweiter Hard-Platz statt Dauer-Quest, Slot 4, +3 Limit auf Rang 9), Auswahl, Varianten (`input_variants`, `input_enchantment_variants`), Dauer-Quest-Item nie in anderen Pools, Abschluss, Pause, Reroll + Wartezeit | `quest/QuestSlots.java`, `quest/QuestLogic.java` |
| Quest-Zustand (Rotation, Reroll-Zeit, Tageszähler, Pausen) als eigenes, synchronisiertes Attachment | `quest/QuestState.java` |
| Quest-Zeilen im Fenster (Reroll-Button + Tage-Timer) | `client/ui/QuestClientLogic.java`, `client/ui/RerollButtonWidget.java`, Registrierung `client/ui/TradeReworkClient.java` |
| Passive: Leiste je Beruf, Schritt an der Passiv-Station | `passive/PassiveLogic.java`, `passive/PassiveWork.java` + je Beruf `passive/*Work.java` |

### Librarian
| Was | Dateien |
|---|---|
| Items, Blöcke, Block-Entities | `librarian/LibrarianItems.java`, `librarian/LibrarianBlocks.java`, `librarian/LibrarianBlockEntities.java` |
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/exchange/librarian_*.json` (Feld `section`, Quests mit `pool`) |
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
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/exchange/mason_*.json` (Feld `section`, Quests mit `pool`) |
| Steinchen `rock_pile` + Wegsteine `rock_path` (je bis 4, wie Bücherstapel; Wegsteine 1 px tiefer für Trampelpfade, Teppich-Halt) | `mason/RockPileBlock.java`, `mason/RockPathBlock.java` |
| Brechstation (3 + 3 Slots, Trichter wie Ofen, Komparator, Redstone-Puls) | `mason/CrushingStationBlock.java`, `mason/CrushingStationBlockEntity.java`, Fenster `station/ThreeInThreeOutMenu.java` + `client/ui/ThreeInThreeOutScreen.java` |
| Brech-Regeln (Steinmetz-Rezepte rückwärts + Datapack-Regeln) | `mason/CrushingRecipes.java`, `mason/CrushingRule.java`, `data/vo_trade_rework/vo_trade_rework/crushing/*.json` |
| Passive: Brechen (Batches à 4 einer Sorte, 8–32 pro Tag, Gang zur Station) | `passive/CrushingWork.java` |
| Placeholder für Blöcke ohne Grafik | `models/block/placeholder_block.json`, `textures/block/placeholder_texture.png` (Quelle `assets/placeholder_block/`) |

### Runesmith (Vanilla-Toolsmith; bis 2026-09-27 beim Repair Smith)
| Was | Dateien |
|---|---|
| Blöcke, Block-Entity, Items (Upgrade- + Reparaturstation, eine Upgrade-Vorlage) | `runesmith/RunesmithBlocks.java`, `runesmith/RunesmithBlockEntities.java`, `runesmith/RunesmithItems.java` |
| Trades (Stationen Basic 4, Vorlage Master 1, Bücher Haltbarkeit I Master 3 + Reparatur Master 4 – Smaragd + Buch → Buch Stufe I) | `data/vo_trade_rework/villageroverhaul/exchange/runesmith_*.json`; Buch-Output über Feld `enchantment` im Output (Kern: `data/ItemAmount.toStack`) |
| Upgrade-Rezepte (Vanilla `smithing_transform`, Vorlage + Teil + 1 Block der Zielstufe), Haltbarkeit relativ übernehmen | `data/vo_trade_rework/recipe/*_smithing.json`, `runesmith/UpgradeTemplates.java`, `mixin/SmithingUpgradeDurabilityMixin.java` |
| Reparaturstation (3 + 3, Trichter, Komparator, Redstone-Puls, Schleifstein-Geräusch) | `runesmith/RepairStationBlock.java`, `runesmith/RepairStationBlockEntity.java` |
| Passive: Reparieren (Schritte je Rang) | `passive/RepairWork.java` |
| Polster (+10 % Haltbarkeit auf Passive-Rang 6, frisst Schaden zuerst, Tooltip) | `runesmith/BonusDurability.java`, `mixin/ItemStackBonusDurabilityMixin.java` |
| Berufsname „Runenschmied“ | `entity.minecraft.villager.toolsmith` in `lang/*.json` |
| Gemeinsames 3 + 3-Stationsfenster (auch Brechstation) | `station/ThreeInThreeOutMenu.java`, `client/ui/ThreeInThreeOutScreen.java` |

### Veteran (Vanilla-Weaponsmith; bis 2026-09-27 „Repair Smith“)
| Was | Dateien |
|---|---|
| Challengestation (Master-Workstation, noch keine Passive-Station) | `veteran/VeteranBlocks.java`, `veteran/VeteranItems.java`, `TradeReworkSections.java` |
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/exchange/veteran_*.json` (Feld `section`, Quests mit `pool`) |
| Mob-Waffen: Markierung, Drop-Chance, Treffer-/Pfeil-Effekt, kürzere Abklingzeit (Liste an Client synchronisiert) | `veteran/MobWeapons.java`, `veteran/MobWeaponEvents.java`, `veteran/MobWeaponDefinition.java`, Liste `data/vo_trade_rework/vo_trade_rework/mob_weapon/*.json` |
| Mob-Waffen-Name („Zombie-Eisenschwert“) + Tooltip „+ Applies … effect“ | `mixin/ItemStackMobWeaponNameMixin.java`, `veteran/MobWeaponTooltip.java`, Sprachschlüssel `item.vo_trade_rework.mob_weapon(.effect)` |
| Quest verlangt Mob-Waffe | Kern: Feld `mob` in `data/ItemAmount.java`, `trade/RequiredEnchantmentCost.of`; Markierung über `ExtensionHooks.setMobWeaponComponent` |
| Berufsname „Veteran“ | `entity.minecraft.villager.weaponsmith` in `lang/*.json` |
| Karten-Trades: Basic 5 Außenposten, Basic 6 Verlies (beide Abenteuerkarte; Verlies aus dem Weltseed + Spawner-Nachprüfung), Master 1–4 Trial Chamber / Anwesen / Monument / Antike Stadt (Diamant + Herausforderungskarte) | `exchange/veteran_*_map.json`, eigene Struktur-Tags `data/vo_trade_rework/tags/worldgen/structure/on_*_maps.json` |
| Basic 0 Fernglas, 1 Rahmen, 2 Waffenständer | `exchange/veteran_*.json` |
| Waffenständer: Wand-Block, eine Waffe (Tag `weapon_rack_weapons`), Rechtsklick aufhängen/abnehmen, Waffe gezeichnet | `veteran/WeaponRackBlock.java`, `WeaponRackBlockEntity.java`, `VeteranBlockEntities.java`, `client/render/WeaponRackRenderer.java` |
| Piglin-Armbrust spannt 20 % schneller (Feld `charge_reduction`, Tooltip) | `mixin/CrossbowChargeMixin.java`, `mob_weapon/piglin.json`, `veteran/MobWeaponTooltip.java` |

### Cartographer (Vanilla, Teil-Gerüst)
| Was | Dateien |
|---|---|
| Erkundungsstation (Master-Workstation, noch keine Passive-Station) | `cartographer/CartographerBlocks.java`, `TradeReworkSections.java` |
| Abenteuer- + Herausforderungskarte (Items, Textur = Vanilla-Karte) | `cartographer/CartographerItems.java`, Modelle `models/item/adventure_map.json`, `challenge_map.json` |
| Trades: Basic 4 Erkundungsstation, Basic 5/6 die zwei Karten, Master 0–4 Dorfkarten Ebene/Wüste/Savanne/Taiga/Schnee | `exchange/cartographer_*.json` |

### Salvager (Vanilla-Armorer, Basic Trades + Schmelz-Passive)
| Was | Dateien |
|---|---|
| Basic 0 Eisenketten, 1 Eisengitter, 4 Veredelungs- + Schmelzstation, 5 Kupferader-Karte, 6 Eisenader-Karte | `exchange/salvager_*.json` |
| Veredelungsstation (Master, noch ohne Funktion) + Schmelzstation (Passive) | `salvager/SalvagerBlocks.java`, `TradeReworkSections.java` |
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
