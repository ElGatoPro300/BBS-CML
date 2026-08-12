package mchorse.bbs_mod.forms.renderers.utils;

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
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        red = (int) (this.color.r * red);
        green = (int) (this.color.g * green);
        blue = (int) (this.color.b * blue);
        alpha = (int) (this.color.a * alpha);

        int[] rgb = { red, green, blue };

        FormColorEffects.applyPaintBlendToBytes(rgb, this.paintColor);
        red = rgb[0];
        green = rgb[1];
        blue = rgb[2];

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
