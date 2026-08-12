package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.dashboard.WorldPropertiesHelper;

import net.minecraft.client.world.ClientWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin
{
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    public void onGetTimeOfDay(CallbackInfoReturnable<Long> info)
    {
        Long worldTime = WorldPropertiesHelper.getClientTimeOverride();

        if (worldTime != null)
        {
            info.setReturnValue(worldTime);

            return;
        }

        Long timeOfDay = BBSRendering.getTimeOfDay();

        if (timeOfDay != null)
        {
            info.setReturnValue(timeOfDay);
        }
    }
}