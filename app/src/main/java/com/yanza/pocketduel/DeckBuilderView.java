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

/** Keypad-first card trunk and deck editor. */
public final class DeckBuilderView extends View {
    public interface Listener { void onBackToMenu(); }
    private static final int CARD_LIST=0, MAIN_DECK=1, EXTRA_DECK=2, DECK_MENU=3;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;
    private final List<CardDefinition> cards=CardCatalog.all();
    private final List<String> main=new ArrayList<String>(), extra=new ArrayList<String>();
    private final List<String> deckNames=new ArrayList<String>();
    private final SharedPreferences prefs;
    private int tab=CARD_LIST, selected=0, scroll=0, gridColumn=0;
    private boolean grid=true, detail=false;
    private String activeDeck="Deck 1", message="5 add/remove | 7 deck options";

    public DeckBuilderView(Context c, Listener l) {
        super(c); listener=l;
        prefs=c.getSharedPreferences("pocket_duel_decks",Context.MODE_PRIVATE);
        loadNames();
        if(deckNames.isEmpty()) {
            deckNames.add("Deck 1"); activeDeck="Deck 1";
            makeStarterDeck(); saveActiveDeck();
        } else {
            activeDeck=prefs.getString("active_deck",deckNames.get(0));
            if(!deckNames.contains(activeDeck)) activeDeck=deckNames.get(0);
            loadActiveDeck();
        }
        setFocusable(true); setFocusableInTouchMode(true);
    }

    private void loadNames() {
        String names=prefs.getString("deck_names","");
        for(String n:names.split("\\|")) if(n.length()>0) deckNames.add(n);
    }
    private void persistNames() {
        StringBuilder b=new StringBuilder();
        for(String n:deckNames) { if(b.length()>0)b.append('|'); b.append(n); }
        prefs.edit().putString("deck_names",b.toString()).putString("active_deck",activeDeck).apply();
    }
    private String key(String name) { return "deck_"+name; }
    private void loadActiveDeck() {
        main.clear(); extra.clear();
        String[] parts=prefs.getString(key(activeDeck),"").split(";");
        if(parts.length>0) for(String id:parts[0].split(",")) if(CardCatalog.find(id)!=null)main.add(id);
        if(parts.length>1) for(String id:parts[1].split(",")) if(CardCatalog.find(id)!=null)extra.add(id);
    }
    private void makeStarterDeck() {
        main.clear();
        for(CardDefinition c:cards) if(!isExtra(c)&&main.size()<40) main.add(c.id);
    }
    private void saveActiveDeck() {
        prefs.edit().putString(key(activeDeck),join(main)+";"+join(extra)).apply();
        persistNames();
    }
    private String join(List<String> a) {
        StringBuilder b=new StringBuilder();
        for(String s:a){if(b.length()>0)b.append(',');b.append(s);}
        return b.toString();
    }
    private boolean isExtra(CardDefinition c) {
        return c.summonMethod==CardDefinition.SummonMethod.FUSION ||
          c.summonMethod==CardDefinition.SummonMethod.SYNCHRO ||
          c.summonMethod==CardDefinition.SummonMethod.XYZ ||
          c.summonMethod==CardDefinition.SummonMethod.LINK;
    }
    private int copies(String id) { int n=0; for(String s:main)if(s.equals(id))n++;return n; }

    public boolean handleKey(int k) {
        if(k==KeyEvent.KEYCODE_0 || k==KeyEvent.KEYCODE_BACK) {
            if(tab==DECK_MENU){tab=CARD_LIST;selected=0;message="Deck: "+activeDeck;}
            else { saveActiveDeck(); if(listener!=null)listener.onBackToMenu(); }
        } else if(tab==DECK_MENU) {
            if(k==KeyEvent.KEYCODE_2||k==KeyEvent.KEYCODE_DPAD_UP)selected=Math.max(0,selected-1);
            else if(k==KeyEvent.KEYCODE_8||k==KeyEvent.KEYCODE_DPAD_DOWN)selected=Math.min(3,selected+1);
            else if(k==KeyEvent.KEYCODE_5||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_DPAD_CENTER)deckAction();
            else return false;
        } else if(k==KeyEvent.KEYCODE_1) changeTab(-1);
        else if(k==KeyEvent.KEYCODE_3) changeTab(1);
        else if(k==KeyEvent.KEYCODE_STAR) { grid=!grid;detail=false;selected=0;scroll=0;message=grid?"Grid view":"List view"; }
        else if(k==KeyEvent.KEYCODE_POUND) { detail=!detail;message=detail?"Card details":"Card overview"; }
        else if(k==KeyEvent.KEYCODE_7) { tab=DECK_MENU;selected=0;message="Manage saved decks"; }
        else if(k==KeyEvent.KEYCODE_2||k==KeyEvent.KEYCODE_DPAD_UP) moveVertical(-1);
        else if(k==KeyEvent.KEYCODE_8||k==KeyEvent.KEYCODE_DPAD_DOWN) moveVertical(1);
        else if(k==KeyEvent.KEYCODE_4||k==KeyEvent.KEYCODE_DPAD_LEFT) moveHorizontal(-1);
        else if(k==KeyEvent.KEYCODE_6||k==KeyEvent.KEYCODE_DPAD_RIGHT) moveHorizontal(1);
        else if(k==KeyEvent.KEYCODE_5||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_DPAD_CENTER) activate();
        else return false;
        invalidate(); return true;
    }

