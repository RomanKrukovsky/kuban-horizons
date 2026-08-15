package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.memory.ProvenanceJournal;
import dev.romankrukovsky.kubanhorizons.genie.memory.UnfulfilledWishRoom;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.history.AlternativeCausalityEngine;
import dev.romankrukovsky.kubanhorizons.genie.social.GenieMythSystem;
import dev.romankrukovsky.kubanhorizons.genie.society.SocietySimulator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Optional;

/** Единый серверный исполнитель всех категорий желаний. */
public final class WishExecutor {
    private WishExecutor() {
    }

    public static Result execute(ServerLevel level, Player player, WishIntent intent) {
        // LLM_DELEGATED wishes are handled directly without recording to memory
        if (intent.target() == WishIntent.Target.LLM_DELEGATED) {
            return LLMWishExecutor.execute(level, player, intent.detailParam());
        }

        Result result = switch (intent.category()) {
            case META_RULE -> MetaRuleEngine.execute(level, player, intent);
            case GIGANTISM -> GigantismScaleEngine.execute(level, player, intent);
            case MATERIAL -> switch (intent.target()) {
                case WORD_MATERIALIZATION -> executeWordMaterialization(level, player, intent);
                case DRAWING -> executeDrawing(level, player);
                default -> executeMaterialWish(level, player, intent);
            };
            case CIVILIZATION -> switch (intent.target()) {
                case BIOME_REWRITE -> executeBiomeRewrite(level, player);
                default -> executeCivilizationWish(level, player, intent);
            };
            case PROVENANCE -> executeProvenanceQuery(level, player, intent);
            case HISTORY -> switch (intent.target()) {
                case WHAT_IF -> executeWhatIf(level, player, intent);
                case THEATER_REENACTMENT -> executeTheater(level, player);
                default -> executeWhatIf(level, player, intent);
            };
            case MUSIC -> executeMusicSpell(level, player, intent);
            case DISTORTED_HIGHER_WISH -> (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    ? DistortedWishEngine.execute(level, serverPlayer, intent)
                    : LLMWishExecutor.execute(level, player, intent.detailParam());
            default -> GeneralWishEngine.execute(level, player, intent.detailParam().isBlank() ? "general wish" : intent.detailParam());
        };
        if (result.executed()) {
            WorldGenieMemory.get(level).recordWish(player.blockPosition(), intent.target().name(),
                    intent.precision(), level.getGameTime());
            SocietySimulator.get().recordWish(level, player.getUUID(), intent.safe());
        }
        return result;
    }

    private static Result executeMaterialWish(ServerLevel level, Player player, WishIntent intent) {
        if (intent.isPreciseAndSafe()) {
            return placeDiamondChest(level, player)
                    ? new Result(true, "message.kubanhorizons.genie.wish.safe")
                    : new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }

        BlockPos source = player.blockPosition().above(3);
        while (!level.isEmptyBlock(source) && source.getY() < level.getMaxY() - 1) {
            source = source.above();
        }
        if (!level.isEmptyBlock(source)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                FallingBlockEntity falling = FallingBlockEntity.fall(level, source.offset(x, 0, z),
                        Blocks.DIAMOND_ORE.defaultBlockState());
                falling.setHurtsEntities(8.0F, 40);
            }
        }
        level.sendParticles(ParticleTypes.PORTAL, source.getX() + 0.5D, source.getY() + 0.5D,
                source.getZ() + 0.5D, 36, 0.7D, 0.7D, 0.7D, 0.1D);
        return new Result(true, "message.kubanhorizons.genie.wish.literal");
    }

