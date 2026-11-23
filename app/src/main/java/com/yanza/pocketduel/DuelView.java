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
    public interface Listener { void onExitToMenu(); void onDuelAgain(); }
    private final Listener listener;
    private boolean pauseMenu;
    private int selectedExitOption;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DuelEngine duel;
    private int selectedHand;
    private int selectedMonster;
    private int selectedDeck;
    private boolean choosingMonster;
    private boolean phaseMenu;
    private boolean summonMenu;
    private int selectedSummonAction;
    private int selectedPhaseIndex;
    private boolean deckBrowser;
    private int browsePile;
    private int navRow = 3;
    private int navColumn;
    private CardDefinition detailCard;
    private int detailScroll;
    private String hint = "D-pad navigate | OK action | * phases | # back";

    public DuelView(Context context, Listener listener) {
        this(context, listener, null, null);
    }

    public DuelView(Context context, Listener listener, List<String> mainDeckIds,
                    List<String> extraDeckIds) {
        super(context);
        this.listener = listener;
        this.duel = new DuelEngine(mainDeckIds, extraDeckIds);
        setFocusable(true);
    }

    public boolean handleKey(int key) {
        if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) selectedExitOption = 1 - selectedExitOption;
            else if (key == KeyEvent.KEYCODE_5 || key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                if (selectedExitOption == 0) { if (listener != null) listener.onDuelAgain(); }
                else if (listener != null) listener.onExitToMenu();
            } else if (key == KeyEvent.KEYCODE_0) { selectedExitOption = 1; if (listener != null) listener.onExitToMenu(); }
            else return true;
            invalidate(); return true;
        }
        if (key == KeyEvent.KEYCODE_SOFT_RIGHT) {
            if (summonMenu) {
                summonMenu = false;
            } else if (detailCard == null && !deckBrowser && !phaseMenu
                    && navRow == 3 && selectedHand < duel.getPlayerHand().size()
                    && selectedHand >= 0
                    && duel.getPlayerHand().get(selectedHand).kind == CardDefinition.Kind.MONSTER) {
                summonMenu = true;
                selectedSummonAction = 0;
            } else {
                pauseMenu = !pauseMenu;
                selectedExitOption = 0;
            }
            invalidate();
            return true;
        }
        if (key == KeyEvent.KEYCODE_0) {
            if (pauseMenu) pauseMenu = false;
            else { pauseMenu = true; selectedExitOption = 0; }
            invalidate(); return true;
        }
        if (pauseMenu) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) selectedExitOption = 1 - selectedExitOption;
            else if (key == KeyEvent.KEYCODE_5 || key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                if (selectedExitOption == 0) pauseMenu = false;
                else if (listener != null) listener.onExitToMenu();
            } else return true;
            invalidate(); return true;
        }
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
            List<CardDefinition> deck = getBrowseCards();
            if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_LEFT) {
                selectedDeck = Math.max(0, selectedDeck - 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
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

        if (summonMenu) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                selectedSummonAction = (selectedSummonAction + 1) % 3;
            } else if (key == KeyEvent.KEYCODE_5 || key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER) {
                executeSummonAction();
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
                summonMenu = false;
            } else return false;
            invalidate();
            return true;
        }

        if (phaseMenu) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                selectedPhaseIndex = clamp(selectedPhaseIndex - 1, 0, 5);
            } else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
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

        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            int direction = key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
            navColumn = clamp(navColumn + direction, 0, rowMaxColumn(navRow));
            syncNavigationSelection();
        } else if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            int direction = key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1;
            navRow = clamp(navRow + direction, 0, 3);
            navColumn = clamp(navColumn, 0, rowMaxColumn(navRow));
            syncNavigationSelection();
        } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_5) {
            activateArenaSelection();
        } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
            phaseMenu = false;
            navRow = 3;
            navColumn = 0;
            syncNavigationSelection();
        } else if (key == KeyEvent.KEYCODE_STAR) {
            phaseMenu = true;
            selectedPhaseIndex = phaseIndex(duel.getPhase());
        } else return false;

        if (detailCard == null && !deckBrowser && !phaseMenu) hint = duel.getMessage();
        invalidate();
        return true;
    }

    private int rowMaxColumn(int row) {
        return row == 0 || row == 1 ? 6 : 4;
    }

    private void syncNavigationSelection() {
        choosingMonster = navRow == 0;
        if (navRow == 0 && navColumn >= 1 && navColumn <= 5) {
            selectedMonster = navColumn - 1;
        }
        if (navRow == 3) selectedHand = clamp(navColumn, 0,
                Math.max(0, duel.getPlayerHand().size() - 1));
        hint = navRow == 0 ? "Monster/Pendulum row"
                : navRow == 1 ? "Spell/Trap row"
                : navRow == 2 ? "Deck and pile row" : "Hand row";
    }

    private void activateArenaSelection() {
        if (duel.getPhase() == DuelEngine.Phase.END) {
            if (navRow == 3) {
                duel.endPlayerTurn();
                navRow = 3;
                navColumn = 0;
                syncNavigationSelection();
            } else hint = "Choose the hand or phase menu";
            return;
        }
        if (navRow == 0) {
            if (navColumn == 0 || navColumn == 6) {
                hint = "Pendulum Zone selected - Pendulum placement is not implemented yet";
                return;
            }
            selectedMonster = navColumn - 1;
            if (duel.getPhase() == DuelEngine.Phase.BATTLE) {
                attackSelected();
            } else {
                CardDefinition[] field = duel.getPlayerField();
                if (field[selectedMonster] != null) {
                    if (duel.isPlayerMonsterFaceDown(selectedMonster)) hint = "Your monster is face-down";
                    else openDetails(field[selectedMonster]);
                }
                else hint = "Empty Monster Zone " + (selectedMonster + 1);
            }
        } else if (navRow == 1) {
            if (navColumn == 0 || navColumn == 6) hint = "Pendulum Zone selected";
            else {
                int slot = navColumn - 1;
                CardDefinition[] zones = duel.getPlayerSpellTrap();
                if (slot >= 0 && slot < zones.length && zones[slot] != null) {
                    if (duel.isPlayerSpellTrapFaceDown(slot)) duel.activateSetTrap(slot);
                    else openDetails(zones[slot]);
                    hint = duel.getMessage();
                } else hint = "Empty Spell/Trap Zone " + navColumn;
            }
        } else if (navRow == 2) {
            if (navColumn == 0) {
                hint = "Main Deck is face-down. Its contents cannot be inspected freely.";
            } else if (navColumn >= 1 && navColumn <= 3) {
                browsePile = navColumn;
                selectedDeck = 0;
                deckBrowser = true;
            } else {
                CardDefinition fieldSpell = duel.getPlayerFieldSpell();
                if (fieldSpell != null) openDetails(fieldSpell);
                else hint = "Field Zone is empty";
            }
        } else {
            selectedHand = clamp(navColumn, 0, Math.max(0, duel.getPlayerHand().size() - 1));
            if (duel.getPhase() == DuelEngine.Phase.BATTLE) {
                hint = "Select a Monster Zone to attack";
            } else if (selectedHand < duel.getPlayerHand().size()
                    && duel.getPlayerHand().get(selectedHand).kind == CardDefinition.Kind.SPELL) {
                duel.activateSpellFromHand(selectedHand);
                hint = duel.getMessage();
            } else if (selectedHand < duel.getPlayerHand().size()
                    && duel.getPlayerHand().get(selectedHand).kind == CardDefinition.Kind.TRAP) {
                duel.setTrapFromHand(selectedHand);
                hint = duel.getMessage();
            } else {
                summonMenu = true;
                selectedSummonAction = 0;
            }
        }
    }

    private void executeSummonAction() {
        List<CardDefinition> hand = duel.getPlayerHand();
        if (selectedHand < 0 || selectedHand >= hand.size()) {
            hint = "Select a monster in your hand";
        } else {
            CardDefinition card = hand.get(selectedHand);
            if (selectedSummonAction == 0) {
                if (card.level >= 5) duel.tributeSummonFromHand(selectedHand);
                else duel.summonFromHand(selectedHand);
            } else if (selectedSummonAction == 1) {
                if (card.level >= 5) duel.tributeSetFromHand(selectedHand);
                else duel.setFromHand(selectedHand);
            } else if (card.isPendulumMonster() && (duel.getPlayerPendulumLeft() == null || duel.getPlayerPendulumRight() == null)) {
                duel.placePendulumFromHand(selectedHand);
            } else {
                duel.pendulumSummonFromHand();
            }
            selectedHand = clamp(selectedHand, 0, Math.max(0, duel.getPlayerHand().size() - 1));
            navColumn = selectedHand;
            hint = duel.getMessage();
        }
        summonMenu = false;
    }

    private List<CardDefinition> getBrowseCards() {
        switch (browsePile) {
            case 1: return duel.getPlayerExtraDeck();
            case 2: return duel.getPlayerGraveyard();
            case 3: return duel.getPlayerBanished();
            default: return duel.getPlayerDeck();
        }
    }

    private String browseTitle() {
        switch (browsePile) {
            case 1: return "EXTRA DECK";
            case 2: return "GRAVEYARD";
            case 3: return "REMOVED";
            default: return "MAIN DECK";
        }
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
        canvas.drawText("D-PAD navigate | 5 inspect | 7/9 text | * phases | RIGHT actions/pause | BACK return", 14, h - 20, paint);

        if (deckBrowser) drawDeckBrowser(canvas, w, h);
        if (phaseMenu) drawPhaseMenu(canvas, w, h);
        if (detailCard != null) drawCardDetails(canvas, w, h);
        if (summonMenu) drawSummonMenu(canvas, w, h);
        if (pauseMenu) drawExitMenu(canvas, w, h, false);
        if (duel.getPhase() == DuelEngine.Phase.FINISHED) drawExitMenu(canvas, w, h, true);
    }

    private void drawExitMenu(Canvas canvas, float w, float h, boolean finished) {
        float left = w * 0.09f, right = w * 0.91f;
        float top = h * 0.39f, bottom = h * 0.62f;
        paint.setColor(0xf0142637);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 12, 12, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffffd477);
        paint.setTextSize(Math.max(16, w * 0.048f));
        canvas.drawText(finished ? (duel.getPlayerLP() > 0 ? "YOU WIN!" : "YOU LOSE") : "DUEL PAUSED", w / 2f, top + 32, paint);
        String[] labels = finished ? new String[]{"DUEL AGAIN", "MAIN MENU"} : new String[]{"RESUME", "EXIT TO MAIN MENU"};
        for (int i = 0; i < labels.length; i++) {
            float x = left + 10 + i * ((right - left - 25) / 2f);
            float width = (right - left - 25) / 2f;
            paint.setColor(selectedExitOption == i ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(x, top + 54, x + width, top + 94), 6, 6, paint);
            paint.setColor(selectedExitOption == i ? 0xff172536 : 0xfff0f5f8);
            paint.setTextSize(Math.max(9, w * 0.027f));
            canvas.drawText(labels[i], x + width / 2f, top + 79, paint);
        }
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(Math.max(10, w * 0.028f));
        canvas.drawText("D-PAD LEFT/RIGHT   5 CONFIRM   RIGHT PAUSE", w / 2f, bottom - 12, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }


    private void drawSummonMenu(Canvas canvas, float w, float h) {
        float left = w * 0.12f, right = w * 0.88f;
        float top = h * 0.42f;
        paint.setColor(0xee14283a);
        canvas.drawRoundRect(new RectF(left, top, right, top + 104), 12, 12, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffffd477);
        paint.setTextSize(16);
        canvas.drawText("MONSTER ACTION", w / 2f, top + 25, paint);
        String[] labels = {"SUMMON", "SET", "PENDULUM"};
        for (int i = 0; i < labels.length; i++) {
            float width = (right - left - 30) / 3f;
            float x = left + 10 + i * (width + 5);
            paint.setColor(selectedSummonAction == i ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(x, top + 38, x + width, top + 72), 6, 6, paint);
            paint.setColor(selectedSummonAction == i ? 0xff172536 : 0xfff0f5f8);
            paint.setTextSize(Math.max(9, w * 0.028f));
            canvas.drawText(labels[i], x + width / 2f, top + 60, paint);
        }
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(10);
        canvas.drawText("D-PAD LEFT/RIGHT | 5 confirm | BACK cancel", w / 2f, top + 94, paint);
        paint.setTextAlign(Paint.Align.LEFT);
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
        canvas.drawText("D-PAD move | 5 select | BACK close", left + 12, top + cellH + 48, paint);
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

        drawPendulumZone(canvas, 7, y, sideW, zoneH * 2 + rowGap, "P-L", playerSide ? duel.getPlayerPendulumLeft() : null);
        drawPendulumZone(canvas, w - 7 - sideW, y, sideW, zoneH * 2 + rowGap, "P-R", playerSide ? duel.getPlayerPendulumRight() : null);

        drawZoneRow(canvas, field, start, y, zoneW, zoneH, gap, true, playerSide);
        drawZoneRow(canvas, playerSide ? duel.getPlayerSpellTrap() : duel.getCpuSpellTrap(), start, spellY, zoneW, zoneH, gap, false, playerSide);

        if (playerSide) {
            float pileY = spellY + zoneH + getHeight() * 0.012f;
            drawPileStrip(canvas, pileY, w);
        } else if (duel.getCpuFieldSpell() != null) {
            paint.setColor(0xffffd36a);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(Math.max(7, Math.min(10, w * 0.023f)));
            canvas.drawText("FIELD: " + duel.getCpuFieldSpell().name,
                    start, spellY + zoneH + getHeight() * 0.023f, paint);
        }
    }

    private void drawZoneRow(Canvas canvas, CardDefinition[] monsters, float start,
                             float y, float zoneW, float zoneH, float gap,
                             boolean monsterRow, boolean playerSide) {
        for (int i = 0; i < 5; i++) {
            float x = start + i * (zoneW + gap);
            RectF rect = new RectF(x, y, x + zoneW, y + zoneH);
            CardDefinition card = monsters != null ? monsters[i] : null;
            boolean selected = monsterRow && playerSide && navRow == 0 && navColumn == i + 1;
            boolean rowSelected = playerSide && navRow == (monsterRow ? 0 : 1)
                    && navColumn == i + 1;
            paint.setColor(rowSelected ? 0xffffc857
                    : card == null ? (monsterRow ? 0xff31495b : 0xff29465a)
                    : 0xffbd9653);
            canvas.drawRoundRect(rect, 4, 4, paint);
            paint.setColor(card == null ? 0xffc6d5df : 0xff101b2b);
            paint.setTextSize(Math.max(7, getWidth() * 0.019f));
            boolean hiddenOpponentCard = monsterRow ? (playerSide
                    ? duel.isPlayerMonsterFaceDown(i) : duel.isCpuMonsterFaceDown(i))
                    : (playerSide ? duel.isPlayerSpellTrapFaceDown(i) : duel.isCpuSpellTrapFaceDown(i));
            String label = monsterRow ? (card == null ? "M" + (i + 1)
                    : hiddenOpponentCard ? "SET" : shortName(card.name, 7))
                    : (card == null ? "S/T " + (i + 1)
                    : hiddenOpponentCard ? "SET" : shortName(card.name, 7));
            canvas.drawText(label, x + 2, y + zoneH * 0.43f, paint);
            if (card != null && !hiddenOpponentCard && monsterRow) {
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
            boolean selected = navRow == 2 && navColumn == i;
            paint.setColor(selected ? 0xffffc857 : (i == 4 ? 0xff294f68 : 0xff263d50));
            canvas.drawRoundRect(new RectF(x, y, x + pileW, y + pileH), 4, 4, paint);
            paint.setColor(selected ? 0xff172536 : 0xffd9eaf4);
            paint.setTextSize(Math.max(7, Math.min(10, w * 0.025f)));
            canvas.drawText(labels[i], x + 3, y + pileH * 0.42f, paint);
            paint.setTextSize(Math.max(7, w * 0.020f));
            String pileInfo = i == 0 ? "FD " + counts[i]
                    : i == 4 ? (duel.getPlayerFieldSpell() == null ? "EMPTY" : "ACTIVE") : String.valueOf(counts[i]);
            canvas.drawText(pileInfo, x + 3, y + pileH * 0.82f, paint);
        }
    }

    private void drawPendulumZone(Canvas canvas, float x, float y, float width,
                                  float height, String label, CardDefinition card) {
        RectF zone = new RectF(x, y, x + width, y + height);
        paint.setColor(card == null ? 0xff294f68 : 0xff8b6740);
        canvas.drawRoundRect(zone, 5, 5, paint);
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(Math.max(7, getWidth() * 0.019f));
        canvas.drawText(label, x + 2, y + height * 0.30f, paint);
        canvas.drawText(card == null ? "EMPTY" : "S" + card.pendulumScale,
                x + 2, y + height * 0.55f, paint);
        if (card != null) {
            paint.setTextSize(Math.max(6, getWidth() * 0.016f));
            canvas.drawText(shortName(card.name, 8), x + 2, y + height * 0.78f, paint);
        }
    }

    private void drawHand(Canvas canvas, float w, float h) {
        List<CardDefinition> hand = duel.getPlayerHand();
        float cardW = (w - 28) / 3.1f, cardH = h * 0.105f;
        for (int i = 0; i < hand.size() && i < 5; i++) {
            float x = 7 + i * (w - 14 - cardW) / 4f;
            RectF rect = new RectF(x, h * 0.715f, x + cardW, h * 0.715f + cardH);
            boolean selected = navRow == 3 && navColumn == i;
            paint.setColor(selected ? 0xffffc857 : 0xff52758b);
            canvas.drawRoundRect(rect, 7, 7, paint);
            paint.setColor(selected ? 0xff172536 : 0xff101b2b);
            paint.setTextSize(Math.max(9, w * 0.024f));
            canvas.drawText(shortName(hand.get(i).name, 13), x + 4, rect.top + 18, paint);
            canvas.drawText(hand.get(i).kind == CardDefinition.Kind.MONSTER
                    ? "ATK " + hand.get(i).attack : hand.get(i).kind.name(), x + 4, rect.top + 35, paint);
        }
    }

    private void drawDeckBrowser(Canvas canvas, float w, float h) {
        List<CardDefinition> deck = getBrowseCards();
        paint.setColor(0xee101820);
        canvas.drawRoundRect(new RectF(12, 65, w - 12, h - 90), 12, 12, paint);
        paint.setColor(0xffffd477);
        paint.setTextSize(17);
        canvas.drawText(browseTitle() + "  (" + deck.size() + " cards)", 25, 92, paint);
        paint.setTextSize(13);
        int first = Math.max(0, selectedDeck - 7);
        int last = Math.min(deck.size(), first + 15);
        for (int i = first; i < last; i++) {
            paint.setColor(i == selectedDeck ? 0xffffd477 : 0xffe7edf2);
            canvas.drawText((i + 1) + ". " + deck.get(i).name, 26, 120 + (i - first) * 22, paint);
        }
        paint.setColor(0xffd9eaf4);
        canvas.drawText("D-PAD browse | 5 inspect | BACK close", 25, h - 108, paint);
    }

    private void drawCardDetails(Canvas canvas, float w, float h) {
        CardDefinition c = detailCard;
        List<String> lines = new ArrayList<String>();
        lines.add(c.name);
        lines.add("KIND: " + c.kind);
        if (c.kind == CardDefinition.Kind.SPELL || c.kind == CardDefinition.Kind.TRAP) {
            lines.add("CARD TYPE: " + c.spellTrapType);
        }
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
