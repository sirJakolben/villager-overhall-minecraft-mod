# Smart Villager – Master-Übersicht

Vanilla-artiges Rework aller Villager für Minecraft 26.1 (NeoForge 26.1.0.19-beta, Java 25), ohne Fremd-Libs, Lizenz CC0. Ein Gradle-Projekt, vier Mods in einer Rangleiter (Vanilla → Smart Villager → Progression → Professions; Smart Iron Golem steht daneben); jeder hat eine Feature-Datei mit Vorhandenem (was, wo im Code) und Geplantem (bei Trade Rework je Beruf).

| Baustein | Ordner / Mod-ID | Rolle | Details |
|---|---|---|---|
| Smart Villager | `smart-villager/` · `smartvillager` | unterste Stufe, braucht nichts: Claim und Anlocken per Smaragd, Handelsblock, doppelt so viele Zombie-Villager mit Heilen per goldenem Essen, Brauständer mit Brennstoff | [docs/smart-villager.md](docs/smart-villager.md) |
| Smart Villager Progression | `smart-villager-progression/` · `smartvillager_progression` | Sektionen, Leveln durch Arbeit, Handelsfenster, Restock, Glück, Arbeitsplätze; Vanilla-Trades für jeden Beruf; braucht Smart Villager | [docs/progression.md](docs/progression.md) |
| Smart Villager Professions | `smart-villager-professions/` · `smartvillager_professions` | die designten Berufe, Quests/Masteries/Passive; braucht Progression | [docs/professions.md](docs/professions.md) |
| Smart Iron Golem | `smart-golem/` · `smartgolem` | Nachtpatrouille, Klettern, Anlocken; eigenständig | [docs/smart-golem.md](docs/smart-golem.md) |

Abhängigkeit nur nach unten, wie eine Leiter: Professions → Progression (über `api/ExtensionHooks`) → Smart Villager (über `api/SmartVillagerApi`); keine Stufe importiert etwas aus einer höheren. Smart Villager und `smart-golem` hängen von nichts ab.

## Wo steht was

| Was | Wo |
|---|---|
| Arbeitsregeln für Sessions (inkl. Pflege der Dokumente) | [CLAUDE.md](CLAUDE.md) (in jeder Session geladen) |
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
| `./gradlew :smart-villager-professions:runClient` | alle drei Villager-Stufen + Golem (`run/`) |
| `./gradlew :smart-villager-progression:runClient` | Smart Villager + Progression (`run-core/`) |
| `./gradlew :smart-villager:runClient` | nur Smart Villager (`run-villager/`) |
| `./gradlew :smart-golem:runClient` | nur Golem (`run-golem/`) |
| `./gradlew <mod>:runGameTestServer` | Ladetest + Gametests ohne Fenster |

## Grundentscheidungen und Fallen

- Zustand als Data Attachment am Vanilla-Villager (kein eigener Entity-Typ): Spawning, Zucht, Raids, Heilen bleiben Vanilla
- Vanilla-Erhalt: so spät und eng wie möglich eingreifen (Regeln in `CLAUDE.md`)
- Handelsliste = Ansicht des eigenen Zustands (Vanillas `MerchantOffers` wird neu gebaut); Werte als Datapack-JSON
- Datapack-Falle: Einträge liegen unter `data/<Namespace der Datei>/<Namespace der Registry>/<registry>/` (Extension-Trades: `data/smartvillager_professions/smartvillager_progression/exchange/`)
- 26.1: `ResourceLocation` heißt `Identifier`; Screens rendern über `GuiGraphicsExtractor` / `extractBackground`
- Credits: vergoldete Eisenkette/-gitter und Eisenrahmen von verstärktem Glas aus Abyssal Decor von starrysock (CC0, https://modrinth.com/mod/abyssal-decor); Chipped (Terrarium): Texturen nicht übernehmbar
- NeoForge-Doku: [Attachments](https://docs.neoforged.net/docs/datastorage/attachments/), [Registries](https://docs.neoforged.net/docs/concepts/registries/)
