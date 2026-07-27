package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_4588;
import net.minecraft.class_765;
import org.joml.Matrix4f;

/**
 * Glow overlay for text geometry. Multiplies glow emission with per-vertex text tint
 * so label glow matches the text color (custom fonts use {@code color(float)}).
 */
public class TextGlowEmissionVertexConsumer implements class_4588
{
    protected class_4588 consumer;
    protected Color color;

    public TextGlowEmissionVertexConsumer(class_4588 consumer, Color color)
    {
        this.consumer = consumer;
        this.color = color;
    }

    @Override
    public class_4588 method_22912(float x, float y, float z)
    {
        return this.consumer.method_22912(x, y, z);
    }

    @Override
    public class_4588 method_22918(Matrix4f matrix, float x, float y, float z)
    {
        return this.consumer.method_22918(matrix, x, y, z);
    }

    @Override
    public class_4588 method_1336(int red, int green, int blue, int alpha)
    {
        int r = MathUtils.clamp((int) (this.color.r * red), 0, 255);
        int g = MathUtils.clamp((int) (this.color.g * green), 0, 255);
        int b = MathUtils.clamp((int) (this.color.b * blue), 0, 255);
        int a = MathUtils.clamp((int) (this.color.a * alpha), 0, 255);

        return this.consumer.method_1336(r, g, b, a);
    }

    @Override
    public class_4588 method_22915(float red, float green, float blue, float alpha)
    {
        float r = MathUtils.clamp(this.color.r * red, 0F, 1F);
        float g = MathUtils.clamp(this.color.g * green, 0F, 1F);
        float b = MathUtils.clamp(this.color.b * blue, 0F, 1F);
        float a = MathUtils.clamp(this.color.a * alpha, 0F, 1F);

        return this.consumer.method_22915(r, g, b, a);
    }

    @Override
    public class_4588 method_22913(float u, float v)
    {
        return this.consumer.method_22913(u, v);
    }

    @Override
    public class_4588 method_60796(int u, int v)
    {
        return this.consumer.method_60796(u, v);
    }

    @Override
    public class_4588 method_22921(int u, int v)
    {
        return this.consumer.method_60803(class_765.field_32767);
    }

    @Override
    public class_4588 method_22914(float x, float y, float z)
    {
        return this.consumer.method_22914(x, y, z);
    }
}
