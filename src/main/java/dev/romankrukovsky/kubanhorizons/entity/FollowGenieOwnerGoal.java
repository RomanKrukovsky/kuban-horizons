package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Держит спутницу рядом с хозяином и возвращает её после сильного отставания. */
final class FollowGenieOwnerGoal extends Goal {
    private final KubanGenie genie;
    private Player owner;

    FollowGenieOwnerGoal(KubanGenie genie) {
        this.genie = genie;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        owner = genie.getOwner();
        return owner != null && !owner.isSpectator()
                && genie.brain().mode() != GenieBehaviorMode.STAY
                && genie.distanceToSqr(owner) > 16.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive()
                && genie.brain().mode() != GenieBehaviorMode.STAY
                && genie.distanceToSqr(owner) > 9.0D;
    }

    @Override
    public void tick() {
        genie.getLookControl().setLookAt(owner, 20.0F, 20.0F);
        if (genie.distanceToSqr(owner) > 256.0D) {
            genie.teleportTo(owner.getX() + 1.5D, owner.getY() + 1.0D, owner.getZ() + 1.5D);
            genie.getNavigation().stop();
            return;
        }
        double speed = genie.brain().mode() == GenieBehaviorMode.GUARD ? 1.3D : 1.15D;
        genie.getNavigation().moveTo(owner, speed);
    }
}