    private void changeTab(int d) {
        tab=(tab+d+3)%3;selected=0;scroll=0;gridColumn=0;detail=false;
        message=tab==CARD_LIST?"Card Trunk":tab==MAIN_DECK?"Main Deck":"Extra Deck";
    }
    private List<CardDefinition> items() {
        List<CardDefinition> out=new ArrayList<CardDefinition>();
        if(tab==CARD_LIST) { for(CardDefinition c:cards)if(!isExtra(c))out.add(c); }
        else { List<String> ids=tab==MAIN_DECK?main:extra;for(String id:ids){CardDefinition c=CardCatalog.find(id);if(c!=null)out.add(c);} }
        return out;
    }
    private void moveVertical(int d) {
        int cols=grid?3:1, size=items().size();
        selected=Math.max(0,Math.min(Math.max(0,size-1),selected+d*cols));
        gridColumn=selected%cols;keepVisible();
    }
    private void moveHorizontal(int d) {
        if(!grid) { selected=Math.max(0,Math.min(Math.max(0,items().size()-1),selected+d)); }
        else {
            int row=selected/3, col=Math.max(0,Math.min(2,selected%3+d));
            int next=row*3+col; if(next<items().size())selected=next;
        }
        keepVisible();
    }
    private void keepVisible() {
        int rows=grid?4:7, row=selected/(grid?3:1);
        if(row<scroll)scroll=row;
        if(row>=scroll+rows)scroll=row-rows+1;
    }
    private void activate() {
        List<CardDefinition> list=items();
        if(selected>=list.size()){message="No card selected";return;}
        CardDefinition c=list.get(selected);
        if(tab==CARD_LIST) {
            if(copies(c.id)>=3)message="Maximum 3 copies";
            else if(main.size()>=60)message="Main Deck full (60)";
            else {main.add(c.id);saveActiveDeck();message=c.name+" added";}
        } else {
            List<String> target=tab==MAIN_DECK?main:extra;
            target.remove(c.id);selected=Math.max(0,Math.min(selected,target.size()-1));
            saveActiveDeck();message=c.name+" removed";
        }
    }
    private void deckAction() {
        if(selected==0) {
            int n=1;String name;
            do{name="Deck "+n++;}while(deckNames.contains(name));
            deckNames.add(name);activeDeck=name;main.clear();extra.clear();
            saveActiveDeck();message="Created "+name;
        } else if(selected==1) {
            saveActiveDeck();message="Saved "+activeDeck;
        } else if(selected==2) {
            if(deckNames.isEmpty()){message="No saved deck";return;}
            int i=deckNames.indexOf(activeDeck);activeDeck=deckNames.get((i+1)%deckNames.size());
            loadActiveDeck();message="Loaded "+activeDeck;
        } else {
            if(deckNames.size()<=1){message="Keep at least one deck";return;}
            prefs.edit().remove(key(activeDeck)).apply();deckNames.remove(activeDeck);
            activeDeck=deckNames.get(0);loadActiveDeck();persistNames();message="Deleted deck";
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);float w=getWidth(),h=getHeight();
        c.drawColor(0xFF101923);p.setColor(0xFF172B3A);
        c.drawRoundRect(new RectF(w*.025f,h*.025f,w*.975f,h*.975f),16,16,p);
        p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFFFD36A);p.setFakeBoldText(true);
        p.setTextSize(Math.max(18,w*.06f));c.drawText(tab==DECK_MENU?"DECK OPTIONS":"DECK BUILDER",w/2,h*.105f,p);p.setFakeBoldText(false);
        if(tab==DECK_MENU) {
            String[] opts={"NEW DECK","SAVE DECK","LOAD NEXT DECK","DELETE DECK"};
            for(int i=0;i<4;i++)drawRow(c,w,h,i,opts[i],selected==i);
            footer(c,w,h,"2/8 SELECT   5 CONFIRM   0 BACK");
            return;
        }
        String[] tabs={"TRUNK","MAIN","EXTRA"};
        p.setTextSize(Math.max(10,w*.027f));
        for(int i=0;i<3;i++){float x=w*.035f+i*w*.31f;p.setColor(tab==i?0xFFB87832:0xFF294354);
            c.drawRoundRect(new RectF(x,h*.15f,x+w*.29f,h*.22f),7,7,p);p.setColor(-1);c.drawText(tabs[i],x+w*.145f,h*.198f,p);}
        p.setTextAlign(Paint.Align.LEFT);p.setColor(0xFFB8C9D6);p.setTextSize(Math.max(10,w*.027f));
        c.drawText(activeDeck+"   Main "+main.size()+"/60   Extra "+extra.size()+"/15",w*.06f,h*.27f,p);
        List<CardDefinition> list=items();
        if(detail) {
            if(selected<list.size())drawDetail(c,w,h,list.get(selected));
        } else if(grid) {
            int start=scroll*3;
            for(int i=0;i<12;i++){int ix=start+i;if(ix>=list.size())break;int col=i%3,row=i/3;
                float cw=w*.275f,left=w*.055f+col*w*.30f,top=h*.31f+row*h*.135f;
                p.setColor(ix==selected?0xFFB87832:0xFF294354);
                c.drawRoundRect(new RectF(left,top,left+cw,top+h*.12f),7,7,p);
                p.setColor(-1);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(9,w*.025f));
                String name=list.get(ix).name; if(name.length()>15)name=name.substring(0,14)+"…";
                c.drawText(name,left+cw/2,top+h*.05f,p);
                p.setTextSize(Math.max(8,w*.021f));c.drawText(list.get(ix).kind==CardDefinition.Kind.MONSTER?
                    list.get(ix).attack+"/"+list.get(ix).defense:list.get(ix).kind.name(),left+cw/2,top+h*.09f,p);
            }
        } else {
            for(int i=0;i<7;i++){int ix=scroll+i;if(ix>=list.size())break;CardDefinition card=list.get(ix);
                drawRow(c,w,h,i,card.name+(card.kind==CardDefinition.Kind.MONSTER?"  "+card.attack+"/"+card.defense:"  "+card.kind.name()),ix==selected);}
        }
        footer(c,w,h,"2/8/4/6 CURSOR   1/3 TAB   * GRID/LIST   # DETAILS   5 SELECT   7 DECKS   0 BACK");
    }
    private void drawRow(Canvas c,float w,float h,int i,String s,boolean chosen) {
        float top=h*.30f+i*h*.075f;p.setColor(chosen?0xFFB87832:0xFF294354);
        c.drawRoundRect(new RectF(w*.07f,top,w*.93f,top+h*.065f),7,7,p);
        p.setColor(-1);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(11,w*.032f));
        c.drawText(s,w/2,top+h*.043f,p);
    }
    private void drawDetail(Canvas c,float w,float h,CardDefinition d) {
        p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFFFD36A);p.setTextSize(Math.max(16,w*.05f));c.drawText(d.name,w/2,h*.43f,p);
        p.setColor(-1);p.setTextSize(Math.max(12,w*.032f));
        c.drawText(d.kind==CardDefinition.Kind.MONSTER?"Level "+d.level+"  ATK "+d.attack+"  DEF "+d.defense:d.kind.name(),w/2,h*.53f,p);
        if(d.monsterType!=null)c.drawText(d.monsterType.name()+" / "+d.attribute.name(),w/2,h*.61f,p);
        if(d.isPendulumMonster())c.drawText("Pendulum Scale "+d.pendulumScale,w/2,h*.69f,p);
    }
    private void footer(Canvas c,float w,float h,String s) {
        p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFB8C9D6);p.setTextSize(Math.max(8,w*.021f));c.drawText(message,w/2,h*.88f,p);c.drawText(s,w/2,h*.94f,p);
    }
}
