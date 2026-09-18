package mchorse.bbs_mod.mixin.client.sodium;

import mchorse.bbs_mod.client.BBSRendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

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
    private void onDrawChunkLayer(CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            info.cancel();
        }
    }
}
