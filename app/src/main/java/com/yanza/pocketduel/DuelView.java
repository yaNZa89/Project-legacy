package com.yanza.pocketduel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Portrait duel screen with context-sensitive keypad controls and card inspection. */
public class DuelView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DuelEngine duel = new DuelEngine();
    private int selectedHand;
    private int selectedMonster;
    private int selectedDeck;
    private boolean choosingMonster;
    private boolean phaseMenu;
    private int selectedPhaseIndex;
    private boolean deckBrowser;
    private CardDefinition detailCard;
    private int detailScroll;
    private String hint = "Keypad: 2/8 row | 4/6 cursor | 5 action | * phases | # back";

    public DuelView(Context context) {
        super(context);
        setFocusable(true);
    }

    public boolean handleKey(int key) {
        if (detailCard != null) {
            if (key == KeyEvent.KEYCODE_7) {
                detailScroll = Math.max(0, detailScroll - 1);
            } else if (key == KeyEvent.KEYCODE_9) {
                detailScroll++;
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_5) {
                detailCard = null;
                detailScroll = 0;
            } else return false;
            invalidate();
            return true;
        }

        if (deckBrowser) {
            List<CardDefinition> deck = duel.getPlayerDeck();
            if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_LEFT
                    || key == KeyEvent.KEYCODE_2 || key == KeyEvent.KEYCODE_4) {
                selectedDeck = Math.max(0, selectedDeck - 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_RIGHT
                    || key == KeyEvent.KEYCODE_8 || key == KeyEvent.KEYCODE_6) {
                selectedDeck = Math.min(Math.max(0, deck.size() - 1), selectedDeck + 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_5) {
                if (!deck.isEmpty()) openDetails(deck.get(selectedDeck));
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
                deckBrowser = false;
            } else return false;
            invalidate();
            return true;
        }

        if (phaseMenu) {
            if (key == KeyEvent.KEYCODE_4 || key == KeyEvent.KEYCODE_DPAD_LEFT) {
                selectedPhaseIndex = clamp(selectedPhaseIndex - 1, 0, 5);
            } else if (key == KeyEvent.KEYCODE_6 || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                selectedPhaseIndex = clamp(selectedPhaseIndex + 1, 0, 5);
            } else if (key == KeyEvent.KEYCODE_5 || key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER) {
                activateSelectedPhase();
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK
                    || key == KeyEvent.KEYCODE_STAR) {
                phaseMenu = false;
            } else return false;
            invalidate();
            return true;
        }

        if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
            if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                    || key == KeyEvent.KEYCODE_5) {
                hint = "Duel finished - restart the app for a new duel";
                invalidate();
                return true;
            }
            return true;
        }

        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_4 || key == KeyEvent.KEYCODE_6) {
            int direction = (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_4) ? -1 : 1;
            if (duel.getPhase() == DuelEngine.Phase.BATTLE || choosingMonster) {
                selectedMonster = clamp(selectedMonster + direction, 0, 4);
                choosingMonster = true;
            } else {
                selectedHand = clamp(selectedHand + direction, 0,
                        Math.max(0, duel.getPlayerHand().size() - 1));
                choosingMonster = false;
            }
        } else if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_2 || key == KeyEvent.KEYCODE_8) {
            boolean moveUp = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_2;
            if (duel.getPhase() != DuelEngine.Phase.BATTLE) {
                choosingMonster = !choosingMonster;
                hint = choosingMonster ? "Monster row selected - 4/6 move, 5 action"
                        : "Hand selected - 4/6 move, 5 action";
            } else {
                selectedMonster = clamp(selectedMonster + (moveUp ? -1 : 1), 0, 4);
            }
        } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_5) {
            if (duel.getPhase() == DuelEngine.Phase.END) {
                duel.endPlayerTurn();
                selectedHand = 0;
                selectedMonster = 0;
                choosingMonster = false;
            } else if (duel.getPhase() == DuelEngine.Phase.BATTLE) {
                attackSelected();
            } else if (choosingMonster) {
                CardDefinition[] field = duel.getPlayerField();
                if (selectedMonster >= 0 && selectedMonster < field.length
                        && field[selectedMonster] != null) openDetails(field[selectedMonster]);
                else hint = "Empty Monster Zone";
            } else {
                duel.summonFromHand(selectedHand);
                selectedHand = Math.min(selectedHand,
                        Math.max(0, duel.getPlayerHand().size() - 1));
            }
        } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
            phaseMenu = false;
            choosingMonster = false;
            selectedHand = 0;
        } else if (key == KeyEvent.KEYCODE_STAR) {
            phaseMenu = true;
            selectedPhaseIndex = phaseIndex(duel.getPhase());
        } else return false;

        if (detailCard == null && !deckBrowser && !phaseMenu) hint = duel.getMessage();
        invalidate();
        return true;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private CardDefinition selectedCard() {
        if (choosingMonster || duel.getPhase() == DuelEngine.Phase.BATTLE) {
            CardDefinition[] field = duel.getPlayerField();
            return selectedMonster >= 0 && selectedMonster < field.length
                    ? field[selectedMonster] : null;
        }
        List<CardDefinition> hand = duel.getPlayerHand();
        return selectedHand >= 0 && selectedHand < hand.size() ? hand.get(selectedHand) : null;
    }

    private void openDetails(CardDefinition card) {
        detailCard = card;
        detailScroll = 0;
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

        drawField(canvas, duel.getCpuField(), h * 0.115f, w, false);
        drawField(canvas, duel.getPlayerField(), h * 0.385f, w, true);

        paint.setColor(0xfff0f5f8);
        paint.setTextSize(Math.max(14, w * 0.039f));
        canvas.drawText("YOU  LP: " + duel.getPlayerLP(), 14, h * 0.65f, paint);
        canvas.drawText("HAND (" + duel.getPlayerHand().size() + ")", 14, h * 0.70f, paint);
        drawHand(canvas, w, h);

        drawPhaseRibbon(canvas, w, h);
        paint.setColor(0xfff3f5f7);
        paint.setTextSize(Math.max(10, w * 0.028f));
        canvas.drawText(hint, 14, h - 48, paint);
        canvas.drawText("2/8 row | 4/6 cursor | 5 action | * phases | # back", 14, h - 20, paint);

        if (deckBrowser) drawDeckBrowser(canvas, w, h);
        if (phaseMenu) drawPhaseMenu(canvas, w, h);
        if (detailCard != null) drawCardDetails(canvas, w, h);
    }


    private int phaseIndex(DuelEngine.Phase phase) {
        switch (phase) {
            case DRAW: return 0;
            case STANDBY: return 1;
            case MAIN1: return 2;
            case BATTLE: return 3;
            case MAIN2: return 4;
            case END: return 5;
            default: return 0;
        }
    }

    private void activateSelectedPhase() {
        DuelEngine.Phase next;
        switch (selectedPhaseIndex) {
            case 0: next = DuelEngine.Phase.DRAW; break;
            case 1: next = DuelEngine.Phase.STANDBY; break;
            case 2: next = DuelEngine.Phase.MAIN1; break;
            case 3: next = DuelEngine.Phase.BATTLE; break;
            case 4: next = DuelEngine.Phase.MAIN2; break;
            case 5: next = DuelEngine.Phase.END; break;
            default: return;
        }
        if (next == DuelEngine.Phase.DRAW && duel.getPhase() == DuelEngine.Phase.END) {
            duel.endPlayerTurn();
            selectedPhaseIndex = 0;
            selectedHand = 0;
            selectedMonster = 0;
            choosingMonster = false;
        } else {
            duel.advanceToPhase(next);
            if (duel.getPhase() == next) {
                selectedPhaseIndex = phaseIndex(next);
                choosingMonster = next == DuelEngine.Phase.BATTLE;
            }
        }
        hint = duel.getMessage();
        phaseMenu = false;
    }

    private void drawPhaseMenu(Canvas canvas, float w, float h) {
        String[] labels = {"DRAW", "STBY", "MAIN 1", "BATTLE", "MAIN 2", "END"};
        float panelW = w * 0.94f;
        float left = (w - panelW) / 2f;
        float top = h * 0.39f;
        float cellW = (panelW - 16 - 5 * 3) / 6f;
        float cellH = 42;
        paint.setColor(0xee14283a);
        canvas.drawRoundRect(new RectF(left, top - 34, left + panelW, top + cellH + 28), 12, 12, paint);
        paint.setColor(0xffffd477);
        paint.setTextSize(Math.max(13, w * 0.038f));
        canvas.drawText("SELECT PHASE", left + 12, top - 12, paint);
        for (int i = 0; i < labels.length; i++) {
            float x = left + 8 + i * (cellW + 3);
            paint.setColor(i == selectedPhaseIndex ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(x, top, x + cellW, top + cellH), 6, 6, paint);
            paint.setColor(i == selectedPhaseIndex ? 0xff172536 : 0xffd8e3eb);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.max(8, Math.min(11, w * 0.027f)));
            canvas.drawText(labels[i], x + cellW / 2f, top + 25, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(Math.max(10, w * 0.028f));
        canvas.drawText("4/6 move | 5 select | # close", left + 12, top + cellH + 48, paint);
    }

    private void drawPhaseRibbon(Canvas canvas, float w, float h) {
        String[] labels = {"DRAW", "STBY", "MAIN 1", "BATTLE", "MAIN 2", "END"};
        DuelEngine.Phase[] phases = {
                DuelEngine.Phase.DRAW, DuelEngine.Phase.STANDBY, DuelEngine.Phase.MAIN1,
                DuelEngine.Phase.BATTLE, DuelEngine.Phase.MAIN2, DuelEngine.Phase.END
        };
        float gap = 3;
        float cellW = (w - 14 - gap * 5) / 6f;
        float top = h - 105;
        float bottom = h - 79;
        for (int i = 0; i < labels.length; i++) {
            float x = 7 + i * (cellW + gap);
            boolean active = duel.getPhase() == phases[i];
            boolean cursor = false;
            paint.setColor(active ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(x, top, x + cellW, bottom), 5, 5, paint);
            paint.setColor(active ? 0xff172536 : 0xffd8e3eb);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.max(8, Math.min(11, w * 0.027f)));
            canvas.drawText(labels[i], x + cellW / 2f, top + 17, paint);
            if (cursor) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.5f);
                paint.setColor(0xffffffff);
                canvas.drawRoundRect(new RectF(x - 1, top - 1, x + cellW + 1, bottom + 1), 6, 6, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawField(Canvas canvas, CardDefinition[] field, float y, float w,
                           boolean playerSide) {
        float gap = 3;
        float sideW = w * 0.085f;
        float start = 7 + sideW + gap;
        float zoneW = (w - 14 - sideW * 2 - gap * 6) / 5f;
        float zoneH = getHeight() * 0.075f;
        float rowGap = getHeight() * 0.010f;
        float spellY = y + zoneH + rowGap;

        drawPendulumZone(canvas, 7, y, sideW, zoneH * 2 + rowGap, "P-L");
        drawPendulumZone(canvas, w - 7 - sideW, y, sideW, zoneH * 2 + rowGap, "P-R");

        drawZoneRow(canvas, field, start, y, zoneW, zoneH, gap, true, playerSide);
        drawZoneRow(canvas, null, start, spellY, zoneW, zoneH, gap, false, false);

        if (playerSide) {
            float pileY = spellY + zoneH + getHeight() * 0.012f;
            drawPileStrip(canvas, pileY, w);
        }
    }

    private void drawZoneRow(Canvas canvas, CardDefinition[] monsters, float start,
                             float y, float zoneW, float zoneH, float gap,
                             boolean monsterRow, boolean playerSide) {
        for (int i = 0; i < 5; i++) {
            float x = start + i * (zoneW + gap);
            RectF rect = new RectF(x, y, x + zoneW, y + zoneH);
            CardDefinition card = monsterRow && monsters != null ? monsters[i] : null;
            boolean selected = monsterRow && playerSide && selectedMonster == i
                    && (choosingMonster || duel.getPhase() == DuelEngine.Phase.BATTLE);
            paint.setColor(selected ? 0xffffc857
                    : card == null ? (monsterRow ? 0xff31495b : 0xff29465a)
                    : 0xffbd9653);
            canvas.drawRoundRect(rect, 4, 4, paint);
            paint.setColor(card == null ? 0xffc6d5df : 0xff101b2b);
            paint.setTextSize(Math.max(7, getWidth() * 0.019f));
            String label = monsterRow ? (card == null ? "M" + (i + 1)
                    : shortName(card.name, 7)) : "S/T " + (i + 1);
            canvas.drawText(label, x + 2, y + zoneH * 0.43f, paint);
            if (card != null) {
                paint.setTextSize(Math.max(7, getWidth() * 0.018f));
                canvas.drawText("A" + card.attack + " D" + card.defense,
                        x + 2, y + zoneH * 0.78f, paint);
            }
        }
    }

    private void drawPileStrip(Canvas canvas, float y, float w) {
        String[] labels = {"DECK", "EXTRA", "GRAVE", "REMOVED", "FIELD"};
        int[] counts = {duel.getPlayerDeck().size(), duel.getPlayerExtraDeck().size(),
                duel.getPlayerGraveyard().size(), duel.getPlayerBanished().size(), 0};
        float gap = 4;
        float pileW = (w - 14 - gap * 4) / 5f;
        float pileH = getHeight() * 0.045f;
        for (int i = 0; i < labels.length; i++) {
            float x = 7 + i * (pileW + gap);
            paint.setColor(i == 4 ? 0xff294f68 : 0xff263d50);
            canvas.drawRoundRect(new RectF(x, y, x + pileW, y + pileH), 4, 4, paint);
            paint.setColor(0xffd9eaf4);
            paint.setTextSize(Math.max(7, Math.min(10, w * 0.025f)));
            canvas.drawText(labels[i], x + 3, y + pileH * 0.42f, paint);
            paint.setTextSize(Math.max(7, w * 0.020f));
            canvas.drawText(i == 4 ? "ZONE" : String.valueOf(counts[i]),
                    x + 3, y + pileH * 0.82f, paint);
        }
    }

    private void drawPendulumZone(Canvas canvas, float x, float y, float width,
                                  float height, String label) {
        RectF zone = new RectF(x, y, x + width, y + height);
        paint.setColor(0xff294f68);
        canvas.drawRoundRect(zone, 5, 5, paint);
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(Math.max(7, getWidth() * 0.019f));
        canvas.drawText("PEND", x + 2, y + height * 0.43f, paint);
        canvas.drawText(label, x + 2, y + height * 0.68f, paint);
    }

    private void drawHand(Canvas canvas, float w, float h) {
        List<CardDefinition> hand = duel.getPlayerHand();
        float cardW = (w - 28) / 3.1f, cardH = h * 0.105f;
        for (int i = 0; i < hand.size() && i < 5; i++) {
            float x = 7 + i * (w - 14 - cardW) / 4f;
            RectF rect = new RectF(x, h * 0.715f, x + cardW, h * 0.715f + cardH);
            paint.setColor(i == selectedHand && !choosingMonster
                    ? 0xffffc857 : 0xff52758b);
            canvas.drawRoundRect(rect, 7, 7, paint);
            paint.setColor(0xff101b2b);
            paint.setTextSize(Math.max(9, w * 0.024f));
            canvas.drawText(shortName(hand.get(i).name, 13), x + 4, rect.top + 18, paint);
            canvas.drawText("ATK " + hand.get(i).attack, x + 4, rect.top + 35, paint);
        }
    }

    private void drawDeckBrowser(Canvas canvas, float w, float h) {
        List<CardDefinition> deck = duel.getPlayerDeck();
        paint.setColor(0xee101820);
        canvas.drawRoundRect(new RectF(12, 65, w - 12, h - 90), 12, 12, paint);
        paint.setColor(0xffffd477);
        paint.setTextSize(17);
        canvas.drawText("DECK VIEW  (" + deck.size() + " cards)", 25, 92, paint);
        paint.setTextSize(13);
        int first = Math.max(0, selectedDeck - 7);
        int last = Math.min(deck.size(), first + 15);
        for (int i = first; i < last; i++) {
            paint.setColor(i == selectedDeck ? 0xffffd477 : 0xffe7edf2);
            canvas.drawText((i + 1) + ". " + deck.get(i).name, 26, 120 + (i - first) * 22, paint);
        }
        paint.setColor(0xffd9eaf4);
        canvas.drawText("2/8 browse | 5 details | # close", 25, h - 108, paint);
    }

    private void drawCardDetails(Canvas canvas, float w, float h) {
        CardDefinition c = detailCard;
        List<String> lines = new ArrayList<String>();
        lines.add(c.name);
        lines.add("KIND: " + c.kind);
        if (c.kind == CardDefinition.Kind.MONSTER) {
            lines.add("TYPE: " + (c.monsterType == null ? "Unassigned" : c.monsterType));
            lines.add("ATTRIBUTE: " + (c.attribute == null ? "Unassigned" : c.attribute));
            lines.add("LEVEL: " + c.level);
            lines.add("ATK: " + c.attack + "    DEF: " + c.defense);
            if (c.isPendulumMonster()) lines.add("PENDULUM SCALE: " + c.pendulumScale);
            lines.add("SUMMON: " + c.summonMethod);
        }
        lines.add("EFFECT:");
        if ("DRAW_ONE_ON_SUMMON".equals(c.effectId)) {
            lines.add("When this card is Normal Summoned:");
            lines.add("draw 1 card.");
        } else if (c.effectId.length() == 0) {
            lines.add("No effect text assigned in this prototype yet.");
        } else {
            lines.add(c.effectId);
        }
        lines.add("CARD ID: " + c.id);

        paint.setColor(0xf0101820);
        canvas.drawRoundRect(new RectF(10, 45, w - 10, h - 45), 12, 12, paint);
        paint.setColor(0xffffd477);
        paint.setTextSize(Math.max(17, w * 0.05f));
        canvas.drawText("CARD DETAILS", 24, 75, paint);
        paint.setTextSize(Math.max(12, w * 0.034f));
        float lineHeight = Math.max(20, h * 0.032f);
        int visible = Math.max(1, (int) ((h - 165) / lineHeight));
        detailScroll = clamp(detailScroll, 0, Math.max(0, lines.size() - visible));
        for (int i = 0; i < visible && i + detailScroll < lines.size(); i++) {
            String line = lines.get(i + detailScroll);
            paint.setColor(i + detailScroll == 0 ? 0xffffd477 : 0xfff0f5f8);
            canvas.drawText(line, 24, 110 + i * lineHeight, paint);
        }
        paint.setColor(0xffb9cad5);
        paint.setTextSize(Math.max(10, w * 0.027f));
        canvas.drawText("7 scroll up | 9 scroll down | 5/# close",
                24, h - 65, paint);
    }

    private String shortName(String name, int max) {
        return name.length() > max ? name.substring(0, max - 1) : name;
    }
}
