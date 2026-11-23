package com.yanza.pocketduel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Original starter roster. Every monster has an explicit Type and Attribute. */
public final class CardCatalog {
    private static final List<CardDefinition> CARDS = new ArrayList<CardDefinition>();
    static {
        add("ember_scout", "Ember Scout", CardDefinition.MonsterType.WARRIOR, CardDefinition.Attribute.FIRE, 3, 1200, 800, "");
        add("stone_guardian", "Stone Guardian", CardDefinition.MonsterType.ROCK, CardDefinition.Attribute.EARTH, 4, 900, 1500, "");
        add("sky_striker", "Sky Striker", CardDefinition.MonsterType.WARRIOR, CardDefinition.Attribute.WIND, 4, 1500, 1000, "");
        add("forest_wisp", "Forest Wisp", CardDefinition.MonsterType.PLANT, CardDefinition.Attribute.EARTH, 2, 700, 1100, "");
        add("iron_cub", "Iron Cub", CardDefinition.MonsterType.MACHINE, CardDefinition.Attribute.EARTH, 3, 1000, 1000, "");
        add("spark_mage", "Spark Mage", CardDefinition.MonsterType.SPELLCASTER, CardDefinition.Attribute.LIGHT, 4, 1300, 1000, "DRAW_ONE_ON_SUMMON");
        add("tide_sentinel", "Tide Sentinel", CardDefinition.MonsterType.AQUA, CardDefinition.Attribute.WATER, 4, 1000, 1600, "");
        add("rune_apprentice", "Rune Apprentice", CardDefinition.MonsterType.SPELLCASTER, CardDefinition.Attribute.DARK, 2, 500, 1200, "");
        add("flame_knight", "Flame Knight", CardDefinition.MonsterType.WARRIOR, CardDefinition.Attribute.FIRE, 5, 1800, 1400, "");
        add("ancient_colossus", "Ancient Colossus", CardDefinition.MonsterType.ROCK, CardDefinition.Attribute.EARTH, 7, 2400, 2100, "");

        add("dusk_drake", "Dusk Drake", CardDefinition.MonsterType.DRAGON, CardDefinition.Attribute.DARK, 4, 1600, 1200, "");
        add("radiant_wyvern", "Radiant Wyvern", CardDefinition.MonsterType.DRAGON, CardDefinition.Attribute.LIGHT, 6, 2100, 1700, "");
        add("moonlit_sage", "Moonlit Sage", CardDefinition.MonsterType.SPELLCASTER, CardDefinition.Attribute.DARK, 3, 1100, 1300, "");
        add("thunder_hawk", "Thunder Hawk", CardDefinition.MonsterType.WINGED_BEAST, CardDefinition.Attribute.LIGHT, 4, 1500, 900, "");
        add("abyss_stalker", "Abyss Stalker", CardDefinition.MonsterType.FIEND, CardDefinition.Attribute.DARK, 4, 1700, 800, "");
        add("sunpetal_fairy", "Sunpetal Fairy", CardDefinition.MonsterType.FAIRY, CardDefinition.Attribute.LIGHT, 3, 1000, 1400, "");
        add("mossback_beast", "Mossback Beast", CardDefinition.MonsterType.BEAST, CardDefinition.Attribute.EARTH, 4, 1400, 1600, "");
        add("tidecaller_serpent", "Tidecaller Serpent", CardDefinition.MonsterType.SEA_SERPENT, CardDefinition.Attribute.WATER, 5, 1700, 1900, "SPECIAL_SUMMON_IF_OPPONENT_HAS_MONSTER");
        add("cinder_imp", "Cinder Imp", CardDefinition.MonsterType.FIEND, CardDefinition.Attribute.FIRE, 2, 800, 500, "SPECIAL_SUMMON_IF_NO_MONSTERS");
        add("frost_mech", "Frost Mech", CardDefinition.MonsterType.MACHINE, CardDefinition.Attribute.WATER, 4, 1300, 1800, "");
        add("volt_fox", "Volt Fox", CardDefinition.MonsterType.THUNDER, CardDefinition.Attribute.LIGHT, 3, 1200, 700, "");
        add("grave_moth", "Grave Moth", CardDefinition.MonsterType.INSECT, CardDefinition.Attribute.DARK, 3, 1000, 1000, "");
        add("coral_keeper", "Coral Keeper", CardDefinition.MonsterType.AQUA, CardDefinition.Attribute.WATER, 3, 900, 1500, "");
        add("quartz_golem", "Quartz Golem", CardDefinition.MonsterType.ROCK, CardDefinition.Attribute.EARTH, 5, 1900, 2000, "");
        add("ember_lizard", "Ember Lizard", CardDefinition.MonsterType.REPTILE, CardDefinition.Attribute.FIRE, 4, 1500, 1200, "");
        add("windrider_archer", "Windrider Archer", CardDefinition.MonsterType.WARRIOR, CardDefinition.Attribute.WIND, 4, 1600, 1000, "");
        add("clockwork_guard", "Clockwork Guard", CardDefinition.MonsterType.MACHINE, CardDefinition.Attribute.LIGHT, 5, 1800, 1800, "");
        add("nightshade_bloom", "Nightshade Bloom", CardDefinition.MonsterType.PLANT, CardDefinition.Attribute.DARK, 4, 1400, 1500, "");
        add("stormfang_wolf", "Stormfang Wolf", CardDefinition.MonsterType.BEAST, CardDefinition.Attribute.WIND, 4, 1700, 900, "");
        add("deepsea_raider", "Deepsea Raider", CardDefinition.MonsterType.FISH, CardDefinition.Attribute.WATER, 4, 1500, 1300, "");
        add("sunforge_paladin", "Sunforge Paladin", CardDefinition.MonsterType.WARRIOR, CardDefinition.Attribute.LIGHT, 6, 2000, 1600, "");
        add("obsidian_titan", "Obsidian Titan", CardDefinition.MonsterType.ROCK, CardDefinition.Attribute.DARK, 7, 2300, 2500, "");
        add("ancient_fang", "Ancient Fang", CardDefinition.MonsterType.DINOSAUR, CardDefinition.Attribute.EARTH, 6, 2100, 1500, "");
        add("phantom_marionette", "Phantom Marionette", CardDefinition.MonsterType.ZOMBIE, CardDefinition.Attribute.DARK, 4, 1500, 1100, "");
        add("prism_psion", "Prism Psion", CardDefinition.MonsterType.PSYCHIC, CardDefinition.Attribute.LIGHT, 3, 1000, 1200, "");
        add("azure_wyrm", "Azure Wyrm", CardDefinition.MonsterType.WYRM, CardDefinition.Attribute.WATER, 6, 2000, 1800, "");
        add("nova_cyberling", "Nova Cyberling", CardDefinition.MonsterType.CYBERSE, CardDefinition.Attribute.LIGHT, 4, 1600, 1200, "");
        add("twilight_illusionist", "Twilight Illusionist", CardDefinition.MonsterType.ILLUSION, CardDefinition.Attribute.DARK, 4, 1400, 1300, "");
        add("divine_sentinel", "Divine Sentinel", CardDefinition.MonsterType.DIVINE_BEAST, CardDefinition.Attribute.DIVINE, 7, 2500, 2200, "");

        addSpell("field_repair", "Field Repair", "DRAW_ONE");
        addSpell("arcane_surge", "Arcane Surge", "DRAW_TWO");
        addSpell("guardian_charm", "Guardian Charm", "GAIN_LP");
        addSpell("monster_reborn_echo", "Rebirth Echo", "REVIVE_MONSTER");
        addTrap("mirror_guard", "Mirror Guard", "NEGATE_ATTACK");
        addTrap("last_stand", "Last Stand", "PREVENT_DAMAGE");
        addTrap("grave_snare", "Grave Snare", "DESTROY_ATTACKER");

        addPendulum("scale_flare_adept", "Scale Flare Adept", CardDefinition.MonsterType.SPELLCASTER, CardDefinition.Attribute.FIRE, 4, 1500, 1000, 2);
        addPendulum("scale_tide_beast", "Scale Tide Beast", CardDefinition.MonsterType.AQUA, CardDefinition.Attribute.WATER, 4, 1400, 1600, 7);
        addPendulum("scale_gale_ranger", "Scale Gale Ranger", CardDefinition.MonsterType.WARRIOR, CardDefinition.Attribute.WIND, 3, 1200, 900, 1);
        addPendulum("scale_terra_golem", "Scale Terra Golem", CardDefinition.MonsterType.ROCK, CardDefinition.Attribute.EARTH, 5, 1800, 2000, 8);
        addPendulum("scale_dusk_dragon", "Scale Dusk Dragon", CardDefinition.MonsterType.DRAGON, CardDefinition.Attribute.DARK, 6, 2100, 1700, 3);
        addPendulum("scale_radiant_fairy", "Scale Radiant Fairy", CardDefinition.MonsterType.FAIRY, CardDefinition.Attribute.LIGHT, 2, 800, 1500, 6);
    }

