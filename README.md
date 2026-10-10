# Villager Overhaul + Trade Rework

> **Temporär – Terminal-Befehle** (im Projekt-Hauptordner):
> - Bauen (beide Mods, Jars in `core/build/libs/` + `trade-rework/build/libs/`): `./gradlew build`
> - Spiel starten, Kern + Trade Rework (Ordner `run/`): `./gradlew :trade-rework:runClient`
> - Spiel starten, nur Kern (Ordner `run-core/`): `./gradlew :core:runClient`

Kompakter Überblick: was die zwei Mods können und wo es im Code steht. Zum Wiederfinden, nicht zum Durchlesen.
Seit 2026-09-28 zwei Mods in einem Gradle-Projekt: **Villager Overhaul** (Kern, `core/`, Mod-ID `villageroverhaul`) und **Villager Overhaul: Trade Rework** (Extension, `trade-rework/`, Mod-ID `vo_trade_rework`, braucht den Kern).
Offenes steht in [active-development.md](active-development.md), Zahlen in [tweaks/](tweaks/), Arbeitsregeln in [villager-overhaul-projektrahmen.md](villager-overhaul-projektrahmen.md).
Pfade stehen am Anfang der beiden „Wo steht was“-Teile. Stand: 2026-09-30 (Farmer + Fisherman).

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
- **Zombie-Umwandlung**: Level, Punkte, Ränge und Bestand überstehen Villager → Zombie → geheilt (Arbeitsplätze werden neu gesucht)
- **Heilen leichter**: 20 % der natürlich spawnenden Zombies kommen als Zombie-Villager; Braustände in Dörfern (Tempel) starten mit 1–3 Lohenstaub

### Extension (Trade Rework)

