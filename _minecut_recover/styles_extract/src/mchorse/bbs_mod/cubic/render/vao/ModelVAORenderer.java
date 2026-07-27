package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_284;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_5944;
import net.minecraft.class_757;
import net.minecraft.class_8251;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class ModelVAORenderer
{
    private static final Matrix3f IDENTITY_NORMAL = new Matrix3f();
    private static final Matrix4f IDENTITY_MODEL_VIEW = new Matrix4f();

    /* FS-style paint overlay uniform state (rgb + strength). Set by form renderers before a draw and reset after.
     * "base" holds the whole-form paint; "current" is what the uniform uses and can be overridden per model group (bone). */
    private static float baseR;
    private static float baseG;
    private static float baseB;
    private static float baseStrength;

    private static float paintR;
    private static float paintG;
    private static float paintB;
    private static float paintStrength;

    private static float baseGlowR;
    private static float baseGlowG;
    private static float baseGlowB;
    private static float baseGlowStrength;

    private static float glowR;
    private static float glowG;
    private static float glowB;
    private static float glowStrength;
    private static boolean glowPaintOnly;

    private static boolean glowingUniformActive;

    private static boolean textureBlendActive;
    private static float textureBlendFactor;
    private static Link textureBlendTo;

    /* When true, the model is being drawn as a shader-pack paint overlay pass. Groups still sample their
     * real skin texture so transparent UV regions are discarded; only textured pixels receive paint. */
    private static boolean paintPass;
    private static boolean paintOverlayPass;
    private static boolean paintOverlaySynced;
    /* Multiply Iris-lit pixels by FormColorTint inside the color mask (keeps pack lighting/shadows). */
    private static boolean colorTintOverlayPass;
    /* Replace Iris-lit model pixels with FormColorGrade(sceneColor) after composite. */
    private static boolean colorGradeOverlayPass;
    /* Captured-matrix redraw after Iris (or immediate low-opacity bypass) — not the paint-overlay shader branch. */
    private static boolean deferredTranslucentPass;

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
    private static final GradeMaskState gradeBrightnessMask = new GradeMaskState();
    private static final GradeMaskState gradeContrastMask = new GradeMaskState();
    private static final GradeMaskState gradeHueMask = new GradeMaskState();
    private static final GradeMaskState gradeSaturationMask = new GradeMaskState();
    private static float formColorR = 1F;
    private static float formColorG = 1F;
    private static float formColorB = 1F;
    private static float formColorA = 1F;
    private static boolean colorTintMasked;
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
    private static class_1043 whiteTexture;
    /* Scene color copy for ColorGradeOverlay (Iris-lit pixels → FormColorGrade). */
    private static mchorse.bbs_mod.graphics.texture.Texture gradeSceneColor;

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
            this.setModel(transform);
        }

        private void setModel(EffectTransform transform)
        {
            EffectTransformMath.buildInverseMatrix(transform, this.inverse);
            this.active = EffectTransformMath.isTransformActive(transform);
            this.shape = transform == null || transform.shape == null ? 0F : transform.shape.id;
            EffectTransformMath.resolveModelMaskHalfExtents(transform, this.half);
            this.bottomAnchored = true;
        }

        /**
         * Structure Color Grade: UI scale 1 covers the full AABB (same as paint / Blend Color).
         */
        private void setStructure(EffectTransform transform, float sizeX, float sizeY, float sizeZ)
        {
            EffectTransformMath.buildInverseMatrix(transform, this.inverse);
            this.active = EffectTransformMath.isTransformActive(transform);
            this.shape = transform == null || transform.shape == null ? 0F : transform.shape.id;
            EffectTransformMath.resolveStructureMaskHalfExtents(transform, this.half, sizeX, sizeY, sizeZ);
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

        private void upload(class_5944 shader, String prefix)
        {
            class_284 inverseUniform = shader.method_34582(prefix + "Inverse");

            if (inverseUniform != null)
            {
                inverseUniform.method_1250(this.inverse);
            }

            class_284 activeUniform = shader.method_34582(prefix + "Active");

            if (activeUniform != null)
            {
                activeUniform.method_1251(this.active ? 1F : 0F);
            }

            class_284 halfUniform = shader.method_34582(prefix + "Half");

            if (halfUniform != null)
            {
                halfUniform.method_1249(this.half.x, this.half.y, this.half.z);
            }

            class_284 bottomUniform = shader.method_34582(prefix + "BottomAnchored");

            if (bottomUniform != null)
            {
                bottomUniform.method_1251(this.bottomAnchored ? 1F : 0F);
            }

            class_284 shapeUniform = shader.method_34582(prefix + "Shape");

            if (shapeUniform != null)
            {
                shapeUniform.method_1251(this.shape);
            }
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
        return new Matrix4f(RenderSystem.getModelViewMatrix()).mul(rootStackMatrix);
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
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
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

        /* Shadow-pass matrices are light-space; flushing them on the color buffer draws tiny
         * paint blobs at the screen center (one per model block / form that queued paint). */
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
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
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

        class_757 gameRenderer = class_310.method_1551().field_1773;

        gameRenderer.method_22974().method_3316();
        gameRenderer.method_22975().method_23209();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShader(BBSShaders::getModel);

        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f savedModelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        try
        {
            paintOverlaySynced = entry.synced;

            RenderSystem.setProjectionMatrix(entry.projection, class_8251.field_43361);

            MatrixStackUtils.pushIdentityModelView();

            if (entry.fullModel)
            {
                beginDeferredTranslucentModelPass(entry.depthWrite, entry.depthTest);
            }
            else if (entry.colorGrade)
            {
                beginColorGradeOverlayPass();
            }
            else if (entry.colorTint)
            {
                beginColorTintOverlayPass();
            }
            else if (entry.vanillaComposite)
            {
                beginVanillaPostCompositePass();
            }
            else
            {
                beginPaintOverlayPass(entry.synced);
            }

            try
            {
                entry.draw.run();
            }
            finally
            {
                if (entry.fullModel)
                {
                    endDeferredTranslucentModelPass();
                }
                else if (entry.colorGrade)
                {
                    endColorGradeOverlayPass();
                }
                else if (entry.colorTint)
                {
                    endColorTintOverlayPass();
                }
                else if (entry.vanillaComposite)
                {
                    endVanillaPostCompositePass();
                }
                else
                {
                    endPaintOverlayPass();
                }

                MatrixStackUtils.popModelView();
            }
        }
        finally
        {
            RenderSystem.setProjectionMatrix(savedProjection, class_8251.field_43361);

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

            modelViewStack.pushMatrix();
            modelViewStack.set(savedModelView);
            RenderSystem.applyModelViewMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();

            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    /**
     * Queues a paint/glow overlay for {@link #flushPaintOverlayQueue()} at the end of the
     * world frame.
     */
    public static void submitPaintOverlay(boolean synced, Runnable draw)
    {
        ModelVAORenderer.enqueuePaintOverlay(
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
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
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
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
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
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

    /**
     * Second-pass paint overlay for external shader packs. Re-draws the same geometry with the BBS
     * model shader so paint can be alpha-blended over the shader-pack first pass using the same
     * mix semantics as the no-shader path: mix(litTextureRgb, paintRgb, paintStrength).
     */
    public static void beginPaintOverlayPass(boolean synced)
    {
        beginPaintPass();
        paintOverlayPass = true;
        paintOverlaySynced = synced;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        /* Flat / extruded / billboard overlays need a large units bias — factor alone is not
         * enough for near-zero depth slope at distance (see FlatPaintOverlayPass). */
        GL11.glPolygonOffset(mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
    }

    /**
     * Post-Iris composite path for vanilla block-entity redraws. Pulls slightly toward the
     * camera without rewriting depth so the tinted pass does not z-fight the Iris-lit BE.
     */
    public static void beginVanillaPostCompositePass()
    {
        paintOverlayPass = false;
        paintOverlaySynced = false;
        colorTintOverlayPass = false;
        colorGradeOverlayPass = false;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -2F);
    }

    public static void endVanillaPostCompositePass()
    {
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.depthMask(savedDepthMask);

        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        GL11.glPolygonOffset(0F, 0F);

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Multiply the Iris-lit framebuffer by FormColorTint inside the color mask. Keeps pack
     * lighting/shadows while applying the spatial Color transform.
     */
    public static void beginColorTintOverlayPass()
    {
        colorTintOverlayPass = true;
        paintOverlayPass = false;
        paintOverlaySynced = false;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

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
        GL11.glPolygonOffset(mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
    }

    /**
     * Replace Iris-lit model pixels with FormColorGrade(sceneColor). Sampler3 holds the
     * pre-overlay scene copy from {@link #captureGradeSceneColor()}.
     */
    public static void beginColorGradeOverlayPass()
    {
        colorGradeOverlayPass = true;
        colorTintOverlayPass = false;
        paintOverlayPass = false;
        paintOverlaySynced = false;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        if (gradeSceneColor != null && gradeSceneColor.isValid())
        {
            RenderSystem.setShaderTexture(3, gradeSceneColor.id);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
    }

    public static void endColorGradeOverlayPass()
    {
        colorGradeOverlayPass = false;

        GL11.glPolygonOffset(0F, 0F);

        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    /**
     * Copy the current paint-overlay target color into {@link #gradeSceneColor} so
     * ColorGradeOverlay can sample Iris-lit pixels without feedback loops.
     *
     * @return true when Sampler3 has a valid scene copy for this frame
     */
    public static boolean captureGradeSceneColor()
    {
        net.minecraft.class_276 source = BBSRendering.getPaintOverlaySourceFramebuffer();

        if (source == null)
        {
            return false;
        }

        int width = source.field_1482;
        int height = source.field_1481;

        if (width <= 0 || height <= 0)
        {
            return false;
        }

        if (gradeSceneColor == null)
        {
            gradeSceneColor = new mchorse.bbs_mod.graphics.texture.Texture();
            gradeSceneColor.setFilter(GL11.GL_NEAREST);
        }

        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try
        {
            source.method_35610();
            gradeSceneColor.bind();

            if (gradeSceneColor.width != width || gradeSceneColor.height != height)
            {
                gradeSceneColor.setSize(width, height);
            }

            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        }
        finally
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
            BBSRendering.ensurePaintOverlayTargetFramebuffer();
        }

        return gradeSceneColor.isValid() && gradeSceneColor.width == width && gradeSceneColor.height == height;
    }

    /**
     * Bind the scene copy from {@link #captureGradeSceneColor()} to texture unit 3.
     */
    public static void bindGradeSceneColorTexture()
    {
        if (gradeSceneColor != null && gradeSceneColor.isValid())
        {
            RenderSystem.setShaderTexture(3, gradeSceneColor.id);
        }
    }

    public static void endColorTintOverlayPass()
    {
        colorTintOverlayPass = false;

        GL11.glPolygonOffset(0F, 0F);

        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    /**
     * Full translucent redraw after Iris composite — BBS model shader keeps low form alpha.
     * {@code depthWrite} true matches the no-shader path so render-depth panels can occlude
     * forms behind them. {@code depthTest} false for zero-thickness billboards whose captured
     * depth does not match the post-Iris depth buffer (stippled bleed-through).
     */
    public static void beginDeferredTranslucentModelPass(boolean depthWrite)
    {
        beginDeferredTranslucentModelPass(depthWrite, true);
    }

    public static void beginDeferredTranslucentModelPass(boolean depthWrite, boolean depthTest)
    {
        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        /* Captured full camera+entity matrix while RenderSystem model-view is identity.
         * Do not set paintOverlayPass — that enables the paint-only shader branch and would
         * discard textured geometry when PaintColor.a is 0. */
        deferredTranslucentPass = true;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.class_4535.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.class_4534.ONE_MINUS_SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.class_4535.ONE,
            com.mojang.blaze3d.platform.GlStateManager.class_4534.ONE_MINUS_SRC_ALPHA
        );

        if (depthTest)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
        else
        {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.depthMask(depthWrite);
        RenderSystem.enableCull();
    }

    public static void beginDeferredTranslucentModelPass()
    {
        beginDeferredTranslucentModelPass(true, true);
    }

    public static void endDeferredTranslucentModelPass()
    {
        deferredTranslucentPass = false;
        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    public static void endPaintOverlayPass()
    {
        endPaintPass();
        paintOverlayPass = false;
        paintOverlaySynced = false;

        GL11.glPolygonOffset(0F, 0F);

        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    public static boolean isPaintOverlayPass()
    {
        return paintOverlayPass;
    }

    public static boolean isColorTintOverlayPass()
    {
        return colorTintOverlayPass;
    }

    public static boolean isColorGradeOverlayPass()
    {
        return colorGradeOverlayPass;
    }

    public static boolean isDeferredTranslucentPass()
    {
        return deferredTranslucentPass;
    }

    private static boolean usesCapturedModelView()
    {
        return paintOverlayPass || deferredTranslucentPass || colorTintOverlayPass || colorGradeOverlayPass;
    }

    /**
     * Temporarily toggles the paint-overlay shader branch while a deferred Iris overlay draw is running.
     */
    public static void runWithPaintOverlayPass(boolean paintOverlay, Runnable draw)
    {
        boolean previous = paintOverlayPass;

        paintOverlayPass = paintOverlay;

        try
        {
            draw.run();
        }
        finally
        {
            paintOverlayPass = previous;
        }
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

    /**
     * Lazily builds (on the render thread) a 1x1 fully-white texture and returns its GL id. Sampling this
     * texture yields white, so a shader's texel * vertexColour becomes the vertex colour verbatim.
     */
    public static int getWhiteTextureId()
    {
        if (whiteTexture == null)
        {
            try
            {
                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

                image.setRGB(0, 0, 0xFFFFFFFF);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                ImageIO.write(image, "png", baos);

                whiteTexture = new class_1043(class_1011.method_4309(new ByteArrayInputStream(baos.toByteArray())));
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        return whiteTexture.method_4624();
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
        if (strength != 0F)
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
        glowPaintOnly = settings != null && settings.resolvePaintOnly();
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
        glowPaintOnly = false;
    }

    public static boolean isGlowPaintOnly()
    {
        return glowPaintOnly;
    }

    public static boolean isGlowingUniformActive()
    {
        return glowingUniformActive;
    }

    /**
     * CPU mesh builders emit vertices before draw-time uniform upload. Probe the active shader early so
     * shape-key geometry can skip vanilla brighten/light boosts when the BBS GlowingColor uniform applies.
     */
    public static boolean isSuppressShapeKeyMainPassGlow()
    {
        return suppressShapeKeyMainPassGlow;
    }

    public static void setSuppressShapeKeyMainPassGlow(boolean suppress)
    {
        suppressShapeKeyMainPassGlow = suppress;
    }

    public static void beginCpuGeometry(class_5944 shader)
    {
        class_284 glowingUniform = shader.method_34582("GlowingColor");

        glowingUniformActive = glowingUniform != null;
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

    public static void setTextureBlend(Link toTexture, float blend)
    {
        ModelVAORenderer.textureBlendActive = toTexture != null && blend > 0F && blend < 1F;
        ModelVAORenderer.textureBlendFactor = blend;
        ModelVAORenderer.textureBlendTo = toTexture;
    }

    public static void clearTextureBlend()
    {
        ModelVAORenderer.textureBlendActive = false;
        ModelVAORenderer.textureBlendFactor = 0F;
        ModelVAORenderer.textureBlendTo = null;
    }

    public static void setPaintEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf)
    {
        setPaintEffectTransform(formRootInverseMatrix, transform, maskHalf, true);
    }

    public static void setPaintEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf, boolean bottomAnchoredY)
    {
        if (formRootInverseMatrix != null)
        {
            formRootInverse.set(formRootInverseMatrix);
        }
        else
        {
            formRootInverse.identity();
        }

        EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
        paintEffectActive = EffectTransformMath.isTransformActive(transform);
        paintMaskShape = transform == null || transform.shape == null ? 0F : transform.shape.id;

        if (maskHalf != null)
        {
            paintMaskHalf.set(maskHalf);
        }
        else
        {
            EffectTransformMath.resolveModelMaskHalfExtents(transform, paintMaskHalf);
        }

        paintMaskBottomAnchored = bottomAnchoredY;
    }

    public static void clearPaintEffectTransform()
    {
        if (!colorEffectActive)
        {
            formRootInverse.identity();
        }

        paintEffectInverse.identity();
        paintEffectActive = false;
        paintMaskBottomAnchored = true;
        paintMaskShape = 0F;
        paintMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
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
    }

    public static void setFormColorTint(float r, float g, float b, float a)
    {
        formColorR = r;
        formColorG = g;
        formColorB = b;
        formColorA = a;
        colorTintMasked = true;
    }

    public static void clearFormColorTint()
    {
        formColorR = 1F;
        formColorG = 1F;
        formColorB = 1F;
        formColorA = 1F;
        colorTintMasked = false;
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

    /**
     * Structure Color Grade channel masks: scale 1 = 100% of the structure AABB
     * (same convention as paint / Blend Color on structures).
     */
    public static void setGradeEffectTransformsForStructure(Color color, float sizeX, float sizeY, float sizeZ)
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
        applyGradeEffectTransformsStructure(color.brightnessTransform, color.contrastTransform, color.hueTransform, color.saturationTransform, sizeX, sizeY, sizeZ);
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
        if (color != null && color.hasColorAdjustments())
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

    private static void applyFormColorGrade(float brightness, float contrast, float hue, float saturation)
    {
        formColorGradeBrightness = brightness;
        formColorGradeContrast = contrast;
        formColorGradeHue = hue;
        formColorGradeSaturation = saturation;
        mchorse.bbs_mod.utils.iris.FormColorGradePatch.set(brightness, contrast, hue, saturation);
    }

    private static void applyGradeEffectTransforms(EffectTransform brightness, EffectTransform contrast, EffectTransform hue, EffectTransform saturation)
    {
        gradeBrightnessMask.setModel(brightness);
        gradeContrastMask.setModel(contrast);
        gradeHueMask.setModel(hue);
        gradeSaturationMask.setModel(saturation);
    }

    private static void applyGradeEffectTransformsStructure(EffectTransform brightness, EffectTransform contrast, EffectTransform hue, EffectTransform saturation, float sizeX, float sizeY, float sizeZ)
    {
        gradeBrightnessMask.setStructure(brightness, sizeX, sizeY, sizeZ);
        gradeContrastMask.setStructure(contrast, sizeX, sizeY, sizeZ);
        gradeHueMask.setStructure(hue, sizeX, sizeY, sizeZ);
        gradeSaturationMask.setStructure(saturation, sizeX, sizeY, sizeZ);
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
        mchorse.bbs_mod.utils.iris.FormColorGradePatch.clear();
    }

    public static void clearColorEffectTransform()
    {
        if (!paintEffectActive)
        {
            formRootInverse.identity();
        }

        colorEffectInverse.identity();
        colorEffectActive = false;
        colorMaskBottomAnchored = true;
        colorMaskShape = 0F;
        colorMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
    }

    private static Matrix4f overlayFormRootInverse()
    {
        if (usesCapturedModelView())
        {
            return overlayFormRootInverse.identity();
        }

        return formRootInverse;
    }

    public static void render(class_5944 shader, IModelVAO modelVAO, class_4587 stack, float r, float g, float b, float a, int light, int overlay)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        if (ModelVAORenderer.textureBlendActive && ModelVAORenderer.textureBlendTo != null)
        {
            BBSModClient.getTextures().bindTexture(ModelVAORenderer.textureBlendTo, 3);
        }

        setupUniforms(stack, shader);

        RenderSystem.setShader(() -> shader);
        shader.method_34586();
        mchorse.bbs_mod.utils.iris.ShaderOpacityPatch.reassertPostDeferredDepthState();
        mchorse.bbs_mod.utils.iris.FormColorGradePatch.uploadToCurrentProgram();
        modelVAO.render(shader.method_35786(), r, g, b, a, light, overlay);
        shader.method_34585();

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }

    public static void setupUniforms(class_4587 stack, class_5944 shader)
    {

        if (colorGradeOverlayPass && gradeSceneColor != null && gradeSceneColor.isValid())
        {
            RenderSystem.setShaderTexture(3, gradeSceneColor.id);
        }


        setupUniforms(stack, shader, false);
    }

    /**
     * CPU shape-key path writes positions/normals already transformed by the render stack.
     * ModelViewMat must not multiply that stack again (or meshes vanish at the origin when
     * {@code drawWithGlobalProgram} keeps only the camera matrix), and NormalMat must stay
     * identity or diffuse lighting is applied twice.
     */
    public static void setupUniformsCpuPretransformed(class_5944 shader)
    {
        setupUniforms(null, shader, true);
    }

    private static void setupUniforms(class_4587 stack, class_5944 shader, boolean cpuPretransformed)
    {

        for (int i = 0; i < 12; i++)
        {
            shader.method_34583("Sampler" + i, RenderSystem.getShaderTexture(i));
        }

        if (shader.field_29471 != null)
        {
            shader.field_29471.method_1250(RenderSystem.getProjectionMatrix());
        }

        if (shader.field_29470 != null)
        {
            if (cpuPretransformed)
            {
                if (usesCapturedModelView())
                {
                    /* Captured draws already baked the full transform into the vertex buffer. */
                    shader.field_29470.method_1250(IDENTITY_MODEL_VIEW);
                }
                else
                {
                    shader.field_29470.method_1250(new Matrix4f(RenderSystem.getModelViewMatrix()));
                }
            }
            else
            {
                ModelVAORenderer.setModelViewUniform(stack, shader);
            }
        }

        /* NormalMat is present by default in Iris' shaders, but when there is no Iris,
         * the BBS mod's model.json shader is being used instead that provides NormalMat
         * uniform.
         */
        class_284 normalUniform = shader.method_34582("NormalMat");

        if (normalUniform != null)
        {
            if (cpuPretransformed)
            {
                normalUniform.method_39978(IDENTITY_NORMAL);
            }
            else
            {
                normalUniform.method_39978(stack.method_23760().method_23762());
            }
        }

        class_284 paintUniform = shader.method_34582("PaintColor");

        if (paintUniform != null)
        {
            paintUniform.method_35657(paintR, paintG, paintB, paintStrength);
        }

        class_284 glowingUniform = shader.method_34582("GlowingColor");

        glowingUniformActive = glowingUniform != null;

        if (glowingUniform != null)
        {
            glowingUniform.method_35657(glowR, glowG, glowB, glowStrength);
        }

        class_284 glowPaintOnlyUniform = shader.method_34582("GlowPaintOnly");

        if (glowPaintOnlyUniform != null)
        {
            glowPaintOnlyUniform.method_1251(glowPaintOnly ? 1F : 0F);
        }

        class_284 paintOverlayUniform = shader.method_34582("PaintOverlay");

        if (paintOverlayUniform != null)
        {
            paintOverlayUniform.method_1251(paintOverlayPass ? 1F : 0F);
        }

        class_284 textureBlendFactorUniform = shader.method_34582("TextureBlendFactor");

        if (textureBlendFactorUniform != null)
        {
            textureBlendFactorUniform.method_1251(ModelVAORenderer.textureBlendActive ? ModelVAORenderer.textureBlendFactor : 0F);
        }

        class_284 textureBlendActiveUniform = shader.method_34582("TextureBlendActive");

        if (textureBlendActiveUniform != null)
        {
            textureBlendActiveUniform.method_1251(ModelVAORenderer.textureBlendActive ? 1F : 0F);
        }

        class_284 formRootInverseUniform = shader.method_34582("FormRootInverse");

        if (formRootInverseUniform != null)
        {
            formRootInverseUniform.method_1250(overlayFormRootInverse());
        }

        class_284 paintEffectInverseUniform = shader.method_34582("PaintEffectInverse");

        if (paintEffectInverseUniform != null)
        {
            paintEffectInverseUniform.method_1250(paintEffectInverse);
        }

        class_284 paintEffectActiveUniform = shader.method_34582("PaintEffectActive");

        if (paintEffectActiveUniform != null)
        {
            paintEffectActiveUniform.method_1251(paintEffectActive ? 1F : 0F);
        }

        class_284 paintMaskHalfUniform = shader.method_34582("PaintMaskHalf");

        if (paintMaskHalfUniform != null)
        {
            paintMaskHalfUniform.method_1249(paintMaskHalf.x, paintMaskHalf.y, paintMaskHalf.z);
        }

        class_284 paintMaskBottomAnchoredUniform = shader.method_34582("PaintMaskBottomAnchored");

        if (paintMaskBottomAnchoredUniform != null)
        {
            paintMaskBottomAnchoredUniform.method_1251(paintMaskBottomAnchored ? 1F : 0F);
        }

        class_284 paintMaskShapeUniform = shader.method_34582("PaintMaskShape");

        if (paintMaskShapeUniform != null)
        {
            paintMaskShapeUniform.method_1251(paintMaskShape);
        }

        class_284 colorEffectInverseUniform = shader.method_34582("ColorEffectInverse");

        if (colorEffectInverseUniform != null)
        {
            colorEffectInverseUniform.method_1250(colorEffectInverse);
        }

        class_284 colorEffectActiveUniform = shader.method_34582("ColorEffectActive");

        if (colorEffectActiveUniform != null)
        {
            colorEffectActiveUniform.method_1251(colorEffectActive ? 1F : 0F);
        }

        class_284 colorMaskHalfUniform = shader.method_34582("ColorMaskHalf");

        if (colorMaskHalfUniform != null)
        {
            colorMaskHalfUniform.method_1249(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        class_284 colorMaskBottomAnchoredUniform = shader.method_34582("ColorMaskBottomAnchored");

        if (colorMaskBottomAnchoredUniform != null)
        {
            colorMaskBottomAnchoredUniform.method_1251(colorMaskBottomAnchored ? 1F : 0F);
        }

        class_284 colorMaskShapeUniform = shader.method_34582("ColorMaskShape");

        if (colorMaskShapeUniform != null)
        {
            colorMaskShapeUniform.method_1251(colorMaskShape);
        }

        class_284 formColorTintUniform = shader.method_34582("FormColorTint");

        if (formColorTintUniform != null)
        {
            formColorTintUniform.method_35657(formColorR, formColorG, formColorB, formColorA);
        }

        class_284 formColorGradeUniform = shader.method_34582("FormColorGrade");

        if (formColorGradeUniform != null)
        {
            formColorGradeUniform.method_35657(formColorGradeBrightness, formColorGradeContrast, formColorGradeHue, formColorGradeSaturation);
        }

        gradeBrightnessMask.upload(shader, "GradeBrightness");
        gradeContrastMask.upload(shader, "GradeContrast");
        gradeHueMask.upload(shader, "GradeHue");
        gradeSaturationMask.upload(shader, "GradeSaturation");

        class_284 colorTintMaskedUniform = shader.method_34582("ColorTintMasked");

        if (colorTintMaskedUniform != null)
        {
            colorTintMaskedUniform.method_1251(colorTintMasked ? 1F : 0F);
        }

        class_284 colorTintOverlayUniform = shader.method_34582("ColorTintOverlay");

        if (colorTintOverlayUniform != null)
        {
            colorTintOverlayUniform.method_1251(colorTintOverlayPass ? 1F : 0F);
        }

        class_284 colorGradeOverlayUniform = shader.method_34582("ColorGradeOverlay");

        if (colorGradeOverlayUniform != null)
        {
            colorGradeOverlayUniform.method_1251(colorGradeOverlayPass ? 1F : 0F);
        }

        /* After Iris composite, RenderSystem fog is often collapsed (FogEnd≈1) or left as
         * dense atmospheric fog — linear_fog then replaces the whole mesh with FogColor
         * (featureless sky-tinted silhouette, texture gone). Captured-matrix redraws already
         * sit on the final image; skip fog so low-opacity / render-depth fades keep albedo. */
        if (usesCapturedModelView())
        {
            if (shader.field_29477 != null)
            {
                shader.field_29477.method_1251(1_000_000F);
            }

            if (shader.field_29478 != null)
            {
                shader.field_29478.method_1251(1_000_001F);
            }

            if (shader.field_29479 != null)
            {
                shader.field_29479.method_35657(0F, 0F, 0F, 0F);
            }

            if (shader.field_36373 != null)
            {
                shader.field_36373.method_35649(0);
            }
        }
        else
        {
            if (shader.field_29477 != null)
            {
                shader.field_29477.method_1251(RenderSystem.getShaderFogStart());
            }

            if (shader.field_29478 != null)
            {
                shader.field_29478.method_1251(RenderSystem.getShaderFogEnd());
            }

            if (shader.field_29479 != null)
            {
                shader.field_29479.method_1253(RenderSystem.getShaderFogColor());
            }

            if (shader.field_36373 != null)
            {
                shader.field_36373.method_35649(RenderSystem.getShaderFogShape().method_40036());
            }
        }

        if (shader.field_29474 != null)
        {
            shader.field_29474.method_35657(1F, 1F, 1F, 1F);
        }

        if (shader.field_29481 != null)
        {
            shader.field_29481.method_1251(RenderSystem.getShaderGameTime());
        }

        if (shader.field_29472 != null)
        {
            shader.field_29472.method_1250(RenderSystem.getTextureMatrix());
        }

        RenderSystem.setupShaderLights(shader);
    }

    private static void setModelViewUniform(class_4587 stack, class_5944 shader)
    {
        Matrix4f modelView;

        if (usesCapturedModelView())
        {
            /* Overlay/deferred stack already carries the full terrain + entity transform captured
             * at enqueue; RenderSystem model-view is identity during these draws. */
            modelView = new Matrix4f(stack.method_23760().method_23761());
        }
        else
        {
            modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.method_23760().method_23761());
        }

        shader.field_29470.method_1250(modelView);
    }
}