package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import com.mojang.blaze3d.textures.GpuSampler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class SectionRenderStateMixin
{
    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    public void onRenderSection(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            info.cancel();
        }
    }
}
