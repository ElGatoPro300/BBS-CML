package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.client.render.BufferRenderer;

import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Compatibility adapter providing the MultiBufferSource interface for 26.2.
 */
public interface MultiBufferSource
{
    public VertexConsumer getBuffer(RenderType renderType);

    public static BufferSource immediate(ByteBufferBuilder fallbackBuffer)
    {
        return immediateWithBuffers(new HashMap<>(), fallbackBuffer);
    }

    public static BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers, ByteBufferBuilder fallbackBuffer)
    {
        return new BufferSource(fallbackBuffer, fixedBuffers);
    }

    public static BufferSource immediateWithBuffers(Map<RenderType, ByteBufferBuilder> fixedBuffers, ByteBufferBuilder fallbackBuffer)
    {
        return new BufferSource(fallbackBuffer, fixedBuffers);
    }

    public static class BufferSource implements MultiBufferSource
    {
        protected final ByteBufferBuilder fallbackBuffer;
        protected final Map<RenderType, ByteBufferBuilder> fixedBuffers;
        protected final Map<RenderType, BufferBuilder> startedBuilders = new LinkedHashMap<>();
        protected RenderType lastStartedType;

        public BufferSource(ByteBufferBuilder fallbackBuffer, Map<RenderType, ByteBufferBuilder> fixedBuffers)
        {
            this.fallbackBuffer = fallbackBuffer;
            this.fixedBuffers = fixedBuffers;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType)
        {
            BufferBuilder builder = this.startedBuilders.get(renderType);

            if (builder != null)
            {
                return builder;
            }

            ByteBufferBuilder bufferBuilder = this.fixedBuffers.get(renderType);

            if (bufferBuilder == null)
            {
                /* Only one builder may write into the shared fallback allocation. */
                this.endLastBatch();
                bufferBuilder = this.fallbackBuffer;
                this.lastStartedType = renderType;
            }

            builder = new BufferBuilder(bufferBuilder, renderType.primitiveTopology(), renderType.format());
            this.startedBuilders.put(renderType, builder);

            return builder;
        }

        public void endLastBatch()
        {
            if (this.lastStartedType != null)
            {
                this.endBatch(this.lastStartedType);
            }
        }

        public void endBatch()
        {
            this.endLastBatch();

            for (RenderType type : new ArrayList<>(this.startedBuilders.keySet()))
            {
                this.endBatch(type);
            }
        }

        public void endBatch(RenderType renderType)
        {
            BufferBuilder builder = this.startedBuilders.remove(renderType);

            if (builder != null)
            {
                if (this.lastStartedType == renderType)
                {
                    this.lastStartedType = null;
                }

                MeshData meshData = builder.build();

                if (meshData != null)
                {
                    try
                    {
                        if (renderType.sortOnUpload() && renderType.primitiveTopology() == PrimitiveTopology.QUADS)
                        {
                            meshData.sortQuads(this.fixedBuffers.getOrDefault(renderType, this.fallbackBuffer),
                                RenderSystem.getProjectionType().vertexSorting());
                        }
                    }
                    catch (RuntimeException | Error exception)
                    {
                        meshData.close();

                        throw exception;
                    }

                    BufferRenderer.draw(renderType, meshData);
                }
            }
        }
    }
}
