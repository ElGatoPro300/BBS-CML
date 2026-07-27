package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_4588;
import org.joml.Matrix4f;

/**
 * VertexConsumer que fija un color constante (incluido alpha) en el
 * Buffer subyacente mediante {@link class_4588#fixedColor}.
 *
 * Útil para casos donde el renderer nunca llama a {@link class_4588#method_39415},
 * como muchos Block Entity renderers; así la transparencia global se aplica
 * igualmente.
 */
public class FixedColorVertexConsumer implements class_4588
{
    private final class_4588 delegate;
    private final Color color;
    private final int r, g, b, a;

    public FixedColorVertexConsumer(class_4588 delegate, Color color)
    {
        this.delegate = delegate;
        this.color = color;
        this.r = (int)(color.r * 255f);
        this.g = (int)(color.g * 255f);
        this.b = (int)(color.b * 255f);
        this.a = (int)(color.a * 255f);
    }

    @Override
    public class_4588 method_22912(float x, float y, float z)
    {
        return this.delegate.method_22912(x, y, z).method_1336(r, g, b, a);
    }

    @Override
    public class_4588 method_22918(Matrix4f matrix, float x, float y, float z)
    {
        return this.delegate.method_22918(matrix, x, y, z).method_1336(r, g, b, a);
    }

    @Override
    public class_4588 method_1336(int red, int green, int blue, int alpha)
    {
        return this.delegate.method_1336(red, green, blue, alpha);
    }

    @Override
    public class_4588 method_22913(float u, float v)
    {
        return this.delegate.method_22913(u, v);
    }

    @Override
    public class_4588 method_60796(int u, int v)
    {
        return this.delegate.method_60796(u, v);
    }

    @Override
    public class_4588 method_22921(int u, int v)
    {
        return this.delegate.method_22921(u, v);
    }

    @Override
    public class_4588 method_22914(float x, float y, float z)
    {
        return this.delegate.method_22914(x, y, z);
    }

}
