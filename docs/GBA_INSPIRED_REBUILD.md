# GBA-Inspired Pocket Duel Rebuild

## Goal

Rebuild Pocket Duel around the clarity and compact flow of a classic handheld Yu-Gi-Oh! duel game, then add modern mechanics in isolated, testable stages. This is a clean-room implementation: existing commercial ROM code, game assets, and proprietary card data are not copied.

Target compatibility remains Android 4.4 (API 19), Java 8 source compatibility, and the project's existing Android Gradle Plugin 3.5.4 baseline until a separate compatibility review justifies a change.

## Repository audit (main, 2026-11-23)

- `MainActivity` hosts a custom `DuelView` and forwards keypad input.
- `DuelView` currently combines drawing, navigation, menus, deck editing, card inspection, and duel actions in one large class.
- `DuelEngine` owns duel state and core rules, but also executes the CPU turn directly.
- `CardEffectResolver` dispatches a small set of effects by effect ID. It is not yet a general chain/response engine.
- `CardCatalog` contains original prototype cards, not a complete official card database.
- The model has five player Monster Zones, five player Spell/Trap Zones, and five CPU Monster Zones. There is no CPU Spell/Trap zone array yet.
- Fusion/Synchro/Xyz/Link are represented in card metadata, but their complete summon procedures are not implemented. Pendulum metadata exists, but Pendulum Summoning is not implemented.
- No `app/src/test` test suite was found at the expected location during this audit.
- Build configuration declares minSdk 19, compileSdk 28, Java 8 compatibility, and Android Gradle Plugin 3.5.4. A Gradle wrapper properties file was not found at the expected path, so a reproducible local/CI build still needs verification.

## Design direction

Use a classic handheld-style core as the default experience:

1. A compact duel board, readable life points, a clear phase indicator, and direct keypad actions.
2. A small, explicit rules core for turn order, Normal Summon/Set, Tribute Summon/Set, battle, damage, destruction, and card movement.
3. CPU decisions isolated from the rules engine.
4. Effects represented as structured definitions with timing, activation conditions, costs, targets, and resolution.
5. Chain handling introduced before implementing effects that respond to other effects.
6. Modern summon methods added as separate modules, not mixed into the classic turn/battle foundation.

## Migration sequence

### Stage 1 — Stabilize the foundation

- Keep Android API 19 compatibility.
- Separate CPU decision-making from `DuelEngine`.
- Add JVM tests for opening hand, phase transitions, summon limits, tribute counts, battle outcomes, deck-out, and card-zone movement.
- Keep the existing custom keypad mapping and deck save/load behavior.

### Stage 2 — Classic duel loop

- Make legal phase transitions explicit and test every transition.
- Complete both players' Monster and Spell/Trap zones.
- Make CPU actions use the same validated game operations as player actions.
- Ensure every move between Hand, Deck, Field, Graveyard, Extra Deck, and Banished Zone has one authoritative implementation.

### Stage 3 — Effect and response system

- Add a Chain Manager with response windows and resolution order.
- Model activation timing, costs, targets, restrictions, and resolution separately.
- Add tests for negation, destruction, damage prevention, and effects that change zones.

### Stage 4 — Modern extensions

- Add Fusion and Ritual procedures, then Synchro and Xyz.
- Add Pendulum Zones and Pendulum Summoning.
- Add Link zones and Link Summoning only after the board model and Extra Monster Zone rules are covered by tests.

## Guardrails

- Do not replace working code wholesale before the replacement path is built and tested.
- Do not claim an APK or gameplay test succeeded without an actual build/test result.
- Do not copy a commercial GBA ROM, its executable code, art, music, or proprietary card database. Use the game as behavioral/UI reference and implement original code.
- Keep each change small, reviewable, and compatible with Android API 19.
