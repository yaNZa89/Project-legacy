# Pocket Duel — Android API 19

An original, lightweight portrait card-duel prototype targeting Android 4.4 (API 19) and ARMv7.

## Current prototype
- Portrait-only duel screen.
- Player and CPU life points.
- Five Monster Zones per side.
- Dedicated Pendulum Zones on both sides (displayed; Pendulum Summoning is not implemented yet).
- Initial scope excludes Link Monster Zones and Link Summoning.
- Small starter hand and turn/phase loop.
- Simple CPU opponent.
- Basic Normal Spell activation from the hand.
- Basic Trap Set and response to CPU attacks.
- Classic keypad controls with on-screen hints.

## Keypad
- 2: leave the phase ribbon and return to the duel area
- 8: focus the visible phase ribbon
- 4 / 6: move the phase cursor left / right while the ribbon is focused
- 5: select the highlighted phase or perform the current contextual action
- 7 / 9: scroll card details up / down while inspecting a card
- #: back / close the current view

The six phases remain visible in a compact ribbon at the bottom of the duel screen. The active phase is highlighted; no phase-selection popup covers the arena. The phase ribbon is navigated directly: press 8 to focus it, 4 / 6 to move its cursor, and 5 to select a phase. Press 2 to return to the duel area. No popup or separate phase-advance key is used. Legal transitions are enforced by the duel engine; Battle Phase is unavailable on the first turn. From End Phase, move the cursor to Draw and press 5 to start the next turn. The opening hand is already dealt, so the first Draw Phase does not draw an extra card.

## Build
Open with Android Studio using JDK 8 and Android SDK Platform 28, or run:

```bash
gradle assembleDebug
```

The project uses Android Gradle Plugin 3.5.4 and Gradle 5.6.4. Minimum SDK is 19.

## Scope
This is a clean-room prototype. It does not contain original game assets or proprietary card data. Basic Spell/Trap handling is present, but a full Chain/Spell Speed system, Quick-Play timing, continuous/equip effects, Ritual, Fusion, Synchro, Xyz, Pendulum summoning, and a larger tested card catalog remain future milestones. Link mechanics are intentionally deferred.
