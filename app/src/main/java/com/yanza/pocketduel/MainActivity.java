package com.yanza.pocketduel;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private DuelView duelView;
    private MainMenuView mainMenuView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        showMainMenu();
    }

    private void showMainMenu() {
        duelView = null;
        mainMenuView = new MainMenuView(this, new MainMenuView.Listener() {
            @Override
            public void onStartDuel(String opponent) {
                startDuel(opponent);
            }
        });
        setContentView(mainMenuView);
        mainMenuView.requestFocus();
    }

    private void startDuel(String opponent) {
        // The selected opponent is shown in the menu; AI deck profiles will be
        // connected when the deck-selection and CPU profile systems are added.
        duelView = new DuelView(this);
        mainMenuView = null;
        setContentView(duelView);
        duelView.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mainMenuView != null && mainMenuView.handleKey(keyCode)) return true;
        if (duelView != null && duelView.handleKey(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
