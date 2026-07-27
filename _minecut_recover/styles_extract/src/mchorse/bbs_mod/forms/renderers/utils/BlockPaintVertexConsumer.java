package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_4588;

public class BlockPaintVertexConsumer extends RecolorVertexConsumer
{
    public BlockPaintVertexConsumer(class_4588 consumer, Color color, Color paintColor)
    {
        super(consumer, color, paintColor);
    }

    @Override
    public class_4588 method_1336(int red, int green, int blue, int alpha)
    {
        Color vertex = new Color(red / 255F, green / 255F, blue / 255F, alpha / 255F);

        vertex.mul(this.color);

        if (this.paintColor != null)
        {
            FormColorBlend.applyPaintBlend(vertex, this.paintColor, this.paintColor.a);
        }

        red = MathUtils.clamp((int) (vertex.r * 255F), 0, 255);
        green = MathUtils.clamp((int) (vertex.g * 255F), 0, 255);
        blue = MathUtils.clamp((int) (vertex.b * 255F), 0, 255);
        alpha = MathUtils.clamp((int) (vertex.a * 255F), 0, 255);

        return this.consumer.method_1336(red, green, blue, alpha);
    }

    @Override
    public class_4588 method_22915(float red, float green, float blue, float alpha)
    {
        Color vertex = new Color(red, green, blue, alpha);

        vertex.mul(this.color);

        if (this.paintColor != null)
        {
            FormColorBlend.applyPaintBlend(vertex, this.paintColor, this.paintColor.a);
        }

        return this.consumer.method_22915(vertex.r, vertex.g, vertex.b, vertex.a);
    }
}
