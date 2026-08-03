package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.FormColorGradePatch;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.List;

public class ModelVAORenderer
{
    /* Paint overlay state. Used by CubicVAORenderer and ModelFormRenderer */
    private static float baseR;
    private static float baseG;
    private static float baseB;
    private static float baseStrength;

    private static float paintR;
    private static float paintG;
    private static float paintB;
    private static float paintStrength;
    /* When true, the model is being drawn as a shader-pack paint overlay pass. Groups still sample their
     * real skin texture so transparent UV regions are discarded; only textured pixels receive paint. */
    private static boolean paintPass;
    private static boolean paintOverlayPass;
    private static boolean paintOverlaySynced;

    private static float baseGlowR;
    private static float baseGlowG;
    private static float baseGlowB;
    private static float baseGlowStrength;
    private static float glowR;
    private static float glowG;
    private static float glowB;
    private static float glowStrength;

    private static final Matrix4f formRootInverse = new Matrix4f();
    private static final Matrix4f paintEffectInverse = new Matrix4f();
    private static final Vector3f paintMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static final Matrix4f colorEffectInverse = new Matrix4f();
    private static final Vector3f colorMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static final Matrix4f overlayFormRootInverse = new Matrix4f();
    private static float paintMaskShape;
    private static float colorMaskShape;
    private static boolean paintEffectActive;
    private static boolean colorEffectActive;
    private static boolean paintMaskBottomAnchored = true;
    private static boolean colorMaskBottomAnchored = true;
    private static boolean glowingUniformActive;
    /* Form-level paint/color masks snapshotted by set*EffectTransform; setGroup* restores these. */
    private static final Matrix4f basePaintEffectInverse = new Matrix4f();
    private static final Vector3f basePaintMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static float basePaintMaskShape;
    private static boolean basePaintEffectActive;
    private static boolean basePaintMaskBottomAnchored = true;
    private static final Matrix4f baseColorEffectInverse = new Matrix4f();
    private static final Vector3f baseColorMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static float baseColorMaskShape;
    private static boolean baseColorEffectActive;
    private static boolean baseColorMaskBottomAnchored = true;
    private static final Matrix4f glowEffectInverse = new Matrix4f();
    private static final Vector3f glowMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static float glowMaskShape;
    private static boolean glowEffectActive;
    private static boolean glowMaskBottomAnchored = true;
    private static final Matrix4f baseGlowEffectInverse = new Matrix4f();
    private static final Vector3f baseGlowMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static float baseGlowMaskShape;
    private static boolean baseGlowEffectActive;
    private static boolean baseGlowMaskBottomAnchored = true;
    private static final GradeMaskState gradeBrightnessMask = new GradeMaskState();
    private static final GradeMaskState gradeContrastMask = new GradeMaskState();
    private static final GradeMaskState gradeHueMask = new GradeMaskState();
    private static final GradeMaskState gradeSaturationMask = new GradeMaskState();
    private static float formColorR = 1F;
    private static float formColorG = 1F;
    private static float formColorB = 1F;
    private static float formColorA = 1F;
    private static boolean colorTintMasked;
    private static float baseFormColorR = 1F;
    private static float baseFormColorG = 1F;
    private static float baseFormColorB = 1F;
    private static float baseFormColorA = 1F;
    private static boolean baseColorTintMasked;
    private static float formColorGradeBrightness;
    private static float formColorGradeContrast;
    private static float formColorGradeHue;
    private static float formColorGradeSaturation;
    private static float baseFormColorGradeBrightness;
    private static float baseFormColorGradeContrast;
    private static float baseFormColorGradeHue;
    private static float baseFormColorGradeSaturation;
    private static final EffectTransform baseGradeBrightnessTransform = new EffectTransform();
    private static final EffectTransform baseGradeContrastTransform = new EffectTransform();
    private static final EffectTransform baseGradeHueTransform = new EffectTransform();
    private static final EffectTransform baseGradeSaturationTransform = new EffectTransform();
    private static boolean suppressShapeKeyMainPassGlow;

    /* 1x1 white texture used as the albedo source during the paint overlay pass. */
    private static NativeImageBackedTexture whiteTexture;
    /* Scene color copy for ColorGradeOverlay (Iris-lit pixels → FormColorGrade). */
    private static Texture gradeSceneColor;

