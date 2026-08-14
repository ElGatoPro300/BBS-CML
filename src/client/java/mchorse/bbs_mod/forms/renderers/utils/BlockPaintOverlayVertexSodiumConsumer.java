package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.sodium.SodiumUtils;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

import org.lwjgl.system.MemoryStack;

public class BlockPaintOverlayVertexSodiumConsumer extends BlockPaintOverlayVertexConsumer implements VertexBufferWriter
{
    public BlockPaintOverlayVertexSodiumConsumer(VertexConsumer consumer, Color paintColor)
    {
        super(consumer, paintColor);

        paintOverlayColor = paintColor;
    }

    @Override
    public boolean canUseIntrinsics()
    {
        return SodiumUtils.canUseIntrinsics(this.consumer);
    }

    @Override
    public void push(MemoryStack memoryStack, long l, int i, VertexFormat vertexFormat)
    {
        SodiumUtils.push(this.consumer, memoryStack, l, i, vertexFormat);
    }
}
