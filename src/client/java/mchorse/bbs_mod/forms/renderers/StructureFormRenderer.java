package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.renderer.LightTexture;
import mchorse.bbs_mod.client.renderer.MultiBufferSource;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.StructureVAOCollector;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.StructureData;
import mchorse.bbs_mod.forms.renderers.utils.StructureData.BlockEntry;
import mchorse.bbs_mod.forms.renderers.utils.StructureFormOverlayRenderer;
import mchorse.bbs_mod.forms.renderers.utils.StructureFormOverlayRenderer.StructurePaintLayer;
import mchorse.bbs_mod.forms.renderers.utils.StructureVaoManager;
import mchorse.bbs_mod.forms.renderers.utils.StructureVirtualBlockRenderView;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * StructureForm Renderer
 *
 * Coordinates structure preview and 3D rendering using modular StructureData,
 * StructureVaoManager, and StructureFormOverlayRenderer delegates.
 */
public class StructureFormRenderer extends FormRenderer<StructureForm>
{
    private final StructureData data = new StructureData();
    private final StructureVaoManager vaoManager = new StructureVaoManager();
    private final StructureFormOverlayRenderer overlayRenderer = new StructureFormOverlayRenderer();

    private boolean lastEmitLight = false;
    private int lastLightIntensity = 0;

    public static void clearAllCachedVaos()
    {
        StructureVaoManager.clearAllCachedVaos();
    }

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        StructureVaoManager.ensureLightingRevision();
        this.ensureLoaded();

        PoseStack matrices = new PoseStack();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.pushPose();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        BBSRendering.depthFunc(GL11.GL_LEQUAL);

        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float baseScale = cellH / 2.5F;
        float targetPixels = Math.min(cellW, cellH) * 0.9F;

        int wUnits = 1;
        int hUnits = 1;
        int dUnits = 1;

        if (this.data.getBoundsMin() != null && this.data.getBoundsMax() != null)
        {
            wUnits = Math.max(1, this.data.getBoundsMax().getX() - this.data.getBoundsMin().getX() + 1);
            hUnits = Math.max(1, this.data.getBoundsMax().getY() - this.data.getBoundsMin().getY() + 1);
            dUnits = Math.max(1, this.data.getBoundsMax().getZ() - this.data.getBoundsMin().getZ() + 1);
        }
        else
        {
            wUnits = Math.max(1, this.data.getSize().getX());
            hUnits = Math.max(1, this.data.getSize().getY());
            dUnits = Math.max(1, this.data.getSize().getZ());
        }

        int maxUnits = Math.max(wUnits, Math.max(hUnits, dUnits));
        float auto = maxUnits > 0 ? targetPixels / (baseScale * maxUnits) : 1F;
        float finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        float structScaleUI = Math.max(Math.max(this.form.scaleX.get(), this.form.scaleY.get()), this.form.scaleZ.get());

        finalScale *= structScaleUI;
        matrices.scale(finalScale, finalScale, finalScale);
        MatrixStackUtils.invertUiNormalY(matrices);

        BBSRendering.setupLevelLighting();

        this.checkLightState();

        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
        Color tint = Color.white();

        if (FormColorEffects.shouldBakeFormColor(storedFormColor))
        {
            tint.mul(rawFormColor);
        }

