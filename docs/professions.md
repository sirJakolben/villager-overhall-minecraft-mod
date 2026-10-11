# Smart Villager Professions – Extension (`smart-villager-professions/`, `smartvillager_professions`)

Die designten Berufe mit eigenen Trades, Items, Blöcken und Stationen; braucht Progression ([progression.md](progression.md)) und hängt sich über `api/ExtensionHooks` ein. Zahlen und Trade-Tabellen: Obsidian `<Beruf>.md`. Texturen: Obsidian `03_Fehlende Texturen und Modelle`.
Pfade: Java `smart-villager-professions/src/main/java/com/smartvillager/professions/`, Daten `smart-villager-professions/src/main/resources/`. Einstieg: `SmartVillagerProfessionsMod`, `ProfessionsSections` (Stationen, Sektionen, Haken), `ProfessionsRegistries` (`crushing`, `salvage`, `mob_weapon`, `biome_fish`, `map_pool`). Einträge: `data/smartvillager_professions/smartvillager_progression/exchange/<beruf>_*.json`. Texte: `assets/smartvillager_professions/lang/de_de.json`, `en_us.json`.
Aufbau: je Bereich/Beruf eine Tabelle der vorhandenen Features (Feature | Was | Wo im Code), darunter **Geplant**.

## Gemeinsame Systeme
| Feature | Was | Wo |
|---|---|---|
| Sektionen Quests / Masteries / Passive | Quests nur am Vanilla-Arbeitsplatz; Easy/Hard/Dauer-Plätze, Tageslimit, Reroll mit Wartezeit; Masteries an der Master-Station; Passive (Plakette oben links) an der Passiv-Station | `ProfessionsSections`, `quest/QuestSlots`, `QuestLogic`, `QuestState`, `QuestEntry`, `client/ui/QuestClientLogic`, `RerollButtonWidget`, `ProfessionsClient` |
| Passive-Arbeit | Leiste je Beruf, ein Schritt an der Station | `passive/PassiveLogic`, `PassiveWork` + `passive/*Work` |
| Passive Stationsblöcke | Menü, Redstone-Puls, Komparator, nicht begehbar; Basis aller Passive-Stationen | `station/PassiveStationBlock`, `FacingPassiveStationBlock`, `ThreeInThreeOutMenu`, `client/ui/ThreeInThreeOutScreen` |
| Korb-Station (Farmer + Fisherman) | 5 Slots im Trichterfenster, Füllstand, Trichter unten | `basket/BasketStationBlock`, `BasketStationBlockEntity`, `BasketBlockEntities` |
| Last tragen (Farmer + Fisherman) | Leiste = Last (Kapazität 3–9); voll oder 1 min Stillstand → zum Korb, dort alle 540 ÷ Kapazität Ticks eine Einheit würfeln | `passive/CarryingWork`, `PassiveCounters` (Attachment), `PassiveLogic.withLoadMeter`; Kern-Haken `SectionLogic.fillsFromWork`, `ExtensionHooks.registerStayPut` |
| Entdeckerkarten als Trade-Output | Trade-Erweiterung `explorer_map` (Struktur, Feature, Ader); Suche einmal je Villager + Trade, Geoden/Verliese/Adern aus dem Weltseed ohne Chunk-Laden | `trade/ExplorerMap`, `ExplorerMaps`, `explore/FeatureLocator`, `OreVeinLocator`, `VeinType`, `ChunkSearch`, `mixin/ChunkGeneratorAccessor` |
| Kartenzeichnen (Passive) | Station nimmt Karten, zeichnet über Arbeitstage eine Karte zu zufälliger Struktur; Pools als Registry `map_pool` | `mapping/MapPool`, `MapTargets`, `MapStationBlock`, `MapStationBlockEntity`, `MappingBlocks`, `passive/MappingWork`, `map_pool/adventure.json`, `challenge.json` |
| Debug | `/vo locate geode|copper_vein|iron_vein|dungeon`, `/vo counters`, `/vo passive fill` | `explore/LocateCommand`, `passive/CountersCommand`, `PassiveFillCommand` |
| Stations-Partikel | Blockmodell-`particle` = Vanilla-Workstation des Berufs (lectern_sides, stonecutter_bottom, smithing_table_front, grindstone_side, cartography_table_side3, blast_furnace_front, composter_side, barrel_side) | `models/block/*.json` |

**Geplant**
- Kern-API-Ausbau, der die Extension betrifft (Components statt `enchantment`/`mob`, `TradeRequirement` statt XP-Hook): [progression.md](progression.md)
- Alle Features sind im Spiel noch ungetestet – Tests: Obsidian `04_Testliste`

