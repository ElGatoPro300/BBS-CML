package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.screen.PlayerScreenHandler;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

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
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderProgram program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored, maskHalfBase);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /**
     * Structure paint overlay: UI scale 1 covers the full AABB for box / circle / triangle.
     */
    public static void configurePaintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha, float sizeX, float sizeY, float sizeZ)
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderProgram program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
            bindFormRootInverse(program, rootInverse);
            bindPaintStructure(program, transform, bottomAnchored, sizeX, sizeY, sizeZ);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
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
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, maskHalfBase, gradeSource, false, 1F, 1F, 1F);
    }

    /**
     * Structure color / grade overlay: UI scale 1 covers the full AABB for box / circle / triangle.
     */
    public static void configureColorTintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, Color gradeSource, float sizeX, float sizeY, float sizeZ)
    {
        configureColorTintOverlayRenderState(rootInverse, transform, bottomAnchored, formColor, 0.5F, gradeSource, true, sizeX, sizeY, sizeZ);
    }

    private static void configureColorTintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, Color formColor, float maskHalfBase, Color gradeSource, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        boolean wantGrade = gradeSource != null && gradeSource.hasColorAdjustments();
        boolean gradeActive = wantGrade && ModelVAORenderer.captureGradeSceneColor();

        RenderSystem.enableBlend();

        if (gradeActive)
        {
            /* Replace lit pixels with graded lit pixels — never leave DST_COLOR for UI. */
            RenderSystem.defaultBlendFunc();
        }
        else
        {
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.DST_COLOR,
                GlStateManager.DstFactor.ZERO,
                GlStateManager.SrcFactor.DST_ALPHA,
                GlStateManager.DstFactor.ZERO
            );
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        ShaderProgram program = BBSShaders.getBlockColorTintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
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

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindFormColorGrade(ShaderProgram shader, Color gradeSource)
    {
        bindFormColorGrade(shader, gradeSource, true, 0.5F);
    }

    public static void bindFormColorGrade(ShaderProgram shader, Color gradeSource, boolean bottomAnchored, float maskHalfBase)
    {
        bindFormColorGradeInternal(shader, gradeSource, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindFormColorGradeStructure(ShaderProgram shader, Color gradeSource, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindFormColorGradeInternal(shader, gradeSource, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindFormColorGradeInternal(ShaderProgram shader, Color gradeSource, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (shader == null)
        {
            return;
        }

        GlUniform gradeUniform = shader.getUniform("FormColorGrade");
        GlUniform activeUniform = shader.getUniform("ColorGradeActive");
        boolean active = gradeSource != null && gradeSource.hasColorAdjustments();

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

        EffectTransform brightness = active ? gradeSource.brightnessTransform : null;
        EffectTransform contrast = active ? gradeSource.contrastTransform : null;
        EffectTransform hue = active ? gradeSource.hueTransform : null;
        EffectTransform saturation = active ? gradeSource.saturationTransform : null;

        bindGradeChannelMask(shader, "GradeBrightness", brightness, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        bindGradeChannelMask(shader, "GradeContrast", contrast, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        bindGradeChannelMask(shader, "GradeHue", hue, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
        bindGradeChannelMask(shader, "GradeSaturation", saturation, bottomAnchored, maskHalfBase, structureSized, sizeX, sizeY, sizeZ);
    }

    private static void bindGradeChannelMask(ShaderProgram shader, String prefix, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
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

        GlUniform inverseUniform = shader.getUniform(prefix + "Inverse");

        if (inverseUniform != null)
        {
            inverseUniform.set(colorEffectInverse);
        }

        GlUniform halfUniform = shader.getUniform(prefix + "Half");

        if (halfUniform != null)
        {
            halfUniform.set(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        GlUniform activeUniform = shader.getUniform(prefix + "Active");

        if (activeUniform != null)
        {
            activeUniform.set(active ? 1F : 0F);
        }

        GlUniform anchorUniform = shader.getUniform(prefix + "BottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.set(bottomAnchored ? 1F : 0F);
        }

        GlUniform shapeUniform = shader.getUniform(prefix + "Shape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
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

    public static void bindFormRootInverse(ShaderProgram shader, Matrix4f rootInverse)
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

        GlUniform uniform = shader.getUniform("FormRootInverse");

        if (uniform != null)
        {
            uniform.set(formRootInverse);
        }
    }

    public static void bindPaint(ShaderProgram shader, EffectTransform transform)
    {
        bindPaint(shader, transform, true, 0.5F);
    }

    public static void bindPaint(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored)
    {
        bindPaint(shader, transform, bottomAnchored, 0.5F);
    }

    public static void bindPaint(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase)
    {
        bindPaintInternal(shader, transform, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindPaintStructure(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindPaintInternal(shader, transform, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindPaintInternal(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
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
    }

    public static void bindGlowOverlay(ShaderProgram shader, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        GlUniform glowUniform = shader == null ? null : shader.getUniform("GlowOverlayColor");
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

        if (glowUniform != null)
        {
            glowUniform.set(glowR, glowG, glowB, glowStrength);
        }
    }

    public static void bindColorEffect(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored)
    {
        bindColorEffect(shader, transform, bottomAnchored, 0.5F);
    }

    public static void bindColorEffect(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase)
    {
        bindColorEffectInternal(shader, transform, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindColorEffectStructure(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindColorEffectInternal(shader, transform, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindColorEffectInternal(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
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
    }

    public static void bindFormColorTint(ShaderProgram shader, Color formColor)
    {
        if (shader == null)
        {
            return;
        }

        GlUniform tintUniform = shader.getUniform("FormColorTint");

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
    }
}
