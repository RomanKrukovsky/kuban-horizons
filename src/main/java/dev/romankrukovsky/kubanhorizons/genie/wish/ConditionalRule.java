package dev.romankrukovsky.kubanhorizons.genie.wish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Персистентное условное правило: триггер и действие владельца.
 *
 * <p>Действие хранится формулировкой ({@link #actionDescription}), а не готовым
 * {@link WishIntent}: у намерения нет Codec, а на срабатывании формулировка
 * снова проходит через {@link WishParser}, поэтому исполнение всегда свежее.</p>
 */
public record ConditionalRule(
        UUID id,
        UUID ownerUuid,
        TriggerType trigger,
        String triggerParam,
        String actionDescription,
        boolean enabled,
        long createdAt) {

    public enum TriggerType {
        TIME_NIGHT,
        TIME_DAY,
        HEALTH_LOW,
        ENTITY_NEARBY,
        BLOCK_PLACED,
        RAIN_START,
        RAIN_STOP;

        public static TriggerType fromKey(String key) {
            for (TriggerType value : values()) {
                if (value.name().equalsIgnoreCase(key)) {
                    return value;
                }
            }
            return TIME_NIGHT;
        }
    }

    private static final StreamCodec<ByteBuf, TriggerType> TRIGGER_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    raw -> TriggerType.fromKey(raw.toLowerCase(Locale.ROOT)),
                    TriggerType::name);

    public static final Codec<ConditionalRule> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(rule -> rule.id.toString()),
                    Codec.STRING.fieldOf("owner").forGetter(rule -> rule.ownerUuid.toString()),
                    Codec.STRING.fieldOf("trigger").forGetter(rule -> rule.trigger.name()),
                    Codec.STRING.fieldOf("triggerParam").forGetter(ConditionalRule::triggerParam),
                    Codec.STRING.fieldOf("action").forGetter(ConditionalRule::actionDescription),
                    Codec.BOOL.fieldOf("enabled").forGetter(ConditionalRule::enabled),
                    Codec.LONG.fieldOf("createdAt").forGetter(ConditionalRule::createdAt)
            ).apply(instance, ConditionalRule::fromComponents));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConditionalRule> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, rule) -> {
                        buffer.writeUtf(rule.id().toString());
                        buffer.writeUtf(rule.ownerUuid().toString());
                        TRIGGER_CODEC.encode(buffer, rule.trigger());
                        buffer.writeUtf(rule.triggerParam());
                        buffer.writeUtf(rule.actionDescription());
                        buffer.writeBoolean(rule.enabled());
                        buffer.writeVarLong(rule.createdAt());
                    },
                    buffer -> fromComponents(
                            buffer.readUtf(),
                            buffer.readUtf(),
                            TRIGGER_CODEC.decode(buffer).name(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readBoolean(),
                            buffer.readVarLong()));

    private static ConditionalRule fromComponents(String id, String owner, String trigger,
            String triggerParam, String action, boolean enabled, long createdAt) {
        return new ConditionalRule(parseUuid(id), parseUuid(owner), TriggerType.fromKey(trigger),
                triggerParam, action, enabled, createdAt);
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    /** Копия правила с изменённым флагом включения. */
    public ConditionalRule withEnabled(boolean newEnabled) {
        return new ConditionalRule(id, ownerUuid, trigger, triggerParam, actionDescription,
                newEnabled, createdAt);
    }
}
