package com.yanza.pocketduel;

/** Data-only card model. Rules are resolved by DuelEngine, not by card-specific UI code. */
public final class CardDefinition {
    public enum Kind { MONSTER, SPELL, TRAP }
    public enum SummonMethod {
        NORMAL, TRIBUTE, RITUAL, FUSION, SYNCHRO, XYZ, PENDULUM, LINK, SPECIAL
    }

    public final String id;
    public final String name;
    public final Kind kind;
    public final SummonMethod summonMethod;
    public final int level;
    public final int attack;
    public final int defense;
    public final String effectId;

    public CardDefinition(String id, String name, Kind kind, SummonMethod summonMethod,
                          int level, int attack, int defense, String effectId) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.summonMethod = summonMethod;
        this.level = level;
        this.attack = attack;
        this.defense = defense;
        this.effectId = effectId == null ? "" : effectId;
    }
}
