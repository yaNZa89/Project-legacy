package com.yanza.pocketduel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DuelView extends View {
    private final Paint p = new Paint(3);
    private final List<Card> hand = new ArrayList<Card>();
    private final Card[] playerField = new Card[3];
    private final Card[] cpuField = new Card[3];
    private int selected = 0;
    private int playerLP = 4000;
    private int cpuLP = 4000;
    private int turn = 1;
    private boolean playerTurn = true;
    private String message = "Your turn - select a card";
    private int phase = 0;

    private static class Card {
        String name;
        int atk;
        int def;
        Card(String n, int a, int d) { name = n; atk = a; def = d; }
    }

    public DuelView(Context context) {
        super(context);
        setFocusable(true);
        hand.add(new Card("Ember Scout", 1200, 800));
        hand.add(new Card("Stone Guardian", 900, 1500));
        hand.add(new Card("Sky Striker", 1500, 1000));
        hand.add(new Card("Forest Wisp", 700, 1100));
        hand.add(new Card("Iron Cub", 1000, 1000));
    }

    public boolean handleKey(int key) {
        if (!playerTurn) return true;
        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_UP) {
            selected = Math.max(0, selected - 1);
        } else if (key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            selected = Math.min(Math.max(0, hand.size() - 1), selected + 1);
        } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_5) {
            summonSelected();
        } else if (key == KeyEvent.KEYCODE_6) {
            attack();
        } else if (key == KeyEvent.KEYCODE_0) {
            endTurn();
        } else if (key == KeyEvent.KEYCODE_BACK) {
            selected = 0;
        } else {
            return false;
        }
        invalidate();
        return true;
    }

    private void summonSelected() {
        if (hand.size() == 0) { message = "Your hand is empty"; return; }
        int slot = firstEmpty(playerField);
        if (slot < 0) { message = "Your monster area is full"; return; }
        Card c = hand.remove(selected);
        playerField[slot] = c;
        selected = Math.min(selected, Math.max(0, hand.size() - 1));
        message = c.name + " summoned";
    }

    private int firstEmpty(Card[] field) {
        for (int i = 0; i < field.length; i++) if (field[i] == null) return i;
        return -1;
    }

    private void attack() {
        Card attacker = playerField[0];
        if (attacker == null) attacker = playerField[1];
        if (attacker == null) attacker = playerField[2];
        if (attacker == null) { message = "Summon a monster first"; return; }
        Card defender = cpuField[0];
        if (defender == null) defender = cpuField[1];
        if (defender == null) defender = cpuField[2];
        if (defender == null) {
            cpuLP -= attacker.atk;
            message = "Direct attack: " + attacker.atk;
        } else if (attacker.atk > defender.atk) {
            cpuLP -= attacker.atk - defender.atk;
            removeCard(cpuField, defender);
            message = "You won the battle";
        } else if (attacker.atk < defender.atk) {
            playerLP -= defender.atk - attacker.atk;
            removeCard(playerField, attacker);
            message = "Your monster lost";
        } else {
            removeCard(cpuField, defender);
            removeCard(playerField, attacker);
            message = "Both monsters were removed";
        }
        if (cpuLP < 0) cpuLP = 0;
        if (playerLP < 0) playerLP = 0;
    }

    private void removeCard(Card[] field, Card c) {
        for (int i = 0; i < field.length; i++) if (field[i] == c) field[i] = null;
    }

    private void endTurn() {
        playerTurn = false;
        message = "CPU turn";
        Card cpu = new Card("CPU Golem", 800 + (turn % 4) * 200, 900);
        int slot = firstEmpty(cpuField);
        if (slot >= 0) cpuField[slot] = cpu;
        Card attacker = cpuField[0];
        if (attacker == null) attacker = cpuField[1];
        if (attacker == null) attacker = cpuField[2];
        if (attacker != null) {
            Card target = playerField[0];
            if (target == null) target = playerField[1];
            if (target == null) target = playerField[2];
            if (target == null) playerLP = Math.max(0, playerLP - attacker.atk);
            else if (attacker.atk > target.atk) {
                playerLP = Math.max(0, playerLP - (attacker.atk - target.atk));
                removeCard(playerField, target);
            }
        }
        turn++;
        playerTurn = true;
        message = "Your turn - CPU played a card";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        c.drawColor(0xff101b2b);
        p.setColor(0xff203b50);
        c.drawRect(0, 0, w, h, p);
        p.setColor(0xffd8e7ef);
        p.setTextSize(Math.max(16, w * 0.045f));
        c.drawText("POCKET DUEL", 18, 34, p);
        p.setTextSize(Math.max(13, w * 0.035f));
        c.drawText("CPU  LP: " + cpuLP, 18, 65, p);
        c.drawText("TURN " + turn, w - 110, 65, p);
        drawField(c, cpuField, 95, w);
        p.setColor(0xffb9cbd8);
        c.drawText("DUEL FIELD", 18, h * 0.48f, p);
        drawField(c, playerField, h * 0.52f, w);
        p.setColor(0xffd8e7ef);
        c.drawText("YOU  LP: " + playerLP, 18, h * 0.78f, p);
        c.drawText("HAND", 18, h * 0.82f, p);
        float cardW = (w - 36) / 3.2f;
        float cardH = h * 0.105f;
        for (int i = 0; i < hand.size() && i < 5; i++) {
            float x = 12 + i * (w - 24 - cardW) / 4f;
            RectF r = new RectF(x, h * 0.835f, x + cardW, h * 0.835f + cardH);
            p.setColor(i == selected ? 0xffffc857 : 0xff52758b);
            c.drawRoundRect(r, 8, 8, p);
            p.setColor(0xff101b2b);
            p.setTextSize(Math.max(10, w * 0.025f));
            c.drawText(hand.get(i).name, x + 5, r.top + 20, p);
            c.drawText("ATK " + hand.get(i).atk, x + 5, r.top + 39, p);
        }
        p.setColor(0xfff3f5f7);
        p.setTextSize(Math.max(11, w * 0.029f));
        c.drawText(message, 14, h - 50, p);
        c.drawText("D-pad: select   OK/5: summon   6: attack   0: end turn", 14, h - 22, p);
    }

    private void drawField(Canvas c, Card[] field, float y, float w) {
        float gap = 8;
        float cw = (w - 36 - gap * 2) / 3;
        float ch = getHeight() * 0.105f;
        for (int i = 0; i < field.length; i++) {
            float x = 12 + i * (cw + gap);
            RectF r = new RectF(x, y, x + cw, y + ch);
            p.setColor(field[i] == null ? 0xff31495b : 0xffbd9653);
            c.drawRoundRect(r, 8, 8, p);
            if (field[i] != null) {
                p.setColor(0xff101b2b);
                p.setTextSize(Math.max(10, w * 0.027f));
                c.drawText(field[i].name, x + 5, y + 22, p);
                c.drawText("ATK " + field[i].atk, x + 5, y + 42, p);
            }
        }
    }
}
