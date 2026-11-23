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
    private final List<CardDefinition> cpuGraveyard = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerBanished = new ArrayList<CardDefinition>();
    private final CardDefinition[] playerField = new CardDefinition[5];
    private final CardDefinition[] playerSpellTrap = new CardDefinition[5];
    private final CardDefinition[] cpuField = new CardDefinition[5];
    private final boolean[] playerAttacked = new boolean[5];
    private final boolean[] cpuAttacked = new boolean[5];
    private final boolean[] cpuMonsterFaceDown = new boolean[5];
    private final boolean[] playerMonsterFaceDown = new boolean[5];
    /** Trap cards cannot be activated during the same turn they were Set. */
    private final boolean[] playerTrapSetThisTurn = new boolean[5];

    private int playerLP = 8000;
    private int cpuLP = 8000;
    private int turn = 1;
    private int playerNormalSummons;
    private Phase phase = Phase.DRAW;
    private String message = "Duel started";

    public DuelEngine() {
        this(null, null);
    }

    /** Starts the player with a supplied deck while the CPU uses its prototype deck. */
    public DuelEngine(List<CardDefinition> customMainDeck, List<CardDefinition> customExtraDeck) {
        if (customMainDeck == null || customMainDeck.isEmpty()) {
            for (int copy = 0; copy < 3; copy++) {
                for (CardDefinition card : CardCatalog.all()) {
                    if (card.kind == CardDefinition.Kind.MONSTER && !isExtraDeckMonster(card)) {
                        playerDeck.add(card);
                    }
                }
            }
        } else {
            playerDeck.addAll(customMainDeck);
        }
        if (customExtraDeck == null) {
            for (int copy = 0; copy < 3; copy++) {
                for (CardDefinition card : CardCatalog.all()) {
                    if (card.kind == CardDefinition.Kind.MONSTER && isExtraDeckMonster(card)) {
                        playerExtraDeck.add(card);
                    }
                }
            }
        } else {
            playerExtraDeck.addAll(customExtraDeck);
        }
        for (int copy = 0; copy < 3; copy++) {
            for (CardDefinition card : CardCatalog.all()) {
                if (card.kind == CardDefinition.Kind.MONSTER) {
                    if (isExtraDeckMonster(card)) cpuExtraDeck.add(card);
                    else cpuDeck.add(card);
                }
            }
        }
        Collections.reverse(cpuDeck);
        draw(playerDeck, playerHand, 5);
        draw(cpuDeck, cpuHand, 5);
        phase = Phase.DRAW;
        message = "Your Draw Phase (first turn draw skipped)";
    }

    public List<CardDefinition> getPlayerHand() { return Collections.unmodifiableList(playerHand); }
    public List<CardDefinition> getPlayerDeck() { return Collections.unmodifiableList(playerDeck); }
    public List<CardDefinition> getPlayerExtraDeck() { return Collections.unmodifiableList(playerExtraDeck); }
    public List<CardDefinition> getPlayerGraveyard() { return Collections.unmodifiableList(playerGraveyard); }
    public List<CardDefinition> getCpuGraveyard() { return Collections.unmodifiableList(cpuGraveyard); }
    public List<CardDefinition> getPlayerBanished() { return Collections.unmodifiableList(playerBanished); }
    public boolean banishFromGraveyard(int index) {
        if (index < 0 || index >= playerGraveyard.size()) return reject("Select a card in the Graveyard");
        playerBanished.add(playerGraveyard.remove(index));
        message = "Card moved to Banished Zone";
        return true;
    }
    public CardDefinition[] getPlayerField() { return playerField.clone(); }
    public CardDefinition[] getPlayerSpellTrap() { return playerSpellTrap.clone(); }
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
        playerMonsterFaceDown[slot] = false;
        playerAttacked[slot] = false;
        playerNormalSummons++;
        message = card.name + " summoned to slot " + (slot + 1);
        String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
        if (phase != Phase.FINISHED && effectMessage.length() > 0) {
            message += " - " + effectMessage;
        }
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
        playerAttacked[slot] = false;
        playerNormalSummons++;
        message = "Monster Set face-down in Defense Position";
        return true;
    }

    /** Special Summon from hand only when the card's own registered condition is met. */
    public boolean canSpecialSummonFromHand(int handIndex) {
        if (!isMainPhase() || handIndex < 0 || handIndex >= playerHand.size()) return false;
        CardDefinition card = playerHand.get(handIndex);
        return card.kind == CardDefinition.Kind.MONSTER
                && CardEffectResolver.canSpecialSummonFromHand(this, card)
                && firstEmpty(playerField) >= 0;
    }

    public boolean specialSummonFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Special Summon from hand requires Main Phase 1 or 2");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.MONSTER
                || !CardEffectResolver.canSpecialSummonFromHand(this, card))
            return reject("This monster's Special Summon condition is not met");
        int slot = firstEmpty(playerField);
        if (slot < 0) return reject("Your Monster Zone is full");
        playerHand.remove(handIndex);
        playerField[slot] = card;
        playerMonsterFaceDown[slot] = false;
        playerAttacked[slot] = false;
        message = card.name + " Special Summoned from hand";
        String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
        if (phase != Phase.FINISHED && effectMessage.length() > 0) {
            message += " - " + effectMessage;
        }
        return true;
    }

    boolean hasPlayerMonsters() {
        return hasMonsters(playerField);
    }

    boolean hasCpuMonsters() {
        return hasMonsters(cpuField);
    }

    public boolean activateSpellFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Spell activation requires a Main Phase");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.SPELL) return reject("Select a Spell Card");
        // A Normal Spell resolves from the hand and goes to the Graveyard;
        // it does not occupy a Spell/Trap Zone, even when all five zones are full.
        playerHand.remove(handIndex);
        String resolution = CardEffectResolver.resolveSpell(this, card);
        playerGraveyard.add(card);
        // A draw effect may end the duel by Deck-out; preserve that terminal message.
        if (phase != Phase.FINISHED) {
            message = card.name + " activated";
            if (resolution.length() > 0) message += " - " + resolution;
        }
        return true;
    }

    public boolean setTrapFromHand(int handIndex) {
        if (!isMainPhase()) return reject("Trap Set requires a Main Phase");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        CardDefinition card = playerHand.get(handIndex);
        if (card.kind != CardDefinition.Kind.TRAP) return reject("Select a Trap Card");
        int slot = firstEmpty(playerSpellTrap);
        if (slot < 0) return reject("Your Spell/Trap Zone is full");
        playerHand.remove(handIndex);
        playerSpellTrap[slot] = card;
        playerTrapSetThisTurn[slot] = true;
        message = card.name + " Set face-down";
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
        playerAttacked[slot] = false;
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
            if (tributeSlot >= 0) {
                playerGraveyard.add(playerField[tributeSlot]);
                playerField[tributeSlot] = null;
                playerMonsterFaceDown[tributeSlot] = false;
            }
        }
        int slot = firstEmpty(playerField);
        if (slot < 0) return reject("No monster area available after Tribute");
        playerHand.remove(handIndex);
        playerField[slot] = card;
        playerMonsterFaceDown[slot] = false;
        playerAttacked[slot] = false;
        playerNormalSummons++;
        message = card.name + " Tribute Summoned (" + required + " tribute)";
        String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
        if (phase != Phase.FINISHED && effectMessage.length() > 0) {
            message += " - " + effectMessage;
        }
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
                    cpuGraveyard.add(defender);
                    cpuField[defenderSlot] = null;
                    message = "Face-down Defense Position monster revealed and destroyed";
                } else if (attacker.attack < defenderValue) {
                    playerLP = Math.max(0, playerLP - (defenderValue - attacker.attack));
                    message = "Defense Position monster revealed; attacker survives and you take battle damage";
                } else {
                    message = "Defense Position monster revealed; both survive";
                }
            } else if (attacker.attack > defenderValue) {
                cpuLP = Math.max(0, cpuLP - (attacker.attack - defenderValue));
                cpuGraveyard.add(defender);
                cpuField[defenderSlot] = null;
                message = attacker.name + " wins the battle";
            } else if (attacker.attack < defenderValue) {
                playerLP = Math.max(0, playerLP - (defenderValue - attacker.attack));
                playerGraveyard.add(attacker);
                playerField[attackerSlot] = null;
                playerMonsterFaceDown[attackerSlot] = false;
                message = attacker.name + " loses the battle";
            } else {
                playerGraveyard.add(attacker);
                playerField[attackerSlot] = null;
                playerMonsterFaceDown[attackerSlot] = false;
                cpuGraveyard.add(defender);
                cpuField[defenderSlot] = null;
                cpuMonsterFaceDown[defenderSlot] = false;
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
        // The opponent's turn starts now: Traps Set during our turn become live
        // before the CPU processes its attacks.
        clearAttacks(playerTrapSetThisTurn);
        runCpuTurn();
        if (phase == Phase.FINISHED) return;
        turn++;
        playerNormalSummons = 0;
        clearAttacks(playerAttacked);
        int drawn = draw(playerDeck, playerHand, 1);
        if (drawn < 1 && playerDeck.isEmpty()) {
            finishDeckOut(true);
            return;
        }
        phase = Phase.DRAW;
        message = "Your turn " + turn + " - Draw Phase";
    }

    private void runCpuTurn() {
        int drawn = draw(cpuDeck, cpuHand, 1);
        if (drawn < 1 && cpuDeck.isEmpty()) {
            finishDeckOut(false);
            return;
        }
        if (firstEmpty(cpuField) >= 0 && !cpuHand.isEmpty()) {
            int strongestIndex = -1;
            int strongestAtk = -1;
            for (int i = 0; i < cpuHand.size(); i++) {
                CardDefinition c = cpuHand.get(i);
                if (c.level < 5 && c.attack > strongestAtk) {
                    strongestAtk = c.attack;
                    strongestIndex = i;
                }
            }
            if (strongestIndex >= 0) {
                int slot = firstEmpty(cpuField);
                CardDefinition choice = cpuHand.get(strongestIndex);
                cpuField[slot] = cpuHand.remove(strongestIndex);
                cpuAttacked[slot] = false;
                cpuMonsterFaceDown[slot] = false;
                // Set decisions depend on the board state, never on ATK/DEF thresholds.
                cpuMonsterFaceDown[slot] = hasMonsters(playerField);
                message = cpuMonsterFaceDown[slot] ? "Opponent Set a monster face-down" : "Opponent Normal Summoned a monster";
            }
        }
        for (int i = 0; i < cpuField.length; i++) {
            CardDefinition attacker = cpuField[i];
            if (attacker == null || cpuAttacked[i] || cpuMonsterFaceDown[i]) continue;
            int target = cpuChooseAttackTarget();
            boolean preventBattleDamage = false;
            int trapSlot = firstSetTrap("NEGATE_ATTACK", "PREVENT_DAMAGE", "DESTROY_ATTACKER");
            if (trapSlot >= 0) {
                CardDefinition trap = playerSpellTrap[trapSlot];
                String resolution = CardEffectResolver.resolveTrapOnAttack(this, trap, i);
                playerSpellTrap[trapSlot] = null;
                playerTrapSetThisTurn[trapSlot] = false;
                playerGraveyard.add(trap);
                message = trap.name + " activated";
                if (resolution.length() > 0) message += " - " + resolution;
                if ("PREVENT_DAMAGE".equals(trap.effectId)) {
                    preventBattleDamage = true;
                } else {
                    cpuAttacked[i] = true;
                    if (phase == Phase.FINISHED) return;
                    continue;
                }
            }
            if (target < 0) {
                if (!preventBattleDamage) playerLP = Math.max(0, playerLP - attacker.attack);
            } else if (playerMonsterFaceDown[target]) {
                int defense = playerField[target].defense;
                playerMonsterFaceDown[target] = false;
                if (attacker.attack > defense) {
                    playerGraveyard.add(playerField[target]);
                    playerField[target] = null;
                    playerMonsterFaceDown[target] = false;
                    message = "Your face-down monster was revealed and destroyed";
                } else {
                    if (attacker.attack < defense && !preventBattleDamage) {
                        playerLP = Math.max(0, playerLP - (defense - attacker.attack));
                    }
                    message = "Your face-down monster was revealed and survives";
                }
            } else if (attacker.attack > playerField[target].attack) {
                playerLP = Math.max(0, playerLP - (attacker.attack - playerField[target].attack));
                playerGraveyard.add(playerField[target]);
                playerMonsterFaceDown[target] = false;
                playerField[target] = null;
            } else if (attacker.attack == playerField[target].attack) {
                playerGraveyard.add(playerField[target]);
                cpuGraveyard.add(attacker);
                playerMonsterFaceDown[target] = false;
                cpuMonsterFaceDown[i] = false;
                playerField[target] = null;
                cpuField[i] = null;
            } else {
                if (!preventBattleDamage) {
                    playerLP = Math.max(0, playerLP - (playerField[target].attack - attacker.attack));
                }
                cpuGraveyard.add(attacker);
                cpuField[i] = null;
                cpuMonsterFaceDown[i] = false;
            }
            cpuAttacked[i] = true;
            checkWinner();
            if (phase == Phase.FINISHED) return;
        }
        clearAttacks(cpuAttacked);
    }

    boolean drawOneForEffect() {
        int drawn = draw(playerDeck, playerHand, 1);
        if (drawn < 1 && playerDeck.isEmpty()) finishDeckOut(true);
        return drawn > 0;
    }

    int drawCardsForEffect(int count) {
        int drawn = draw(playerDeck, playerHand, count);
        if (drawn < count && playerDeck.isEmpty()) finishDeckOut(true);
        return drawn;
    }

    int gainLifePointsForEffect(int amount) {
        int before = playerLP;
        playerLP = Math.min(12000, playerLP + amount);
        return playerLP - before;
    }

    boolean reviveMonsterForEffect() {
        int target = firstEmpty(playerField);
        if (target < 0) return false;
        for (int i = playerGraveyard.size() - 1; i >= 0; i--) {
            CardDefinition candidate = playerGraveyard.get(i);
            if (candidate.kind == CardDefinition.Kind.MONSTER) {
                playerField[target] = candidate;
                playerMonsterFaceDown[target] = false;
                playerAttacked[target] = false;
                playerGraveyard.remove(i);
                return true;
            }
        }
        return false;
    }

    String destroyCpuAttackerForEffect(int slot) {
        if (!validSlot(slot) || cpuField[slot] == null) return "no valid attacking monster";
        CardDefinition attacker = cpuField[slot];
        cpuGraveyard.add(attacker);
        cpuField[slot] = null;
        cpuMonsterFaceDown[slot] = false;
        cpuAttacked[slot] = true;
        message = attacker.name + " destroyed by Trap effect";
        return "attacking monster destroyed";
    }

    private int draw(List<CardDefinition> deck, List<CardDefinition> hand, int count) {
        int drawn = 0;
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
            drawn++;
        }
        return drawn;
    }

    private void finishDeckOut(boolean playerDeckOut) {
        phase = Phase.FINISHED;
        message = playerDeckOut ? "You lose: your Deck is empty" : "You win: opponent's Deck is empty";
    }
    private int firstSetTrap(String... effects) {
        for (int i = 0; i < playerSpellTrap.length; i++) {
            CardDefinition card = playerSpellTrap[i];
            if (card == null || card.kind != CardDefinition.Kind.TRAP || playerTrapSetThisTurn[i]) continue;
            for (String effect : effects) if (effect.equals(card.effectId)) return i;
        }
        return -1;
    }

    private int firstEmpty(CardDefinition[] field) {
        for (int i = 0; i < field.length; i++) if (field[i] == null) return i;
        return -1;
    }
    /** CPU targets the weakest visible monster; face-down stats remain unknown. */
    private int cpuChooseAttackTarget() {
        int weakestVisible = -1;
        for (int i = 0; i < playerField.length; i++) {
            if (playerField[i] != null && !playerMonsterFaceDown[i]
                    && (weakestVisible < 0 || playerField[i].attack < playerField[weakestVisible].attack)) {
                weakestVisible = i;
            }
        }
        if (weakestVisible >= 0) return weakestVisible;
        for (int i = 0; i < playerField.length; i++) {
            if (playerField[i] != null) return i;
        }
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
