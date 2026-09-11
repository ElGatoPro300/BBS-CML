package mchorse.bbs_mod.client.render;

import mchorse.bbs_mod.forms.renderers.utils.ModelEffectPass;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Compatibility shim for 26.2 where vanilla {@code BufferRenderer} was removed.
 * Routes {@link MeshData} draws to appropriate {@link RenderType} pipelines.
 */
public class BufferRenderer
{
    private static RenderType defaultGuiTexturedLayer;
    private static RenderType defaultGuiLayer;
    private static RenderType defaultTranslucentParticleLayer;
    private static RenderType defaultLinesLayer;
    private static RenderType defaultDebugQuadsLayer;

    public static void drawWithGlobalProgram(MeshData buffer)
    {
        if (buffer == null)
        {
            return;
        }

        MeshData.DrawState params = buffer.drawState();

        if (params == null || params.vertexCount() == 0)
        {
            buffer.close();

            return;
        }

        if (ModelEffectPass.drawBound(buffer))
        {
            return;
        }

        RenderType layer = resolveLayer(params);

        draw(layer, buffer);
    }

    public static void draw(RenderType layer, MeshData buffer)
    {
        if (buffer == null)
        {
            return;
        }

        if (layer == null)
        {
            drawWithGlobalProgram(buffer);

            return;
        }

        MeshData.DrawState params = buffer.drawState();

        if (params == null || params.vertexCount() == 0)
        {
            buffer.close();

            return;
        }

        try
        {
            PreparedRenderType prepared = layer.prepare();
            GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> "BBS immediate vertices", GpuBuffer.USAGE_VERTEX, buffer.vertexBuffer());
            GpuBuffer indices;
            IndexType indexType;

            if (buffer.indexBuffer() == null)
            {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(params.primitiveTopology());
                indices = sequential.getBuffer(params.indexCount());
                indexType = sequential.type();
            }
            else
            {
                indices = RenderSystem.getDevice().createBuffer(() -> "BBS immediate indices", GpuBuffer.USAGE_INDEX, buffer.indexBuffer());
                indexType = params.indexType();
            }

            prepared.drawFromBuffer(vertices, indices, indexType, params.indexCount(), params.vertexCount(), 1);
        }
        finally
        {
            buffer.close();
        }
    }

    public static void draw(MeshData buffer)
    {
        drawWithGlobalProgram(buffer);
    }

    private static RenderType resolveLayer(MeshData.DrawState params)
    {
        VertexFormat format = params.format();
        PrimitiveTopology mode = params.primitiveTopology();

        if (mode == PrimitiveTopology.LINES || mode == PrimitiveTopology.DEBUG_LINES)
        {
            if (defaultLinesLayer == null)
            {
                defaultLinesLayer = RenderType.create("bbs_compat_lines", RenderSetup.builder(RenderPipelines.LINES).createRenderSetup());
            }

            return defaultLinesLayer;
        }

        if (format == DefaultVertexFormat.PARTICLE)
        {
            if (defaultTranslucentParticleLayer == null)
            {
                defaultTranslucentParticleLayer = RenderType.create("bbs_compat_particle", RenderSetup.builder(RenderPipelines.TRANSLUCENT_PARTICLE).createRenderSetup());
            }

            return defaultTranslucentParticleLayer;
        }

        if (format == DefaultVertexFormat.POSITION_COLOR)
        {
            if (defaultGuiLayer == null)
            {
                defaultGuiLayer = RenderType.create("bbs_compat_gui", RenderSetup.builder(RenderPipelines.GUI).createRenderSetup());
            }

            return defaultGuiLayer;
        }

        if (format == DefaultVertexFormat.POSITION)
        {
            if (defaultDebugQuadsLayer == null)
            {
                defaultDebugQuadsLayer = RenderType.create("bbs_compat_debug_quads", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup());
            }

            return defaultDebugQuadsLayer;
        }

        if (defaultGuiTexturedLayer == null)
        {
            defaultGuiTexturedLayer = RenderType.create("bbs_compat_gui_textured", RenderSetup.builder(RenderPipelines.GUI_TEXTURED).createRenderSetup());
        }

        return defaultGuiTexturedLayer;
    }
}
