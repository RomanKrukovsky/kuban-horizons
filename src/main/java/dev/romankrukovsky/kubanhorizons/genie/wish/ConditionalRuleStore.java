package dev.romankrukovsky.kubanhorizons.genie.wish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Персистентное хранилище условных желаний (SavedData).
 *
 * <p>Правила переживают перезапуск мира: сериализация через {@link Codec} в
 * формате MC 26.2 SavedDataType, как у {@link
 * dev.romankrukovsky.kubanhorizons.genie.memory.ProvenanceJournal}. Серверный
 * тик раз в {@link #CHECK_INTERVAL_TICKS} оценивает триггеры включённых правил;
 * сработавшее правило отключается и исполняет действие владельцу.</p>
 */
public final class ConditionalRuleStore extends SavedData {

    static final int CHECK_INTERVAL_TICKS = 20;

    public static final Codec<ConditionalRuleStore> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.list(ConditionalRule.CODEC).fieldOf("rules").forGetter(store -> store.rules)
            ).apply(instance, ConditionalRuleStore::new));

    public static final SavedDataType<ConditionalRuleStore> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "conditional_rule_store"),
            ConditionalRuleStore::new,
            CODEC);

    private final List<ConditionalRule> rules = new ArrayList<>();
    private int ticksSinceCheck;
    private boolean wasRaining;

    public ConditionalRuleStore() {
    }

    public ConditionalRuleStore(List<ConditionalRule> rules) {
        this.rules.addAll(rules);
    }

    public static ConditionalRuleStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(ConditionalRule rule) {
        rules.add(rule);
        setDirty();
    }

    public boolean remove(UUID id) {
        boolean removed = rules.removeIf(rule -> rule.id().equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<ConditionalRule> forOwner(UUID owner) {
        return rules.stream().filter(rule -> rule.ownerUuid().equals(owner)).toList();
    }

    public List<ConditionalRule> all() {
        return List.copyOf(rules);
    }

    /**
     * Проверяет триггеры включённых правил.
     *
     * <p>Само-троттлинг счётчиком вызовов, а не {@code gameTime % 20}: счётчик
     * детерминирован и в тестах, где мир не тикает синхронно с проверкой.</p>
     */
    public void tick(ServerLevel level) {
        if (++ticksSinceCheck < CHECK_INTERVAL_TICKS) {
            return;
        }
        ticksSinceCheck = 0;
        boolean raining = level.isRaining();
        boolean changed = false;
        for (ConditionalRule rule : List.copyOf(rules)) {
            if (!rule.enabled() || !fires(level, rule, raining)) {
                continue;
            }
            Player owner = level.getServer().getPlayerList().getPlayer(rule.ownerUuid());
            if (owner != null) {
                WishExecutor.execute(level, owner, WishParser.parse(rule.actionDescription()));
            }
            rules.set(rules.indexOf(rule), rule.withEnabled(false));
            changed = true;
        }
        wasRaining = raining;
        if (changed) {
            setDirty();
        }
    }

    private static boolean fires(ServerLevel level, ConditionalRule rule, boolean raining) {
        long dayTime = level.getOverworldClockTime() % 24000L;
        return switch (rule.trigger()) {
            case TIME_NIGHT -> dayTime >= 13000L;
            case TIME_DAY -> dayTime < 13000L;
            case HEALTH_LOW -> {
                Player owner = level.getServer().getPlayerList().getPlayer(rule.ownerUuid());
                yield owner != null && owner.getHealth() < 6.0F;
            }
            case RAIN_START -> raining;
            case RAIN_STOP -> !raining;
            // ENTITY_NEARBY и BLOCK_PLACED требуют событийных хуков, в тике не оцениваются.
            default -> false;
        };
    }
}
