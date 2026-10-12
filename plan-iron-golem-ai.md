# Plan: Eisengolem-KI – Umbau auf Vanilla-nahe Eingriffe (2026-10-09)

- Ziel: dieselben Features wie jetzt (Nachtpatrouille, Klettern an 2-Block-Wänden, Anlocken mit Eisenbarren, schneller im Wasser), aber **so nativ wie möglich**: ein schmaler Mixin auf den `IronGolem`, sonst Vanillas eigene Klassen
- Status: nur Plan, noch nichts umgesetzt; offene Fragen am Ende

## 1. Verstehen – wie die Vanilla-KI des Golems aufgebaut ist

- `IronGolem.registerGoals` (Prioritäten, kleiner = wichtiger): 1 `MeleeAttackGoal`, 2 `MoveTowardsTargetGoal`, 2 `MoveBackToVillageGoal`, 4 `GolemRandomStrollInVillageGoal`, 5 `OfferFlowerGoal`, 7 `LookAtPlayerGoal`, 8 `RandomLookAroundGoal`; Ziel-Auswahl (`targetSelector`) bleibt komplett unberührt
- `GolemRandomStrollInVillageGoal extends RandomStrollGoal`: Intervall 240 Ticks, wählt in `getPosition()` einen Zielpunkt (Villager, Dorf-POI oder zufällig) – **genau der Haken für die Patrouille**
- Bewegung läuft immer gleich: Goal → `PathNavigation.moveTo` → `PathFinder` (A*) + `WalkNodeEvaluator` (welche Nachbarknoten gibt es) → `followThePath` (Knoten abarbeiten) → `MoveControl` (laufen, bei Höhenunterschied springen)
- `WalkNodeEvaluator.getNeighbors`: erlaubt Nachbarn nur bis `floor(max(1, Stufenhöhe))` höher; der Golem hat Stufenhöhe 1,0 → 1 Block hoch geht (Stufe), 2 Blöcke sind für den Pathfinder Wand
- Pfadlänge: `FOLLOW_RANGE` (Mob-Standard 16) begrenzt den Suchradius; weiter entfernte Ziele werden in Teilstücken angelaufen (Vanilla-Verhalten, kein Sonderfall nötig)
- `TemptGoal` (Anlocken, z. B. Kühe) liest das Attribut `tempt_range` – der Golem hat es nicht (Absturz von vorhin), alle Tiere haben es
- Wichtigster Fund: `PathFinder.distance(from, to)` ist `protected` (Kosten pro Kante), `NodeEvaluator.getNode/getCachedPathType` sind `protected`, `PathNavigation.createPathFinder/tick/followThePath` sind überschreibbar → ein eigener Pathfinder ist **Unterklassen-Arbeit**, kein Umschreiben

## 2. Zielbild – wie sich der Golem verhalten soll

- **Laufen/Springen**: Vanilla (Stufe 1 Block, kein Hüpfen über 2 Blöcke)
- **Pathfinding**: plant zusätzlich eine „Kletterkante“ über eine 2 Blöcke hohe Wand (auch aus einem Loch heraus); geht den Weg bis zum Fuß der Wand, bleibt stehen, klettert (Animation, 4 s), steht oben und läuft weiter
- **Patrouille** (früh/abends/nachts): Streifen ohne Pause, Ziele aus der Patrouillenkarte (Pfadblock je Chunk, am längsten nicht besucht); der Weg bevorzugt Pfadblöcke
- **Anlocken**: Eisenbarren in der Hand → folgt wie eine Kuh dem Weizen
- **Wasser**: ca. 1,5-fache Geschwindigkeit
- Alles andere (Angriff, Dorfverteidigung, Blume, Zurück-ins-Dorf, Zielwahl) bleibt Vanilla

## 3. Planen – welche Vanilla-Klasse für welchen Zweck

