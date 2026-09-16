package mchorse.bbs_mod.client.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

public class Tesselator
{
    private static final ByteBufferBuilder BUFFER = new ByteBufferBuilder(2 * 1024 * 1024);
    private static final Tesselator INSTANCE = new Tesselator();

    public static Tesselator getInstance()
    {
        return INSTANCE;
    }

    public BufferBuilder begin(PrimitiveTopology topology, VertexFormat format)
    {
        return new BufferBuilder(BUFFER, topology, format);
    }
}
