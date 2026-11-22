package com.yanza.pocketduel;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private DuelView duelView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        duelView = new DuelView(this);
        setContentView(duelView);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (duelView != null && duelView.handleKey(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
