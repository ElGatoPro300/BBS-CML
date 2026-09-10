package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.renderer.LightTexture;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class BlockPaintOverlayVertexConsumer implements VertexConsumer
{
    public static Color paintOverlayColor;

    protected VertexConsumer consumer;
    protected Color paintColor;
    protected float strength;

    public BlockPaintOverlayVertexConsumer(VertexConsumer consumer, Color paintColor)
    {
        this.consumer = consumer;
        this.paintColor = paintColor;
        this.strength = paintColor.a;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z)
    {
        return this.consumer.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        int r = MathUtils.clamp((int) (this.paintColor.r * 255F), 0, 255);
        int g = MathUtils.clamp((int) (this.paintColor.g * 255F), 0, 255);
        int b = MathUtils.clamp((int) (this.paintColor.b * 255F), 0, 255);
        int a = MathUtils.clamp((int) (this.strength * alpha), 0, 255);

        return this.consumer.setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(int argb)
    {
        return this.consumer.setColor(argb);
    }

    @Override
    public VertexConsumer setColor(float red, float green, float blue, float alpha)
    {
        float r = MathUtils.clamp(this.paintColor.r * red, 0F, 1F);
        float g = MathUtils.clamp(this.paintColor.g * green, 0F, 1F);
        float b = MathUtils.clamp(this.paintColor.b * blue, 0F, 1F);
        float a = MathUtils.clamp(this.strength * alpha, 0F, 1F);

        return this.consumer.setColor(r, g, b, a);
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
