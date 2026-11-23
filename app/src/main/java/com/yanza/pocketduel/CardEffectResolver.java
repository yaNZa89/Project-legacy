package com.yanza.pocketduel;

import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven card effect dispatcher. Card IDs point to definitions; operations
 * are resolved here so the duel engine does not need per-card special cases.
 */
public final class CardEffectResolver {
    private interface Operation {
        String execute(DuelEngine duel, CardDefinition source, int context);
    }

    private static final Map<String, EffectDefinition> DEFINITIONS =
            new HashMap<String, EffectDefinition>();
    private static final Map<String, Operation> OPERATIONS =
            new HashMap<String, Operation>();

    static {
        register("DRAW_ONE_ON_SUMMON", EffectDefinition.Timing.ON_SUMMON, "DRAW_ONE",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return duel.drawOneForEffect() ? "draw 1 card" : "no card could be drawn";
                    }
                });
        register("DRAW_ONE", EffectDefinition.Timing.ON_ACTIVATION, "DRAW_ONE",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return "drew " + duel.drawCardsForEffect(1) + " card";
                    }
                });
        register("DRAW_TWO", EffectDefinition.Timing.ON_ACTIVATION, "DRAW_TWO",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return "drew " + duel.drawCardsForEffect(2) + " cards";
                    }
                });
        register("GAIN_LP", EffectDefinition.Timing.ON_ACTIVATION, "GAIN_LP",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        int gained = duel.gainLifePointsForEffect(500);
                        return "gained " + gained + " LP";
                    }
                });
        register("REVIVE_MONSTER", EffectDefinition.Timing.ON_ACTIVATION, "REVIVE_MONSTER",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return duel.reviveMonsterForEffect()
                                ? "Special Summoned 1 monster from the Graveyard"
                                : "no eligible monster or open Monster Zone";
                    }
                });
        register("NEGATE_ATTACK", EffectDefinition.Timing.WHEN_ATTACKS, "NEGATE_ATTACK",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return "opponent attack negated";
                    }
                });
        register("PREVENT_DAMAGE", EffectDefinition.Timing.WHEN_ATTACKS, "PREVENT_BATTLE_DAMAGE",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return "battle damage prevented; battle proceeds";
                    }
                });
        register("SPECIAL_SUMMON_IF_NO_MONSTERS", EffectDefinition.Timing.HAND_SPECIAL_SUMMON,
                "NO_MONSTERS_ON_FIELD", new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return "Special Summoned while you control no monsters";
                    }
                });
        register("SPECIAL_SUMMON_IF_OPPONENT_HAS_MONSTER",
                EffectDefinition.Timing.HAND_SPECIAL_SUMMON,
                "OPPONENT_MONSTER_AND_EMPTY_FIELD", new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return "Special Summoned while the opponent controls a monster";
                    }
                });
        register("DESTROY_ATTACKER", EffectDefinition.Timing.WHEN_ATTACKS, "DESTROY_ATTACKER",
                new Operation() {
                    public String execute(DuelEngine duel, CardDefinition source, int context) {
                        return duel.destroyCpuAttackerForEffect(context);
                    }
                });
    }

    private CardEffectResolver() { }

    private static void register(String id, EffectDefinition.Timing timing, String operationId,
                                 Operation operation) {
        DEFINITIONS.put(id, new EffectDefinition(id, timing, operationId, false));
        OPERATIONS.put(operationId, operation);
    }

    private static String resolve(DuelEngine duel, CardDefinition source,
                                  EffectDefinition.Timing requiredTiming, int context) {
        if (source == null || source.effectId.length() == 0) return "";
        EffectDefinition definition = DEFINITIONS.get(source.effectId);
        if (definition == null || definition.timing != requiredTiming) return "";
        Operation operation = OPERATIONS.get(definition.operation);
        return operation == null ? "" : operation.execute(duel, source, context);
    }

    public static boolean canSpecialSummonFromHand(DuelEngine duel, CardDefinition source) {
        if (source == null) return false;
        EffectDefinition definition = DEFINITIONS.get(source.effectId);
        if (definition == null || definition.timing != EffectDefinition.Timing.HAND_SPECIAL_SUMMON)
            return false;
        if (duel.hasPlayerMonsters()) return false;
        if ("NO_MONSTERS_ON_FIELD".equals(definition.operation)) return true;
        if ("OPPONENT_MONSTER_AND_EMPTY_FIELD".equals(definition.operation))
            return duel.hasCpuMonsters();
        return false;
    }

    public static String resolveOnSummon(DuelEngine duel, CardDefinition source) {
        return resolve(duel, source, EffectDefinition.Timing.ON_SUMMON, -1);
    }

    public static String resolveSpell(DuelEngine duel, CardDefinition source) {
        return resolve(duel, source, EffectDefinition.Timing.ON_ACTIVATION, -1);
    }

    /** A non-empty result means the Trap successfully responded to the attack. */
    public static String resolveTrapOnAttack(DuelEngine duel, CardDefinition source, int attackerSlot) {
        return resolve(duel, source, EffectDefinition.Timing.WHEN_ATTACKS, attackerSlot);
    }
}
