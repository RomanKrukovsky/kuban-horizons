package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Материализация произнесённых слов в физические 3D-буквы/блоки (Word Materializer). */
public final class WordMaterializer {
    private WordMaterializer() {
    }

    public static WordPlan buildWordPlan(ServerLevel level, BlockPos origin, String word,
                                         UUID ownerId) throws IOException {
        String normalized = word == null ? "" : word.trim().toUpperCase(java.util.Locale.ROOT);
        normalized = normalized.replaceAll("[^A-ZА-ЯЁ0-9 ]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("word is empty");
        }
        normalized = normalized.substring(0, Math.min(12, normalized.length()));
        BlockState material = normalized.equals("GOLD") || normalized.equals("ЗОЛОТО")
                ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
        int width = Math.max(3, normalized.length() * 4 - 1);
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(),
                origin, origin.offset(width - 1, 4, 0));
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, selection);
        var targetBlocks = new ArrayList<RegionSnapshot.BlockRecord>(current.blocks());
        int cursor = 0;
        for (char letter : normalized.toCharArray()) {
            if (letter == ' ') {
                cursor += 2;
                continue;
            }
            int[] glyph = glyph(letter);
            for (int row = 0; row < 5; row++) {
                for (int column = 0; column < 3; column++) {
                    if ((glyph[row] & (1 << (2 - column))) != 0) {
                        int rx = cursor + column;
                        int ry = 4 - row;
                        int index = ry * width + rx;
                        targetBlocks.set(index, new RegionSnapshot.BlockRecord(rx, ry, 0,
                                NbtUtils.writeBlockState(material), null));
                    }
                }
            }
            cursor += 4;
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(targetBlocks,
                current.blockTicks(), current.fluidTicks(), current.entities(), current.biomes());
        RegionSnapshot before = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "word_before"), ownerId, Instant.now(), selection,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(),
                current.biomes(), SnapshotService.digest(current));
        RegionSnapshot after = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "word_materialized"), ownerId, Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(),
                target.biomes(), SnapshotService.digest(target));
        return new WordPlan(before, after, normalized);
    }

    public record WordPlan(RegionSnapshot current, RegionSnapshot target, String word) {
    }

    private static int[] glyph(char letter) {
        return switch (letter) {
            case 'A', 'А' -> new int[]{2, 5, 7, 5, 5};
            case 'B', 'В' -> new int[]{6, 5, 6, 5, 6};
            case 'C', 'С' -> new int[]{3, 4, 4, 4, 3};
            case 'D', 'Д' -> new int[]{6, 5, 5, 5, 6};
            case 'E', 'Е', 'Ё' -> new int[]{7, 4, 6, 4, 7};
            case 'F' -> new int[]{7, 4, 6, 4, 4};
            case 'G' -> new int[]{3, 4, 5, 5, 3};
            case 'H', 'Н' -> new int[]{5, 5, 7, 5, 5};
            case 'I', 'І' -> new int[]{7, 2, 2, 2, 7};
            case 'J' -> new int[]{1, 1, 1, 5, 2};
            case 'K', 'К' -> new int[]{5, 5, 6, 5, 5};
            case 'L', 'Г' -> new int[]{4, 4, 4, 4, 7};
            case 'M', 'М' -> new int[]{5, 7, 7, 5, 5};
            case 'N', 'И', 'Й' -> new int[]{5, 7, 7, 7, 5};
            case 'O', 'О', 'Ф' -> new int[]{2, 5, 5, 5, 2};
            case 'P', 'Р' -> new int[]{6, 5, 6, 4, 4};
            case 'Q' -> new int[]{2, 5, 5, 3, 1};
            case 'R', 'Я' -> new int[]{6, 5, 6, 5, 5};
            case 'S', 'З' -> new int[]{3, 4, 2, 1, 6};
            case 'T', 'Т' -> new int[]{7, 2, 2, 2, 2};
            case 'U', 'Ц' -> new int[]{5, 5, 5, 5, 7};
            case 'V', 'У' -> new int[]{5, 5, 5, 5, 2};
            case 'W', 'Ш', 'Щ' -> new int[]{5, 5, 7, 7, 5};
            case 'X', 'Х', 'Ж' -> new int[]{5, 5, 2, 5, 5};
            case 'Y', 'Ч' -> new int[]{5, 5, 2, 2, 2};
            case 'Z' -> new int[]{7, 1, 2, 4, 7};
            case 'Б' -> new int[]{7, 4, 6, 5, 6};
            case 'П' -> new int[]{7, 5, 5, 5, 5};
            case 'Л' -> new int[]{1, 3, 5, 5, 5};
            case 'Ы' -> new int[]{5, 6, 5, 5, 6};
            case 'Ь', 'Ъ' -> new int[]{4, 6, 5, 5, 6};
            case 'Э' -> new int[]{6, 1, 3, 1, 6};
            case 'Ю' -> new int[]{5, 7, 5, 5, 7};
            default -> new int[]{7, 1, 2, 0, 2};
        };
    }
}
