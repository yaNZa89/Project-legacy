package com.yanza.pocketduel;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Compact keypad deck editor. Deck lists are saved locally by card ID. */
public final class DeckBuilderView extends View {
    public interface Listener { void onBackToMenu(); }

    private static final int CARD_LIST = 0;
    private static final int MAIN_DECK = 1;
    private static final int EXTRA_DECK = 2;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;
    private final List<CardDefinition> cards = CardCatalog.all();
    private final List<String> mainDeck = new ArrayList<String>();
    private final List<String> extraDeck = new ArrayList<String>();
    private final SharedPreferences prefs;
    private int tab = CARD_LIST;
    private int selected;
    private int scroll;
    private String message = "5 add/remove | 4/6 change tab | # back";

    public DeckBuilderView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        prefs = context.getSharedPreferences("pocket_duel_decks", Context.MODE_PRIVATE);
        loadDecks();
        if (mainDeck.isEmpty()) createStarterDeck();
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void loadDecks() {
        String main = prefs.getString("main", "");
        String extra = prefs.getString("extra", "");
        for (String id : main.split(",")) if (CardCatalog.find(id) != null) mainDeck.add(id);
        for (String id : extra.split(",")) if (CardCatalog.find(id) != null) extraDeck.add(id);
    }

    private void createStarterDeck() {
        // A playable prototype starting point: 40 different catalog cards.
        for (int i = 0; i < cards.size() && mainDeck.size() < 40; i++) {
            CardDefinition card = cards.get(i);
            if (!isExtraCard(card)) mainDeck.add(card.id);
        }
        saveDecks();
    }

    public boolean handleKey(int key) {
        if (key == KeyEvent.KEYCODE_2 || key == KeyEvent.KEYCODE_DPAD_UP) {
            selected = Math.max(0, selected - 1);
            keepSelectionVisible();
        } else if (key == KeyEvent.KEYCODE_8 || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            selected = Math.min(Math.max(0, currentItems().size() - 1), selected + 1);
            keepSelectionVisible();
        } else if (key == KeyEvent.KEYCODE_4 || key == KeyEvent.KEYCODE_DPAD_LEFT) {
            changeTab(-1);
        } else if (key == KeyEvent.KEYCODE_6 || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            changeTab(1);
        } else if (key == KeyEvent.KEYCODE_5 || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_DPAD_CENTER) {
            activateSelection();
        } else if (key == KeyEvent.KEYCODE_POUND || key == KeyEvent.KEYCODE_BACK) {
            saveDecks();
            if (listener != null) listener.onBackToMenu();
        } else {
            return false;
        }
        invalidate();
        return true;
    }

    private void changeTab(int direction) {
        tab = (tab + direction + 3) % 3;
        selected = 0;
        scroll = 0;
        message = tab == CARD_LIST ? "Select a card, press 5 to add"
                : tab == MAIN_DECK ? "Main Deck: select a card, press 5 to remove"
                : "Extra Deck: select a card, press 5 to remove";
    }

    private List<CardDefinition> currentItems() {
        List<CardDefinition> result = new ArrayList<CardDefinition>();
        if (tab == CARD_LIST) {
            for (CardDefinition card : cards) if (!isExtraCard(card)) result.add(card);
        } else {
            List<String> ids = tab == MAIN_DECK ? mainDeck : extraDeck;
            for (String id : ids) {
                CardDefinition card = CardCatalog.find(id);
                if (card != null) result.add(card);
            }
        }
        return result;
    }

    private void activateSelection() {
        List<CardDefinition> items = currentItems();
        if (selected >= items.size()) {
            message = "No card selected";
            return;
        }
        CardDefinition card = items.get(selected);
        if (tab == CARD_LIST) {
            int copies = count(mainDeck, card.id);
            if (copies >= 3) message = "Maximum 3 copies of this card";
            else if (mainDeck.size() >= 60) message = "Main Deck is full (60)";
            else {
                mainDeck.add(card.id);
                message = card.name + " added to Main Deck";
                saveDecks();
            }
        } else {
            List<String> target = tab == MAIN_DECK ? mainDeck : extraDeck;
            target.remove(card.id);
            selected = Math.max(0, Math.min(selected, target.size() - 1));
            message = card.name + " removed";
            saveDecks();
        }
    }

