package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;

import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.vertex.MeshData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public class RenderLayerMixin
{
    @Inject(method = "draw", at = @At("HEAD"))
    public void onDraw(MeshData buffer, CallbackInfo info)
    {
        CustomVertexConsumerProvider.drawLayer((RenderType) (Object) this);
    }
}