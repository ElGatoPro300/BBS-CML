package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1309.class)
public class LivingEntityMixin
{
    @Inject(method = "applyDamage", at = @At("HEAD"))
    public void onApplyDamage(class_1282 source, float amount, CallbackInfo info)
    {
        class_1297 attacker = source.method_5529();

        if (source.method_60489() && attacker != null && attacker.getClass() == class_3222.class)
        {
            BBSMod.getActions().addAction((class_3222) attacker, () ->
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(amount);

                return clip;
            });
        }
    }

    /**
     * LivingEntity overrides {@link class_1297#method_49476()}, so the boost must live here
     * (not on Entity) or players never receive the higher step for short solid hitboxes.
     */
    @Inject(method = "getStepHeight", at = @At("RETURN"), cancellable = true)
    private void bbs$boostSolidHitboxStepHeight(CallbackInfoReturnable<Float> info)
    {
        class_1297 entity = (class_1297) (Object) this;
        float boosted = ModelBlockSolidCollisions.boostStepHeight(entity, info.getReturnValueF());

        if (boosted > info.getReturnValueF())
        {
            info.setReturnValue(boosted);
        }
    }

    /* @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    public void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo info)
    {
        info.cancel();
    } */
}