# Eisengolem – Nachtpatrouille (trade-rework)

- Tag (Uhrzeit 1000–11000): Vanilla-Verhalten (Villager besuchen, Blume schenken, wenig laufen)
- Früh, abends, nachts: `PatrolGoal` – ständig in Bewegung, läuft das Dorf über Pfadblöcke ab
- Patrouillenkarte (`PatrolMap`, ab 2026-10-09): pro Chunk ein Patrouillenpunkt = Pfadblock (Dirt Path zuerst, sonst Ersatz: Gravel nicht unter Wasser, Cobblestone, Mud, Packed Mud, Mud Bricks, Coarse Dirt) nahe der Chunk-Mitte
- Dorf = Chunks im Umkreis von 4 Chunks um die nächste Glocke (ohne Glocke: um den Golem); selbstgebaute Dörfer zählen genauso
- Die Karte füllt sich beim Patrouillieren nach und nach (2 geladene Chunks pro Planung, nach jedem Neustart neu, nichts gespeichert); ein vom Spieler gelegter Pfadblock in einem Chunk ohne Punkt wird sofort ergänzt (Platzieren und Schaufel)
- Ziel = der Punkt, der am längsten nicht besucht wurde (16–64 Blöcke entfernt) → gemeinsam decken die Golems das ganze Dorf ab
- Weg: so viel Pfad wie möglich (Flood-Fill entlang erreichbarer Pfadblöcke bis zum, der dem Ziel am nächsten ist, Wegpunkte alle 5 Blöcke), Rest normales Pathfinding
- Punkt weg (Block entfernt) → verworfen, Chunk neu gescannt; Punkt nicht erreichbar (Dach, Insel) → 5 Min Pause, dann neuer Scan
- Kein Punkt im Dorf: zufälliger Punkt im Dorf
- Mob in Sicht: Vanilla-Angriff hat Vorrang (Priorität 1–2), danach läuft dieselbe Route weiter
- Priorität 3 (zwischen Angriff und Vanilla-Herumlaufen); Vanilla „zurück ins Dorf“ (2) bleibt

| Wert | Standard | Einheit | Wo |
|---|---|---|---|
| Tag von / bis | 1000 / 11000 | Ticks | `PatrolGoal.DAY_START/DAY_END` |
| Tempo | 0.7 | Faktor | `PatrolGoal.SPEED` |
| Dorf-Umkreis | 4 | Chunks um die Glocke | `PatrolMap.VILLAGE_CHUNKS` |
| Scans pro Planung | 2 | Chunks | `PatrolMap.SCAN_PER_PLAN` |
| Entfernung zum nächsten Punkt | 16–64 | Blöcke | `PatrolGoal.MIN_DISTANCE` / `MAX_TRAVEL` |
| Glocken-Suche | 64 | Blöcke | `PatrolMap.BELL_REACH` |
| Pause für unerreichbaren Punkt | 6000 | Ticks (5 min) | `PatrolMap.UNREACHABLE_RETRY_TICKS` |
| Flood-Fill-Limit / -Radius | 800 / 40 | Blöcke | `MAX_NODES` / `FLOOD_RADIUS` |
| Wegpunkt-Abstand | 5 | Pfadblöcke | `WAYPOINT_STEP` |
| Neu suchen höchstens alle | 10 | Ticks | `REPATH_TICKS` |
| Stecken / Route aufgeben | 100 / 1200 | Ticks | `STUCK_TICKS` / `ROUTE_TIMEOUT_TICKS` |
