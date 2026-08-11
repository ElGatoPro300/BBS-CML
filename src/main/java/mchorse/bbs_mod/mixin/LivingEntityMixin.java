package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.AttackDamage;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    /**
     * Record the amount passed into {@link LivingEntity#damage} after a successful
     * hit. That value already includes vanilla attack cooldown, critical hits,
     * strength, and weapon enchants — do <b>not</b> replace it with full weapon
     * damage ({@link AttackDamage#fromAttacker}), or spam-clicks replay as full hits.
     */
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info)
    {
        if (!Boolean.TRUE.equals(info.getReturnValue()))
        {
            return;
        }

        Entity attacker = source.getAttacker();

        if (!source.isDirect() || !(attacker instanceof ServerPlayerEntity player))
        {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        float recorded = amount;

        if (AttackDamage.isMobKiller(player.getMainHandStack()))
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
    }
}
