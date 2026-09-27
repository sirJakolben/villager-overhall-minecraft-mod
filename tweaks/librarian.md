# Tweaks – Librarian

Zwilling der Librarian-Werte aus Obsidian (`Librarian.md` Wertetabellen + Librarian-Teil von `Tweak-Werte.md`), kompakt. Bei Abweichung gilt Obsidian.
Base = Rang 0 der Gruppe, Max = Max-Rang, dazwischen linear. Fehlt ein Wert → 1. Stand: 2026-09-26.

## Basic Trades — `data/vo_trade_rework/villageroverhaul/trade/librarian_*.json`
| Rang | Trade | Preis (Smaragde) | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 0 | Laterne | 1 → 1 | – | 1 → 1 | 1 → 1 |
| 1 | Namensschild | 1 → 1 | – | 1 → 1 | 1 → 1 |
| 2 | Bücherstapel | 1 → 1 | 1 Buch | 1 → 1 | 1 → 1 |
| 3 | Papierstapel | 1 → 1 | 1 Papier | 1 → 1 | 1 → 1 |
| 4 | Schreibstation | 1 → 1 | – | 1 → 1 | 1 → 1 |
| 4 | Verbesserungsstation | 1 → 1 | – | 1 → 1 | 1 → 1 |
| 5 | XP abfüllen (+55 Spieler-XP) | 1 → 1 | 1 Glasflasche | 1 → 1 | 1 → 1 |
| 6 | Villager-XP-Flasche | 1 → 1 | 1 XP-Flasche | 1 → 1 | 1 → 1 |

## Master Trades
| Rang | Trade | Preis | 2. Slot | Output | Stock |
|---|---|---|---|---|---|
| 1 | Waffenbuch | 1 → 1 | 1 Buch | 1 → 1 | 1 → 1 |
| 2 | Werkzeugbuch | 1 → 1 | 1 Buch | 1 → 1 | 1 → 1 |
| 3 | Rüstungsbuch | 1 → 1 | 1 Buch | 1 → 1 | 1 → 1 |
| 4 | Fernkampfbuch | 1 → 1 | 1 Buch | 1 → 1 | 1 → 1 |

## Quests — `.../quest/librarian_*.json` (Abgabe → Smaragde)
| Pool | Quest | Abgabe | Smaragde |
|---|---|---|---|
| Dauer | Papier | 1 → 1 | 1 → 1 |
| Easy | Buch · Tintenbeutel · Leuchttintenbeutel · Feder | 1 → 1 | 1 → 1 |
| Easy | Schild · Laterne · gemeißelter Block (je 1 Quest, Variante zufällig) | 1 → 1 | 1 → 1 |
| Easy | Lore-Schriftrolle I (Oberwelt/Höhle/Unterwasser) | 1 | 15 → 25 |
| Hard | Nether-Schild · Seelenlaterne · gemeißelter Nether-Block | 1 → 1 | 1 → 1 |
| Hard | Verzaubertes Buch mit Seelenläufer/Huschen/Windstoß | 1 → 1 | 1 → 1 |
| Hard | Nether-Schriftrolle I | 1 | 15 → 25 |
| Hard | Lore-Schriftrolle II (Herkunft zufällig) | 1 | 30 → 45 |
| Hard | Lore-Schriftrolle III (Herkunft zufällig) | 1 | 50 → 64 |

## Passive: Buch-Upgrade — `librarian/BookUpgrades.java`, `trade/RestockService.java`
| Passiv-Rang | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|---|
| Upgrades pro vollem Arbeitstag | 1 | 1 | 1 | 1 | 2 | 3 | 4 |
| Höchste Stufe (nie über Vanilla-Max) | II | III | IV | V | V | V | Vanilla-Max + 1 (nur wenn Max > 1) |

## Verbesserungsstation & Spezialbücher
| Wert | Aktuell | Code |
|---|---|---|
| Slots (je 1 Buch) | 5 | `EnchantmentStationBlockEntity.SLOTS` |
| Reichweite zum Aufwerten | 2 Blöcke | `BookUpgradeWork.UPGRADE_REACH` |
| Redstone-Puls | 2 Ticks | `EnchantmentStationBlock.PULSE_TICKS` |
| Besitzer gilt als weg nach | 250 Ticks | `EnchantmentStationBlockEntity.OWNER_TIMEOUT_TICKS` |
| Verzauberbarkeit Spezialbücher | 1 (wie Buch) | `LibrarianItems` |
- Kategorien: `data/vo_trade_rework/tags/enchantment/{weapon,ranged,tool,armor}_book.json` (Haltbarkeit in allen, Dreizack = Waffe)

## Erfahrungsflaschen — `librarian/ExperienceBottles.java`, `librarian/VillagerExperienceOrb.java`
| Wert | Aktuell |
|---|---|
| Erfahrungsfläschchen / Abfüllen | 55 Spieler-XP |
| Villager-XP-Flasche | 2500 Villager-XP |
| Kugel: Radius / Lebensdauer | 8 Blöcke / 6000 Ticks |

## Lore-Schriftrollen — `data/vo_trade_rework/loot_modifiers/*.json` (`chance`)
| Quelle | Chance | Schriftrolle |
|---|---|---|
| Strukturtruhen | 30 % | je nach Ort I–III |
| Bastion-Schatztruhe | 100 % | Nether III |
| Vergrabener Schatz | 100 % | Unterwasser II |
| Großer Wächter | 100 % | Unterwasser III |
| Dorftruhen | 15 % | Oberwelt I |
| Antiker Schrott | 15 % | Nether III |
| Hexe | 5 % | Oberwelt I |
| Angeln | 2 % | Unterwasser I |
| Piglin-Tausch | 2 % | Nether I |
| Fragmente pro Schriftrolle | 3 | `LoreScrollItem.FRAGMENTS_PER_SCROLL` |
