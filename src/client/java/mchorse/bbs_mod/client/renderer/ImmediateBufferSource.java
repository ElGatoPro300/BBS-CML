package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
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
        /* Only one unfinished builder may write into the shared fallback buffer. */
        if (!this.fixedBuffers.containsKey(layer) && this.lastLayer != layer)
        {
            this.endLastBatch();
        }

        BufferBuilder builder = this.startedBuilders.get(layer);

        if (builder == null)
        {
            ByteBufferBuilder byteBuffer = this.fixedBuffers.getOrDefault(layer, this.fallback);

            builder = new BufferBuilder(byteBuffer, layer.primitiveTopology(), layer.format());
            this.startedBuilders.put(layer, builder);
        }

        if (!this.fixedBuffers.containsKey(layer))
        {
            this.lastLayer = layer;
        }

        return builder;
    }

    @Override
    public void endBatch()
    {
        if (this.startedBuilders.isEmpty())
        {
            this.lastLayer = null;
            return;
        }

        Map<RenderType, BufferBuilder> copy = new LinkedHashMap<>(this.startedBuilders);
        this.startedBuilders.clear();
        this.lastLayer = null;

        for (Map.Entry<RenderType, BufferBuilder> entry : copy.entrySet())
        {
            RenderType layer = entry.getKey();
            BufferBuilder builder = entry.getValue();

            this.draw(builder, layer);
        }
    }

    @Override
    public void endBatch(RenderType layer)
    {
        BufferBuilder builder = this.startedBuilders.remove(layer);

        if (builder != null)
        {
            this.draw(builder, layer);
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

    private void draw(BufferBuilder builder, RenderType layer)
    {
        if (builder != null)
        {
            CustomVertexConsumerProvider.drawLayer(layer);
            Draw.flush(builder, layer);
        }
    }
}
