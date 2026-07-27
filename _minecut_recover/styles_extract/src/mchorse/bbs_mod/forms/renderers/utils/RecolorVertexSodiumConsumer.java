package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.class_293;
import net.minecraft.class_4588;
import org.lwjgl.system.MemoryStack;

public class RecolorVertexSodiumConsumer extends RecolorVertexConsumer implements VertexBufferWriter
{
    public RecolorVertexSodiumConsumer(class_4588 consumer, Color color)
    {
        this(consumer, color, null);
    }

    public RecolorVertexSodiumConsumer(class_4588 consumer, Color color, Color paintColor)
    {
        super(consumer, color, paintColor);

        newColor = color;
        newPaintColor = paintColor != null && paintColor.a != 0F ? paintColor : null;
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
