package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.renderer.LightTexture;

import com.mojang.blaze3d.vertex.VertexConsumer;

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
    public VertexConsumer addVertex(float x, float y, float z)
    {
        return this.consumer.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        int r = MathUtils.clamp((int) (this.color.r * 255F), 0, 255);
        int g = MathUtils.clamp((int) (this.color.g * 255F), 0, 255);
        int b = MathUtils.clamp((int) (this.color.b * 255F), 0, 255);
        int a = MathUtils.clamp((int) (this.color.a * alpha), 0, 255);

        return this.consumer.setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(int argb)
    {
        return this.consumer.setColor(argb);
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
        return this.consumer.setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z)
    {
        return this.consumer.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width)
    {
        return this.consumer.setLineWidth(width);
    }
}
