package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.ItemUseRenderState;
import mchorse.bbs_mod.selectors.ISelectorOwnerProvider;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityUpdateMixin
{
    @Inject(method = "baseTick", at = @At("TAIL"))
    public void onBaseTick(CallbackInfo info)
    {
        ((ISelectorOwnerProvider) this).getOwner().update();
    }

    @Inject(method = "tickActiveItemStack", at = @At("HEAD"), cancellable = true)
    private void bbs$skipDrivenFirstPersonItemUse(CallbackInfo info)
    {
        if ((Object) this instanceof ClientPlayerEntity && ItemUseRenderState.isDrivingLocalPlayerUse())
        {
            info.cancel();
        }
    }
}