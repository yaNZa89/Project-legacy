package com.yanza.pocketduel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Original test cards used to exercise the first summon rules. */
public final class CardCatalog {
    private static final List<CardDefinition> CARDS = new ArrayList<CardDefinition>();
    static {
        add("ember_scout", "Ember Scout", 3, 1200, 800, "");
        add("stone_guardian", "Stone Guardian", 4, 900, 1500, "");
        add("sky_striker", "Sky Striker", 4, 1500, 1000, "");
        add("forest_wisp", "Forest Wisp", 2, 700, 1100, "");
        add("iron_cub", "Iron Cub", 3, 1000, 1000, "");
        add("spark_mage", "Spark Mage", 4, 1300, 1000, "DRAW_ONE_ON_SUMMON");
        add("tide_sentinel", "Tide Sentinel", 4, 1000, 1600, "");
        add("rune_apprentice", "Rune Apprentice", 2, 500, 1200, "GAIN_300_ATK");
        add("flame_knight", "Flame Knight", 5, 1800, 1400, "");
        add("ancient_colossus", "Ancient Colossus", 7, 2400, 2100, "");
    }
    private CardCatalog() { }
    private static void add(String id, String name, int level, int atk, int def, String effect) {
        CARDS.add(new CardDefinition(id, name, CardDefinition.Kind.MONSTER,
                CardDefinition.SummonMethod.NORMAL, level, atk, def, effect));
    }
    public static List<CardDefinition> all() { return Collections.unmodifiableList(CARDS); }
    public static CardDefinition find(String id) {
        for (CardDefinition card : CARDS) if (card.id.equals(id)) return card;
        return null;
    }
}
