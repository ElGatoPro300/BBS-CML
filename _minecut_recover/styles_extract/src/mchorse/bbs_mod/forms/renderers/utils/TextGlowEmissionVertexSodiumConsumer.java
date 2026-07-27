package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.class_293;
import net.minecraft.class_4588;
import org.lwjgl.system.MemoryStack;

public class TextGlowEmissionVertexSodiumConsumer extends TextGlowEmissionVertexConsumer implements VertexBufferWriter
{
    public TextGlowEmissionVertexSodiumConsumer(class_4588 consumer, Color color)
    {
        super(consumer, color);
    }

    @Override
    public void push(MemoryStack memoryStack, long l, int i, class_293 vertexFormat)
    {
        if (this.consumer instanceof VertexBufferWriter writer)
        {
            writer.push(memoryStack, l, i, vertexFormat);
        }
    }
}
