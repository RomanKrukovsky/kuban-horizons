package dev.romankrukovsky.kubanhorizons.registry;

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
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, KubanHorizons.MOD_ID);

    /** Плодородие грядок чанка. Сериализуется только при наличии записей. */
    public static final Supplier<AttachmentType<ChunkFertilityData>> CHUNK_FERTILITY =
            ATTACHMENTS.register("chunk_fertility",
                    () -> AttachmentType.serializable(ChunkFertilityData::new).build());

    private KHAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
