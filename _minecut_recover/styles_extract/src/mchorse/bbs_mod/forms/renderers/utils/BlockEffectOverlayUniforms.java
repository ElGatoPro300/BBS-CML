package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_1723;
import net.minecraft.class_284;
import net.minecraft.class_5944;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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

        class_5944 program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored, maskHalfBase);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        RenderSystem.setShaderTexture(0, class_1723.field_21668);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /**
     * Structure paint overlay: UI scale 1 covers the full AABB for box / circle / triangle.
     */
    public static void configurePaintOverlayRenderStateStructure(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha, float sizeX, float sizeY, float sizeZ)
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        class_5944 program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
            bindFormRootInverse(program, rootInverse);
            bindPaintStructure(program, transform, bottomAnchored, sizeX, sizeY, sizeZ);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        RenderSystem.setShaderTexture(0, class_1723.field_21668);
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
                com.mojang.blaze3d.platform.GlStateManager.class_4535.DST_COLOR,
                com.mojang.blaze3d.platform.GlStateManager.class_4534.ZERO,
                com.mojang.blaze3d.platform.GlStateManager.class_4535.DST_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.class_4534.ZERO
            );
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        class_5944 program = BBSShaders.getBlockColorTintOverlayProgram();

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

        RenderSystem.setShaderTexture(0, class_1723.field_21668);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindFormColorGrade(class_5944 shader, Color gradeSource)
    {
        bindFormColorGrade(shader, gradeSource, true, 0.5F);
    }

    public static void bindFormColorGrade(class_5944 shader, Color gradeSource, boolean bottomAnchored, float maskHalfBase)
    {
        bindFormColorGradeInternal(shader, gradeSource, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindFormColorGradeStructure(class_5944 shader, Color gradeSource, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindFormColorGradeInternal(shader, gradeSource, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindFormColorGradeInternal(class_5944 shader, Color gradeSource, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
    {
        if (shader == null)
        {
            return;
        }

        class_284 gradeUniform = shader.method_34582("FormColorGrade");
        class_284 activeUniform = shader.method_34582("ColorGradeActive");
        boolean active = gradeSource != null && gradeSource.hasColorAdjustments();

        if (gradeUniform != null)
        {
            if (active)
            {
                gradeUniform.method_35657(gradeSource.brightness, gradeSource.contrast, gradeSource.hue, gradeSource.saturation);
            }
            else
            {
                gradeUniform.method_35657(0F, 0F, 0F, 0F);
            }
        }

        if (activeUniform != null)
        {
            activeUniform.method_1251(active ? 1F : 0F);
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

    private static void bindGradeChannelMask(class_5944 shader, String prefix, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
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

        class_284 inverseUniform = shader.method_34582(prefix + "Inverse");

        if (inverseUniform != null)
        {
            inverseUniform.method_1250(colorEffectInverse);
        }

        class_284 halfUniform = shader.method_34582(prefix + "Half");

        if (halfUniform != null)
        {
            halfUniform.method_1249(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        class_284 activeUniform = shader.method_34582(prefix + "Active");

        if (activeUniform != null)
        {
            activeUniform.method_1251(active ? 1F : 0F);
        }

        class_284 anchorUniform = shader.method_34582(prefix + "BottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.method_1251(bottomAnchored ? 1F : 0F);
        }

        class_284 shapeUniform = shader.method_34582(prefix + "Shape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.method_1251(shape);
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

    public static void bindFormRootInverse(class_5944 shader, Matrix4f rootInverse)
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

        class_284 uniform = shader.method_34582("FormRootInverse");

        if (uniform != null)
        {
            uniform.method_1250(formRootInverse);
        }
    }

    public static void bindPaint(class_5944 shader, EffectTransform transform)
    {
        bindPaint(shader, transform, true, 0.5F);
    }

    public static void bindPaint(class_5944 shader, EffectTransform transform, boolean bottomAnchored)
    {
        bindPaint(shader, transform, bottomAnchored, 0.5F);
    }

    public static void bindPaint(class_5944 shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase)
    {
        bindPaintInternal(shader, transform, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindPaintStructure(class_5944 shader, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindPaintInternal(shader, transform, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindPaintInternal(class_5944 shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
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

        class_284 inverseUniform = shader.method_34582("PaintEffectInverse");

        if (inverseUniform != null)
        {
            inverseUniform.method_1250(paintEffectInverse);
        }

        class_284 halfUniform = shader.method_34582("PaintMaskHalf");

        if (halfUniform != null)
        {
            halfUniform.method_1249(paintMaskHalf.x, paintMaskHalf.y, paintMaskHalf.z);
        }

        class_284 activeUniform = shader.method_34582("PaintEffectActive");

        if (activeUniform != null)
        {
            activeUniform.method_1251(active ? 1F : 0F);
        }

        class_284 anchorUniform = shader.method_34582("PaintMaskBottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.method_1251(bottomAnchored ? 1F : 0F);
        }

        class_284 shapeUniform = shader.method_34582("PaintMaskShape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.method_1251(shape);
        }
    }

    public static void bindGlowOverlay(class_5944 shader, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        class_284 glowUniform = shader == null ? null : shader.method_34582("GlowOverlayColor");
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
            glowUniform.method_35657(glowR, glowG, glowB, glowStrength);
        }
    }

    public static void bindColorEffect(class_5944 shader, EffectTransform transform, boolean bottomAnchored)
    {
        bindColorEffect(shader, transform, bottomAnchored, 0.5F);
    }

    public static void bindColorEffect(class_5944 shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase)
    {
        bindColorEffectInternal(shader, transform, bottomAnchored, maskHalfBase, false, 1F, 1F, 1F);
    }

    public static void bindColorEffectStructure(class_5944 shader, EffectTransform transform, boolean bottomAnchored, float sizeX, float sizeY, float sizeZ)
    {
        bindColorEffectInternal(shader, transform, bottomAnchored, 0.5F, true, sizeX, sizeY, sizeZ);
    }

    private static void bindColorEffectInternal(class_5944 shader, EffectTransform transform, boolean bottomAnchored, float maskHalfBase, boolean structureSized, float sizeX, float sizeY, float sizeZ)
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

        class_284 inverseUniform = shader.method_34582("ColorEffectInverse");

        if (inverseUniform != null)
        {
            inverseUniform.method_1250(colorEffectInverse);
        }

        class_284 halfUniform = shader.method_34582("ColorMaskHalf");

        if (halfUniform != null)
        {
            halfUniform.method_1249(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        class_284 activeUniform = shader.method_34582("ColorEffectActive");

        if (activeUniform != null)
        {
            activeUniform.method_1251(active ? 1F : 0F);
        }

        class_284 anchorUniform = shader.method_34582("ColorMaskBottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.method_1251(bottomAnchored ? 1F : 0F);
        }

        class_284 shapeUniform = shader.method_34582("ColorMaskShape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.method_1251(shape);
        }
    }

    public static void bindFormColorTint(class_5944 shader, Color formColor)
    {
        if (shader == null)
        {
            return;
        }

        class_284 tintUniform = shader.method_34582("FormColorTint");

        if (tintUniform != null)
        {
            if (formColor == null)
            {
                tintUniform.method_35657(1F, 1F, 1F, 1F);
            }
            else
            {
                tintUniform.method_35657(formColor.r, formColor.g, formColor.b, formColor.a);
            }
        }
    }
}
