package dev.romankrukovsky.kubanhorizons.genie.player;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.Optional;
import java.util.UUID;

/**
 * Data Attachment для отслеживания джинновского состояния игрока,
 * стадии трансформации, прогресса всемогущества и привязки к хозяину.
 */
public class PlayerGenieAttachment implements ValueIOSerializable {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public enum Stage {
        HUMAN,
        BODY_REWRITE,
        TAIL_FORMATION,
        AVATAR_CUSTOMIZATION,
        INVULNERABILITY_TEST,
        FULL_GENIE
    }

    /**
     * Сетевой кодек: только то, что нужно рендеру, а не всё состояние.
     *
     * <p>Превращение решается на сервере, а рисуется на клиенте, и до этого
     * кодека клиент вообще не знал, что игрок стал джиннией — поэтому хвоста не
     * было видно, хотя серверные стадии проходились и сохранялись.</p>
     *
     * <p>Передаются три поля: флаг, стадия и стиль аватара. Прогресс желания,
     * хозяин, позиция сосуда и тик следующего перехода остаются серверными —
     * это игровая логика, клиенту она не нужна, а лишние поля в кодеке
     * означали бы рассылку при каждом их изменении.</p>
     *
     * <p>Стадия едет как порядковый номер, а не как строка: имена enum'а —
     * внутренняя деталь, и {@code ordinal} на входе проверяется по длине
     * массива, поэтому испорченный или устаревший пакет даёт {@code HUMAN},
     * а не исключение при рендере.</p>
     */
    public static final StreamCodec<ByteBuf, PlayerGenieAttachment> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, attachment) -> {
                        buffer.writeBoolean(attachment.isGenie());
                        buffer.writeVarInt(attachment.getStage().ordinal());
                        buffer.writeUtf(attachment.getAvatarStyle());
                    },
                    buffer -> {
                        boolean isGenie = buffer.readBoolean();
                        int stageOrdinal = buffer.readVarInt();
                        String avatarStyle = buffer.readUtf();
                        PlayerGenieAttachment attachment = new PlayerGenieAttachment();
                        attachment.isGenie = isGenie;
                        Stage[] stages = Stage.values();
                        attachment.stage = stageOrdinal >= 0 && stageOrdinal < stages.length
                                ? stages[stageOrdinal]
                                : Stage.HUMAN;
                        attachment.avatarStyle = avatarStyle;
                        return attachment;
                    }
            );

    /** Собирает клиентскую копию: только визуальные поля, остальные — по умолчанию. */
    private static PlayerGenieAttachment fromNetwork(boolean isGenie, int stageOrdinal, String avatarStyle) {
        PlayerGenieAttachment attachment = new PlayerGenieAttachment();
        attachment.isGenie = isGenie;
        Stage[] stages = Stage.values();
        attachment.stage = stageOrdinal >= 0 && stageOrdinal < stages.length
                ? stages[stageOrdinal]
                : Stage.HUMAN;
        attachment.avatarStyle = avatarStyle;
        return attachment;
    }

    private boolean isGenie = false;
    private Stage stage = Stage.HUMAN;
    private int wishProgressPercent = 0;
    private int tierLevel = 1;
    private Optional<UUID> masterUUID = Optional.empty();
    private Optional<BlockPos> boundVesselPos = Optional.empty();
    private Optional<ResourceKey<Level>> boundVesselDimension = Optional.empty();
    private Optional<Vec3> boundVesselEntry = Optional.empty();
    private float boundVesselYaw;
    private Optional<ResourceKey<Level>> dimensionalReturnDimension = Optional.empty();
    private Optional<Vec3> dimensionalReturnPosition = Optional.empty();
    private float dimensionalReturnYaw;
    private float dimensionalReturnPitch;
    private String avatarStyle = "DEFAULT_KUBAN";
    private long nextTransformationTick = 0L;
    private boolean vesselCreated = false;
    private int corruption = 0;
    private long lastWishTick = 0L;

    public PlayerGenieAttachment() {
    }

    public boolean isGenie() {
        return isGenie;
    }

    public void setGenie(boolean genie) {
        this.isGenie = genie;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int getWishProgressPercent() {
        return wishProgressPercent;
    }

    public void setWishProgressPercent(int wishProgressPercent) {
        this.wishProgressPercent = Math.clamp(wishProgressPercent, 0, 100);
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public void setTierLevel(int tierLevel) {
        this.tierLevel = Math.clamp(tierLevel, 1, 5);
    }

    public Optional<UUID> getMasterUUID() {
        return masterUUID;
    }

    public void setMasterUUID(UUID masterUUID) {
        this.masterUUID = Optional.ofNullable(masterUUID);
    }

    public Optional<BlockPos> getBoundVesselPos() {
        return boundVesselPos;
    }

    public void setBoundVesselPos(BlockPos boundVesselPos) {
        this.boundVesselPos = Optional.ofNullable(boundVesselPos);
    }

    /**
     * Измерение, из которого игрока затянуло в сосуд.
     *
     * <p>Отдельно от позиции, потому что одной позиции недостаточно:
     * затянутый из Нижнего мира вернулся бы на те же координаты, но в
     * оверворлде — то есть в случайное место под землёй. Хранится как
     * {@code ResourceKey}, а сериализуется строкой идентификатора.</p>
     */
    public Optional<ResourceKey<Level>> getBoundVesselDimension() {
        return boundVesselDimension;
    }

    public void setBoundVesselDimension(ResourceKey<Level> dimension) {
        this.boundVesselDimension = Optional.ofNullable(dimension);
    }

    public Optional<Vec3> getBoundVesselEntry() {
        return boundVesselEntry;
    }

    public float getBoundVesselYaw() {
        return boundVesselYaw;
    }

    public void setBoundVesselEntry(Vec3 position, float yaw) {
        boundVesselEntry = Optional.ofNullable(position);
        boundVesselYaw = yaw;
        if (position != null) {
            boundVesselPos = Optional.of(BlockPos.containing(position));
        }
    }

    public Optional<ResourceKey<Level>> getDimensionalReturnDimension() {
        return dimensionalReturnDimension;
    }

    public Optional<Vec3> getDimensionalReturnPosition() {
        return dimensionalReturnPosition;
    }

    public float getDimensionalReturnYaw() {
        return dimensionalReturnYaw;
    }

    public float getDimensionalReturnPitch() {
        return dimensionalReturnPitch;
    }

    public boolean hasDimensionalReturn() {
        return dimensionalReturnDimension.isPresent() && dimensionalReturnPosition.isPresent();
    }

    public void setDimensionalReturn(ResourceKey<Level> dimension, Vec3 position,
                                     float yaw, float pitch) {
        dimensionalReturnDimension = Optional.ofNullable(dimension);
        dimensionalReturnPosition = Optional.ofNullable(position);
        dimensionalReturnYaw = yaw;
        dimensionalReturnPitch = pitch;
    }

    public void clearDimensionalReturn() {
        dimensionalReturnDimension = Optional.empty();
        dimensionalReturnPosition = Optional.empty();
        dimensionalReturnYaw = 0.0F;
        dimensionalReturnPitch = 0.0F;
    }

    public String getAvatarStyle() {
        return avatarStyle;
    }

    public void setAvatarStyle(String avatarStyle) {
        this.avatarStyle = avatarStyle == null || avatarStyle.isBlank() ? "DEFAULT_KUBAN" : avatarStyle;
    }

    public long getNextTransformationTick() {
        return nextTransformationTick;
    }

    public void setNextTransformationTick(long nextTransformationTick) {
        this.nextTransformationTick = Math.max(0L, nextTransformationTick);
    }

    public boolean isVesselCreated() {
        return vesselCreated;
    }

    public void setVesselCreated(boolean vesselCreated) {
        this.vesselCreated = vesselCreated;
    }

    /**
     * Искажение желаниями у самого игрока, 0..100.
     *
     * <p>Своё поле, а не {@code corruption} джиннии: тот параметр принадлежит
     * ей и описывает, во что её превратил хозяин. У игрока-джиннии счёт
     * отдельный — он отвечает за свои желания, а не за её прошлое.</p>
     *
     * <p>Из этого числа выводится окно затягивания в сосуд: чем больше
     * жестоких и крупных желаний, тем короче окно и тем быстрее натягивается
     * хвост.</p>
     */
    public int getCorruption() {
        return corruption;
    }

    public void setCorruption(int corruption) {
        this.corruption = Math.clamp(corruption, 0, 100);
    }

    /**
     * Игровое время последнего желания.
     *
     * <p>Нужно для закона сосуда: тишина обесценивает выход, а желание
     * обнуляет отсчёт. Хранится игровое время, а не системное — иначе выход
     * из игры на неделю обнулял бы риск реальным временем.</p>
     */
    public long getLastWishTick() {
        return lastWishTick;
    }

    public void setLastWishTick(long lastWishTick) {
        this.lastWishTick = Math.max(0L, lastWishTick);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("SchemaVersion", CURRENT_SCHEMA_VERSION);
        output.putBoolean("IsGenie", isGenie);
        output.putString("Stage", stage.name());
        output.putInt("WishProgressPercent", wishProgressPercent);
        output.putInt("TierLevel", tierLevel);
        output.putString("AvatarStyle", avatarStyle);
        output.putLong("NextTransformationTick", nextTransformationTick);
        output.putBoolean("VesselCreated", vesselCreated);
        output.putInt("Corruption", corruption);
        output.putLong("LastWishTick", lastWishTick);
        masterUUID.ifPresent(uuid -> output.putString("Master", uuid.toString()));
        boundVesselPos.ifPresent(pos -> output.putLong("BoundVesselPos", pos.asLong()));
        boundVesselDimension.ifPresent(key -> output.putString("BoundVesselDim", key.identifier().toString()));
        boundVesselEntry.ifPresent(entry -> {
            output.putBoolean("HasBoundVesselEntry", true);
            output.putDouble("BoundVesselX", entry.x);
            output.putDouble("BoundVesselY", entry.y);
            output.putDouble("BoundVesselZ", entry.z);
            output.putFloat("BoundVesselYaw", boundVesselYaw);
        });
        dimensionalReturnDimension.ifPresent(key ->
                output.putString("DimensionalReturnDim", key.identifier().toString()));
        dimensionalReturnPosition.ifPresent(position -> {
            output.putBoolean("HasDimensionalReturn", true);
            output.putDouble("DimensionalReturnX", position.x);
            output.putDouble("DimensionalReturnY", position.y);
            output.putDouble("DimensionalReturnZ", position.z);
            output.putFloat("DimensionalReturnYaw", dimensionalReturnYaw);
            output.putFloat("DimensionalReturnPitch", dimensionalReturnPitch);
        });
    }

    @Override
    public void deserialize(ValueInput input) {
        isGenie = input.getBooleanOr("IsGenie", false);
        try {
            stage = Stage.valueOf(input.getStringOr("Stage", "HUMAN"));
        } catch (IllegalArgumentException e) {
            stage = Stage.HUMAN;
        }
        wishProgressPercent = Math.clamp(input.getIntOr("WishProgressPercent", 0), 0, 100);
        tierLevel = Math.clamp(input.getIntOr("TierLevel", 1), 1, 5);
        avatarStyle = input.getStringOr("AvatarStyle", "DEFAULT_KUBAN");
        nextTransformationTick = Math.max(0L, input.getLongOr("NextTransformationTick", 0L));
        vesselCreated = input.getBooleanOr("VesselCreated", false);
        corruption = Math.clamp(input.getIntOr("Corruption", 0), 0, 100);
        lastWishTick = Math.max(0L, input.getLongOr("LastWishTick", 0L));
        String master = input.getStringOr("Master", "");
        try {
            masterUUID = master.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(master));
        } catch (IllegalArgumentException ignored) {
            masterUUID = Optional.empty();
        }
        long vesselPos = input.getLongOr("BoundVesselPos", Long.MIN_VALUE);
        boundVesselPos = vesselPos == Long.MIN_VALUE ? Optional.empty() : Optional.of(BlockPos.of(vesselPos));
        String dim = input.getStringOr("BoundVesselDim", "");
        // Неизвестное измерение — то же, что отсутствующее: выход тогда идёт
        // через точку возрождения, а не в сохранённые координаты чужого мира.
        Identifier dimId = dim.isEmpty() ? null : Identifier.tryParse(dim);
        boundVesselDimension = dimId == null
                ? Optional.empty()
                : Optional.of(ResourceKey.create(Registries.DIMENSION, dimId));
        if (input.getBooleanOr("HasBoundVesselEntry", false)) {
            boundVesselEntry = Optional.of(new Vec3(
                    input.getDoubleOr("BoundVesselX", 0.5D),
                    input.getDoubleOr("BoundVesselY", 64.0D),
                    input.getDoubleOr("BoundVesselZ", 0.5D)));
            boundVesselYaw = input.getFloatOr("BoundVesselYaw", 0.0F);
        } else {
            boundVesselEntry = boundVesselPos.map(Vec3::atCenterOf);
            boundVesselYaw = 0.0F;
        }
        String returnDimension = input.getStringOr("DimensionalReturnDim", "");
        Identifier returnDimensionId = returnDimension.isEmpty()
                ? null
                : Identifier.tryParse(returnDimension);
        dimensionalReturnDimension = returnDimensionId == null
                ? Optional.empty()
                : Optional.of(ResourceKey.create(Registries.DIMENSION, returnDimensionId));
        if (input.getBooleanOr("HasDimensionalReturn", false)) {
            dimensionalReturnPosition = Optional.of(new Vec3(
                    input.getDoubleOr("DimensionalReturnX", 0.5D),
                    input.getDoubleOr("DimensionalReturnY", 64.0D),
                    input.getDoubleOr("DimensionalReturnZ", 0.5D)));
            dimensionalReturnYaw = input.getFloatOr("DimensionalReturnYaw", 0.0F);
            dimensionalReturnPitch = input.getFloatOr("DimensionalReturnPitch", 0.0F);
        } else {
            dimensionalReturnPosition = Optional.empty();
            dimensionalReturnYaw = 0.0F;
            dimensionalReturnPitch = 0.0F;
        }
    }
}
