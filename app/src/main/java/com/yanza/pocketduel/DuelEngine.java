package com.yanza.pocketduel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Platform-independent duel rules for the first prototype. */
public final class DuelEngine {
    public enum Phase { DRAW, STANDBY, MAIN1, BATTLE, MAIN2, END, FINISHED }

    private final List<CardDefinition> playerDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerExtraDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuExtraDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerHand = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuHand = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerGraveyard = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerBanished = new ArrayList<CardDefinition>();
    private final CardDefinition[] playerField = new CardDefinition[5];
    private final CardDefinition[] cpuField = new CardDefinition[5];
    private final CardDefinition[] playerSpellTrap = new CardDefinition[5];
    private CardDefinition playerFieldSpell;
    private final boolean[] playerSpellTrapFaceDown = new boolean[5];
    private final int[] playerSpellTrapSetTurn = {-1, -1, -1, -1, -1};
    private CardDefinition playerPendulumLeft;
    private CardDefinition playerPendulumRight;
    private boolean playerPendulumSummonedThisTurn;
    private final boolean[] playerAttacked = new boolean[5];
    private final boolean[] cpuAttacked = new boolean[5];
    private final boolean[] cpuMonsterFaceDown = new boolean[5];
    private final boolean[] playerMonsterFaceDown = new boolean[5];

    private int playerLP = 8000;
    private int cpuLP = 8000;
    private int turn = 1;
    private int playerNormalSummons;
    private Phase phase = Phase.DRAW;
    private String message = "Duel started";

    /** Default prototype constructor retained for rule tests and fallback callers. */
    public DuelEngine() {
        this(null, null);
    }

    /** Starts a duel with the player's saved Main and Extra Deck lists. */
    public DuelEngine(List<String> mainDeckIds, List<String> extraDeckIds) {
        if (mainDeckIds == null) {
            for (int copy = 0; copy < 3; copy++) {
                for (CardDefinition card : CardCatalog.all()) {
                    if (card.kind != CardDefinition.Kind.MONSTER || !isExtraDeckMonster(card)) {
                        playerDeck.add(card);
                    }
                }
            }
        } else {
            for (String id : mainDeckIds) {
                CardDefinition card = CardCatalog.find(id);
                if (card != null && (card.kind != CardDefinition.Kind.MONSTER || !isExtraDeckMonster(card))) {
                    playerDeck.add(card);
                }
            }
        }
        if (extraDeckIds == null) {
            for (int copy = 0; copy < 3; copy++) {
                for (CardDefinition card : CardCatalog.all()) {
                    if (card.kind == CardDefinition.Kind.MONSTER && isExtraDeckMonster(card)) {
                        playerExtraDeck.add(card);
                    }
                }
            }
        } else {
            for (String id : extraDeckIds) {
                CardDefinition card = CardCatalog.find(id);
                if (card != null && card.kind == CardDefinition.Kind.MONSTER && isExtraDeckMonster(card)) {
                    playerExtraDeck.add(card);
                }
            }
        }
        // The CPU keeps its own temporary prototype deck until opponent deck profiles are added.
        for (int copy = 0; copy < 3; copy++) {
            for (CardDefinition card : CardCatalog.all()) {
                if (card.kind == CardDefinition.Kind.MONSTER) {
                    if (isExtraDeckMonster(card)) cpuExtraDeck.add(card);
                    else cpuDeck.add(card);
                }
            }
        }
        Collections.shuffle(playerDeck);
        Collections.shuffle(cpuDeck);
        draw(playerDeck, playerHand, 5);
        draw(cpuDeck, cpuHand, 5);
        phase = Phase.DRAW;
        message = "Your Draw Phase (first turn draw skipped)";
    }