## Librarian
| Feature | Was | Wo |
|---|---|---|
| Items, Blöcke | Laterne, Namensschild, Schreib- und Verbesserungsstation, Bücherstapel (bis 4 Bücher) | `librarian/LibrarianItems`, `LibrarianBlocks`, `LibrarianBlockEntities`, `BookPileBlock`, `ModelShapedBlock` |
| XP abfüllen | Erfahrungsfläschchen = 55 XP; Villager-XP-Flasche + Kugel | `librarian/ExperienceBottles`, `VillagerExperienceBottleItem`, `ThrownVillagerExperienceBottle`, `VillagerExperienceOrb`, `LibrarianEntities`, `mixin/ThrownExperienceBottleMixin`, `client/render/` |
| Spezialbücher | vier Bücher; Zaubertisch nur Kategorie, Ergebnis verzaubertes Buch | `librarian/SpecialBookItem`, `mixin/EnchantmentMenuMixin`, `tags/enchantment/*_book.json` |
| Verbesserungsstation | 5 Slots, Trichter, Komparator | `librarian/EnchantmentStationBlock`, `EnchantmentStationBlockEntity`, `EnchantmentStationMenu`, `client/ui/EnchantmentStationScreen` |
| Buch-Upgrade (Passive) | niedrigste Stufe zuerst, Stufen-Obergrenze, Gang zur Station | `passive/BookUpgradeWork`, `librarian/BookUpgrades` |
| Lore-Schriftrollen | 12 Items, Buch-Fenster, Fundorte per Loot-Modifier | `librarian/LoreScrollItem`, `network/OpenLoreScrollPayload`, `client/ui/LoreScrollScreens`, `data/smartvillager_professions/loot_modifiers/`, `loot_table/lore_scroll/` |

**Geplant**
- Papierstapel-Block (Item-Icon da)
- 36 Lore-Fragment-Texte (Platzhalter), Balancing, Texturen

## Mason
| Feature | Was | Wo |
|---|---|---|
| Blöcke, Items | Steinchen `rock_pile`, Wegsteine `rock_path` (je bis 4), Metamorphstation | `mason/MasonBlocks`, `MasonBlockEntities`, `MasonItems`, `RockPileBlock`, `RockPathBlock` |
| Brauner Sandstein | ganze Familie + brauner Sand, Vanilla-Sandstein mit anderen IDs | `mason/MasonBlocks`, `textures/block/*brown_sand*.png`, Tags in `data/minecraft/tags/` |
| Brechstation + Brech-Passive | 3 + 3 Slots; Steinmetz-Rezepte rückwärts + Regeln; Batches, Gang zur Station | `mason/CrushingStationBlock`, `CrushingStationBlockEntity`, `CrushingRecipes`, `CrushingRule`, `passive/CrushingWork`, `crushing/*.json` |
| Dorfbewohner-Statuen | je Typ ein Block, 2 hoch, grau gerendert; Trade liefert die Statue des Villager-Typs; +10 % Glück im Umkreis | `mason/VillagerStatueBlock`, `StatueFigure`, `VillagerStatueBlockEntity`, `VillagerStatueHappiness`, `trade/VillagerStatueOutput`, `client/render/VillagerStatue*`, `GrayscaleTextures` |
| Placeholder | Würfel für Blöcke ohne Grafik | `models/block/placeholder_block.json`, `textures/block/placeholder_texture.png` |
| Geoden-Karte | Entdeckerkarte | `exchange/mason_*.json` |

**Geplant**
- Grafiken (Metamorphstation, Fenster), Balancing

## Runesmith (Toolsmith)
| Feature | Was | Wo |
|---|---|---|
| Bücher | Haltbarkeit I, Reparatur (Smaragd + Buch → Buch Stufe I) | `exchange/runesmith_*.json`, `data/ItemAmount.toStack` (Feld `enchantment`) |
| Upgrade-Vorlage | eine Vorlage, Vanilla-`smithing_transform`, 1 Block der Zielstufe, Haltbarkeit relativ | `runesmith/RunesmithItems`, `UpgradeTemplates`, `recipe/*_smithing.json`, `mixin/SmithingUpgradeDurabilityMixin` |
| Reparaturstation + Passive | 3 + 3, Schleifstein-Geräusch | `runesmith/RepairStationBlock`, `RepairStationBlockEntity`, `RunesmithBlocks`, `RunesmithBlockEntities`, `passive/RepairWork` |
| Polster | +10 % Haltbarkeit auf Passive-Rang 6, frisst Schaden zuerst | `runesmith/BonusDurability`, `mixin/ItemStackBonusDurabilityMixin` |