    private CardCatalog() { }

    private static void add(String id, String name, CardDefinition.MonsterType type,
                            CardDefinition.Attribute attribute, int level, int atk, int def,
                            String effect) {
        CARDS.add(new CardDefinition(id, name, CardDefinition.Kind.MONSTER,
                CardDefinition.SummonMethod.NORMAL, type, attribute, level, atk, def, -1, effect));
    }

    private static void addPendulum(String id, String name, CardDefinition.MonsterType type,
                                    CardDefinition.Attribute attribute, int level, int atk,
                                    int def, int scale) {
        CARDS.add(new CardDefinition(id, name, CardDefinition.Kind.MONSTER,
                CardDefinition.SummonMethod.PENDULUM, type, attribute, level, atk, def, scale, ""));
    }


    private static void addSpell(String id, String name, String effect) {
        CARDS.add(new CardDefinition(id, name, CardDefinition.Kind.SPELL,
                CardDefinition.SummonMethod.NORMAL, null, null, 0, 0, 0, -1, effect));
    }

    private static void addTrap(String id, String name, String effect) {
        CARDS.add(new CardDefinition(id, name, CardDefinition.Kind.TRAP,
                CardDefinition.SummonMethod.NORMAL, null, null, 0, 0, 0, -1, effect));
    }

    public static List<CardDefinition> all() { return Collections.unmodifiableList(CARDS); }

    public static CardDefinition find(String id) {
        for (CardDefinition card : CARDS) if (card.id.equals(id)) return card;
        return null;
    }
}
