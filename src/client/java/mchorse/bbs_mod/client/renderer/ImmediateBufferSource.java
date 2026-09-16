package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.graphics.Draw;

import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

public class ImmediateBufferSource implements MultiBufferSource.BufferSource
{
    private final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
    private final ByteBufferBuilder fallback;
    private final Map<RenderType, BufferBuilder> startedBuilders = new LinkedHashMap<>();
    private RenderType lastLayer;

    public ImmediateBufferSource(SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers, ByteBufferBuilder fallback)
    {
        this.fixedBuffers = fixedBuffers;
        this.fallback = fallback;
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer)
    {
        BufferBuilder builder = this.startedBuilders.get(layer);

        if (builder == null)
        {
            ByteBufferBuilder byteBuffer = this.fixedBuffers.getOrDefault(layer, this.fallback);

            builder = new BufferBuilder(byteBuffer, layer.primitiveTopology(), layer.format());
            this.startedBuilders.put(layer, builder);
        }

        this.lastLayer = layer;

        return builder;
    }

    @Override
    public void endBatch()
    {
        for (Map.Entry<RenderType, BufferBuilder> entry : this.startedBuilders.entrySet())
        {
            RenderType layer = entry.getKey();
            BufferBuilder builder = entry.getValue();

            Draw.flush(builder, layer);
        }

        this.startedBuilders.clear();
        this.lastLayer = null;
    }

    @Override
    public void endBatch(RenderType layer)
    {
        BufferBuilder builder = this.startedBuilders.remove(layer);

        if (builder != null)
        {
            Draw.flush(builder, layer);
        }

        if (this.lastLayer == layer)
        {
            this.lastLayer = null;
        }
    }

    @Override
    public void endLastBatch()
    {
        if (this.lastLayer != null)
        {
            this.endBatch(this.lastLayer);
        }
    }
}
