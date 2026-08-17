package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

import org.lwjgl.system.MemoryStack;

public class GlowEmissionVertexSodiumConsumer extends GlowEmissionVertexConsumer implements VertexBufferWriter
{
    public GlowEmissionVertexSodiumConsumer(VertexConsumer consumer, Color color)
    {
        super(consumer, color);

        emissionColor = color;
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
