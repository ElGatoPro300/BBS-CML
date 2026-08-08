package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;

import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public class RenderLayerMixin
{
    @Inject(method = "draw", at = @At("HEAD"))
    public void onDraw(BuiltBuffer buffer, CallbackInfo info)
    {
        CustomVertexConsumerProvider.drawLayer((RenderLayer) (Object) this);
    }
}