    /* Saved GL state for the paint overlay pass (restored in endPaintOverlayPass). */
    private static int savedDepthFunc;
    private static boolean savedDepthMask;
    private static boolean savedPolygonOffsetFill;
    private static boolean savedCullEnabled;

    private static final class GradeMaskState
    {
        private final Matrix4f inverse = new Matrix4f();
        private final Vector3f half = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
        private boolean active;
        private boolean bottomAnchored = true;
        private float shape;

        private void set(EffectTransform transform)
        {
            EffectTransformMath.buildInverseMatrix(transform, this.inverse);
            this.active = EffectTransformMath.isTransformActive(transform);
            this.shape = transform == null || transform.shape == null ? 0F : transform.shape.id;
            EffectTransformMath.resolveModelMaskHalfExtents(transform, this.half);
            this.bottomAnchored = true;
        }

        private void clear()
        {
            this.inverse.identity();
            this.active = false;
            this.bottomAnchored = true;
            this.shape = 0F;
            this.half.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
        }

        private void upload(ShaderProgram shader, String prefix)
        {
            /* 1.21.11: GlUniform class removed; uniforms accessed via shader fields */
        }
    }

    private static final List<PaintOverlayEntry> paintOverlayQueue = new ArrayList<>();

    private static final class PaintOverlayEntry
    {
        private final Matrix4f projection;
        private final Matrix4f modelView;
        private final boolean synced;
        private final boolean fullModel;
        private final boolean colorTint;
        private final boolean colorGrade;
        private final boolean vanillaComposite;
        private final boolean depthWrite;
        private final boolean depthTest;
        private final Runnable draw;

        private PaintOverlayEntry(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean colorTint, boolean colorGrade, boolean vanillaComposite, boolean depthWrite, boolean depthTest, Runnable draw)
        {
            this.projection = projection;
            this.modelView = modelView;
            this.synced = synced;
            this.fullModel = fullModel;
            this.colorTint = colorTint;
            this.colorGrade = colorGrade;
            this.vanillaComposite = vanillaComposite;
            this.depthWrite = depthWrite;
            this.depthTest = depthTest;
            this.draw = draw;
        }
    }

    /**
     * Full root matrix for deferred Iris paint overlays (terrain/camera matrix already baked in).
     */
    public static Matrix4f capturePaintOverlayRootMatrix(Matrix4f rootStackMatrix)
    {
        return new Matrix4f().mul(rootStackMatrix);
    }

    public static void clearPaintOverlayQueue()
    {
        paintOverlayQueue.clear();
    }

