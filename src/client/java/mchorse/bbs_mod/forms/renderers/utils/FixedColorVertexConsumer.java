package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;

import org.joml.Matrix4fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * VertexConsumer que fija un color constante (incluido alpha) en el
 * Buffer subyacente mediante {@link VertexConsumer#fixedColor}.
 *
 * Útil para casos donde el renderer nunca llama a {@link VertexConsumer#setColor},
 * como muchos Block Entity renderers; así la transparencia global se aplica
 * igualmente.
 */
public class FixedColorVertexConsumer implements VertexConsumer
{
    private final VertexConsumer delegate;
    private final Color color;
    private final int r, g, b, a;

    public FixedColorVertexConsumer(VertexConsumer delegate, Color color)
    {
        this.delegate = delegate;
        this.color = color;
        this.r = (int)(color.r * 255f);
        this.g = (int)(color.g * 255f);
        this.b = (int)(color.b * 255f);
        this.a = (int)(color.a * 255f);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z)
    {
        return this.delegate.addVertex(x, y, z).setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z)
    {
        return this.delegate.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        return this.delegate.setColor(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer setUv(float u, float v)
    {
        return this.delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v)
    {
        return this.delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v)
    {
        return this.delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z)
    {
        return this.delegate.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int argb)
    {
        return this.delegate.setColor(argb);
    }

    @Override
    public VertexConsumer setLineWidth(float width)
    {
        return this.delegate.setLineWidth(width);
    }
}
