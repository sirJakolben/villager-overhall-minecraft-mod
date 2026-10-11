# Smart Iron Golem (`smart-golem/`, `smartgolem`)

Eigenständige Mod (braucht weder Kern noch Extension). Ein schmaler Mixin auf den `IronGolem`, sonst Unterklassen von Vanillas Navigation, NeoForge-Events und Vanillas `TemptGoal`; Renderer bleibt Vanilla. Zahlen: Obsidian `02_Tweak-Werte`.
Pfade: Java `smart-golem/src/main/java/com/smartgolem/`. Zombie-Cage-Tragen des Golems: [trade-rework.md](trade-rework.md), Veteran.

## Vorhanden

| Feature | Was | Wo |
|---|---|---|
| Einstieg, Attribute | `tempt_range` 10, Wasser-Effizienz +0,1 (≈ 1,5× Tempo) per Event | `SmartGolemMod`, `GolemAttributes` |
| Mixin | tauscht das Stroll-Goal, hängt `TemptGoal` an, ersetzt die Navigation nur wenn sie noch Vanilla ist | `mixin/IronGolemMixin` |
| Nachtpatrouille | früh/abends/nachts ohne Pause zum am längsten nicht besuchten Pfadblock (16–64 Blöcke), Weg bevorzugt Pfadblöcke; Karte pro Chunk, Dorf = 4 Chunks um die Glocke | `ai/GolemPatrolStrollGoal`, `patrol/PatrolMap`, `PathBlocks`, `PatrolEvents` |
| Klettern | Pfadfinder plant durch Wände aus 2 vollen Blöcken (auch aus Löchern, unter Wasser), Kosten +8, 4 s Animation, danach weiter; Laufen/Springen/Stufen Vanilla | `ai/GolemPathNavigation`, `GolemNodeEvaluator`, `GolemClimbs`, `ClimbGeometry`, `climb/GolemClimb` |
| Darstellung | Render-Ebene + Modell + Animation, erzeugt aus `assets/Block References/iron_golem/iron_golem.bbmodel` per `tools/gen_golem_climb.ps1` (nicht von Hand ändern) | `client/ClimbGolemLayer`, `ClimbGolemModel`, `ClimbGolemParts`, `ClimbGolemAnimations`, `ClimbGolemClient` |
| Anlocken | Eisenbarren in der Hand, Tempo 0,7, Halteabstand 3 | `IronGolemMixin` |
| Tests | 16 Gametests (`./gradlew :smart-golem:runGameTestServer`) | `test/GolemGameTests` |

Entscheidungen: lieber Umweg bis ca. 8 Blöcke als klettern; klettern überall; nur volle Blöcke. Fallen: Kletterknoten brauchen Vanillas `isNeighborValid` (sonst Endlosschleife in `PathFinder.reconstructPath`); Knoten eines 1,4 breiten Mobs sind Ecken der 2×2-Fläche (`ClimbGeometry`); Kosten am Zielknoten setzen, sonst verbraucht die Kantenlänge die maximale Pfadlänge.

## Geplant

- Animation im Spiel prüfen (Drehrichtungen, Endposition 0,63 Blöcke), Wassergeschwindigkeit messen, Nachtpatrouille im Spiel
- Idee: Eisengolems laufen bevorzugt zu Villagern, Kupfergolem als Begleiter
