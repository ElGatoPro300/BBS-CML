package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.dashboard.WorldPropertiesHelper;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class ClientWorldPropertiesMixin
{
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    public void onGetTotalTicks(Holder<WorldClock> clock, CallbackInfoReturnable<Long> info)
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