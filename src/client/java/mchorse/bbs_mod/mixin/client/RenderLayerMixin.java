package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public class RenderLayerMixin
{
    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;startDrawing()V", ordinal = 0, shift = At.Shift.AFTER))
    public void onDraw(BufferBuilder buffer, VertexSorter sorter, CallbackInfo info)
    {
        CustomVertexConsumerProvider.drawLayer((RenderLayer) (Object) this);
    }
}