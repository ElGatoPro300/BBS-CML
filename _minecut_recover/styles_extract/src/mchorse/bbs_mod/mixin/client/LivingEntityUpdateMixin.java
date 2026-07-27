package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.selectors.ISelectorOwnerProvider;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_1309.class)
public class LivingEntityUpdateMixin
{
    @Inject(method = "baseTick", at = @At("TAIL"))
    public void onBaseTick(CallbackInfo info)
    {
        ((ISelectorOwnerProvider) this).getOwner().update();
    }
}