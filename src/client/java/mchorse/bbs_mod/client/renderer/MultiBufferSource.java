package mchorse.bbs_mod.client.renderer;

import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public interface MultiBufferSource
{
    VertexConsumer getBuffer(RenderType renderType);

    public interface BufferSource extends MultiBufferSource
    {
        void endBatch();

        void endBatch(RenderType layer);

        void endLastBatch();
    }

    public static BufferSource immediate(ByteBufferBuilder fallback)
    {
        return new ImmediateBufferSource(new LinkedHashMap<>(), fallback);
    }

    public static BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> layers, ByteBufferBuilder fallback)
    {
        return new ImmediateBufferSource(layers, fallback);
    }
}
