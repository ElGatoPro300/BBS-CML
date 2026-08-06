package mchorse.bbs_mod.utils.sodium;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexSodiumConsumer;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.render.VertexConsumer;

public class SodiumUtils
{
    public static VertexConsumer createVertexBuffer(VertexConsumer b, Color color)
    {
        return new RecolorVertexSodiumConsumer(b, color);
    }
}