- Eigene Sektionen **Quests** (Easy/Hard/Dauer-Plätze, Tageslimit, Reroll mit Wartezeit), **Masteries** (Master-Station) und **Passive** (Plakette oben links, Passiv-Station) plus eigene Einträge für sechs Berufe; alle anderen Berufe laufen mit ihren Vanilla-Trades auf dem Kern
- **Librarian** komplett: Laterne, Namensschild, Bücherstapel, XP abfüllen, Villager-XP-Flasche, Schreib- und Verbesserungsstation, vier Spezialbücher, Buch-Upgrades als Passive, Lore-Schriftrollen
- **Mason** (Grundgerüst, Placeholder-Grafik): Sandsteine inkl. neuem braunem Sandstein (ganze Familie + brauner Sand), Steinchen, Wegsteine, Dorfbewohner-Statue (Deko), Veredelung mit Kohle/Quarz statt Smaragden, Metamorph- und Brechstation, Brech-Passive (Varianten eine Stufe zurück), Quests, Geoden-Karte
- **Runesmith** (Vanilla-Toolsmith, Teil-Gerüst, Placeholder-Grafik): Bücher Haltbarkeit I + Reparatur, eine Upgrade-Vorlage (Vanilla-Schmiede-Rezepte, 1 Block der Zielstufe), Upgrade- und Reparaturstation, Reparieren + Polster als Passive
- **Veteran** (Vanilla-Weaponsmith, Teil-Gerüst, Placeholder-Grafik): Mob-Waffen (markierte Vanilla-Items mit Namen und Treffer-Effekt, 25 % Drop; Diener-Axt schlägt, Piglin-Armbrust spannt 20 % schneller), Quests auf Mob-Waffen, Fernglas, Rahmen, Waffenständer, Trappings-Station, Challengestation (Passive: zeichnet Herausforderungskarten)
- **Cartographer** (Vanilla, Teil-Gerüst): Kartenstation (vier Masteries: Abenteuer-, Herausforderungs-, Höhlen-, Netherkarte; Vanilla-Kartentextur), Abenteuerstation (Passive: zeichnet Abenteuerkarten zu zufälligen Strukturen), Höhlen-/Netherkarte (Raycast-Karte je Höhenebene); Entdeckerkarten als Trade-Output für alle Berufe (Strukturen, Geoden, Erzadern – die letzten zwei aus dem Weltseed)
- **Goldsmith / Goldschmied** (Vanilla-Armorer, Teil-Gerüst): vergoldete Eisenkette + vergoldetes Eisengitter (Smaragd + Vanilla-Kette/-Gitter), Kupfer- und Eisenader-Karte, Verwertungsstation als Passive (Metall-Ausrüstung → Barren), Vergoldungsstation mit Masteries 1–4 (Metall + Block → verstärktes Glas, eisengerahmter Redstone, goldgerahmter Lapis + goldverzierte Varianten per Steinmetz, vergoldeter Schwarzstein)
- **Farmer / Bauer** (Vanilla, Teil-Gerüst, Placeholder-Grafik): Passive mit Erntekorbstation – er erntet wirklich am Feld, jede Ernte füllt seine Passive-Leiste (Last, Kapazität 3–9 je Rang); voll oder nach einer Minute Stillstand bringt er sie zum Korb, wo die Leiste alle 3 s um eine Einheit sinkt und jede Einheit eine Chance auf goldenes Brot/Rote Bete/Kartoffel/Karotte hat (0 % auf Rang 0, 20 % auf Rang 6); Glasurstation mit vier Masteries (Kandierter Apfel, goldene Rote-Bete-Suppe, glasierte Karotte, Goldtoast, alle mit Effekt); Suppen stapeln bis 16 (global)
- **Fisherman / Fischer** (Vanilla, Teil-Gerüst, Placeholder-Grafik): Passive mit Fischkorbstation – er fischt wirklich (hat vor jedem Wurf freie Sicht aufs Wasser (sonst läuft er zu einem Platz mit Sicht), dreht sich zum Wasser, Angel in der Hand, Schwimmer auf dem Wasser, kein Fisch wird erzeugt: der Fang ist eine Einheit auf seiner Last, Kapazität 3–9; 10 Fänge pro Arbeitstag auf Rang 0 bis 30 auf Rang 6, auf Rang 6 zählt ein Fang mit 50 % doppelt); am Korb sinkt die Leiste alle 3 s um eine Einheit und jede Einheit gibt genau einen Fisch – Kabeljau, Lachs oder mit 1–7 % der Biom-Fisch seines Bioms (acht Biom-Fische, der Spieler fängt sie mit 8 % auch selbst); Werftstation ohne Masteries (die Boote wurden am 2026-10-07 entfernt, neues Konzept folgt)

## Technische Basis

- NeoForge 26.1.0.19-beta, Minecraft 26.1, Java 25, ModDevGradle 2.x, Mojang-Namen (kein Parchment), keine Fremd-Libs
- **Lizenz**: CC0-1.0 (gemeinfrei, beide Mods) – [LICENSE](LICENSE), `mod_license` in `gradle.properties`
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
- **Werte als Datapack**: Einträge als JSON, Balancing ohne Neukompilieren
- **Handelsliste = Ansicht** des eigenen Zustands: Vanillas `MerchantOffers` wird aus `VillagerState` und den Sektions-Logiken neu gebaut
- **Claim**: verdrängter Villager behält Level, Punkte und Beruf; **kein Teleport** zum Handelsblock (Panik, Raid, Schlaf haben Vorrang)
- **Stations-Partikel = Vanilla-Workstation** (2026-09-29): jede eigene Station (auch Platzhalter) nutzt als `particle` im Blockmodell die Partikel-Textur der Vanilla-Workstation ihres Berufs – Librarian `lectern_sides`, Mason `stonecutter_bottom`, Toolsmith/Runesmith `smithing_table_front`, Weaponsmith/Veteran `grindstone_side`, Cartographer `cartography_table_side3`, Armorer/Goldsmith `blast_furnace_front`, Farmer `composter_side`, Fisherman `barrel_side` (alle `minecraft:block/…`)

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
| Zustand übersteht Zombie-Umwandlung und Heilen (ohne Arbeitsplätze) | `state/VillagerConversion.java` |
| Mehr Zombie-Villager, Lohenstaub in Dorf-Brauständen | `world/ZombieVillagerSpawns.java`, `world/VillageBrewingStands.java` |
| Datapack-Registry `exchange` (alle Trades und Quests aller Sektionen) | `data/ModDataPackRegistries.java`, `data/ItemExchange.java` (`section`, `extensions`), `data/ExchangeExtensions.java`, `data/ItemAmount.java` |
| Einträge | keine im Kern – liefert die Extension (`data/vo_trade_rework/villageroverhaul/exchange/`) |
| Spieltag | `work/DayClock.java` |

