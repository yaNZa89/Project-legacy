package com.yanza.pocketduel;

import android.content.Context;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.EditText;
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
    private DuelEngine duel;
    private boolean pauseMenu;
    private boolean titleScreen;
    private boolean deckEditor;
    private int titleOption;
    private int editorTab;
    private int editorCursor;
    private final List<CardDefinition> mainDeckDraft = new ArrayList<CardDefinition>();
    private final List<CardDefinition> extraDeckDraft = new ArrayList<CardDefinition>();
    private boolean opponentMenu;
    private int selectedPauseOption;
    private int selectedOpponent;
    private int selectedHand;
    private int selectedMonster;
    private int selectedDeck;
    private boolean choosingMonster;
    private boolean phaseMenu;
    private boolean summonMenu;
    private int selectedSummonAction;
    private int selectedPhaseIndex;
    private boolean deckBrowser;
    private boolean gridView = true;
    private int browsePile;
    private String browseSearch = "";
    private int navRow = 3;
    private int navColumn;
    private CardDefinition detailCard;
    private int detailScroll;
    private String hint = "D-pad: navigate/confirm | * Grid/List | # back";

    public DuelView(Context context) {
        super(context);
        duel = createConfiguredDuel();
        setFocusable(true);
    }

    public boolean handleKey(int key) {
        if (deckEditor) {
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                editorCursor = Math.max(0, editorCursor - 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                editorCursor = Math.min(Math.max(0, editorCards().size() - 1), editorCursor + 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                editorTab = 1 - editorTab;
                editorCursor = 0;
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                addEditorCard();
            } else if (key == KeyEvent.KEYCODE_SOFT_LEFT) {
                removeEditorCard();
            } else if (key == KeyEvent.KEYCODE_STAR) {
                gridView = !gridView;
            } else if (key == KeyEvent.KEYCODE_SOFT_RIGHT) {
                showDeckActions();
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
                deckEditor = false;
                titleScreen = true;
            } else return false;
            invalidate();
            return true;
        }
        if (titleScreen) {
            if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                titleOption = 1 - titleOption;
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                if (titleOption == 0) {
                    titleScreen = false;
                    duel = new DuelEngine();
                    resetArenaCursor();
                    hint = duel.getMessage();
                } else {
                    titleScreen = false;
                    deckEditor = true;
                    editorTab = 0;
                    editorCursor = 0;
                }
            } else if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_POUND) {
                titleScreen = false;
            } else return false;
            invalidate();
            return true;
        }
        if (opponentMenu) {
            if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                selectedOpponent = 0;
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                opponentMenu = false;
                pauseMenu = false;
                duel = new DuelEngine();
                resetArenaCursor();
                hint = "Opponent selected: CPU";
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
                opponentMenu = false;
                pauseMenu = true;
            } else return false;
            invalidate();
            return true;
        }
        if (pauseMenu) {
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                selectedPauseOption = clamp(selectedPauseOption - 1, 0, duel.getPhase() == DuelEngine.Phase.FINISHED ? 2 : 3);
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                selectedPauseOption = clamp(selectedPauseOption + 1, 0, duel.getPhase() == DuelEngine.Phase.FINISHED ? 2 : 3);
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
                    if (selectedPauseOption == 0) {
                        pauseMenu = false;
                        duel = new DuelEngine();
                        resetArenaCursor();
                        hint = "Duel restarted";
                    } else if (selectedPauseOption == 1) {
                        opponentMenu = true;
                        selectedOpponent = 0;
                    } else {
                        pauseMenu = false;
                        titleScreen = true;
                    }
                } else if (selectedPauseOption == 0) {
                    pauseMenu = false;
                } else if (selectedPauseOption == 1) {
                    pauseMenu = false;
                    duel = new DuelEngine();
                    resetArenaCursor();
                    hint = "Duel restarted";
                } else if (selectedPauseOption == 2) {
                    opponentMenu = true;
                    selectedOpponent = 0;
                } else {
                    pauseMenu = false;
                    titleScreen = true;
                }
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
                if (duel.getPhase() != DuelEngine.Phase.FINISHED) pauseMenu = false;
            } else return false;
            invalidate();
            return true;
        }
        if (key == KeyEvent.KEYCODE_SOFT_RIGHT) {
            pauseMenu = true;
            selectedPauseOption = 0;
            invalidate();
            return true;
        }
        if (detailCard != null) {
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                detailScroll = Math.max(0, detailScroll - 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                detailScroll++;
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER) {
                detailCard = null;
                detailScroll = 0;
            } else return false;
            invalidate();
            return true;
        }

        if (key == KeyEvent.KEYCODE_SOFT_LEFT) {
            if (!deckBrowser) {
                browsePile = 0;
                deckBrowser = true;
            }
            showCardSearch();
            invalidate();
            return true;
        }

        if (deckBrowser) {
            List<CardDefinition> deck = getBrowseCards();
            if (key == KeyEvent.KEYCODE_STAR) {
                gridView = !gridView;
                hint = gridView ? "Card view: GRID" : "Card view: LIST";
            } else if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_LEFT) {
                selectedDeck = Math.max(0, selectedDeck - 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_RIGHT
                   ) {
                selectedDeck = Math.min(Math.max(0, deck.size() - 1), selectedDeck + 1);
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER) {
                if (!deck.isEmpty()) openDetails(deck.get(selectedDeck));
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
                deckBrowser = false;
                browseSearch = "";
                selectedDeck = 0;
            } else return false;
            invalidate();
            return true;
        }

        if (summonMenu) {
            List<CardDefinition> hand = duel.getPlayerHand();
            CardDefinition selected = selectedHand >= 0 && selectedHand < hand.size()
                    ? hand.get(selectedHand) : null;
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (selected != null && selected.kind == CardDefinition.Kind.MONSTER) {
                    int actionCount = duel.canSpecialSummonFromHand(selectedHand) ? 3 : 2;
                    int direction = key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
                    selectedSummonAction = (selectedSummonAction + direction + actionCount) % actionCount;
                }
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
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
            } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                activateSelectedPhase();
            } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK
                    || key == KeyEvent.KEYCODE_STAR) {
                phaseMenu = false;
            } else return false;
            invalidate();
            return true;
        }

        if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
            invalidate();
            return true;
        }

        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
               ) {
            int direction = key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
            navColumn = clamp(navColumn + direction, 0, rowMaxColumn(navRow));
            syncNavigationSelection();
        } else if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
               ) {
            int direction = key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1;
            navRow = clamp(navRow + direction, 0, 4);
            navColumn = clamp(navColumn, 0, rowMaxColumn(navRow));
            syncNavigationSelection();
        } else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            activateArenaSelection();
        } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
            phaseMenu = false;
            navRow = 3;
            navColumn = 0;
            syncNavigationSelection();
        } else if (key == KeyEvent.KEYCODE_STAR) {
            gridView = !gridView;
            hint = gridView ? "Card view: GRID" : "Card view: LIST";
        } else return false;

        if (detailCard == null && !deckBrowser && !phaseMenu && key != KeyEvent.KEYCODE_STAR) hint = duel.getMessage();
        invalidate();
        return true;
    }

    private int rowMaxColumn(int row) {
        if (row == 3) return Math.max(0, duel.getPlayerHand().size() - 1);
        return row == 0 || row == 1 || row == 2 ? 4 : row == 4 ? 5 : 4;
    }

    private void syncNavigationSelection() {
        choosingMonster = navRow == 0;
        if (navRow == 0 && navColumn >= 0 && navColumn < 5) {
            selectedMonster = navColumn;
        }
        if (navRow == 3) selectedHand = clamp(navColumn, 0,
                Math.max(0, duel.getPlayerHand().size() - 1));
        hint = navRow == 0 ? "Monster/Pendulum row"
                : navRow == 1 ? "Spell/Trap row"
                : navRow == 2 ? "Deck and pile row"
                : navRow == 3 ? "Hand row" : "Phase row - choose a phase with D-pad";
    }

    private void activateArenaSelection() {
        if (navRow == 4) {
            selectedPhaseIndex = clamp(navColumn, 0, 5);
            activateSelectedPhase();
            return;
        }
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
            selectedMonster = navColumn;
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
            CardDefinition card = duel.getPlayerSpellTrap()[navColumn];
            if (card == null) hint = "Empty Spell/Trap Zone " + (navColumn + 1);
            else openDetails(card);
        } else if (navRow == 2) {
            if (navColumn == 0) {
                hint = "Main Deck is face-down. Its contents cannot be inspected freely.";
            } else if (navColumn >= 1 && navColumn <= 3) {
                browsePile = navColumn;
                selectedDeck = 0;
                deckBrowser = true;
            } else {
                hint = "Field Zone selected - card placement is not implemented yet";
            }
        } else {
            selectedHand = clamp(navColumn, 0, Math.max(0, duel.getPlayerHand().size() - 1));
            if (duel.getPhase() == DuelEngine.Phase.BATTLE) {
                hint = "Select a Monster Zone to attack";
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
            if (card.kind == CardDefinition.Kind.MONSTER) {
                if (selectedSummonAction == 0) {
                    if (card.level >= 5) duel.tributeSummonFromHand(selectedHand);
                    else duel.summonFromHand(selectedHand);
                } else if (selectedSummonAction == 1) {
                    if (card.level >= 5) duel.tributeSetFromHand(selectedHand);
                    else duel.setFromHand(selectedHand);
                } else if (selectedSummonAction == 2 && duel.canSpecialSummonFromHand(selectedHand)) {
                    duel.specialSummonFromHand(selectedHand);
                } else {
                    hint = "Special Summon condition is not met";
                }
            } else if (card.kind == CardDefinition.Kind.SPELL) {
                duel.activateSpellFromHand(selectedHand);
            } else {
                duel.setTrapFromHand(selectedHand);
            }
            selectedHand = clamp(selectedHand, 0, Math.max(0, duel.getPlayerHand().size() - 1));
            navColumn = selectedHand;
            hint = duel.getMessage();
        }
        summonMenu = false;
    }

    private List<CardDefinition> getBrowseCards() {
        List<CardDefinition> source;
        switch (browsePile) {
            case 1: source = duel.getPlayerExtraDeck(); break;
            case 2: source = duel.getPlayerGraveyard(); break;
            case 3: source = duel.getPlayerBanished(); break;
            default: source = duel.getPlayerDeck(); break;
        }
        if (browseSearch.length() == 0) return source;
        List<CardDefinition> matches = new ArrayList<CardDefinition>();
        String query = browseSearch.toLowerCase(java.util.Locale.ROOT);
        for (CardDefinition card : source) {
            if (card.name.toLowerCase(java.util.Locale.ROOT).contains(query)
                    || card.id.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                matches.add(card);
            }
        }
        return matches;
    }

    private void showCardSearch() {
        final EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setText(browseSearch);
        input.setHint("Nama atau ID kartu");
        new AlertDialog.Builder(getContext())
                .setTitle("Cari kartu - " + browseTitle())
                .setView(input)
                .setPositiveButton("Cari", (dialog, which) -> {
                    browseSearch = input.getText().toString().trim();
                    selectedDeck = 0;
                    hint = browseSearch.length() == 0 ? "Pencarian dibersihkan"
                            : "Hasil pencarian: " + getBrowseCards().size() + " kartu";
                    invalidate();
                })
                .setNegativeButton("Batal", (dialog, which) -> invalidate())
                .setNeutralButton("Reset", (dialog, which) -> {
                    browseSearch = "";
                    selectedDeck = 0;
                    hint = "Trunk dikembalikan ke tampilan normal";
                    invalidate();
                })
                .show();
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
        // Never use hidden ATK values to choose an attack target.
        for (int i = 0; i < enemy.length; i++) {
            if (enemy[i] != null && !duel.isCpuMonsterFaceDown(i)
                    && enemy[i].attack < weakest) {
                weakest = enemy[i].attack;
                target = i;
            }
        }
        // If every opposing monster is face-down, choose the first occupied slot
        // without inspecting its hidden stats.
        if (target < 0) {
            for (int i = 0; i < enemy.length; i++) {
                if (enemy[i] != null) {
                    target = i;
                    break;
                }
            }
        }
        duel.playerAttack(selectedMonster, target);
        hint = duel.getMessage();
        if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
            pauseMenu = true;
            selectedPauseOption = 0;
        }
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
        canvas.drawText("D-pad navigate/confirm | * Grid/List | # back", 14, h - 20, paint);

        if (deckBrowser) drawDeckBrowser(canvas, w, h);
        if (phaseMenu) drawPhaseMenu(canvas, w, h);
        if (detailCard != null) drawCardDetails(canvas, w, h);
        if (summonMenu) drawSummonMenu(canvas, w, h);
        if (pauseMenu) drawPauseMenu(canvas, w, h);
        if (opponentMenu) drawOpponentMenu(canvas, w, h);
        if (deckEditor) drawDeckEditor(canvas, w, h);
        if (titleScreen) drawTitleScreen(canvas, w, h);
    }



    private DuelEngine createConfiguredDuel() {
        return mainDeckDraft.isEmpty() ? new DuelEngine()
                : new DuelEngine(mainDeckDraft, extraDeckDraft);
    }

    private void resetArenaCursor() {
        selectedHand = 0;
        selectedMonster = 0;
        selectedDeck = 0;
        navRow = 3;
        navColumn = 0;
        choosingMonster = false;
        phaseMenu = false;
        summonMenu = false;
        deckBrowser = false;
        detailCard = null;
        detailScroll = 0;
        selectedPhaseIndex = 0;
    }

    private void drawPauseMenu(Canvas canvas, float w, float h) {
        String[] labels = duel.getPhase() == DuelEngine.Phase.FINISHED
                ? new String[]{"DUEL AGAIN", "CHOOSE OPPONENT", "MAIN SCREEN"}
                : new String[]{"RESUME", "DUEL AGAIN", "CHOOSE OPPONENT", "MAIN SCREEN"};
        float left = w * 0.13f, top = h * 0.30f, width = w * 0.74f;
        paint.setColor(0xf0142637);
        canvas.drawRoundRect(new RectF(left, top, left + width, top + 260), 12, 12, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffffd477);
        paint.setTextSize(20);
        canvas.drawText(duel.getPhase() == DuelEngine.Phase.FINISHED ? "DUEL FINISHED" : "PAUSE", w / 2f, top + 30, paint);
        for (int i = 0; i < labels.length; i++) {
            float y = top + 48 + i * 48;
            paint.setColor(selectedPauseOption == i ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(left + 12, y, left + width - 12, y + 38), 6, 6, paint);
            paint.setColor(selectedPauseOption == i ? 0xff172536 : 0xfff0f5f8);
            paint.setTextSize(14);
            canvas.drawText(labels[i], w / 2f, y + 25, paint);
        }
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(10);
        canvas.drawText("D-pad select | Center confirm | # close", w / 2f, top + 250, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawOpponentMenu(Canvas canvas, float w, float h) {
        float left = w * 0.13f, top = h * 0.36f, width = w * 0.74f;
        paint.setColor(0xf0142637);
        canvas.drawRoundRect(new RectF(left, top, left + width, top + 150), 12, 12, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffffd477);
        paint.setTextSize(18);
        canvas.drawText("CHOOSE OPPONENT", w / 2f, top + 32, paint);
        paint.setColor(0xffffc857);
        canvas.drawRoundRect(new RectF(left + 14, top + 52, left + width - 14, top + 98), 7, 7, paint);
        paint.setColor(0xff172536);
        paint.setTextSize(15);
        canvas.drawText("CPU OPPONENT", w / 2f, top + 80, paint);
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(10);
        canvas.drawText("Center start | # back", w / 2f, top + 128, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTitleScreen(Canvas canvas, float w, float h) {
        paint.setColor(0xff101b2b);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffffd477);
        paint.setTextSize(Math.max(25, w * 0.085f));
        canvas.drawText("POCKET DUEL", w / 2f, h * 0.30f, paint);
        String[] options = {"DUEL", "DECK BUILDER"};
        for (int i = 0; i < options.length; i++) {
            float top = h * 0.43f + i * 58;
            paint.setColor(titleOption == i ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(w * 0.18f, top, w * 0.82f, top + 42), 7, 7, paint);
            paint.setColor(titleOption == i ? 0xff172536 : 0xffe7edf2);
            paint.setTextSize(16);
            canvas.drawText(options[i], w / 2f, top + 27, paint);
        }
        paint.setTextSize(11);
        paint.setColor(0xffd9eaf4);
        canvas.drawText("D-pad choose | Center confirm", w / 2f, h * 0.72f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private List<CardDefinition> editorCards() {
        List<CardDefinition> cards = new ArrayList<CardDefinition>();
        for (CardDefinition card : CardCatalog.all()) {
            boolean extra = card.summonMethod == CardDefinition.SummonMethod.FUSION
                    || card.summonMethod == CardDefinition.SummonMethod.SYNCHRO
                    || card.summonMethod == CardDefinition.SummonMethod.XYZ
                    || card.summonMethod == CardDefinition.SummonMethod.LINK;
            if (extra == (editorTab == 1)) cards.add(card);
        }
        return cards;
    }

    private List<CardDefinition> editorDeck() {
        return editorTab == 0 ? mainDeckDraft : extraDeckDraft;
    }

    private void addEditorCard() {
        List<CardDefinition> cards = editorCards();
        if (editorCursor < 0 || editorCursor >= cards.size()) return;
        CardDefinition chosen = cards.get(editorCursor);
        editorDeck().add(chosen);
        hint = chosen.name + " added to deck";
        invalidate();
    }

    private void removeEditorCard() {
        List<CardDefinition> cards = editorCards();
        if (editorCursor < 0 || editorCursor >= cards.size()) return;
        CardDefinition chosen = cards.get(editorCursor);
        List<CardDefinition> deck = editorDeck();
        for (int i = deck.size() - 1; i >= 0; i--) {
            if (deck.get(i).id.equals(chosen.id)) {
                deck.remove(i);
                hint = chosen.name + " removed from deck";
                invalidate();
                return;
            }
        }
        hint = "That card is not in this deck";
        invalidate();
    }

    private void showDeckActions() {
        final String[] actions = {"Save Deck", "Load Deck", "Delete Deck"};
        new AlertDialog.Builder(getContext()).setTitle("Deck options")
                .setItems(actions, (dialog, which) -> {
                    SharedPreferences prefs = getContext().getSharedPreferences("pocket_duel_decks", Context.MODE_PRIVATE);
                    if (which == 0) {
                        prefs.edit().putString("main", encodeDeck(mainDeckDraft))
                                .putString("extra", encodeDeck(extraDeckDraft)).apply();
                        hint = "Deck saved";
                    } else if (which == 1) {
                        mainDeckDraft.clear();
                        extraDeckDraft.clear();
                        decodeDeck(prefs.getString("main", ""), mainDeckDraft);
                        decodeDeck(prefs.getString("extra", ""), extraDeckDraft);
                        hint = "Deck loaded";
                    } else {
                        prefs.edit().remove("main").remove("extra").apply();
                        mainDeckDraft.clear();
                        extraDeckDraft.clear();
                        hint = "Saved deck deleted";
                    }
                    invalidate();
                }).setNegativeButton("Cancel", null).show();
    }

    private String encodeDeck(List<CardDefinition> cards) {
        StringBuilder value = new StringBuilder();
        for (CardDefinition card : cards) {
            if (value.length() > 0) value.append(",");
            value.append(card.id);
        }
        return value.toString();
    }

    private void decodeDeck(String value, List<CardDefinition> target) {
        if (value == null || value.length() == 0) return;
        String[] ids = value.split(",");
        for (String id : ids) {
            CardDefinition card = CardCatalog.find(id);
            if (card != null) target.add(card);
        }
    }

    private void drawDeckEditor(Canvas canvas, float w, float h) {
        paint.setColor(0xff101b2b);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffffd477);
        paint.setTextSize(20);
        canvas.drawText("DECK BUILDER", w / 2f, 32, paint);
        paint.setTextSize(13);
        paint.setColor(editorTab == 0 ? 0xffffc857 : 0xffd9eaf4);
        canvas.drawText("MAIN (" + mainDeckDraft.size() + ")", w * 0.28f, 65, paint);
        paint.setColor(editorTab == 1 ? 0xffffc857 : 0xffd9eaf4);
        canvas.drawText("EXTRA (" + extraDeckDraft.size() + ")", w * 0.72f, 65, paint);
        List<CardDefinition> cards = editorCards();
        int first = Math.max(0, editorCursor - 7);
        int last = Math.min(cards.size(), first + 15);
        for (int i = first; i < last; i++) {
            CardDefinition card = cards.get(i);
            boolean selected = i == editorCursor;
            paint.setColor(selected ? 0xffffc857 : 0xff29465a);
            float y = 88 + (i - first) * 25;
            canvas.drawRoundRect(new RectF(12, y, w - 12, y + 22), 4, 4, paint);
            paint.setColor(selected ? 0xff172536 : 0xffe7edf2);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(card.name, 20, y + 16, paint);
            int copies = 0;
            for (CardDefinition item : editorDeck()) if (item.id.equals(card.id)) copies++;
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(copies > 0 ? "IN DECK" : "", w - 20, y + 16, paint);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(10);
        canvas.drawText("Up/Down select | Left/Right tab | Center add | Softkey Left remove", w / 2f, h - 54, paint);
        canvas.drawText("Softkey Right: Save/Load/Delete | # back", w / 2f, h - 34, paint);
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
        canvas.drawText("CARD ACTION", w / 2f, top + 25, paint);
        List<CardDefinition> hand = duel.getPlayerHand();
        CardDefinition selected = selectedHand >= 0 && selectedHand < hand.size()
                ? hand.get(selectedHand) : null;
        String[] labels = selected != null && selected.kind == CardDefinition.Kind.SPELL
                ? new String[]{"ACTIVATE SPELL"}
                : selected != null && selected.kind == CardDefinition.Kind.TRAP
                ? new String[]{"SET TRAP"}
                : duel.canSpecialSummonFromHand(selectedHand)
                ? new String[]{"SUMMON", "SET FACE-DOWN", "SPECIAL SUMMON"}
                : new String[]{"SUMMON", "SET FACE-DOWN"};
        for (int i = 0; i < labels.length; i++) {
            float width = (right - left - 20 - (labels.length - 1) * 5) / labels.length;
            float x = left + 10 + i * (width + 5);
            paint.setColor(selectedSummonAction == i ? 0xffffc857 : 0xff29465a);
            canvas.drawRoundRect(new RectF(x, top + 38, x + width, top + 72), 6, 6, paint);
            paint.setColor(selectedSummonAction == i ? 0xff172536 : 0xfff0f5f8);
            paint.setTextSize(Math.max(9, w * 0.028f));
            canvas.drawText(labels[i], x + width / 2f, top + 60, paint);
        }
        paint.setColor(0xffd9eaf4);
        paint.setTextSize(10);
        canvas.drawText("D-pad choose/confirm | # cancel", w / 2f, top + 94, paint);
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
        if (duel.getPhase() == DuelEngine.Phase.FINISHED) {
            pauseMenu = true;
            selectedPauseOption = 0;
        }
        phaseMenu = false;
        navRow = 4;
        navColumn = selectedPhaseIndex;
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
        canvas.drawText("D-pad move/confirm | # close", left + 12, top + cellH + 48, paint);
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
            boolean cursor = navRow == 4 && navColumn == i;
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
        float start = 7;
        float zoneW = (w - 14 - gap * 4) / 5f;
        float zoneH = getHeight() * 0.075f;
        float rowGap = getHeight() * 0.010f;
        float spellY = y + zoneH + rowGap;

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
            CardDefinition card = monsterRow && monsters != null ? monsters[i]
                    : !monsterRow && playerSide ? duel.getPlayerSpellTrap()[i] : null;
            boolean rowSelected = playerSide && navRow == (monsterRow ? 0 : 1)
                    && navColumn == i;
            paint.setColor(rowSelected ? 0xffffc857
                    : card == null ? (monsterRow ? 0xff31495b : 0xff29465a)
                    : 0xffbd9653);
            canvas.drawRoundRect(rect, 4, 4, paint);
            paint.setColor(card == null ? 0xffc6d5df : 0xff101b2b);
            paint.setTextSize(Math.max(7, getWidth() * 0.019f));
            boolean hiddenOpponentCard = monsterRow && (playerSide
                    ? duel.isPlayerMonsterFaceDown(i) : duel.isCpuMonsterFaceDown(i));
            String label = monsterRow ? (card == null ? "M" + (i + 1)
                    : hiddenOpponentCard ? "SET" : shortName(card.name, 7))
                    : card == null ? ((i == 0 || i == 4) ? "P/S-T " : "S/T ") + (i + 1)
                    : shortName(card.name, 7);
            canvas.drawText(label, x + 2, y + zoneH * 0.43f, paint);
            if (card != null && !hiddenOpponentCard) {
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
                    : i == 4 ? "ZONE" : String.valueOf(counts[i]);
            canvas.drawText(pileInfo, x + 3, y + pileH * 0.82f, paint);
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
        int selectedIndex = clamp(navColumn, 0, Math.max(0, hand.size() - 1));
        int firstVisible = Math.max(0, Math.min(selectedIndex - 2, hand.size() - 5));
        int lastVisible = Math.min(hand.size(), firstVisible + 5);
        for (int i = firstVisible; i < lastVisible; i++) {
            int visibleIndex = i - firstVisible;
            float x = 7 + visibleIndex * (w - 14 - cardW) / 4f;
            RectF rect = new RectF(x, h * 0.715f, x + cardW, h * 0.715f + cardH);
            boolean selected = navRow == 3 && navColumn == i;
            paint.setColor(selected ? 0xffffc857 : 0xff52758b);
            canvas.drawRoundRect(rect, 7, 7, paint);
            paint.setColor(selected ? 0xff172536 : 0xff101b2b);
            paint.setTextSize(Math.max(9, w * 0.024f));
            canvas.drawText(shortName(hand.get(i).name, 13), x + 4, rect.top + 18, paint);
            canvas.drawText("ATK " + hand.get(i).attack, x + 4, rect.top + 35, paint);
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
        if (gridView) {
            int columns = 3;
            float gap = 7;
            float cellW = (w - 64 - gap * (columns - 1)) / columns;
            float cellH = 62;
            int first = Math.max(0, (selectedDeck / 12) * 12);
            int last = Math.min(deck.size(), first + 12);
            for (int i = first; i < last; i++) {
                int local = i - first;
                int col = local % columns;
                int row = local / columns;
                float x = 26 + col * (cellW + gap);
                float y = 108 + row * (cellH + gap);
                paint.setColor(i == selectedDeck ? 0xffffd477 : 0xff29465a);
                canvas.drawRoundRect(new RectF(x, y, x + cellW, y + cellH), 6, 6, paint);
                paint.setColor(i == selectedDeck ? 0xff172536 : 0xffe7edf2);
                canvas.drawText((i + 1) + ". " + shortName(deck.get(i).name, 16), x + 5, y + 22, paint);
            }
        } else {
            int first = Math.max(0, selectedDeck - 7);
            int last = Math.min(deck.size(), first + 15);
            for (int i = first; i < last; i++) {
                paint.setColor(i == selectedDeck ? 0xffffd477 : 0xffe7edf2);
                canvas.drawText((i + 1) + ". " + deck.get(i).name, 26, 120 + (i - first) * 22, paint);
            }
        }
        paint.setColor(0xffd9eaf4);
        canvas.drawText("D-pad browse/inspect | * Grid/List | # close", 25, h - 108, paint);
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
        canvas.drawText("Up/Down scroll | Center/# close",
                24, h - 65, paint);
    }

    private String shortName(String name, int max) {
        return name.length() > max ? name.substring(0, max - 1) : name;
    }
}
