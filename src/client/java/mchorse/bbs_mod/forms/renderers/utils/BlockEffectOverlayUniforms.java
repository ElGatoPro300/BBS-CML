package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.texture.SpriteAtlasTexture;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import org.lwjgl.opengl.GL11;

/**
 * Uploads spatial paint / color-tint mask uniforms for block/item overlay shaders.
 */
public final class BlockEffectOverlayUniforms
{
    private static final Matrix4f formRootInverse = new Matrix4f();
    private static final Matrix4f paintEffectInverse = new Matrix4f();
    private static final Vector3f paintMaskHalf = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final Matrix4f colorEffectInverse = new Matrix4f();
    private static final Vector3f colorMaskHalf = new Vector3f(0.5F, 0.5F, 0.5F);
    /** When non-null, {@link #resolveOverlayMaskHalf} uses {@link EffectTransformMath#resolveBlockVisualMaskHalfExtents}. */
    private static Vector3f blockVisualMaskSize = null;

    private BlockEffectOverlayUniforms()
    {}

    public static void setBlockVisualMaskSize(Vector3f size)
    {
        blockVisualMaskSize = size;
    }

    public static void clearBlockVisualMaskSize()
    {
        blockVisualMaskSize = null;
    }

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
        configurePaintOverlayRenderState(rootInverse, transform, bottomAnchored, glow, legacyGlow, glowIntensity, alpha, maskHalfBase, true);
    }

    private static void configurePaintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha, float maskHalfBase, boolean bindBlockAtlas)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        RenderPipeline program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored, maskHalfBase);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }
    }

    public static void configureGlowOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, float glowScale)
    {
        configureGlowOverlayRenderStateInternal(rootInverse, transform, bottomAnchored, maskHalfBase, glowScale, true);
    }

    private static void configureGlowOverlayRenderStateInternal(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, float glowScale, boolean bindBlockAtlas)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getBlockGlowOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored, maskHalfBase);
        }
    }

    public static void configureGlowOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ, float glowScale)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getBlockGlowOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindPaintStructure(program, transform, bottomAnchored, sizeX, sizeY, sizeZ);
        }
    }

    /**
     * Structure paint overlay: UI scale 1 covers the full AABB for box / circle / triangle.
     */
    public static void configurePaintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha, float sizeX, float sizeY, float sizeZ)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        RenderPipeline program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindPaintStructure(program, transform, bottomAnchored, sizeX, sizeY, sizeZ);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }
    }

    /**
     * Multiply-blend color-mask overlay (DST_COLOR / ZERO) — same semantics as Model color tint.
     * When {@code gradeSource} has Color Grade, copies the lit framebuffer and regrades those
     * pixels (keeps shading/shadows), same idea as model ColorGradeOverlay.
     */
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
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, maskHalfBase, gradeSource, false, 1F, 1F, 1F, true);
    }

    /**
     * Signs / chests / beds use entity atlases — keep each draw call's bound texture instead of
     * forcing {@link SpriteAtlasTexture#BLOCK_ATLAS_TEXTURE}.
     */
    public static void configureColorTintOverlayRenderStateEntityVisual(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, float maskHalfBase, Color gradeSource)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, maskHalfBase, gradeSource, false, 1F, 1F, 1F, false);
    }

    /**
     * Structure color / grade overlay: UI scale 1 covers the full AABB for box / circle / triangle.
     */
    public static void configureColorTintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, Color gradeSource, float sizeX, float sizeY, float sizeZ)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, 0.5F, gradeSource, true, sizeX, sizeY, sizeZ, true);
    }

    private static void configureColorTintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, float maskHalfBase, Color gradeSource, boolean structureSized, float sizeX, float sizeY, float sizeZ, boolean bindBlockAtlas)
    {
        boolean wantGrade = gradeSource != null && gradeSource.hasColorAdjustments();
        boolean gradeActive = wantGrade && ModelVAORenderer.captureGradeSceneColor();

        GlStateManager._enableBlend();

        if (gradeActive)
        {
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
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
    }

    public static void configurePaintOverlayRenderStateEntityVisual(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        configurePaintOverlayRenderState(rootInverse, transform, bottomAnchored, glow, legacyGlow, glowIntensity, alpha, 0.5F, false);
    }

    public static void configureGlowOverlayRenderStateEntityVisual(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, float glowScale)
    {
        configureGlowOverlayRenderStateInternal(rootInverse, transform, bottomAnchored, maskHalfBase, glowScale, false);
    }

    public static void bindFormColorTint(RenderPipeline shader, Color color)
    {
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
    }

    public static void bindFormRootInverse(RenderPipeline shader, Matrix4f rootInverse)
    {
    }



    private static void resolveOverlayMaskHalf(EffectTransform transform, Vector3f dest, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (blockVisualMaskSize != null)
        {
            EffectTransformMath.resolveBlockVisualMaskHalfExtents(transform, dest, blockVisualMaskSize.x, blockVisualMaskSize.y, blockVisualMaskSize.z);

            return;
        }

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
    }

    public static void bindPaintPrecomputed(RenderPipeline shader, Matrix4f effectInverse, boolean bottomAnchored, Vector3f maskHalf)
    {
    }

    public static void bindGlowOverlay(RenderPipeline shader, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
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
    }

    public static void bindColorEffectPrecomputed(RenderPipeline shader, Matrix4f effectInverse, boolean bottomAnchored, Vector3f maskHalf)
    {
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

            if (transform != null)
            {
                bindPaint(program, transform, bottomAnchored);
            }
            else
            {
                bindPaintPrecomputed(program, null, bottomAnchored, maskHalf);
            }
        }
    }

    public static void configureFlatGlowOverlay(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf, float glowScale)
    {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getBlockGlowOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored);
        }
    }

    public static void configureFlatColorTintOverlay(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf, Color formColor)
    {
        configureFlatColorTintOverlay(rootInverse, transform, bottomAnchored, maskHalf, formColor, null);
    }

    public static void configureFlatColorTintOverlay(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Vector3f maskHalf, Color formColor, Color gradeSource)
    {
        boolean wantGrade = gradeSource != null && gradeSource.hasColorAdjustments();
        boolean gradeActive = wantGrade && ModelVAORenderer.captureGradeSceneColor();

        GlStateManager._enableBlend();

        if (gradeActive)
        {
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        }
        else
        {
            GlStateManager._blendFuncSeparate(GL11.GL_DST_COLOR, GL11.GL_ZERO, GL11.GL_DST_ALPHA, GL11.GL_ZERO);
        }

        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(false);

        RenderPipeline program = BBSShaders.getFlatColorTintOverlayProgram();

        if (program != null)
        {
            bindFormRootInverse(program, rootInverse);

            if (transform != null)
            {
                bindColorEffect(program, transform, bottomAnchored, 0.5F);
            }
            else
            {
                bindColorEffectPrecomputed(program, null, bottomAnchored, maskHalf);
            }

            bindFormColorTint(program, formColor);
            bindFormColorGrade(program, gradeActive ? gradeSource : null, bottomAnchored, 0.5F);

            if (gradeActive)
            {
                ModelVAORenderer.bindGradeSceneColorTexture();
            }
        }
    }
}