    public List<CardDefinition> getPlayerHand() { return Collections.unmodifiableList(playerHand); }
    public List<CardDefinition> getPlayerDeck() { return Collections.unmodifiableList(playerDeck); }
    public List<CardDefinition> getPlayerExtraDeck() { return Collections.unmodifiableList(playerExtraDeck); }
    public List<CardDefinition> getPlayerGraveyard() { return Collections.unmodifiableList(playerGraveyard); }
    public List<CardDefinition> getPlayerBanished() { return Collections.unmodifiableList(playerBanished); }
    public boolean banishFromGraveyard(int index) {
        if (index < 0 || index >= playerGraveyard.size()) return reject("Select a card in the Graveyard");
        playerBanished.add(playerGraveyard.remove(index));
        message = "Card moved to Banished Zone";
        return true;
    }
    public CardDefinition[] getPlayerField() { return playerField.clone(); }
    public CardDefinition[] getPlayerSpellTrap() { return playerSpellTrap.clone(); }
    public CardDefinition getPlayerFieldSpell() { return playerFieldSpell; }
    public boolean isPlayerSpellTrapFaceDown(int slot) { return slot >= 0 && slot < 5 && playerSpellTrap[slot] != null && playerSpellTrapFaceDown[slot]; }
    public boolean activateSpellFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Spell activation requires Main Phase 1 or 2");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a Spell in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.SPELL) return reject("Select a Spell Card");
        int slot = firstEmpty(playerSpellTrap);
        if (card.spellTrapType != CardDefinition.SpellTrapType.FIELD_SPELL && slot < 0) {
            return reject("Spell/Trap Zone is full");
        }
        playerHand.remove(handIndex);
        if (card.spellTrapType == CardDefinition.SpellTrapType.FIELD_SPELL) {
            if (playerFieldSpell != null) playerGraveyard.add(playerFieldSpell);
            playerFieldSpell = card;
            message = card.name + " activated in the Field Zone";
        } else if (card.spellTrapType == CardDefinition.SpellTrapType.CONTINUOUS_SPELL
                || card.spellTrapType == CardDefinition.SpellTrapType.EQUIP_SPELL) {
            playerSpellTrap[slot] = card;
            message = card.name + " activated and remains face-up";
        } else {
            String effect = resolveSpellTrapEffect(card);
            playerGraveyard.add(card);
            message = card.name + " activated" + (effect.length() == 0 ? "" : " - " + effect);
        }
        return true;
    }
    public boolean setTrapFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Trap Set requires Main Phase 1 or 2");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a Trap in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.TRAP) return reject("Only Trap Cards can be Set");
        int slot = firstEmpty(playerSpellTrap);
        if (slot < 0) return reject("Spell/Trap Zone is full");
        playerHand.remove(handIndex);
        playerSpellTrap[slot] = card;
        playerSpellTrapFaceDown[slot] = true;
        playerSpellTrapSetTurn[slot] = turn;
        message = card.name + " Set face-down; it cannot activate this turn";
        return true;
    }
    public boolean activateSetTrap(int slot) {
        if (slot < 0 || slot >= 5 || playerSpellTrap[slot] == null
                || !playerSpellTrapFaceDown[slot]) return reject("Select a face-down Trap");
        if (playerSpellTrapSetTurn[slot] >= turn) return reject("A Trap cannot activate on the turn it was Set");
        CardDefinition card = playerSpellTrap[slot];
        playerSpellTrapFaceDown[slot] = false;
        String effect = resolveSpellTrapEffect(card);
        if (card.spellTrapType == CardDefinition.SpellTrapType.CONTINUOUS_TRAP) {
            message = card.name + " activated and remains face-up";
        } else {
            playerSpellTrap[slot] = null;
            playerGraveyard.add(card);
            message = card.name + " activated" + (effect.length() == 0 ? "" : " - " + effect);
        }
        return true;
    }
    private String resolveSpellTrapEffect(CardDefinition card) {
        if ("DRAW_TWO".equals(card.effectId)) {
            int before = playerHand.size(); draw(playerDeck, playerHand, 2);
            return "draw " + (playerHand.size() - before) + " card(s)";
        }
        if ("RECOVER_1000".equals(card.effectId)) {
            playerLP += 1000; return "recover 1000 LP";
        }
        if ("DESTROY_WEAKEST".equals(card.effectId)) {
            int target = weakestMonster(cpuField);
            if (target >= 0) { cpuField[target] = null; return "destroyed an opponent monster"; }
            return "no valid monster to destroy";
        }
        if ("NEGATE_NEXT".equals(card.effectId)) return "Counter Trap activated (chain negation pending)";
        return "";
    }
    public CardDefinition getPlayerPendulumLeft() { return playerPendulumLeft; }
    public CardDefinition getPlayerPendulumRight() { return playerPendulumRight; }
    public boolean canPlacePendulum(int handIndex) {
        if (!isMainPhase() || handIndex < 0 || handIndex >= playerHand.size()) return false;
        CardDefinition card = playerHand.get(handIndex);
        return card.isPendulumMonster()
                && (playerPendulumLeft == null || playerPendulumRight == null);
    }
    public boolean placePendulumFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Pendulum placement requires a Main Phase");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a Pendulum Monster in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (!card.isPendulumMonster()) return reject("Select a Pendulum Monster");
        if (playerPendulumLeft == null) playerPendulumLeft = card;
        else if (playerPendulumRight == null) playerPendulumRight = card;
        else return reject("Both Pendulum Zones are occupied");
        playerHand.remove(handIndex);
        message = card.name + " placed in " + (playerPendulumLeft == card ? "Left" : "Right") + " Pendulum Zone (Scale " + card.pendulumScale + ")";
        return true;
    }
    public boolean canPendulumSummon() {
        if (!isMainPhase() || playerPendulumSummonedThisTurn
                || playerPendulumLeft == null || playerPendulumRight == null
                || playerPendulumLeft.pendulumScale == playerPendulumRight.pendulumScale) return false;
        int low = Math.min(playerPendulumLeft.pendulumScale, playerPendulumRight.pendulumScale);
        int high = Math.max(playerPendulumLeft.pendulumScale, playerPendulumRight.pendulumScale);
        for (CardDefinition card : playerHand)
            if (card.kind == CardDefinition.Kind.MONSTER && card.level > low && card.level < high) return true;
        return false;
    }
    public boolean pendulumSummonFromHand() {
        if (!isMainPhase()) return reject("Pendulum Summon requires a Main Phase");
        if (playerPendulumSummonedThisTurn) return reject("Pendulum Summon already used this turn");
        if (playerPendulumLeft == null || playerPendulumRight == null)
            return reject("Place Pendulum Monsters in both Pendulum Zones first");
        int low = Math.min(playerPendulumLeft.pendulumScale, playerPendulumRight.pendulumScale);
        int high = Math.max(playerPendulumLeft.pendulumScale, playerPendulumRight.pendulumScale);
        int summoned = 0;
        for (int i = 0; i < playerHand.size() && firstEmpty(playerField) >= 0;) {
            CardDefinition card = playerHand.get(i);
            if (card.kind == CardDefinition.Kind.MONSTER && card.level > low && card.level < high) {
                int slot = firstEmpty(playerField);
                playerField[slot] = card;
                playerHand.remove(i);
                summoned++;
                String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
                if (effectMessage.length() > 0) message = effectMessage;
            } else i++;
        }
        if (summoned == 0) return reject("No monster in hand has a Level between the Pendulum Scales");
        playerPendulumSummonedThisTurn = true;
        message = "Pendulum Summoned " + summoned + " monster(s) from hand";
        return true;
    }
    public CardDefinition[] getCpuField() { return cpuField.clone(); }
    /** Opponent face-down monsters expose no card identity or stats to the view. */
    public boolean isCpuMonsterFaceDown(int slot) {
        return validSlot(slot) && cpuMonsterFaceDown[slot] && cpuField[slot] != null;
    }
    /** Used by CPU set actions; setting a card face-up reveals it normally. */
    public void setCpuMonsterFaceDown(int slot, boolean faceDown) {
        if (validSlot(slot) && cpuField[slot] != null) cpuMonsterFaceDown[slot] = faceDown;
    }
    public boolean isPlayerMonsterFaceDown(int slot) { return validSlot(slot) && playerMonsterFaceDown[slot] && playerField[slot] != null; }
    public int getPlayerLP() { return playerLP; }
    public int getCpuLP() { return cpuLP; }
    public int getTurn() { return turn; }
    public Phase getPhase() { return phase; }
    public String getMessage() { return message; }

    public boolean summonFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Summoning is only allowed in Main Phase 1 or 2");
        if (playerNormalSummons >= 1) return reject("Normal Summon already used this turn");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.MONSTER) return reject("Select a Monster");
        if (card.level >= 5) return reject("Level 5+ monsters require Tribute Summon");
        int slot = firstEmpty(playerField);
        if (slot < 0) return reject("Your monster area is full");
        playerHand.remove(handIndex);
        playerField[slot] = card;
        playerNormalSummons++;
        message = card.name + " summoned to slot " + (slot + 1);
        String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
        if (effectMessage.length() > 0) message += " - " + effectMessage;
        return true;
    }

    /** Normal Set: no ATK/DEF threshold. */
    public boolean setFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Setting is only allowed in Main Phase 1 or 2");
        if (playerNormalSummons >= 1) return reject("Normal Summon/Set already used this turn");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.MONSTER) return reject("Select a Monster");
        if (card.level >= 5) return reject("Level 5+ monsters require Tribute Set");
        int slot = firstEmpty(playerField);
        if (slot < 0) return reject("Your monster area is full");
        playerHand.remove(handIndex);
        playerField[slot] = card;
        playerMonsterFaceDown[slot] = true;
        playerNormalSummons++;
        message = "Monster Set face-down in Defense Position";
        return true;
    }

    public boolean tributeSetFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Tribute Set requires Main Phase 1 or 2");
        if (playerNormalSummons >= 1) return reject("Normal Summon/Set already used this turn");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.MONSTER) return reject("Only monsters can be Set");
        int required = card.level >= 7 ? 2 : card.level >= 5 ? 1 : 0;
        if (required == 0) return reject("Use a regular Set for this monster");
        if (countMonsters(playerField) < required) return reject("Need " + required + " monster tribute(s)");
        for (int n = 0; n < required; n++) {
            int tributeSlot = weakestMonster(playerField);
            playerGraveyard.add(playerField[tributeSlot]);
            playerMonsterFaceDown[tributeSlot] = false;
            playerField[tributeSlot] = null;
        }
        int slot = firstEmpty(playerField);
        playerHand.remove(handIndex);
        playerField[slot] = card;
        playerMonsterFaceDown[slot] = true;
        playerNormalSummons++;
        message = "Tribute Set face-down in Defense Position (" + required + " tribute)";
        return true;
    }

    /** Tribute Summon: Level 5-6 needs one tribute; Level 7+ needs two. */
    public boolean tributeSummonFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Tribute Summon requires Main Phase 1 or 2");
        if (playerNormalSummons >= 1) return reject("Normal Summon already used this turn");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.MONSTER) return reject("Only monsters can be Tribute Summoned");
        int required = card.level >= 7 ? 2 : card.level >= 5 ? 1 : 0;
        if (required == 0) return reject("This monster does not need a Tribute");
        if (countMonsters(playerField) < required) return reject("Need " + required + " monster tribute(s)");
        for (int n = 0; n < required; n++) {
            int tributeSlot = weakestMonster(playerField);
            if (tributeSlot >= 0) playerGraveyard.add(playerField[tributeSlot]);
            if (tributeSlot >= 0) playerField[tributeSlot] = null;
        }
        int slot = firstEmpty(playerField);
        if (slot < 0) return reject("No monster area available after Tribute");
        playerHand.remove(handIndex);
        playerField[slot] = card;
        playerNormalSummons++;
        message = card.name + " Tribute Summoned (" + required + " tribute)";
        String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
        if (effectMessage.length() > 0) message += " - " + effectMessage;
        return true;
    }

    public boolean advanceToPhase(Phase next) {
        if (phase == Phase.DRAW) {
            if (next != Phase.STANDBY && next != Phase.MAIN1)
                return reject("Choose Standby Phase or Main Phase 1");
        } else if (phase == Phase.STANDBY) {
            if (next != Phase.MAIN1) return reject("Choose Main Phase 1");
        } else if (phase == Phase.MAIN1) {
            if (next != Phase.BATTLE && next != Phase.END)
                return reject("Choose Battle Phase or End Phase");
            if (turn == 1 && next == Phase.BATTLE)
                return reject("Battle Phase is unavailable on your first turn");
        } else if (phase == Phase.BATTLE) {
            if (next != Phase.MAIN2 && next != Phase.END)
                return reject("Choose Main Phase 2 or End Phase");
        } else if (phase == Phase.MAIN2) {
            if (next != Phase.END) return reject("Choose End Phase");
        } else {
            return reject("Phase selection is not available now");
        }
        phase = next;
        message = "Your " + phaseLabel(next);
        if (next == Phase.BATTLE) message += ": choose a monster, then press 5";
        if (next == Phase.END) message += ": press 5 to finish your turn";
        return true;
    }

    private boolean isExtraDeckMonster(CardDefinition card) {
        return card.summonMethod == CardDefinition.SummonMethod.FUSION
                || card.summonMethod == CardDefinition.SummonMethod.SYNCHRO
                || card.summonMethod == CardDefinition.SummonMethod.XYZ
                || card.summonMethod == CardDefinition.SummonMethod.LINK;
    }

    private boolean isMainPhase() {
        return phase == Phase.MAIN1 || phase == Phase.MAIN2;
    }

    public static String phaseLabel(Phase value) {
        switch (value) {
            case DRAW: return "Draw Phase";
            case STANDBY: return "Standby Phase";
            case MAIN1: return "Main Phase 1";
            case BATTLE: return "Battle Phase";
            case MAIN2: return "Main Phase 2";
            case END: return "End Phase";
            default: return "Duel Finished";
        }
    }

    public boolean playerAttack(int attackerSlot, int defenderSlot) {
        if (phase != Phase.BATTLE) return reject("Enter Battle Phase first");
        if (!validSlot(attackerSlot) || playerField[attackerSlot] == null)
            return reject("Choose your monster first");
        if (playerMonsterFaceDown[attackerSlot]) return reject("A face-down monster cannot attack");
        if (playerAttacked[attackerSlot]) return reject("That monster already attacked");
        if (hasMonsters(cpuField)) {
            if (!validSlot(defenderSlot) || cpuField[defenderSlot] == null)
                return reject("Choose an opposing monster");
            CardDefinition attacker = playerField[attackerSlot];
            CardDefinition defender = cpuField[defenderSlot];
            boolean wasFaceDown = cpuMonsterFaceDown[defenderSlot];
            cpuMonsterFaceDown[defenderSlot] = false;
            int defenderValue = wasFaceDown ? defender.defense : defender.attack;
            if (wasFaceDown) {
                if (attacker.attack > defenderValue) {
                    playerGraveyard.add(defender);
                    cpuField[defenderSlot] = null;
                    message = "Face-down monster revealed and destroyed";
                } else if (attacker.attack < defenderValue) {
                    playerLP = Math.max(0, playerLP - (defenderValue - attacker.attack));
                    message = "Face-down monster revealed; attacker survives and you take battle damage";
                } else {
                    message = "Face-down monster revealed; both monsters survive";
                }
            } else if (attacker.attack > defenderValue) {
                cpuLP = Math.max(0, cpuLP - (attacker.attack - defenderValue));
                playerGraveyard.add(defender);
                cpuField[defenderSlot] = null;
                message = attacker.name + " wins the battle";
            } else if (attacker.attack < defenderValue) {
                playerLP = Math.max(0, playerLP - (defenderValue - attacker.attack));
                playerGraveyard.add(attacker);
                playerField[attackerSlot] = null;
                message = attacker.name + " loses the battle";
            } else {
                playerGraveyard.add(attacker);
                playerField[attackerSlot] = null;
                cpuField[defenderSlot] = null;
                message = "Both monsters are removed";
            }
        } else {
            cpuLP = Math.max(0, cpuLP - playerField[attackerSlot].attack);
            message = "Direct attack: " + playerField[attackerSlot].attack;
        }
        playerAttacked[attackerSlot] = true;
        checkWinner();
        return true;
    }

    public void endPlayerTurn() {
        if (phase == Phase.FINISHED) return;
        if (phase != Phase.END) {
            reject("Select End Phase first");
            return;
        }
        runCpuTurn();
        if (phase == Phase.FINISHED) return;
        turn++;
        playerNormalSummons = 0;
        playerPendulumSummonedThisTurn = false;
        clearAttacks(playerAttacked);
        draw(playerDeck, playerHand, 1);
        phase = Phase.DRAW;
        message = "Your turn " + turn + " - Draw Phase";
    }

    private void runCpuTurn() {
        draw(cpuDeck, cpuHand, 1);
        if (firstEmpty(cpuField) >= 0 && !cpuHand.isEmpty()) {
            int bestIndex = -1;
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < cpuHand.size(); i++) {
                CardDefinition candidate = cpuHand.get(i);
                if (candidate.level >= 5) continue;
                int score = scoreCpuSummon(candidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }
            if (bestIndex >= 0) {
                int slot = firstEmpty(cpuField);
                CardDefinition choice = cpuHand.remove(bestIndex);
                cpuField[slot] = choice;
                // Prefer Set when the card is defensive and the player already controls a monster.
                cpuMonsterFaceDown[slot] = hasMonsters(playerField)
                        && choice.defense > choice.attack;
                message = cpuMonsterFaceDown[slot]
                        ? "Opponent Set a monster face-down"
                        : "Opponent Normal Summoned a monster";
            }
        }
        for (int i = 0; i < cpuField.length; i++) {
            CardDefinition attacker = cpuField[i];
            if (attacker == null || cpuAttacked[i] || cpuMonsterFaceDown[i]) continue;
            int target = weakestCpuBattleTarget();
            if (target < 0) {
                playerLP = Math.max(0, playerLP - attacker.attack);
            } else if (playerMonsterFaceDown[target]) {
                int defense = playerField[target].defense;
                playerMonsterFaceDown[target] = false;
                if (attacker.attack > defense) {
                    playerField[target] = null;
                    message = "Your face-down monster was revealed and destroyed";
                } else if (attacker.attack < defense) {
                    cpuLP = Math.max(0, cpuLP - (defense - attacker.attack));
                    message = "Your face-down monster survives; opponent takes battle damage";
                } else {
                    message = "Your face-down monster was revealed; both monsters survive";
                }
            } else if (attacker.attack > playerField[target].attack) {
                playerLP = Math.max(0, playerLP - (attacker.attack - playerField[target].attack));
                playerField[target] = null;
            } else if (attacker.attack == playerField[target].attack) {
                playerField[target] = null;
                cpuField[i] = null;
            } else {
                cpuField[i] = null;
                cpuMonsterFaceDown[i] = false;
            }
            cpuAttacked[i] = true;
            checkWinner();
            if (phase == Phase.FINISHED) return;
        }
        clearAttacks(cpuAttacked);
    }

    /** Scores available low-level monsters using board synergy before raw ATK. */
    private int scoreCpuSummon(CardDefinition candidate) {
        int score = candidate.attack + candidate.defense / 4;
        if (candidate.effectId.length() > 0) score += 350;
        for (CardDefinition onField : cpuField) {
            if (onField == null) continue;
            if (candidate.monsterType != null && candidate.monsterType == onField.monsterType) score += 500;
            if (candidate.attribute != null && candidate.attribute == onField.attribute) score += 200;
        }
        return score;
    }

    /** Face-down monsters are evaluated by DEF; face-up monsters by ATK. */
    private int weakestCpuBattleTarget() {
        int slot = -1;
        int weakestValue = Integer.MAX_VALUE;
        for (int i = 0; i < playerField.length; i++) {
            CardDefinition target = playerField[i];
            if (target == null) continue;
            int value = playerMonsterFaceDown[i] ? target.defense : target.attack;
            if (value < weakestValue) {
                weakestValue = value;
                slot = i;
            }
        }
        return slot;
    }

    boolean drawOneForEffect() {
        int before = playerHand.size();
        draw(playerDeck, playerHand, 1);
        return playerHand.size() > before;
    }

    private void draw(List<CardDefinition> deck, List<CardDefinition> hand, int count) {
        for (int i = 0; i < count && !deck.isEmpty() && hand.size() < 7; i++) hand.add(deck.remove(0));
    }
    private int firstEmpty(CardDefinition[] field) {
        for (int i = 0; i < field.length; i++) if (field[i] == null) return i;
        return -1;
    }
    private int weakestMonster(CardDefinition[] field) {
        int slot = -1;
        for (int i = 0; i < field.length; i++)
            if (field[i] != null && (slot < 0 || field[i].attack < field[slot].attack)) slot = i;
        return slot;
    }
    private int countMonsters(CardDefinition[] field) {
        int count = 0;
        for (CardDefinition c : field) if (c != null) count++;
        return count;
    }
    private boolean hasMonsters(CardDefinition[] field) {
        for (CardDefinition card : field) if (card != null) return true;
        return false;
    }
    private boolean validSlot(int slot) { return slot >= 0 && slot < 5; }
    private void clearAttacks(boolean[] values) {
        for (int i = 0; i < values.length; i++) values[i] = false;
    }
    private boolean reject(String reason) { message = reason; return false; }
    private void checkWinner() {
        if (playerLP <= 0 || cpuLP <= 0) {
            phase = Phase.FINISHED;
            message = playerLP <= 0 ? "CPU wins the duel" : "You win the duel";
        }
    }
}
