package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Функциональный интерфейс одной операции желания.
 *
 * <p>Рантайм вызывает {@link #execute} внутри контролируемого контекста
 * (try/catch + causality ledger). Реализации находятся в движках
 * ({@link GeneralWishEngine}, {@link LiteralWishEngine} и т. д.) и
 * возвращают произвольный результат — конкретный тип знает только вызывающий.</p>
 */
@FunctionalInterface
public interface WishOperation {

    Object execute(ParsedWish wish, ServerLevel level, ServerPlayer player) throws Exception;
}