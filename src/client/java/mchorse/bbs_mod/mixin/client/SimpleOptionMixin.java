package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.dashboard.WorldPropertiesHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class SimpleOptionMixin
{
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    public void onGetValue(CallbackInfoReturnable info)
    {
        OptionInstance option = (OptionInstance) (Object) this;

        if (Minecraft.getInstance().options != null && option == Minecraft.getInstance().options.gamma())
        {
            Double value = BBSRendering.getBrightness();

            if (value == null)
            {
                /* World Properties' gamma tool; the film brightness curve takes priority. */
                value = WorldPropertiesHelper.getGammaOverride();
            }

            if (value != null)
            {
                info.setReturnValue(value);
            }
        }
    }
}