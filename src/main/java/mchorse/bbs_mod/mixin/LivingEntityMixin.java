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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Inject(method = "applyDamage", at = @At("HEAD"))
    public void onApplyDamage(DamageSource source, float amount, CallbackInfo info)
    {
        Entity attacker = source.getAttacker();

        if (source.isDirect() && attacker instanceof ServerPlayerEntity player)
        {
            LivingEntity target = (LivingEntity) (Object) this;
            float recorded = Math.max(amount, AttackDamage.fromAttacker(player, target));

            BBSMod.getActions().addAction(player, () ->
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(recorded);

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
}
