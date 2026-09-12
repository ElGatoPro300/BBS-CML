package mchorse.bbs_mod.mixin.client.audio;

import com.mojang.blaze3d.audio.CallbackDeviceTracker;

import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CallbackDeviceTracker.class)
public class CallbackDeviceTrackerMixin
{
    @Inject(method = "isSupported", at = @At("HEAD"), cancellable = true)
    private static void bbs$isSupported(CallbackInfoReturnable<Boolean> cir)
    {
        try
        {
            ALCCapabilities caps = ALC.getCapabilities();

            if (caps == null || !caps.ALC_SOFT_system_events || caps.alcEventIsSupportedSOFT == 0L || caps.alcEventControlSOFT == 0L || caps.alcEventCallbackSOFT == 0L)
            {
                cir.setReturnValue(false);
            }
        }
        catch (Throwable t)
        {
            cir.setReturnValue(false);
        }
    }
}
