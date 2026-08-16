package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.AttackActionClip;
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

        if (!source.isIndirect() && attacker != null && attacker.getClass() == ServerPlayerEntity.class)
        {
            BBSMod.getActions().addAction((ServerPlayerEntity) attacker, () ->
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(amount);

                return clip;
            });
        }
    }

    /* @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    public void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo info)
    {
        info.cancel();
    } */
}