    private int count(List<String> ids, String id) {
        int n = 0;
        for (String value : ids) if (value.equals(id)) n++;
        return n;
    }

    private boolean isExtraCard(CardDefinition card) {
        return card.summonMethod == CardDefinition.SummonMethod.FUSION
                || card.summonMethod == CardDefinition.SummonMethod.SYNCHRO
                || card.summonMethod == CardDefinition.SummonMethod.XYZ
                || card.summonMethod == CardDefinition.SummonMethod.LINK;
    }

    private void keepSelectionVisible() {
        int visible = 7;
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + visible) scroll = selected - visible + 1;
    }

    private void saveDecks() {
        prefs.edit().putString("main", join(mainDeck))
                .putString("extra", join(extraDeck)).apply();
    }

    private String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(',');
            out.append(value);
        }
        return out.toString();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        canvas.drawColor(0xFF101923);
        paint.setColor(0xFF172B3A);
        canvas.drawRoundRect(new RectF(w*.035f,h*.035f,w*.965f,h*.965f),18,18,paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFFFFD36A);
        paint.setTextSize(Math.max(20f,w*.065f));
        paint.setFakeBoldText(true);
        canvas.drawText("DECK BUILDER",w/2f,h*.12f,paint);
        paint.setFakeBoldText(false);

        String[] tabs = {"CARD LIST","MAIN DECK","EXTRA DECK"};
        float tabW = w*.29f;
        for(int i=0;i<3;i++) {
            float left = w*.045f + i*tabW;
            paint.setColor(tab==i?0xFFB87832:0xFF294354);
            canvas.drawRoundRect(new RectF(left,h*.17f,left+tabW-5,h*.25f),8,8,paint);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(Math.max(10f,w*.027f));
            canvas.drawText(tabs[i],left+(tabW-5)/2f,h*.222f,paint);
        }

        List<CardDefinition> items=currentItems();
        int mainCount=mainDeck.size();
        String countText=tab==CARD_LIST?"Main: "+mainCount+"/60"
                :tab==MAIN_DECK?"Main Deck: "+mainCount+" cards"
                :"Extra Deck: "+extraDeck.size()+"/15";
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xFFB8C9D6);
        paint.setTextSize(Math.max(11f,w*.03f));
        canvas.drawText(countText,w*.07f,h*.31f,paint);

        int visible=7;
        float rowH=h*.071f;
        for(int row=0;row<visible;row++) {
            int index=scroll+row;
            if(index>=items.size()) break;
            CardDefinition card=items.get(index);
            float top=h*.35f+row*rowH;
            paint.setColor(index==selected?0xFFB87832:0xFF294354);
            canvas.drawRoundRect(new RectF(w*.065f,top,w*.935f,top+rowH-4),7,7,paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(Math.max(11f,w*.034f));
            canvas.drawText(card.name,w*.09f,top+rowH*.47f,paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(0xFFCFDCE5);
            paint.setTextSize(Math.max(9f,w*.026f));
            String detail=card.kind==CardDefinition.Kind.MONSTER
                    ? "Lv "+card.level+"  "+card.attack+"/"+card.defense : card.kind.name();
            canvas.drawText(detail,w*.90f,top+rowH*.47f,paint);
            if(tab==CARD_LIST) {
                paint.setColor(0xFFFFD36A);
                paint.setTextSize(Math.max(9f,w*.024f));
                canvas.drawText("x"+count(mainDeck,card.id),w*.90f,top+rowH*.82f,paint);
            }
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFFB8C9D6);
        paint.setTextSize(Math.max(9f,w*.026f));
        canvas.drawText(message,w/2f,h*.91f,paint);
        canvas.drawText("2/8 MOVE   4/6 TAB   5 ADD/REMOVE   # SAVE & BACK",w/2f,h*.95f,paint);
    }
}
