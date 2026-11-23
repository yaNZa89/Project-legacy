package com.yanza.pocketduel;

/** Declarative effect metadata; the resolver will interpret timing and operation IDs. */
public final class EffectDefinition {
    public enum Timing {
        ON_SUMMON, ON_ACTIVATION, IGNITION, WHEN_ATTACKS, WHEN_DESTROYED, END_PHASE,
        QUICK, CONTINUOUS
    }

    public final String id;
    public final Timing timing;
    public final String operation;
    public final boolean oncePerTurn;

    public EffectDefinition(String id, Timing timing, String operation, boolean oncePerTurn) {
        this.id = id;
        this.timing = timing;
        this.operation = operation;
        this.oncePerTurn = oncePerTurn;
    }
}
