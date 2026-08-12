package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;

import org.joml.Matrix4f;

public class GlowEmissionVertexConsumer implements VertexConsumer
{
    public static Color emissionColor;

    protected VertexConsumer consumer;
    protected Color color;

    public GlowEmissionVertexConsumer(VertexConsumer consumer, Color color)
    {
        this.consumer = consumer;
        this.color = color;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z)
    {
        return this.consumer.vertex(x, y, z);
    }

    @Override
    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z)
    {
        return this.consumer.vertex(matrix, x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        int r = MathUtils.clamp((int) (this.color.r * 255F), 0, 255);
        int g = MathUtils.clamp((int) (this.color.g * 255F), 0, 255);
        int b = MathUtils.clamp((int) (this.color.b * 255F), 0, 255);
        int a = MathUtils.clamp((int) (this.color.a * alpha), 0, 255);

        return this.consumer.color(r, g, b, a);
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
        return this.consumer.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this.consumer.normal(x, y, z);
    }
}