**Geplant**
- Basic Trades Rang 0–3 + 5–6, Master 2, Quests (Easy/Hard-Pool), Balancing, Grafiken
- Pferde-/Nautilus-Rüstung upgradebar?

## Veteran (Weaponsmith)
| Feature | Was | Wo |
|---|---|---|
| Mob-Waffen | markierte Vanilla-Items mit Namen + Treffer-Effekt, 25 % Drop; Quests verlangen sie | `veteran/MobWeapons`, `MobWeaponEvents`, `MobWeaponDefinition`, `MobWeaponTooltip`, `mixin/ItemStackMobWeaponNameMixin`, `mob_weapon/*.json` |
| Piglin-Armbrust | spannt 20 % schneller | `mixin/CrossbowChargeMixin`, `mob_weapon/piglin.json` |
| Trappings-Station, Fernglas, Rahmen | Master-Workstation + Basic 0–1 | `veteran/VeteranBlocks`, `VeteranItems` |
| Waffenständer | Wand-Block, eine Waffe, Rechtsklick | `veteran/WeaponRackBlock`, `WeaponRackBlockEntity`, `VeteranBlockEntities`, `client/render/WeaponRackRenderer` |
| Challengestation (Passive) | 1 Eingabe + 4 Ausgabe; zeichnet Herausforderungskarten über Arbeitstage | `mapping/*`, `passive/MappingWork`, `exchange/veteran_charting_station.json`, `map_pool/challenge.json` |
| Zombie Villager Cage | Block 1×2 hält einen Zombie-Villager als Daten; Schwäche-Trank + goldener Apfel heilen; Eisengolem trägt Zombie-Villager hinein (greift ihn nicht an, wenn ein leerer Cage nahe ist; bei Treffer fallen lassen, betäubt) | `cage/Cages`, `ZombieVillagerCageBlock`, `ZombieVillagerCageBlockEntity`, `CarryZombieVillagerGoal`, `ZombieCaptives`, `CageAware`, `CarryPose`, `mixin/CageIronGolemMixin`, `CageMobMixin`, `client/render/CageRenderer`, `CarryClient`, `cage/test/CageGameTests` |

**Geplant**
- Basic Rang 5–6, Masteries mit düsteren Items (rostige Eisenbarren, flackernde Laterne, Skelett-Deko)
- Challengestation im Spiel prüfen (Dauer, Pool-Gewichte), Modell/Textur
- Waffenständer: Trichter/Komparator?

## Cartographer
| Feature | Was | Wo |
|---|---|---|
| Kartenstation (Master) | vier Masteries: Abenteuer-, Herausforderungs-, Höhlen-, Netherkarte | `cartographer/CartographerBlocks`, `CartographerItems`, `models/item/adventure_map.json`, `challenge_map.json` |
| Abenteuerstation (Passive) | 3 + 3; zeichnet Abenteuerkarten zu Strukturen | siehe Kartenzeichnen |
| Höhlen- / Netherkarte | leere Karte, Rechtsklick in einer Ebene → gefüllte Karte, an die Ebene gebunden; 32 Strahlen, Schattierung, Pixel bleiben gespeichert; Nether in vier Ebenen; Rauszoomen mit Papier | `cavemap/CaveMapKind`, `CaveLayer`, `CaveMapDrawer`, `CaveMapMemory`, `EmptyCaveMapItem`, `CaveMapItem`, `CaveMapItems`, `mapping/test/MapGameTests` |

**Geplant**
- Basic Trades Rang 0–3 (Idee: Fernrohr), Quests (Idee: Tonscherben), Modelle/Texturen, eigenes Symbol für gefüllte Karten
- Höhlen-/Netherkarte im Spiel prüfen (Reichweite, Ebenen, Optik)

## Goldsmith (Armorer)
| Feature | Was | Wo |
|---|---|---|
| Vergoldete Kette / Gitter | Smaragd + Vanilla-Kette/-Gitter (Seabrass-Texturen aus Abyssal Decor) | `goldsmith/GoldsmithBlocks` |
| Vergoldungsstation (Master 1–4) | Metall + Block → verstärktes Glas (Connected Textures), eisengerahmter Redstone (+ Treppe, Stufe, Ornated, Säule; nur Grundblock gibt Signal), goldgerahmter Lapis (+ Varianten), vergoldeter Schwarzstein; Steinmetz-Varianten | `goldsmith/GoldsmithBlocks`, `ReinforcedGlassBlock`, `OrnatedBlock`, `OrnatedRedstoneBlock`, `recipe/*_stonecutting.json`, `exchange/goldsmith_*.json` |
| Verwertungsstation (Passive) | 3 + 3, Metall-Ausrüstung → Barren/Nuggets, Netherite → Diamant-Teil + Scraps; Tiegel animiert beim Arbeiten | `goldsmith/SalvageStationBlock`, `SalvageStationBlockEntity`, `GoldsmithBlockEntities`, `GoldsmithParticles`, `SalvageRule`, `client/render/MoltenBubbleParticle`, `passive/SalvageWork`, `salvage/*.json` |
| Ader-Karten | Kupfer- und Eisenader-Karte als Basic 5/6 | `exchange/goldsmith_*.json` |

