package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.colors.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

/**
 * Additive glow overlay for flat geometry (billboards, labels, zero-thickness quads).
 * Main pass should apply negative glow only; positive emission is drawn here without depth writes.
 * <p>
 * Intensity is applied via layered additive draws with ColorModulator left at identity.
 * Scaling ColorModulator by {@code intensity * 8} broke Iris/Complementary (red → cyan, no bloom).
 */
public class FlatGlowOverlayPass
{
    private FlatGlowOverlayPass()
    {
    }

    public static void render(GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, Consumer<Color> drawLayer)
    {
        if (glowIntensity <= 0F || drawLayer == null)
        {
            return;
        }

        int layers = FormColorEffects.resolveGlowOverlayLayers(glowIntensity);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        try
        {
            for (int i = 0; i < layers; i++)
            {
                Color layer = FormColorEffects.resolveGlowOverlayColor(glowSettings, legacyGlow, alpha, glowIntensity, layers);

                drawLayer.accept(layer);
            }
        }
        finally
        {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            GL11.glPolygonOffset(0F, 0F);

            if (!savedPolygonOffsetFill)
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.defaultBlendFunc();
        }
    }
}
