package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.FormLightingRender;
import mchorse.bbs_mod.forms.renderers.utils.GlowEmissionVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.StructureData;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import mchorse.bbs_mod.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BlockFormRenderer extends FormRenderer<BlockForm>
{
    public static final Color color = new Color();

    private final Vector3f blockVisualMaskSize = new Vector3f(1F, 1F, 1F);

    /* Iris gbuffer bloom for entity-visual BER (signs, chests): vertex emission, not ColorModulator. */
    private Color blockMainPassGlowEmission;
    private VirtualBlockRenderView blockView;

    public BlockFormRenderer(BlockForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        PoseStack matrices = new PoseStack();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.pushPose();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color formColor = rawFormColor.copy();
        boolean colorGradeWanted = storedFormColor.hasColorAdjustments();
        Color set = Color.white();

        if (this.shouldBakeBlockFormColor(storedFormColor))
        {
            set.mul(rawFormColor);
        }

        this.form.applyFormOpacity(set);
        this.form.applyFormOpacity(formColor);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        Color resolvedPaint = FormColorEffects.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean blockEntityVisual = this.isBlockEntityVisual();
        EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
        boolean hasGlowTransform = glowTransform != null && glowTransform.isActive();
        boolean uiGlowMasked = glowIntensity < 0F && hasGlowTransform && !glowSettings.resolvePaintOnly();
        final Color uiNegativeGlowTint;

        /* Match world path: atlas negative glow via ColorModulator; BE via resolveBlockEntityColor.
         * Masked negative glow uses the overlay pass instead. */
        if (glowIntensity < 0F && !blockEntityVisual && !uiGlowMasked)
        {
            float factor = Math.max(0F, 1F + glowIntensity);

            uiNegativeGlowTint = new Color(factor, factor, factor, 1F);
        }
        else
        {
            uiNegativeGlowTint = null;
        }

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
        {
            this.applyBlockMainPassHijackLayer(layer, uiNegativeGlowTint);
        });

        BBSRendering.setupLevelLighting();

        Color mainPassPaint = FormColorEffects.defersNegativePaintToOverlay(this.form.paintSettings.get(), this.form.paintColor.get())
            ? null
            : resolvedPaint;

        consumers.setSubstitute(this.getBlockMainConsumer(set, mainPassPaint));
        consumers.setUI(true);
        this.renderRepeatedBlocks(null, matrices, consumers, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, false, true, false, false, false);

        consumers.draw();
        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        CustomVertexConsumerProvider.clearRunnables();

        boolean runPaintOverlay = FormColorEffects.wantsPaintOverlay(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean runColorTintOverlay = this.shouldRunBlockColorTintOverlay(blockEntityVisual, storedFormColor);
        boolean runGlowOverlay = this.shouldRunBlockGlowOverlay(
            (glowIntensity > 0F && !glowSettings.resolvePaintOnly()) || uiGlowMasked);

        if (runColorTintOverlay)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderBlockColorTintOverlay(null, matrices, overlayTint, set.a, OverlayTexture.NO_OVERLAY, true, storedFormColor);
        }

        if (runPaintOverlay)
        {
            this.submitDeferredBlockPaintOverlay(null, matrices, resolvedPaint, set.a, OverlayTexture.NO_OVERLAY, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if (runGlowOverlay)
        {
            this.renderGlowOverlayMasked(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.NO_OVERLAY, true, hasGlowTransform ? glowTransform : null);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);
        matrices.popPose();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        StructureLightSettings sl = this.form.structureLight.get();
        boolean lightsEnabled = (sl != null) ? sl.enabled : this.form.emitLight.get();
        int lightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();
        BlockState currentBlockState = this.form.blockState.get();
        int luminance = (lightsEnabled && currentBlockState != null) ? Math.min(currentBlockState.getLightEmission(), lightIntensity) : 0;

        if (luminance > 0 && !context.isPicking())
        {
            int blockLight = Math.max(LightTexture.block(light), luminance);
            int skyLight = LightTexture.sky(light);

            light = LightTexture.pack(blockLight, skyLight);
        }

        context.stack.pushPose();

        try
        {
            if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    BBSRendering.bindProgram(BBSShaders.getPickerModelsProgram());
                    BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
                    /* Unit pick cubes need both faces; culling clipped the volume to a flat slab. */
                    BBSRendering.disableCull();
                });

                light = 0;
                /* Form opacity / blend intensity must not discard pick pixels (picker_models a < 0.1). */
                consumers.setSubstitute(BBSRendering.getColorConsumer(new Color(1F, 1F, 1F, 1F)));
            }
            else
            {
                CustomVertexConsumerProvider.hijackVertexFormat((l) ->
                {
                    this.applyBlockMainPassHijackLayer(l, null);
                });
            }

            Color storedFormColor = this.form.color.get();
            Color rawFormColor = storedFormColor.copyBakingColorGrade();
            Color formColor = rawFormColor.copy();
            boolean colorGradeWanted = storedFormColor.hasColorAdjustments();

            color.set(context.color);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            if (shadowPass)
            {
                color.a *= storedFormColor.a;
            }
            else if (this.shouldBakeBlockFormColor(storedFormColor))
            {
                color.mul(rawFormColor);
            }

            this.form.applyFormOpacity(color);
            this.form.applyFormOpacity(formColor);

            FormColorEffects.applyShadowPassColorFix(color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

            if (color.a <= 0.001F && !shadowPass && !context.isPicking())
            {
                return;
            }

            GlowSettings glowSettings = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
            boolean positiveGlow = !context.isPicking() && !shadowPass && glowIntensity > 0F;
            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
            boolean positivePaint = !context.isPicking() && !shadowPass && FormColorEffects.hasPositivePaint(paintSettings, legacyPaint);
            /* Chests/beds/signs use entity textures — block atlas paint/tint overlays corrupt them.
             * Bake blend/paint/grade into ColorModulator tint instead (Iris: deferred redraw).
             * Spatial transform masks still use the block-atlas overlay pass (BE redraw skipped). */
            boolean blockEntityVisual = this.isBlockEntityVisual();
            EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
            boolean hasGlowTransform = glowTransform != null && glowTransform.isActive();
            boolean runPaintOverlay = !context.isPicking() && !shadowPass && FormColorEffects.wantsPaintOverlay(paintSettings, legacyPaint);
            boolean runColorTintOverlay = this.shouldRunBlockColorTintOverlay(blockEntityVisual, storedFormColor);
            boolean hasEmissiveGlow = positiveGlow && !glowSettings.resolvePaintOnly();
            boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
            final EffectTransform deferredGlowTransform = hasGlowTransform ? glowTransform.copy() : null;
            /* BlockForm atlas / cutout / BER paths often ignore or overwrite vertex RGB (esp.
             * non-cube models). Negative glow must use ColorModulator with and without shaders —
             * do not bake into `color` here (would double-darken when ColorModulator applies).
             * BE tint gets its own bake in resolveBlockEntityColor. Masked negative glow uses
             * the multiply darken overlay instead (same as ModelForm GlowEffect mask). */
            boolean negativeGlow = !context.isPicking() && !shadowPass && glowIntensity < 0F;
            boolean negativeGlowMasked = negativeGlow && hasGlowTransform && !glowSettings.resolvePaintOnly();
            boolean negativeGlowUniform = negativeGlow && !negativeGlowMasked;

            boolean localPreview = context.isLocalPreview();
            boolean noshadingDefer = !localPreview
                && !context.isPicking()
                && !shadowPass
                && BBSRendering.needsIrisNoshadingOpacityDeferral(color.a, this.form.noshadingOpacity.get());
            boolean softPostDeferred = !localPreview
                && !context.isPicking()
                && !shadowPass
                && ShaderOpacityPatch.shouldDelayUntilPostDeferred(color.a)
                && !noshadingDefer;
            boolean glowBakedInMainPass = irisWorldPaintDeferral && hasEmissiveGlow && !hasGlowTransform && !noshadingDefer;
            boolean runGlowOverlay = this.shouldRunBlockGlowOverlay(
                (positiveGlow && !glowSettings.resolvePaintOnly() && !glowBakedInMainPass) || negativeGlowMasked);
            final Color blockRecolorSource;

            if (glowBakedInMainPass)
            {
                blockRecolorSource = new Color(1F, 1F, 1F, color.a);
            }
            else
            {
                blockRecolorSource = color;
            }

            /* Negative paint with an active transform is owned by the overlay — do not bake. */
            Color mainPassPaint = FormColorEffects.defersNegativePaintToOverlay(paintSettings, legacyPaint)
                ? null
                : resolvedPaint;
            final Function<VertexConsumer, VertexConsumer> blockMainRecolor = this.getBlockMainConsumer(blockRecolorSource, mainPassPaint);
            final Color blockShaderTint;

            if (glowBakedInMainPass && BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
            {
                /* Match StructureForm / ItemForm: emission via ColorModulator on neutral white
                 * vertices during the Iris gbuffer pass (post-composite overlays never bloom).
                 * Entity-visual blocks (signs, chests, …) hit this through BER render layers +
                 * blockMainRecolor, not only renderBlockAsEntity quads. */
                blockShaderTint = new Color(1F, 1F, 1F, color.a);
                FormColorEffects.blendFormGlowBrighten(blockShaderTint, glowSettings, legacyGlow);
            }
            else if (negativeGlowUniform && !blockEntityVisual)
            {
                /* Atlas / cutout models: darken via ColorModulator (vertex RGB is unreliable on
                 * non-cube layers). Entity-visual BER uses resolveBlockEntityColor instead —
                 * sharing this factor would double-darken when BER also setShaderColor(beTint). */
                float factor = Math.max(0F, 1F + glowIntensity);

                blockShaderTint = new Color(factor, factor, factor, 1F);
            }
            else
            {
                blockShaderTint = null;
            }

            /* Positive Iris emission only — never treat negative ColorModulator as BE emission. */
            if (blockEntityVisual && glowBakedInMainPass && blockShaderTint != null)
            {
                this.blockMainPassGlowEmission = blockShaderTint.copy();
            }
            else
            {
                this.blockMainPassGlowEmission = null;
            }

            if (softPostDeferred || noshadingDefer)
            {
                boolean irisCamera = BBSRendering.isIrisWorldModelPass() && !noshadingDefer;
                Matrix4f positionMatrix = irisCamera
                    ? new Matrix4f(context.stack.last().pose())
                    : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.last().pose()));
                Matrix3f normalMatrix = new Matrix3f(context.stack.last().normal());
                Color colorSnapshot = blockRecolorSource.copy();
                Color blockShaderTintSnapshot = blockShaderTint == null ? null : blockShaderTint.copy();
                Color resolvedPaintSnapshot = resolvedPaint == null ? null : resolvedPaint.copy();
                int lightSnapshot = light;
                int overlaySnapshot = context.overlay;
                boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(color.a);
                boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(color.a);
                double formSortKey = this.computeBlockFormSortKey(context.stack.last().pose(), context);
                boolean positiveGlowSnapshot = runGlowOverlay;
                float glowIntensitySnapshot = glowIntensity;
                GlowSettings glowSettingsSnapshot = glowSettings;
                Color legacyGlowSnapshot = legacyGlow;
                boolean positivePaintSnapshot = runPaintOverlay;
                PaintSettings paintSettingsSnapshot = paintSettings == null ? null : paintSettings.copy();
                boolean colorTransformWantedSnapshot = runColorTintOverlay;
                Color storedFormColorSnapshot = storedFormColor == null ? null : storedFormColor.copy();
                Color formColorSnapshot = formColor.copy();
                boolean colorGradeWantedSnapshot = colorGradeWanted;

                boolean irisWorldPaintDeferralSnapshot = irisWorldPaintDeferral;

                Runnable deferredDraw = () ->
                {
                    PoseStack overlayStack = new PoseStack();

                    overlayStack.last().pose().set(positionMatrix);
                    overlayStack.last().normal().set(normalMatrix);

                    CustomVertexConsumerProvider deferredConsumers = FormUtilsClient.getProvider();

                    BBSRendering.enableDepthTest();
                    BBSRendering.depthMask(depthWrite);
                    ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        if (FormUtilsClient.isCrumblingLayer(layer))
                        {
                            return;
                        }

                        if (blockShaderTintSnapshot != null)
                        {
                            this.applyBlockMainPassHijackLayer(layer, blockShaderTintSnapshot);
                        }
                        else
                        {
                            BBSRendering.enableBlend();
                            BBSRendering.defaultBlendFunc();
                            /* Same as Structure soft: never leave a leftover ColorModulator. */
                            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                        }
                        BBSRendering.depthMask(depthWrite);
                        ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                    });

                    deferredConsumers.setSubstitute(this.getBlockMainConsumer(colorSnapshot, resolvedPaintSnapshot));

                    try
                    {
                        this.renderRepeatedBlocks(context, overlayStack, deferredConsumers, lightSnapshot, overlaySnapshot, false, false, false, false, false);
                        deferredConsumers.draw();
                    }
                    finally
                    {
                        if (blockShaderTintSnapshot != null)
                        {
                            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                        }

                        deferredConsumers.setSubstitute(null);
                        CustomVertexConsumerProvider.clearRunnables();
                    }

                    if (colorTransformWantedSnapshot && !irisWorldPaintDeferralSnapshot)
                    {
                        Color overlayTint = colorGradeWantedSnapshot ? storedFormColorSnapshot.copyDeferringColorGrade() : formColorSnapshot;

                        this.form.applyFormOpacity(overlayTint);
                        this.renderBlockColorTintOverlay(context, overlayStack, overlayTint, colorSnapshot.a, overlaySnapshot, false, storedFormColorSnapshot);
                    }

                    if (positivePaintSnapshot && !irisWorldPaintDeferralSnapshot)
                    {
                        this.renderPaintOverlay(context, overlayStack, deferredConsumers, resolvedPaintSnapshot, colorSnapshot.a, overlaySnapshot, false, paintSettingsSnapshot.transform, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot);
                    }

                    if (positiveGlowSnapshot && !irisWorldPaintDeferralSnapshot)
                    {
                        this.renderGlowOverlayMasked(context, overlayStack, deferredConsumers, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot, colorSnapshot.a, overlaySnapshot, false, deferredGlowTransform);
                    }

                    /* Soft flush isolation — glow leaves additive blend / depthMask false. */
                    BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                    BBSRendering.defaultBlendFunc();
                    CustomVertexConsumerProvider.clearRunnables();
                    BBSRendering.depthMask(depthWrite);
                    ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                };

                if (noshadingDefer)
                {
                    ModelVAORenderer.submitDeferredTranslucentModel(deferredDraw, depthWrite);
                }
                else if (irisCamera)
                {
                    ShaderOpacityPatch.submitPostDeferredForm(0D, formSortKey, depthWrite, afterFluids, deferredDraw);
                }
                else
                {
                    ShaderOpacityPatch.submitPostDeferredBbsForm(0D, formSortKey, depthWrite, afterFluids, deferredDraw);
                }
            }
            else
            {
                if (shadowPass)
                {
                    ShaderOpacityPatch.beginShadowForm();
                }

                if (!context.isPicking())
                {
                    consumers.setSubstitute(blockMainRecolor);
                }

                if (blockShaderTint != null)
                {
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        this.applyBlockMainPassHijackLayer(layer, blockShaderTint);
                        ShaderOpacityPatch.uploadShadowFormUniform();
                    });
                }
                else if (shadowPass)
                {
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        /* Recolor already has form opacity; leftover ColorModulator.a would square
                         * Bayer dither (same as soft Structure leaves / negative paint). */
                        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                        ShaderOpacityPatch.uploadShadowFormUniform();
                    });
                }

                try
                {
                    this.renderRepeatedBlocks(context, context.stack, consumers, light, context.overlay, context.isPicking(), false, false, false, false);
                    consumers.draw();
                }
                finally
                {
                    if (shadowPass)
                    {
                        ShaderOpacityPatch.endShadowForm();
                    }

                    if (blockShaderTint != null)
                    {
                        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                    }

                    consumers.setSubstitute(null);
                    CustomVertexConsumerProvider.clearRunnables();
                }
            }

            boolean submitIrisOverlays = irisWorldPaintDeferral && !noshadingDefer;

            if (((!softPostDeferred && !noshadingDefer) || (softPostDeferred && submitIrisOverlays)) && runColorTintOverlay && !shadowPass && !context.isPicking())
            {
                Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

                this.form.applyFormOpacity(overlayTint);

                if (BBSRendering.isIrisWorldPaintDeferral())
                {
                    this.submitDeferredBlockColorTintOverlay(context, context.stack, overlayTint, color.a, context.overlay, false, storedFormColor);
                }
                else if (!softPostDeferred)
                {
                    this.renderBlockColorTintOverlay(context, context.stack, overlayTint, color.a, context.overlay, false, storedFormColor);
                }
            }

            if ((!softPostDeferred && !noshadingDefer && runPaintOverlay) || (softPostDeferred && submitIrisOverlays && runPaintOverlay))
            {
                this.submitDeferredBlockPaintOverlay(context, context.stack, resolvedPaint, color.a, context.overlay, paintSettings.transform, glowSettings, legacyGlow, glowIntensity, false);
            }



            if ((!softPostDeferred && !noshadingDefer && runGlowOverlay) || (softPostDeferred && submitIrisOverlays && runGlowOverlay))
            {
                if (irisWorldPaintDeferral)
                {
                    this.submitDeferredBlockGlowOverlayMasked(context, context.stack, glowSettings, legacyGlow, glowIntensity, color.a, context.overlay, deferredGlowTransform);
                }
                else if (!softPostDeferred)
                {
                    this.renderGlowOverlayMasked(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, color.a, context.overlay, false, deferredGlowTransform);
                }
            }
            else if (!softPostDeferred && !noshadingDefer)
            {
                CustomVertexConsumerProvider.clearRunnables();
            }

            BBSRendering.defaultBlendFunc();
        }
        finally
        {
            this.blockMainPassGlowEmission = null;
            CustomVertexConsumerProvider.clearRunnables();
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);

            if (context.isPicking())
            {
                BBSRendering.enableCull();
            }

            context.stack.popPose();
        }

        BBSRendering.enableDepthTest();
    }

    private void applyBlockMainPassHijackLayer(RenderType layer, Color shaderTint)
    {
        if (FormUtilsClient.isCrumblingLayer(layer))
        {
            return;
        }

        BBSRendering.enableBlend();
        BBSRendering.defaultBlendFunc();

        if (shaderTint != null)
        {
            /* RGB carries Iris emission (glow bake) or negative-glow darken factor. Alpha must
             * stay 1 — soft/form opacity is already in the vertex recolor. Matching StructureForm
             * soft bloom; using shaderTint.a here squared opacity and caused a sudden darkening
             * when glow > 0. */
            BBSRendering.setShaderColor(shaderTint.r, shaderTint.g, shaderTint.b, 1F);
        }
        else
        {
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    private Function<VertexConsumer, VertexConsumer> getBlockMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void renderRepeatedBlocks(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay, boolean entityVisualOverlay)
    {
        int repeatX = this.form.repeatX.get();
        int repeatY = this.form.repeatY.get();
        int repeatZ = this.form.repeatZ.get();
        int startX = BlockForm.repeatAxisStart(repeatX, this.form.repeatCenterX.get());
        int startY = BlockForm.repeatAxisStart(repeatY, this.form.repeatCenterY.get());
        int startZ = BlockForm.repeatAxisStart(repeatZ, this.form.repeatCenterZ.get());

        for (int y = 0; y < repeatY; y++)
        {
            for (int z = 0; z < repeatZ; z++)
            {
                for (int x = 0; x < repeatX; x++)
                {
                    stack.pushPose();
                    stack.translate(startX + x, startY + y, startZ + z);

                    int blockLight = light;
                    BlockPos worldPos = null;

                    if (context != null)
                    {
                        worldPos = this.getRepeatBlockWorldPos(context, startX + x, startY + y, startZ + z);
                    }

                    if (!glowOverlay && context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    boolean coarsePick = picking && context != null && context.stencilMap != null && !context.stencilMap.increment;

                    this.renderSingleBlock(stack, consumers, blockLight, overlay, picking, coarsePick, ui, glowOverlay, paintOverlay, entityVisualOverlay, worldPos);
                    stack.popPose();
                }
            }
        }
    }

    /**
     * Samples world skylight/blocklight at each repeated block's world position.
     * Uses the entity/world matrix instead of the camera-relative render matrix.
     */
    private int resolveBlockLight(FormRenderingContext context, int localX, int localY, int localZ, int fallback)
    {
        StructureLightSettings sl = this.form.structureLight.get();
        boolean lightsEnabled = (sl != null) ? sl.enabled : this.form.emitLight.get();
        int lightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();
        BlockState blockState = this.form.blockState.get();
        int luminance = (lightsEnabled && blockState != null) ? Math.min(blockState.getLightEmission(), lightIntensity) : 0;

        if (this.form.repeatX.get() == 1 && this.form.repeatY.get() == 1 && this.form.repeatZ.get() == 1)
        {
            if (luminance > 0)
            {
                int blockLight = Math.max(LightTexture.block(fallback), luminance);
                int skyLight = LightTexture.sky(fallback);

                fallback = LightTexture.pack(blockLight, skyLight);
            }

            return fallback;
        }

        Level world = null;

        if (context.entity != null)
        {
            world = context.entity.getWorld();
        }

        if (world == null)
        {
            world = Minecraft.getInstance().level;
        }

        if (world == null)
        {
            if (luminance > 0)
            {
                int blockLight = Math.max(LightTexture.block(fallback), luminance);
                int skyLight = LightTexture.sky(fallback);

                fallback = LightTexture.pack(blockLight, skyLight);
            }

            return fallback;
        }

        BlockPos blockPos = this.getRepeatBlockWorldPos(context, localX, localY, localZ);

        if (blockPos == null)
        {
            if (luminance > 0)
            {
                int blockLight = Math.max(LightTexture.block(fallback), luminance);
                int skyLight = LightTexture.sky(fallback);

                fallback = LightTexture.pack(blockLight, skyLight);
            }

            return fallback;
        }

        int sampled = LevelRenderer.getLightCoords(world, blockPos);

        if (luminance > 0)
        {
            int blockLight = Math.max(LightTexture.block(sampled), luminance);
            int skyLight = LightTexture.sky(sampled);

            sampled = LightTexture.pack(blockLight, skyLight);
        }

        return FormLightingRender.apply(sampled, this.form.lightingSettings, this.form.lighting.get());
    }

    private BlockPos getRepeatBlockWorldPos(FormRenderingContext context, int localX, int localY, int localZ)
    {
        if (context.world != null)
        {
            PoseStack probe = new PoseStack();

            probe.last().pose().set(context.world.last().pose());
            probe.translate(localX, localY, localZ);

            Vector3f translation = probe.last().pose().getTranslation(new Vector3f());

            return BlockPos.containing(translation.x, translation.y + 0.5D, translation.z);
        }

        if (context.entity == null)
        {
            return null;
        }

        Transform transform = this.createTransform();
        Vector3f offset = transform.createMatrix().transformPosition(new Vector3f(localX + 0.5F, localY, localZ + 0.5F), new Vector3f());
        float transition = context.getTransition();
        double x = Lerps.lerp(context.entity.getPrevX(), context.entity.getX(), transition) + offset.x;
        double y = Lerps.lerp(context.entity.getPrevY(), context.entity.getY(), transition) + offset.y;
        double z = Lerps.lerp(context.entity.getPrevZ(), context.entity.getZ(), transition) + offset.z;

        return BlockPos.containing(x, y, z);
    }

    private void renderSingleBlock(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean coarsePick, boolean ui, boolean glowOverlay, boolean paintOverlay, boolean entityVisualOverlay, BlockPos worldPos)
    {
        stack.pushPose();
        stack.translate(-0.5F, 0F, -0.5F);

        /* UI preview uses fixed diffuse lights; world rendering relied on vanilla block lighting before repeat. */
        if (ui && !picking)
        {
            MatrixStackUtils.invertUiNormalY(stack);
        }

        /* Glass/ice etc. write depth in the entity pass and hide models behind the morph.
         * Terrain glass is drawn later in translucent; match that by not writing depth here.
         * Soft post-deferred already owns depth write — do not suppress it there. */
        boolean effectOverlay = paintOverlay || glowOverlay || entityVisualOverlay;
        boolean translucent = !picking && !effectOverlay && this.isTranslucentBlockState(this.form.blockState.get())
            && !ShaderOpacityPatch.isPostDeferredPhase();
        boolean savedDepthMask = false;

        if (translucent)
        {
            savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            BBSRendering.depthMask(false);
        }

        try
        {
            BlockState blockState = this.form.blockState.get();
            boolean stencilPickProxy = picking && this.needsStencilPickProxy(blockState);
            boolean coarseUnitPick = stencilPickProxy && coarsePick;
            boolean coarseThinPick = coarsePick && !stencilPickProxy && this.needsPickVolume(blockState);

            /* Entity-visual blocks skip BER during picking (wrong shader) and often have no baked
             * quads — draw picker_models proxy geometry instead. Alt-pick uses a unit cube; hover
             * uses the outline/model bounds so the highlight follows the sign shape. */
            if (coarseUnitPick || coarseThinPick)
            {
                this.renderPickVolume(stack, consumers, light, overlay);
            }
            else if (stencilPickProxy)
            {
                this.renderOutlinePickVolume(stack, consumers, blockState, light, overlay);
            }
            else
            {
                /* ENTITYBLOCK_ANIMATED (chests, ender chests, shulker boxes, …) delegates to
                 * BuiltinModelItemRenderer in renderBlockAsEntity, drawing a second duplicate item chest.
                 * Only invoke renderBlockModel when the block has a baked model. */
                if (blockState.getRenderShape() == RenderShape.MODEL)
                {
                    this.renderBlockModel(blockState, stack, consumers, light, overlay, worldPos);
                }

                boolean skipBlockEntity = effectOverlay && !entityVisualOverlay;

                if (!picking && !skipBlockEntity)
                {
                    this.renderBlockEntity(stack, consumers, light, overlay, false, effectOverlay);
                }

                int breakingLevel = this.form.breaking.get();

                if (!picking && !effectOverlay && breakingLevel > 0 && breakingLevel <= 10 && blockState.getRenderShape() == RenderShape.MODEL)
                {
                    RenderType crackingLayer = ModelBakery.DESTROY_TYPES.get(breakingLevel - 1);
                    VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
                    VertexConsumer crackingConsumer = new SheetedDecalTextureGenerator(delegateConsumer, stack.last(), 1.0F);
                    Function<VertexConsumer, VertexConsumer> previousSubstitute = consumers.getSubstitute();

                    consumers.setSubstitute((vertexConsumer) -> crackingConsumer);

                    try
                    {
                        this.renderBlockModel(this.form.blockState.get(), stack, consumers, light, overlay, worldPos);
                    }
                    finally
                    {
                        consumers.setSubstitute(previousSubstitute);
                    }
                }
            }
        }
        finally
        {
            if (translucent)
            {
                BBSRendering.depthMask(savedDepthMask);
            }
        }

        stack.popPose();
    }

    private void renderBlockModel(BlockState blockState, PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, BlockPos worldPos)
    {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);

        if (model == null)
        {
            return;
        }

        int tint = this.resolveBlockTint(blockState, worldPos);
        float r = (float) (tint >> 16 & 0xFF) / 255.0F;
        float g = (float) (tint >> 8 & 0xFF) / 255.0F;
        float b = (float) (tint & 0xFF) / 255.0F;

        VertexConsumer consumer = consumers.getBuffer(this.resolveBlockLayer(blockState));
        QuadInstance quadInstance = new QuadInstance();

        quadInstance.setColor(ARGB.colorFromFloat(1F, r, g, b));
        quadInstance.setLightCoords(light);
        quadInstance.setOverlayCoords(overlay);

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);

        for (BlockStateModelPart part : parts)
        {
            for (Direction direction : Direction.values())
            {
                for (BakedQuad quad : part.getQuads(direction))
                {
                    consumer.putBakedQuad(stack.last(), quad, quadInstance);
                }
            }

            for (BakedQuad quad : part.getQuads(null))
            {
                consumer.putBakedQuad(stack.last(), quad, quadInstance);
            }
        }
    }

    private RenderType resolveBlockLayer(BlockState state)
    {
        StructureData.syncFancyGraphicsFromOptions();
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);

        if (model != null && (model.materialFlags() & BakedQuad.FLAG_TRANSLUCENT) != 0)
        {
            return Sheets.translucentBlockSheet();
        }

        return Sheets.cutoutBlockSheet();
    }

    private int resolveBlockTint(BlockState state, BlockPos worldPos)
    {
        String biomeId = this.form.biomeId.get();
        boolean hasBiomeOverride = biomeId != null && !biomeId.isEmpty();

        if (hasBiomeOverride || Minecraft.getInstance().level != null)
        {
            if (this.blockView == null)
            {
                this.blockView = new VirtualBlockRenderView(new ArrayList<>());
            }

            this.blockView.setBiomeOverride(biomeId);

            if (worldPos != null)
            {
                this.blockView.setWorldAnchor(worldPos, 0, 0, 0);
            }
            else if (Minecraft.getInstance().player != null)
            {
                this.blockView.setWorldAnchor(Minecraft.getInstance().player.blockPosition(), 0, 0, 0);
            }
            else
            {
                this.blockView.setWorldAnchor(BlockPos.ZERO, 0, 0, 0);
            }

            BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);

            if (source != null)
            {
                return source.colorInWorld(state, this.blockView, BlockPos.ZERO);
            }

            return -1;
        }

        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);

        if (source != null)
        {
            return source.color(state);
        }

        return -1;
    }

    private boolean isTranslucentBlockState(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);

        return model != null && (model.materialFlags() & BakedQuad.FLAG_TRANSLUCENT) != 0;
    }

    /**
     * Soft-opacity queue key for the block form origin (farther first).
     */
    private double computeBlockFormSortKey(Matrix4f drawMatrix, FormRenderingContext context)
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

    /**
     * Signs, chests, beds, … — stencil picking skips BER (non-picker shaders) and baked block
     * meshes are often empty; draw a picker_models proxy instead.
     */
    private boolean needsStencilPickProxy(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        return state.getRenderShape() == RenderShape.INVISIBLE
            || state.getBlock() instanceof EntityBlock;
    }

    private boolean needsPickVolume(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        /* Signs / hanging signs / chests / beds / … — animated or invisible mesh, or any BE. */
        if (state.getRenderShape() == RenderShape.INVISIBLE
            || state.getBlock() instanceof EntityBlock)
        {
            return true;
        }

        try
        {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());

            if (shape.isEmpty())
            {
                return true;
            }

            AABB box = shape.bounds();

            /* Fences, panes, rods, chains, … — thin outline is nearly impossible to Alt-pick from the side. */
            return (box.maxX - box.minX) < 0.999D
                || (box.maxY - box.minY) < 0.999D
                || (box.maxZ - box.minZ) < 0.999D;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    /**
     * One solid unit cube for Alt-pick stencil — clean single hitbox for signs/chests/beds/….
     * Stack is already translated to block local space (-0.5, 0, -0.5).
     * UVs must sample an opaque atlas texel; UV 0–1 spans the whole atlas and picker_models
     * discards transparent samples, which left only a noisy flat square (and looked like
     * extra offset hitboxes from the side).
     */
    private void renderPickVolume(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
        BBSRendering.disableCull();

        VertexConsumer buffer = consumers.getBuffer(RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose entry = stack.last();
        Matrix4f matrix = entry.pose();
        float[] uv = this.getOpaquePickUv();

        this.emitPickCube(buffer, entry, matrix, 0F, 0F, 0F, 1F, 1F, 1F, uv[0], uv[1], light, overlay);
    }

    /**
     * Hover stencil proxy for entity-visual blocks — outline plus baked-model bounds so the
     * highlight wraps the sign board/post instead of a full 16×16 unit cube.
     */
    private void renderOutlinePickVolume(PoseStack stack, CustomVertexConsumerProvider consumers, BlockState state, int light, int overlay)
    {
        float x0 = 0F;
        float y0 = 0F;
        float z0 = 0F;
        float x1 = 1F;
        float y1 = 1F;
        float z1 = 1F;

        if (state != null)
        {
            try
            {
                VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());

                if (!shape.isEmpty())
                {
                    AABB box = shape.bounds();

                    x0 = (float) box.minX;
                    y0 = (float) box.minY;
                    z0 = (float) box.minZ;
                    x1 = (float) box.maxX;
                    y1 = (float) box.maxY;
                    z1 = (float) box.maxZ;
                }
            }
            catch (Exception ignored)
            {}

            Vector3f modelMin = new Vector3f();
            Vector3f modelMax = new Vector3f();

            if (this.sampleBlockModelBounds(state, modelMin, modelMax))
            {
                x0 = Math.min(x0, modelMin.x);
                y0 = Math.min(y0, modelMin.y);
                z0 = Math.min(z0, modelMin.z);
                x1 = Math.max(x1, modelMax.x);
                y1 = Math.max(y1, modelMax.y);
                z1 = Math.max(z1, modelMax.z);
            }
        }

        BBSRendering.bindTexture(TextureAtlas.LOCATION_BLOCKS);
        BBSRendering.disableCull();

        VertexConsumer buffer = consumers.getBuffer(RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose entry = stack.last();
        Matrix4f matrix = entry.pose();
        float[] uv = this.getOpaquePickUv();

        this.emitPickCube(buffer, entry, matrix, x0, y0, z0, x1, y1, z1, uv[0], uv[1], light, overlay);
    }

    private float[] getOpaquePickUv()
    {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
            .getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)
            .getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/white_concrete"));
        float u = (sprite.getU0() + sprite.getU1()) * 0.5F;
        float v = (sprite.getV0() + sprite.getV1()) * 0.5F;

        return new float[] {u, v};
    }

    private void emitPickCube(VertexConsumer buffer, PoseStack.Pose entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, int light, int overlay)
    {
        /* Front faces */
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0F, 0F, -1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0F, 0F, 1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 0F, -1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0F, 1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1F, 0F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1F, 0F, 0F, u, v, light, overlay);
        /* Back faces — entity solid layers may re-enable cull after hijack. */
        this.emitPickQuad(buffer, entry, matrix, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0F, 0F, 1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0F, 0F, -1F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0, 0F, 1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0F, -1F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, 1F, 0F, 0F, u, v, light, overlay);
        this.emitPickQuad(buffer, entry, matrix, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, -1F, 0F, 0F, u, v, light, overlay);
    }

    private void emitPickQuad(VertexConsumer buffer, PoseStack.Pose entry, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float u, float v, int light, int overlay)
    {
        buffer.addVertex(matrix, x0, y0, z0).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
        buffer.addVertex(matrix, x1, y1, z1).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
        buffer.addVertex(matrix, x2, y2, z2).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
        buffer.addVertex(matrix, x3, y3, z3).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz);
    }

    private boolean isBlockEntityVisual()
    {
        BlockState state = this.form.blockState.get();

        if (state == null)
        {
            return false;
        }

        return state.getBlock() instanceof EntityBlock
            || state.getRenderShape() == RenderShape.INVISIBLE;
    }

    /**
     * Solid blocks use the overlay when {@link FormColorEffects#wantsColorTintOverlay} is true.
     * Entity-visual blocks (signs, chests, …) use the spatial overlay for color transform at
     * scale 1 as well as 0.99 — only Color Grade alone stays on flat BE tint.
     */
    private boolean shouldRunBlockColorTintOverlay(boolean blockEntityVisual, Color storedFormColor)
    {
        if (!blockEntityVisual)
        {
            return FormColorEffects.wantsColorTintOverlay(storedFormColor);
        }

        return this.shouldRunBlockEntitySpatialColorOverlay(storedFormColor);
    }

    /**
     * Color tint / Color Grade overlay on entity-visual blocks (chests, beds, etc.).
     */
    private boolean shouldRunBlockEntitySpatialColorOverlay(Color storedFormColor)
    {
        if (storedFormColor == null)
        {
            return false;
        }

        return FormColorEffects.wantsColorTintOverlay(storedFormColor);
    }

    /**
     * Main-pass vertex bake — skipped on entity-visual blocks when the spatial overlay will
     * apply the same color (avoids double tint at scale 1).
     */
    private boolean shouldBakeBlockFormColor(Color storedFormColor)
    {
        if (!FormColorEffects.shouldBakeFormColor(storedFormColor))
        {
            return false;
        }

        if (this.isBlockEntityVisual() && this.shouldRunBlockEntitySpatialColorOverlay(storedFormColor))
        {
            return false;
        }

        return true;
    }

    private boolean shouldRunBlockPaintOverlay(boolean blockEntityVisual, PaintSettings paintSettings, boolean positivePaint)
    {
        return positivePaint;
    }

    private boolean shouldUseEntityVisualPaintOverlay()
    {
        return this.isBlockEntityVisual();
    }

    private boolean shouldRunBlockGlowOverlay(boolean positiveGlow)
    {
        return positiveGlow;
    }

    private boolean shouldUseEntityVisualGlowOverlay()
    {
        return this.isBlockEntityVisual();
    }

    private Color resolveBlockEntityColor()
    {
        Color storedFormColor = this.form.color.get();
        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color tint;

        if (this.shouldUseEntityVisualColorTintOverlay(storedFormColor))
        {
            tint = Color.white();
        }
        else
        {
            tint = storedFormColor == null ? Color.white() : storedFormColor.copyBakingColorGrade();
        }

        /* Uniform and masked paint on entity-visual blocks use the BER paint overlay pass
         * (block atlas overlays corrupt sign/chest atlases). Do not also bake paint into BE tint.
         * Negative paint with an active transform is owned by the multiply darken overlay. */
        if (!FormColorEffects.wantsPaintOverlay(paintSettings, legacyPaint)
            && paintSettings != null
            && paintSettings.resolveIntensity(legacyPaint) != 0F)
        {
            FormColorEffects.applyPaintBlend(tint, paintSettings, legacyPaint);
        }

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
        boolean glowMasked = glowTransform != null && glowTransform.isActive();

        /* Negative glow must land in BER ColorModulator / recolor — atlas vertex bake never
         * reaches sign/chest/bed meshes (non-cube entity visuals). Masked negative glow uses
         * the overlay instead so color transforms are respected. */
        if (glowIntensity < 0F && !glowMasked)
        {
            FormColorEffects.blendFormGlowBrighten(tint, glowSettings, legacyGlow);
        }

        this.form.applyFormOpacity(tint);

        return tint;
    }

    private boolean shouldUseEntityVisualColorTintOverlay(Color storedFormColor)
    {
        return this.isBlockEntityVisual() && this.shouldRunBlockEntitySpatialColorOverlay(storedFormColor);
    }

    /**
     * Block-local spans for entity-visual spatial masks when the transform is active (scale != 1).
     * At neutral scale the shader uses the inactive full-mask shortcut instead; these sizes apply
     * once scale deviates. Never below a full block so 1 → 0.99 does not jump to a tiny sign AABB.
     */
    private void resolveBlockFormMaskSize(Vector3f dest)
    {
        dest.set(1F, 1F, 1F);

        BlockState state = this.form.blockState.get();

        if (state == null)
        {
            return;
        }

        float sizeX = 1F;
        float sizeY = 1F;
        float sizeZ = 1F;

        try
        {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());

            if (!shape.isEmpty())
            {
                AABB box = shape.bounds();

                sizeX = (float) Math.max(box.maxX - box.minX, 0.001D);
                sizeZ = (float) Math.max(box.maxZ - box.minZ, 0.001D);
                sizeY = (float) Math.max(box.maxY, 0.001D);
            }
        }
        catch (Exception ignored)
        {}

        Vector3f modelMin = new Vector3f();
        Vector3f modelMax = new Vector3f();

        if (this.sampleBlockModelBounds(state, modelMin, modelMax))
        {
            sizeX = Math.max(sizeX, modelMax.x - modelMin.x);
            sizeY = Math.max(sizeY, modelMax.y - modelMin.y);
            sizeZ = Math.max(sizeZ, modelMax.z - modelMin.z);
        }

        dest.set(
            Math.max(sizeX, 1F),
            Math.max(sizeY, 1F),
            Math.max(sizeZ, 1F)
        );
    }

    /**
     * @return false when the baked model exposes no quads (caller keeps outline / unit fallback).
     */
    private boolean sampleBlockModelBounds(BlockState state, Vector3f min, Vector3f max)
    {
        min.set(1F, 1F, 1F);
        max.set(0F, 0F, 0F);

        Minecraft client = Minecraft.getInstance();
        BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);

        if (model == null)
        {
            return false;
        }

        boolean found = false;
        RandomSource random = RandomSource.create(42L);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);

        for (BlockStateModelPart part : parts)
        {
            for (Direction direction : Direction.values())
            {
                for (BakedQuad quad : part.getQuads(direction))
                {
                    found = this.expandBoundsFromQuad(quad, min, max) || found;
                }
            }

            for (BakedQuad quad : part.getQuads(null))
            {
                found = this.expandBoundsFromQuad(quad, min, max) || found;
            }
        }

        return found;
    }

    private boolean expandBoundsFromQuad(BakedQuad quad, Vector3f min, Vector3f max)
    {
        boolean found = false;

        for (int v = 0; v < 4; v++)
        {
            org.joml.Vector3fc pos = quad.position(v);

            min.x = Math.min(min.x, pos.x());
            min.y = Math.min(min.y, pos.y());
            min.z = Math.min(min.z, pos.z());
            max.x = Math.max(max.x, pos.x());
            max.y = Math.max(max.y, pos.y());
            max.z = Math.max(max.z, pos.z());
            found = true;
        }

        return found;
    }

    private void beginBlockVisualMaskSize(boolean enabled)
    {
        if (!enabled)
        {
            return;
        }

        this.resolveBlockFormMaskSize(this.blockVisualMaskSize);
        BlockEffectOverlayUniforms.setBlockVisualMaskSize(this.blockVisualMaskSize);
    }

    private void endBlockVisualMaskSize(boolean enabled)
    {
        if (!enabled)
        {
            return;
        }

        BlockEffectOverlayUniforms.clearBlockVisualMaskSize();
    }

    private boolean needsDeferredBlockEntityTint()
    {
        if (!this.isBlockEntityVisual() || !BBSRendering.isIrisWorldPaintDeferral())
        {
            return false;
        }

        Color beTint = this.resolveBlockEntityColor();

        return beTint.r < 0.999F || beTint.g < 0.999F || beTint.b < 0.999F || beTint.a < 0.999F;
    }

    private void submitDeferredBlockEntityTint(FormRenderingContext context, int overlay)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(context.stack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(context.stack.last().normal());

        ModelVAORenderer.submitVanillaPostComposite(() ->
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
            PoseStack overlayStack = new PoseStack();

            overlayStack.last().pose().set(exactStack);
            overlayStack.last().normal().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            MatrixStackUtils.applyModelViewMatrix();

            try
            {
                this.renderRepeatedBlockEntitiesTinted(context, overlayStack, consumers, LightTexture.FULL_BRIGHT, overlay);
                consumers.draw();
            }
            catch (Throwable ignored)
            {}
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
                consumers.setSubstitute(null);
                BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            }
        });
    }

    private void renderRepeatedBlockEntitiesTinted(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        int repeatX = this.form.repeatX.get();
        int repeatY = this.form.repeatY.get();
        int repeatZ = this.form.repeatZ.get();
        int startX = BlockForm.repeatAxisStart(repeatX, this.form.repeatCenterX.get());
        int startY = BlockForm.repeatAxisStart(repeatY, this.form.repeatCenterY.get());
        int startZ = BlockForm.repeatAxisStart(repeatZ, this.form.repeatCenterZ.get());

        for (int y = 0; y < repeatY; y++)
        {
            for (int z = 0; z < repeatZ; z++)
            {
                for (int x = 0; x < repeatX; x++)
                {
                    stack.pushPose();
                    stack.translate(startX + x, startY + y, startZ + z);
                    stack.translate(-0.5F, 0F, -0.5F);

                    int blockLight = light;

                    if (context != null)
                    {
                        blockLight = this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);
                    }

                    this.renderBlockEntity(stack, consumers, blockLight, overlay, true);
                    stack.popPose();
                }
            }
        }
    }

    private void renderBlockEntity(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean forceTint)
    {
        this.renderBlockEntity(stack, consumers, light, overlay, forceTint, false);
    }

    private void renderBlockEntity(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean forceTint, boolean effectOverlay)
    {
        if (!(this.form.blockState.get().getBlock() instanceof EntityBlock provider))
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        BlockEntity blockEntity = provider.newBlockEntity(BlockPos.ZERO, this.form.blockState.get());

        if (blockEntity == null)
        {
            return;
        }

        if (client.level != null)
        {
            blockEntity.setLevel(client.level);
        }

        BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
        BlockEntityRenderer<?, ?> renderer = dispatcher.getRenderer(blockEntity);

        if (renderer == null)
        {
            return;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        BlockEntityRenderer raw = (BlockEntityRenderer) renderer;
        BlockEntityRenderState state = raw.createRenderState();

        raw.extractRenderState(blockEntity, state, 0F, Vec3.ZERO, null);
        state.lightCoords = light;

        Color beTint = this.resolveBlockEntityColor();
        boolean applyTint = !effectOverlay;

        try
        {
            FeatureRenderDispatcher renderDispatcher = client.gameRenderer.getFeatureRenderDispatcher();
            CameraRenderState cameraRenderState = new CameraRenderState();

            raw.submit(state, stack, renderDispatcher.getSubmitNodeStorage(), cameraRenderState);

            if (applyTint)
            {
                BBSRendering.setShaderColor(beTint.r, beTint.g, beTint.b, beTint.a);
            }

            renderDispatcher.renderAllFeatures();
        }
        finally
        {
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    private void submitDeferredBlockColorTintOverlay(FormRenderingContext context, PoseStack stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(stack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(stack.last().normal());
        Color formColorSnapshot = formColor.copy();
        Color gradeSnapshot = gradeSource == null ? null : gradeSource.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            PoseStack overlayStack = new PoseStack();

            overlayStack.last().pose().set(exactStack);
            overlayStack.last().normal().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            MatrixStackUtils.applyModelViewMatrix();

            try
            {
                this.renderBlockColorTintOverlay(context, overlayStack, formColorSnapshot, alpha, overlay, ui, gradeSnapshot);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
            }
        });
    }

    private void renderBlockColorTintOverlay(FormRenderingContext context, PoseStack stack, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        boolean entityVisual = this.shouldUseEntityVisualColorTintOverlay(gradeSource != null ? gradeSource : formColor);

        this.renderColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, gradeSource, entityVisual);
    }

    private void renderColorTintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, Color gradeSource, boolean entityVisual)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        boolean savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) ->
        {
            if (entityVisual)
            {
                BlockEffectOverlayUniforms.configureColorTintOverlayRenderStateEntityVisual(formRootInverse, formColor.transform, true, formColor, 0.5F, gradeSource);
            }
            else
            {
                BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, true, formColor, 0.5F, gradeSource);
            }
        });

        BBSRendering.enableBlend();
        BBSRendering.enableDepthTest();
        BBSRendering.depthFunc(GL11.GL_LEQUAL);
        BBSRendering.depthMask(false);
        /* Pull tint overlay toward camera so it does not z-fight the main block pass. */
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -2F);

        /* Neutral vertices — lighting lives in the scene copy when grading. */
        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        this.beginBlockVisualMaskSize(entityVisual);

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, false, ui, false, !entityVisual, entityVisual);
            consumers.draw();
        }
        finally
        {
            this.endBlockVisualMaskSize(entityVisual);

            consumers.setSubstitute(null);
            BBSRendering.depthMask(savedDepthMask);
            BBSRendering.depthFunc(savedDepthFunc);
            GL11.glPolygonOffset(0F, 0F);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            if (savedCull)
            {
                BBSRendering.enableCull();
            }
            else
            {
                BBSRendering.disableCull();
            }

            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void submitDeferredBlockPaintOverlay(FormRenderingContext context, PoseStack stack, Color resolvedPaint, float alpha, int overlay, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(stack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(stack.last().normal());
        Color paintOverlay = this.resolvePaintOverlayDrawColor(resolvedPaint, alpha);
        boolean multiplyDarken = resolvedPaint != null && resolvedPaint.a < 0F;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            PoseStack overlayStack = new PoseStack();

            overlayStack.last().pose().set(exactStack);
            overlayStack.last().normal().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            MatrixStackUtils.applyModelViewMatrix();

            try
            {
                this.renderPaintOverlayPass(null, overlayStack, overlayConsumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha, multiplyDarken);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
            }
        });
    }

    /**
     * Positive paint: RGB + strength*formAlpha in {@code a}.
     * Negative paint: RGB = darken factor, {@code a} = form opacity (multiply-mask coverage).
     */
    private Color resolvePaintOverlayDrawColor(Color resolvedPaint, float alpha)
    {
        if (resolvedPaint == null)
        {
            return new Color(1F, 1F, 1F, alpha);
        }

        if (resolvedPaint.a < 0F)
        {
            float factor = Math.max(0F, 1F + resolvedPaint.a);

            return new Color(factor, factor, factor, alpha);
        }

        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        return paintOverlay;
    }

    private void renderPaintOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = this.resolvePaintOverlayDrawColor(resolvedPaint, alpha);
        boolean multiplyDarken = resolvedPaint != null && resolvedPaint.a < 0F;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha, multiplyDarken);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, null, null, 0F, 1F, false);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform, glowSettings, legacyGlow, glowIntensity, alpha, false);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, boolean multiplyDarken)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        boolean entityVisual = this.shouldUseEntityVisualPaintOverlay();
        EffectTransform paintTransform = transform;

        if (entityVisual && paintTransform != null && !paintTransform.isActive())
        {
            paintTransform = null;
        }

        final EffectTransform maskTransform = paintTransform;
        final boolean darken = multiplyDarken;
        boolean savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) ->
        {
            if (entityVisual)
            {
                BlockEffectOverlayUniforms.configurePaintOverlayRenderStateEntityVisual(formRootInverse, maskTransform, true, glowSettings, legacyGlow, glowIntensity, alpha, darken);
            }
            else
            {
                BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, maskTransform, true, glowSettings, legacyGlow, glowIntensity, alpha, 0.5F, true, darken);
            }
        });

        BBSRendering.enableBlend();

        if (darken)
        {
            BBSRendering.blendFuncSeparate(
                GL11.GL_DST_COLOR,
                GL11.GL_ZERO,
                GL11.GL_DST_ALPHA,
                GL11.GL_ZERO
            );
        }
        else
        {
            BBSRendering.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        BBSRendering.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        this.beginBlockVisualMaskSize(entityVisual);

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, false, ui, false, !entityVisual, entityVisual);
            consumers.draw();
        }
        finally
        {
            this.endBlockVisualMaskSize(entityVisual);

            consumers.setSubstitute(null);
            BBSRendering.depthMask(true);

            if (savedCull)
            {
                BBSRendering.enableCull();
            }
            else
            {
                BBSRendering.disableCull();
            }

            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderGlowOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui)
    {
        this.renderGlowOverlayMasked(context, stack, consumers, glowSettings, legacyGlow, glowIntensity, alpha, overlay, ui, null);
    }

    private void submitDeferredBlockGlowOverlayMasked(FormRenderingContext context, PoseStack stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, EffectTransform glowTransform)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(stack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(stack.last().normal());
        GlowSettings glowSnapshot = glowSettings.copy();
        Color legacyGlowSnapshot = legacyGlow == null ? null : legacyGlow.copy();
        EffectTransform glowTransformSnapshot = glowTransform == null ? null : glowTransform.copy();

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            PoseStack overlayStack = new PoseStack();

            overlayStack.last().pose().set(exactStack);
            overlayStack.last().normal().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            MatrixStackUtils.applyModelViewMatrix();

            try
            {
                this.renderGlowOverlayMasked(context, overlayStack, overlayConsumers, glowSnapshot, legacyGlowSnapshot, glowIntensity, alpha, overlay, false, glowTransformSnapshot);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
            }
        });
    }

    private void renderGlowOverlayMasked(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui, EffectTransform glowTransform)
    {
        if (glowIntensity < 0F)
        {
            /* Negative glow + spatial mask: multiply darken (ModelForm GlowEffect), not additive bloom. */
            float factor = Math.max(0F, 1F + glowIntensity);
            Color darken = new Color(factor, factor, factor, alpha);
            EffectTransform mask = glowTransform;

            if (mask == null)
            {
                mask = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
            }

            this.renderPaintOverlayPass(context, stack, consumers, darken, overlay, ui, mask, null, null, 0F, alpha, true);

            return;
        }

        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);
        EffectTransform resolvedTransform = glowTransform;

        if (resolvedTransform == null)
        {
            resolvedTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
        }

        final EffectTransform maskTransform = resolvedTransform;
        boolean entityVisual = this.shouldUseEntityVisualGlowOverlay();

        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        boolean savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) ->
        {
            if (entityVisual)
            {
                BlockEffectOverlayUniforms.configureGlowOverlayRenderStateEntityVisual(formRootInverse, maskTransform, true, 0.5F, shaderScale);
            }
            else
            {
                BlockEffectOverlayUniforms.configureGlowOverlayRenderState(formRootInverse, maskTransform, true, 0.5F, shaderScale);
            }

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        });

        BBSRendering.enableBlend();
        BBSRendering.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        BBSRendering.depthMask(false);

        boolean wasOffset = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        if (wasOffset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(glowColor));
        consumers.setUI(ui);

        this.beginBlockVisualMaskSize(entityVisual);

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, false, ui, true, false, entityVisual);
            consumers.draw();
        }
        finally
        {
            this.endBlockVisualMaskSize(entityVisual);

            if (wasOffset) GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            else GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

            GL11.glPolygonOffset(0F, 0F);

            consumers.setUI(false);
            consumers.setSubstitute(null);
            BBSRendering.depthMask(true);

            if (savedCull)
            {
                BBSRendering.enableCull();
            }
            else
            {
                BBSRendering.disableCull();
            }

            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }
}
