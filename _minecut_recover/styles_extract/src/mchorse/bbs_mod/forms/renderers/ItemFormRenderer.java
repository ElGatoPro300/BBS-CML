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
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.class_1087;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4608;
import net.minecraft.class_765;
import net.minecraft.class_7833;
import net.minecraft.class_811;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

import java.util.function.Function;

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
        context.batcher.getContext().method_51452();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        class_4587 matrices = context.batcher.getContext().method_51448();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.method_22903();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.method_22905(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        MatrixStackUtils.invertUiNormalY(matrices);

        Color storedFormColor = this.form.getFormColor();
        Color rawFormColor = storedFormColor.copyWithBlendIntensity();
        Color formColor = rawFormColor.copy();
        boolean colorTransformWanted = FormColorBlend.wantsColorTintOverlay(storedFormColor);
        boolean colorGradeWanted = storedFormColor.hasColorAdjustments();
        Color set = Color.white();

        if (FormColorBlend.shouldBakeFormColor(storedFormColor))
        {
            set.mul(rawFormColor);
        }

        this.form.applyFormOpacity(set);
        this.form.applyFormOpacity(formColor);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(set, glowSettings, legacyGlow);
        }

        Color resolvedPaint = FormColorBlend.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorBlend.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        class_811 mode = this.form.modelTransform.get();

        consumers.setSubstitute(this.getMainConsumer(set, resolvedPaint));
        consumers.setUI(true);
        this.renderItem(null, matrices, consumers, class_765.field_32769, class_4608.field_21444, mode, false, null);
        consumers.draw();

        if (positivePaint)
        {
            this.submitDeferredItemPaintOverlay(null, matrices, resolvedPaint, set.a, class_4608.field_21444, mode, false, null, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if (colorTransformWanted)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyWithBlendIntensityOnly() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderItemColorTintOverlay(null, matrices, overlayTint, set.a, class_4608.field_21444, mode, false, null, true, storedFormColor);
        }

        if (glowIntensity > 0F && !glowSettings.resolvePaintOnly())
        {
            this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, class_4608.field_21444, true, mode, null, false);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);

        class_308.method_24210();

        matrices.method_22909();
        BBSRendering.restoreGuiRenderState();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;
        boolean isDropped = context.type == FormRenderType.ITEM;
        boolean useDroppedMode = this.shouldUseDroppedMode(isDropped);
        class_811 mode = this.getRenderMode(useDroppedMode);

        context.stack.method_22903();

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
                        RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                    });

                    light = 0;
                }
                else
                {
                    CustomVertexConsumerProvider.hijackVertexFormat((l) ->
                    {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                    });
                }
            }
            else if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                });

                light = 0;
            }

            Color storedFormColor = this.form.getFormColor();
            Color rawFormColor = storedFormColor.copyWithBlendIntensity();
            Color formColor = rawFormColor.copy();
            boolean colorTransformWanted = FormColorBlend.wantsColorTintOverlay(storedFormColor);
            boolean colorGradeWanted = storedFormColor.hasColorAdjustments();

            BlockFormRenderer.color.set(context.color);

            if (FormColorBlend.shouldBakeFormColor(storedFormColor))
            {
                BlockFormRenderer.color.mul(rawFormColor);
            }

            this.form.applyFormOpacity(BlockFormRenderer.color);
            this.form.applyFormOpacity(formColor);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            FormColorBlend.applyShadowPassColorFix(BlockFormRenderer.color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

            if (BlockFormRenderer.color.a <= 0.001F && !shadowPass && !context.isPicking())
            {
                return;
            }

            GlowSettings glowSettings = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
            boolean positiveGlow = !context.isPicking() && !shadowPass && glowIntensity > 0F;

            if (glowIntensity < 0F)
            {
                FormColorBlend.blendFormGlowBrighten(BlockFormRenderer.color, glowSettings, legacyGlow);
            }

            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorBlend.resolvePaintColor(paintSettings, legacyPaint);
            boolean positivePaint = !context.isPicking() && !shadowPass && FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);

            consumers.setSubstitute(this.getMainConsumer(BlockFormRenderer.color, resolvedPaint));

            class_1799 itemStack = this.form.stack.get();
            double usingItemValue = this.form.usingItem.get();
            double itemUseTimeValue = this.form.itemUseTime.get();
            class_1309 itemEntity = null;

            if (usingItemValue > 0D || itemUseTimeValue > 0D)
            {
                StubEntity stub = new StubEntity(context.entity.getWorld());

                stub.setUsingItem(usingItemValue > 0D);
                stub.setItemUseTimeLeft((int) itemUseTimeValue);
                stub.setEquipmentStack(class_1304.field_6173, itemStack);
                itemEntity = ItemUseRenderState.prepareProxy(context.entity.getWorld(), stub, class_1304.field_6173, itemStack);
            }

            boolean leftHand = mode == class_811.field_4323;

            this.renderItem(context, context.stack, consumers, light, context.overlay, mode, leftHand, itemEntity);

            if (!deferFlush)
            {
                consumers.draw();
            }

            consumers.setSubstitute(null);

            if (positivePaint)
            {
                this.submitDeferredItemPaintOverlay(context, context.stack, resolvedPaint, BlockFormRenderer.color.a, context.overlay, mode, leftHand, itemEntity, paintSettings.transform, glowSettings, legacyGlow, glowIntensity, false);
            }

            if (colorTransformWanted && !shadowPass && !context.isPicking())
            {
                Color overlayTint = colorGradeWanted ? storedFormColor.copyWithBlendIntensityOnly() : formColor;

                this.form.applyFormOpacity(overlayTint);

                if (BBSRendering.isIrisWorldPaintDeferral())
                {
                    this.submitDeferredItemColorTintOverlay(context, context.stack, overlayTint, BlockFormRenderer.color.a, context.overlay, mode, leftHand, itemEntity, false, storedFormColor);
                }
                else
                {
                    this.renderItemColorTintOverlay(context, context.stack, overlayTint, BlockFormRenderer.color.a, context.overlay, mode, leftHand, itemEntity, false, storedFormColor);
                }
            }

            if (positiveGlow && !glowSettings.resolvePaintOnly())
            {
                this.renderGlowOverlay(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, BlockFormRenderer.color.a, context.overlay, false, mode, itemEntity, leftHand);
            }
            else if (!deferFlush)
            {
                CustomVertexConsumerProvider.clearRunnables();
            }

            RenderSystem.defaultBlendFunc();
        }
        finally
        {
            context.stack.method_22909();
        }

        RenderSystem.enableDepthTest();
    }

    boolean shouldUseDroppedMode(boolean isDropped)
    {
        return isDropped || this.form.sameAnimationWhenDropped.get();
    }

    class_811 getRenderMode(boolean useDroppedMode)
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

            return class_811.field_4318;
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
        float bob = class_3532.method_15374(age / 10F + uniqueOffset) * 0.1F + 0.1F;
        float angle = (age / 20F + uniqueOffset) * 57.295776F;

        context.stack.method_46416(0F, bob + 0.25F, 0F);
        context.stack.method_22907(class_7833.field_40716.rotationDegrees(angle));
    }

    private float getDroppedUniqueOffset()
    {
        int hash = this.form.stack.get().hashCode();

        return (hash & 65535) / 65535F * 6.2831855F;
    }

    Function<class_4588, class_4588> getMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void submitDeferredItemColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, float alpha, int overlay, class_811 mode, boolean leftHand, class_1309 itemEntity, boolean ui, Color gradeSource)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(stack.method_23760().method_23762());
        Color formColorSnapshot = formColor.copy();
        Color gradeSnapshot = gradeSource == null ? null : gradeSource.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            this.renderItemColorTintOverlay(context, overlayStack, formColorSnapshot, alpha, overlay, mode, leftHand, itemEntity, ui, gradeSnapshot);
        });
    }

    private void renderItemColorTintOverlay(FormRenderingContext context, class_4587 stack, Color formColor, float alpha, int overlay, class_811 mode, boolean leftHand, class_1309 itemEntity, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        this.renderItemColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, mode, leftHand, itemEntity, gradeSource);
    }

    private void renderItemColorTintOverlayPass(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, class_811 mode, boolean leftHand, class_1309 itemEntity, Color gradeSource)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, false, formColor, 0.5F, gradeSource));

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, class_765.field_32767, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
            consumers.setUI(false);
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void submitDeferredItemPaintOverlay(FormRenderingContext context, class_4587 stack, Color resolvedPaint, float alpha, int overlay, class_811 mode, boolean leftHand, class_1309 itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.method_23760().method_23761()));
        Matrix3f normalMatrix = new Matrix3f(stack.method_23760().method_23762());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            class_4587 overlayStack = new class_4587();

            overlayStack.method_23760().method_23761().set(positionMatrix);
            overlayStack.method_23760().method_23762().set(normalMatrix);

            this.renderPaintOverlayPass(context, overlayStack, overlayConsumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha);
        });
    }

    private void renderPaintOverlay(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, class_811 mode, boolean leftHand, class_1309 itemEntity, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, mode, leftHand, itemEntity, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, class_811 mode, boolean leftHand, class_1309 itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, class_811 mode, boolean leftHand, class_1309 itemEntity, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, null, null, 0F, 1F);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, class_811 mode, boolean leftHand, class_1309 itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.method_23760().method_23761()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, transform, false, glowSettings, legacyGlow, glowIntensity, alpha));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, class_765.field_32767, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
            consumers.setUI(false);
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderItem(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, int light, int overlay, class_811 mode, boolean leftHand, class_1309 itemEntity)
    {
        class_1799 itemStack = this.form.stack.get();
        class_310 client = class_310.method_1551();
        class_1087 cachedModel = ItemBodyPartBatch.getCachedModel();

        if (cachedModel != null)
        {
            client.method_1480().method_23179(itemStack, mode, leftHand, stack, consumers, light, overlay, cachedModel);

            return;
        }

        if (context == null || context.entity == null)
        {
            client.method_1480().method_23178(itemStack, mode, light, overlay, stack, consumers, client.field_1687, 0);
        }
        else
        {
            client.method_1480().method_23177(itemEntity, itemStack, mode, leftHand, stack, consumers, context.entity.getWorld(), light, overlay, 0);
        }
    }

    private void renderGlowOverlay(FormRenderingContext context, class_4587 stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui, class_811 mode, class_1309 itemEntity, boolean leftHand)
    {
        Color glowColor = FormColorBlend.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorBlend.resolveGlowOverlayShaderScale(glowIntensity);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));

        try
        {
            this.renderItem(context, stack, consumers, class_765.field_32767, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }
}
