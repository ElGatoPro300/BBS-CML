package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_4588;
import net.minecraft.class_765;
import org.joml.Matrix4f;

public class GlowEmissionVertexConsumer implements class_4588
{
    public static Color emissionColor;

    protected class_4588 consumer;
    protected Color color;

    public GlowEmissionVertexConsumer(class_4588 consumer, Color color)
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
        int r = MathUtils.clamp((int) (this.color.r * 255F), 0, 255);
        int g = MathUtils.clamp((int) (this.color.g * 255F), 0, 255);
        int b = MathUtils.clamp((int) (this.color.b * 255F), 0, 255);
        int a = MathUtils.clamp((int) (this.color.a * alpha), 0, 255);

        return this.consumer.method_1336(r, g, b, a);
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