### Handel und Fenster
| Was | Dateien |
|---|---|
| Villager-Klick öffnet eigenes Fenster (Vanilla-Schritte bleiben; „Nein“ nur ohne offene Sektion, nicht schon bei null Angeboten) | `mixin/VillagerTradingMixin.java`, `mixin/VillagerAccessor.java` |
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
| Glücks-Elemente von Erweiterungen (Gewicht, Timer, Beobachter; im Scan gefragt, bis heute gesehen) | `api/HappinessElement.java`, `happiness/HappinessElements.java`, Tage in `state/Happiness.extraDays` |
| Begleiter-Liste | `data/villageroverhaul/tags/entity_type/happiness_companions.json` |

### Arbeitsplätze, Claim, Handelsblock
| Was | Dateien |
|---|---|
| Stationen als Vanilla-Arbeitsort (POI) je Beruf, welche Station ein Block ist, Morgen-Priorität | `station/ProfessionStations.java` |
| Stationen suchen/prüfen/besetzen, Arbeitslose zuerst | `station/StationClaims.java`, `station/UnemployedPriority.java`, `mixin/VillagerGoalPackagesMixin.java` |
| Ein Besitzer pro Station (Index; wer zuerst da ist, behält sie, Smaragd-Claim gewinnt; der andere lässt los ohne Ticket-Freigabe) | `station/StationOwners.java` |
| Hinlaufen, Vanilla-Verhalten am Arbeitsblock/an der Glocke pausieren (auch beim Handelsblock-Ruf) | `station/WorkAtStation.java`, `station/PausedWhileAway.java` |
| Claim per Smaragd, Verdrängen | `station/EmeraldClaimTool.java`, `station/ManualClaims.java` |
| Anlocken mit Smaragd | `station/EmeraldLure.java`, `mixin/VillagerLureMixin.java` |
| Handelsblock (Redstone-Ruf, Besuch per Rechtsklick, beim Setzen zum Spieler gedreht wie ein Ofen) | `interaction/TradingBlock.java`, `interaction/TradingBlockCall.java`, `interaction/GoToTradingBlock.java`, `interaction/TradingBlocks.java`, Rezept `data/villageroverhaul/recipe/trading_block.json` |

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
- `data/villageroverhaul/timeline/work_time_marker.json`: `/time set work` (Tick 1400, Beginn der Villager-Arbeitszeit) – nur ein Time Marker, kein Java
- `data/minecraft/timeline/villager_schedule.json`: Vanillas Villager-Tagesablauf überschrieben – Arbeit Tick 1400–9600 statt 2000–9000 (30 s früher, 30 s länger), sonst Vanilla

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
| Passive Stationsblöcke (Menü auf Rechtsklick, Redstone-Puls, Komparator, nicht begehbar; `FacingPassiveStationBlock` dreht zum Spieler) – alle fünf Passive-Stationen erben davon | `station/PassiveStationBlock.java`, `station/FacingPassiveStationBlock.java` |

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
| Dorfbewohner-Statuen (je Typ ein Block, Figur beim Setzen gewürfelt, 2 Blöcke hoch wie Tür, grau gerendert auf Rüstungsständer-Platte; Mason-Trade liefert die Statue seines Typs) | `mason/VillagerStatueBlock.java`, `mason/StatueFigure.java`, `mason/VillagerStatueBlockEntity.java`, Trade `trade/VillagerStatueOutput.java`, Grafik `client/render/VillagerStatueRenderer.java`, `VillagerStatueSpecialRenderer.java` (Item), `VillagerStatueFigure.java`, `GrayscaleTextures.java` |
| Statuen machen glücklich (+10 %, 8 Blöcke, 2 Tage; Statuen sind POIs, Abfrage statt Block-Scan) | `mason/VillagerStatueHappiness.java` |
| Brauner Sandstein-Familie + brauner Sand (umgefärbte Vanilla-Texturen als Dateien, Rohdateien in `assets/Mason/brown_sandstone`; Modelle, Loot, Rezepte = Vanilla-Sandstein mit getauschten IDs) | `mason/MasonBlocks.java`, `textures/block/*brown_sand*.png`, Tags in `data/minecraft/tags/` |
| Brech-Regeln (Steinmetz-Rezepte rückwärts + Datapack-Regeln) | `mason/CrushingRecipes.java`, `mason/CrushingRule.java`, `data/vo_trade_rework/vo_trade_rework/crushing/*.json` |
| Passive: Brechen (Batches à 4 einer Sorte, Rang 5: 6, Rang 6: 8, 8–32 pro Tag, Gang zur Station) | `passive/CrushingWork.java` |
| Placeholder für Blöcke ohne Grafik | `models/block/placeholder_block.json`, `textures/block/placeholder_texture.png` (Quelle `assets/placeholder_block/`) |

