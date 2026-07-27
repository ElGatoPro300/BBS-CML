package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_4588;
import org.joml.Matrix4f;

public class RecolorVertexConsumer implements class_4588
{
    public static Color newColor;
    public static Color newPaintColor;

    protected class_4588 consumer;
    protected Color color;
    protected Color paintColor;

    public RecolorVertexConsumer(class_4588 consumer, Color color)
    {
        this(consumer, color, null);
    }

    public RecolorVertexConsumer(class_4588 consumer, Color color, Color paintColor)
    {
        this.consumer = consumer;
        this.color = color;
        this.paintColor = paintColor;
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
        red = (int) (this.color.r * red);
        green = (int) (this.color.g * green);
        blue = (int) (this.color.b * blue);
        alpha = (int) (this.color.a * alpha);

        int[] rgb = { red, green, blue };

        FormColorBlend.applyPaintBlendToBytes(rgb, this.paintColor);
        red = rgb[0];
        green = rgb[1];
        blue = rgb[2];

        return this.consumer.method_1336(red, green, blue, alpha);
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
        return this.consumer.method_22921(u, v);
    }

    @Override
    public class_4588 method_22914(float x, float y, float z)
    {
        return this.consumer.method_22914(x, y, z);
    }

}
