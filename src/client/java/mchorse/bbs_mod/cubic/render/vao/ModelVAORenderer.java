package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexFormat;

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
            GlUniform inverseUniform = shader.getUniform(prefix + "Inverse");

            if (inverseUniform != null)
            {
                inverseUniform.set(this.inverse);
            }

            GlUniform activeUniform = shader.getUniform(prefix + "Active");

            if (activeUniform != null)
            {
                activeUniform.set(this.active ? 1F : 0F);
            }

            GlUniform halfUniform = shader.getUniform(prefix + "Half");

            if (halfUniform != null)
            {
                halfUniform.set(this.half.x, this.half.y, this.half.z);
            }

            GlUniform bottomUniform = shader.getUniform(prefix + "BottomAnchored");

            if (bottomUniform != null)
            {
                bottomUniform.set(this.bottomAnchored ? 1F : 0F);
            }

            GlUniform shapeUniform = shader.getUniform(prefix + "Shape");

            if (shapeUniform != null)
            {
                shapeUniform.set(this.shape);
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

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShader(BBSShaders::getModel);

        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f savedModelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        try
        {
            paintOverlaySynced = entry.synced;

            RenderSystem.setProjectionMatrix(entry.projection, VertexSorter.BY_Z);

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
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorter.BY_Z);

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

            modelViewStack.pushMatrix();
            modelViewStack.set(savedModelView);
            RenderSystem.applyModelViewMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
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
        snapshotPaintEffectBase();
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
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        if (ModelVAORenderer.textureBlendActive && ModelVAORenderer.textureBlendTo != null)
        {
            BBSModClient.getTextures().bindTexture(ModelVAORenderer.textureBlendTo, 3);
        }

        setupUniforms(stack, shader);

        RenderSystem.setShader(() -> shader);
        shader.bind();
        ShaderOpacityPatch.reassertPostDeferredDepthState();
        FormColorGradePatch.uploadToCurrentProgram();
        modelVAO.render(shader.getFormat(), r, g, b, a, light, overlay);
        shader.unbind();

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }

    public static void setupUniforms(MatrixStack stack, ShaderProgram shader)
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
    public static void setupUniformsCpuPretransformed(ShaderProgram shader)
    {
        setupUniforms(null, shader, true);
    }

    private static void setupUniforms(MatrixStack stack, ShaderProgram shader, boolean cpuPretransformed)
    {

        for (int i = 0; i < 12; i++)
        {
            shader.addSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
        }

        if (shader.projectionMat != null)
        {
            shader.projectionMat.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.modelViewMat != null)
        {
            if (cpuPretransformed)
            {
                if (usesCapturedModelView())
                {
                    /* Captured draws already baked the full transform into the vertex buffer. */
                    shader.modelViewMat.set(IDENTITY_MODEL_VIEW);
                }
                else
                {
                    shader.modelViewMat.set(new Matrix4f(RenderSystem.getModelViewMatrix()));
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
        GlUniform normalUniform = shader.getUniform("NormalMat");

        if (normalUniform != null)
        {
            if (cpuPretransformed)
            {
                normalUniform.set(IDENTITY_NORMAL);
            }
            else
            {
                normalUniform.set(stack.peek().getNormalMatrix());
            }
        }

        GlUniform paintUniform = shader.getUniform("PaintColor");

        if (paintUniform != null)
        {
            paintUniform.set(paintR, paintG, paintB, paintStrength);
        }

        GlUniform glowingUniform = shader.getUniform("GlowingColor");

        glowingUniformActive = glowingUniform != null;

        if (glowingUniform != null)
        {
            glowingUniform.set(glowR, glowG, glowB, glowStrength);
        }

        GlUniform glowPaintOnlyUniform = shader.getUniform("GlowPaintOnly");

        if (glowPaintOnlyUniform != null)
        {
            glowPaintOnlyUniform.set(glowPaintOnly ? 1F : 0F);
        }

        GlUniform paintOverlayUniform = shader.getUniform("PaintOverlay");

        if (paintOverlayUniform != null)
        {
            paintOverlayUniform.set(paintOverlayPass ? 1F : 0F);
        }

        GlUniform textureBlendFactorUniform = shader.getUniform("TextureBlendFactor");

        if (textureBlendFactorUniform != null)
        {
            textureBlendFactorUniform.set(ModelVAORenderer.textureBlendActive ? ModelVAORenderer.textureBlendFactor : 0F);
        }

        GlUniform textureBlendActiveUniform = shader.getUniform("TextureBlendActive");

        if (textureBlendActiveUniform != null)
        {
            textureBlendActiveUniform.set(ModelVAORenderer.textureBlendActive ? 1F : 0F);
        }

        GlUniform formRootInverseUniform = shader.getUniform("FormRootInverse");

        if (formRootInverseUniform != null)
        {
            formRootInverseUniform.set(overlayFormRootInverse());
        }

        GlUniform paintEffectInverseUniform = shader.getUniform("PaintEffectInverse");

        if (paintEffectInverseUniform != null)
        {
            paintEffectInverseUniform.set(paintEffectInverse);
        }

        GlUniform paintEffectActiveUniform = shader.getUniform("PaintEffectActive");

        if (paintEffectActiveUniform != null)
        {
            paintEffectActiveUniform.set(paintEffectActive ? 1F : 0F);
        }

        GlUniform paintMaskHalfUniform = shader.getUniform("PaintMaskHalf");

        if (paintMaskHalfUniform != null)
        {
            paintMaskHalfUniform.set(paintMaskHalf.x, paintMaskHalf.y, paintMaskHalf.z);
        }

        GlUniform paintMaskBottomAnchoredUniform = shader.getUniform("PaintMaskBottomAnchored");

        if (paintMaskBottomAnchoredUniform != null)
        {
            paintMaskBottomAnchoredUniform.set(paintMaskBottomAnchored ? 1F : 0F);
        }

        GlUniform paintMaskShapeUniform = shader.getUniform("PaintMaskShape");

        if (paintMaskShapeUniform != null)
        {
            paintMaskShapeUniform.set(paintMaskShape);
        }

        GlUniform glowEffectInverseUniform = shader.getUniform("GlowEffectInverse");

        if (glowEffectInverseUniform != null)
        {
            glowEffectInverseUniform.set(glowEffectInverse);
        }

        GlUniform glowEffectActiveUniform = shader.getUniform("GlowEffectActive");

        if (glowEffectActiveUniform != null)
        {
            glowEffectActiveUniform.set(glowEffectActive ? 1F : 0F);
        }

        GlUniform glowMaskHalfUniform = shader.getUniform("GlowMaskHalf");

        if (glowMaskHalfUniform != null)
        {
            glowMaskHalfUniform.set(glowMaskHalf.x, glowMaskHalf.y, glowMaskHalf.z);
        }

        GlUniform glowMaskBottomAnchoredUniform = shader.getUniform("GlowMaskBottomAnchored");

        if (glowMaskBottomAnchoredUniform != null)
        {
            glowMaskBottomAnchoredUniform.set(glowMaskBottomAnchored ? 1F : 0F);
        }

        GlUniform glowMaskShapeUniform = shader.getUniform("GlowMaskShape");

        if (glowMaskShapeUniform != null)
        {
            glowMaskShapeUniform.set(glowMaskShape);
        }

        GlUniform colorEffectInverseUniform = shader.getUniform("ColorEffectInverse");

        if (colorEffectInverseUniform != null)
        {
            colorEffectInverseUniform.set(colorEffectInverse);
        }

        GlUniform colorEffectActiveUniform = shader.getUniform("ColorEffectActive");

        if (colorEffectActiveUniform != null)
        {
            colorEffectActiveUniform.set(colorEffectActive ? 1F : 0F);
        }

        GlUniform colorMaskHalfUniform = shader.getUniform("ColorMaskHalf");

        if (colorMaskHalfUniform != null)
        {
            colorMaskHalfUniform.set(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        GlUniform colorMaskBottomAnchoredUniform = shader.getUniform("ColorMaskBottomAnchored");

        if (colorMaskBottomAnchoredUniform != null)
        {
            colorMaskBottomAnchoredUniform.set(colorMaskBottomAnchored ? 1F : 0F);
        }

        GlUniform colorMaskShapeUniform = shader.getUniform("ColorMaskShape");

        if (colorMaskShapeUniform != null)
        {
            colorMaskShapeUniform.set(colorMaskShape);
        }

        GlUniform formColorTintUniform = shader.getUniform("FormColorTint");

        if (formColorTintUniform != null)
        {
            formColorTintUniform.set(formColorR, formColorG, formColorB, formColorA);
        }

        GlUniform formColorGradeUniform = shader.getUniform("FormColorGrade");

        if (formColorGradeUniform != null)
        {
            formColorGradeUniform.set(formColorGradeBrightness, formColorGradeContrast, formColorGradeHue, formColorGradeSaturation);
        }

        gradeBrightnessMask.upload(shader, "GradeBrightness");
        gradeContrastMask.upload(shader, "GradeContrast");
        gradeHueMask.upload(shader, "GradeHue");
        gradeSaturationMask.upload(shader, "GradeSaturation");

        GlUniform colorTintMaskedUniform = shader.getUniform("ColorTintMasked");

        if (colorTintMaskedUniform != null)
        {
            colorTintMaskedUniform.set(colorTintMasked ? 1F : 0F);
        }

        GlUniform colorTintOverlayUniform = shader.getUniform("ColorTintOverlay");

        if (colorTintOverlayUniform != null)
        {
            colorTintOverlayUniform.set(colorTintOverlayPass ? 1F : 0F);
        }

        GlUniform colorGradeOverlayUniform = shader.getUniform("ColorGradeOverlay");

        if (colorGradeOverlayUniform != null)
        {
            colorGradeOverlayUniform.set(colorGradeOverlayPass ? 1F : 0F);
        }

        /* After Iris composite, RenderSystem fog is often collapsed (FogEnd≈1) or left as
         * dense atmospheric fog — linear_fog then replaces the whole mesh with FogColor
         * (featureless sky-tinted silhouette, texture gone). Captured-matrix redraws already
         * sit on the final image; skip fog so low-opacity / render-depth fades keep albedo. */
        if (usesCapturedModelView())
        {
            if (shader.fogStart != null)
            {
                shader.fogStart.set(1_000_000F);
            }

            if (shader.fogEnd != null)
            {
                shader.fogEnd.set(1_000_001F);
            }

            if (shader.fogColor != null)
            {
                shader.fogColor.set(0F, 0F, 0F, 0F);
            }

            if (shader.fogShape != null)
            {
                shader.fogShape.set(0);
            }
        }
        else
        {
            if (shader.fogStart != null)
            {
                shader.fogStart.set(RenderSystem.getShaderFogStart());
            }

            if (shader.fogEnd != null)
            {
                shader.fogEnd.set(RenderSystem.getShaderFogEnd());
            }

            if (shader.fogColor != null)
            {
                shader.fogColor.set(RenderSystem.getShaderFogColor());
            }

            if (shader.fogShape != null)
            {
                shader.fogShape.set(RenderSystem.getShaderFogShape().getId());
            }
        }

        if (shader.colorModulator != null)
        {
            shader.colorModulator.set(1F, 1F, 1F, 1F);
        }

        if (shader.gameTime != null)
        {
            shader.gameTime.set(RenderSystem.getShaderGameTime());
        }

        if (shader.textureMat != null)
        {
            shader.textureMat.set(RenderSystem.getTextureMatrix());
        }

        RenderSystem.setupShaderLights(shader);
    }

    private static void setModelViewUniform(MatrixStack stack, ShaderProgram shader)
    {
        Matrix4f modelView;

        if (usesCapturedModelView())
        {
            /* Overlay/deferred stack already carries the full terrain + entity transform captured
             * at enqueue; RenderSystem model-view is identity during these draws. */
            modelView = new Matrix4f(stack.peek().getPositionMatrix());
        }
        else
        {
            modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());
        }

        shader.modelViewMat.set(modelView);
    }
}
