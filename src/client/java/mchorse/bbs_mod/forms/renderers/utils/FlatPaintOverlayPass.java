package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;

import net.minecraft.client.gl.ShaderProgram;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

/**
 * Second-pass paint for flat textured forms (billboards, trails). Samples the caller-bound
 * texture and tints it toward the paint color; does not switch to the block atlas shader.
 */
public class FlatPaintOverlayPass
{

    /**
     * Camera-facing quads have ~0 depth slope, so only {@code units} separates the overlay.
     * Far away, float depth precision needs a larger units bias than near-camera draws.
     */
    public static final float POLYGON_OFFSET_FACTOR = -1F;
    public static final float POLYGON_OFFSET_UNITS = -32F;

    /** Default bias — clears the camera-facing base face when close / angled. */
    public static final float DEFAULT_FACTOR = -2F;
    public static final float DEFAULT_UNITS = -4F;
    /**
     * Stronger than deferred billboard base ({@code -1.5/-1.5}). Paint flushes after the Iris
     * base redraw; the factor term dominates at distance and must clearly beat the base offset.
     */
    public static final float DEFERRED_BILLBOARD_FACTOR = -4F;
    public static final float DEFERRED_BILLBOARD_UNITS = -8F;


    private FlatPaintOverlayPass()
    {
    }

    public static void render(EffectTransform transform, Runnable draw)
    {
        render(draw);
    }

    public static void render(Runnable draw)
    {
        render(DEFAULT_FACTOR, DEFAULT_UNITS, draw);
    }

    public static void render(float factor, float units, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(POLYGON_OFFSET_FACTOR, POLYGON_OFFSET_UNITS);
        GL11.glPolygonOffset(factor, units);

        ShaderProgram program = BBSShaders.getFlatPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(program);
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
