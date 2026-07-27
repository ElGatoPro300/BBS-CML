package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.dashboard.WorldPropertiesHelper;
import net.minecraft.class_310;
import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_7172.class)
public class SimpleOptionMixin
{
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    public void onGetValue(CallbackInfoReturnable info)
    {
        class_7172 option = (class_7172) (Object) this;

        if (class_310.method_1551().field_1690 != null && option == class_310.method_1551().field_1690.method_42473())
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