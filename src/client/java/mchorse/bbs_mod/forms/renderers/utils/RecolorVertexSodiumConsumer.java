package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

import org.lwjgl.system.MemoryStack;

public class RecolorVertexSodiumConsumer extends RecolorVertexConsumer
{
    public RecolorVertexSodiumConsumer(VertexConsumer consumer, Color color)
    {
        this(consumer, color, null);
    }

    public RecolorVertexSodiumConsumer(VertexConsumer consumer, Color color, Color paintColor)
    {
        super(consumer, color, paintColor);
    }
}