    private static Result executeCivilizationWish(ServerLevel level, Player player, WishIntent intent) {
        if (intent.target() == WishIntent.Target.GENIE_FESTIVAL) {
            return executeSocietyWish(level, player, intent);
        }
        BlockPos target = player.blockPosition().relative(player.getDirection(), 2);
        if (level.isEmptyBlock(target)) {
            level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(target) instanceof Container chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    chest.setItem(i, new ItemStack(Items.EMERALD, 64));
                }
                chest.setChanged();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5D, target.getY() + 0.5D,
                        target.getZ() + 0.5D, 40, 0.8D, 0.8D, 0.8D, 0.1D);
                memoryRecordVillage(level, target);
                return new Result(true, "message.kubanhorizons.genie.wish.village_wealth");
            }
        }
        return new Result(false, "message.kubanhorizons.genie.wish.no_space");
    }

    private static void memoryRecordVillage(ServerLevel level, BlockPos pos) {
        WorldGenieMemory memory = WorldGenieMemory.get(level);
        memory.recordVillageSaved(pos, level.getGameTime());
    }

    /** Социум: ежегодный праздник джиннии прямо у игрока. */
    private static Result executeSocietyWish(ServerLevel level, Player player, WishIntent intent) {
        GenieMythSystem.celebrateAnnualFestival(level, player.blockPosition());
        return new Result(true, "message.kubanhorizons.genie.wish.festival");
    }

    /**
     * Магическая музыка: «сыграй песню» исполняется как танец-заклинание.
     *
     * <p>Без названной песни звучит Песня дождя — первая и самая простая;
     * названные рост/покой/огонь дают свою песню. Каст идёт через
     * {@link DanceEngine}, чтобы эффект продолжался всю длительность песни.</p>
     */
    private static Result executeMusicSpell(ServerLevel level, Player player, WishIntent intent) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "wish.kubanhorizons.music.rain");
        }
        dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell spell = switch (intent.detailParam()) {
            case "GROWTH" -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.GROWTH_MELODY;
            case "PEACE" -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.PEACE_LULLABY;
            case "FIRE" -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.DANCE_OF_FIRE;
            default -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.RAIN_SONG;
        };
        dev.romankrukovsky.kubanhorizons.genie.music.DanceEngine.cast(level, serverPlayer, spell);
        String messageKey = switch (spell) {
            case RAIN_SONG -> "wish.kubanhorizons.music.rain";
            case GROWTH_MELODY -> "wish.kubanhorizons.music.growth";
            case PEACE_LULLABY -> "wish.kubanhorizons.music.peace";
            case DANCE_OF_FIRE -> "wish.kubanhorizons.music.fire";
        };
        return new Result(true, messageKey);
    }

    private static Result executeProvenanceQuery(ServerLevel level, Player player, WishIntent intent) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.empty_hand"));
            return new Result(false, "wish.kubanhorizons.provenance.empty_hand");
        }
        String id = held.getItem().builtInRegistryHolder().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(held.getItem().toString());
        ProvenanceJournal journal = ProvenanceJournal.get(level);
        List<ProvenanceJournal.ProvenanceRecord> hits = journal.queryById(id);
        if (hits.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.empty", id));
            return new Result(false, "wish.kubanhorizons.provenance.empty");
        }
        ProvenanceJournal.ProvenanceRecord last = hits.get(hits.size() - 1);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.entry",
                id, last.action(), last.wishText()));
        return new Result(true, "wish.kubanhorizons.provenance.entry");
    }

    /** История: «А что если?» — описательный отчёт об альтернативной версии мира. */
    private static Result executeWhatIf(ServerLevel level, Player player, WishIntent intent) {
        Optional<AlternativeCausalityEngine.WhatIfResult> alternative =
                AlternativeCausalityEngine.whatIf(level, player.getUUID(), intent.detailParam());
        if (alternative.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.whatif.empty"));
            return new Result(false, "wish.kubanhorizons.whatif.empty");
        }
        AlternativeCausalityEngine.WhatIfResult result = alternative.get();
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.whatif.result",
                result.wishText(), result.actualOutcome(), result.changedBlocks(),
                result.alternativeOutcome()));
        return new Result(true, "wish.kubanhorizons.whatif.result");
    }

    private static Result executeTheater(ServerLevel level, Player player) {
        boolean shown = dev.romankrukovsky.kubanhorizons.genie.dimension.VisualReenactmentEngine
                .reenactPastEvent(level, player.blockPosition(), player);
        return shown
                ? new Result(true, "message.kubanhorizons.genie.theater_reenactment")
                : new Result(false, "message.kubanhorizons.genie.theater_empty");
    }

    /** Материализация слова: «напиши слово Х» — блочный шрифт в воздухе. */
    private static Result executeWordMaterialization(ServerLevel level, Player player, WishIntent intent) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        String word = extractWord(intent.detailParam());
        if (word.isBlank() || word.length() > 12) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.word.bad"));
            return new Result(false, "wish.kubanhorizons.word.bad");
        }
        try {
            var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
            if (!runtime.ready()) runtime.recover();
            var preview = runtime.previewWord(serverPlayer, word);
            var report = runtime.executeWord(serverPlayer,
                    runtime.confirmWord(player.getUUID(), preview));
            boolean ok = report.outcome()
                    == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED;
            return new Result(ok, ok ? "wish.kubanhorizons.word.written" : "message.kubanhorizons.genie.wish.no_space");
        } catch (java.io.IOException | RuntimeException exception) {
            return new Result(false, "message.kubanhorizons.genie.runtime.failed");
        }
    }

    /** Рисунок-линия: «нарисуй» — линия блоков по выделению (или перед игроком). */
    private static Result executeDrawing(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        try {
            var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
            if (!runtime.ready()) runtime.recover();
            var preview = runtime.previewSelectedDrawing(serverPlayer);
            var report = runtime.executeDrawing(serverPlayer,
                    runtime.confirmDrawing(player.getUUID(), preview));
            boolean ok = report.outcome()
                    == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED;
            return new Result(ok, ok ? "wish.kubanhorizons.drawing.drawn" : "message.kubanhorizons.genie.wish.no_space");
        } catch (java.io.IOException | RuntimeException exception) {
            return new Result(false, "message.kubanhorizons.genie.runtime.failed");
        }
    }

    /** Переписывание биома: область вокруг игрока становится кубанской степью. */
    private static Result executeBiomeRewrite(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        try {
            var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
            if (!runtime.ready()) runtime.recover();
            var preview = runtime.previewBiomeRewrite(serverPlayer, player.blockPosition());
            var report = runtime.executeBiomeRewrite(serverPlayer,
                    runtime.confirmBiomeRewrite(player.getUUID(), preview));
            boolean ok = report.outcome()
                    == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED;
            return new Result(ok, ok ? "wish.kubanhorizons.biome.rewritten" : "message.kubanhorizons.genie.wish.no_space");
        } catch (java.io.IOException | RuntimeException exception) {
            return new Result(false, "message.kubanhorizons.genie.runtime.failed");
        }
    }

    /** Достаёт слово из «напиши слово X»: берёт первый подряд латиницей/кириллицей токен после «слово». */
    private static String extractWord(String detailParam) {
        String text = detailParam == null ? "" : detailParam.toLowerCase(java.util.Locale.ROOT);
        int marker = text.indexOf("слово");
        String after = marker >= 0 ? text.substring(marker + 5) : text;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[а-яёa-z]{1,12}").matcher(after);
        return matcher.find() ? matcher.group() : "";
    }

    private static boolean placeDiamondChest(ServerLevel level, Player player) {
        Direction facing = player.getDirection();
        BlockPos origin = player.blockPosition().relative(facing, 2);
        BlockPos target = origin;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos candidate = origin.above(dy);
            if (level.isEmptyBlock(candidate) && level.getBlockState(candidate.below()).isSolid()) {
                target = candidate;
                break;
            }
        }
        if (!level.isEmptyBlock(target) || !level.getBlockState(target.below()).isSolid()) {
            return false;
        }

        level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        if (!(level.getBlockEntity(target) instanceof Container chest)) {
            return false;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }
        chest.setChanged();
        level.sendParticles(ParticleTypes.ENCHANT, target.getX() + 0.5D, target.getY() + 0.8D,
                target.getZ() + 0.5D, 48, 0.8D, 0.6D, 0.8D, 0.1D);
        return true;
    }

    public record Result(boolean executed, String messageKey) {
        public Component message(int precision) {
            return Component.translatable(messageKey, precision);
        }
    }
}
