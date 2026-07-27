package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.class_1937;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1937.class)
public class WorldMixin
{
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    public void onGetRainGradient(CallbackInfoReturnable<Float> info)
    {
        Double rainFactor = BBSRendering.getWeather();

        if (rainFactor != null)
        {
            info.setReturnValue(rainFactor.floatValue());
        }
    }
}