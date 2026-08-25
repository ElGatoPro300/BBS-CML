package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;

import org.joml.Matrix4f;

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
    public VertexConsumer vertex(double x, double y, double z)
    {
        return this.consumer.vertex(x, y, z);
    }

    @Override
    public void next()
    {
        this.consumer.next();
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha)
    {
        this.consumer.fixedColor(red, green, blue, alpha);
    }

    @Override
    public void unfixColor()
    {
        this.consumer.unfixColor();
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
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

        return this.consumer.color(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        return this.consumer.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        return this.consumer.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        return this.consumer.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this.consumer.normal(x, y, z);
    }

}
