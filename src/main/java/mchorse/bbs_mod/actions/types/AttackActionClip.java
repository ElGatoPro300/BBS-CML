package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.AttackDamage;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AttackActionClip extends ActionClip
{
    public final ValueFloat damage = new ValueFloat("damage", 0F);

    public AttackActionClip()
    {
        super();

        this.add(this.damage);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        this.applyPositionRotation(player, replay, tick);

        /* Keep fake player / actor weapon in sync so attribute + Mob Killer match. */
        if (actor != null)
        {
            ItemStack main = actor.getMainHandStack();

            if (!ItemStack.areEqual(player.getMainHandStack(), main))
            {
                player.setStackInHand(Hand.MAIN_HAND, main.copy());
            }
        }
        else if (replay != null)
        {
            ItemStack main = replay.keyframes.mainHand.interpolate(tick, ItemStack.EMPTY);

            player.setStackInHand(Hand.MAIN_HAND, main == null ? ItemStack.EMPTY : main.copy());
        }

        LivingEntity damageSource = actor != null ? actor : player;
        double distance = 6D;
        HitResult blockHit = player.raycast(distance, 1F, false);
        Vec3d origin = player.getCameraPosVec(1F);
        Vec3d rotation = player.getRotationVec(1F);
        Vec3d direction = origin.add(rotation.x * distance, rotation.y * distance, rotation.z * distance);

        double newDistance = blockHit != null ? blockHit.getPos().squaredDistanceTo(origin) : distance * distance;
        Box box = player.getBoundingBox().stretch(rotation.multiply(distance)).expand(1, 1, 1);
        EntityHitResult entityHit = ProjectileUtil.raycast(damageSource, origin, direction, box, entity -> !entity.isSpectator() && entity.canHit(), newDistance);

        if (entityHit == null)
        {
            return;
        }

        Entity entity = entityHit.getEntity();

        if (entity == null)
        {
            return;
        }

        AttackDamage.applyHit(damageSource, entity, this.damage.get());
    }

    @Override
    protected Clip create()
    {
        return new AttackActionClip();
    }
}
