# Pocket Duel — Android API 19

An original, lightweight portrait card-duel prototype targeting Android 4.4 (API 19) and ARMv7.

## Current prototype
- Portrait-only duel screen.
- Player and CPU life points.
- Five Monster Zones per side.
- Dedicated Pendulum Zones on both sides.
- Initial scope excludes Link Monster Zones and Link Summoning.
- Small starter hand and turn/phase loop.
- Simple CPU opponent.
- Classic keypad controls with on-screen hints.

## Keypad
- 2 / 4 / 6 / 8: move selection up / left / right / down, matching the D-pad
- 5: confirm the selected action
- 7 / 9: scroll card details up / down while inspecting a card
- *: advance to the next phase (temporary mapping; can be refined later)
- #: back / close the current view

The six phases remain visible in a compact ribbon at the bottom of the duel screen. The active phase is highlighted; no phase-selection popup covers the arena. Phase progression follows Draw → Standby → Main Phase 1 → Battle → Main Phase 2 → End. Battle Phase is skipped on the first turn. Press * to advance phases; press 5 during End Phase to finish the turn. The opening hand is already dealt, so the first Draw Phase does not draw an extra card.


## Build
Open with Android Studio using JDK 8 and Android SDK Platform 28, or run:

```bash
gradle assembleDebug
```

The project uses Android Gradle Plugin 3.5.4 and Gradle 5.6.4. Minimum SDK is 19.

## Scope
This is a clean-room prototype. It does not contain original game assets or proprietary card data. The current code is an early playable foundation, not a complete rules implementation. Ritual, Fusion, Synchro, Xyz, Pendulum summoning, full card effects, Spell/Trap play, and a larger tested card catalog remain future milestones. Link mechanics are intentionally deferred.
