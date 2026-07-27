package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import net.minecraft.class_5944;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

/**
 * Multiply-blend color-mask overlay for flat textured forms (billboards, shapes).
 * Matches {@link mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer#beginColorTintOverlayPass()}.
 */
public final class FlatColorTintOverlayPass
{
    private FlatColorTintOverlayPass()
    {}

    public static void render(Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.class_4535.DST_COLOR,
            com.mojang.blaze3d.platform.GlStateManager.class_4534.ZERO,
            com.mojang.blaze3d.platform.GlStateManager.class_4535.DST_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.class_4534.ZERO
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        /* Same far-distance units bias as FlatPaintOverlayPass (camera-facing quads). */
        GL11.glPolygonOffset(FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);

        class_5944 program = BBSShaders.getFlatColorTintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        try
        {
            draw.run();
        }
        finally
        {
            GL11.glPolygonOffset(0F, 0F);

            if (!savedPolygonOffsetFill)
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
        }
    }
}
