package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;

import net.minecraft.client.ResourceLoadStateTracker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public class ResourceReloadLoggerMixin
{
    @Inject(method = "finishReload", at = @At("TAIL"))
    public void onOnFinishedLoading(CallbackInfo info)
    {
        BBSModClient.getSounds().deleteSounds();
    }
}