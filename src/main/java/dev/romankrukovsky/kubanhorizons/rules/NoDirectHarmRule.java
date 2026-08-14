package dev.romankrukovsky.kubanhorizons.rules;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * NoDirectHarm — prevents a player from directly damaging their own genie
 * or other protected entities via LivingIncomingDamageEvent.
 *
 * <p>Old event signature: LivingIncomingDamageEvent(entity, source, amount)
 * is respected; we only read via getEntity()/getSource()/getAmount().</p>
 */
public final class NoDirectHarmRule implements MetaRuleEngine.MetaRule {

    private final java.util.function.Predicate<LivingEntity> protectedEntityTest;

    public NoDirectHarmRule(java.util.function.Predicate<LivingEntity> protectedEntityTest) {
        this.protectedEntityTest = protectedEntityTest;
    }

    /**
     * Default constructor — protects owned genies.
     */
    public NoDirectHarmRule() {
        this(entity ->
                entity instanceof dev.romankrukovsky.kubanhorizons.entity.KubanGenie genie
                        && genie.getOwner() != null);
    }

    @Override
    public List<Class<? extends Event>> getHandledEventTypes() {
        return List.of(LivingIncomingDamageEvent.class);
    }

    @Override
    public boolean test(Event event, MetaRuleEngine.EvaluationContext context) {
        if (!(event instanceof LivingIncomingDamageEvent damageEvent)) {
            return true;
        }
        LivingEntity target = damageEvent.getEntity();
        if (!protectedEntityTest.test(target)) {
            return true;
        }
        // Check if the attacker is the owner (or the same player)
        DamageSource source = damageEvent.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker) {
            if (target instanceof dev.romankrukovsky.kubanhorizons.entity.KubanGenie genie) {
                if (genie.isOwnedBy(attacker)) {
                    // Direct self-harm via owned genie — veto
                    return false;
                }
            }
        }
        return true;
    }
}
