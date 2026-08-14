package mchorse.bbs_mod.utils.sodium;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexSodiumConsumer;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

import org.lwjgl.system.MemoryStack;

public class SodiumUtils
{
    public static VertexConsumer createVertexBuffer(VertexConsumer b, Color color)
    {
        return new RecolorVertexSodiumConsumer(b, color);
    }

    public static VertexConsumer createVertexBuffer(VertexConsumer b, Color color, Color paintColor)
    {
        return new RecolorVertexSodiumConsumer(b, color, paintColor);
    }

    /**
     * Always false. Sodium 0.8 {@code CubeMixin} calls {@code tryOf(wrapper, EntityVertex.FORMAT)};
     * {@code canUseIntrinsics(format)} defaults to this no-arg method, so following the inner
     * {@code BufferBuilder} packs trident/shield ENTITY verts into the wrong stride, throws
     * mid-{@code ModelPart.render}, and leaks the pose stack.
     */
    public static boolean canUseIntrinsics(VertexConsumer consumer)
    {
        return false;
    }

    public static void push(VertexConsumer consumer, MemoryStack stack, long pointer, int count, VertexFormat format)
    {
    }
}