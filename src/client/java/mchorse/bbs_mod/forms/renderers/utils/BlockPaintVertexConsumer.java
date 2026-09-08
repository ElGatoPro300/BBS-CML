package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;

public class BlockPaintVertexConsumer extends RecolorVertexConsumer
{
    public BlockPaintVertexConsumer(VertexConsumer consumer, Color color, Color paintColor)
    {
        super(consumer, color, paintColor);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        Color vertex = new Color(red / 255F, green / 255F, blue / 255F, alpha / 255F);

        vertex.mul(this.color);

        if (this.paintColor != null)
        {
            FormColorEffects.applyPaintBlend(vertex, this.paintColor, this.paintColor.a);
        }

        red = MathUtils.clamp((int) (vertex.r * 255F), 0, 255);
        green = MathUtils.clamp((int) (vertex.g * 255F), 0, 255);
        blue = MathUtils.clamp((int) (vertex.b * 255F), 0, 255);
        alpha = MathUtils.clamp((int) (vertex.a * 255F), 0, 255);

        return this.consumer.color(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha)
    {
        Color vertex = new Color(red, green, blue, alpha);

        vertex.mul(this.color);

        if (this.paintColor != null)
        {
            FormColorEffects.applyPaintBlend(vertex, this.paintColor, this.paintColor.a);
        }

        return this.consumer.color(vertex.r, vertex.g, vertex.b, vertex.a);
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
    {
        Color vertex = new Color(red, green, blue, alpha);

        vertex.mul(this.color);

        if (this.paintColor != null)
        {
            FormColorEffects.applyPaintBlend(vertex, this.paintColor, this.paintColor.a);
        }

        this.consumer.vertex(x, y, z, vertex.r, vertex.g, vertex.b, vertex.a, u, v, overlay, light, normalX, normalY, normalZ);
    }
}
