package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Colors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex")
public class EntityVertexMixin
{
    @ModifyVariable(method = "write2", at = @At("HEAD"), ordinal = 0, remap = false, require = 0)
    private static int onWrite2(int color)
    {
        if (RecolorVertexConsumer.newColor != null)
        {
            Colors.COLOR.set(color);
            Colors.COLOR.mul(RecolorVertexConsumer.newColor);

            return Colors.COLOR.getARGBColor();
        }

        return color;
    }
}
