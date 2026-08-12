package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.texture.SpriteAtlasTexture;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

public final class BlockEffectOverlayUniforms
{
    private static final Matrix4f formRootInverse = new Matrix4f();
    private static final Matrix4f paintEffectInverse = new Matrix4f();
    private static final Vector3f paintMaskHalf = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final Matrix4f colorEffectInverse = new Matrix4f();
    private static final Vector3f colorMaskHalf = new Vector3f(0.5F, 0.5F, 0.5F);

    private BlockEffectOverlayUniforms()
    {}

    public static boolean hasPaintOverlayShader()
    {
        return BBSShaders.getBlockPaintOverlayProgram() != null;
    }

    public static boolean hasColorTintOverlayShader()
    {
        return BBSShaders.getBlockColorTintOverlayProgram() != null;
    }

    public static void configurePaintOverlayRenderState(EffectTransform transform)
    {
        configurePaintOverlayRenderState(null, transform, true, null, null, 0F, 1F, 0.5F);
    }

    public static void configurePaintOverlayRenderState(EffectTransform transform, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        configurePaintOverlayRenderState(null, transform, true, glow, legacyGlow, glowIntensity, alpha, 0.5F);
    }

    public static void configurePaintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        configurePaintOverlayRenderState(rootInverse, transform, true, glow, legacyGlow, glowIntensity, alpha, 0.5F);
    }

    public static void configurePaintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        configurePaintOverlayRenderState(rootInverse, transform, bottomAnchored, glow, legacyGlow, glowIntensity, alpha, 0.5F);
    }

    public static void configurePaintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha, float maskHalfBase)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        RenderPipeline program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            // RenderSystem.setShader(program);
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored, maskHalfBase);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void configurePaintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha, float sizeX, float sizeY, float sizeZ)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        RenderPipeline program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            // RenderSystem.setShader(program);
            bindFormRootInverse(program, rootInverse);
            bindPaintStructure(program, transform, bottomAnchored, sizeX, sizeY, sizeZ);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void configureColorTintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, 0.5F, null);
    }

    public static void configureColorTintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, float maskHalfBase)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, maskHalfBase, null);
    }

    public static void configureColorTintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, float maskHalfBase, Color gradeSource)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, maskHalfBase, gradeSource, false, 1F, 1F, 1F);
    }

    public static void configureColorTintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, Color gradeSource, float sizeX, float sizeY, float sizeZ)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, 0.5F, gradeSource, true, sizeX, sizeY, sizeZ);
    }

    private static void configureColorTintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, float maskHalfBase, Color gradeSource, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        boolean wantGrade = gradeSource != null && gradeSource.hasColorAdjustments();
        boolean gradeActive = wantGrade && ModelVAORenderer.captureGradeSceneColor();

        GlStateManager._enableBlend();

        if (gradeActive)
        {
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        }
        else
        {
            GlStateManager._blendFuncSeparate(GL11.GL_DST_COLOR, GL11.GL_ZERO, GL11.GL_DST_ALPHA, GL11.GL_ZERO);
        }

        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getBlockColorTintOverlayProgram();

        if (program != null)
        {
            // RenderSystem.setShader(program);
            bindFormRootInverse(program, rootInverse);

            if (structureSized)
            {
                bindColorEffectStructure(program, transform, bottomAnchored, sizeX, sizeY, sizeZ);
                bindFormColorTint(program, formColor);
                bindFormColorGradeStructure(program, gradeActive ? gradeSource : null, bottomAnchored, sizeX, sizeY, sizeZ);
            }
            else
            {
                bindColorEffect(program, transform, bottomAnchored, maskHalfBase);
                bindFormColorTint(program, formColor);
                bindFormColorGrade(program, gradeActive ? gradeSource : null, bottomAnchored, maskHalfBase);
            }

            if (gradeActive)
            {
                ModelVAORenderer.bindGradeSceneColorTexture();
            }
        }

        BBSModClient.getTextures().bindTextureId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindFormColorGrade(RenderPipeline shader, Color gradeSource)
    {
        bindFormColorGrade(shader, gradeSource, true, 0.5F);
    }

    public static void bindFormColorGrade(RenderPipeline shader, Color gradeSource, boolean bottomAnchored, float maskHalfBase)
    {
        bindFormColorGradeInternal(shader, gradeSource, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindFormColorGradeStructure(RenderPipeline shader, Color gradeSource, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindFormColorGradeInternal(shader, gradeSource, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindFormColorGradeInternal(RenderPipeline shader, Color gradeSource, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (shader == null)
        {
            return;
        }

        GlUniform gradeUniform = null;
        GlUniform activeUniform = null;
        boolean active = gradeSource != null && gradeSource.hasColorAdjustments();

        /*
        if (gradeUniform != null)
        {
            if (active)
            {
                gradeUniform.set(gradeSource.brightness, gradeSource.contrast, gradeSource.hue, gradeSource.saturation);
            }
            else
            {
                gradeUniform.set(0F, 0F, 0F, 0F);
            }
        }

        if (activeUniform != null)
        {
            activeUniform.set(active ? 1F : 0F);
        }
        */

        EffectTransform brightness = active ? gradeSource.brightnessTransform : null;
        EffectTransform contrast = active ? gradeSource.contrastTransform : null;
        EffectTransform hue = active ? gradeSource.hueTransform : null;
        EffectTransform saturation = active ? gradeSource.saturationTransform : null;

        bindGradeChannelMask(shader, "GradeBrightness", brightness, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        bindGradeChannelMask(shader, "GradeContrast", contrast, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        bindGradeChannelMask(shader, "GradeHue", hue, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        bindGradeChannelMask(shader, "GradeSaturation", saturation, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
    }

    private static void bindGradeChannelMask(RenderPipeline shader, String prefix, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, colorEffectInverse);
            resolveOverlayMaskHalf(transform, colorMaskHalf, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        }
        else
        {
            colorEffectInverse.identity();
            resolveOverlayMaskHalf(null, colorMaskHalf, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        }

        GlUniform inverseUniform = null;
        GlUniform halfUniform = null;
        GlUniform activeUniform = null;
        GlUniform anchorUniform = null;
        GlUniform shapeUniform = null;

        /*
        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
        */
    }

    private static void resolveOverlayMaskHalf(EffectTransform transform, Vector3f dest, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (structureSized)
        {
            EffectTransformMath.resolveStructureMaskHalfExtents(transform, dest, sizeX, sizeY, sizeZ);

            return;
        }

        if (!bottomAnchored)
        {
            EffectTransformMath.resolveItemMaskHalfExtents(transform, dest);

            return;
        }

        if (transform == null)
        {
            dest.set(maskHalfBase, maskHalfBase, maskHalfBase);

            return;
        }

        EffectTransformMath.resolveMaskHalfExtents(transform, dest, maskHalfBase, 1F);
    }

    public static void bindFormRootInverse(RenderPipeline shader, Matrix4f rootInverse)
    {
        if (shader == null)
        {
            return;
        }

        if (rootInverse != null)
        {
            formRootInverse.set(rootInverse);
        }
        else
        {
            formRootInverse.identity();
        }

        GlUniform uniform = null;

        /*
        if (uniform != null)
        {
            uniform.set(formRootInverse);
        }
        */
    }

    public static void configureFlatPaintOverlay(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getFlatPaintOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindPaintPrecomputed(program, transform, bottomAnchored, maskHalf);
        }

        // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindPaintPrecomputed(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf)
    {
        if (shader == null)
        {
            return;
        }

        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
        }
        else
        {
            paintEffectInverse.identity();
        }

        if (maskHalf != null)
        {
            paintMaskHalf.set(maskHalf);
        }
        else if (active)
        {
            resolveOverlayMaskHalf(transform, paintMaskHalf, bottomAnchored, 0.5F, false, 1F, 1F, 1F);
        }
        else
        {
            paintMaskHalf.set(0.5F, 0.5F, 0.5F);
        }

        /*
        GlUniform inverseUniform = shader.getUniform("PaintEffectInverse");

        if (inverseUniform != null)
        {
            inverseUniform.set(paintEffectInverse);
        }

        GlUniform halfUniform = shader.getUniform("PaintMaskHalf");

        if (halfUniform != null)
        {
            halfUniform.set(paintMaskHalf.x, paintMaskHalf.y, paintMaskHalf.z);
        }

        GlUniform activeUniform = shader.getUniform("PaintEffectActive");

        if (activeUniform != null)
        {
            activeUniform.set(active ? 1F : 0F);
        }

        GlUniform anchorUniform = shader.getUniform("PaintMaskBottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.set(bottomAnchored ? 1F : 0F);
        }

        GlUniform shapeUniform = shader.getUniform("PaintMaskShape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
        */
    }

    public static void bindPaint(RenderPipeline shader, EffectTransform transform)
    {
        bindPaint(shader, transform, true, 0.5F);
    }

    public static void bindPaint(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored)
    {
        bindPaint(shader, transform, bottomAnchored, 0.5F);
    }

    public static void bindPaint(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase)
    {
        bindPaintInternal(shader, transform, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindPaintStructure(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindPaintInternal(shader, transform, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindPaintInternal(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (shader == null)
        {
            return;
        }

        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
            resolveOverlayMaskHalf(transform, paintMaskHalf, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        }
        else
        {
            paintEffectInverse.identity();
            resolveOverlayMaskHalf(null, paintMaskHalf, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        }

        GlUniform inverseUniform = null;
        GlUniform halfUniform = null;
        GlUniform activeUniform = null;
        GlUniform anchorUniform = null;
        GlUniform shapeUniform = null;

        /*
        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
        */
    }

    public static void bindGlowOverlay(RenderPipeline shader, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        GlUniform glowUniform = null;
        float glowR = 0F;
        float glowG = 0F;
        float glowB = 0F;
        float glowStrength = 0F;

        if (glow != null && glow.resolvePaintOnly() && glowIntensity > 0F)
        {
            Color resolved = new Color();

            glow.resolveColor(legacyGlow, resolved);
            glowR = resolved.r;
            glowG = resolved.g;
            glowB = resolved.b;
            glowStrength = glowIntensity * alpha;
        }

        /*
        if (glowUniform != null)
        {
            glowUniform.set(glowR, glowG, glowB, glowStrength);
        }
        */
    }

    public static void bindColorEffect(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored)
    {
        bindColorEffect(shader, transform, bottomAnchored, 0.5F);
    }

    public static void bindColorEffect(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase)
    {
        bindColorEffectInternal(shader, transform, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindColorEffectStructure(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindColorEffectInternal(shader, transform, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindColorEffectInternal(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (shader == null)
        {
            return;
        }

        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, colorEffectInverse);
            resolveOverlayMaskHalf(transform, colorMaskHalf, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        }
        else
        {
            colorEffectInverse.identity();
            resolveOverlayMaskHalf(null, colorMaskHalf, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        }

        GlUniform inverseUniform = null;
        GlUniform halfUniform = null;
        GlUniform activeUniform = null;
        GlUniform anchorUniform = null;
        GlUniform shapeUniform = null;

        /*
        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
        */
    }

    /**
     * Flat billboard / shape color-tint overlay: fragment mask in quad-local space.
     * {@code maskHalf} must already include transform scale (see
     * {@link EffectTransformMath#resolveBillboardMaskHalfExtents}).
     */
    public static void configureFlatColorTintOverlay(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf, Color formColor)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getFlatColorTintOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindColorEffectPrecomputed(program, transform, bottomAnchored, maskHalf);
            bindFormColorTint(program, formColor);
        }

        // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindColorEffectPrecomputed(RenderPipeline shader, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf)
    {
        if (shader == null)
        {
            return;
        }

        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, colorEffectInverse);
        }
        else
        {
            colorEffectInverse.identity();
        }

        if (maskHalf != null)
        {
            colorMaskHalf.set(maskHalf);
        }
        else if (active)
        {
            resolveOverlayMaskHalf(transform, colorMaskHalf, bottomAnchored, 0.5F, false, 1F, 1F, 1F);
        }
        else
        {
            colorMaskHalf.set(0.5F, 0.5F, 0.5F);
        }

        /*
        GlUniform inverseUniform = shader.getUniform("ColorEffectInverse");

        if (inverseUniform != null)
        {
            inverseUniform.set(colorEffectInverse);
        }

        GlUniform halfUniform = shader.getUniform("ColorMaskHalf");

        if (halfUniform != null)
        {
            halfUniform.set(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        GlUniform falloffUniform = shader.getUniform("ColorMaskFalloff");

        if (falloffUniform != null)
        {
            falloffUniform.set(EffectTransformMath.resolveMaskFalloff(transform, colorMaskHalf));
        }

        GlUniform activeUniform = shader.getUniform("ColorEffectActive");

        if (activeUniform != null)
        {
            activeUniform.set(active ? 1F : 0F);
        }

        GlUniform anchorUniform = shader.getUniform("ColorMaskBottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.set(bottomAnchored ? 1F : 0F);
        }

        GlUniform shapeUniform = shader.getUniform("ColorMaskShape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
        */
    }

    public static void bindFormColorTint(RenderPipeline shader, Color formColor)
    {
        if (shader == null)
        {
            return;
        }

        GlUniform tintUniform = null;

        /*
        if (tintUniform != null)
        {
            if (formColor == null)
            {
                tintUniform.set(1F, 1F, 1F, 1F);
            }
            else
            {
                tintUniform.set(formColor.r, formColor.g, formColor.b, formColor.a);
            }
        }
        */
    }
}
