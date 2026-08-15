package dev.romankrukovsky.kubanhorizons.genie.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Комната невыполненных желаний (GENIE_VISION, §«Память и история»).
 *
 * <p>Отклонённые и не исполненные желания (темперамент, бюджет, отказ) оседают
 * здесь персистентной записью, к которой игрок может вернуться и исполнить её
 * позже. Каждая запись дополнительно материализуется в мире колонной из
 * гладкого камня с сундуком наверху и бумагой с текстом желания внутри.</p>
 */
public final class UnfulfilledWishRoom extends SavedData {

    private static final int MAX_WISHES = 512;

    public static final Codec<UnfulfilledWish> WISH_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(w -> w.id.toString()),
            Codec.STRING.fieldOf("owner").forGetter(w -> w.ownerUuid.toString()),
            Codec.STRING.fieldOf("wish").forGetter(UnfulfilledWish::wishText),
            Codec.STRING.fieldOf("reason").forGetter(UnfulfilledWish::reason),
            Codec.LONG.fieldOf("createdAt").forGetter(UnfulfilledWish::createdAt),
            Codec.LONG.fieldOf("roomPos").forGetter(w -> w.roomPos.asLong()),
            Codec.BOOL.fieldOf("resolved").forGetter(UnfulfilledWish::resolved)
    ).apply(instance, UnfulfilledWishRoom::decodeWish));

    public static final Codec<UnfulfilledWishRoom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(WISH_CODEC).fieldOf("wishes").forGetter(r -> r.wishes)
    ).apply(instance, UnfulfilledWishRoom::new));

    public static final SavedDataType<UnfulfilledWishRoom> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "unfulfilled_wish_room"),
            UnfulfilledWishRoom::new,
            CODEC);

    private final List<UnfulfilledWish> wishes;

    public UnfulfilledWishRoom() {
        this(new ArrayList<>());
    }

    public UnfulfilledWishRoom(List<UnfulfilledWish> wishes) {
        this.wishes = new ArrayList<>(wishes);
    }

    public static UnfulfilledWishRoom get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(UUID owner, String wishText, String reason, BlockPos roomPos) {
        wishes.add(new UnfulfilledWish(UUID.randomUUID(), owner, wishText, reason,
                System.currentTimeMillis(), roomPos, false));
        while (wishes.size() > MAX_WISHES) {
            wishes.removeFirst();
        }
        setDirty();
    }

    public boolean hasPending(UUID owner) {
        return wishes.stream().anyMatch(w -> w.ownerUuid.equals(owner) && !w.resolved);
    }

    public List<UnfulfilledWish> forOwner(UUID owner) {
        return wishes.stream().filter(w -> w.ownerUuid.equals(owner)).toList();
    }

    public void resolve(UUID id) {
        for (int i = 0; i < wishes.size(); i++) {
            UnfulfilledWish wish = wishes.get(i);
            if (wish.id.equals(id) && !wish.resolved) {
                wishes.set(i, new UnfulfilledWish(wish.id, wish.ownerUuid, wish.wishText,
                        wish.reason, wish.createdAt, wish.roomPos, true));
                setDirty();
                return;
            }
        }
    }

    public int count() {
        return wishes.size();
    }

    /** Материализует невыполненное желание колонной с сундуком и подписанной бумагой. */
    public static void materialize(ServerLevel level, BlockPos pos, String wishText) {
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above(2))) {
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.SMOOTH_STONE.defaultBlockState());
        BlockPos chestPos = pos.above(2);
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof Container chest) {
            ItemStack paper = new ItemStack(Items.PAPER);
            if (wishText != null && !wishText.isBlank()) {
                paper.set(DataComponents.CUSTOM_NAME, Component.literal(wishText));
            }
            chest.setItem(0, paper);
            chest.setChanged();
        }
    }

    private static UnfulfilledWish decodeWish(String id, String owner, String wish, String reason,
                                              long createdAt, long roomPos, boolean resolved) {
        return new UnfulfilledWish(parseUuid(id), parseUuid(owner), wish, reason,
                createdAt, BlockPos.of(roomPos), resolved);
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    public record UnfulfilledWish(
            UUID id,
            UUID ownerUuid,
            String wishText,
            String reason,
            long createdAt,
            BlockPos roomPos,
            boolean resolved) {
    }
}