| Zweck | Vanilla-Klasse / Haken | Eingriff |
|---|---|---|
| Tag-Stroll → Patrouille | `GolemRandomStrollInVillageGoal` | **Unterklasse** `GolemPatrolStrollGoal`: `getPosition()` nachts = Punkt der Patrouillenkarte, sonst `super`; nachts `interval` = 1 (Vanilla-API `setInterval`). Alles andere (`canUse`, `canContinueToUse`, `start/stop`) bleibt Vanilla |
| Goal austauschen | `IronGolem.registerGoals` | Mixin `@WrapOperation` auf **genau den einen** `new GolemRandomStrollInVillageGoal(..)`-Aufruf (kettet mit anderen Mods, die ihn auch umwickeln) |
| Anlocken | `TemptGoal` | **Vanilla-Klasse unverändert**, Mixin `@Inject` am Ende von `registerGoals`: `addGoal(3, new TemptGoal(golem, 0.7, Eisenbarren, false, 3.0))` (Priorität wie bei Tieren). Fehlendes Attribut: NeoForge-Event `EntityAttributeModificationEvent` → `TEMPT_RANGE` (10) für den Golem – **kein Mixin dafür** |
| Wasser | Attribut `WATER_MOVEMENT_EFFICIENCY` | wie bisher (+0,1); prüfen, ob der Attribut-Event den Basiswert überschreiben darf, sonst bleibt der Modifier beim Spawn |
| Navigation | `GroundPathNavigation` | **Unterklasse** `GolemPathNavigation`: `createPathFinder` liefert unseren Pathfinder + Evaluator; `tick`/`followThePath` bekommen den Kletter-Haken (s. u.) |
| Navigation einsetzen | `Mob.createNavigation` (im Konstruktor) | Mixin `@Inject` am Ende des `IronGolem`-Konstruktors: `navigation = new GolemPathNavigation(..)`, **nur wenn** sie noch eine ganz normale `GroundPathNavigation` ist (fremde Mods bleiben unberührt) |
| Knoten | `WalkNodeEvaluator` | **Unterklasse** `GolemNodeEvaluator`: `getNeighbors` = `super` + je Himmelsrichtung eine „Kletterkante“ (s. u.) + im Patrouillenmodus Aufschlag auf Knoten, die kein Pfadblock sind |
| Kantenkosten | `PathFinder.distance` | **Unterklasse** (anonym in `createPathFinder`): Kante mit Höhensprung ≥ 2 kostet Zuschlag (Klettern ist langsam: lieber 8 Blöcke Umweg als klettern) |
| Klettern-Zustand | NeoForge-Datenanhang | `GolemClimb` (schon da, synchronisiert zum Client): Start, Halteposition, Ziel |
| Darstellung | `IronGolemRenderer` | wie jetzt; siehe offene Frage 3 (Renderer ersetzen oder nur das Modell tauschen) |

### 3.1 Die Kletterkante im Pathfinder (Kern des Umbaus)

- `GolemNodeEvaluator.getNeighbors(neighbors, pos)`: nach `super` für jede der vier Richtungen prüfen:
  - Wand: Block auf Höhe `pos.y` und `pos.y+1` in dieser Richtung hat Kollision
  - Platz oben: der Block auf `pos.y+2`, `+3`, `+4` (Golemhöhe 2,7) ist frei (Kollisionsprüfung mit der Golem-Hitbox am Zielknoten, `noCollision`)
  - dann Knoten `getNode(x+dx, y+2, z+dz)` mit Typ `WALKABLE` und `costMalus` = 0 zurückgeben (Kosten kommen aus `PathFinder.distance`)
- Eine Kante ist „Klettern“, wenn der Höhenunterschied zweier aufeinanderfolgender Pfadknoten ≥ 2 ist – dafür braucht es **kein Zusatzfeld im Knoten**
- Der Pfad kann damit aus einem 2 Blöcke tiefen Loch heraus über die Lochwand führen (gewollt) und über Wände im Dorf

### 3.2 Klettern ausführen (statt Kollisions-Erkennung wie jetzt)

- `GolemPathNavigation.tick()` (nach `super.tick()`): ist der nächste Knoten eine Kletterkante und der Golem steht am Fuß (vorheriger Knoten erreicht) → Kletter-Start
- Kletter-Start: `MoveControl` auf die eigene Position setzen (verhindert den Vanilla-Sprung in diesem Tick), Golem 0,5 Blöcke vor die Wandfläche in die Blockmitte, zur Wand drehen, `GolemClimb` setzen
- Während des Kletterns (80 Ticks): `tick()` tut nichts (keine Pfadfolge, keine Stuck-Erkennung), Position und Blickrichtung werden jeden Tick festgehalten
- Ende: Golem auf die Kante setzen (0,63 Blöcke hinter der Fläche, 2 Blöcke höher), den Kletterknoten im Pfad abhaken (`path.advance()`), Pfad geht normal weiter – **die Navigation wird nicht gestoppt**, der Golem läuft sofort weiter
- Fällt das Ziel während des Kletterns weg (Spieler lässt den Barren los, Mob stirbt): Klettern wird zu Ende geführt, danach entscheidet die KI neu
- Fällt die Oberseite zu (Block gesetzt): Abbruch beim Start/Ende, Golem bleibt unten, Pfad wird neu berechnet
- Entfällt: `GolemClimbing`-Kollisionsverfolgung (`BLOCKED_TICKS`, `goalOf`, `requestClimb`, Aufruf aus der Patrouille)

