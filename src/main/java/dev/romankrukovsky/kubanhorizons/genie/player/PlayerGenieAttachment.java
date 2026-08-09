package dev.romankrukovsky.kubanhorizons.genie.player;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
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
    public static final int CURRENT_SCHEMA_VERSION = 1;

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
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PlayerGenieAttachment::isGenie,
                    ByteBufCodecs.VAR_INT,
                    attachment -> attachment.getStage().ordinal(),
                    ByteBufCodecs.STRING_UTF8,
                    PlayerGenieAttachment::getAvatarStyle,
                    PlayerGenieAttachment::fromNetwork);

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
    private String avatarStyle = "DEFAULT_KUBAN";
    private long nextTransformationTick = 0L;
    private boolean vesselCreated = false;

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
        masterUUID.ifPresent(uuid -> output.putString("Master", uuid.toString()));
        boundVesselPos.ifPresent(pos -> output.putLong("BoundVesselPos", pos.asLong()));
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
        String master = input.getStringOr("Master", "");
        try {
            masterUUID = master.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(master));
        } catch (IllegalArgumentException ignored) {
            masterUUID = Optional.empty();
        }
        long vesselPos = input.getLongOr("BoundVesselPos", Long.MIN_VALUE);
        boundVesselPos = vesselPos == Long.MIN_VALUE ? Optional.empty() : Optional.of(BlockPos.of(vesselPos));
    }
}
