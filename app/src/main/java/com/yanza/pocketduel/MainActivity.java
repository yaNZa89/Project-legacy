package com.yanza.pocketduel;

import android.app.Activity;
import android.widget.Toast;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private DuelView duelView;
    private MainMenuView mainMenuView;
    private DeckBuilderView deckBuilderView;
    private String currentOpponent = "AI Duelist 01";

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

    private void showOpponentSelection() {
        duelView = null;
        deckBuilderView = null;
        mainMenuView = new MainMenuView(this, new MainMenuView.Listener() {
            @Override public void onStartDuel(String opponent) { startDuel(opponent); }
            @Override public void onOpenDeck() { showDeckBuilder(); }
        });
        mainMenuView.openOpponentSelection();
        setContentView(mainMenuView);
        mainMenuView.requestFocus();
    }

    private void startDuel(String opponent) {
        java.util.List<String> mainDeck = DeckStorage.loadMainDeck(this);
        java.util.List<String> extraDeck = DeckStorage.loadExtraDeck(this);
        if (mainDeck.size() < 40 || mainDeck.size() > 60) {
            Toast.makeText(this, "Main Deck harus berisi 40-60 kartu. Periksa Deck Builder.", Toast.LENGTH_LONG).show();
            showDeckBuilder();
            return;
        }
        currentOpponent = opponent;
        duelView = new DuelView(this, new DuelView.Listener() {
            @Override public void onExitToMenu() { showMainMenu(); }
            @Override public void onDuelAgain() { showOpponentSelection(); }
        }, mainDeck, extraDeck);
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
