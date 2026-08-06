package mchorse.bbs_mod.mixin.client.sodium;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Colors;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute")
public class ColorAttributeMixin
{
    @ModifyVariable(method = "set", at = @At("HEAD"), ordinal = 0, remap = false)
    private static int onSet(int color)
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