package mchorse.bbs_mod.utils.sodium;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexSodiumConsumer;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_4588;

public class SodiumUtils
{
    public static class_4588 createVertexBuffer(class_4588 b, Color color)
    {
        return new RecolorVertexSodiumConsumer(b, color);
    }

    public static class_4588 createVertexBuffer(class_4588 b, Color color, Color paintColor)
    {
        return new RecolorVertexSodiumConsumer(b, color, paintColor);
    }
}