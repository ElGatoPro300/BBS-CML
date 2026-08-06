package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

import org.lwjgl.system.MemoryStack;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

public class TextGlowEmissionVertexSodiumConsumer extends TextGlowEmissionVertexConsumer implements VertexBufferWriter
{
    public TextGlowEmissionVertexSodiumConsumer(VertexConsumer consumer, Color color)
    {
        super(consumer, color);
    }

    @Override
    public void push(MemoryStack memoryStack, long l, int i, VertexFormatDescription vertexFormat)
    {
        if (this.consumer instanceof VertexBufferWriter writer)
        {
            writer.push(memoryStack, l, i, vertexFormat);
        }
    }
}