### Runesmith (Vanilla-Toolsmith)
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

- **Vogelscheuche** (2026-10-08, ungetestet im Spiel): `farmer/ScarecrowBlock|ScarecrowGoal|ScarecrowEvents.java`, Trade `farmer_scarecrow.json` – siehe tweaks/farmer.md
- **Eisengolem-Nachtpatrouille** (2026-10-08, ungetestet im Spiel): `golem/PatrolGoal.java`, `golem/IronGolemEvents.java` – siehe tweaks/iron-golem.md

### Veteran (Vanilla-Weaponsmith)
| Was | Dateien |
|---|---|
| Trappings-Station (Master-Workstation) | `veteran/VeteranBlocks.java`, `veteran/VeteranItems.java`, `TradeReworkSections.java` |
| Trades / Quests | `data/vo_trade_rework/villageroverhaul/exchange/veteran_*.json` (Feld `section`, Quests mit `pool`) |
| Mob-Waffen: Markierung, Drop-Chance, Treffer-/Pfeil-Effekt, kürzere Abklingzeit (Liste an Client synchronisiert) | `veteran/MobWeapons.java`, `veteran/MobWeaponEvents.java`, `veteran/MobWeaponDefinition.java`, Liste `data/vo_trade_rework/vo_trade_rework/mob_weapon/*.json` |
| Mob-Waffen-Name („Zombie-Eisenschwert“) + Tooltip „+ Applies … effect“ | `mixin/ItemStackMobWeaponNameMixin.java`, `veteran/MobWeaponTooltip.java`, Sprachschlüssel `item.vo_trade_rework.mob_weapon(.effect)` |
| Quest verlangt Mob-Waffe | Kern: Feld `mob` in `data/ItemAmount.java`, `trade/RequiredEnchantmentCost.of`; Markierung über `ExtensionHooks.setMobWeaponComponent` |
| Berufsname „Veteran“ | `entity.minecraft.villager.weaponsmith` in `lang/*.json` |
| Challengestation (Passive, Basic 4): nimmt Herausforderungskarten, zeichnet über mehrere Arbeitstage eine Karte zu einer zufälligen Struktur des Herausforderungs-Pools (Karten-Trades gestrichen 2026-10-10) | `mapping/*`, `passive/MappingWork.java`, `exchange/veteran_charting_station.json`, Pool `map_pool/challenge.json` |
| Basic 0 Fernglas, 1 Rahmen, 2 Waffenständer | `exchange/veteran_*.json` |
| Waffenständer: Wand-Block, eine Waffe (Tag `weapon_rack_weapons`), Rechtsklick aufhängen/abnehmen, Waffe gezeichnet | `veteran/WeaponRackBlock.java`, `WeaponRackBlockEntity.java`, `VeteranBlockEntities.java`, `client/render/WeaponRackRenderer.java` |
| Piglin-Armbrust spannt 20 % schneller (Feld `charge_reduction`, Tooltip) | `mixin/CrossbowChargeMixin.java`, `mob_weapon/piglin.json`, `veteran/MobWeaponTooltip.java` |
| Zombie Villager Cage (Basic 3, 8 Smaragde): Block 1 × 2, hält einen Zombie-Villager als gespeicherte Daten, Abbauen setzt ihn frei, Schwäche-Trank + goldener Apfel heilen ihn (Vanilla-Verwandlung); Cage = Punkt von Interesse, Reservierung nur im Speicher | `cage/Cages.java`, `ZombieVillagerCageBlock.java`, `ZombieVillagerCageBlockEntity.java`, `client/render/CageRenderer.java` |
| Eisengolem trägt Zombie-Villager in den Cage: greift ihn nicht an, solange ein leerer Cage nahe ist; Ziel Goal Priorität 0 (Arme voll), Treffer von anderem Mob → fallen lassen, 3–4 s betäubt, Golem greift Angreifer an; Zombie ohne KI (kein gespeichertes Flag), brennt getragen nicht | `cage/CarryZombieVillagerGoal.java`, `ZombieCaptives.java`, `CarryPose.java`, `mixin/CageIronGolemMixin.java`, `mixin/CageMobMixin.java`, `client/render/CarryClient.java`, Tests `cage/test/CageGameTests.java` |

