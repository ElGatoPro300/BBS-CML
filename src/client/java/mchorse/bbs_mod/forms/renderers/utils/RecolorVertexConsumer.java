package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

import org.joml.Matrix4fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class RecolorVertexConsumer implements VertexConsumer
{
    public static Color newColor;
    public static Color newPaintColor;

    protected VertexConsumer consumer;
    protected Color color;
    protected Color paintColor;

    public RecolorVertexConsumer(VertexConsumer consumer, Color color)
    {
        this(consumer, color, null);
    }

    public RecolorVertexConsumer(VertexConsumer consumer, Color color, Color paintColor)
    {
        this.consumer = consumer;
        this.color = color;
        this.paintColor = paintColor;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z)
    {
        return this.consumer.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z)
    {
        return this.consumer.addVertex(matrix, x, y, z);
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
    {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = MathUtils.clamp((int) (this.color.r * r), 0, 255);
        g = MathUtils.clamp((int) (this.color.g * g), 0, 255);
        b = MathUtils.clamp((int) (this.color.b * b), 0, 255);
        a = MathUtils.clamp((int) (this.color.a * a), 0, 255);

        if (this.paintColor != null)
        {
            int[] rgb = { r, g, b };

            FormColorEffects.applyPaintBlendToBytes(rgb, this.paintColor);
            r = MathUtils.clamp(rgb[0], 0, 255);
            g = MathUtils.clamp(rgb[1], 0, 255);
            b = MathUtils.clamp(rgb[2], 0, 255);
        }

        int recolored = (a << 24) | (r << 16) | (g << 8) | b;

        this.consumer.addVertex(x, y, z, recolored, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        red = MathUtils.clamp((int) (this.color.r * red), 0, 255);
        green = MathUtils.clamp((int) (this.color.g * green), 0, 255);
        blue = MathUtils.clamp((int) (this.color.b * blue), 0, 255);
        alpha = MathUtils.clamp((int) (this.color.a * alpha), 0, 255);

        int[] rgb = { red, green, blue };

        FormColorEffects.applyPaintBlendToBytes(rgb, this.paintColor);
        red = MathUtils.clamp(rgb[0], 0, 255);
        green = MathUtils.clamp(rgb[1], 0, 255);
        blue = MathUtils.clamp(rgb[2], 0, 255);

        return this.consumer.setColor(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer setUv(float u, float v)
    {
        return this.consumer.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v)
    {
        return this.consumer.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v)
    {
        return this.consumer.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z)
    {
        return this.consumer.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int argb)
    {
        return this.consumer.setColor(argb);
    }

    @Override
    public VertexConsumer setLineWidth(float width)
    {
        return this.consumer.setLineWidth(width);
    }
}
