package com.yanza.pocketduel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;

import java.util.List;

/** Portrait duel screen. All rule changes are delegated to DuelEngine. */
public class DuelView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DuelEngine duel = new DuelEngine();
    private int selectedHand;
    private int selectedMonster;
    private boolean choosingMonster;
    private String hint = "Select a card and press OK/5 to summon";

    public DuelView(Context context) {
        super(context);
        setFocusable(true);
    }

    public boolean handleKey(int key) {
        if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
            if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                    || key == KeyEvent.KEYCODE_5) {
                hint = "Duel finished - restart the app for a new duel";
                invalidate();
                return true;
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_UP) {
            if (duel.getPhase() == DuelEngine.Phase.BATTLE) {
                choosingMonster = true;
                selectedMonster = Math.max(0, selectedMonster - 1);
            } else {
                choosingMonster = false;
                selectedHand = Math.max(0, selectedHand - 1);
            }
        } else if (key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (duel.getPhase() == DuelEngine.Phase.BATTLE) {
                choosingMonster = true;
                selectedMonster = Math.min(2, selectedMonster + 1);
            } else {
                choosingMonster = false;
                selectedHand = Math.min(Math.max(0, duel.getPlayerHand().size() - 1), selectedHand + 1);
            }
        } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_5) {
            if (duel.getPhase() == DuelEngine.Phase.BATTLE) attackSelected();
            else {
                duel.summonFromHand(selectedHand);
                selectedHand = Math.min(selectedHand, Math.max(0, duel.getPlayerHand().size() - 1));
            }
        } else if (key == KeyEvent.KEYCODE_8) {
            duel.tributeSummonFromHand(selectedHand);
            selectedHand = Math.min(selectedHand, Math.max(0, duel.getPlayerHand().size() - 1));
        } else if (key == KeyEvent.KEYCODE_7) {
            duel.enterBattlePhase();
            choosingMonster = true;
            hint = "Choose a monster slot with D-pad, then press 6";
        } else if (key == KeyEvent.KEYCODE_6) {
            if (duel.getPhase() != DuelEngine.Phase.BATTLE) duel.enterBattlePhase();
            attackSelected();
        } else if (key == KeyEvent.KEYCODE_0) {
            duel.endPlayerTurn();
            selectedHand = 0;
            selectedMonster = 0;
            choosingMonster = false;
        } else if (key == KeyEvent.KEYCODE_BACK) {
            choosingMonster = false;
            selectedHand = 0;
        } else return false;
        hint = duel.getMessage();
        invalidate();
        return true;
    }

    private void attackSelected() {
        CardDefinition[] own = duel.getPlayerField();
        CardDefinition[] enemy = duel.getCpuField();
        if (selectedMonster < 0 || selectedMonster >= own.length || own[selectedMonster] == null) {
            hint = "Choose an occupied monster slot";
            return;
        }
        int target = -1;
        int weakest = Integer.MAX_VALUE;
        for (int i = 0; i < enemy.length; i++) {
            if (enemy[i] != null && enemy[i].attack < weakest) {
                weakest = enemy[i].attack;
                target = i;
            }
        }
        duel.playerAttack(selectedMonster, target);
        hint = duel.getMessage();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        canvas.drawColor(0xff101b2b);
        paint.setColor(0xff203b50);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(0xfff0f5f8);
        paint.setTextSize(Math.max(17, w * 0.052f));
        canvas.drawText("POCKET DUEL", 14, 30, paint);
        paint.setTextSize(Math.max(13, w * 0.036f));
        canvas.drawText("CPU  LP: " + duel.getCpuLP(), 14, 58, paint);
        canvas.drawText("TURN " + duel.getTurn(), w - 105, 58, paint);
        drawField(canvas, duel.getCpuField(), h * 0.13f, w, false);
        paint.setColor(0xffc6d5df);
        paint.setTextSize(Math.max(12, w * 0.034f));
        canvas.drawText("OPPONENT FIELD", 14, h * 0.265f, paint);
        canvas.drawText("YOUR FIELD", 14, h * 0.485f, paint);
        drawField(canvas, duel.getPlayerField(), h * 0.51f, w, true);
        paint.setColor(0xfff0f5f8);
        paint.setTextSize(Math.max(14, w * 0.039f));
        canvas.drawText("YOU  LP: " + duel.getPlayerLP(), 14, h * 0.65f, paint);
        canvas.drawText("HAND (" + duel.getPlayerHand().size() + ")", 14, h * 0.70f, paint);
        drawHand(canvas, w, h);
        paint.setColor(0xffffd477);
        paint.setTextSize(Math.max(11, w * 0.031f));
        canvas.drawText(duel.getPhase().name() + " PHASE", 14, h - 74, paint);
        paint.setColor(0xfff3f5f7);
        paint.setTextSize(Math.max(10, w * 0.028f));
        canvas.drawText(hint, 14, h - 48, paint);
        canvas.drawText("D-pad select | OK/5 summon | 8 tribute | 7 battle | 6 attack | 0 end", 14, h - 20, paint);
    }

    private void drawField(Canvas canvas, CardDefinition[] field, float y, float w, boolean playerSide) {
        float gap = 4;
        float sideW = w * 0.135f;
        float monsterStart = 7 + sideW + gap;
        float monsterW = (w - 14 - sideW * 2 - gap * 4) / 3f;
        float ch = getHeight() * 0.105f;

        // Dedicated Pendulum Zones are always visible on both sides of each field.
        drawPendulumZone(canvas, 7, y, sideW, ch, playerSide ? "P-L" : "CPU P-L");
        drawPendulumZone(canvas, w - 7 - sideW, y, sideW, ch, playerSide ? "P-R" : "CPU P-R");

        for (int i = 0; i < field.length; i++) {
            float x = monsterStart + i * (monsterW + gap);
            RectF rect = new RectF(x, y, x + monsterW, y + ch);
            boolean selected = playerSide && duel.getPhase() == DuelEngine.Phase.BATTLE
                    && choosingMonster && selectedMonster == i;
            paint.setColor(selected ? 0xffffc857 : field[i] == null ? 0xff31495b : 0xffbd9653);
            canvas.drawRoundRect(rect, 7, 7, paint);
            if (field[i] != null) {
                paint.setColor(0xff101b2b);
                paint.setTextSize(Math.max(8, w * 0.023f));
                canvas.drawText(shortName(field[i].name), x + 3, y + 17, paint);
                canvas.drawText("ATK " + field[i].attack, x + 3, y + 34, paint);
                canvas.drawText("DEF " + field[i].defense, x + 3, y + 49, paint);
            } else {
                paint.setColor(0xff9aabba);
                paint.setTextSize(Math.max(8, w * 0.022f));
                canvas.drawText("MON", x + 5, y + ch * 0.58f, paint);
            }
        }
    }

    private void drawPendulumZone(Canvas canvas, float x, float y, float width,
                                  float height, String label) {
        RectF zone = new RectF(x, y, x + width, y + height);
        paint.setColor(0xff294f68);
        canvas.drawRoundRect(zone, 6, 6, paint);
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(Math.max(8, getWidth() * 0.021f));
        canvas.drawText("PEND", x + 3, y + height * 0.43f, paint);
        canvas.drawText("ZONE", x + 3, y + height * 0.62f, paint);
        paint.setColor(0xff9fc7dc);
        paint.setTextSize(Math.max(7, getWidth() * 0.017f));
        canvas.drawText(label, x + 2, y + height * 0.82f, paint);
    }

    private void drawHand(Canvas canvas, float w, float h) {
        List<CardDefinition> hand = duel.getPlayerHand();
        float cardW = (w - 28) / 3.1f, cardH = h * 0.105f;
        for (int i = 0; i < hand.size() && i < 5; i++) {
            float x = 7 + i * (w - 14 - cardW) / 4f;
            RectF rect = new RectF(x, h * 0.715f, x + cardW, h * 0.715f + cardH);
            paint.setColor(i == selectedHand && duel.getPhase() == DuelEngine.Phase.MAIN ? 0xffffc857 : 0xff52758b);
            canvas.drawRoundRect(rect, 7, 7, paint);
            paint.setColor(0xff101b2b);
            paint.setTextSize(Math.max(9, w * 0.024f));
            canvas.drawText(shortName(hand.get(i).name), x + 4, rect.top + 18, paint);
            canvas.drawText("ATK " + hand.get(i).attack, x + 4, rect.top + 35, paint);
        }
    }

    private String shortName(String name) { return name.length() > 13 ? name.substring(0, 12) : name; }
}