### Cartographer (Vanilla, Teil-Gerüst)
| Was | Dateien |
|---|---|
| Kartenstation (Master-Workstation, vier Masteries: Abenteuer-, Herausforderungs-, Höhlen-, Netherkarte; Basic 5/6 entfielen 2026-10-10) | `cartographer/CartographerBlocks.java`, `TradeReworkSections.java` |
| Abenteuer- + Herausforderungskarte (Items, Textur = Vanilla-Karte) | `cartographer/CartographerItems.java`, Modelle `models/item/adventure_map.json`, `challenge_map.json` |
| Trades: Basic 4 Kartenstation + Abenteuerstation; Masteries Rang 1–4 Abenteuerkarte, Herausforderungskarte, Höhlenkarte, Netherkarte (je 1 Smaragd; Dorfkarten-Masteries gestrichen 2026-10-10) | `exchange/cartographer_*.json` |
| Höhlenkarte / Netherkarte: leere Karte, Rechtsklick in einer Ebene der eigenen Dimension (Overworld ohne offenen Himmel / Nether) macht eine gefüllte Karte, fest an die Ebene gebunden (unten/oben), Name danach „Tiefe/Flache Höhlenkarte“ bzw. Netherkarte; sonst passiert nichts. Gezeichnet wird nur beim Halten in der eigenen Ebene: 32 waagerechte Strahlen auf Augenhöhe, +2, +4 (obere nur wo die untere Höhe keine Luft fand), Boden-Farbe mit Gefälle-Schattierung, Wände schwarz (überschreibt nie Entdecktes), unentdeckte Pixel im Umkreis 32 werden schwarz | `cavemap/CaveMapKind.java`, `CaveLayer.java`, `CaveMapDrawer.java`, `EmptyCaveMapItem.java`, `CaveMapItem.java`, `CaveMapItems.java`, Test `mapping/test/MapGameTests.java` (`cave_map_use`) |
| Abenteuerstation (Passive, 3 + 3 Slots): nimmt Abenteuerkarten, zeichnet über mehrere Arbeitstage eine Karte zu einer zufälligen Struktur des Abenteuer-Pools. Pools = Datenpaket-Registry `map_pool` (Gewichte; Strukturen ohne Eintrag, auch von Mods, landen nach Generierungsschritt im Pool: Untergrund → Herausforderung, sonst Abenteuer). Passive-Dauer über `PassiveWork.daysPerStep` | `mapping/MapPool.java`, `MapTargets.java`, `MapStationBlock.java`, `MapStationBlockEntity.java`, `MappingBlocks.java`, `passive/MappingWork.java`, `map_pool/adventure.json` + `challenge.json`, Tests `mapping/test/MapGameTests.java` |

