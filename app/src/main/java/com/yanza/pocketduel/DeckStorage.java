package com.yanza.pocketduel;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Reads the active deck saved by DeckBuilderView. */
public final class DeckStorage {
    private DeckStorage() { }

    public static List<String> loadMainDeck(Context context) {
        return load(context, false);
    }

    public static List<String> loadExtraDeck(Context context) {
        return load(context, true);
    }

    private static List<String> load(Context context, boolean extra) {
        SharedPreferences prefs = context.getSharedPreferences("pocket_duel_decks", Context.MODE_PRIVATE);
        String active = prefs.getString("active_deck", "Deck 1");
        String saved = prefs.getString("deck_" + active, "");
        String[] parts = saved.split(";", -1);
        List<String> ids = new ArrayList<String>();
        if (parts.length <= (extra ? 1 : 0)) return ids;
        String section = parts[extra ? 1 : 0];
        if (section.length() == 0) return ids;
        for (String id : section.split(",")) {
            CardDefinition card = CardCatalog.find(id);
            if (card == null || card.kind != CardDefinition.Kind.MONSTER) continue;
            boolean extraCard = card.summonMethod == CardDefinition.SummonMethod.FUSION
                    || card.summonMethod == CardDefinition.SummonMethod.SYNCHRO
                    || card.summonMethod == CardDefinition.SummonMethod.XYZ
                    || card.summonMethod == CardDefinition.SummonMethod.LINK;
            if (extraCard == extra) ids.add(id);
        }
        return ids;
    }
}
