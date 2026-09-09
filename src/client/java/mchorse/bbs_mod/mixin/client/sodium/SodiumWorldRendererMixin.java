package mchorse.bbs_mod.mixin.client.sodium;

import mchorse.bbs_mod.client.BBSRendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

import com.mojang.blaze3d.textures.GpuSampler;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Pseudo
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class SodiumWorldRendererMixin
{
    @Inject(method = "drawChunkLayer", at = @At("HEAD"), cancellable = true, require = 0)
    public void onDrawChunkLayer(ChunkSectionLayerGroup group, ChunkRenderMatrices matrices, double x, double y, double z, GpuSampler sampler, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            info.cancel();
        }
    }
}
