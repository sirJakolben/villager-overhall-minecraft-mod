# Smart Iron Golem (`smart-golem/`, `smartgolem`)

Eigenständige Mod (braucht keine der anderen Mods). Ein schmaler Mixin auf den `IronGolem`, sonst Unterklassen von Vanillas Navigation, NeoForge-Events und Vanillas `TemptGoal`; Renderer bleibt Vanilla. Zahlen: Obsidian `02_Tweak-Werte`.
Pfade: Java `smart-golem/src/main/java/com/smartgolem/`. Zombie-Cage-Tragen des Golems: [professions.md](professions.md), Veteran.

## Vorhanden

| Feature | Was | Wo |
|---|---|---|
| Einstieg, Attribute | `tempt_range` 10, Wasser-Effizienz +0,1 (≈ 1,5× Tempo) per Event | `SmartGolemMod`, `GolemAttributes` |
| Mixin | tauscht das Stroll-Goal, hängt `TemptGoal` an, ersetzt die Navigation nur wenn sie noch Vanilla ist | `mixin/IronGolemMixin` |
| Nachtpatrouille | früh/abends/nachts ohne Pause zum am längsten nicht besuchten Pfadblock (16–64 Blöcke), Weg bevorzugt Pfadblöcke; Karte pro Chunk, Dorf = 4 Chunks um die Glocke | `ai/GolemPatrolStrollGoal`, `patrol/PatrolMap`, `PathBlocks`, `PatrolEvents` |
| Klettern | Pfadfinder plant durch Wände aus 2 vollen Blöcken (auch aus Löchern, unter Wasser), Kosten +8; oben reicht eine Reihe Boden (1 tief × 2 breit, die Reihe dahinter ist egal); der echte Golem bewegt sich mit der Animation (gleitet an den Start, hängt still bis 2 s, dann gerade nach oben, Hitbox läuft mit; Client rechnet dieselbe Bahn aus der Zeit); danach weiter; mit Passagier (getragener Zombie-Villager) plant und startet er keine Kletterkante; Laufen/Springen/Stufen Vanilla | `ai/GolemPathNavigation`, `GolemNodeEvaluator`, `GolemClimbs`, `ClimbGeometry`, `climb/GolemClimb`, `climb/ClimbPath` |
| Darstellung | ein Skelett mit Vanillas Würfeln (Rumpf geteilt) + Textur, Haltung pro Frame aus Tabellen, die per `./gradlew :smart-golem:bakeClimb` aus `assets/Block References/iron_golem/iron_golem.bbmodel` entstehen (Blockbench-Kurven inkl. Bézier exakt, 48 Zeilen/s, Fehler unter 0,1 px / 0,2°; Bewegung des Golems herausgerechnet); `tools/gen_golem_climb.ps1` erzeugt nur noch das Zwei-Golem-Gerüst für das Werkzeug (`src/bake`) | `client/ClimbGolemLayer`, `ClimbGolemModel`, `ClimbGolemClient`, `climb/ClimbPose` (erzeugt), `src/bake/.../BakeClimb` |
| Anlocken | Eisenbarren in der Hand, Tempo 0,7, Halteabstand 3 | `IronGolemMixin` |
| Tests | 21 Gametests (`./gradlew :smart-golem:runGameTestServer`) | `test/GolemGameTests` |

Entscheidungen: lieber Umweg bis ca. 8 Blöcke als klettern; klettern überall; nur volle Blöcke. Fallen: Kletterknoten brauchen Vanillas `isNeighborValid` (sonst Endlosschleife in `PathFinder.reconstructPath`); Knoten eines 1,4 breiten Mobs sind Ecken der 2×2-Fläche (`ClimbGeometry`); Kosten am Zielknoten setzen, sonst verbraucht die Kantenlänge die maximale Pfadlänge.

## Geplant

- Animation im Spiel prüfen (Drehrichtungen, Endposition 0,63 Blöcke), Wassergeschwindigkeit messen, Nachtpatrouille im Spiel
- Idee: Eisengolems laufen bevorzugt zu Villagern, Kupfergolem als Begleiter
