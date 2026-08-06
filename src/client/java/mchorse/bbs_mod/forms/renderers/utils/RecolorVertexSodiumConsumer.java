package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.mixin.client.sodium.SodiumBufferBuilderAccessor;
import mchorse.bbs_mod.utils.colors.Color;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;

public class RecolorVertexSodiumConsumer extends RecolorVertexConsumer implements VertexBufferWriter
{
    public RecolorVertexSodiumConsumer(VertexConsumer consumer, Color color)
    {
        super(consumer, color);

        newColor = color;
    }

    @Override
    public void push(MemoryStack memoryStack, long l, int i, VertexFormatDescription vertexFormatDescription)
    {
        if (this.consumer instanceof SodiumBufferBuilderAccessor accessor)
        {
            accessor.bbs$getBuilder().push(memoryStack, l, i, vertexFormatDescription);
        }
    }
}