### Goldsmith (Vanilla-Armorer, Basic Trades + Masteries + Verwertungs-Passive)
| Was | Dateien |
|---|---|
| Basic 0 vergoldete Eisenkette, 1 vergoldetes Eisengitter (je Smaragd + Vanilla-Kette bzw. -Gitter), 4 Vergoldungs- + Verwertungsstation, 5 Kupferader-Karte, 6 Eisenader-Karte | `exchange/goldsmith_*.json` |
| Vergoldete Eisenkette / vergoldetes Eisengitter (Seabrass-Texturen aus Abyssal Decor auf Vanillas `template_chain` / `template_bars_*`, Verhalten wie Eisenkette/-gitter, Tags `chains` / `bars`) | `goldsmith/GoldsmithBlocks.java` |
| Vergoldungsstation (Master, Placeholder-Würfel) + Verwertungsstation (Passive) | `goldsmith/GoldsmithBlocks.java`, `TradeReworkSections.java` |
| Masteries 1–4 ohne Smaragde (Metall im 1. Slot, Block im 2.): Eisen + Glas → verstärktes Glas (Rahmen verbindet sich mit angrenzendem verstärktem Glas (Connected Textures: jede Seite wählt eine von 16 Texturen nach den 4 Nachbarn in ihrer Ebene, `ReinforcedGlassBlock`, eigene Texturen in `assets/Goldsmith/reinforced_glass`)), Eisen + Redstone-Block → eisengerahmter Redstone (gibt Signal; Steinmetz: eisengerahmter Redstone → eisengerahmte Redstone-Treppe (L-Textur, Modelle wie die Lapis-Treppe), -Stufe ×2, eisenverzierter Redstone (Ornated Redstone, setzt sich wie ein Kolben), eisenverzierte Redstone-Säule (Ornated Redstone Pillar); nur der Grundblock gibt Signal (entschieden 2026-09-30), Texturen selbst gezeichnet (`assets/Goldsmith/redstone variants`)), Gold + Lapisblock → goldgerahmter Lapis (Steinmetz: goldgerahmter Lapis → goldgerahmte Stufe ×2 / Treppe, goldverzierter Lapis, goldverzierte Lapissäule), Gold + Schwarzstein → Vanilla vergoldeter Schwarzstein | `exchange/goldsmith_reinforced_glass.json` / `_iron_framed_redstone` / `_gold_framed_lapis` / `_gilded_blackstone`, Blöcke in `goldsmith/GoldsmithBlocks.java`, Steinmetz `recipe/*_from_gold_framed_lapis_stonecutting.json` |
| Verwertungsstation (3 + 3 wie die Brechstation, Trichter, Komparator, Redstone-Puls, Hochofen-Knistern; WORKING: Tiegel mit animiertem geschmolzenem Metall nur beim Arbeiten) | `goldsmith/SalvageStationBlock.java`, Modelle `salvage_station.json` / `salvage_station_working.json`, Lava-Blasen `goldsmith/GoldsmithParticles.java` + `client/render/MoltenBubbleParticle.java`, `goldsmith/SalvageStationBlockEntity.java`, `goldsmith/GoldsmithBlockEntities.java` |
| Passive: Einschmelzen (1 Teil pro Schritt, 8–32 pro Tag; Rezeptmenge × Rang-Anteil × Zustand, min. 1; Kette → Nuggets; Netherite → Diamant-Teil bleibt im Input-Slot, Scraps in den Output; Trichter unten zieht Output + übrig gebliebene Diamant-Teile) | `passive/SalvageWork.java`, Regeln `data/vo_trade_rework/vo_trade_rework/salvage/*.json` (`goldsmith/SalvageRule.java`) |
| Berufsname „Goldschmied“ / „Goldsmith“ | `entity.minecraft.villager.armorer` in `lang/*.json` |

