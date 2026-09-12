package mchorse.bbs_mod.client.render;

import mchorse.bbs_mod.forms.renderers.utils.ModelEffectPass;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Compatibility shim for 1.21.11 where vanilla {@code BufferRenderer} was removed.
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

        layer.draw(buffer);
    }

    public static void draw(MeshData buffer)
    {
        drawWithGlobalProgram(buffer);
    }

    private static RenderType resolveLayer(MeshData.DrawState params)
    {
        VertexFormat format = params.format();
        VertexFormat.Mode mode = params.mode();

        if (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.DEBUG_LINES || mode == VertexFormat.Mode.DEBUG_LINE_STRIP)
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
