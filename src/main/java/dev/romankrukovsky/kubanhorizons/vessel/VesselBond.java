package dev.romankrukovsky.kubanhorizons.vessel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/**
 * Привязка сосуда к владельцу.
 * Сосуд обладает простым сознанием: выбирает владельца, не передаёт связь при краже,
 * может стать неподъёмным или вернуться к предыдущему владельцу.
 */
public final class VesselBond {
    private static final int SCHEMA_VERSION = 1;

    private UUID ownerId;
    private String ownerName;
    private long bondedAt;
    private int rejectionCount;      // Сколько раз сосуд отказался от кражи
    private boolean isUnliftable;    // Сосуд отказывается быть поднятым
    private int loyaltyLevel;        // 0-100: растёт при верности владельца

    public VesselBond() {
        this.rejectionCount = 0;
        this.isUnliftable = false;
        this.loyaltyLevel = 0;
    }

    /** Проверяет, является ли игрок владельцем. */
    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    /** Пытается связать сосуд с новым владельцем.
     * @return true если связь установлена, false если сосуд отказал */
    public boolean attemptBond(Player candidate) {
        // Если уже есть владелец — отказываем вору
        if (ownerId != null && !ownerId.equals(candidate.getUUID())) {
            rejectionCount++;
            // При 3+ попытках кражи сосуд может стать неподъёмным
            if (rejectionCount >= 3) {
                isUnliftable = true;
            }
            return false;
        }

        // Новый владелец — устанавливаем связь
        this.ownerId = candidate.getUUID();
        this.ownerName = candidate.getName().getString();
        this.bondedAt = System.currentTimeMillis();
        this.rejectionCount = 0;
        this.isUnliftable = false;
        this.loyaltyLevel = Math.min(100, loyaltyLevel + 10);
        return true;
    }

    /** Проверяет, может ли игрок поднять сосуд. */
    public boolean canBeLiftedBy(Player player) {
        if (ownerId == null) return true; // бесхозный — любой может взять
        if (isOwner(player)) return true; // владелец всегда может
        return !isUnliftable; // вор может, если сосуд ещё не "закрылся"
    }

    /** Возвращает сосуд предыдущему владельцу (если есть). */
    public UUID getPreviousOwner() {
        // В полной реализации здесь была бы история владельцев
        return null;
    }

    /** Увеличивает лояльность при верном использовании. */
    public void rewardLoyalty() {
        loyaltyLevel = Math.min(100, loyaltyLevel + 5);
        if (loyaltyLevel >= 80) {
            isUnliftable = false; // высокая лояльность снимает "закрытие"
        }
    }

    /** Снижает лояльность при злоупотреблении. */
    public void penalizeMisuse() {
        loyaltyLevel = Math.max(0, loyaltyLevel - 15);
        if (loyaltyLevel < 30 && ownerId != null) {
            // Сосуд может "устать" от владельца
            isUnliftable = true;
        }
    }

    public UUID getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public long getBondedAt() { return bondedAt; }
    public int getRejectionCount() { return rejectionCount; }
    public boolean isUnliftable() { return isUnliftable; }
    public int getLoyaltyLevel() { return loyaltyLevel; }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        if (ownerId != null) tag.putUUID("OwnerId", ownerId);
        if (ownerName != null) tag.putString("OwnerName", ownerName);
        tag.putLong("BondedAt", bondedAt);
        tag.putInt("RejectionCount", rejectionCount);
        tag.putBoolean("IsUnliftable", isUnliftable);
        tag.putInt("LoyaltyLevel", loyaltyLevel);
        return tag;
    }

    public static VesselBond load(CompoundTag tag) {
        VesselBond bond = new VesselBond();
        if (tag.hasUUID("OwnerId")) {
            bond.ownerId = tag.getUUID("OwnerId");
        }
        if (tag.contains("OwnerName")) {
            bond.ownerName = tag.getString("OwnerName");
        }
        bond.bondedAt = tag.getLong("BondedAt");
        bond.rejectionCount = tag.getInt("RejectionCount");
        bond.isUnliftable = tag.getBoolean("IsUnliftable");
        bond.loyaltyLevel = tag.getInt("LoyaltyLevel");
        return bond;
    }

    // Codec для DataComponent serialization
    public static final Codec<VesselBond> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("ownerId", null)
                .xmap(s -> s != null ? UUID.fromString(s) : null, u -> u != null ? u.toString() : null)
                .forGetter(b -> b.ownerId),
            Codec.STRING.optionalFieldOf("ownerName", null).forGetter(b -> b.ownerName),
            Codec.LONG.fieldOf("bondedAt").orElse(0L).forGetter(b -> b.bondedAt),
            Codec.INT.fieldOf("rejectionCount").orElse(0).forGetter(b -> b.rejectionCount),
            Codec.BOOL.fieldOf("isUnliftable").orElse(false).forGetter(b -> b.isUnliftable),
            Codec.INT.fieldOf("loyaltyLevel").orElse(0).forGetter(b -> b.loyaltyLevel)
        ).apply(instance, (oid, on, ba, rc, iu, ll) -> {
            VesselBond b = new VesselBond();
            b.ownerId = oid;
            b.ownerName = on;
            b.bondedAt = ba;
            b.rejectionCount = rc;
            b.isUnliftable = iu;
            b.loyaltyLevel = ll;
            return b;
        })
    );

    // StreamCodec для network synchronization
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselBond> STREAM_CODEC =
        StreamCodec.of(
            (buf, bond) -> {
                buf.writeBoolean(bond.ownerId != null);
                if (bond.ownerId != null) buf.writeUUID(bond.ownerId);
                buf.writeBoolean(bond.ownerName != null);
                if (bond.ownerName != null) buf.writeUtf(bond.ownerName);
                buf.writeLong(bond.bondedAt);
                buf.writeInt(bond.rejectionCount);
                buf.writeBoolean(bond.isUnliftable);
                buf.writeInt(bond.loyaltyLevel);
            },
            buf -> {
                VesselBond bond = new VesselBond();
                if (buf.readBoolean()) bond.ownerId = buf.readUUID();
                if (buf.readBoolean()) bond.ownerName = buf.readUtf();
                bond.bondedAt = buf.readLong();
                bond.rejectionCount = buf.readInt();
                bond.isUnliftable = buf.readBoolean();
                bond.loyaltyLevel = buf.readInt();
                return bond;
            }
        );
}
