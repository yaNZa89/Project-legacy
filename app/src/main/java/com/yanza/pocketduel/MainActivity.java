package com.yanza.pocketduel;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private DuelView duelView;
    private MainMenuView mainMenuView;
    private DeckBuilderView deckBuilderView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        showMainMenu();
    }

    private void showMainMenu() {
        duelView = null;
        deckBuilderView = null;
        mainMenuView = new MainMenuView(this, new MainMenuView.Listener() {
            @Override public void onStartDuel(String opponent) { startDuel(opponent); }
            @Override public void onOpenDeck() { showDeckBuilder(); }
        });
        setContentView(mainMenuView);
        mainMenuView.requestFocus();
    }

    private void showDeckBuilder() {
        mainMenuView = null;
        deckBuilderView = new DeckBuilderView(this, new DeckBuilderView.Listener() {
            @Override public void onBackToMenu() { showMainMenu(); }
        });
        setContentView(deckBuilderView);
        deckBuilderView.requestFocus();
    }

    private void startDuel(String opponent) {
        // CPU profiles and the selected saved deck are connected in a later rules pass.
        duelView = new DuelView(this);
        mainMenuView = null;
        deckBuilderView = null;
        setContentView(duelView);
        duelView.requestFocus();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mainMenuView != null && mainMenuView.handleKey(keyCode)) return true;
        if (deckBuilderView != null && deckBuilderView.handleKey(keyCode)) return true;
        if (duelView != null && duelView.handleKey(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
