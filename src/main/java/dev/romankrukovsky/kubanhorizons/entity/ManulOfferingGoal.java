package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * Подход к оставленному подношению.
 *
 * <p>Основной путь знакомства для дикого зверя: игрок кладёт корм на землю и
 * отходит. Из рук дикий манул не берёт — это проверяется в
 * {@link Manul#mobInteract}, а здесь он идёт к лежащей еде.</p>
 *
 * <p>Цель намеренно не срабатывает, пока игрок стоит рядом: смысл механики в
 * том, чтобы отойти и подождать. Так «заслужить доверие» становится действием
 * игрока, а не просто задержкой по таймеру.</p>
 */
public final class ManulOfferingGoal extends Goal {
    private static final int SCAN_INTERVAL = 20;
    private static final double SCAN_RADIUS = 12.0D;
    /** Ближе этого игрок мешает зверю подойти к корму. */
    private static final double PLAYER_KEEP_AWAY = 5.0D;

    private final Manul manul;
    private ItemEntity target;

    public ManulOfferingGoal(Manul manul) {
        this.manul = manul;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!manul.canAcceptOffering()) {
            return false;
        }
        if (manul.tickCount % SCAN_INTERVAL != manul.getId() % SCAN_INTERVAL) {
            return false;
        }
        target = findOffering();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && manul.canAcceptOffering()
                && !playerTooClose(target);
    }

    @Override
    public void start() {
        if (target != null) {
            manul.getNavigation().moveTo(target, 1.0D);
        }
    }

    @Override
    public void stop() {
        target = null;
        manul.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        manul.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (manul.getNavigation().isDone()) {
            manul.getNavigation().moveTo(target, 1.0D);
        }
        // Подбор выполняет сама сущность (pickUpItem), здесь только подход.
    }

    /** Ближайшее подношение, к которому не стоит вплотную игрок. */
    private ItemEntity findOffering() {
        AABB area = manul.getBoundingBox().inflate(SCAN_RADIUS, 4.0D, SCAN_RADIUS);
        List<ItemEntity> items = manul.level().getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && item.getItem().is(Manul.OFFERINGS));
        ItemEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            if (playerTooClose(item)) {
                continue;
            }
            double distance = manul.distanceToSqr(item);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = item;
            }
        }
        return best;
    }

    /** Стоит ли игрок слишком близко к корму. */
    private boolean playerTooClose(ItemEntity item) {
        // Уже доверяющего зверя присутствие игрока не смущает.
        if (manul.trust().atLeast(ManulTrust.ACCEPTING)) {
            return false;
        }
        var player = manul.level().getNearestPlayer(item.getX(), item.getY(), item.getZ(),
                PLAYER_KEEP_AWAY, false);
        return player != null && !player.isCreative() && !player.isSpectator();
    }
}
