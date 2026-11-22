package com.yanza.pocketduel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Platform-independent duel rules for the first prototype.
 * This deliberately implements a small original ruleset, not the complete TCG rules.
 */
public final class DuelEngine {
    public enum Phase { DRAW, MAIN, BATTLE, END, FINISHED }

    private final List<CardDefinition> playerDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuDeck = new ArrayList<CardDefinition>();
    private final List<CardDefinition> playerHand = new ArrayList<CardDefinition>();
    private final List<CardDefinition> cpuHand = new ArrayList<CardDefinition>();
    private final CardDefinition[] playerField = new CardDefinition[3];
    private final CardDefinition[] cpuField = new CardDefinition[3];
    private final boolean[] playerAttacked = new boolean[3];
    private final boolean[] cpuAttacked = new boolean[3];

    private int playerLP = 4000;
    private int cpuLP = 4000;
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
        // Fixed order makes the first prototype reproducible while rules are tested.
        Collections.reverse(cpuDeck);
        draw(playerDeck, playerHand, 5);
        draw(cpuDeck, cpuHand, 5);
        phase = Phase.MAIN;
        message = "Your Main Phase";
    }

    public List<CardDefinition> getPlayerHand() {
        return Collections.unmodifiableList(playerHand);
    }
    public CardDefinition[] getPlayerField() { return playerField.clone(); }
    public CardDefinition[] getCpuField() { return cpuField.clone(); }
    public int getPlayerLP() { return playerLP; }
    public int getCpuLP() { return cpuLP; }
    public int getTurn() { return turn; }
    public Phase getPhase() { return phase; }
    public String getMessage() { return message; }

    public boolean summonFromHand(int handIndex) {
        if (phase != Phase.MAIN) return reject("Summoning is only allowed in Main Phase");
        if (playerNormalSummons >= 1) return reject("Normal Summon already used this turn");
        if (handIndex < 0 || handIndex >= playerHand.size()) return reject("Select a card in your hand");
        int slot = firstEmpty(playerField);
        if (slot < 0) return reject("Your monster area is full");
        CardDefinition card = playerHand.remove(handIndex);
        playerField[slot] = card;
        playerNormalSummons++;
        message = card.name + " summoned to slot " + (slot + 1);
        String effectMessage = CardEffectResolver.resolveOnSummon(this, card);
        if (effectMessage.length() > 0) message += " - " + effectMessage;
        return true;
    }

    public void enterBattlePhase() {
        if (phase != Phase.MAIN) { reject("Cannot enter Battle Phase now"); return; }
        phase = Phase.BATTLE;
        message = "Battle Phase: choose a monster, then attack";
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
        if (phase == Phase.MAIN) phase = Phase.END;
        if (phase != Phase.BATTLE && phase != Phase.END) {
            reject("Finish the current phase first");
            return;
        }
        runCpuTurn();
        if (phase == Phase.FINISHED) return;
        turn++;
        playerNormalSummons = 0;
        clearAttacks(playerAttacked);
        draw(playerDeck, playerHand, 1);
        phase = Phase.MAIN;
        message = "Your turn " + turn + " - Main Phase";
    }

    private void runCpuTurn() {
        draw(cpuDeck, cpuHand, 1);
        if (firstEmpty(cpuField) >= 0 && !cpuHand.isEmpty()) {
            CardDefinition strongest = cpuHand.get(0);
            int strongestIndex = 0;
            for (int i = 1; i < cpuHand.size(); i++) {
                if (cpuHand.get(i).attack > strongest.attack) {
                    strongest = cpuHand.get(i);
                    strongestIndex = i;
                }
            }
            cpuField[firstEmpty(cpuField)] = cpuHand.remove(strongestIndex);
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
                // A weaker attacking monster is destroyed by the stronger defender.
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
        for (int i = 0; i < count && !deck.isEmpty() && hand.size() < 7; i++) {
            hand.add(deck.remove(0));
        }
    }

    private int firstEmpty(CardDefinition[] field) {
        for (int i = 0; i < field.length; i++) if (field[i] == null) return i;
        return -1;
    }
    private int weakestMonster(CardDefinition[] field) {
        int slot = -1;
        for (int i = 0; i < field.length; i++) {
            if (field[i] != null && (slot < 0 || field[i].attack < field[slot].attack)) slot = i;
        }
        return slot;
    }
    private boolean hasMonsters(CardDefinition[] field) {
        for (CardDefinition card : field) if (card != null) return true;
        return false;
    }
    private boolean validSlot(int slot) { return slot >= 0 && slot < 3; }
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
