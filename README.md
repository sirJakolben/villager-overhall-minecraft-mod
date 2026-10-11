# Villager Overhaul – Master-Übersicht

Vanilla-artiges Rework aller Villager für Minecraft 26.1 (NeoForge 26.1.0.19-beta, Java 25), ohne Fremd-Libs, Lizenz CC0. Ein Gradle-Projekt, vier Bausteine; jeder hat eine Feature-Datei mit Vorhandenem (was, wo im Code) und Geplantem (bei Trade Rework je Beruf).

| Baustein | Ordner / Mod-ID | Rolle | Details |
|---|---|---|---|
| Villager Overhaul – Kern | `core/` · `villageroverhaul` | Sektionen, Leveln durch Arbeit, Handelsfenster, Restock, Glück, Arbeitsplätze; Vanilla-Trades für jeden Beruf | [docs/core.md](docs/core.md) |
| Villager Overhaul: Trade Rework | `trade-rework/` · `vo_trade_rework` | die designten Berufe, Quests/Masteries/Passive; braucht den Kern | [docs/trade-rework.md](docs/trade-rework.md) |
| Smart Iron Golem | `smart-golem/` · `smartgolem` | Nachtpatrouille, Klettern, Anlocken; eigenständig | [docs/smart-golem.md](docs/smart-golem.md) |
| Smart Villager | (heute im Kern) | Claim per Smaragd, Anlocken, Handelsblock | [docs/smart-villager.md](docs/smart-villager.md) |

Abhängigkeit nur in eine Richtung: Extension → Kern (über `api/ExtensionHooks`), Kern kennt nie die Extension; `smart-golem` kennt keinen der beiden.

## Wo steht was

| Was | Wo |
|---|---|
| Arbeitsregeln für Sessions (inkl. Pflege der Dokumente) | [arbeitsregeln.md](arbeitsregeln.md) (geladen über `CLAUDE.md`) |
| Zahlen, Trade-Tabellen, Design je Beruf | Obsidian `Jakobs Vault/Improved Villagers/` (`02_Tweak-Werte`, `<Beruf>.md`) |
| Ungetestetes, Edge Cases | Obsidian `04_Testliste` |
| Fehlende Texturen/Modelle | Obsidian `03_Fehlende Texturen und Modelle` |
| Fortschritt je Beruf | Obsidian `01_Entwicklungsplan` |
| Rohe Grafiken (Blockbench, Pixelorama, PNG) | `assets/` |
| Skripte | `tools/` |

## Bauen und Starten

| Befehl | Wirkung |
|---|---|
| `./gradlew build` | alle Mods bauen (Jars in `<mod>/build/libs/`) |
| `./gradlew :trade-rework:runClient` | Kern + Extension (`run/`) |
| `./gradlew :core:runClient` | nur Kern (`run-core/`) |
| `./gradlew :smart-golem:runClient` | nur Golem (`run-golem/`) |
| `./gradlew <mod>:runGameTestServer` | Ladetest + Gametests ohne Fenster |

## Grundentscheidungen und Fallen

- Zustand als Data Attachment am Vanilla-Villager (kein eigener Entity-Typ): Spawning, Zucht, Raids, Heilen bleiben Vanilla
- Vanilla-Erhalt: so spät und eng wie möglich eingreifen (Regeln in `arbeitsregeln.md`)
- Handelsliste = Ansicht des eigenen Zustands (Vanillas `MerchantOffers` wird neu gebaut); Werte als Datapack-JSON
- Datapack-Falle: Einträge liegen unter `data/<Namespace der Datei>/<Namespace der Registry>/<registry>/` (Extension-Trades: `data/vo_trade_rework/villageroverhaul/exchange/`)
- 26.1: `ResourceLocation` heißt `Identifier`; Screens rendern über `GuiGraphicsExtractor` / `extractBackground`
- Credits: vergoldete Eisenkette/-gitter und Eisenrahmen von verstärktem Glas aus Abyssal Decor von starrysock (CC0, https://modrinth.com/mod/abyssal-decor); Chipped (Terrarium): Texturen nicht übernehmbar
- NeoForge-Doku: [Attachments](https://docs.neoforged.net/docs/datastorage/attachments/), [Registries](https://docs.neoforged.net/docs/concepts/registries/)
