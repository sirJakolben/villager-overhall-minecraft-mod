# Smart Iron Golem (eigene Mod `smart-golem/`, Mod-ID `smartgolem`)

- Eigene Mod, braucht weder Kern noch Extension; alle Golem-Änderungen liegen hier (Stand 2026-10-09)
- So nativ wie möglich: ein schmaler Mixin auf den `IronGolem` (`mixin/IronGolemMixin`), Unterklassen von Vanillas Navigation (`ai/`), NeoForge-Events, Vanillas `TemptGoal`; der Renderer bleibt Vanilla (Klettern = zusätzliche Render-Ebene)
- Plan und Begründung: `plan-iron-golem-ai.md`; Tests: `smart-golem/.../test/GolemGameTests` (`./gradlew :smart-golem:runGameTestServer`)

## Verhalten
- Tag (Uhrzeit 1000–11000): Vanilla; früh/abends/nachts: Patrouille (`GolemPatrolStrollGoal`, Unterklasse von `GolemRandomStrollInVillageGoal`)
- Patrouillenkarte (`PatrolMap`): pro Chunk ein Pfadblock (Dirt Path zuerst, sonst Gravel nicht unter Wasser, Cobblestone, Mud, Packed Mud, Mud Bricks, Coarse Dirt) nahe der Chunk-Mitte; Dorf = 4 Chunks um die nächste Glocke (ohne Glocke: um den Golem); füllt sich beim Patrouillieren (2 Chunks pro Planung, nach Neustart neu), neue Pfadblöcke von Spielern kommen sofort dazu
- Ziel = der am längsten nicht besuchte Punkt (16–64 Blöcke); der Weg bevorzugt Pfadblöcke (Patrouillenmodus der Pfadbewertung: +1 Kosten je Knoten abseits der Pfade); Punkt ohne Weg → 5 Min gesperrt
- Klettern: der Pfadfinder plant durch Wände aus genau 2 vollen Blöcken (über die ganze Golembreite, oben Platz), auch aus Löchern und unter Wasser; Kosten +8 (lieber Umweg bis ca. 8 Blöcke); der Golem geht bis an den Fuß der Wand, springt nie, klettert 4 s (Animation `2_block_climb`, er kann sich nicht bewegen oder drehen): der Golem steht von Anfang an oben auf der Kante, nur sein Modell wird unten gezeichnet (gleitet in 6 Ticks an den Start) und die Animation endet genau in der Stehpose oben – kein Teleport, kein Ruckeln; danach läuft er weiter; Laufen/Springen/1-Block-Stufen bleiben Vanilla
- Anlocken: Eisenbarren in der Hand → Vanillas `TemptGoal` (Priorität 3, Tempo 0,7, Halteabstand 3, Reichweite = Attribut `tempt_range` 10)
- Wasser: Attribut `water_movement_efficiency` +0,1 → ca. 1,5-fache Geschwindigkeit

## Werte
| Wert | Standard | Wo |
|---|---|---|
| Tag von / bis | 1000 / 11000 Ticks | `GolemPatrolStrollGoal.DAY_START/DAY_END` |
| Zielentfernung | 16–64 Blöcke | `GolemPatrolStrollGoal.MIN_DISTANCE/MAX_DISTANCE` |
| Dorf-Umkreis | 4 Chunks | `PatrolMap.VILLAGE_CHUNKS` |
| Scans pro Planung | 2 Chunks | `PatrolMap.SCAN_PER_PLAN` |
| Pause unerreichbarer Punkt | 6000 Ticks | `PatrolMap.UNREACHABLE_RETRY_TICKS` |
| Aufschlag abseits der Pfade | 1,0 | `GolemNodeEvaluator.OFF_PATH_COST` |
| Kletterkosten | 8,0 | `GolemNodeEvaluator.CLIMB_COST` |
| Mehr Suchknoten der Pfadsuche | ×3 | `GolemPathNavigation.MAX_VISITED_FACTOR` |
| Klettern, Dauer | 80 Ticks (4 s) | `GolemClimbs.CLIMB_TICKS` |
| Start vor der Wand / Ende hinter der Fläche | 1,0 / 0,13 Blöcke (Modell startet 1 Block vor der Wand, Golem steht am Ende fast an der Kante) | `ClimbGeometry.START_DISTANCE/END_PAST_FACE` |
| Anlocken Tempo / Halteabstand | 0,7 / 3,0 | `IronGolemMixin.TEMPT_SPEED/TEMPT_STOP_DISTANCE` |
| tempt_range / Wasser-Effizienz | 10 / +0,1 | `GolemAttributes` |

## Modell und Animation
- Blockbench-Projekt: `assets/Block References/iron_golem/iron_golem.bbmodel`
- `ClimbGolemParts` und `ClimbGolemAnimations` werden daraus per Skript erzeugt: `tools/gen_golem_climb.ps1` (nicht von Hand ändern)
- Bezier-Keyframes werden Catmull-Rom (Vanilla kennt nur linear und Catmull-Rom), ein „step“ ist ein Sprung von 1 ms
