package com.yanza.pocketduel;

import java.util.HashMap;
import java.util.Map;

/**
 * Small data-driven effect dispatcher. New effects are added by registering metadata
 * and an operation handler instead of adding card-name checks to the duel screen.
 */
public final class CardEffectResolver {
    private interface Operation {
        String execute(DuelEngine duel, CardDefinition source);
    }

    private static final Map<String, EffectDefinition> DEFINITIONS =
            new HashMap<String, EffectDefinition>();
    private static final Map<String, Operation> OPERATIONS =
            new HashMap<String, Operation>();

    static {
        register(new EffectDefinition("DRAW_ONE_ON_SUMMON",
                EffectDefinition.Timing.ON_SUMMON, "DRAW_ONE", false),
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source) {
                        return duel.drawOneForEffect()
                                ? "Effect: draw 1 card"
                                : "Effect activated, but no card could be drawn";
                    }
                });
    }

    private CardEffectResolver() { }

    private static void register(EffectDefinition definition, Operation operation) {
        DEFINITIONS.put(definition.id, definition);
        OPERATIONS.put(definition.operation, operation);
    }

    public static String resolveOnSummon(DuelEngine duel, CardDefinition source) {
        if (source == null || source.effectId.length() == 0) return "";
        EffectDefinition definition = DEFINITIONS.get(source.effectId);
        if (definition == null || definition.timing != EffectDefinition.Timing.ON_SUMMON) {
            return "";
        }
        Operation operation = OPERATIONS.get(definition.operation);
        return operation == null ? "" : operation.execute(duel, source);
    }
}