### 3.3 Patrouille

- `GolemPatrolStrollGoal.getPosition()`: nachts `PatrolMap.next(..)` (Patrouillenkarte bleibt, inkl. Glocken-Umkreis, Scan, Ereignisse beim Pfad-Legen); gibt es keinen Punkt → `super.getPosition()` (Vanilla-Zielwahl)
- Der **Wegbefehl** ist jetzt nur noch `moveTo(Punkt)` – kein eigener BFS/Wegpunkt-Zug mehr; die Pfadnähe kommt aus dem Evaluator:
  - Patrouillenmodus der Navigation (Flag, das das Goal in `start()` setzt und in `stop()` löscht)
  - dann +Aufschlag je Knoten, dessen Boden kein Pfad-/Ersatzblock ist (gleiche Liste `PathBlocks`)
  - A* wählt von selbst Wege über Pfadblöcke, solange der Umweg kleiner als der Aufschlag-Gewinn ist
- Weit entfernte Punkte: Teilstücke durch `FOLLOW_RANGE` – das Goal wird nachts mit Intervall 1 sofort neu ausgelöst
- Unerreichbarer Punkt: Pfad `null` oder `!canReach()` und Golem steht → `PatrolMap.unreachable(..)` (5 min Sperre) – bleibt wie bisher
- Angreifen und danach zurück: Vanilla-Prioritäten (1–2 schlagen 4) reichen; nach dem Kampf wählt das Goal den nächsten Punkt der Karte
- Entfällt: `PatrolGoal` (eigene Goal-Klasse mit Route/Wegpunkten/Stuck-Logik), `PatrolMap` bleibt

### 3.4 Anlocken

- Entfällt: `IngotLureGoal` (eigene Klasse)
- Stattdessen Vanillas `TemptGoal` + Attribut `tempt_range` (Event) + Mixin-Inject am Ende von `registerGoals`
- Der Pfad zum Spieler läuft durch die eigene Navigation → der Golem klettert dabei automatisch über 2-Block-Wände
- Gleiche Priorität wie bei Tieren (3, also vor dem Streifen, nach Angriff und Zurück-ins-Dorf)

### 3.5 Wasser

- Wie jetzt: `WATER_MOVEMENT_EFFICIENCY` +0,1 (≈ 1,5-fache Endgeschwindigkeit)
- Plan: über `EntityAttributeModificationEvent` als Basiswert (kein Eingriff beim Spawn); falls der Event vorhandene Attribute nicht überschreibt, bleibt der Modifier bei `EntityJoinLevelEvent`

## 4. Mixins im Überblick (alle in `traderework/mixin`, alle schmal)

- `IronGolemMixin` (eine Datei, zwei Haken):
  - `registerGoals`: `@WrapOperation` auf `new GolemRandomStrollInVillageGoal` + `@Inject(TAIL)` für `TemptGoal`
  - Konstruktor: `@Inject(TAIL)` setzt `GolemPathNavigation`, wenn die Navigation noch Vanilla ist
- sonst **keine** weiteren Mixins; alles andere: NeoForge-Events (Attribute, Tick-Hold, Block-Events) und Unterklassen
- Prinzip aus `villager-overhaul-projektrahmen.md` („so spät und so eng wie möglich“) bleibt eingehalten: kein `HEAD`-Cancel einer ganzen Methode

## 5. Einheitlichkeit und Kompatibilität

- Alle Werte in einer Stelle und in `tweaks/iron-golem.md` + Obsidian `Tweak-Werte.md` (wie bisher)
- Namensschema: Klassen im Paket `traderework/golem` (Logik) bzw. `traderework/client/golem` (Darstellung), Mixin in `traderework/mixin`
- Vanilla-Verhalten, das **unverändert** bleibt: Zielwahl (`targetSelector`), Angriff, Dorfverteidigung, Blume, Zurück-ins-Dorf, Wasserschaden, Stufenhöhe/Springen
- Andere Mods:
  - Navigation wird nur ersetzt, wenn sie noch `GroundPathNavigation` ist
  - Goal-Umwicklung per `@WrapOperation` (MixinExtras) kettet mit anderen Mods
  - Attribute per Event (NeoForge-Standardweg)
  - Renderer: siehe offene Frage 3
- Dieses Projekt: der Kern (`villageroverhaul`) und die Villager-Mixins werden nicht berührt; der Golem hängt nur in der Extension `trade-rework`