**Geplant**
- Basic Trades Rang 2–3, Quests, Mengen der Masteries, Balancing, Grafiken
- Steinmetz-Varianten für Glas und Schwarzstein, hohe Gitter-Teile; Metall-Einlagen in gemeißelten Blöcken (Idee)

## Farmer
| Feature | Was | Wo |
|---|---|---|
| Erntekorbstation (Passive) | er erntet wirklich; Ernte = Einheit auf der Last; am Korb Chance auf goldene Sorte (0 % Rang 0 … 20 % Rang 6) | `farmer/FarmerBlocks`, `HarvestCounting` (`BlockDropsEvent`), `passive/HarvestWork` |
| Glasurstation (Master 1–4) | Kandierter Apfel, goldene Rote-Bete-Suppe, glasierte Karotte, Goldtoast, alle mit Effekt | `farmer/FarmerItems`, `SugarRushEffect`, `exchange/farmer_*.json` |
| Suppen stapeln bis 16 | global, alle Items mit Stapel 1 + Schüssel | `farmer/SoupStacking` |
| Vogelscheuche | Dekoblock 2 hoch; Zombies laufen hin, auf Hard brechen sie sie wie eine Tür | `farmer/ScarecrowBlock`, `ScarecrowGoal`, `ScarecrowEvents`, `exchange/farmer_scarecrow.json` |
| Goldene Items | goldenes Brot/Rote Bete/Kartoffel; Rezept der goldenen Karotte entfernt | `farmer/FarmerItems`, `data/minecraft/recipe/golden_carrot.json` |

**Geplant**
- Basic Trades Rang 2–3 + 5, Quests (Ankauf alles Pflanzliche außer Crops), permanente Trades (Kürbis-/Melonenblock), Balancing, Grafiken (Gemüse-Schichten: Stufe 1 `vegetables_1`, Stufen 2–5 nur `vegetables`)
- Hot Potato (goldene Kartoffel, Kettensprung), Zutaten der Masteries

## Fisherman
| Feature | Was | Wo |
|---|---|---|
| Fischkorbstation (Passive) | er fischt wirklich: freie Sicht, fester Fischplatz, Angel + Schwimmer, Fang = Einheit; 10 Fänge/Tag (Rang 0) bis 30 (Rang 6); am Korb ein Fisch je Einheit | `fisherman/FishermanBlocks`, `VillagerBobber`, `FishermanEntities`, `passive/FishingWork`, `client/render/VillagerBobberRenderer` |
| Biom-Fische | acht Fische je Biomgruppe (Datapack `biome_fish`); Spieler fängt sie mit 8 % | `fisherman/BiomeFish`, `FishermanItems`, `BiomeFishModifier`, `biome_fish/*.json`, `tags/worldgen/biome/fish_*.json`, `loot_modifiers/biome_fish.json` |
| Werftstation | Master-Station ohne Einträge | `fisherman/FishermanBlocks` |

**Geplant**
- Basic Trades Rang 2–3 + 5–6, Quests (Ankauf Lore-Schriftrollen aus dem Meer), permanenter Trade Smaragdfisch, Balancing, Grafiken
- Eigenschaften der Biom-Fische
- Werftstation-Masteries (Boote am 2026-10-07 entfernt, Konzept folgt)

## Noch nicht designt (Ideen in Obsidian)
- **Butcher:** Nutztier-Zucht (Passive: züchtet selbst, Futter in der Station, beschleunigt Babys); Master: Biom-Fisch-Gerichte; Umbenennung geplant
- **Colorist** (ex-Shepherd): Farbeimer, Farbrolle, Farbbombe, Villager-Banner; Quests Ankauf Blumen/Farbstoffe
- **Cleric:** Passive: Tränke kombinieren; Quests: Mobdrops; Master: Transfiguration (unsicher)
- **Fletcher:** noch keine Ideen
- **Leatherworker / Ranger:** Brutstation für Mob-Eier, reitbare Spinnen; Master-Gadgets für Reittiere (Gleit-Pferderüstung, Happy-Ghast-Gondel, Wasser-Kamel), Leatherblock
