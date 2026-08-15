package dev.romankrukovsky.kubanhorizons.genie.social;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.society.SocietySimulator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Мифы о джиннии, слухи о её владельцах и ежегодный праздник (Genie Myth & Festival System). */
public final class GenieMythSystem {
    /** Свечение жителей после праздника — 10 секунд. */
    private static final int GLOW_TICKS = 200;

    /** Радиус, в котором жители участвуют в празднике. */
    private static final int FESTIVAL_RADIUS = 12;

    /** Половина стороны коврового пятна под праздничный стол. */
    private static final int TABLE_HALF = 1;

    private GenieMythSystem() {
    }

    /**
     * Мифы о джиннии и её владельце, порождённые историей мира.
     *
     * <p>Источник фактов — репутация владельца из {@link SocietyData} и общий
     * счётчик желаний из {@link WorldGenieMemory}: чем больше магия сделала
     * для мира, тем увереннее люди рассказывают о ней.</p>
     *
     * @return 1–2 строки-легенды с именем владельца
     */
    public static List<String> mythsFor(ServerLevel level, UUID owner) {
        String name = ownerName(level, owner);
        int reputation = SocietySimulator.get().reputation(level, owner);
        int wishes = WorldGenieMemory.get(level).totalWishesGranted();

        List<String> myths = new ArrayList<>(2);
        if (wishes > 0) {
            myths.add("Говорят, что " + name + " однажды сжал лампу — и степь услышала его имя.");
        }
        if (reputation >= 70) {
            myths.add("Старики в деревне божатся: у " + name + " сбываются желания.");
        } else if (reputation <= 20 && reputation > 0) {
            myths.add("Ходят тёмные слухи, что " + name + " опасен для магии.");
        } else if (myths.isEmpty()) {
            myths.add("О " + name + " говорят по-разному: джинния выбрала его, но зачем?");
        }
        return myths;
    }

    /**
     * Проводит ежегодный праздник джиннии: стол из красного ковра с тортом,
     * частицы радости, музыка и свечение довольных жителей.
     */
    public static void celebrateAnnualFestival(ServerLevel level, BlockPos villageCenter) {
        BlockPos ground = groundAt(level, villageCenter);
        for (int dx = -TABLE_HALF; dx <= TABLE_HALF; dx++) {
            for (int dz = -TABLE_HALF; dz <= TABLE_HALF; dz++) {
                BlockPos spot = ground.offset(dx, 0, dz);
                if (level.isEmptyBlock(spot) && level.getBlockState(spot.below()).isSolid()) {
                    level.setBlockAndUpdate(spot, Blocks.CARPET.red().defaultBlockState());
                }
            }
        }
        BlockPos cakeSpot = ground.above();
        if (level.isEmptyBlock(cakeSpot) && level.getBlockState(cakeSpot.below()).isSolid()) {
            level.setBlockAndUpdate(cakeSpot, Blocks.CAKE.defaultBlockState());
        }

        double cx = villageCenter.getX() + 0.5D;
        double cy = villageCenter.getY() + 1.0D;
        double cz = villageCenter.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 120,
                3.0D, 2.0D, 3.0D, 0.01D);
        level.playSound(null, villageCenter, SoundEvents.VILLAGER_CELEBRATE,
                SoundSource.NEUTRAL, 1.0F, 1.0F);

        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new AABB(villageCenter).inflate(FESTIVAL_RADIUS))) {
            villager.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0));
        }
    }

    /** Старый вход в праздник: волшебная подпись и сообщение игроку. */
    public static void startGenieFestival(ServerLevel level, BlockPos villageCenter, Player player) {
        celebrateAnnualFestival(level, villageCenter);
        MagicalSignature.cast(level, Vec3.atCenterOf(villageCenter));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.festival_start"));
    }

    /** Находит первый твёрдый пол под центром (иначе использует сам центр). */
    private static BlockPos groundAt(ServerLevel level, BlockPos center) {
        BlockPos candidate = center;
        for (int step = 0; step < 6; step++) {
            if (level.getBlockState(candidate.below()).isSolid()) {
                return candidate;
            }
            candidate = candidate.below();
            if (candidate.getY() < level.getMinY()) {
                break;
            }
        }
        return center;
    }

    private static String ownerName(ServerLevel level, UUID owner) {
        Player player = level.getPlayerByUUID(owner);
        if (player != null) {
            return player.getName().getString();
        }
        return "незнакомец " + owner.toString().substring(0, 8);
    }
}
