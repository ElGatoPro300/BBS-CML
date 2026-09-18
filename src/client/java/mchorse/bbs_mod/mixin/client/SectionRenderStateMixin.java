package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class SectionRenderStateMixin
{
    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    private void onRenderSection(CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            info.cancel();
        }
    }
}
