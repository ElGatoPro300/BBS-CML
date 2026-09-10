package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class WorldMixin
{
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    public void onGetRainGradient(CallbackInfoReturnable<Float> info)
    {
        Double rainFactor = BBSRendering.getWeather();

        if (rainFactor != null)
        {
            info.setReturnValue(rainFactor.floatValue());
        }
    }
}