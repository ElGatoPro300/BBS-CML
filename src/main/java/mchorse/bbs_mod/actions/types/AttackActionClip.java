package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.actions.AttackDamage;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;

public class AttackActionClip extends ActionClip
{
    public final ValueFloat damage = new ValueFloat("damage", 0F);
    /**
     * Replay id of the {@link ActorEntity} hit while recording. Preferred over
     * raycast on playback so actor-vs-actor kills stay reliable when body yaw
     * and look diverge.
     */
    public final ValueString target = new ValueString("target", "");

    public AttackActionClip()
    {
        super();

        this.target.invisible();
        this.add(this.damage);
        this.add(this.target);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        /* Dead / discarded attackers must not keep dealing bound-target damage
         * (FakePlayer + target id would otherwise still hit survivors). */
        if (actor != null && (actor.isRemoved() || actor.isDeadOrDying() || actor.getHealth() <= 0F || actor.deathTime > 0))
        {
            return;
        }

        this.applyPositionRotation(player, replay, tick);

        /* Aim with look (headYaw + pitch), not body yaw — ActionPlayer snaps
         * actor yaw to headYaw, but applyPositionRotation still sets body yaw. */
        float lookYaw = replay.keyframes.headYaw.interpolate(tick).floatValue();
        float lookPitch = replay.keyframes.pitch.interpolate(tick).floatValue();

        player.setYRot(lookYaw);
        player.setYHeadRot(lookYaw);
        player.setXRot(lookPitch);

        /* Keep fake player / actor weapon in sync so attribute + Mob Killer match. */
        if (actor != null)
        {
            ItemStack main = actor.getMainHandItem();

            if (!ItemStack.matches(player.getMainHandItem(), main))
            {
                player.setItemInHand(InteractionHand.MAIN_HAND, main.copy());
            }
        }
        else if (replay != null)
        {
            ItemStack main = replay.keyframes.mainHand.interpolate(tick, ItemStack.EMPTY);

            player.setItemInHand(InteractionHand.MAIN_HAND, main == null ? ItemStack.EMPTY : main.copy());
        }

        LivingEntity damageSource = actor != null ? actor : player;
        Entity hit = this.resolveTarget(film, damageSource, player, lookYaw, lookPitch);

        if (hit == null)
        {
            return;
        }

        AttackDamage.applyHit(damageSource, hit, this.damage.get());
    }

    private Entity resolveTarget(Film film, LivingEntity damageSource, SuperFakePlayer player, float lookYaw, float lookPitch)
    {
        String targetId = this.target.get();

        if (targetId != null && !targetId.isEmpty() && film != null)
        {
            ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(film.getId());
            LivingEntity bound = actionPlayer == null ? null : actionPlayer.getActor(targetId);

            if (bound != null && !bound.isRemoved() && !bound.isDeadOrDying() && bound.getHealth() > 0F && bound.deathTime <= 0 && bound != damageSource)
            {
                return bound;
            }
        }

        double distance = 6D;
        Vec3 origin = player.getEyePosition(1F);
        Vec3 rotation = this.getLookVector(lookPitch, lookYaw);
        Vec3 end = origin.add(rotation.x * distance, rotation.y * distance, rotation.z * distance);
        HitResult blockHit = player.pick(distance, 1F, false);
        double maxDistSq = blockHit != null ? blockHit.getLocation().distanceToSqr(origin) : distance * distance;
        AABB box = player.getBoundingBox().expandTowards(rotation.scale(distance)).inflate(1D, 1D, 1D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(damageSource, origin, end, box,
            entity -> !entity.isSpectator() && entity.isPickable(), maxDistSq);

        if (entityHit != null && entityHit.getEntity() != null)
        {
            return entityHit.getEntity();
        }

        /* World raycast can miss film actors when look is slightly off; scan
         * ActionPlayer bodies with a small AABB pad (same combat session only). */
        return this.findFilmActorAlongRay(film, damageSource, origin, end, maxDistSq);
    }

    private Entity findFilmActorAlongRay(Film film, LivingEntity exclude, Vec3 origin, Vec3 end, double maxDistSq)
    {
        if (film == null)
        {
            return null;
        }

        ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(film.getId());

        if (actionPlayer == null)
        {
            return null;
        }

        Entity best = null;
        double bestDist = maxDistSq;

        for (Map.Entry<String, LivingEntity> entry : actionPlayer.getActors().entrySet())
        {
            LivingEntity entity = entry.getValue();

            if (entity == null || entity == exclude || entity.isRemoved() || entity.isDeadOrDying() || entity.getHealth() <= 0F || entity.deathTime > 0 || !entity.isPickable())
            {
                continue;
            }

            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.35D).clip(origin, end);

            if (hit.isEmpty())
            {
                continue;
            }

            double dist = origin.distanceToSqr(hit.get());

            if (dist < bestDist)
            {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
    }

    private Vec3 getLookVector(float pitch, float yaw)
    {
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float yawRad = -yaw * ((float) Math.PI / 180F);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);

        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    @Override
    protected Clip create()
    {
        return new AttackActionClip();
    }
}
