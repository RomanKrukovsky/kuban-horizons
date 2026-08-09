package dev.romankrukovsky.kubanhorizons.genie.memory;

import dev.romankrukovsky.kubanhorizons.genie.GenieBrain;
import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.WishborneState;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Снимок личности джиннии, живущий отдельно от сущности.
 *
 * <p>Джинния — одна личность мира, и эта личность не должна зависеть от
 * судьбы конкретного объекта в памяти сервера. Если сущность всё-таки
 * исчезла, поводок восстанавливает её из снимка, и накопленные отношения,
 * решения и состояние Wishborne сохраняются. Без снимка возвращалась бы
 * чужая джинния с чистым характером.</p>
 *
 * <p>Данные хранятся сырым NBT: {@link GeniePersonality}, {@link GenieBrain}
 * и {@link WishborneState} уже умеют сохранять себя в {@link ValueOutput}, и
 * дублировать их поля здесь значило бы поддерживать две схемы вместо одной.</p>
 */
public record GenieStateSnapshot(UUID ownerId, CompoundTag personality, CompoundTag brain,
        CompoundTag wishborne) {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenieStateSnapshot.class);

    /** Собирает снимок из живых компонентов джиннии. */
    public static GenieStateSnapshot capture(UUID ownerId, GeniePersonality personality,
            GenieBrain brain, WishborneState wishborne, HolderLookup.Provider registries) {
        return new GenieStateSnapshot(ownerId,
                toTag(registries, personality::save),
                toTag(registries, brain::save),
                toTag(registries, wishborne::save));
    }

    /** Применяет снимок к компонентам джиннии. */
    public void applyTo(GeniePersonality personality, GenieBrain brain, WishborneState wishborne,
            HolderLookup.Provider registries) {
        fromTag(registries, this.personality, personality::load);
        fromTag(registries, this.brain, brain::load);
        fromTag(registries, this.wishborne, wishborne::load);
    }

    /** Владелец, к которому джинния была привязана; пусто, если связи не было. */
    public Optional<UUID> owner() {
        return Optional.ofNullable(ownerId);
    }

    void save(ValueOutput output) {
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
        output.store("Personality", CompoundTag.CODEC, personality);
        output.store("Brain", CompoundTag.CODEC, brain);
        output.store("Wishborne", CompoundTag.CODEC, wishborne);
    }

    static GenieStateSnapshot load(ValueInput input) {
        UUID owner = null;
        String rawOwner = input.getStringOr("Owner", "");
        if (!rawOwner.isEmpty()) {
            try {
                owner = UUID.fromString(rawOwner);
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        return new GenieStateSnapshot(owner,
                input.read("Personality", CompoundTag.CODEC).orElseGet(CompoundTag::new),
                input.read("Brain", CompoundTag.CODEC).orElseGet(CompoundTag::new),
                input.read("Wishborne", CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    private static CompoundTag toTag(HolderLookup.Provider registries, Consumer<ValueOutput> writer) {
        try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            writer.accept(output);
            return output.buildResult();
        }
    }

    private static void fromTag(HolderLookup.Provider registries, CompoundTag tag,
            Consumer<ValueInput> reader) {
        try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
            reader.accept(TagValueInput.create(reporter, registries, tag));
        }
    }
}
