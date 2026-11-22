# Pocket Duel roadmap

## Milestone 1 — Playable foundation
- [x] Portrait-only Android activity (API 19 minimum)
- [x] Basic duel board, LP display, hand, and CPU turn
- [x] Initial classic-keypad input mapping
- [x] Data-only card definitions and starter catalog
- [x] Generic effect metadata model

## Milestone 2 — Core rules
- [ ] Separate duel state from Android View
- [ ] Select a specific monster before attacking
- [ ] Summon limits, battle phase, and turn phases
- [ ] Draw phase and deck/hand limits
- [ ] Win/loss conditions and restart flow
- [ ] Effect queue and chain resolution

## Milestone 3 — Field layout and summon systems (Link deferred)
- [ ] Design the portrait duel field with standard Monster Zones and Spell/Trap Zones
- [ ] Include two clearly visible, dedicated Pendulum Zones (left and right) for both players
- [ ] Do not create Link Monster Zones, Extra Monster Zones, or Link-arrow placement rules in the current version
- [x] Preserve Monster Type and Attribute in card data
- [x] Add Pendulum scale metadata and original Pendulum test cards
- [ ] Complete Tribute Summon validation and selection
- [ ] Ritual Summon
- [ ] Fusion Summon
- [ ] Synchro Summon
- [ ] Xyz Summon
- [ ] Implement Pendulum Zones, scale placement, and Pendulum Summon rules
- [ ] Link Summon, Link Monster Zones, Extra Monster Zones, and Link-related card support — all deferred until the other systems are stable

## Milestone 4 — Synergistic card content
- [ ] Build coherent card packages/archetypes before maximizing raw card count
- [ ] Prioritize cards that support one another: searchers, summon enablers, materials, extenders, protection, recovery, and payoff cards
- [ ] Preserve and use Monster Type and Attribute as meaningful deck-building and effect dimensions (including Warrior, Dragon, DARK, LIGHT, and other categories)
- [ ] Add matching Spell/Trap support and counterplay for each archetype
- [ ] Add distinct CPU deck strategies that use the card synergies
- [ ] Expand the catalog in tested stages; long-term scale target is 10,000+ cards, not a requirement for the first playable release
- [ ] Keep card definitions/text separate from optional artwork and measure actual APK size against the 50 MB design ceiling

## Milestone 5 — Device controls and polish
- [ ] Configurable key map and actual device keycode test
- [ ] Sound, animation, settings, save/load
- [ ] Regression tests for rules, card effects, deck consistency, field zones, and API 19 compatibility

All current card names and sample data are original placeholders. This project is an original prototype and does not include proprietary card artwork or card text.
