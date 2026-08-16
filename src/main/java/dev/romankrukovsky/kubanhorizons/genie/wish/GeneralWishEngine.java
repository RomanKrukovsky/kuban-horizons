package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transform.FlyingStructureController;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Locale;

/**
 * Мощный общий исполнитель желаний.
 * Используется когда LLM поняла запрос, но жёсткий парсер не смог дать точную категорию.
 * Всегда пытается сделать что-то реальное и заметное в мире.
 */
public final class GeneralWishEngine {

    private GeneralWishEngine() {}

    public static WishExecutor.Result execute(ServerLevel level, Player player, String rawText) {
        String text = rawText.toLowerCase(Locale.ROOT).trim();
        BlockPos base = player.blockPosition().relative(player.getDirection(), 3);

        // === ПРЕДМЕТЫ ===
        if (text.contains("алмаз") || text.contains("diamond")) {
            return give(level, player, new ItemStack(Items.DIAMOND, 32), "message.kubanhorizons.genie.wish.gave_diamonds");
        }
        if (text.contains("золот") || text.contains("gold") || text.contains("слиток")) {
            return give(level, player, new ItemStack(Items.GOLD_INGOT, 64), "message.kubanhorizons.genie.wish.gave_gold");
        }
        if (text.contains("еда") || text.contains("хлеб") || text.contains("food")) {
            return give(level, player, new ItemStack(Items.BREAD, 64), "message.kubanhorizons.genie.wish.gave_food");
        }
        if (text.contains("меч") || text.contains("sword")) {
            return give(level, player, new ItemStack(Items.DIAMOND_SWORD), "message.kubanhorizons.genie.wish.gave_sword");
        }
        if (text.contains("кирка") || text.contains("pickaxe")) {
            return give(level, player, new ItemStack(Items.DIAMOND_PICKAXE), "message.kubanhorizons.genie.wish.gave_pickaxe");
        }

        // === МОБЫ ===
        if (text.contains("куриц") || text.contains("chicken")) {
            return spawn(level, player, EntityTypes.CHICKEN, 25, "message.kubanhorizons.genie.wish.spawned_chickens");
        }
        if (text.contains("коров") || text.contains("cow")) {
            return spawn(level, player, EntityTypes.COW, 8, "message.kubanhorizons.genie.wish.spawned_cows");
        }
        if (text.contains("волк") || text.contains("wolf") || text.contains("собак")) {
            return spawn(level, player, EntityTypes.WOLF, 6, "message.kubanhorizons.genie.wish.spawned_wolves");
        }
        if (text.contains("лошад") || text.contains("horse")) {
            return spawn(level, player, EntityTypes.HORSE, 3, "message.kubanhorizons.genie.wish.spawned_horses");
        }
        if (text.contains("кот") || text.contains("cat")) {
            return spawn(level, player, EntityTypes.CAT, 5, "message.kubanhorizons.genie.wish.spawned_cats");
        }

        // === ПОГОДА И ВРЕМЯ ===
        if (text.contains("дождь") || text.contains("rain")) {
            level.setRainLevel(1.0f);
            level.sendParticles(ParticleTypes.RAIN, player.getX(), player.getY() + 20, player.getZ(), 150, 12, 6, 12, 0);
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.rain_started");
        }
        if (text.contains("солнц") || text.contains("clear") || text.contains("погода")) {
            level.setRainLevel(0.0f);
            level.setThunderLevel(0.0f);
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.weather_cleared");
        }
        if (text.contains("ночь") || text.contains("night")) {
            var stack = player instanceof net.minecraft.server.level.ServerPlayer sp ? sp.createCommandSourceStack() : level.getServer().createCommandSourceStack();
            level.getServer().getCommands().performPrefixedCommand(stack, "time set night");
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.night_set");
        }
        if (text.contains("день") || text.contains("day")) {
            var stack = player instanceof net.minecraft.server.level.ServerPlayer sp ? sp.createCommandSourceStack() : level.getServer().createCommandSourceStack();
            level.getServer().getCommands().performPrefixedCommand(stack, "time set day");
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.day_set");
        }

        // === БЛОКИ И СТРОИТЕЛЬСТВО ===
        if (text.contains("дерево") || text.contains("tree")) {
            level.setBlock(base, Blocks.OAK_LOG.defaultBlockState(), 3);
            level.setBlock(base.above(), Blocks.OAK_LEAVES.defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5, 30, 0.8, 1, 0.8, 0.05);
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.placed_tree");
        }
        if (text.contains("дом") || text.contains("house")) {
            // Простой домик 5x5
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    if (x == -2 || x == 2 || z == -2 || z == 2) {
                        level.setBlock(base.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                        level.setBlock(base.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    }
                }
            }
            level.setBlock(base.offset(0, 3, 0), Blocks.OAK_PLANKS.defaultBlockState(), 3); // крыша
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.placed_house");
        }
        if (text.contains("летающ") && text.contains("дом")
                || text.contains("flying") && text.contains("house")
                || text.contains("подними") && text.contains("дом")) {
            return flyingHouse(level, player);
        }

        // === МАГИЧЕСКИЕ ЭФФЕКТЫ ===
        if (text.contains("взрыв") || text.contains("explosion") || text.contains("фейерверк")) {
            level.explode(player, base.getX(), base.getY(), base.getZ(), 2.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            level.sendParticles(ParticleTypes.FIREWORK, base.getX(), base.getY() + 3, base.getZ(), 80, 1.5, 2, 1.5, 0.1);
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.firework");
        }

        // === FALLBACK — красивый магический эффект ===
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 2.5, player.getZ(), 60, 1.2, 1.5, 1.2, 0.03);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 40, 0.8, 0.6, 0.8, 0.02);
        return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.general_fulfilled");
    }

    private static WishExecutor.Result give(ServerLevel level, Player player, ItemStack stack, String key) {
        ItemEntity e = new ItemEntity(level, player.getX(), player.getY() + 1.2, player.getZ(), stack);
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
        return new WishExecutor.Result(true, key);
    }

    private static WishExecutor.Result spawn(ServerLevel level, Player player, EntityType<? extends LivingEntity> type, int count, String key) {
        for (int i = 0; i < count; i++) {
            LivingEntity mob = type.create(level, EntitySpawnReason.COMMAND);
            if (mob != null) {
                double ox = (level.getRandom().nextDouble() - 0.5) * 5.0;
                double oz = (level.getRandom().nextDouble() - 0.5) * 5.0;
                mob.snapTo(player.getX() + ox, player.getY() + 1, player.getZ() + oz, player.getYRot(), 0.0F);
                level.addFreshEntity(mob);
            }
        }
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 50, 1.8, 0.8, 1.8, 0.02);
        return new WishExecutor.Result(true, key);
    }

    /** Поднимает выбранную область вокруг игрока в воздух через движок летающих структур. */
    private static WishExecutor.Result flyingHouse(ServerLevel level, Player player) {
        BlockPos center = player.blockPosition();
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(),
                center.offset(-3, 0, -3), center.offset(3, 4, 3));
        if (selection.volume() > KHServerConfig.genieMaxRegionVolume()) {
            return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.failed");
        }
        try {
            SnapshotService.SnapshotState state = SnapshotService.captureState(level, selection);
            RegionSnapshot snapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                    new SnapshotId(UUID.randomUUID(), "flying_house"), player.getUUID(), Instant.now(),
                    selection, state.blocks(), state.blockTicks(), state.fluidTicks(),
                    state.entities(), state.biomes(), SnapshotService.digest(state));
            FlyingStructureController.get(level).start(level, snapshot,
                    new Vec3(0.0D, 0.5D, 0.0D), KHServerConfig.genieFlyingHouseDurationTicks());
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.flying_house");
        } catch (IOException | RuntimeException exception) {
            return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.failed");
        }
    }
}
