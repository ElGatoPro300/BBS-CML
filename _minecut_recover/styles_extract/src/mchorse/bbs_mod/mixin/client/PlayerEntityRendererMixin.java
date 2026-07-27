package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.class_1007;
import net.minecraft.class_1268;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_630;
import net.minecraft.class_742;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1007.class)
public class PlayerEntityRendererMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(class_742 abstractClientPlayerEntity, float f, float g, class_4587 matrixStack, class_4597 vertexConsumerProvider, int i, CallbackInfo info)
    {
        if (MorphRenderer.renderPlayer(abstractClientPlayerEntity, f, g, matrixStack, vertexConsumerProvider, i))
        {
            info.cancel();
        }
    }

    @Inject(method = "getPositionOffset", at = @At("HEAD"), cancellable = true)
    public void onPositionOffset(class_742 abstractClientPlayerEntity, float f, CallbackInfoReturnable<class_243> info)
    {
        Morph morph = Morph.getMorph(abstractClientPlayerEntity);

        if (morph != null && morph.getForm() != null)
        {
            info.setReturnValue(class_243.field_1353);
        }
    }

    @Inject(method = "renderArm", at = @At("HEAD"), cancellable = true)
    public void onRenderArmBegin(class_4587 matrices, class_4597 vertexConsumers, int light, class_742 player, class_630 arm, class_630 sleeve, CallbackInfo info)
    {
        Morph morph = Morph.getMorph(player);

        if (morph != null)
        {
            Form form = morph.getForm();

            if (form != null)
            {
                FormRenderer renderer = FormUtilsClient.getRenderer(form);
                class_1268 hand = ((class_1007) (Object) this).method_4038().field_3401 == arm ? class_1268.field_5808 : class_1268.field_5810;

                if (renderer != null && renderer.renderArm(matrices, light, player, hand))
                {
                    info.cancel();
                }
            }
        }
    }
}