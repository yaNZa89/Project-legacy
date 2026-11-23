package com.yanza.pocketduel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;

/** Keypad-first entry screen for Pocket Duel. */
public final class MainMenuView extends View {
    public interface Listener {
        void onStartDuel(String opponent);
    }

    private enum Screen { MAIN, OPPONENT, DECK, OPTIONS }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;
    private final String[] menu = { "DUEL", "DECK", "OPTIONS" };
    private final String[] opponents = { "AI Duelist 01", "AI Duelist 02", "AI Duelist 03" };
    private Screen screen = Screen.MAIN;
    private int selected;
    private String notice = "Choose an option and press 5";

    public MainMenuView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public boolean handleKey(int key) {
        if (key == KeyEvent.KEYCODE_2 || key == KeyEvent.KEYCODE_DPAD_UP) {
            selected = Math.max(0, selected - 1);
        } else if (key == KeyEvent.KEYCODE_8 || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            int count = screen == Screen.MAIN ? menu.length
                    : screen == Screen.OPPONENT ? opponents.length : 1;
            selected = Math.min(count - 1, selected + 1);
        } else if (key == KeyEvent.KEYCODE_5 || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_DPAD_CENTER) {
            activate();
        } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
            if (screen != Screen.MAIN) {
                screen = Screen.MAIN;
                selected = 0;
                notice = "Choose an option and press 5";
            }
        } else {
            return false;
        }
        invalidate();
        return true;
    }

    private void activate() {
        if (screen == Screen.MAIN) {
            if (selected == 0) {
                screen = Screen.OPPONENT;
                selected = 0;
                notice = "Choose your AI opponent";
            } else if (selected == 1) {
                screen = Screen.DECK;
                selected = 0;
                notice = "Deck builder is the next implementation step";
            } else {
                screen = Screen.OPTIONS;
                selected = 0;
                notice = "Keypad controls";
            }
        } else if (screen == Screen.OPPONENT) {
            if (listener != null) listener.onStartDuel(opponents[selected]);
        } else if (screen == Screen.DECK) {
            notice = "Deck editing is not connected yet";
        } else {
            notice = "2/8 move | 5 select | # back";
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawColor(0xFF101923);
        paint.setColor(0xFF172B3A);
        canvas.drawRoundRect(new RectF(w * .06f, h * .08f, w * .94f, h * .92f),
                22f, 22f, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFFFFD36A);
        paint.setTextSize(Math.max(24f, w * .085f));
        paint.setFakeBoldText(true);
        canvas.drawText("POCKET DUEL", w / 2f, h * .20f, paint);
        paint.setFakeBoldText(false);
        paint.setColor(0xFFB8C9D6);
        paint.setTextSize(Math.max(11f, w * .031f));
        canvas.drawText("CLASSIC CARD BATTLE", w / 2f, h * .25f, paint);

        String heading = screen == Screen.MAIN ? "MAIN MENU"
                : screen == Screen.OPPONENT ? "SELECT AI OPPONENT"
                : screen == Screen.DECK ? "DECK" : "OPTIONS";
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(Math.max(15f, w * .047f));
        canvas.drawText(heading, w / 2f, h * .37f, paint);

        String[] entries = screen == Screen.MAIN ? menu
                : screen == Screen.OPPONENT ? opponents
                : screen == Screen.DECK ? new String[] { "Deck builder coming next" }
                : new String[] { "Keypad: 2/8 move, 5 select", "Back: #" };
        float itemH = Math.min(h * .105f, 58f);
        float startY = h * .43f;
        for (int i = 0; i < entries.length; i++) {
            float top = startY + i * (itemH + 10f);
            RectF rect = new RectF(w * .16f, top, w * .84f, top + itemH);
            paint.setColor(i == selected ? 0xFFB87832 : 0xFF294354);
            canvas.drawRoundRect(rect, 12f, 12f, paint);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(Math.max(13f, w * .043f));
            canvas.drawText(entries[i], w / 2f, top + itemH * .64f, paint);
        }

        paint.setColor(0xFFB8C9D6);
        paint.setTextSize(Math.max(10f, w * .029f));
        canvas.drawText(notice, w / 2f, h * .84f, paint);
        canvas.drawText("2/8 NAVIGATE     5 CONFIRM     # BACK", w / 2f, h * .89f, paint);
    }
}