### Körbe (Passive von Farmer + Fisherman, 2026-09-30)
| Was | Dateien |
|---|---|
| Korb-Block (5 Slots im Vanilla-Trichterfenster wie die Verbesserungsstation, Füllstand 0–5 = belegte Slots wie beim Komposter, Trichter unten zieht, Komparator, Redstone-Puls; Hitbox 14 × 14 × 9 px) – ein Typ für Erntekorb- und Fischkorbstation | `basket/BasketStationBlock.java`, `basket/BasketStationBlockEntity.java`, `basket/BasketBlockEntities.java` |
| Last am Villager (Einheiten je Sorte, Rückkehr-Flag, Schritt-Takt, gemerktes Wasser; nur Server, übersteht Zombie-Umwandlung, `/vo reset` leert); die Passive-Leiste zeigt sie (`PassiveLogic.withLoadMeter`), Kern-Haken `SectionLogic.fillsFromWork` | `passive/PassiveCounters.java` (Attachment), `passive/PassiveLogic.java` |
| Fisherman bleibt beim Wurf stehen: Kern-Haken `ExtensionHooks.registerStayPut` (Gründe, ein Villager steht still) – `station/PausedWhileAway` pausiert damit Vanillas Schlendern wie beim Handelsblock-Ruf | `api/ExtensionHooks.java`, `station/PausedWhileAway.java`, `passive/FishingWork.isHeld` |
| Gemeinsames Verhalten: Last aufnehmen, bei voll oder Stillstand zum Korb, alle 5 s eine Einheit herausnehmen und würfeln | `passive/CarryingWork.java` |
| Debug: `/vo counters` (zeigt die Zähler des nächsten Villagers), `/vo counters give <punkte>` | `passive/CountersCommand.java` |

### Farmer (Vanilla, Teil-Gerüst)
| Was | Dateien |
|---|---|
| Stationen: Glasurstation (Master, Placeholder-Würfel), Erntekorbstation (Passive) | `farmer/FarmerBlocks.java`, `TradeReworkSections.java` |
| Ernte zählen (NeoForge-`BlockDropsEvent`: Vanillas `HarvestFarmland` bricht mit dem Villager als Breaker; eine Einheit je Ernte bis zur Kapazität) | `farmer/HarvestCounting.java` |
| Passive: je Einheit am Korb Gold-Wurf (Chance auf die goldene Sorte: Rang 0 = 0 %, Rang 6 = 20 %; kein goldener Apfel) | `passive/HarvestWork.java` |
| Items: goldenes Brot/Rote Bete/Kartoffel, Kandierter Apfel, goldene Rote-Bete-Suppe, glasierte Karotte, Goldtoast | `farmer/FarmerItems.java` |
| Kandierter Apfel: „danach Geschwindigkeit“ über den unsichtbaren Effekt Zuckerschub (gibt beim Ablaufen Speed) | `farmer/SugarRushEffect.java` |
| Suppen stapeln bis 16 (alle Items mit Stapel 1 und Schüssel als Rückgabe) | `farmer/SoupStacking.java` |
| Trades: Stationen Basic 4, leere Zeilen Basic 0–3, 5, 6, Masteries 1–4 | `exchange/farmer_*.json` |
| Rezept der goldenen Karotte entfernt | `data/minecraft/recipe/golden_carrot.json`, `data/minecraft/advancement/recipes/brewing/golden_carrot.json` (`neoforge:never`) |

