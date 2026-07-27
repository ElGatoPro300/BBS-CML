package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import net.minecraft.class_1921;
import net.minecraft.class_4668;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_4668.class)
public class RenderLayerMixin
{
    @Inject(method = "startDrawing", at = @At("TAIL"))
    public void onStartDrawing(CallbackInfo info)
    {
        if ((Object) this instanceof class_1921)
        {
            CustomVertexConsumerProvider.drawLayer((class_1921) (Object) this);
        }
    }
}