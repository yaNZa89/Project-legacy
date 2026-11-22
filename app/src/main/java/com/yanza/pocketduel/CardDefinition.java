package com.yanza.pocketduel;

/** Original card data. Rules and presentation are kept outside this model. */
public final class CardDefinition {
    public enum Kind { MONSTER, SPELL, TRAP }
    public enum SummonMethod {
        NORMAL, TRIBUTE, RITUAL, FUSION, SYNCHRO, XYZ, PENDULUM, LINK, SPECIAL
    }
    public enum MonsterType {
        WARRIOR, SPELLCASTER, DRAGON, MACHINE, FIEND, FAIRY, BEAST,
        BEAST_WARRIOR, WINGED_BEAST, AQUA, PYRO, THUNDER, ROCK, PLANT,
        INSECT, REPTILE, FISH, SEA_SERPENT, DINOSAUR, WYRM, CYBERSE,
        PSYCHIC, ZOMBIE, DIVINE_BEAST, ILLUSION, OTHER
    }
    public enum Attribute { DARK, LIGHT, EARTH, WATER, FIRE, WIND, DIVINE }

    public final String id;
    public final String name;
    public final Kind kind;
    public final SummonMethod summonMethod;
    public final MonsterType monsterType;
    public final Attribute attribute;
    public final int level;
    public final int attack;
    public final int defense;
    public final int pendulumScale;
    public final String effectId;

    /** Backward-compatible constructor for older prototype callers. */
    public CardDefinition(String id, String name, Kind kind, SummonMethod summonMethod,
                          int level, int attack, int defense, String effectId) {
        this(id, name, kind, summonMethod, null, null, level, attack, defense, -1, effectId);
    }

    public CardDefinition(String id, String name, Kind kind, SummonMethod summonMethod,
                          MonsterType monsterType, Attribute attribute, int level,
                          int attack, int defense, int pendulumScale, String effectId) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.summonMethod = summonMethod;
        this.monsterType = monsterType;
        this.attribute = attribute;
        this.level = level;
        this.attack = attack;
        this.defense = defense;
        this.pendulumScale = pendulumScale;
        this.effectId = effectId == null ? "" : effectId;
    }

    public boolean isPendulumMonster() {
        return kind == Kind.MONSTER && pendulumScale >= 0;
    }
}