### Fisherman (Vanilla, Teil-Gerüst)
| Was | Dateien |
|---|---|
| Stationen: Werftstation (Master, Placeholder-Würfel), Fischkorbstation (Passive) | `fisherman/FishermanBlocks.java`, `TradeReworkSections.java` |
| Passive: zum Wasser laufen (Kern-Haken `SectionLogic.workPlace`), Angel in die Hand, Schwimmer werfen (Bogenflug, Biss nach 5–15 s, Einholen = eine Einheit); am Korb je Einheit 1–3 Fische (Kabeljau/Lachs/Biom-Fisch) | `passive/FishingWork.java`, `fisherman/VillagerBobber.java`, `fisherman/FishermanEntities.java`, Darstellung `client/render/VillagerBobberRenderer.java` |
| Biom-Fische: Datapack-Registry `biome_fish` (Biomgruppe → Fisch), acht Items, Biom-Listen | `fisherman/BiomeFish.java`, `fisherman/FishermanItems.java`, `data/vo_trade_rework/vo_trade_rework/biome_fish/*.json`, `tags/worldgen/biome/fish_*.json` |
| Spieler fängt den Biom-Fisch mit 8 % statt eines Fischs (Loot-Modifier, nur `gameplay/fishing`) | `fisherman/BiomeFishModifier.java`, `data/vo_trade_rework/loot_modifiers/biome_fish.json` |
| Trades: Stationen Basic 4, leere Zeilen Basic 0–3, 5, 6, Masteries 1–4 | `exchange/fisherman_*.json` |

### Entdeckerkarten als Trade-Output (gemeinsam für alle Berufe)
| Was | Dateien |
|---|---|
| Erweiterung `explorer_map` im Trade: genau eins von `destination` (Struktur-Tag), `feature` (Platziertes Feature), `ore_vein` (`copper`/`iron`), dazu Markierung + Name | `trade/ExplorerMap.java` (Erweiterungstyp der Extension) |
| Suche einmal pro Villager + Trade beim ersten Handeln, fertige Karte am Villager gespeichert; vorher Platzhalter, nichts gefunden = ausverkauft (1 Tag Pause bis zur nächsten Suche); Geoden-/Aderkarten zeigen die Höhe im Tooltip | `trade/ExplorerMaps.java` (Attachment `explorer_maps`) (Kern: `trade/VillagerOffers.build`) |
| Strukturen: Vanilla-Suche (100 Chunks, noch nicht verwendete Struktur) | `ServerLevel.findNearestMapStructure` |
| Geoden (Mason) + Verliese (Veteran): Platzierung aus dem Weltseed nachgerechnet, ohne Chunks zu laden (Deko-Seed, Feature-Index, Platzierungsregeln, Biom aus der Biom-Quelle); Verliese zusätzlich in der Welt nachgeprüft (`verify_block` Spawner, max. 64 Kandidaten, lädt den Chunk) | `explore/FeatureLocator.java`, `mixin/ChunkGeneratorAccessor.java` |
| Erzadern (Goldsmith): Ader-Rauschen (`OreVeinifier`) aus dem Weltseed ausgewertet | `explore/OreVeinLocator.java` |
| Ring-Suche um den Villager (32 Chunks) | `explore/ChunkSearch.java` |
### Debug der Extension
- `explore/LocateCommand.java`: `/vo locate geode|copper_vein|iron_vein|dungeon` – Vorhersage ab der eigenen Position, lädt die Stelle und zählt passende Blöcke (0 = daneben)

## Externe Quellen

- Obsidian-Vault `Jakobs Vault/Improved Villagers/`: Berufs-Dateien (z. B. `Librarian.md`, Wertetabellen = Quelle der Datapack-Werte), `Tweak-Werte.md` (Quelle aller Zahlen), `Fehlende Texturen und Modelle.md`
- Rohe Grafiken: `assets/` (Blockbench, Pixelorama, PNG)
- **Credits:** Vergoldete Eisenkette + vergoldetes Eisengitter (Abyssal Decors „Seabrass“) und der Eisenrahmen von verstärktem Glas stammen aus **Abyssal Decor** von **starrysock** (https://modrinth.com/mod/abyssal-decor, CC0; Nutzung zusätzlich vom Autor ausdrücklich erlaubt, 2026-09-29). Chipped (Terrarium) geprüft: Code MIT, Texturen All Rights Reserved → nicht übernehmbar
- NeoForge-Doku: Attachments https://docs.neoforged.net/docs/datastorage/attachments/ · Registries https://docs.neoforged.net/docs/concepts/registries/
