package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.FormGlowBloomPatch;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Photoshop-like Outer Glow for flat textured forms.
 * <p>
 * Intensity = bright layered PTC (always visible). Size/Spread = soft outer halo shells.
 */
public final class FlatGlowOverlayPass
{
    private static final float SHELL_OFFSET_BIAS = 0.0003F;

    private FlatGlowOverlayPass()
    {}

    /**
     * @param emitPass draws the full quad at given local Z-offset and RGBA color
     */
    public static void render(
        GlowSettings glow,
        Color legacyGlow,
        PaintSettings paint,
        Color paintColor,
        Color formColor,
        float entityAlpha,
        BiConsumer<Float, Color> emitPass
    )
    {
        if (emitPass == null)
        {
            return;
        }

        float intensity = glow != null ? glow.intensity : 0F;

        if (intensity <= 0.0001F)
        {
            return;
        }

        int layers = FormColorEffects.resolveGlowOverlayLayers(intensity);
        Color glowColor = FormColorEffects.resolveGlowOverlayColor(
            glow, legacyGlow, paint, paintColor, formColor, entityAlpha, intensity, layers
        );

        if (glowColor.a <= 0.0001F)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        try
        {
            /* Base emission core */
            for (int i = 0; i < layers; i++)
            {
                emitPass.accept(0F, glowColor);
            }

            /* Outer expansion shells when Size > 0 (omitted if pack handles bloom itself) */
            float size = glow != null ? glow.size : 0F;
            float spread = glow != null ? glow.spread : 0F;

            if (size > 0.0001F && !FormGlowBloomPatch.shouldSkipGeometrySizeShells())
            {
                int shells = Math.min(8, Math.max(1, (int) Math.ceil(size * 4F)));
                Color shellColor = glowColor.copy();

                for (int s = 1; s <= shells; s++)
                {
                    float t = (float) s / (float) shells;
                    /* Spread biases opacity toward the outer edge */
                    float alphaFactor = (1F - t) * (1F - spread * 0.5F);

                    shellColor.a = glowColor.a * alphaFactor / (float) shells;

                    if (shellColor.a > 0.001F)
                    {
                        emitPass.accept(s * SHELL_OFFSET_BIAS, shellColor);
                    }
                }
            }
        }
        finally
        {
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.defaultBlendFunc();
        }
    }

    /**
     * Variant for draw calls using shader uniform-based outer expansion (e.g. model/flat glow shader).
     */
    public static void renderWithShader(
        GlowSettings glow,
        Color legacyGlow,
        PaintSettings paint,
        Color paintColor,
        Color formColor,
        float entityAlpha,
        Consumer<Color> drawCall
    )
    {
        if (drawCall == null)
        {
            return;
        }

        float intensity = glow != null ? glow.intensity : 0F;

        if (intensity <= 0.0001F)
        {
            return;
        }

        int layers = FormColorEffects.resolveGlowOverlayLayers(intensity);
        Color glowColor = FormColorEffects.resolveGlowOverlayColor(
            glow, legacyGlow, paint, paintColor, formColor, entityAlpha, intensity, layers
        );

        if (glowColor.a <= 0.0001F)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        float size = glow != null ? glow.size : 0F;
        float spread = glow != null ? glow.spread : 0F;
        ShaderProgram program = GameRenderer.getPositionTexColorProgram();

        bindGlowUniforms(program, intensity, size, spread);

        try
        {
            for (int i = 0; i < layers; i++)
            {
                drawCall.accept(glowColor);
            }
        }
        finally
        {
            bindGlowUniforms(program, 0F, 0F, 0F);

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.defaultBlendFunc();
        }
    }

    /**
     * Map UV on an expanded Outer Glow quad so the texture stays world-sized in the center
     * and padded UV (outside 0..1 after remap) becomes empty for true outside glow.
     */
    public static float remapUvForOuterGlow(float uv, float expand)
    {
        if (Math.abs(expand) <= 0.0001F)
        {
            return uv;
        }

        float scale = Math.max(0.05F, 1F + expand);

        return 0.5F + (uv - 0.5F) * scale;
    }

    private static void bindGlowUniforms(ShaderProgram program, float intensity, float size, float spread)
    {
        if (program == null)
        {
            return;
        }

        GlUniform intensityUniform = program.getUniform("GlowIntensity");
        GlUniform sizeUniform = program.getUniform("GlowSize");
        GlUniform spreadUniform = program.getUniform("GlowSpread");

        if (intensityUniform != null)
        {
            intensityUniform.set(intensity);
        }

        if (sizeUniform != null)
        {
            sizeUniform.set(size);
        }

        if (spreadUniform != null)
        {
            spreadUniform.set(spread);
        }
    }

    public static void renderMasked(float factor, float units, Matrix4f formRootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf, float glowScale, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        BlockEffectOverlayUniforms.configureFlatGlowOverlay(formRootInverse, transform, bottomAnchored, maskHalf, glowScale);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
        GL11.glPolygonOffset(factor, units);

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
