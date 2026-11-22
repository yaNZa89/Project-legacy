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

## Keypad (initial mapping)
- D-pad: move selection
- Center / Enter: confirm or summon selected card
- 5: confirm / summon / open selected deck card
- 6: attack with selected monster
- 7 / 9: scroll card details when the detail view is open
- 8: Tribute Summon
- 9: enter Battle Phase when not viewing card details
- 0: end turn
- *: open the deck browser
- #: back / close the current view

The mapping is context-sensitive: 7 and 9 scroll long card text while card details are open; outside that view, 9 enters Battle Phase. `*` opens the deck browser and `#` backs out.

## Build
Open with Android Studio using JDK 8 and Android SDK Platform 28, or run:

```bash
gradle assembleDebug
```

The project uses Android Gradle Plugin 3.5.4 and Gradle 5.6.4. Minimum SDK is 19.

## Scope
This is a clean-room prototype. It does not contain original game assets or proprietary card data. The current code is an early playable foundation, not a complete rules implementation. Ritual, Fusion, Synchro, Xyz, Pendulum summoning, full card effects, Spell/Trap play, and a larger tested card catalog remain future milestones. Link mechanics are intentionally deferred.