## 6. Prüfen (eigene Tests, damit nicht alles „im Spiel ausprobiert“ werden muss)

- Server-Gametests (laufen headless wie jetzt `runGameTestServer`): Golem + 2-Block-Wand → Pfad enthält eine Kletterkante; 1-Block-Stufe → keine; Loch 2 tief → Kante aus dem Loch; Klettern → nach 80 Ticks Position oben, Pfad geht weiter; Oberseite zu → kein Pfad über die Wand
- Patrouille: Karte mit zwei Pfadpunkten → `getPosition` liefert den älteren; Nachtumschaltung
- Anlocken: Spieler mit Eisenbarren → `TemptGoal` läuft; ohne Barren nicht
- Client-Teil (Modell/Animation) bleibt Handprüfung im Spiel; Start-Test des Clients (Modell baut ohne Fehler) wie zuletzt

## 7. Umsetzungsschritte (nach deiner Freigabe)

1. Aufräumen: `IngotLureGoal`, `PatrolGoal` (alte Fassung), `GolemClimbing`-Kollisionsteil entfernen; Attribut-Events einführen (`tempt_range`, Wasser)
2. `IronGolemMixin` (Goals + Navigation) + `GolemPatrolStrollGoal`; Tempt über Vanilla-Klasse
3. `GolemNodeEvaluator` + Pathfinder-Kosten + `GolemPathNavigation` (nur Pfad, noch ohne Klettern ausführen) + Gametests dafür
4. Klettern ausführen in der Navigation (Start, Halten, Ende, Pfad weiter) + Gametests
5. Patrouillenmodus (Aufschlag für Nicht-Pfadblöcke) + Anbindung an `PatrolMap`
6. Doku (`tweaks/iron-golem.md`, Obsidian, README), Memory, Drive, Client-Start-Test

## 8. Offene Fragen (aus Sicht des Spiels)

1. **Klettern oder Umweg?** Soll der Golem lieber einen Umweg bis ca. 8 Blöcke laufen, bevor er klettert? (Ich würde ja sagen: Klettern dauert 4 s, Laufen ist Standard.)
2. **Klettern in der Patrouille**: Dürfen Patrouille und Stroll tagsüber ebenfalls klettern, oder nur bei Angriff/Anlocken/Patrouille? (Vorschlag: überall, da der Pfad es sowieso plant.)
3. **Renderer**: Aktuell ersetze ich den Golem-Renderer komplett (Risiko mit Mods, die den Golem auch umrüsten). Alternative: Renderer bleibt, nur das Modell wird per NeoForge-Event vertauscht (kompatibler, braucht einen kleinen Accessor). Welcher Weg?
4. **Halbblöcke**: Soll das Klettern nur an vollen 2-Block-Wänden gehen (Vorschlag) oder auch an 2-Block-Wänden mit Halbblock-Kante (Platte/Treppe oben)?
5. Dein Satz brach ab bei „Einheitlichkeit und Kompatibilität mit,“ – **womit** genau soll es kompatibel/einheitlich sein (andere Mods, dem Kern der Mod, den Villager-Mixins)?

## Entscheidungen 2026-10-09 und Stand der Umsetzung

- Entschieden: lieber Umweg bis ca. 8 Blöcke als Klettern; klettern überall (auch am Tag, beim Anlocken, im Wasser); Renderer bleibt Vanilla (kompatibel mit anderen Mods); nur volle Blöcke; eigene Mod `smart-golem` (Smart Iron Golem); der Kern bleibt „Villager Overhaul“, später ggf. „Smart Villager“ (Smaragd-Anlocken, Handelsblock)
- Umgesetzt: Modul `smart-golem`, Mixin (Goals + Navigation), `GolemPathNavigation`/`GolemNodeEvaluator`/`ClimbGeometry`/`GolemClimbs`, `GolemPatrolStrollGoal`, Patrouillenkarte, Vanilla-`TemptGoal`, Attribute per Event, Render-Ebene + Render-State-Modifier (Vanilla-Golem wird für die Dauer unsichtbar), 16 Gametests (alle grün)
- Gefunden und behoben: Kletterknoten ohne Vanillas `isNeighborValid` → Endlosschleife in `PathFinder.reconstructPath`; Knoten eines 1,4 breiten Mobs sind Ecken der 2×2-Fläche (Geometrie in `ClimbGeometry`); Kosten über die Kantenlänge verbrauchen die maximale Pfadlänge → Kosten am Zielknoten
- Offen: Client-Test der Animation im Spiel (Drehrichtungen, Endposition 0,63 Blöcke), Wassergeschwindigkeit messen
