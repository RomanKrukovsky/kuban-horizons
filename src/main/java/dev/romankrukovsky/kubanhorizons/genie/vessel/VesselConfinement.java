package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Заключение игрока-джиннии в сосуд и выход обратно.
 *
 * <p>Связывает две вещи, которые до сих пор существовали по отдельности:
 * измерение {@code kubanhorizons:eternal_kuban} было зарегистрировано, но
 * недостижимо — {@code ETERNAL_KUBAN} не упоминался нигде за пределами
 * пакета {@code worldgen}, — а {@code boundVesselPos} хранился в attachment и
 * никем не читался. Пока сосуд не ведёт в измерение, любое затягивание
 * показывает игроку пустоту, поэтому это первый пункт порядка работ.</p>
 *
 * <p>Правило, из которого выведено поведение: <b>рука свободна, слово
 * связано</b>. Внутри сосуда игрок строит и ломает как обычно — это его труд;
 * связано только исполнение желаний. Поэтому заключение не отбирает
 * управление, а меняет место: игрока не выключают из игры, а отправляют домой,
 * в собственный дворец.</p>
 */
public final class VesselConfinement {
    /**
     * Точка появления внутри дворца.
     *
     * <p>Пол главного зала из {@link KHDimensions#PALACE_FLOOR_Y} плюс два
     * блока: игрок должен встать на пол, а не оказаться в нём. X и Z нулевые —
     * дворец стоит в начале координат измерения.</p>
     */
    private static final Vec3 PALACE_SPAWN =
            new Vec3(0.5D, KHDimensions.PALACE_FLOOR_Y + 2, 0.5D);

    private VesselConfinement() {
    }

    /** Находится ли игрок внутри сосуда прямо сейчас. */
    public static boolean isConfined(ServerPlayer player) {
        return player.level().dimension().equals(KHDimensions.ETERNAL_KUBAN);
    }

    /**
     * Затягивает игрока в его сосуд.
     *
     * <p>Точка выхода запоминается до телепорта: без неё выход из сосуда некуда
     * вести, а игрок, потерявший сосуд в Нижнем мире, вернулся бы в оверворлд
     * на случайные координаты. Запоминается измерение и позиция, поэтому
     * возврат честен даже через перезаход в мир — attachment сериализуется.</p>
     *
     * @return {@code false}, если игрок не джинния или уже внутри
     */
    public static boolean confine(ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie() || isConfined(player)) {
            return false;
        }
        ServerLevel target = player.level().getServer().getLevel(KHDimensions.ETERNAL_KUBAN);
        if (target == null) {
            // Датапак измерения не загружен: сообщить, а не молча ничего не сделать.
            player.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.genie.vessel.dimension_missing"));
            return false;
        }

        attachment.setBoundVesselEntry(player.position(), player.getYRot());
        attachment.setBoundVesselDimension(player.level().dimension());
        attachment.setVesselCreated(true);

        ServerLevel from = (ServerLevel) player.level();
        MagicalSignature.cast(from, player.position());
        from.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                80, 0.4D, 0.8D, 0.4D, 0.1D);

        player.teleport(new TeleportTransition(target, PALACE_SPAWN, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));

        player.sendSystemMessage(
                Component.translatable("message.kubanhorizons.genie.vessel.confined"));
        return true;
    }

    /**
     * Выпускает игрока обратно в мир.
     *
     * <p>Возвращает туда, откуда затянуло, включая измерение: игрок, которого
     * затянуло из Нижнего мира, вернётся в Нижний мир, а не в оверворлд на
     * случайные координаты.</p>
     *
     * <p>Если точка входа неизвестна — так бывает у игрока, ставшего джиннией
     * уже внутри измерения, — используется его собственная точка возрождения
     * через {@code findRespawnPositionAndUseSpawnBlock}: ванильный метод сам
     * учитывает кровать, якорь возрождения и спавн мира, поэтому вернуть в
     * никуда невозможно.</p>
     *
     * @return {@code false}, если игрок не внутри сосуда
     */
    public static boolean release(ServerPlayer player) {
        if (!isConfined(player)) {
            return false;
        }
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        Optional<Vec3> entry = attachment.getBoundVesselEntry();
        ServerLevel boundLevel = attachment.getBoundVesselDimension()
                .map(key -> player.level().getServer().getLevel(key))
                .orElse(null);

        if (entry.isEmpty() || boundLevel == null) {
            // Точки входа нет — отдать ваниле: она знает про кровать, якорь и спавн.
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(
                    false, TeleportTransition.DO_NOTHING));
            player.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.genie.vessel.released"));
            return true;
        }

        Vec3 destination = entry.get();
        player.teleport(new TeleportTransition(boundLevel, destination, Vec3.ZERO,
                attachment.getBoundVesselYaw(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));

        MagicalSignature.cast(boundLevel, destination);
        boundLevel.sendParticles(ParticleTypes.WITCH, destination.x, destination.y + 1.0D, destination.z,
                60, 0.5D, 1.0D, 0.5D, 0.1D);

        player.sendSystemMessage(
                Component.translatable("message.kubanhorizons.genie.vessel.released"));
        return true;
    }
}
