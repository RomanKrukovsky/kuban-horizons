package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Материализация произнесённых слов в физические 3D-буквы/блоки (Word Materializer). */
public final class WordMaterializer {
    private WordMaterializer() {
    }

    public static void materializeWord(ServerLevel level, Player player, String word) {
        String normalized = word == null ? "" : word.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.equals("RAIN") || normalized.equals("ДОЖДЬ")) {
            level.setRainLevel(1.0F);
            level.setThunderLevel(0.0F);
            MagicalSignature.cast(level, player.position());
            return;
        }
        normalized = normalized.replaceAll("[^A-ZА-ЯЁ0-9 ]", "");
        if (normalized.isBlank()) {
            return;
        }
        normalized = normalized.substring(0, Math.min(12, normalized.length()));
        BlockState material = normalized.equals("GOLD") || normalized.equals("ЗОЛОТО")
                ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockPos pos = player.blockPosition().above(3);
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
                        level.setBlock(pos.offset(cursor + column, 4 - row, 0), material, 3);
                    }
                }
            }
            cursor += 4;
        }
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(pos));
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
