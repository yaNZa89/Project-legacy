package com.yanza.pocketduel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Platform-independent duel rules for the first prototype. */
public final class DuelEngine {
    public enum Phase { DRAW, STANDBY, MAIN1, BATTLE, MAIN2, END, FINISHED }

    private final List<CardDefinition> playerDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerHand = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuHand = new ArrayList<CardDefinition>();
    private final CardDefinition[] playerField = new CardDefinition[5];
    private final CardDefinition[] cpuField = new CardDefinition[5];
    private final boolean[] playerAttacked = new boolean[5];
    private final boolean[] cpuAttacked = new boolean[5];

    private int playerLP = 8000;
    private int cpuLP = 8000;
    private int turn = 1;
    private int playerNormalSummons;
    private Phase phase = Phase.DRAW;
    private String message = "Duel started";

    public DuelEngine() {
        for (int copy = 0; copy < 3; copy++) {
            for (CardDefinition card : CardCatalog.all()) {
                if (card.kind == CardDefinition.Kind.MONSTER) {
                    playerDeck.add(card);
                    cpuDeck.add(card);
                }
            }
        }
        Collections.reverse(cpuDeck);
        draw(playerDeck, playerHand, 5);
        draw(cpuDeck, cpuHand, 5);
        phase = Phase.MAIN1;
        message = "Your Main Phase 1";
    }

    public List<CardDefinition> getPlayerHand() { return Collections.unmodifiableList(playerHand); }
    public List<CardDefinition> getPlayerDeck() { return Collections.unmodifiableList(playerDeck); }
    public CardDefinition[] getPlayerField() { return playerField.clone(); }
    public CardDefinition[] getCpuField() { return cpuField.clone(); }
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
        if (phase == Phase.MAIN1) {
            if (next != Phase.BATTLE && next != Phase.MAIN2 && next != Phase.END)
                return reject("Choose Battle, Main Phase 2, or End Phase");
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
        if (playerAttacked[attackerSlot]) return reject("That monster already attacked");
        if (hasMonsters(cpuField)) {
            if (!validSlot(defenderSlot) || cpuField[defenderSlot] == null)
                return reject("Choose an opposing monster");
            CardDefinition attacker = playerField[attackerSlot];
            CardDefinition defender = cpuField[defenderSlot];
            if (attacker.attack > defender.attack) {
                cpuLP = Math.max(0, cpuLP - (attacker.attack - defender.attack));
                cpuField[defenderSlot] = null;
                message = attacker.name + " wins the battle";
            } else if (attacker.attack < defender.attack) {
                playerLP = Math.max(0, playerLP - (defender.attack - attacker.attack));
                playerField[attackerSlot] = null;
                message = attacker.name + " loses the battle";
            } else {
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
        clearAttacks(playerAttacked);
        draw(playerDeck, playerHand, 1);
        phase = Phase.MAIN1;
        message = "Your turn " + turn + " - Main Phase 1";
    }

    private void runCpuTurn() {
        draw(cpuDeck, cpuHand, 1);
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
            if (strongestIndex >= 0) cpuField[firstEmpty(cpuField)] = cpuHand.remove(strongestIndex);
        }
        for (int i = 0; i < cpuField.length; i++) {
            CardDefinition attacker = cpuField[i];
            if (attacker == null || cpuAttacked[i]) continue;
            int target = weakestMonster(playerField);
            if (target < 0) {
                playerLP = Math.max(0, playerLP - attacker.attack);
            } else if (attacker.attack > playerField[target].attack) {
                playerLP = Math.max(0, playerLP - (attacker.attack - playerField[target].attack));
                playerField[target] = null;
            } else if (attacker.attack == playerField[target].attack) {
                playerField[target] = null;
                cpuField[i] = null;
            } else {
                cpuField[i] = null;
            }
            cpuAttacked[i] = true;
            checkWinner();
            if (phase == Phase.FINISHED) return;
        }
        clearAttacks(cpuAttacked);
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
