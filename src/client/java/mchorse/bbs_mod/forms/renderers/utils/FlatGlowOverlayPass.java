package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.colors.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

/**
 * Additive glow overlay for flat geometry (billboards, labels, zero-thickness quads).
 * Main pass should apply negative glow only; positive emission is drawn here without depth writes.
 */
public class FlatGlowOverlayPass
{
    private FlatGlowOverlayPass()
    {
    }

    public static void render(GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, Consumer<Color> drawLayer)
    {
        render(glowSettings, legacyGlow, alpha, glowIntensity, null, glowSettings == null ? null : glowSettings.transform, false, null, drawLayer);
    }

    public static void render(GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, Matrix4f formRootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf, Consumer<Color> drawLayer)
    {
        if (glowIntensity <= 0F || drawLayer == null)
        {
            return;
        }

        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        BlockEffectOverlayUniforms.configureFlatGlowOverlay(formRootInverse, transform, bottomAnchored, maskHalf);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);

        try
        {
            drawLayer.accept(glowColor);
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
