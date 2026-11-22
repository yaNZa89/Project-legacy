# Pocket Duel — Android API 19

An original, lightweight portrait card-duel prototype targeting Android 4.4 (API 19) and ARMv7.

## Current prototype
- Portrait-only duel screen.
- Player and CPU life points.
- Three monster slots per side.
- Small starter hand and turn/phase loop.
- Simple CPU opponent.
- Classic keypad controls with on-screen hints.

## Keypad (initial mapping)
- D-pad: move selection
- Center / Enter: confirm or summon selected card
- 5: summon selected card
- 6: attack with selected monster
- 0: end turn
- Back: return selection to hand

The mapping is intentionally centralized and may need adjustment after testing on the target keypad.

## Build
Open with Android Studio using JDK 8 and Android SDK Platform 28, or run:

```bash
gradle assembleDebug
```

The project uses Android Gradle Plugin 3.5.4 and Gradle 5.6.4. Minimum SDK is 19.

## Scope
This is a clean-room prototype. It does not contain original game assets or proprietary card data. Summon types and a data-driven card-effect engine will be added in later milestones; they are not yet implemented in this first playable foundation.
