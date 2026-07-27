package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import net.minecraft.class_6360;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_6360.class)
public class ResourceReloadLoggerMixin
{
    @Inject(method = "finish", at = @At("TAIL"))
    public void onOnFinishedLoading(CallbackInfo info)
    {
        BBSModClient.getSounds().deleteSounds();
    }
}