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
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

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
        context.batcher.getContext().draw();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
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

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(set, glowSettings, legacyGlow, this.form.paintSettings.get(), this.form.paintColor.get(), storedFormColor);
        }

        Color resolvedPaint = FormColorEffects.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorEffects.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        ModelTransformationMode mode = this.form.modelTransform.get();

        consumers.setSubstitute(this.getMainConsumer(set, resolvedPaint));
        consumers.setUI(true);
        this.renderItem(null, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, mode, false, null);
        consumers.draw();

        if (positivePaint)
        {
            this.submitDeferredItemPaintOverlay(null, matrices, resolvedPaint, set.a, OverlayTexture.DEFAULT_UV, mode, false, null, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity, true);
        }

        if (colorTransformWanted)
        {
            Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

            this.form.applyFormOpacity(overlayTint);
            this.renderItemColorTintOverlay(null, matrices, overlayTint, set.a, OverlayTexture.DEFAULT_UV, mode, false, null, true, storedFormColor);
        }

        if (glowIntensity > 0F && !glowSettings.resolvePaintOnly())
        {
            this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.DEFAULT_UV, true, mode, null, false);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);

        DiffuseLighting.disableGuiDepthLighting();

        matrices.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;
        boolean isDropped = context.type == FormRenderType.ITEM;
        boolean useDroppedMode = this.shouldUseDroppedMode(isDropped);
        ModelTransformationMode mode = this.getRenderMode(useDroppedMode);

        context.stack.push();

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

            Color storedFormColor = this.form.color.get();
            Color rawFormColor = storedFormColor.copyBakingColorGrade();
            Color formColor = rawFormColor.copy();
            boolean colorTransformWanted = FormColorEffects.wantsColorTintOverlay(storedFormColor);
            boolean colorGradeWanted = storedFormColor.hasColorAdjustments();

            BlockFormRenderer.color.set(context.color);

            if (FormColorEffects.shouldBakeFormColor(storedFormColor))
            {
                BlockFormRenderer.color.mul(rawFormColor);
            }

            this.form.applyFormOpacity(BlockFormRenderer.color);
            this.form.applyFormOpacity(formColor);

            boolean shadowPass = context.isShadowPass || BBSRendering.isIrisShadowPass();

            FormColorEffects.applyShadowPassColorFix(BlockFormRenderer.color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

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
                FormColorEffects.blendFormGlowBrighten(BlockFormRenderer.color, glowSettings, legacyGlow, this.form.paintSettings.get(), this.form.paintColor.get(), storedFormColor);
            }

            PaintSettings paintSettings = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color resolvedPaint = FormColorEffects.resolvePaintColor(paintSettings, legacyPaint);
            boolean positivePaint = !context.isPicking() && !shadowPass && FormColorEffects.hasPositivePaint(paintSettings, legacyPaint);

            consumers.setSubstitute(this.getMainConsumer(BlockFormRenderer.color, resolvedPaint));

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

            boolean leftHand = mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND;

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
                Color overlayTint = colorGradeWanted ? storedFormColor.copyDeferringColorGrade() : formColor;

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
            context.stack.pop();
        }

        RenderSystem.enableDepthTest();
    }

    boolean shouldUseDroppedMode(boolean isDropped)
    {
        return isDropped || this.form.sameAnimationWhenDropped.get();
    }

    ModelTransformationMode getRenderMode(boolean useDroppedMode)
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

            return ModelTransformationMode.GROUND;
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
        float bob = MathHelper.sin(age / 10F + uniqueOffset) * 0.1F + 0.1F;
        float angle = (age / 20F + uniqueOffset) * 57.295776F;

        context.stack.translate(0F, bob + 0.25F, 0F);
        context.stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
    }

    private float getDroppedUniqueOffset()
    {
        int hash = this.form.stack.get().hashCode();

        return (hash & 65535) / 65535F * 6.2831855F;
    }

    Function<VertexConsumer, VertexConsumer> getMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void submitDeferredItemColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, boolean ui, Color gradeSource)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(stack.peek().getNormalMatrix());
        Color formColorSnapshot = formColor.copy();
        Color gradeSnapshot = gradeSource == null ? null : gradeSource.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderItemColorTintOverlay(context, overlayStack, formColorSnapshot, alpha, overlay, mode, leftHand, itemEntity, ui, gradeSnapshot);
        });
    }

    private void renderItemColorTintOverlay(FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, boolean ui, Color gradeSource)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        this.renderItemColorTintOverlayPass(context, stack, consumers, formColor, alpha, overlay, ui, mode, leftHand, itemEntity, gradeSource);
    }

    private void renderItemColorTintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color formColor, float alpha, int overlay, boolean ui, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, Color gradeSource)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderState(formRootInverse, formColor.transform, false, formColor, 0.5F, gradeSource));

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, mode, leftHand, itemEntity);
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

    private void submitDeferredItemPaintOverlay(FormRenderingContext context, MatrixStack stack, Color resolvedPaint, float alpha, int overlay, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, boolean ui)
    {
        Matrix4f exactMvm = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f exactStack = new Matrix4f(stack.peek().getPositionMatrix());
        Matrix3f normalMatrix = new Matrix3f(stack.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(exactStack);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(exactMvm);
            RenderSystem.applyModelViewMatrix();

            try
            {
                this.renderPaintOverlayPass(context, overlayStack, overlayConsumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha);
            }
            finally
            {
                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        });
    }

    private void renderPaintOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform)
    {
        this.renderPaintOverlay(context, stack, consumers, resolvedPaint, alpha, overlay, ui, mode, leftHand, itemEntity, transform, null, null, 0F);
    }

    private void renderPaintOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, glowSettings, legacyGlow, glowIntensity, alpha);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform)
    {
        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, mode, leftHand, itemEntity, transform, null, null, 0F, 1F);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha)
    {
        Matrix4f formRootInverse = new Matrix4f(stack.peek().getPositionMatrix()).invert();

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderState(formRootInverse, transform, false, glowSettings, legacyGlow, glowIntensity, alpha, this.form.paintSettings.get(), this.form.paintColor.get(), this.form.getFormColor()));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        boolean wasOffset = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        if (wasOffset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));
        consumers.setUI(ui);

        try
        {
            this.renderItem(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, mode, leftHand, itemEntity);
            consumers.draw();
        }
        finally
        {
            if (wasOffset) GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            else GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

            GL11.glPolygonOffset(0F, 0F);
            
            consumers.setUI(false);
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderItem(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, ModelTransformationMode mode, boolean leftHand, LivingEntity itemEntity)
    {
        ItemStack itemStack = this.form.stack.get();
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel cachedModel = ItemBodyPartBatch.getCachedModel();

        if (cachedModel != null)
        {
            client.getItemRenderer().renderItem(itemStack, mode, leftHand, stack, consumers, light, overlay, cachedModel);

            return;
        }

        if (context == null || context.entity == null)
        {
            client.getItemRenderer().renderItem(itemStack, mode, light, overlay, stack, consumers, client.world, 0);
        }
        else
        {
            client.getItemRenderer().renderItem(itemEntity, itemStack, mode, leftHand, stack, consumers, context.entity.getWorld(), light, overlay, 0);
        }
    }

    private void renderGlowOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui, ModelTransformationMode mode, LivingEntity itemEntity, boolean leftHand)
    {
        int layers = FormColorEffects.resolveGlowOverlayLayers(glowIntensity);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        try
        {
            for (int i = 0; i < layers; i++)
            {
                Color glowColor = FormColorEffects.resolveGlowOverlayColor(glowSettings, legacyGlow, this.form.paintSettings.get(), this.form.paintColor.get(), this.form.getFormColor(), alpha, glowIntensity, layers);

                consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));
                this.renderItem(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, mode, leftHand, itemEntity);
                consumers.draw();
            }
        }
        finally
        {
            CustomVertexConsumerProvider.clearRunnables();
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }
}
