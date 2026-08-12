package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.AttackDamage;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.network.ServerNetwork;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    /**
     * Record the amount passed into {@link LivingEntity#hurtServer} after a successful
     * hit. That value already includes vanilla attack cooldown, critical hits,
     * strength, and weapon enchants — do <b>not</b> replace it with full weapon
     * damage ({@link AttackDamage#fromAttacker}), or spam-clicks replay as full hits.
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void onDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> info)
    {
        if (!Boolean.TRUE.equals(info.getReturnValue()))
        {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = source.getEntity();

        /* Player melee → ActionRecorder on the player replay (existing path). */
        if (source.isDirect() && attacker instanceof ServerPlayer player)
        {
            float recorded = amount;

            if (AttackDamage.isMobKiller(player.getMainHandItem()))
            {
                recorded = AttackDamage.MOB_KILLER_DAMAGE;
            }
            else if (recorded < 0F)
            {
                recorded = 0F;
            }

            float damageToStore = recorded;

            BBSMod.getActions().addAction(player, () ->
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(damageToStore);

                if (target instanceof ActorEntity actorEntity)
                {
                    Replay replay = actorEntity.getReplay();

                    if (replay != null)
                    {
                        clip.target.set(replay.getId());
                    }
                }

                return clip;
            });

            return;
        }

        /* Mob autocapture combat clips (client places them on captured replays). */
        if (!(target.level() instanceof ServerLevel serverWorld))
        {
            return;
        }

        if (!BBSMod.getActions().hasActiveRecorders(serverWorld))
        {
            return;
        }

        float recorded = Math.max(0F, amount);
        byte kind;
        int sourceEntityId = -1;
        Entity sourceEntity = source.getDirectEntity();

        if (source.is(DamageTypes.THORNS))
        {
            kind = ServerNetwork.MOB_COMBAT_KIND_DAMAGE;
        }
        else if (sourceEntity instanceof Projectile projectile)
        {
            Entity owner = projectile.getOwner();

            if (owner != null)
            {
                kind = ServerNetwork.MOB_COMBAT_KIND_PROJECTILE;
                sourceEntityId = owner.getId();
            }
            else
            {
                kind = ServerNetwork.MOB_COMBAT_KIND_DAMAGE;
            }
        }
        else if (source.isDirect() && attacker instanceof LivingEntity && !(attacker instanceof Player))
        {
            kind = ServerNetwork.MOB_COMBAT_KIND_MELEE;
            sourceEntityId = attacker.getId();
        }
        else
        {
            /* Magic / environmental / other — Damage clip on the victim if captured. */
            kind = ServerNetwork.MOB_COMBAT_KIND_DAMAGE;
        }

        BBSMod.getActions().broadcastMobCombatHit(serverWorld, target.getId(), sourceEntityId, recorded, kind);
    }

    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
    public void onGetBaseDimensions(CallbackInfoReturnable<EntityDimensions> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                LivingEntity entity = (LivingEntity) (Object) this;
                EntityDimensions dimensions = info.getReturnValue();
                float height = form.hitboxHeight.get() * (entity.isShiftKeyDown() ? form.hitboxSneakMultiplier.get() : 1F);

                if (dimensions.fixed())
                {
                    info.setReturnValue(EntityDimensions.fixed(form.hitboxWidth.get(), height));
                }
                else
                {
                    info.setReturnValue(EntityDimensions.scalable(form.hitboxWidth.get(), height));
                }
            }
        }
    }
}