    public static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, false, false, false, true, true, draw);
    }

    public static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, false, false, true, true, draw);
    }

    /**
     * Queues a full translucent mesh redraw for after Iris compositing.
     * {@code depthWrite} true = character meshes (self-occlusion); false = flat panels (keep scene depth / fog).
     */
    public static void submitDeferredTranslucentModel(Runnable draw)
    {
        /* Flat / thin translucent meshes z-fight when depth is rewritten after composite.
         * Character self-occlusion uses the two-arg overload with depthWrite true. */
        submitDeferredTranslucentModel(draw, false, true);
    }

    public static void submitDeferredTranslucentModel(Runnable draw, boolean depthWrite)
    {
        submitDeferredTranslucentModel(draw, depthWrite, true);
    }

    /**
     * @param depthTest false for zero-thickness billboards — post-Iris depth does not match
     *                  captured matrices and LEQUAL produces stippled grass bleed-through.
     */
    public static void submitDeferredTranslucentModel(Runnable draw, boolean depthWrite, boolean depthTest)
    {
        enqueuePaintOverlay(
            new Matrix4f(),
            new Matrix4f(),
            false,
            true,
            false,
            depthWrite,
            depthTest,
            draw
        );
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean depthWrite, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, fullModel, false, depthWrite, true, draw);
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean depthWrite, boolean depthTest, Runnable draw)
    {

        enqueuePaintOverlay(projection, modelView, synced, fullModel, false, depthWrite, depthTest, draw);
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean colorTint, boolean depthWrite, boolean depthTest, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, fullModel, colorTint, false, depthWrite, depthTest, draw);
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean colorTint, boolean colorGrade, boolean depthWrite, boolean depthTest, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, fullModel, colorTint, colorGrade, false, depthWrite, depthTest, draw);
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean colorTint, boolean colorGrade, boolean vanillaComposite, boolean depthWrite, boolean depthTest, Runnable draw)
    {

        /* Shadow-pass matrices are light-space (Iris and IRLights bake). Flushing them on the
         * color buffer draws tint/paint ghosts at wrong NDC (screen-edge masks when a light
         * touches a colored actor, or tiny blobs at center for Iris shadows). */
        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

        PaintOverlayEntry entry = new PaintOverlayEntry(
            new Matrix4f(projection),
            new Matrix4f(modelView),
            synced,
            fullModel,
            colorTint,
            colorGrade,
            vanillaComposite,
            depthWrite,
            depthTest,
            draw
        );

        if (BBSRendering.shouldDeferPaintOverlayToFrameEnd())
        {
            paintOverlayQueue.add(entry);
        }
        else
        {
            if (colorGrade && !captureGradeSceneColor())
            {
                return;
            }

            ModelVAORenderer.runPaintOverlayEntry(entry, false);
        }
    }

    /**
     * After Iris composite: run vanilla entity/BE draws with ColorModulator (no BBS paint pass).
     * Used for structure chests/beds where gbuffer ignores setShaderColor and paint overlays break shading.
     */
    public static void submitVanillaPostComposite(Runnable draw)
    {
        enqueuePaintOverlay(
            new Matrix4f(),
            new Matrix4f(),
            false,
            false,
            false,
            false,
            true,
            true,
            true,
            draw
        );
    }

    private static void runPaintOverlayEntry(PaintOverlayEntry entry, boolean restoreFramebuffer)
    {
        if (restoreFramebuffer)
        {
            BBSRendering.ensurePaintOverlayTargetFramebuffer();
        }

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);

        try
        {
            paintOverlaySynced = entry.synced;
            entry.draw.run();
        }
        finally
        {
            paintOverlaySynced = false;
        }
    }

    /**
     * Queues a paint/glow overlay for {@link #flushPaintOverlayQueue()} at the end of the
     * world frame.
     */
    public static void submitPaintOverlay(boolean synced, Runnable draw)
    {
        ModelVAORenderer.enqueuePaintOverlay(
            new Matrix4f(),
            new Matrix4f(),
            synced,
            draw
        );
    }

    /**
     * Queues a multiply color-mask overlay after Iris composite so FormColorTint keeps pack
     * lighting/shadows instead of redrawing the whole mesh with the unlit BBS path.
     */
    public static void submitColorTintOverlay(Runnable draw)
    {
        ModelVAORenderer.enqueuePaintOverlay(
            new Matrix4f(),
            new Matrix4f(),
            false,
            false,
            true,
            false,
            true,
            true,
            draw
        );
    }

    /**
     * Queues a post-composite regrade of Iris-lit model pixels (scene color → FormColorGrade).
     * Keeps pack lighting/shadows; avoids binding BBS during the gbuffer pass.
     */
    public static void submitColorGradeOverlay(Runnable draw)
    {
        ModelVAORenderer.enqueuePaintOverlay(
            new Matrix4f(),
            new Matrix4f(),
            false,
            false,
            false,
            true,
            false,
            true,
            draw
        );
    }

    /**
     * Queues a paint/glow overlay for {@link #flushPaintOverlayQueue()} at the end of the
     * world frame.
     */
    public static void submitPaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, draw);
    }

    public static void submitPaintOverlay(Matrix4f projection, Matrix4f modelView, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, draw);
    }

    public static boolean hasQueuedPaintOverlays()
    {
        return !paintOverlayQueue.isEmpty();
    }

    /**
     * Runs deferred paint overlay draws. Prefer the final framebuffer at world-render end
     * ({@code restoreFramebuffer = true}). When compositing under soft post-deferred forms
     * during Iris {@code beginTranslucents}, pass {@code false} so draws stay on Iris'
     * already-bound translucent target (rebinding Minecraft's main FB loses paint).
     */
    public static void flushPaintOverlayQueue()
    {
        flushPaintOverlayQueue(true);
    }

    public static void flushPaintOverlayQueue(boolean restoreFramebuffer)
    {
        if (paintOverlayQueue.isEmpty())
        {
            return;
        }

        try
        {
            boolean needsSceneCapture = false;

            for (PaintOverlayEntry entry : paintOverlayQueue)
            {
                if (entry.colorGrade)
                {
                    needsSceneCapture = true;

                    break;
                }
            }

            if (needsSceneCapture)
            {
                BBSRendering.ensurePaintOverlayTargetFramebuffer();

                if (!captureGradeSceneColor())
                {
                    /* Keep Iris-lit mesh; skip broken regrade rather than painting black. */
                    paintOverlayQueue.removeIf(entry -> entry.colorGrade);
                }
            }

            /* Paint/glow overlays first, then full soft-model redraws (Opacity "No shading"
             * path) so translucency composites over painted actors behind the soft form. */
            paintOverlayQueue.sort((a, b) -> Boolean.compare(a.fullModel, b.fullModel));

            for (PaintOverlayEntry entry : paintOverlayQueue)
            {
                ModelVAORenderer.runPaintOverlayEntry(entry, restoreFramebuffer);
            }
        }
        finally
        {
            paintOverlayQueue.clear();
        }
    }

    public static void beginPaintPass()
    {
        paintPass = true;
    }

    public static void endPaintPass()
    {
        paintPass = false;
    }


    public static boolean isPaintOverlayPass()
    {
        return paintOverlayPass;
    }

    public static boolean isPaintOverlaySynced()
    {
        return paintOverlaySynced;
    }

    public static boolean isPaintPass()
    {
        return paintPass;
    }

    public static float getBasePaintR()
    {
        return baseR;
    }

    public static float getBasePaintG()
    {
        return baseG;
    }

    public static float getBasePaintB()
    {
        return baseB;
    }

    public static float getBasePaintStrength()
    {
        return baseStrength;
    }

    public static void setPaint(float r, float g, float b, float strength)
    {
        baseR = r;
        baseG = g;
        baseB = b;
        baseStrength = strength;

        paintR = r;
        paintG = g;
        paintB = b;
        paintStrength = strength;
    }

    public static void setGroupPaint(float r, float g, float b, float strength)
    {
        if (strength > 0F)
        {
            paintR = r;
            paintG = g;
            paintB = b;
            paintStrength = strength;
        }
        else
        {
            paintR = baseR;
            paintG = baseG;
            paintB = baseB;
            paintStrength = baseStrength;
        }
    }

    public static void setGlow(GlowSettings settings, float colorR, float colorG, float colorB)
    {
        setGlow(settings, colorR, colorG, colorB, null);
    }

    public static void setGlow(GlowSettings settings, float colorR, float colorG, float colorB, Color legacyColor)
    {
        float strength = settings.resolveIntensity(legacyColor);

        baseGlowR = colorR;
        baseGlowG = colorG;
        baseGlowB = colorB;
        baseGlowStrength = strength;

        glowR = colorR;
        glowG = colorG;
        glowB = colorB;
        glowStrength = strength;
    }

    public static void setGlowing(float r, float g, float b, float strength, float radius)
    {
        GlowSettings settings = new GlowSettings(strength, radius);

        setGlow(settings, r, g, b);
    }

    public static void setGroupGlowing(float r, float g, float b, float strength)
    {
        glowR = r;
        glowG = g;
        glowB = b;
        glowStrength = strength;
    }

    public static void clearGlowing()
    {
        baseGlowR = 0F;
        baseGlowG = 0F;
        baseGlowB = 0F;
        baseGlowStrength = 0F;

        glowR = 0F;
        glowG = 0F;
        glowB = 0F;
        glowStrength = 0F;
    }

    public static boolean isGlowingUniformActive()
    {
        return glowingUniformActive;
    }

    public static float getBaseGlowingStrength()
    {
        return baseGlowStrength;
    }

    public static float getBaseGlowingR()
    {
        return baseGlowR;
    }

    public static float getBaseGlowingG()
    {
        return baseGlowG;
    }

    public static float getBaseGlowingB()
    {
        return baseGlowB;
    }

    public static void clearPaint()
    {
        baseR = 0F;
        baseG = 0F;
        baseB = 0F;
        baseStrength = 0F;

        paintR = 0F;
        paintG = 0F;
        paintB = 0F;
        paintStrength = 0F;
    }
    /**
     * Draw an {@link IModelVAO} through the immediate model RenderLayer. The 1.21.11 rewrite removed
     * ShaderProgram.bind()/unbind() and the imperative uniform/sampler/fog/light setup; the built-in
     * uniforms now live in the std140 UBOs (DynamicTransforms/Projection/Fog/Lighting) that
     * {@link BBSShaders#getModelLayer()} uploads per draw. The geometry is baked CPU-side into a
     * BufferBuilder (matching the cubic immediate path) and submitted through that layer.
     */
    public static void render(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BuiltBuffer built = write(modelVAO, stack, r, g, b, a, light, overlay);

        if (built != null)
        {
            BBSShaders.getModelLayer().draw(built);
        }
        else
        {
            formRootInverse.identity();
        }
    }

    /**
     * Per-bone paint mask override (same idea as {@link #setGroupPaint}). When the group has an
     * active transform/shape, it replaces the form paint mask for this draw; otherwise restore base.
     */
    public static void setGroupPaintEffectTransform(EffectTransform transform)
    {
        if (EffectTransformMath.isTransformActive(transform))
        {
            EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
            paintEffectActive = true;
            paintMaskShape = transform.shape == null ? 0F : transform.shape.id;
            EffectTransformMath.resolveModelMaskHalfExtents(transform, paintMaskHalf);
            paintMaskBottomAnchored = basePaintMaskBottomAnchored;
        }
        else
        {
            paintEffectInverse.set(basePaintEffectInverse);
            paintMaskHalf.set(basePaintMaskHalf);
            paintMaskShape = basePaintMaskShape;
            paintEffectActive = basePaintEffectActive;
            paintMaskBottomAnchored = basePaintMaskBottomAnchored;
        }
    }

    public static void clearPaintEffectTransform()
    {
        if (!colorEffectActive && !glowEffectActive)
        {
            formRootInverse.identity();
        }

        paintEffectInverse.identity();
        paintEffectActive = false;
        paintMaskBottomAnchored = true;
        paintMaskShape = 0F;
        paintMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
        basePaintEffectInverse.identity();
        basePaintEffectActive = false;
        basePaintMaskBottomAnchored = true;
        basePaintMaskShape = 0F;
        basePaintMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
    }

    public static void setColorEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf)
    {
        if (formRootInverseMatrix != null)
        {
            formRootInverse.set(formRootInverseMatrix);
        }
        else
        {
            formRootInverse.identity();
        }

        EffectTransformMath.buildInverseMatrix(transform, colorEffectInverse);
        colorEffectActive = EffectTransformMath.isTransformActive(transform);
        colorMaskShape = transform == null || transform.shape == null ? 0F : transform.shape.id;

        if (maskHalf != null)
        {
            colorMaskHalf.set(maskHalf);
        }
        else
        {
            EffectTransformMath.resolveModelMaskHalfExtents(transform, colorMaskHalf);
        }

        colorMaskBottomAnchored = true;
        snapshotColorEffectBase();
    }

    /**
     * Per-bone color (tint) mask override. Active bone transform replaces the form color mask
     * for this draw; otherwise restore the form/base mask.
     */
    public static void setGroupColorEffectTransform(EffectTransform transform)
    {
        if (EffectTransformMath.isTransformActive(transform))
        {
            EffectTransformMath.buildInverseMatrix(transform, colorEffectInverse);
            colorEffectActive = true;
            colorMaskShape = transform.shape == null ? 0F : transform.shape.id;
            EffectTransformMath.resolveModelMaskHalfExtents(transform, colorMaskHalf);
            colorMaskBottomAnchored = baseColorMaskBottomAnchored;
        }
        else
        {
            colorEffectInverse.set(baseColorEffectInverse);
            colorMaskHalf.set(baseColorMaskHalf);
            colorMaskShape = baseColorMaskShape;
            colorEffectActive = baseColorEffectActive;
            colorMaskBottomAnchored = baseColorMaskBottomAnchored;
        }
    }

    public static void setGlowEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf)
    {
        setGlowEffectTransform(formRootInverseMatrix, transform, maskHalf, true);
    }

    public static void setGlowEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf, boolean bottomAnchoredY)
    {
        if (formRootInverseMatrix != null)
        {
            formRootInverse.set(formRootInverseMatrix);
        }
        else
        {
            formRootInverse.identity();
        }

        EffectTransformMath.buildInverseMatrix(transform, glowEffectInverse);
        glowEffectActive = EffectTransformMath.isTransformActive(transform);
        glowMaskShape = transform == null || transform.shape == null ? 0F : transform.shape.id;

        if (maskHalf != null)
        {
            glowMaskHalf.set(maskHalf);
        }
        else
        {
            EffectTransformMath.resolveModelMaskHalfExtents(transform, glowMaskHalf);
        }

        glowMaskBottomAnchored = bottomAnchoredY;
        snapshotGlowEffectBase();
    }

    /**
     * Per-bone glow mask override. Active bone transform replaces the form glow mask for this
     * draw; otherwise restore the form/base mask.
     */
    public static void setGroupGlowEffectTransform(EffectTransform transform)
    {
        if (EffectTransformMath.isTransformActive(transform))
        {
            EffectTransformMath.buildInverseMatrix(transform, glowEffectInverse);
            glowEffectActive = true;
            glowMaskShape = transform.shape == null ? 0F : transform.shape.id;
            EffectTransformMath.resolveModelMaskHalfExtents(transform, glowMaskHalf);
            glowMaskBottomAnchored = baseGlowMaskBottomAnchored;
        }
        else
        {
            glowEffectInverse.set(baseGlowEffectInverse);
            glowMaskHalf.set(baseGlowMaskHalf);
            glowMaskShape = baseGlowMaskShape;
            glowEffectActive = baseGlowEffectActive;
            glowMaskBottomAnchored = baseGlowMaskBottomAnchored;
        }
    }

    public static void clearGlowEffectTransform()
    {
        if (!paintEffectActive && !colorEffectActive)
        {
            formRootInverse.identity();
        }

        glowEffectInverse.identity();
        glowEffectActive = false;
        glowMaskBottomAnchored = true;
        glowMaskShape = 0F;
        glowMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
        baseGlowEffectInverse.identity();
        baseGlowEffectActive = false;
        baseGlowMaskBottomAnchored = true;
        baseGlowMaskShape = 0F;
        baseGlowMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
    }

    public static void setFormColorTint(float r, float g, float b, float a)
    {
        formColorR = r;
        formColorG = g;
        formColorB = b;
        formColorA = a;
        colorTintMasked = true;
        baseFormColorR = r;
        baseFormColorG = g;
        baseFormColorB = b;
        baseFormColorA = a;
        baseColorTintMasked = true;
    }

    public static void clearFormColorTint()
    {
        formColorR = 1F;
        formColorG = 1F;
        formColorB = 1F;
        formColorA = 1F;
        colorTintMasked = false;
        baseFormColorR = 1F;
        baseFormColorG = 1F;
        baseFormColorB = 1F;
        baseFormColorA = 1F;
        baseColorTintMasked = false;
    }

    /**
     * Per-bone FormColorTint override when the bone owns a spatial color mask. Otherwise restore
     * the form/base tint so vertex-multiplied bone colors keep working without a transform.
     */
    public static void setGroupFormColorTint(Color color)
    {
        if (color != null && color.hasActiveTransform())
        {
            formColorR = color.r;
            formColorG = color.g;
            formColorB = color.b;
            formColorA = color.a;
            colorTintMasked = true;
        }
        else
        {
            formColorR = baseFormColorR;
            formColorG = baseFormColorG;
            formColorB = baseFormColorB;
            formColorA = baseFormColorA;
            colorTintMasked = baseColorTintMasked;
        }
    }

    public static void setFormColorGrade(float brightness, float contrast, float hue, float saturation)
    {
        baseFormColorGradeBrightness = brightness;
        baseFormColorGradeContrast = contrast;
        baseFormColorGradeHue = hue;
        baseFormColorGradeSaturation = saturation;
        applyFormColorGrade(brightness, contrast, hue, saturation);
    }

    public static void setGradeEffectTransforms(Color color)
    {
        if (color == null)
        {
            clearGradeEffectTransforms();
            clearBaseGradeEffectTransforms();

            return;
        }

        copyEffectTransform(baseGradeBrightnessTransform, color.brightnessTransform);
        copyEffectTransform(baseGradeContrastTransform, color.contrastTransform);
        copyEffectTransform(baseGradeHueTransform, color.hueTransform);
        copyEffectTransform(baseGradeSaturationTransform, color.saturationTransform);
        applyGradeEffectTransforms(color.brightnessTransform, color.contrastTransform, color.hueTransform, color.saturationTransform);
    }

    public static void setGradeEffectTransforms(EffectTransform brightness, EffectTransform contrast, EffectTransform hue, EffectTransform saturation)
    {
        copyEffectTransform(baseGradeBrightnessTransform, brightness);
        copyEffectTransform(baseGradeContrastTransform, contrast);
        copyEffectTransform(baseGradeHueTransform, hue);
        copyEffectTransform(baseGradeSaturationTransform, saturation);
        applyGradeEffectTransforms(brightness, contrast, hue, saturation);
    }

    /**
     * Per-bone Color Grade override (same idea as {@link #setGroupPaint}). When the group
     * has adjustments, they replace the form/base grade for this draw; otherwise restore base.
     */
    public static void setGroupFormColorGrade(Color color)
    {
        if (color != null && (color.hasColorAdjustments() || color.hasActiveGradeTransform()))
        {
            applyFormColorGrade(color.brightness, color.contrast, color.hue, color.saturation);
            applyGradeEffectTransforms(color.brightnessTransform, color.contrastTransform, color.hueTransform, color.saturationTransform);
        }
        else
        {
            applyFormColorGrade(baseFormColorGradeBrightness, baseFormColorGradeContrast, baseFormColorGradeHue, baseFormColorGradeSaturation);
            applyGradeEffectTransforms(baseGradeBrightnessTransform, baseGradeContrastTransform, baseGradeHueTransform, baseGradeSaturationTransform);
        }
    }

    private static BuiltBuffer write(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        modelVAO.writeImmediate(builder, stack, r, g, b, a, light, overlay);

        return builder.endNullable();
    }

    private static void applyGradeEffectTransforms(EffectTransform brightness, EffectTransform contrast, EffectTransform hue, EffectTransform saturation)
    {
        gradeBrightnessMask.set(brightness);
        gradeContrastMask.set(contrast);
        gradeHueMask.set(hue);
        gradeSaturationMask.set(saturation);
    }

    private static void copyEffectTransform(EffectTransform target, EffectTransform source)
    {
        EffectTransform value = source == null ? new EffectTransform() : source;

        target.offsetX = value.offsetX;
        target.offsetY = value.offsetY;
        target.offsetZ = value.offsetZ;
        target.scaleX = value.scaleX;
        target.scaleY = value.scaleY;
        target.scaleZ = value.scaleZ;
        target.rotateX = value.rotateX;
        target.rotateY = value.rotateY;
        target.rotateZ = value.rotateZ;
        target.shape = value.shape;
    }

    private static void clearBaseGradeEffectTransforms()
    {
        copyEffectTransform(baseGradeBrightnessTransform, null);
        copyEffectTransform(baseGradeContrastTransform, null);
        copyEffectTransform(baseGradeHueTransform, null);
        copyEffectTransform(baseGradeSaturationTransform, null);
    }

    public static void clearGradeEffectTransforms()
    {
        gradeBrightnessMask.clear();
        gradeContrastMask.clear();
        gradeHueMask.clear();
        gradeSaturationMask.clear();
    }

    public static void clearFormColorGrade()
    {
        baseFormColorGradeBrightness = 0F;
        baseFormColorGradeContrast = 0F;
        baseFormColorGradeHue = 0F;
        baseFormColorGradeSaturation = 0F;
        formColorGradeBrightness = 0F;
        formColorGradeContrast = 0F;
        formColorGradeHue = 0F;
        formColorGradeSaturation = 0F;
        clearBaseGradeEffectTransforms();
        clearGradeEffectTransforms();
        FormColorGradePatch.clear();
    }

    public static void clearColorEffectTransform()
    {
        if (!paintEffectActive && !glowEffectActive)
        {
            formRootInverse.identity();
        }

        colorEffectInverse.identity();
        colorEffectActive = false;
        colorMaskBottomAnchored = true;
        colorMaskShape = 0F;
        colorMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
        baseColorEffectInverse.identity();
        baseColorEffectActive = false;
        baseColorMaskBottomAnchored = true;
        baseColorMaskShape = 0F;
        baseColorMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
    }

    private static void snapshotPaintEffectBase()
    {
        basePaintEffectInverse.set(paintEffectInverse);
        basePaintMaskHalf.set(paintMaskHalf);
        basePaintMaskShape = paintMaskShape;
        basePaintEffectActive = paintEffectActive;
        basePaintMaskBottomAnchored = paintMaskBottomAnchored;
    }

    private static void snapshotColorEffectBase()
    {
        baseColorEffectInverse.set(colorEffectInverse);
        baseColorMaskHalf.set(colorMaskHalf);
        baseColorMaskShape = colorMaskShape;
        baseColorEffectActive = colorEffectActive;
        baseColorMaskBottomAnchored = colorMaskBottomAnchored;
    }

    private static void snapshotGlowEffectBase()
    {
        baseGlowEffectInverse.set(glowEffectInverse);
        baseGlowMaskHalf.set(glowMaskHalf);
        baseGlowMaskShape = glowMaskShape;
        baseGlowEffectActive = glowEffectActive;
        baseGlowMaskBottomAnchored = glowMaskBottomAnchored;
    }

    private static Matrix4f overlayFormRootInverse()
    {
        if (usesCapturedModelView())
        {
            return overlayFormRootInverse.identity();
        }

        return formRootInverse;
    }

    public static void render(ShaderProgram shader, IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        /* 1.21.11: GL30 VAO bind removed; render through the new BufferBuilder path */
        render(modelVAO, stack, r, g, b, a, light, overlay);
    }

    public static void setupUniforms(MatrixStack stack, ShaderProgram shader)
    {
        /* 1.21.11: uniforms are set through the RenderPipeline, not manually */
    }

    /**
     * CPU shape-key path writes positions/normals already transformed by the render stack.
     * ModelViewMat must not multiply that stack again (or meshes vanish at the origin when
     * {@code drawWithGlobalProgram} keeps only the camera matrix), and NormalMat must stay
     * identity or diffuse lighting is applied twice.
     */
    public static void setupUniformsCpuPretransformed(ShaderProgram shader)
    {
        /* 1.21.11: uniforms are set through the RenderPipeline, not manually */
    }

    private static void setupUniforms(MatrixStack stack, ShaderProgram shader, boolean cpuPretransformed)
    {
        /* 1.21.11: uniforms are set through the RenderPipeline, not manually via GlUniform */
    }

    private static void setModelViewUniform(MatrixStack stack, ShaderProgram shader)
    {
        /* 1.21.11: model-view matrix is set through the RenderPipeline */
    }

    private static boolean usesCapturedModelView()
    {
        return false;
    }

    public static void setPaintEffectTransform(Matrix4f rootInverse, EffectTransform transform, Vector3f maskHalf)
    {
        if (transform == null)
        {
            paintEffectActive = false;
            return;
        }

        EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
        paintEffectActive = EffectTransformMath.isTransformActive(transform);
        paintMaskShape = transform.shape == null ? 0F : transform.shape.id;

        if (maskHalf != null)
        {
            paintMaskHalf.set(maskHalf);
        }
        else
        {
            EffectTransformMath.resolveModelMaskHalfExtents(transform, paintMaskHalf);
        }

        paintMaskBottomAnchored = true;
    }

    public static void runWithPaintOverlayPass(boolean b, Runnable runnable)
    {
        runnable.run();
    }

    public static void setSuppressShapeKeyMainPassGlow(boolean suppress)
    {}

    public static boolean isSuppressShapeKeyMainPassGlow()
    {
        return suppressShapeKeyMainPassGlow;
    }

    public static void beginCpuGeometry(ShaderProgram shader)
    {}

    public static void clearTextureBlend()
    {}

    public static void setTextureBlend(Link link, float f)
    {}

    public static boolean captureGradeSceneColor()
    {
        return false;
    }

    private static void applyFormColorGrade(float brightness, float contrast, float hue, float saturation)
    {}

    private static void beginDeferredTranslucentModelPass(boolean depthWrite, boolean depthTest)
    {}

    private static void endDeferredTranslucentModelPass()
    {}

    private static void beginColorGradeOverlayPass()
    {}

    private static void endColorGradeOverlayPass()
    {}

    private static void beginColorTintOverlayPass()
    {}

    private static void endColorTintOverlayPass()
    {}

    private static void beginVanillaPostCompositePass()
    {}

    private static void endVanillaPostCompositePass()
    {}

    private static void beginPaintOverlayPass(boolean synced)
    {}

    private static void endPaintOverlayPass()
    {}
}