        this.form.applyFormOpacity(tint);
        this.form.applyFormOpacity(formColor);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F && !FormColorEffects.wantsNegativeGlowOverlay(glowSettings, legacyGlow))
        {
            FormColorEffects.blendFormGlowBrighten(tint, glowSettings, legacyGlow);
        }

        boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
        boolean deferColorTintToOverlay = colorTransformWanted && irisWorldPaintDeferral;
        Color resolvedPaint = FormColorEffects.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean runPaintOverlay = FormColorEffects.wantsPaintOverlay(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positiveGlow = glowIntensity > 0F;
        boolean negativeGlowMasked = FormColorEffects.wantsNegativeGlowOverlay(glowSettings, legacyGlow);
        Color mainPassPaint = FormColorEffects.defersNegativePaintToOverlay(this.form.paintSettings.get(), this.form.paintColor.get())
            ? null
            : resolvedPaint;
        Function<VertexConsumer, VertexConsumer> mainRecolor = this.getMainConsumer(tint, mainPassPaint);

        IModelVAO vao = this.getVao();

        if (!this.data.getBlocks().isEmpty())
        {
            FormRenderingContext passContext = new FormRenderingContext()
                .set(FormRenderType.PREVIEW, null, matrices, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, 0F);

            this.renderLayerGroup(this.data.getStaticBlocks(), passContext, matrices,
                LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, mainRecolor, null, false);

            if (this.data.hasBlockEntityLayer())
            {
                this.renderBlockEntitiesPass(passContext, matrices, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, true);
            }

            if (this.data.hasBiomeTintedLayer())
            {
                this.renderLayerGroup(this.data.getBiomeTintedBlocks(), passContext, matrices, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, mainRecolor, null, true);
            }

            if (this.data.hasAnimatedLayer())
            {
                this.renderLayerGroup(this.data.getAnimatedBlocks(), passContext, matrices, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, mainRecolor, null, false);
            }

            if (this.data.hasTranslucentLayer())
            {
                this.renderLayerGroup(this.data.getTranslucentBlocks(), passContext, matrices, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, mainRecolor, null, false);
            }

                BBSRendering.disableBlend();

                if (runPaintOverlay)
                {
                    EffectTransform paintTransform = this.form.paintSettings.get().transform;
                    this.overlayRenderer.renderStructurePaintOverlay(this.data, vao, passContext, matrices, resolvedPaint, tint.a, OverlayTexture.NO_OVERLAY, true, BBSRendering.isIrisShadersEnabled(), paintTransform, glowSettings, legacyGlow, glowIntensity, layer -> this.renderPaintLayer(layer, passContext, matrices, OverlayTexture.NO_OVERLAY, null), (s) -> this.renderStructureCulledWorld(passContext, s, FormUtilsClient.getProvider(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, BBSRendering.isIrisShadersEnabled(), null, true, false));
                }

                if (colorTransformWanted)
                {
                    this.overlayRenderer.renderStructureColorTintOverlay(this.data, this.form, passContext, matrices, formColor, tint.a, OverlayTexture.NO_OVERLAY, true, BBSRendering.isIrisShadersEnabled(), deferColorTintToOverlay, layer -> this.renderPaintLayer(layer, passContext, matrices, OverlayTexture.NO_OVERLAY, null), (s) -> this.renderStructureCulledWorld(passContext, s, FormUtilsClient.getProvider(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, BBSRendering.isIrisShadersEnabled(), null, true, false));
                }

                if (positiveGlow || negativeGlowMasked)
                {
                    this.overlayRenderer.renderStructureGlowOverlay(this.data, passContext, matrices, glowSettings, legacyGlow, glowIntensity, tint.a, OverlayTexture.NO_OVERLAY, false, BBSRendering.isIrisShadersEnabled(), null, (s) -> this.renderStructureCulledWorld(passContext, s, FormUtilsClient.getProvider(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, BBSRendering.isIrisShadersEnabled(), null, true, false));
                }
        }

        matrices.popPose();
        BBSRendering.depthFunc(GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        StructureVaoManager.ensureLightingRevision();
        this.ensureLoaded();

        context.stack.pushPose();

        try
        {
            context.stack.scale(this.form.scaleX.get(), this.form.scaleY.get(), this.form.scaleZ.get());

            boolean picking = context.isPicking();
            this.checkLightState();

            IModelVAO vao = this.getVao();

            Color storedFormColor3D = this.form.color.get();
            Color rawFormColor3D = storedFormColor3D.copyBakingColorGrade();
            Color formColor3D = rawFormColor3D.copy();
            boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor3D);
            Color mainTint3D = new Color().set(context.color);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            if (shadowPass)
            {
                mainTint3D.a *= storedFormColor3D.a;
            }
            else if (FormColorEffects.shouldBakeFormColor(storedFormColor3D))
            {
                mainTint3D.mul(rawFormColor3D);
            }

            this.form.applyFormOpacity(mainTint3D);
            this.form.applyFormOpacity(formColor3D);

            FormColorEffects.applyShadowPassColorFix(mainTint3D, storedFormColor3D, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);
            this.applyBlockEntityOnlyShaderShadow(mainTint3D, shadowPass);

            if (mainTint3D.a <= 0.001F && !shadowPass && !picking)
            {
                return;
            }

            GlowSettings glowSettings = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
            EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
            boolean hasGlowTransform = glowTransform != null && glowTransform.isActive();
            boolean positiveGlow = !picking && !shadowPass && glowIntensity > 0F;
            boolean hasEmissiveGlow = positiveGlow && !glowSettings.resolvePaintOnly();

            boolean localPreview = context.isLocalPreview();
            boolean noshadingDefer = !localPreview
                && !shadowPass
                && BBSRendering.needsIrisNoshadingOpacityDeferral(mainTint3D.a, this.form.noshadingOpacity.get());
            boolean softPostDeferred = !localPreview
                && !shadowPass
                && ShaderOpacityPatch.shouldDelayUntilPostDeferred(mainTint3D.a)
                && !noshadingDefer;

            boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
            boolean deferColorTintToOverlay = colorTransformWanted && irisWorldPaintDeferral && !shadowPass;
            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
            boolean runPaintOverlay = !picking && !shadowPass && FormColorEffects.wantsPaintOverlay(paintSettings, legacyPaint);
            boolean applyColorTint = colorTransformWanted && !picking && !shadowPass;
            Color mainPassPaint = FormColorEffects.defersNegativePaintToOverlay(paintSettings, legacyPaint)
                ? null
                : resolvedPaint;
            Function<VertexConsumer, VertexConsumer> mainRecolor = this.getMainConsumer(mainTint3D, mainPassPaint);
            Color vaoTint = mainTint3D.copy();
            Function<VertexConsumer, VertexConsumer> layerRecolor = mainRecolor;
            /* Same contract as BlockForm/ItemForm: bake emission during Iris world (incl. soft
             * post-deferred flush). Post-composite additive glow overlays never bloom.
             * Structure still draws the additive glow overlay whenever glow is positive — Iris
             * VAO solids ignore ColorModulator, so baking alone is not enough for visible glow. */
            boolean glowBakedInMainPass = irisWorldPaintDeferral && hasEmissiveGlow && !hasGlowTransform && !noshadingDefer;
            boolean negativeGlowMasked = !picking && !shadowPass && FormColorEffects.wantsNegativeGlowOverlay(glowSettings, legacyGlow);

            if (glowIntensity < 0F && !negativeGlowMasked)
            {
                FormColorEffects.blendFormGlowBrighten(mainTint3D, glowSettings, legacyGlow);
                FormColorEffects.blendFormGlowBrighten(vaoTint, glowSettings, legacyGlow);
            }
            else if (glowBakedInMainPass)
            {
                /* Must hit the Iris entity/gbuffer pass - post-composite BBS additive never blooms.
                 * Base emission on a neutral white base so form color tint does not distort bloom. */
                vaoTint = new Color(1F, 1F, 1F, mainTint3D.a);
                FormColorEffects.blendFormGlowBrighten(vaoTint, glowSettings, legacyGlow);
                layerRecolor = this.getMainConsumer(new Color(1F, 1F, 1F, mainTint3D.a), mainPassPaint);
            }

            boolean shaders = BBSRendering.isIrisShadersEnabled();
            /* Soft Structure bloom: ColorModulator must be set at RenderLayer.draw time via
             * hijackVertexFormat (same as BlockForm). Bare setShaderColor before Immediate.draw
             * is too early; Iris rebinds ColorModulator. Do not use ColorModulator on raw VAO. */
            final Color softGlowShaderTint = (glowBakedInMainPass && shaders && BBSRendering.isRenderingWorld())
                ? vaoTint.copy()
                : null;
            /* Iris world solids draw with entity_translucent (no PaintColor uniforms). Uniform
             * negative paint must bake into VAO tint — same idea as negative glow on vaoTint.
             * BBS model.fsh (no shaders / UI) still uses prepareVaoPaintForMainPass. */
            boolean irisEntityVao = shaders && BBSRendering.isRenderingWorld();
            boolean bakeNegativePaintIntoVaoTint = irisEntityVao
                && mainPassPaint != null
                && mainPassPaint.a < 0F;

            if (bakeNegativePaintIntoVaoTint)
            {
                FormColorEffects.applyPaintBlend(vaoTint, paintSettings, legacyPaint);
            }

            if (vao != null || !picking)
            {
                int light = context.isPicking() ? 0 : context.light;

                if (context.isPicking())
                {
                    IModelVAO pickingVao = this.getPickingVao();

                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    BBSRendering.bindProgram(BBSShaders.getPickerModelsProgram());
                    BBSRendering.enableBlend();
                    BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);

                    ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, mainTint3D.r, mainTint3D.g, mainTint3D.b, mainTint3D.a, light, context.overlay);
                }
                else
                {
                    if (softPostDeferred || noshadingDefer)
                {
                    boolean irisCamera = BBSRendering.isIrisWorldModelPass() && !noshadingDefer;
                    Matrix4f positionMatrix = irisCamera
                        ? new Matrix4f(context.stack.last().pose())
                        : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.last().pose()));
                    Matrix3f normalMatrix = new Matrix3f(context.stack.last().normal());
                    Matrix4f sortRootMatrix = new Matrix4f(context.stack.last().pose());
                    Color mainTintSnapshot = mainTint3D.copy();
                    Color formColor3DSnapshot = formColor3D.copy();
                    Color resolvedPaintSnapshot = resolvedPaint == null ? null : resolvedPaint.copy();
                    PaintSettings paintSettingsSnapshot = paintSettings == null ? null : paintSettings.copy();
                    int lightSnapshot = light;
                    int overlaySnapshot = context.overlay;
                    boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(mainTint3D.a);
                    boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(mainTint3D.a);
                    boolean positiveGlowSnapshot = (positiveGlow && !glowSettings.resolvePaintOnly()) || negativeGlowMasked;
                    float glowIntensitySnapshot = glowIntensity;
                    GlowSettings glowSettingsSnapshot = glowSettings;
                    Color legacyGlowSnapshot = legacyGlow;
                    boolean positivePaintSnapshot = runPaintOverlay;
                    boolean applyColorTintSnapshot = applyColorTint;
                    boolean beTintSnapshot = !irisWorldPaintDeferral;
                    IModelVAO vaoSnapshot = vao;
                    boolean shadersSnapshot = shaders;
                    Color softGlowShaderTintSnapshot = softGlowShaderTint == null ? null : softGlowShaderTint.copy();
                    /* Neutral white vertices + ColorModulator emission (BlockForm soft bloom). */
                    Function<VertexConsumer, VertexConsumer> mainRecolorSnapshot = softGlowShaderTintSnapshot != null
                        ? this.getMainConsumer(new Color(1F, 1F, 1F, mainTintSnapshot.a), mainPassPaint == null ? null : mainPassPaint.copy())
                        : this.getMainConsumer(mainTintSnapshot, mainPassPaint == null ? null : mainPassPaint.copy());
                    RenderInfo sortInfo = this.calculateRenderInfo(context, false);
                    List<BlockEntry> softBlocks = new ArrayList<>(this.data.getBlocks());

                    if (noshadingDefer)
                    {
                        Runnable deferredDraw = () -> this.runStructureSoftDeferredPass(
                            context, positionMatrix, normalMatrix, mainRecolorSnapshot, softGlowShaderTintSnapshot,
                            lightSnapshot, overlaySnapshot, beTintSnapshot, depthWrite, vaoSnapshot, mainTintSnapshot,
                            positivePaintSnapshot, paintSettingsSnapshot, resolvedPaintSnapshot, applyColorTintSnapshot,
                            formColor3DSnapshot, positiveGlowSnapshot, glowSettingsSnapshot, legacyGlowSnapshot,
                            glowIntensitySnapshot, shadersSnapshot, true);

                        ModelVAORenderer.submitDeferredTranslucentModel(deferredDraw, depthWrite);
                    }
                    else
                    {
                        /* One queue entry per block (same contract as soft limbs / soft Block forms)
                         * so nearby soft BlockForms can interleave instead of painting over the
                         * whole tree after a single form-origin Structure entry. */
                        double nearestBlockKey = Double.POSITIVE_INFINITY;

                        for (BlockEntry entry : softBlocks)
                        {
                            double blockKey = this.computeStructureBlockFormSortKey(entry, sortInfo, sortRootMatrix, context);

                            /* Prefer drawing cutout/leaves after solids / soft BlockForms at the
                             * same depth (painter: nearer key draws later). */
                            if (this.isSoftStructureNonSolid(entry.state))
                            {
                                blockKey -= 1.0E-3D;
                            }

                            if (blockKey < nearestBlockKey)
                            {
                                nearestBlockKey = blockKey;
                            }

                            BlockEntry entrySnapshot = entry;
                            Runnable blockDraw = () -> this.runStructureSoftBlockDeferredColor(
                                context, positionMatrix, normalMatrix, entrySnapshot, sortInfo,
                                mainRecolorSnapshot, softGlowShaderTintSnapshot, lightSnapshot, overlaySnapshot);

                            if (irisCamera)
                            {
                                ShaderOpacityPatch.submitPostDeferredForm(0D, blockKey, false, afterFluids, blockDraw);
                            }
                            else
                            {
                                ShaderOpacityPatch.submitPostDeferredBbsForm(0D, blockKey, false, afterFluids, blockDraw);
                            }
                        }

                        if (softBlocks.isEmpty())
                        {
                            nearestBlockKey = this.computeStructureFormSortKey(sortRootMatrix, context);
                        }

                        /* After all Structure color entries (and after same-depth BlockForms that
                         * lost the non-solid bias). */
                        double tailKey = nearestBlockKey - 1.0E-3D;
                        Runnable tailDraw = () -> this.runStructureSoftDeferredTail(
                            context, positionMatrix, normalMatrix, lightSnapshot, overlaySnapshot,
                            beTintSnapshot, depthWrite, vaoSnapshot, mainTintSnapshot, positivePaintSnapshot,
                            paintSettingsSnapshot, resolvedPaintSnapshot, applyColorTintSnapshot, formColor3DSnapshot,
                            positiveGlowSnapshot, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot,
                            shadersSnapshot);

                        if (irisCamera)
                        {
                            ShaderOpacityPatch.submitPostDeferredForm(0D, tailKey, depthWrite, afterFluids, tailDraw);
                        }
                        else
                        {
                            ShaderOpacityPatch.submitPostDeferredBbsForm(0D, tailKey, depthWrite, afterFluids, tailDraw);
                        }
                    }
                }
                else
                {
                    if (shadowPass)
                    {
                        ShaderOpacityPatch.beginShadowForm();
                    }

                    try
                    {
                        /* Static block models use the same RenderLayer submission as the special
                         * groups. Raw VAOs do not bind the pipeline's atlas and uniform buffers. */
                        this.renderLayerGroup(this.data.getStaticBlocks(), context, context.stack,
                            light, context.overlay, layerRecolor, null, false);

                        Color layerShaderTint = (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld()) ? vaoTint : mainTint3D;

                        if (this.data.hasBlockEntityLayer())
                        {
                            boolean beTint = !irisWorldPaintDeferral;
                            this.renderBlockEntitiesPass(context, context.stack, light, context.overlay, beTint);
                        }

                        if (this.data.hasBiomeTintedLayer())
                        {
                            this.renderLayerGroup(this.data.getBiomeTintedBlocks(), context, context.stack, light, context.overlay, layerRecolor, layerShaderTint, true);
                        }

                        if (this.data.hasAnimatedLayer())
                        {
                            this.renderLayerGroup(this.data.getAnimatedBlocks(), context, context.stack, light, context.overlay, layerRecolor, layerShaderTint, false);
                        }

                        if (this.data.hasTranslucentLayer())
                        {
                            this.renderLayerGroup(this.data.getTranslucentBlocks(), context, context.stack, light, context.overlay, layerRecolor, layerShaderTint, false);
                        }
                    }
                    finally
                    {
                        if (shadowPass)
                        {
                            ShaderOpacityPatch.endShadowForm();
                        }
                    }
                }

                boolean submitIrisOverlays = irisWorldPaintDeferral && !noshadingDefer;

                /* Soft + Iris: keep color/paint/glow masks on the Iris paint-overlay queue
                 * (same contract as soft BlockForm). Drawing them only inside the soft flush
                 * skips beginColorTintOverlayPass and the masks vanish with shaders. */
                if (((!softPostDeferred && !noshadingDefer) || (softPostDeferred && submitIrisOverlays)) && applyColorTint)
                {
                    if (irisWorldPaintDeferral)
                    {
                        this.overlayRenderer.submitDeferredStructureColorTintOverlay(this.data, this.form, context, formColor3D, mainTint3D.a, context.overlay, true, shaders, layer -> this.renderPaintLayer(layer, context, context.stack, context.overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                    else if (!softPostDeferred)
                    {
                        this.overlayRenderer.renderStructureColorTintOverlay(this.data, this.form, context, context.stack, formColor3D, mainTint3D.a, context.overlay, true, shaders, false, layer -> this.renderPaintLayer(layer, context, context.stack, context.overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                }

                if ((!softPostDeferred && !noshadingDefer && runPaintOverlay) || (softPostDeferred && submitIrisOverlays && runPaintOverlay))
                {
                    EffectTransform paintTransform = paintSettings.transform;
                    this.overlayRenderer.submitDeferredStructurePaintOverlay(this.data, vao, context, resolvedPaint, mainTint3D.a, context.overlay, true, shaders, paintTransform, glowSettings, legacyGlow, glowIntensity, layer -> this.renderPaintLayer(layer, context, context.stack, context.overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                }

                if ((!softPostDeferred && !noshadingDefer && (positiveGlow || negativeGlowMasked)) || (softPostDeferred && submitIrisOverlays && (positiveGlow || negativeGlowMasked)))
                {
                    if (irisWorldPaintDeferral)
                    {
                        this.overlayRenderer.submitDeferredStructureGlowOverlay(this.data, context, glowSettings, legacyGlow, glowIntensity, mainTint3D.a, context.overlay, false, shaders, hasGlowTransform ? glowTransform : null, null, (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                    else if (!softPostDeferred)
                    {
                        this.overlayRenderer.renderStructureGlowOverlay(this.data, context, context.stack, glowSettings, legacyGlow, glowIntensity, mainTint3D.a, context.overlay, false, shaders, null, (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, context.overlay, shaders, null, true, false));
                    }
                }
            }


            BBSRendering.disableBlend();
            BBSRendering.enableDepthTest();
            BBSRendering.depthFunc(GL11.GL_LEQUAL);
        }

            CustomVertexConsumerProvider.clearRunnables();
        }
        finally
        {
            context.stack.popPose();
        }
    }

    /**
     * Noshading soft Structure: single deferred pass (sorted color + stamp + overlays).
     */
    private void runStructureSoftDeferredPass(FormRenderingContext context, Matrix4f positionMatrix, Matrix3f normalMatrix, Function<VertexConsumer, VertexConsumer> mainRecolor, Color glowShaderTint, int light, int overlay, boolean beTint, boolean depthWrite, IModelVAO vao, Color mainTint, boolean positivePaint, PaintSettings paintSettings, Color resolvedPaint, boolean applyColorTint, Color formColor3D, boolean positiveGlow, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean shaders, boolean sortedColor)
    {
        PoseStack overlayStack = new PoseStack();

        overlayStack.last().pose().set(positionMatrix);
        overlayStack.last().normal().set(normalMatrix);

        try
        {
            BBSRendering.enableDepthTest();
            BBSRendering.depthFunc(GL11.GL_LEQUAL);
            BBSRendering.enableBlend();
            BBSRendering.defaultBlendFunc();

            if (sortedColor)
            {
                ShaderOpacityPatch.setFlushingDepthWrite(false);
                BBSRendering.depthMask(false);
                this.renderStructureSoftSortedColor(context, overlayStack, mainRecolor, glowShaderTint, light, overlay);
            }

            this.runStructureSoftDeferredTailBody(context, overlayStack, light, overlay, beTint, depthWrite, vao, mainTint, positivePaint, paintSettings, resolvedPaint, applyColorTint, formColor3D, positiveGlow, glowSettings, legacyGlow, glowIntensity, shaders);
        }
        finally
        {
            BBSRendering.colorMask(true, true, true, true);
            BBSRendering.defaultBlendFunc();
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
            ShaderOpacityPatch.setFlushingDepthWrite(depthWrite);
            BBSRendering.depthMask(depthWrite);
        }
    }

    /**
     * Soft Structure color for one block — own post-deferred queue entry so soft BlockForms
     * can sort between trunk and leaves.
     * <p>
     * Iris: soft cutout/leaves draw with {@code depthMask false}, so pack fog that samples
     * {@code depthtex} sees terrain behind the foliage and washes leaf faces. Soft limbs avoid
     * this via a mesh depth stamp; solid Structure blocks get the VAO depth stamp. Non-solids
     * need a per-block depth-only prepass before color (color still depthMask false for soft
     * compositing). Vanilla FogStart/FogEnd toggles do nothing here — Iris ignores them.
     */
    private void runStructureSoftBlockDeferredColor(FormRenderingContext context, Matrix4f positionMatrix, Matrix3f normalMatrix, BlockEntry entry, RenderInfo info, Function<VertexConsumer, VertexConsumer> recolor, Color glowShaderTint, int light, int overlay)
    {
        PoseStack overlayStack = new PoseStack();
        boolean irisCutoutDepthPrepass = BBSRendering.isIrisShadersEnabled()
            && this.isSoftStructureNonSolid(entry.state);

        overlayStack.last().pose().set(positionMatrix);
        overlayStack.last().normal().set(normalMatrix);

        try
        {
            BBSRendering.enableDepthTest();
            BBSRendering.depthFunc(GL11.GL_LEQUAL);
            BBSRendering.enableBlend();
            BBSRendering.defaultBlendFunc();
            /* Never leave a leftover ColorModulator.a from prior form draws — Iris multiplies
             * it with recolor vertex alpha (opacity² → vanish near 82/255, leaf shadows thin). */
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
            StructureData.syncFancyGraphicsFromOptions();

            CustomVertexConsumerProvider immediateConsumers = FormUtilsClient.getProvider();

            overlayStack.pushPose();
            overlayStack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            if (irisCutoutDepthPrepass)
            {
                CustomVertexConsumerProvider.clearRunnables();
                ShaderOpacityPatch.setFlushingDepthWrite(true);
                BBSRendering.depthMask(true);
                BBSRendering.colorMask(false, false, false, false);
                BBSRendering.disableBlend();
                this.renderStructureSoftBlock(entry, info, overlayStack, immediateConsumers, recolor);
                immediateConsumers.draw();
                BBSRendering.enableBlend();
                BBSRendering.defaultBlendFunc();
                BBSRendering.colorMask(true, true, true, true);
            }

            ShaderOpacityPatch.setFlushingDepthWrite(false);
            BBSRendering.depthMask(false);
            this.beginStructureSoftGlowColorModulator(glowShaderTint);
            this.renderStructureSoftBlock(entry, info, overlayStack, immediateConsumers, recolor);
            overlayStack.popPose();
            immediateConsumers.draw();
            RecolorVertexConsumer.newColor = null;
            CustomVertexConsumerProvider.clearRunnables();
        }
        finally
        {
            BBSRendering.colorMask(true, true, true, true);
            BBSRendering.defaultBlendFunc();
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
            ShaderOpacityPatch.setFlushingDepthWrite(false);
            BBSRendering.depthMask(false);
        }
    }

    /**
     * Install ColorModulator at {@link RenderType} draw time (via
     * {@link CustomVertexConsumerProvider#hijackVertexFormat}) so Iris soft Structure bloom
     * matches BlockForm. Must run after depth-only prepass so depth stamps stay untinted.
     */
    private void beginStructureSoftGlowColorModulator(Color glowShaderTint)
    {
        if (glowShaderTint == null)
        {
            CustomVertexConsumerProvider.clearRunnables();
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);

            return;
        }

        CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
        {
            if (FormUtilsClient.isCrumblingLayer(layer))
            {
                return;
            }

            BBSRendering.enableBlend();
            BBSRendering.defaultBlendFunc();
            /* RGB carries emission; alpha stays 1 — form opacity is already in white recolor. */
            BBSRendering.setShaderColor(glowShaderTint.r, glowShaderTint.g, glowShaderTint.b, 1F);
            ShaderOpacityPatch.reassertPostDeferredDepthState(false);
        });
    }

    /**
     * Soft Structure tail after per-block colors: block entities, solid VAO depth stamp, overlays.
     * Sorted with the nearest block key so it runs after Structure color entries.
     */
    private void runStructureSoftDeferredTail(FormRenderingContext context, Matrix4f positionMatrix, Matrix3f normalMatrix, int light, int overlay, boolean beTint, boolean depthWrite, IModelVAO vao, Color mainTint, boolean positivePaint, PaintSettings paintSettings, Color resolvedPaint, boolean applyColorTint, Color formColor3D, boolean positiveGlow, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean shaders)
    {
        PoseStack overlayStack = new PoseStack();

        overlayStack.last().pose().set(positionMatrix);
        overlayStack.last().normal().set(normalMatrix);

        try
        {
            BBSRendering.enableDepthTest();
            BBSRendering.depthFunc(GL11.GL_LEQUAL);
            BBSRendering.enableBlend();
            BBSRendering.defaultBlendFunc();
            this.runStructureSoftDeferredTailBody(context, overlayStack, light, overlay, beTint, depthWrite, vao, mainTint, positivePaint, paintSettings, resolvedPaint, applyColorTint, formColor3D, positiveGlow, glowSettings, legacyGlow, glowIntensity, shaders);
        }
        finally
        {
            BBSRendering.colorMask(true, true, true, true);
            BBSRendering.defaultBlendFunc();
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            ShaderOpacityPatch.setFlushingDepthWrite(depthWrite);
            BBSRendering.depthMask(depthWrite);
        }
    }

    private void runStructureSoftDeferredTailBody(FormRenderingContext context, PoseStack overlayStack, int light, int overlay, boolean beTint, boolean depthWrite, IModelVAO vao, Color mainTint, boolean positivePaint, PaintSettings paintSettings, Color resolvedPaint, boolean applyColorTint, Color formColor3D, boolean positiveGlow, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean shaders)
    {
        if (this.data.hasBlockEntityLayer())
        {
            this.renderBlockEntitiesPass(context, overlayStack, light, overlay, beTint);
            ShaderOpacityPatch.setFlushingDepthWrite(false);
            BBSRendering.depthMask(false);
        }

        if (depthWrite)
        {
            ShaderOpacityPatch.setFlushingDepthWrite(true);
            BBSRendering.depthMask(true);
            BBSRendering.colorMask(false, false, false, false);
            BBSRendering.disableBlend();

            try
            {
                this.renderStructureSoftDepthStamp(overlayStack, vao, mainTint, light, overlay);
            }
            finally
            {
                BBSRendering.enableBlend();
                BBSRendering.defaultBlendFunc();
                BBSRendering.colorMask(true, true, true, true);
            }
        }

        /* Iris paint/tint/glow overlays are submitted on the paint-overlay queue when soft
         * (see softPostDeferred + submitIrisOverlays). Only draw them here without Iris. */
        boolean irisOverlays = BBSRendering.isIrisWorldPaintDeferral();

        if (positivePaint && !irisOverlays)
        {
            EffectTransform paintTransform = paintSettings == null ? null : paintSettings.transform;

            this.overlayRenderer.renderStructurePaintOverlay(this.data, vao, context, overlayStack, resolvedPaint, mainTint.a, overlay, true, shaders, paintTransform, glowSettings, legacyGlow, glowIntensity, layer -> this.renderPaintLayer(layer, context, overlayStack, overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, overlay, shaders, null, true, false));
        }

        if (applyColorTint && !irisOverlays)
        {
            this.overlayRenderer.renderStructureColorTintOverlay(this.data, this.form, context, overlayStack, formColor3D, mainTint.a, overlay, true, shaders, false, layer -> this.renderPaintLayer(layer, context, overlayStack, overlay, null), (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, overlay, shaders, null, true, false));
        }

        if (positiveGlow && !irisOverlays)
        {
            ShaderOpacityPatch.setFlushingDepthWrite(false);
            BBSRendering.depthMask(false);
            this.overlayRenderer.renderStructureGlowOverlay(this.data, context, overlayStack, glowSettings, legacyGlow, glowIntensity, mainTint.a, overlay, false, shaders, null, (s) -> this.renderStructureCulledWorld(context, s, FormUtilsClient.getProvider(), light, overlay, shaders, null, true, false));
        }

        ShaderOpacityPatch.setFlushingDepthWrite(depthWrite);
        BBSRendering.depthMask(depthWrite);
        CustomVertexConsumerProvider.clearRunnables();
    }

    /**
     * Soft Structure color: every block back-to-front with depth-write off.
     * Used by the noshading single-pass path; Iris/BBS soft uses per-block queue entries.
     */
    private void renderStructureSoftSortedColor(FormRenderingContext context, PoseStack stack, Function<VertexConsumer, VertexConsumer> recolor, Color glowShaderTint, int light, int overlay)
    {
        BBSRendering.enableBlend();
        BBSRendering.defaultBlendFunc();
        BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
        StructureData.syncFancyGraphicsFromOptions();
        ShaderOpacityPatch.setFlushingDepthWrite(false);
        BBSRendering.depthMask(false);
        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        this.beginStructureSoftGlowColorModulator(glowShaderTint);

        RenderInfo info = this.calculateRenderInfo(context, false);
        List<BlockEntry> sorted = new ArrayList<>(this.data.getBlocks());
        Matrix4f drawMatrix = stack.last().pose();
        Matrix4f viewLocal = new Matrix4f(RenderSystem.getModelViewMatrixCopy()).mul(drawMatrix);

        sorted.sort((a, b) ->
        {
            int byDepth = Double.compare(
                this.computeStructureBlockViewSortKey(b, info, viewLocal),
                this.computeStructureBlockViewSortKey(a, info, viewLocal)
            );

            if (byDepth != 0)
            {
                return byDepth;
            }

            return Boolean.compare(this.isSoftStructureNonSolid(a.state), this.isSoftStructureNonSolid(b.state));
        });

        CustomVertexConsumerProvider immediateConsumers = FormUtilsClient.getProvider();

        try
        {
            for (BlockEntry entry : sorted)
            {
                stack.pushPose();
                stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);
                this.renderStructureSoftBlock(entry, info, stack, immediateConsumers, recolor);
                stack.popPose();
            }

            immediateConsumers.draw();
        }
        finally
        {
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
            BBSRendering.disableBlend();
            RecolorVertexConsumer.newColor = null;
            ShaderOpacityPatch.setFlushingDepthWrite(false);
        }
    }

    /**
     * Queue sort key for one Structure block — same formula as soft {@code BlockForm}
     * ({@code capturePaintOverlayRootMatrix} + film look-axis −Z / lengthSq) so soft
     * BlockForms and Structure leaves share one comparable depth axis.
     */
    private double computeStructureBlockFormSortKey(BlockEntry entry, RenderInfo info, Matrix4f formLocalMatrix, FormRenderingContext context)
    {
        Matrix4f blockMatrix = new Matrix4f(formLocalMatrix);

        blockMatrix.translate(
            entry.pos.getX() - info.pivotX + 0.5F,
            entry.pos.getY() - info.pivotY + 0.5F,
            entry.pos.getZ() - info.pivotZ + 0.5F
        );

        return this.computeStructureCompatibleFormSortKey(blockMatrix, context);
    }

    private double computeStructureBlockViewSortKey(BlockEntry entry, RenderInfo info, Matrix4f viewLocal)
    {
        Vector4f center = new Vector4f(
            entry.pos.getX() - info.pivotX + 0.5F,
            entry.pos.getY() - info.pivotY + 0.5F,
            entry.pos.getZ() - info.pivotZ + 0.5F,
            1F
        );

        viewLocal.transform(center);

        return -center.z;
    }

    private void renderBlockBatched(BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack stack, VertexConsumer vc)
    {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);

        if (model != null)
        {
            ModelBlockRenderer modelRenderer = new ModelBlockRenderer(true, true, Minecraft.getInstance().getBlockColors());
            modelRenderer.tesselateBlock(
                (x, y, z, quad, instance) -> vc.putBakedQuad(stack.last(), quad, instance),
                0F, 0F, 0F,
                view, pos, state, model, state.getSeed(pos)
            );
        }
    }

    private void renderFluidBatched(BlockPos pos, BlockAndTintGetter view, VertexConsumer vc, BlockState state)
    {
        FluidState fluidState = state.getFluidState();

        if (fluidState.isEmpty())
        {
            return;
        }

        FluidRenderer fluidRenderer = new FluidRenderer(Minecraft.getInstance().getModelManager().getFluidStateModelSet());
        fluidRenderer.tesselate(view, pos, (layer) -> vc, state, fluidState);
    }

    private boolean isSoftStructureNonSolid(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        if (state.getBlock() instanceof LeavesBlock
            || StructureData.isTranslucentBlock(state)
            || StructureData.isBiomeTinted(state))
        {
            return true;
        }

        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);

        if (model != null)
        {
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(42L), parts);

            for (BlockStateModelPart part : parts)
            {
                for (Direction direction : Direction.values())
                {
                    for (BakedQuad quad : part.getQuads(direction))
                    {
                        ChunkSectionLayer layer = quad.materialInfo().layer();

                        if (layer == ChunkSectionLayer.CUTOUT || layer == ChunkSectionLayer.TRANSLUCENT)
                        {
                            return true;
                        }
                    }
                }

                for (BakedQuad quad : part.getQuads(null))
                {
                    ChunkSectionLayer layer = quad.materialInfo().layer();

                    if (layer == ChunkSectionLayer.CUTOUT || layer == ChunkSectionLayer.TRANSLUCENT)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void renderStructureSoftBlock(BlockEntry entry, RenderInfo info, PoseStack stack, MultiBufferSource consumers, Function<VertexConsumer, VertexConsumer> recolor)
    {
        if (entry.state.getBlock() instanceof LeavesBlock)
        {
            this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);

            return;
        }

        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
        RenderType layer = Sheets.translucentBlockItemSheet();
        VertexConsumer vc = consumers.getBuffer(layer);

        if (recolor != null)
        {
            vc = recolor.apply(vc);
        }

        if (this.form.renderFluid.get() && !entry.state.getFluidState().isEmpty())
        {
            RenderType fluidLayer = shadersEnabled
                ? Sheets.translucentBlockItemSheet()
                : RenderTypes.translucentMovingBlock();
            VertexConsumer fluidVc = consumers.getBuffer(fluidLayer);

            if (recolor != null)
            {
                fluidVc = recolor.apply(fluidVc);
            }

            fluidVc = new TransformingVertexConsumer(fluidVc, stack.last(), entry.pos, shadersEnabled);
            this.renderFluidBatched(entry.pos, info.view, fluidVc, entry.state);
        }

        if (entry.state.getRenderShape() != RenderShape.INVISIBLE)
        {
            this.renderBlockBatched(entry.state, entry.pos, info.view, stack, vc);
        }
    }

    /**
     * Depth-only stamp of the solid structure VAO after soft color (no color write).
     * Keeps other forms/world occluded without letting soft color depth-kill itself.
     */
    private void renderStructureSoftDepthStamp(PoseStack stack, IModelVAO vao, Color mainTint, int light, int overlay)
    {
        if (vao == null)
        {
            return;
        }

        GlProgram shader = (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
            ? BBSRendering.getEntityTranslucentProgram()
            : BBSShaders.getModel();

        BBSRendering.bindProgram(shader);
        BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
        ModelVAORenderer.render(shader, vao, stack, mainTint.r, mainTint.g, mainTint.b, mainTint.a, light, overlay);
    }

    /**
     * Soft-opacity queue key — same axis as soft BlockForm so forms interleave correctly.
     */
    private double computeStructureFormSortKey(Matrix4f drawMatrix, FormRenderingContext context)
    {
        return this.computeStructureCompatibleFormSortKey(drawMatrix, context);
    }

    /**
     * Matches {@code BlockFormRenderer.computeBlockFormSortKey}: camera-baked view space,
     * film look-axis −Z, otherwise lengthSq.
     */
    private double computeStructureCompatibleFormSortKey(Matrix4f drawMatrix, FormRenderingContext context)
    {
        Vector4f origin = new Vector4f(0F, 0F, 0F, 1F);
        Matrix4f viewSpace = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(drawMatrix));

        viewSpace.transform(origin);

        boolean filmLookAxis = context != null
            && context.type == FormRenderType.ENTITY
            && context.camera != null
            && !context.modelRenderer;

        if (filmLookAxis)
        {
            return -origin.z;
        }

        return origin.x * origin.x + origin.y * origin.y + origin.z * origin.z;
    }

    private void checkLightState()
    {
        StructureLightSettings sl = this.form.structureLight.get();
        boolean currentEmitLight = (sl != null) ? sl.enabled : this.form.emitLight.get();
        int currentLightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();

        if (currentEmitLight != this.lastEmitLight || currentLightIntensity != this.lastLightIntensity)
        {
            this.vaoManager.setVaoDirty(true);
            this.lastEmitLight = currentEmitLight;
            this.lastLightIntensity = currentLightIntensity;
        }
    }

    private IModelVAO getVao()
    {
        IModelVAO vao = this.vaoManager.getStructureVao(this.data.getLastFile());

        if (vao == null || this.vaoManager.isVaoDirty())
        {
            this.vaoManager.buildStructureVAO(this.data.getLastFile(), () ->
            {
                Function<VertexConsumer, VertexConsumer> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());
                PoseStack captureStack = new PoseStack();
                FormRenderingContext captureContext = new FormRenderingContext()
                    .set(FormRenderType.PREVIEW, null, captureStack, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, 0F);

                this.renderStructureCulledWorld(captureContext, captureStack, FormUtilsClient.getProvider(), LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, false, captureRecolor, false, false);
            });

            vao = this.vaoManager.getStructureVao(this.data.getLastFile());
        }

        return vao;
    }

    private IModelVAO getPickingVao()
    {
        IModelVAO pickingVao = this.vaoManager.getStructureVaoPicking(this.data.getLastFile());

        if (pickingVao == null || this.vaoManager.isVaoPickingDirty())
        {
            this.vaoManager.buildStructureVAOPicking(
                this.data.getLastFile(),
                this.data,
                () ->
                {
                    Function<VertexConsumer, VertexConsumer> captureRecolor = BBSRendering.getColorConsumer(this.resolveStructureBlendColor());
                    PoseStack captureStack = new PoseStack();
                    FormRenderingContext captureContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, captureStack, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, 0F);

                    this.renderStructureCulledWorld(captureContext, captureStack, FormUtilsClient.getProvider(), LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, false, captureRecolor, false, false);
                },
                () ->
                {
                    PoseStack captureStack = new PoseStack();
                    FormRenderingContext captureContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, captureStack, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, 0F);

                    this.renderBlockEntitiesOnly(captureContext, captureStack, FormUtilsClient.getProvider(), LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, false);
                },
                collector ->
                {
                    PoseStack captureStack = new PoseStack();
                    FormRenderingContext captureContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, captureStack, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, 0F);

                    this.appendBlockEntityPickCubes(collector, captureContext);
                }
            );

            pickingVao = this.vaoManager.getStructureVaoPicking(this.data.getLastFile());
        }

        return pickingVao;
    }

    private static class RenderInfo
    {
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public VirtualBlockRenderView view;
        public BlockPos anchor;
    }

    private RenderInfo calculateRenderInfo(FormRenderingContext context, boolean forceMaxSkyLight)
    {
        RenderInfo info = new RenderInfo();
        float cx;
        float cy;
        float cz;
        float parityXAuto = 0F;
        float parityZAuto = 0F;

        if (this.data.getBoundsMin() != null && this.data.getBoundsMax() != null)
        {
            cx = (this.data.getBoundsMin().getX() + this.data.getBoundsMax().getX()) / 2F;
            cz = (this.data.getBoundsMin().getZ() + this.data.getBoundsMax().getZ()) / 2F;
            cy = this.data.getBoundsMin().getY();

            int widthX = this.data.getBoundsMax().getX() - this.data.getBoundsMin().getX() + 1;
            int widthZ = this.data.getBoundsMax().getZ() - this.data.getBoundsMin().getZ() + 1;

            parityXAuto = (widthX % 2 == 1) ? -0.5F : 0F;
            parityZAuto = (widthZ % 2 == 1) ? -0.5F : 0F;
        }
        else
        {
            cx = this.data.getSize().getX() / 2F;
            cy = 0F;
            cz = this.data.getSize().getZ() / 2F;
        }

        info.pivotX = cx - parityXAuto;
        info.pivotY = cy;
        info.pivotZ = cz - parityZAuto;

        if (this.data.getEntriesCache() == null || this.data.getEntriesCache().length != this.data.getBlocks().size())
        {
            VirtualBlockRenderView.Entry[] cache = new VirtualBlockRenderView.Entry[this.data.getBlocks().size()];

            for (int i = 0; i < this.data.getBlocks().size(); i++)
            {
                BlockEntry be = this.data.getBlocks().get(i);
                cache[i] = new VirtualBlockRenderView.Entry(be.state, be.pos);
            }

            this.data.setEntriesCache(cache);
        }

        StructureLightSettings slRuntime = this.form.structureLight.get();
        boolean lightsEnabled;
        int lightIntensity;

        if (slRuntime != null)
        {
            lightsEnabled = slRuntime.enabled;
            lightIntensity = slRuntime.intensity;
        }
        else
        {
            lightsEnabled = this.form.emitLight.get();
            lightIntensity = this.form.lightIntensity.get();
        }

        if (this.data.getCachedView() == null)
        {
            this.data.setCachedView(new StructureVirtualBlockRenderView(Arrays.asList(this.data.getEntriesCache())));
        }

        info.view = this.data.getCachedView()
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled)
            .setLightIntensity(lightIntensity);

        if (lightsEnabled)
        {
            this.data.getCachedView().setVirtualMode(true, lightIntensity).setIgnoreWorldBlockLight(false);
        }
        else
        {
            this.data.getCachedView().setVirtualMode(false, 0).setIgnoreWorldBlockLight(true);
        }

        boolean isItemContext = (context.type == FormRenderType.ITEM
            || context.type == FormRenderType.ITEM_FP
            || context.type == FormRenderType.ITEM_TP
            || context.type == FormRenderType.ITEM_INVENTORY);

        if (isItemContext || context.entity == null)
        {
            Minecraft mc = Minecraft.getInstance();
            info.anchor = (mc.player != null) ? mc.player.blockPosition() : BlockPos.ZERO;
        }
        else
        {
            info.anchor = new BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        int baseDx = (int) Math.floor(-info.pivotX);
        int baseDy = (int) Math.floor(-info.pivotY);
        int baseDz = (int) Math.floor(-info.pivotZ);

        info.view.setWorldAnchor(info.anchor, baseDx, baseDy, baseDz)
            .setForceMaxSkyLight(!this.vaoManager.isCapturingVAO() && (context.ui
                || context.type == FormRenderType.PREVIEW
                || context.type == FormRenderType.ITEM_INVENTORY || forceMaxSkyLight));

        return info;
    }

    private RenderType resolveStructureBlockLayer(BlockState state, boolean useEntityLayers)
    {
        if (state.getBlock() instanceof LeavesBlock)
        {
            return this.resolveStructureLeavesLayer(state, useEntityLayers);
        }

        return useEntityLayers
            ? Sheets.translucentBlockItemSheet()
            : RenderTypes.cutoutMovingBlock();
    }

    private RenderType resolveStructureLeavesLayer(BlockState state, boolean useEntityLayers)
    {
        boolean irisWorld = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();

        if (irisWorld || useEntityLayers)
        {
            return Sheets.cutoutBlockItemSheet();
        }

        if (StructureData.isFancyGraphicsEnabled())
        {
            return RenderTypes.cutoutMovingBlock();
        }

        StructureData.syncFancyGraphicsFromOptions();
        return RenderTypes.solidMovingBlock();
    }

    private void renderStructureLeaves(BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack stack, MultiBufferSource consumers, Function<VertexConsumer, VertexConsumer> recolor)
    {
        boolean softOpacity = this.wantsSoftStructureBlockLayers();
        RenderType layer = softOpacity
            ? Sheets.translucentBlockItemSheet()
            : this.resolveStructureLeavesLayer(state, false);
        VertexConsumer vc = consumers.getBuffer(layer);

        if (recolor != null)
        {
            vc = recolor.apply(vc);
        }

        this.renderBlockBatched(state, pos, view, stack, vc);
    }

    /**
     * Soft form opacity (or an active soft post-deferred redraw) needs entity translucent
     * layers for cutout/biome/translucent special blocks — not terrain cutout/translucent.
     * <p>
     * Iris shadow pass keeps cutout/entity-block layers so leaf holes stay texture-cutout while
     * form opacity is Bayer-dithered via {@code bbs_is_shadow_form} (same as solid VAO).
     */
    private boolean wantsSoftStructureBlockLayers()
    {
        if (BBSRendering.isIrisShadowPass())
        {
            return false;
        }

        return this.form.getFormOpacity() < ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA
            || ShaderOpacityPatch.isPostDeferredPhase();
    }

    private void renderStructureCulledWorld(FormRenderingContext context, PoseStack stack, MultiBufferSource consumers, int light, int overlay, boolean useEntityLayers, Function<VertexConsumer, VertexConsumer> recolor, boolean skipBlockEntities, boolean skipSpecialBlocks)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        float globalAlpha;

        StructureData.syncFancyGraphicsFromOptions();

        for (BlockEntry entry : this.data.getBlocks())
        {
            RenderType layer;
            VertexConsumer vc;
            Block block;

            stack.pushPose();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            if (this.vaoManager.isCapturingVAO() && !this.vaoManager.isCapturingIncludeSpecialBlocks()
                && (StructureData.isAnimatedTexture(entry.state) || StructureData.isBiomeTinted(entry.state) || StructureData.isTranslucentBlock(entry.state)))
            {
                stack.popPose();
                continue;
            }

            if (skipSpecialBlocks && (StructureData.isAnimatedTexture(entry.state) || StructureData.isBiomeTinted(entry.state) || StructureData.isTranslucentBlock(entry.state)))
            {
                stack.popPose();
                continue;
            }

            layer = this.resolveStructureBlockLayer(entry.state, useEntityLayers);
            globalAlpha = this.form.getFormOpacity();

            if (globalAlpha < ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA || ShaderOpacityPatch.isPostDeferredPhase())
            {
                if (!BBSRendering.isIrisShadowPass())
                {
                    /* Entity translucent — terrain translucent/cutout fails in soft post-deferred. */
                    layer = Sheets.translucentBlockItemSheet();
                }
            }

            vc = consumers.getBuffer(layer);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            if (this.form.renderFluid.get() && !entry.state.getFluidState().isEmpty())
            {
                boolean shaders = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                RenderType fluidLayer = shaders
                    ? Sheets.translucentBlockItemSheet()
                    : RenderTypes.translucentMovingBlock();
                VertexConsumer fluidVc = consumers.getBuffer(fluidLayer);

                if (recolor != null)
                {
                    fluidVc = recolor.apply(fluidVc);
                }

                fluidVc = new TransformingVertexConsumer(fluidVc, stack.last(), entry.pos, shaders);
                this.renderFluidBatched(entry.pos, info.view, fluidVc, entry.state);
            }

            if (entry.state.getRenderShape() != RenderShape.INVISIBLE)
            {
                if (entry.state.getBlock() instanceof LeavesBlock)
                {
                    this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
                }
                else
                {
                    this.renderBlockBatched(entry.state, entry.pos, info.view, stack, vc);
                }
            }

            block = entry.state.getBlock();

            if (!this.vaoManager.isCapturingVAO() && !skipBlockEntities && block instanceof EntityBlock)
            {
                this.renderSingleBlockEntity(entry, info, context, stack, overlay);
            }

            stack.popPose();
        }

        RecolorVertexConsumer.newColor = null;
    }

    private void renderSingleBlockEntity(BlockEntry entry, RenderInfo info, FormRenderingContext context, PoseStack stack, int overlay)
    {
        Block block = entry.state.getBlock();
        int dx = (int) Math.floor(entry.pos.getX() - info.pivotX);
        int dy = (int) Math.floor(entry.pos.getY() - info.pivotY);
        int dz = (int) Math.floor(entry.pos.getZ() - info.pivotZ);
        BlockPos worldPos = info.anchor.offset(dx, dy, dz);
        BlockEntity be = ((EntityBlock) block).newBlockEntity(worldPos, entry.state);

        if (be != null)
        {
            if (entry.nbt != null)
            {
                this.readBlockEntityNbt(be, entry.nbt);
            }

            if (Minecraft.getInstance().level != null)
            {
                be.setLevel(Minecraft.getInstance().level);
            }

            BlockEntityRenderDispatcher beDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
            BlockEntityRenderer<?, ?> renderer = beDispatcher.getRenderer(be);

            int skyLight = info.view.getBrightness(LightLayer.SKY, entry.pos);
            int blockLight = info.view.getBrightness(LightLayer.BLOCK, entry.pos);
            int beLight = LightTexture.pack(blockLight, skyLight);

            if (renderer != null)
            {
                @SuppressWarnings({"rawtypes", "unchecked"})
                BlockEntityRenderer raw = (BlockEntityRenderer) renderer;
                BlockEntityRenderState state = raw.createRenderState();

                raw.extractRenderState(be, state, 0F, Vec3.ZERO, null);
                state.lightCoords = beLight;

                Color beTint = this.resolveStructureBlockEntityColor();
                boolean beShadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

                this.applyBlockEntityOnlyShaderShadow(beTint, beShadowPass);

                try
                {
                    FeatureRenderDispatcher dispatcher = Minecraft.getInstance().gameRenderer.featureRenderDispatcher();
                    SubmitNodeStorage storage = new SubmitNodeStorage();
                    CameraRenderState cameraRenderState = new CameraRenderState();

                    raw.submit(state, stack, storage, cameraRenderState);

                    if (beTint != null)
                    {
                        BBSRendering.setShaderColor(beTint.r, beTint.g, beTint.b, beTint.a);
                    }

                    dispatcher.renderAllFeatures(storage);
                }
                finally
                {
                    if (beTint != null)
                    {
                        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                    }
                }
            }
        }
    }

    private void renderLayerGroup(List<BlockEntry> group, FormRenderingContext context, PoseStack stack, int light, int overlay, Function<VertexConsumer, VertexConsumer> recolor, Color shaderTint, boolean forceDrawLeaves)
    {
        BBSRendering.enableBlend();
        BBSRendering.defaultBlendFunc();
        BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
        StructureData.syncFancyGraphicsFromOptions();

        RenderInfo info = this.calculateRenderInfo(context, false);
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
        boolean shadowPass = BBSRendering.isIrisShadowPass()
            || (context != null && context.isShadowPass);

        /* Reset before hijack — leftover ColorModulator.a squares leaf/cutout opacity. */
        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);

        if (shadowPass)
        {
            /* Always force ColorModulator.a = 1 in shadow (even without shaderTint / when
             * isRenderingWorld is false). Leftover modulator alpha × recolor opacity squares
             * leaf Bayer dither vs solid VAO. Negative paint uses BlockPaint recolor — same rule. */
            final Color tint = shaderTint;

            CustomVertexConsumerProvider.hijackVertexFormat((l) ->
            {
                if (tint != null)
                {
                    BBSRendering.setShaderColor(tint.r, tint.g, tint.b, 1F);
                }
                else
                {
                    BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                }

                ShaderOpacityPatch.uploadShadowFormUniform();
            });
        }
        else if (shadersEnabled && shaderTint != null)
        {
            CustomVertexConsumerProvider.hijackVertexFormat((l) ->
            {
                BBSRendering.setShaderColor(shaderTint.r, shaderTint.g, shaderTint.b, shaderTint.a);
            });
        }
        else
        {
            CustomVertexConsumerProvider.clearRunnables();
        }

        try
        {
            for (BlockEntry entry : group)
            {
                stack.pushPose();
                stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

                if (entry.state.getBlock() instanceof LeavesBlock)
                {
                    this.renderStructureLeaves(entry.state, entry.pos, info.view, stack, consumers, recolor);
                    stack.popPose();
                    continue;
                }

                RenderType layer = this.resolveStructureBlockLayer(entry.state, shadersEnabled);

                if (this.wantsSoftStructureBlockLayers())
                {
                    /* Always entity translucent under soft opacity - terrain translucent vanishes
                     * when drawn from the soft post-deferred flush without shaders. */
                    layer = Sheets.translucentBlockItemSheet();
                }

                VertexConsumer vc = consumers.getBuffer(layer);

                if (recolor != null)
                {
                    vc = recolor.apply(vc);
                }

                if (this.form.renderFluid.get() && !entry.state.getFluidState().isEmpty())
                {
                    RenderType fluidLayer = shadersEnabled
                        ? Sheets.translucentBlockItemSheet()
                        : RenderTypes.translucentMovingBlock();
                    VertexConsumer fluidVc = consumers.getBuffer(fluidLayer);

                    if (recolor != null)
                    {
                        fluidVc = recolor.apply(fluidVc);
                    }

                    fluidVc = new TransformingVertexConsumer(fluidVc, stack.last(), entry.pos, shadersEnabled);
                    this.renderFluidBatched(entry.pos, info.view, fluidVc, entry.state);
                }

                if (entry.state.getRenderShape() != RenderShape.INVISIBLE)
                {
                    this.renderBlockBatched(entry.state, entry.pos, info.view, stack, vc);
                }

                stack.popPose();
            }

            consumers.draw();
        }
        finally
        {
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.disableBlend();
            CustomVertexConsumerProvider.clearRunnables();
            RecolorVertexConsumer.newColor = null;
        }
    }

    private void renderPaintLayer(StructurePaintLayer layer, FormRenderingContext context, PoseStack stack, int overlay, Function<VertexConsumer, VertexConsumer> recolor)
    {
        if (layer == StructurePaintLayer.BIOME)
        {
            this.renderLayerGroup(this.data.getBiomeTintedBlocks(), context, stack, LightTexture.FULL_BRIGHT, overlay, recolor, null, true);
        }
        else if (layer == StructurePaintLayer.ANIMATED)
        {
            this.renderLayerGroup(this.data.getAnimatedBlocks(), context, stack, LightTexture.FULL_BRIGHT, overlay, recolor, null, false);
        }
        else
        {
            this.renderLayerGroup(this.data.getTranslucentBlocks(), context, stack, LightTexture.FULL_BRIGHT, overlay, recolor, null, false);
        }
    }

    private Color resolveStructureBlendColor()
    {
        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color tint = Color.white();

        if (FormColorEffects.shouldBakeFormColor(storedFormColor))
        {
            tint.mul(rawFormColor);
        }

        this.form.applyFormOpacity(tint);
        return tint;
    }

    private Color resolveStructureBlockEntityColor()
    {
        Color tint = FormColorEffects.resolveBlockEntityTint(this.form.color.get(), this.form.paintSettings.get(), this.form.paintColor.get());
        this.form.applyFormOpacity(tint);
        return tint;
    }

    private void applyBlockEntityOnlyShaderShadow(Color color, boolean shadowPass)
    {
        if (color == null || !shadowPass || !this.data.isEntirelyBlockEntities())
        {
            return;
        }

        color.a = PaintSettings.SHADER_SHADOW_BLOCK_ENTITY;
    }

    private boolean needsDeferredBlockEntityTint(boolean positivePaint, boolean applyColorTint, Color storedFormColor)
    {
        Color beTint = this.resolveStructureBlockEntityColor();
        return beTint.r < 0.999F || beTint.g < 0.999F || beTint.b < 0.999F;
    }

    private void submitDeferredStructureBlockEntityTint(FormRenderingContext context, int overlay)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrixCopy());
        Matrix4f exactStack = new Matrix4f(context.stack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(context.stack.last().normal());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            PoseStack overlayStack = new PoseStack();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            overlayStack.last().pose().set(exactStack);
            overlayStack.last().normal().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            MatrixStackUtils.applyModelViewMatrix();

            try
            {
                this.renderBlockEntitiesOnly(context, overlayStack, consumers, LightTexture.FULL_BRIGHT, overlay, true);
                consumers.draw();
            }
            catch (Throwable ignored)
            {
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
                consumers.setSubstitute(null);
                BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            }
        });
    }

    private Function<VertexConsumer, VertexConsumer> getMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void renderBlockEntitiesPass(FormRenderingContext context, PoseStack stack, int light, int overlay, boolean applyColorTint)
    {
        try
        {
            CustomVertexConsumerProvider beConsumers = FormUtilsClient.getProvider();
            this.renderBlockEntitiesOnly(context, stack, beConsumers, light, overlay, applyColorTint);
            beConsumers.draw();
        }
        catch (Throwable ignored)
        {
        }
    }

    private void renderBlockEntitiesOnly(FormRenderingContext context, PoseStack stack, MultiBufferSource consumers, int light, int overlay, boolean applyColorTint)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        BlockEntityRenderDispatcher beDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        for (BlockEntry entry : this.data.getBlockEntitiesList())
        {
            Block block = entry.state.getBlock();

            stack.pushPose();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            int dx = (int) Math.floor(entry.pos.getX() - info.pivotX);
            int dy = (int) Math.floor(entry.pos.getY() - info.pivotY);
            int dz = (int) Math.floor(entry.pos.getZ() - info.pivotZ);
            BlockPos worldPos = info.anchor.offset(dx, dy, dz);

            BlockEntity be = ((EntityBlock) block).newBlockEntity(worldPos, entry.state);

            if (be != null)
            {
                if (entry.nbt != null)
                {
                    this.readBlockEntityNbt(be, entry.nbt);
                }

                if (Minecraft.getInstance().level != null)
                {
                    be.setLevel(Minecraft.getInstance().level);
                }

                BlockEntityRenderer<?, ?> renderer = beDispatcher.getRenderer(be);
                int skyLight = info.view.getBrightness(LightLayer.SKY, entry.pos);
                int blockLight = info.view.getBrightness(LightLayer.BLOCK, entry.pos);
                int beLight = LightTexture.pack(blockLight, skyLight);

                if (renderer != null)
                {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    BlockEntityRenderer raw = (BlockEntityRenderer) renderer;
                    BlockEntityRenderState state = raw.createRenderState();

                    raw.extractRenderState(be, state, 0F, Vec3.ZERO, null);
                    state.lightCoords = beLight;

                    Color beTint = null;
                    boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

                    if (applyColorTint)
                    {
                        beTint = this.resolveStructureBlockEntityColor();
                        this.applyBlockEntityOnlyShaderShadow(beTint, shadowPass);
                    }
                    else if (shadowPass && this.data.isEntirelyBlockEntities())
                    {
                        beTint = new Color(1F, 1F, 1F, PaintSettings.SHADER_SHADOW_BLOCK_ENTITY);
                    }

                    try
                    {
                        FeatureRenderDispatcher dispatcher = Minecraft.getInstance().gameRenderer.featureRenderDispatcher();
                        SubmitNodeStorage storage = new SubmitNodeStorage();
                        CameraRenderState cameraRenderState = new CameraRenderState();

                        raw.submit(state, stack, storage, cameraRenderState);

                        if (beTint != null)
                        {
                            BBSRendering.setShaderColor(beTint.r, beTint.g, beTint.b, beTint.a);
                        }

                        dispatcher.renderAllFeatures(storage);
                    }
                    finally
                    {
                        if (beTint != null)
                        {
                            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                        }
                    }
                }
            }

            stack.popPose();
        }
    }

    private void appendBlockEntityPickCubes(StructureVAOCollector collector, FormRenderingContext context)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.data.getBlockEntitiesList())
        {
            float x0 = entry.pos.getX() - info.pivotX;
            float y0 = entry.pos.getY() - info.pivotY;
            float z0 = entry.pos.getZ() - info.pivotZ;

            this.emitPickCube(collector, x0, y0, z0, x0 + 1F, y0 + 1F, z0 + 1F);
        }
    }

    private void emitPickCube(StructureVAOCollector collector, float x0, float y0, float z0, float x1, float y1, float z1)
    {
        this.emitPickQuad(collector, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0F, 0F, -1F);
        this.emitPickQuad(collector, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0F, 0F, 1F);
        this.emitPickQuad(collector, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 0F, -1F, 0F);
        this.emitPickQuad(collector, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0F, 1F, 0F);
        this.emitPickQuad(collector, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1F, 0F, 0F);
        this.emitPickQuad(collector, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1F, 0F, 0F);
    }

    private void emitPickQuad(StructureVAOCollector collector, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz)
    {
        collector.addVertex(x0, y0, z0).setColor(255, 255, 255, 255).setUv(0F, 0F).setUv1(0, 0).setUv2(0, 0).setNormal(nx, ny, nz);
        collector.addVertex(x1, y1, z1).setColor(255, 255, 255, 255).setUv(1F, 0F).setUv1(0, 0).setUv2(0, 0).setNormal(nx, ny, nz);
        collector.addVertex(x2, y2, z2).setColor(255, 255, 255, 255).setUv(1F, 1F).setUv1(0, 0).setUv2(0, 0).setNormal(nx, ny, nz);
        collector.addVertex(x3, y3, z3).setColor(255, 255, 255, 255).setUv(0F, 1F).setUv1(0, 0).setUv2(0, 0).setNormal(nx, ny, nz);
    }

    private void readBlockEntityNbt(BlockEntity be, CompoundTag nbt)
    {
        if (be == null || nbt == null)
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        HolderLookup.Provider registries = client.level != null
            ? client.level.registryAccess()
            : BBSMod.getRegistryManager();

        if (registries != null)
        {
            ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, registries, nbt);
            be.loadWithComponents(readView);
        }
    }

    private void ensureLoaded()
    {
        String file = this.form.structureFile.get();

        if (this.data.ensureLoaded(file))
        {
            this.vaoManager.clearCachedVao(this.data.getLastFile());
            this.vaoManager.setVaoDirty(true);
            this.vaoManager.setVaoPickingDirty(true);
        }
    }

    private static class TransformingVertexConsumer implements VertexConsumer
    {
        private final VertexConsumer parent;
        private final Matrix4f positionMatrix;
        private final Matrix3f normalMatrix;
        private final BlockPos offset;
        private final boolean injectOverlay;

        public TransformingVertexConsumer(VertexConsumer parent, PoseStack.Pose entry, BlockPos offset, boolean injectOverlay)
        {
            this.parent = parent;
            this.positionMatrix = new Matrix4f(entry.pose());
            this.normalMatrix = new Matrix3f(entry.normal());
            this.offset = offset;
            this.injectOverlay = injectOverlay;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z)
        {
            float nx = x - this.offset.getX();
            float ny = y - this.offset.getY();
            float nz = z - this.offset.getZ();

            float tx = this.positionMatrix.m00() * nx + this.positionMatrix.m10() * ny + this.positionMatrix.m20() * nz + this.positionMatrix.m30();
            float ty = this.positionMatrix.m01() * nx + this.positionMatrix.m11() * ny + this.positionMatrix.m21() * nz + this.positionMatrix.m31();
            float tz = this.positionMatrix.m02() * nx + this.positionMatrix.m12() * ny + this.positionMatrix.m22() * nz + this.positionMatrix.m32();

            this.parent.addVertex(tx, ty, tz);
            return this;
        }

        @Override
        public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z)
        {
            Vector4f pos = new Vector4f(x, y, z, 1F);

            if (matrix != null)
            {
                matrix.transform(pos);
            }

            return this.addVertex(pos.x, pos.y, pos.z);
        }

        @Override
        public VertexConsumer setLineWidth(float width)
        {
            this.parent.setLineWidth(width);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color)
        {
            this.parent.setColor(color);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha)
        {
            this.parent.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setColor(float red, float green, float blue, float alpha)
        {
            this.parent.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v)
        {
            this.parent.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v)
        {
            this.parent.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v)
        {
            if (this.injectOverlay)
            {
                this.parent.setUv1(0, 10);
            }
            this.parent.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z)
        {
            float tx = this.normalMatrix.m00() * x + this.normalMatrix.m10() * y + this.normalMatrix.m20() * z;
            float ty = this.normalMatrix.m01() * x + this.normalMatrix.m11() * y + this.normalMatrix.m21() * z;
            float tz = this.normalMatrix.m02() * x + this.normalMatrix.m12() * y + this.normalMatrix.m22() * z;

            this.parent.setNormal(tx, ty, tz);
            return this;
        }
    }
}
