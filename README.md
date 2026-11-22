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
- *: open the duel menu (Select Phase / View Deck)
- #: back / close the current view

The first player starts in Draw Phase without drawing an extra card, because the opening hand has already been dealt. The Phase menu follows the legal turn order. After Draw Phase, the player may enter Standby Phase or Main Phase 1. From Main Phase 1 on turn 2 onward, the player may enter Battle Phase or go directly to End Phase. On the first turn, Battle Phase is unavailable. Main Phase 2 is available only after Battle Phase. Battle Phase is unavailable on the player's first turn. From Battle Phase, the player may enter Main Phase 2 or End Phase. Main Phase 2 leads to End Phase. Press 5 during End Phase to finish the turn; there is no separate End Turn shortcut key.


## Build
Open with Android Studio using JDK 8 and Android SDK Platform 28, or run:

```bash
gradle assembleDebug
```

The project uses Android Gradle Plugin 3.5.4 and Gradle 5.6.4. Minimum SDK is 19.

## Scope
This is a clean-room prototype. It does not contain original game assets or proprietary card data. The current code is an early playable foundation, not a complete rules implementation. Ritual, Fusion, Synchro, Xyz, Pendulum summoning, full card effects, Spell/Trap play, and a larger tested card catalog remain future milestones. Link mechanics are intentionally deferred.
