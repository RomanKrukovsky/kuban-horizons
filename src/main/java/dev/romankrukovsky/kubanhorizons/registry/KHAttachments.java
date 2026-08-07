package dev.romankrukovsky.kubanhorizons.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.soil.ChunkFertilityData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Регистрация data attachments мода.
 */
public final class KHAttachments {
    private static final int GUIDE_SCHEMA_VERSION = 1;
    private static final com.mojang.serialization.MapCodec<Boolean> GUIDE_GIVEN_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.optionalFieldOf("SchemaVersion", GUIDE_SCHEMA_VERSION)
                            .forGetter(value -> GUIDE_SCHEMA_VERSION),
                    Codec.BOOL.fieldOf("GuideGiven").forGetter(value -> value)
            ).apply(instance, (schemaVersion, guideGiven) -> guideGiven));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, KubanHorizons.MOD_ID);

    /** Плодородие грядок чанка. Сериализуется только при наличии записей. */
    public static final Supplier<AttachmentType<ChunkFertilityData>> CHUNK_FERTILITY =
            ATTACHMENTS.register("chunk_fertility",
                    () -> AttachmentType.serializable(ChunkFertilityData::new).build());

    /** Метки опыления грядок чанка. Пустая карта не сериализуется. */
    public static final Supplier<AttachmentType<dev.romankrukovsky.kubanhorizons.soil.ChunkPollinationData>> CHUNK_POLLINATION =
            ATTACHMENTS.register("chunk_pollination",
                    () -> AttachmentType.serializable(
                            dev.romankrukovsky.kubanhorizons.soil.ChunkPollinationData::new).build());

    /** Флаг «путеводитель выдан» на игроке; переживает смерть. */
    public static final Supplier<AttachmentType<Boolean>> GUIDE_GIVEN =
            ATTACHMENTS.register("guide_given",
                    () -> AttachmentType.builder(() -> Boolean.FALSE)
                            .serialize(GUIDE_GIVEN_CODEC)
                            .copyOnDeath()
                            .build());

    private KHAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
