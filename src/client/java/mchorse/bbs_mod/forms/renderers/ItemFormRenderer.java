package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.ItemUseRenderState;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;

import org.lwjgl.opengl.GL11;

import java.util.function.Function;

import org.slf4j.Logger;

public class ItemFormRenderer extends FormRenderer<ItemForm>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemFormRenderer(ItemForm form)
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

        MatrixStackUtils.invertUiNormalY(matrices);

        Color storedFormColor = this.form.color.get();
        Color rawFormColor = storedFormColor.copyBakingColorGrade();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
        boolean colorGradeWanted = storedFormColor.hasColorAdjustments();
        Color set = Color.white();

        if (FormColorEffects.shouldBakeFormColor(storedFormColor))
        {
            set.mul(rawFormColor);
        }

        this.form.applyFormOpacity(set);
        this.form.applyFormOpacity(formColor);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
        boolean hasGlowTransform = glowTransform != null && glowTransform.isActive();
        boolean uiGlowMasked = FormColorEffects.wantsNegativeGlowOverlay(glowSettings, legacyGlow);

        if (glowIntensity < 0F && !uiGlowMasked)
        {
            FormColorEffects.blendFormGlowBrighten(set, glowSettings, legacyGlow);
        }

        Color resolvedPaint = FormColorEffects.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean runPaintOverlay = FormColorEffects.wantsPaintOverlay(this.form.paintSettings.get(), this.form.paintColor.get());
        Color mainPassPaint = FormColorEffects.defersNegativePaintToOverlay(this.form.paintSettings.get(), this.form.paintColor.get())
            ? null
            : resolvedPaint;

        BBSRendering.setupLevelLighting();

        ItemDisplayContext mode = this.form.modelTransform.get();

        consumers.setSubstitute(this.getMainConsumer(set, mainPassPaint));
        consumers.setUI(true);
        this.renderItem(null, matrices, consumers, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, mode, false, null);
        consumers.draw();

        if (colorTransformWanted)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderItemColorTintOverlay(null, matrices, overlayTint, set.a, OverlayTexture.NO_OVERLAY, mode, false, null, true, storedFormColor);
        }

        if (runPaintOverlay)
        {
            this.submitDeferredItemPaintOverlay(null, matrices, resolvedPaint, set.a, OverlayTexture.NO_OVERLAY, mode, false, null, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if ((glowIntensity > 0F && !glowSettings.resolvePaintOnly()) || uiGlowMasked)
        {
            if (hasGlowTransform || uiGlowMasked)
            {
                this.renderGlowOverlayMasked(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.NO_OVERLAY, true, mode, null, false, glowTransform);
            }
            else
            {
                this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.NO_OVERLAY, true, mode, null, false);
            }
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
        boolean isDropped = context.type == FormRenderType.ITEM;
        boolean useDroppedMode = this.shouldUseDroppedMode(isDropped);
        ItemDisplayContext mode = this.getRenderMode(useDroppedMode);

        context.stack.pushPose();

        try
        {
            this.applyDroppedAnimation(context, useDroppedMode);

            boolean deferFlush = ItemBodyPartBatch.isDeferringFlush();

            if (!deferFlush)
            {
                if (context.isPicking())
                {
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                        BBSRendering.bindProgram(BBSShaders.getPickerModelsProgram());
                    });

                    light = 0;
                }
                else
                {
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        this.applyItemMainPassHijackLayer(layer, null);
                    });
                }
            }
            else if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    BBSRendering.bindProgram(BBSShaders.getPickerModelsProgram());
                });

                light = 0;
            }

            Color storedFormColor = this.form.color.get();
            Color rawFormColor = storedFormColor.copyBakingColorGrade();
            Color formColor = rawFormColor.copy();
            boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
            boolean colorGradeWanted = storedFormColor.hasColorAdjustments();

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            if (shadowPass)
            {
                BlockFormRenderer.color.a *= storedFormColor.a;
            }
            else if (FormColorEffects.shouldBakeFormColor(storedFormColor))
            {
                BlockFormRenderer.color.mul(rawFormColor);
            }

            this.form.applyFormOpacity(BlockFormRenderer.color);
            this.form.applyFormOpacity(formColor);

            FormColorEffects.applyShadowPassColorFix(BlockFormRenderer.color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

            if (BlockFormRenderer.color.a <= 0.001F && !shadowPass && !context.isPicking())
            {
                return;
            }

            GlowSettings glowSettings = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
            boolean positiveGlow = !context.isPicking() && !shadowPass && glowIntensity > 0F;
            EffectTransform glowTransform = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
            boolean hasGlowTransform = glowTransform != null && glowTransform.isActive();
            boolean hasEmissiveGlow = positiveGlow && !glowSettings.resolvePaintOnly();
            boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
            final EffectTransform deferredGlowTransform = hasGlowTransform ? glowTransform.copy() : null;
            boolean negativeGlowMasked = !context.isPicking() && !shadowPass
                && FormColorEffects.wantsNegativeGlowOverlay(glowSettings, legacyGlow);

            if (glowIntensity < 0F && !negativeGlowMasked)
            {
                FormColorEffects.blendFormGlowBrighten(BlockFormRenderer.color, glowSettings, legacyGlow);
            }

            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
            boolean runPaintOverlay = !context.isPicking() && !shadowPass && FormColorEffects.wantsPaintOverlay(paintSettings, legacyPaint);

            ItemStack itemStack = this.form.stack.get();
            double usingItemValue = this.form.usingItem.get();
            double itemUseTimeValue = this.form.itemUseTime.get();
            LivingEntity itemEntity = null;

            if (usingItemValue > 0D || itemUseTimeValue > 0D)
            {
                StubEntity stub = new StubEntity(context.entity.getWorld());

                stub.setUsingItem(usingItemValue > 0D);
                stub.setItemUseTimeLeft((int) itemUseTimeValue);
                stub.setEquipmentStack(EquipmentSlot.MAINHAND, itemStack);
                itemEntity = ItemUseRenderState.prepareProxy(context.entity.getWorld(), stub, EquipmentSlot.MAINHAND, itemStack);
            }

            boolean leftHand = mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

            boolean localPreview = context.isLocalPreview();
            boolean noshadingDefer = !localPreview
                && !context.isPicking()
                && !shadowPass
                && BBSRendering.needsIrisNoshadingOpacityDeferral(BlockFormRenderer.color.a, this.form.noshadingOpacity.get());
            boolean softPostDeferred = !localPreview
                && !context.isPicking()
                && !shadowPass
                && ShaderOpacityPatch.shouldDelayUntilPostDeferred(BlockFormRenderer.color.a)
                && !noshadingDefer;
            boolean glowBakedInMainPass = irisWorldPaintDeferral && hasEmissiveGlow && !hasGlowTransform && !noshadingDefer;
            final Color itemRecolorSource;

            if (glowBakedInMainPass)
            {
                itemRecolorSource = new Color(1F, 1F, 1F, BlockFormRenderer.color.a);
            }
            else
            {
                itemRecolorSource = BlockFormRenderer.color;
            }

            final Function<VertexConsumer, VertexConsumer> itemMainRecolor = this.getMainConsumer(
                itemRecolorSource,
                FormColorEffects.defersNegativePaintToOverlay(paintSettings, legacyPaint) ? null : resolvedPaint);
            final Color itemShaderTint;

            if (glowBakedInMainPass && BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
            {
                /* Match Block/Structure forms: emission via ColorModulator, neutral vertex recolor. */
                itemShaderTint = new Color(1F, 1F, 1F, BlockFormRenderer.color.a);
                FormColorEffects.blendFormGlowBrighten(itemShaderTint, glowSettings, legacyGlow);
            }
            else
            {
                itemShaderTint = null;
            }

            if (softPostDeferred || noshadingDefer)
            {
                boolean irisCamera = BBSRendering.isIrisWorldModelPass() && !noshadingDefer;
                Matrix4f positionMatrix = irisCamera
                    ? new Matrix4f(context.stack.last().pose())
                    : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.last().pose()));
                Matrix3f normalMatrix = new Matrix3f(context.stack.last().normal());
                Color colorSnapshot = itemRecolorSource.copy();
                Color itemShaderTintSnapshot = itemShaderTint == null ? null : itemShaderTint.copy();
                Color resolvedPaintSnapshot = resolvedPaint == null ? null : resolvedPaint.copy();
                Color mainPaintSnapshot = FormColorEffects.defersNegativePaintToOverlay(paintSettings, legacyPaint)
                    ? null
                    : resolvedPaintSnapshot;
                int lightSnapshot = light;
                int overlaySnapshot = context.overlay;
                boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(BlockFormRenderer.color.a);
                boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(BlockFormRenderer.color.a);
                double formSortKey = this.computeItemFormSortKey(context.stack.last().pose(), context);
                boolean positiveGlowSnapshot = ((positiveGlow && !glowSettings.resolvePaintOnly()) || negativeGlowMasked) && !glowBakedInMainPass;
                float glowIntensitySnapshot = glowIntensity;
                GlowSettings glowSettingsSnapshot = glowSettings;
                Color legacyGlowSnapshot = legacyGlow;
                LivingEntity itemEntitySnapshot = itemEntity;
                ItemDisplayContext modeSnapshot = mode;
                boolean leftHandSnapshot = leftHand;
                boolean positivePaintSnapshot = runPaintOverlay;
                PaintSettings paintSettingsSnapshot = paintSettings == null ? null : paintSettings.copy();
                boolean colorTransformWantedSnapshot = colorTransformWanted;
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

                        if (itemShaderTintSnapshot != null)
                        {
                            this.applyItemMainPassHijackLayer(layer, itemShaderTintSnapshot);
                        }
                        else
                        {
                            BBSRendering.enableBlend();
                            BBSRendering.defaultBlendFunc();
                        }
                        BBSRendering.depthMask(depthWrite);
                        ShaderOpacityPatch.reassertPostDeferredDepthState(depthWrite);
                    });

                    deferredConsumers.setSubstitute(this.getMainConsumer(colorSnapshot, mainPaintSnapshot));

                    try
                    {
                        this.renderItem(context, overlayStack, deferredConsumers, lightSnapshot, overlaySnapshot, modeSnapshot, leftHandSnapshot, itemEntitySnapshot);
                        deferredConsumers.draw();
                    }
                    finally
                    {
                        if (itemShaderTintSnapshot != null)
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
                        this.renderItemColorTintOverlay(context, overlayStack, overlayTint, colorSnapshot.a, overlaySnapshot, modeSnapshot, leftHandSnapshot, itemEntitySnapshot, false, storedFormColorSnapshot);
                    }

                    if (positivePaintSnapshot && !irisWorldPaintDeferralSnapshot)
                    {
                        this.renderPaintOverlay(context, overlayStack, deferredConsumers, resolvedPaintSnapshot, colorSnapshot.a, overlaySnapshot, false, modeSnapshot, leftHandSnapshot, itemEntitySnapshot, paintSettingsSnapshot.transform, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot);
                    }

                    if (positiveGlowSnapshot && !irisWorldPaintDeferralSnapshot)
                    {
                        if (deferredGlowTransform != null)
                        {
                            this.renderGlowOverlayMasked(context, overlayStack, deferredConsumers, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot, colorSnapshot.a, overlaySnapshot, false, modeSnapshot, itemEntitySnapshot, leftHandSnapshot, deferredGlowTransform);
                        }
                        else
                        {
                            this.renderGlowOverlay(context, overlayStack, deferredConsumers, glowSettingsSnapshot, legacyGlowSnapshot, glowIntensitySnapshot, colorSnapshot.a, overlaySnapshot, false, modeSnapshot, itemEntitySnapshot, leftHandSnapshot);
                        }
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

                if (itemShaderTint != null)
                {
                    CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                    {
                        this.applyItemMainPassHijackLayer(layer, itemShaderTint);
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

                consumers.setSubstitute(itemMainRecolor);

                try
                {
                    this.renderItem(context, context.stack, consumers, light, context.overlay, mode, leftHand, itemEntity);

                    if (!deferFlush)
                    {
                        consumers.draw();
                    }
                }
                finally
                {
                    if (shadowPass)
                    {
                        ShaderOpacityPatch.endShadowForm();
                    }

                    if (itemShaderTint != null)
                    {
                        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                    }

                    consumers.setSubstitute(null);
                    CustomVertexConsumerProvider.clearRunnables();
                }
            }

            boolean submitIrisOverlays = irisWorldPaintDeferral && !noshadingDefer;

            if (((!softPostDeferred && !noshadingDefer) || (softPostDeferred && submitIrisOverlays)) && colorTransformWanted && !shadowPass && !context.isPicking())
            {
                Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

                this.form.applyFormOpacity(overlayTint);

                if (BBSRendering.isIrisWorldPaintDeferral())
                {
                    this.submitDeferredItemColorTintOverlay(context, context.stack, overlayTint, BlockFormRenderer.color.a, context.overlay, mode, leftHand, itemEntity, false, storedFormColor);
                }
                else if (!softPostDeferred)
                {
                    this.renderItemColorTintOverlay(context, context.stack, overlayTint, BlockFormRenderer.color.a, context.overlay, mode, leftHand, itemEntity, false, storedFormColor);
                }
            }

            if ((!softPostDeferred && !noshadingDefer && runPaintOverlay) || (softPostDeferred && submitIrisOverlays && runPaintOverlay))
            {
                this.submitDeferredItemPaintOverlay(context, context.stack, resolvedPaint, BlockFormRenderer.color.a, context.overlay, mode, leftHand, itemEntity, paintSettings.transform, glowSettings, legacyGlow, glowIntensity, false);
            }

            if (((!softPostDeferred && !noshadingDefer) || (softPostDeferred && submitIrisOverlays))
                && ((positiveGlow && !glowSettings.resolvePaintOnly()) || negativeGlowMasked)
                && !glowBakedInMainPass)
            {
                if (irisWorldPaintDeferral)
                {
                    this.submitDeferredItemGlowOverlayMasked(context, context.stack, glowSettings, legacyGlow, glowIntensity, BlockFormRenderer.color.a, context.overlay, false, mode, itemEntity, leftHand, deferredGlowTransform);
                }
                else if (!softPostDeferred)
                {
                    this.renderGlowOverlayMasked(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, BlockFormRenderer.color.a, context.overlay, false, mode, itemEntity, leftHand, deferredGlowTransform);
                }
            }
            else if (!deferFlush && !softPostDeferred && !noshadingDefer)
            {
                CustomVertexConsumerProvider.clearRunnables();
            }

            BBSRendering.defaultBlendFunc();
        }
        finally
        {
            context.stack.popPose();
        }

        BBSRendering.enableDepthTest();
    }

    boolean shouldUseDroppedMode(boolean isDropped)
    {
        return isDropped || this.form.sameAnimationWhenDropped.get();
    }

    ItemDisplayContext getRenderMode(boolean useDroppedMode)
    {
        if (useDroppedMode)
        {
            if (this.form.sameAnimationWhenDropped.get())
            {
                LOGGER.debug("Forced dropped animation for form {} using GROUND transform", this.form.getFormId());
            }
            else
            {
                LOGGER.debug("Dropped context for form {} using GROUND transform", this.form.getFormId());
            }

            return ItemDisplayContext.GROUND;
        }

        return this.form.modelTransform.get();
    }

    void applyDroppedAnimation(FormRenderingContext context, boolean useDroppedMode)
    {
        if (!useDroppedMode || context.entity == null || context.entity.getWorld() == null)
        {
            return;
        }

        float age = context.entity.getAge() + context.getTransition();
        float uniqueOffset = this.getDroppedUniqueOffset();
        float bob = Mth.sin(age / 10F + uniqueOffset) * 0.1F + 0.1F;
        float angle = (age / 20F + uniqueOffset) * 57.295776F;

        context.stack.translate(0F, bob + 0.25F, 0F);
        context.stack.mulPose(Axis.YP.rotationDegrees(angle));
    }

    private float getDroppedUniqueOffset()
    {
        int hash = this.form.stack.get().hashCode();

        return (hash & 65535) / 65535F * 6.2831855F;
    }

    /**
     * Soft-opacity queue key for the item form origin (farther first).
     */
    private double computeItemFormSortKey(Matrix4f drawMatrix, FormRenderingContext context)
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

    private void applyItemMainPassHijackLayer(RenderType layer, Color shaderTint)
    {
        BBSRendering.enableBlend();
        BBSRendering.defaultBlendFunc();

        if (shaderTint != null)
        {
            /* RGB carries Iris emission (glow bake). Alpha must stay 1 — soft/form opacity is
             * already in the white vertex recolor (same as BlockForm / StructureForm soft bloom). */
            BBSRendering.setShaderColor(shaderTint.r, shaderTint.g, shaderTint.b, 1F);
        }
        else
        {
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    Function<VertexConsumer, VertexConsumer> getMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void submitDeferredItemColorTintOverlay(FormRenderingContext context, PoseStack stack, Color formColor, float alpha, int overlay, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, boolean ui, Color gradeSource)
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
                this.renderItemColorTintOverlayPass(context, overlayStack, overlayConsumers, formColorSnapshot, alpha, overlay, ui, mode, leftHand, itemEntity, gradeSnapshot);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
            }
        });
    }

    private void renderItemColorTintOverlay(FormRenderingContext context, PoseStack stack, Color formColor, float alpha, int overlay, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        this.renderItemColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, mode, leftHand, itemEntity, gradeSource);
    }

    private void renderItemColorTintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, Color gradeSource)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        boolean savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> {
            BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, false, formColor, 0.5F, gradeSource);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        });

        BBSRendering.enableBlend();
        BBSRendering.depthMask(false);

        boolean wasOffset = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        if (wasOffset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
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

    private void submitDeferredItemPaintOverlay(FormRenderingContext context, PoseStack stack, Color resolvedPaint, float alpha, int overlay, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(stack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(stack.last().normal());
        Color paintOverlay = FormColorEffects.resolvePaintOverlayDrawColor(resolvedPaint, alpha);
        boolean multiplyDarken = resolvedPaint != null && resolvedPaint.a < 0F;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            PoseStack overlayStack = new PoseStack();

            overlayStack.last().pose().set(exactStack);
            overlayStack.last().normal().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);

            try
            {
                this.renderPaintOverlayPass(context, overlayStack, overlayConsumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha, multiplyDarken);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
            }
        });
    }

    private void renderPaintOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, mode, leftHand, itemEntity, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = FormColorEffects.resolvePaintOverlayDrawColor(resolvedPaint, alpha);
        boolean multiplyDarken = resolvedPaint != null && resolvedPaint.a < 0F;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha, multiplyDarken);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, null, null, 0F, 1F, false);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha, false);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, boolean multiplyDarken)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        boolean savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean darken = multiplyDarken;

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> {
            BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, transform, false, glowSettings, legacyGlow, glowIntensity, alpha, 0.5F, true, darken);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
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

        boolean wasOffset = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        if (wasOffset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
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

    private void renderItem(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, ItemDisplayContext mode, boolean leftHand, LivingEntity itemEntity)
    {
        ItemStack itemStack = this.form.stack.get();

        if (itemStack == null || itemStack.isEmpty())
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ItemModelResolver itemModelManager = client.getItemModelResolver();
        ItemStackRenderState itemRenderState = new ItemStackRenderState();

        if (itemEntity != null)
        {
            itemModelManager.updateForLiving(itemRenderState, itemStack, mode, itemEntity);
        }
        else
        {
            Level world = (context != null && context.entity != null) ? context.entity.getWorld() : client.level;
            itemModelManager.updateForTopItem(itemRenderState, itemStack, mode, world, null, 0);
        }

        FeatureRenderDispatcher dispatcher = client.gameRenderer.getFeatureRenderDispatcher();
        itemRenderState.submit(stack, dispatcher.getSubmitNodeStorage(), light, overlay, 0);
        dispatcher.renderAllFeatures();
        /* The dispatcher writes to vanilla buffers, not the BBS provider passed above.
         * Flush while the captured matrices and effect callback still belong to this item. */
        client.renderBuffers().bufferSource().endBatch();
    }

    private void renderGlowOverlay(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui, ItemDisplayContext mode, LivingEntity itemEntity, boolean leftHand)
    {
        this.renderGlowOverlayMasked(context, stack, consumers, glowSettings, legacyGlow, glowIntensity, alpha, overlay, ui, mode, itemEntity, leftHand, null);
    }

    private void submitDeferredItemGlowOverlayMasked(FormRenderingContext context, PoseStack stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui, ItemDisplayContext mode, LivingEntity itemEntity, boolean leftHand, EffectTransform glowTransform)
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
                this.renderGlowOverlayMasked(context, overlayStack, overlayConsumers, glowSnapshot, legacyGlowSnapshot, glowIntensity, alpha, overlay, ui, mode, itemEntity, leftHand, glowTransformSnapshot);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                MatrixStackUtils.applyModelViewMatrix();
            }
        });
    }

    private void renderGlowOverlayMasked(FormRenderingContext context, PoseStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui, ItemDisplayContext mode, LivingEntity itemEntity, boolean leftHand, EffectTransform glowTransform)
    {
        if (glowIntensity < 0F)
        {
            Color darken = FormColorEffects.resolveNegativeGlowDarkenOverlayColor(glowIntensity, alpha);
            EffectTransform mask = glowTransform;

            if (mask == null)
            {
                mask = FormColorEffects.resolveGlowEffectTransform(glowSettings, legacyGlow);
            }

            this.renderPaintOverlayPass(context, stack, consumers, darken, overlay, ui, mode, leftHand, itemEntity, mask, null, null, 0F, alpha, true);

            return;
        }

        Color resolvedGlow = new Color();
        glowSettings.resolveColor(legacyGlow, resolvedGlow);

        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);
        Color glowColor = new Color(
            resolvedGlow.r,
            resolvedGlow.g,
            resolvedGlow.b,
            alpha
        );

        Matrix4f formRootInverse = new Matrix4f(stack.last().pose()).invert();
        boolean savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) ->
        {
            BlockEffectOverlayUniforms.configureGlowOverlayRenderState(formRootInverse, glowTransform, false, 0.5F, shaderScale);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        });

        BBSRendering.enableBlend();
        BBSRendering.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        BBSRendering.depthMask(false);

        boolean wasOffset = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        if (wasOffset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(glowColor));
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, LightTexture.FULL_BRIGHT, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
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
