package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.renderers.MobFormRenderer;

import net.minecraft.client.model.ModelPart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelPart.class)
public class ModelPartMixin
{
    @ModifyVariable(
        method = "render",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int bbs$mobStencilPickLight(int light)
    {
        return MobFormRenderer.getStencilPickOffset((ModelPart) (Object) this, light);
    }
}
