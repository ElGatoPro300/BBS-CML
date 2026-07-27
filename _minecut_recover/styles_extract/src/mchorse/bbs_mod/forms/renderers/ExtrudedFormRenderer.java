package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.film.FormRenderDepth;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.FormColorGradePatch;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4608;
import net.minecraft.class_5944;
import net.minecraft.class_757;
import net.minecraft.class_765;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class ExtrudedFormRenderer extends FormRenderer<ExtrudedForm>
{
    /* Milder than FlatPaintOverlayPass (-32): enough for self z-fight, not terrain punch-through. */
    private static final float EXTRUDED_PAINT_OFFSET_FACTOR = -1F;
    private static final float EXTRUDED_PAINT_OFFSET_UNITS = -4F;

    public ExtrudedFormRenderer(ExtrudedForm form)
    {
        super(form);
    }

    private void applyPBRTextureIntensity()
    {
        BBSRendering.setPBRTextureIntensity(this.form.pbrNormalIntensity.get(), this.form.pbrSpecularIntensity.get());
    }

    private void clearPBRTextureIntensity()
    {
        BBSRendering.clearPBRTextureIntensity();
    }

    private void bindFormTexture(Link texture)
    {
        this.applyPBRTextureIntensity();

        try
        {
            BBSModClient.getTextures().bindTexture(texture);
        }
        finally
        {
            this.clearPBRTextureIntensity();
        }
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        class_4587 stack = context.batcher.getContext().method_51448();

        stack.method_22903();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        this.applyTransforms(uiMatrix, context.getTransition());
        MatrixStackUtils.multiply(stack, uiMatrix);
        stack.method_46416(0F, 1F, 0F);
        stack.method_22905(1.5F, 1.5F, 4F);
        stack.method_22905(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        /* Shading fix */
        MatrixStackUtils.invertUiNormalY(stack);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        this.renderModel(BBSShaders::getModel,
            stack,
            class_4608.field_21444, class_765.field_32767, Colors.WHITE,
            context.getTransition(),
            null,
            true,
            false,
            null,
            null
        );
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        class_308.method_24210();

        stack.method_22909();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        boolean shading = this.form.shading.get();

        if (BBSRendering.isIrisShadersEnabled())
        {
            shading = true;
        }

        PaintSettings paint = this.form.paintSettings.get();
        float paintStrength = paint.resolveIntensity(this.form.paintColor.get());
        boolean irisWorldModelPass = BBSRendering.isIrisWorldModelPass();
        boolean hasColorGrade = this.form.getFormColor() != null && this.form.getFormColor().hasColorAdjustments();
        /* PositionTexColor has no PaintColor / FormColorGrade — keep BBS model.fsh when those run. */
        boolean useShadedFormat = shading
            || ((paintStrength != 0F || hasColorGrade) && !irisWorldModelPass);
        Supplier<class_5944> shader = this.getShader(context,
            useShadedFormat ? (irisWorldModelPass ? class_757::method_34508 : BBSShaders::getModel) : class_757::method_34543,
            shading ? BBSShaders::getPickerBillboardProgram : BBSShaders::getPickerBillboardNoShadingProgram
        );

        this.renderModel(shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking(), context.world, context);
    }

    private void renderModel(Supplier<class_5944> shader, class_4587 matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, class_4587 world, FormRenderingContext renderContext)
    {
        Link texture = this.form.texture.get();
        ModelVAO data = BBSModClient.getTextures().getExtruder().get(texture);

        if (data != null)
        {
            /* World/entity billboard: face the camera and ignore authored rotation.
             * Form/model editor preview (modelRenderer) must keep the real transform so
             * gizmo handles and General translate/rotate/scale fields match what you see. */
            if (this.form.billboard.get() && (renderContext == null || !renderContext.modelRenderer))
            {
                Matrix4f modelMatrix = matrices.method_23760().method_23761();
                Vector3f scale = new Vector3f();

                modelMatrix.getScale(scale);

                if (invertY)
                {
                    scale.y = -scale.y;
                }

                modelMatrix.m00(1).m01(0).m02(0);
                modelMatrix.m10(0).m11(1).m12(0);
                modelMatrix.m20(0).m21(0).m22(1);

                if (camera != null && !modelRenderer)
                {
                    modelMatrix.mul(camera.view);
                }

                modelMatrix.scale(scale);

                matrices.method_23760().method_23762().identity();

                if (camera != null && !modelRenderer)
                {
                    matrices.method_23760().method_23762().set(camera.view);
                }

                matrices.method_23760().method_23762().scale(
                    MatrixStackUtils.safeNormalScaleReciprocal(scale.x),
                    MatrixStackUtils.safeNormalScaleReciprocal(scale.y),
                    MatrixStackUtils.safeNormalScaleReciprocal(scale.z)
                );
            }

            Color color = Colors.COLOR.set(overlayColor, true);
            class_757 gameRenderer = class_310.method_1551().field_1773;
            Color storedFormColor = this.form.getFormColor();
            boolean shadowPass = BBSRendering.isIrisShadowPass();
            boolean ui = modelRenderer;

            this.form.applyFormOpacity(color);
            FormColorBlend.applyShadowPassColorFix(color, storedFormColor, this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

            if (color.a <= 0.001F && !shadowPass)
            {
                return;
            }

            GlowSettings glow = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            boolean hasGlow = glow.resolveIntensity(legacyGlow) != 0F;
            Color resolvedGlow = new Color();

            glow.resolveColor(legacyGlow, resolvedGlow);

            PaintSettings paint = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color paintColor = new Color();

            paint.resolveColor(legacyPaint, paintColor);

            float paintStrength = paint.resolveIntensity(legacyPaint);

            paintColor.a = paintStrength;

            boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
            boolean paintActive = paintStrength != 0F;
            boolean lowAlphaDefer = BBSRendering.needsIrisTranslucentModelDeferral(color.a);
            boolean noshadingOpacityDefer = BBSRendering.needsIrisNoshadingOpacityDeferral(color.a, this.form.noshadingOpacity.get());
            boolean hasColorAdjustments = storedFormColor != null && storedFormColor.hasColorAdjustments();
            /* Iris entity shaders have no PaintColor / FormColorGrade. Live paint/grade overlays
             * also fail LEQUAL on extruded slabs under pack depth. Redraw once post-composite
             * with BBS model.fsh (same as no-shader) so both effects work. Keep depth test so
             * buried faces stay occluded. */
            boolean forceIrisEffectDeferred = irisWorldPaintDeferral && !shadowPass && !ui
                && (paintActive || hasColorAdjustments);
            boolean deferTranslucentModel = lowAlphaDefer || noshadingOpacityDefer || forceIrisEffectDeferred;
            boolean deferPaintToOverlay = paintActive && irisWorldPaintDeferral && !deferTranslucentModel;
            Supplier<class_5944> renderShader = shader;
            boolean bbsModelShader = !BBSRendering.isIrisWorldModelPass() || deferTranslucentModel;
            /* No-shader / UI / Iris effect deferred: FormColorGrade in model.fsh. */
            boolean useFormColorGrade = hasColorAdjustments
                && !shadowPass
                && (!irisWorldPaintDeferral || deferTranslucentModel || ui);
            /* Complementary/BSL live Iris pass only when we are not forcing a BBS redraw. */
            boolean livePackGrade = hasColorAdjustments
                && irisWorldPaintDeferral
                && !shadowPass
                && !deferTranslucentModel
                && FormColorGradePatch.canUseLivePackGrade();
            /* Other Iris packs without force-deferred: scene-copy ColorGradeOverlay. */
            boolean useColorGradeOverlay = hasColorAdjustments
                && irisWorldPaintDeferral
                && !shadowPass
                && !deferTranslucentModel
                && !livePackGrade;
            boolean uploadGrade = useFormColorGrade || livePackGrade;
            Color formColor = (uploadGrade || useColorGradeOverlay)
                ? storedFormColor.copyWithBlendIntensityOnly()
                : storedFormColor.copyWithBlendIntensity();

            color.mul(formColor);

            boolean syncedGlow = hasGlow && glow.resolveSync();
            boolean shaderOverlay = irisWorldPaintDeferral && syncedGlow && !paintActive && !deferTranslucentModel;
            boolean deferGlowToOverlay = shaderOverlay;
            boolean paintOnlyGlow = glow.resolvePaintOnly();
            boolean stripMainPassGlow = deferGlowToOverlay || (deferPaintToOverlay && hasGlow && paintOnlyGlow);
            float gradeBrightnessSnapshot = storedFormColor.brightness;
            float gradeContrastSnapshot = storedFormColor.contrast;
            float gradeHueSnapshot = storedFormColor.hue;
            float gradeSaturationSnapshot = storedFormColor.saturation;

            if (!bbsModelShader && !shaderOverlay && !deferPaintToOverlay && !paintOnlyGlow && !deferTranslucentModel)
            {
                FormColorBlend.blendFormGlowBrighten(color, glow, legacyGlow);
            }

            Matrix4f formRootInverse = new Matrix4f();
            Vector3f paintMaskHalf = new Vector3f();

            EffectTransformMath.resolveBillboardMaskHalfExtents(paint.transform, paintMaskHalf);

            EffectTransform paintTransformSnapshot = paint.transform.copy();
            Vector3f paintMaskHalfSnapshot = new Vector3f(paintMaskHalf);

            if (paintActive && bbsModelShader)
            {
                ModelVAORenderer.setPaintEffectTransform(formRootInverse, paint.transform, paintMaskHalf, false);
            }

            /* Only upload grade on the live path — deferred callback re-sets its own snapshot. */
            if (uploadGrade && !deferTranslucentModel)
            {
                ModelVAORenderer.setFormColorGrade(gradeBrightnessSnapshot, gradeContrastSnapshot, gradeHueSnapshot, gradeSaturationSnapshot);
                ModelVAORenderer.setGradeEffectTransforms(storedFormColor);
            }
            else if (!deferTranslucentModel)
            {
                ModelVAORenderer.clearFormColorGrade();
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            gameRenderer.method_22974().method_3316();
            gameRenderer.method_22975().method_23209();

            if (deferTranslucentModel)
            {
                /* No Iris depth stamp — same as ModelForm: punching depth would erase entities behind. */
                ModelVAORenderer.setPaint(paintActive ? paintColor.r : 0F, paintActive ? paintColor.g : 0F, paintActive ? paintColor.b : 0F, paintActive ? paintStrength : 0F);

                if (hasGlow)
                {
                    ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
                }
                else
                {
                    ModelVAORenderer.clearGlowing();
                }

                Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.method_23760().method_23761()));
                Matrix3f normalMatrix = new Matrix3f(matrices.method_23760().method_23762());
                TextureBlend textureBlendSnapshot = this.form.textureBlend == null ? null : new TextureBlend(this.form.textureBlend.from, this.form.textureBlend.to, this.form.textureBlend.blend);
                boolean useShaderBlend = FormTextureBlendRenderer.isBlending(this.form.textureBlend);
                float ca = lowAlphaDefer
                    ? BBSRendering.easeDeferredModelAlpha(color.a)
                    : color.a;
                final float cr;
                final float cg;
                final float cb;

                if (lowAlphaDefer && !noshadingOpacityDefer)
                {
                    cr = 0F;
                    cg = 0F;
                    cb = 0F;
                }
                else
                {
                    cr = color.r;
                    cg = color.g;
                    cb = color.b;
                }
                int overlayLight = light;
                int overlayOverlay = overlay;
                boolean paintActiveSnapshot = paintActive;
                float pr = paintColor.r;
                float pg = paintColor.g;
                float pb = paintColor.b;
                float pa = paintStrength;

                /* Extruded has real thickness — keep depth test/write so terrain occludes
                 * buried faces. (Billboards use depthTest=false; that made paint show through grass.) */
                boolean deferredDepthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(ca);

                ModelVAORenderer.submitDeferredTranslucentModel(() ->
                {
                    try
                    {
                        if (paintActiveSnapshot)
                        {
                            ModelVAORenderer.setPaintEffectTransform(new Matrix4f().identity(), paintTransformSnapshot, paintMaskHalfSnapshot, false);
                            ModelVAORenderer.setPaint(pr, pg, pb, pa);
                        }
                        else
                        {
                            ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                        }

                        if (hasGlow)
                        {
                            ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
                        }
                        else
                        {
                            ModelVAORenderer.clearGlowing();
                        }

                        if (uploadGrade)
                        {
                            ModelVAORenderer.setFormColorGrade(gradeBrightnessSnapshot, gradeContrastSnapshot, gradeHueSnapshot, gradeSaturationSnapshot);
                            ModelVAORenderer.setGradeEffectTransforms(storedFormColor);
                        }

                        class_4587 overlayStack = new class_4587();

                        overlayStack.method_23760().method_23761().set(positionMatrix);
                        overlayStack.method_23760().method_23762().set(normalMatrix);

                        /* Full-mesh Iris effect redraw with BBS model.fsh (paint + FormColorGrade).
                         * Mild self-bias only — keep world depth so terrain still occludes. */
                        this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, texture, overlayStack, cr, cg, cb, ca, overlayLight, overlayOverlay, paintActiveSnapshot || uploadGrade);
                    }
                    finally
                    {
                        ModelVAORenderer.clearPaintEffectTransform();
                        ModelVAORenderer.clearPaint();
                        ModelVAORenderer.clearGlowing();
                        ModelVAORenderer.clearFormColorGrade();
                    }
                }, deferredDepthWrite, true);

                ModelVAORenderer.clearFormColorGrade();
            }
            else if (deferPaintToOverlay)
            {
                ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
            }
            else if (paintActive)
            {
                ModelVAORenderer.setPaint(paintColor.r, paintColor.g, paintColor.b, paintStrength);
            }
            else
            {
                ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
            }

            if (!deferTranslucentModel)
            {
            TextureBlend textureBlend = this.form.textureBlend;
            boolean useShaderBlend = bbsModelShader && FormTextureBlendRenderer.isBlending(textureBlend);
            TextureBlend textureBlendSnapshot = textureBlend == null ? null : new TextureBlend(textureBlend.from, textureBlend.to, textureBlend.blend);
            float opacityAlpha = color.a;

            if (ShaderOpacityPatch.shouldDelayUntilPostDeferred(opacityAlpha, false))
            {
                boolean irisCamera = BBSRendering.isIrisWorldModelPass() && !bbsModelShader;
                Matrix4f positionMatrix = irisCamera
                    ? new Matrix4f(matrices.method_23760().method_23761())
                    : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.method_23760().method_23761()));
                Matrix3f normalMatrix = new Matrix3f(matrices.method_23760().method_23762());
                Color colorSnapshot = color.copy();
                Color paintSnapshot = paintColor.copy();
                float paintStrengthSnapshot = paintStrength;
                boolean paintActiveSnapshot = paintActive;
                boolean stripGlowSnapshot = stripMainPassGlow;
                boolean hasGlowSnapshot = hasGlow;
                boolean paintOnlyGlowSnapshot = paintOnlyGlow;
                boolean deferPaintSnapshot = deferPaintToOverlay;
                boolean shaderOverlaySnapshot = shaderOverlay;
                GlowSettings glowSnapshot = glow.copy();
                Color resolvedGlowSnapshot = resolvedGlow.copy();
                Color legacyGlowSnapshot = legacyGlow.copy();
                Link textureSnapshot = texture;
                Supplier<class_5944> shaderSnapshot = irisCamera ? renderShader : BBSShaders::getModel;
                int overlayLight = light;
                int overlayOverlay = overlay;
                EffectTransform paintTransformQueued = paintTransformSnapshot;
                Vector3f paintMaskHalfQueued = paintMaskHalfSnapshot;
                double sortDepth = FormRenderDepth.resolveSortDepth(this.form, renderContext == null ? null : renderContext.renderDepthFrame);
                boolean depthWrite = ShaderOpacityPatch.shouldWriteDepthForOpacity(opacityAlpha);
                boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(opacityAlpha);
                boolean uploadGradeSnapshot = uploadGrade;
                Color gradeTransformsSnapshot = storedFormColor;
                Runnable deferredDraw = () ->
                {
                    class_4587 overlayStack = new class_4587();

                    overlayStack.method_23760().method_23761().set(positionMatrix);
                    overlayStack.method_23760().method_23762().set(normalMatrix);

                    try
                    {
                        if (paintActiveSnapshot && !deferPaintSnapshot)
                        {
                            ModelVAORenderer.setPaintEffectTransform(new Matrix4f().identity(), paintTransformQueued, paintMaskHalfQueued, false);
                            ModelVAORenderer.setPaint(paintSnapshot.r, paintSnapshot.g, paintSnapshot.b, paintStrengthSnapshot);
                        }
                        else
                        {
                            ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                        }

                        if (stripGlowSnapshot)
                        {
                            GlowSettings glowOff = glowSnapshot.copy();

                            glowOff.intensity = 0F;
                            ModelVAORenderer.setGlow(glowOff, resolvedGlowSnapshot.r, resolvedGlowSnapshot.g, resolvedGlowSnapshot.b, legacyGlowSnapshot);
                        }
                        else if (hasGlowSnapshot)
                        {
                            ModelVAORenderer.setGlow(glowSnapshot, resolvedGlowSnapshot.r, resolvedGlowSnapshot.g, resolvedGlowSnapshot.b, legacyGlowSnapshot);
                        }
                        else
                        {
                            ModelVAORenderer.clearGlowing();
                        }

                        if (uploadGradeSnapshot)
                        {
                            ModelVAORenderer.setFormColorGrade(gradeBrightnessSnapshot, gradeContrastSnapshot, gradeHueSnapshot, gradeSaturationSnapshot);
                            ModelVAORenderer.setGradeEffectTransforms(gradeTransformsSnapshot);
                        }

                        if (useShaderBlend)
                        {
                            Link fromTexture = FormTextureBlendRenderer.resolveFrom(textureBlendSnapshot, textureSnapshot);
                            Link toTexture = FormTextureBlendRenderer.resolveTo(textureBlendSnapshot, textureSnapshot);
                            ModelVAO fromData = BBSModClient.getTextures().getExtruder().get(fromTexture);

                            if (fromData != null)
                            {
                                ModelVAORenderer.setTextureBlend(toTexture, textureBlendSnapshot.blend);

                                try
                                {
                                    this.bindFormTexture(fromTexture);
                                    ModelVAORenderer.render(shaderSnapshot.get(), fromData, overlayStack, colorSnapshot.r, colorSnapshot.g, colorSnapshot.b, colorSnapshot.a, overlayLight, overlayOverlay);
                                }
                                finally
                                {
                                    ModelVAORenderer.clearTextureBlend();
                                }
                            }
                        }
                        else
                        {
                            FormTextureBlendRenderer.draw(textureBlendSnapshot, textureSnapshot, (link, alphaFactor) ->
                            {
                                ModelVAO passData = BBSModClient.getTextures().getExtruder().get(link);

                                if (passData == null)
                                {
                                    return;
                                }

                                Color passColor = colorSnapshot.copy();

                                passColor.a *= alphaFactor;
                                this.bindFormTexture(link);
                                ModelVAORenderer.render(shaderSnapshot.get(), passData, overlayStack, passColor.r, passColor.g, passColor.b, passColor.a, overlayLight, overlayOverlay);
                            });
                        }

                        if (deferPaintSnapshot)
                        {
                            if (hasGlowSnapshot)
                            {
                                ModelVAORenderer.setGlow(glowSnapshot, resolvedGlowSnapshot.r, resolvedGlowSnapshot.g, resolvedGlowSnapshot.b, legacyGlowSnapshot);
                            }
                            else
                            {
                                GlowSettings glowOff = glowSnapshot.copy();

                                glowOff.intensity = 0F;
                                ModelVAORenderer.setGlow(glowOff, resolvedGlowSnapshot.r, resolvedGlowSnapshot.g, resolvedGlowSnapshot.b, legacyGlowSnapshot);
                            }

                            /* Post-deferred path never entered beginPaintOverlayPass — without it
                             * PaintOverlay=0 and thin extruded slabs fail LEQUAL depth. */
                            ModelVAORenderer.beginPaintOverlayPass(false);

                            try
                            {
                                ModelVAORenderer.setPaint(paintSnapshot.r, paintSnapshot.g, paintSnapshot.b, paintStrengthSnapshot);
                                ModelVAORenderer.setPaintEffectTransform(new Matrix4f().identity(), paintTransformQueued, paintMaskHalfQueued, false);
                                this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, textureSnapshot, overlayStack, colorSnapshot.r, colorSnapshot.g, colorSnapshot.b, colorSnapshot.a, overlayLight, overlayOverlay, true);

                                if (hasGlowSnapshot && !paintOnlyGlowSnapshot)
                                {
                                    ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                                    ModelVAORenderer.setGlow(glowSnapshot, resolvedGlowSnapshot.r, resolvedGlowSnapshot.g, resolvedGlowSnapshot.b, legacyGlowSnapshot);
                                    RenderSystem.enableBlend();
                                    RenderSystem.depthMask(false);
                                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                                    try
                                    {
                                        this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, textureSnapshot, overlayStack, 0F, 0F, 0F, colorSnapshot.a, class_765.field_32767, overlayOverlay, true);
                                    }
                                    finally
                                    {
                                        RenderSystem.depthMask(true);
                                        RenderSystem.defaultBlendFunc();
                                    }
                                }
                            }
                            finally
                            {
                                ModelVAORenderer.endPaintOverlayPass();
                            }
                        }
                        else if (shaderOverlaySnapshot)
                        {
                            ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                            ModelVAORenderer.setGlow(glowSnapshot, resolvedGlowSnapshot.r, resolvedGlowSnapshot.g, resolvedGlowSnapshot.b, legacyGlowSnapshot);
                            RenderSystem.enableBlend();
                            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                            try
                            {
                                this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, textureSnapshot, overlayStack, 0F, 0F, 0F, colorSnapshot.a, class_765.field_32767, overlayOverlay, true);
                            }
                            finally
                            {
                                RenderSystem.defaultBlendFunc();
                            }
                        }
                    }
                    finally
                    {
                        ModelVAORenderer.clearPaintEffectTransform();
                        ModelVAORenderer.clearPaint();
                        ModelVAORenderer.clearGlowing();
                        ModelVAORenderer.clearTextureBlend();
                        ModelVAORenderer.clearFormColorGrade();
                    }
                };

                if (irisCamera)
                {
                    ShaderOpacityPatch.submitPostDeferredForm(sortDepth, 0D, depthWrite, afterFluids, deferredDraw);
                }
                else
                {
                    ShaderOpacityPatch.submitPostDeferredBbsForm(sortDepth, 0D, depthWrite, afterFluids, deferredDraw);
                }

                ModelVAORenderer.clearPaintEffectTransform();
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
                ModelVAORenderer.clearFormColorGrade();
            }
            else
            {
            boolean forceDepth = ShaderOpacityPatch.shouldForceLiveDepthWrite(opacityAlpha);
            boolean suppressDepth = ShaderOpacityPatch.shouldSuppressDepthWrite(opacityAlpha);
            boolean savedDepthMask = false;

            if (forceDepth || suppressDepth)
            {
                savedDepthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
                RenderSystem.enableDepthTest();

                if (forceDepth)
                {
                    ShaderOpacityPatch.setForceLiveDepthWrite(true);
                    RenderSystem.depthMask(true);
                }
                else
                {
                    ShaderOpacityPatch.setSuppressLiveDepthWrite(true);
                    RenderSystem.depthMask(false);
                }
            }

            if (stripMainPassGlow)
            {
                GlowSettings glowOff = glow.copy();

                glowOff.intensity = 0F;
                ModelVAORenderer.setGlow(glowOff, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
            }
            else if (hasGlow)
            {
                ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
            }

            try
            {
                if (useShaderBlend)
                {
                    Link fromTexture = FormTextureBlendRenderer.resolveFrom(textureBlend, texture);
                    Link toTexture = FormTextureBlendRenderer.resolveTo(textureBlend, texture);
                    ModelVAO fromData = BBSModClient.getTextures().getExtruder().get(fromTexture);

                    if (fromData != null)
                    {
                        ModelVAORenderer.setTextureBlend(toTexture, textureBlend.blend);

                        try
                        {
                            this.bindFormTexture(fromTexture);
                            ModelVAORenderer.render(renderShader.get(), fromData, matrices, color.r, color.g, color.b, color.a, light, overlay);
                        }
                        finally
                        {
                            ModelVAORenderer.clearTextureBlend();
                        }
                    }
                }
                else
                {
                    FormTextureBlendRenderer.draw(textureBlend, texture, (link, alphaFactor) ->
                    {
                        ModelVAO passData = BBSModClient.getTextures().getExtruder().get(link);

                        if (passData == null)
                        {
                            return;
                        }

                        Color passColor = color.copy();

                        passColor.a *= alphaFactor;
                        this.bindFormTexture(link);
                        ModelVAORenderer.render(renderShader.get(), passData, matrices, passColor.r, passColor.g, passColor.b, passColor.a, light, overlay);
                    });
                }

                if (deferPaintToOverlay)
                {
                    Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.method_23760().method_23761()));
                    Matrix3f normalMatrix = new Matrix3f(matrices.method_23760().method_23762());
                    float cr = color.r;
                    float cg = color.g;
                    float cb = color.b;
                    float ca = color.a;
                    float pr = paintColor.r;
                    float pg = paintColor.g;
                    float pb = paintColor.b;
                    float pa = paintStrength;
                    int overlayLight = light;
                    int overlayOverlay = overlay;

                    ModelVAORenderer.submitPaintOverlay(false, () ->
                    {
                        /* Mild bias vs Iris-lit self surface — keep depth test so grass occludes. */
                        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                        GL11.glPolygonOffset(EXTRUDED_PAINT_OFFSET_FACTOR, EXTRUDED_PAINT_OFFSET_UNITS);

                        if (hasGlow)
                        {
                            ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
                        }
                        else
                        {
                            GlowSettings glowOff = glow.copy();

                            glowOff.intensity = 0F;
                            ModelVAORenderer.setGlow(glowOff, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
                        }

                        ModelVAORenderer.setPaint(pr, pg, pb, pa);

                        try
                        {
                            ModelVAORenderer.setPaintEffectTransform(new Matrix4f().identity(), paintTransformSnapshot, paintMaskHalfSnapshot, false);

                            class_4587 overlayStack = new class_4587();

                            overlayStack.method_23760().method_23761().set(positionMatrix);
                            overlayStack.method_23760().method_23762().set(normalMatrix);

                            this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, texture, overlayStack, cr, cg, cb, ca, overlayLight, overlayOverlay, true);

                            if (hasGlow && !paintOnlyGlow)
                            {
                                ModelVAORenderer.runWithPaintOverlayPass(false, () ->
                                {
                                    ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                                    ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);

                                    RenderSystem.enableBlend();
                                    RenderSystem.depthMask(false);
                                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                                    try
                                    {
                                        this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, texture, overlayStack, 0F, 0F, 0F, ca, class_765.field_32767, overlayOverlay, true);
                                    }
                                    finally
                                    {
                                        RenderSystem.depthMask(true);
                                        RenderSystem.defaultBlendFunc();
                                    }
                                });
                            }
                        }
                        finally
                        {
                            ModelVAORenderer.clearPaintEffectTransform();
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();
                        }
                    });
                }
                else if (shaderOverlay)
                {
                    Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.method_23760().method_23761()));
                    Matrix3f normalMatrix = new Matrix3f(matrices.method_23760().method_23762());
                    float cr = color.r;
                    float cg = color.g;
                    float cb = color.b;
                    float ca = color.a;
                    int overlayLight = light;
                    int overlayOverlay = overlay;

                    ModelVAORenderer.submitPaintOverlay(false, () ->
                    {
                        try
                        {
                            class_4587 overlayStack = new class_4587();

                            overlayStack.method_23760().method_23761().set(positionMatrix);
                            overlayStack.method_23760().method_23762().set(normalMatrix);

                            ModelVAORenderer.runWithPaintOverlayPass(false, () ->
                            {
                                ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                                ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);

                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                                try
                                {
                                    this.renderExtrudedOverlayPass(useShaderBlend, textureBlendSnapshot, texture, overlayStack, 0F, 0F, 0F, ca, class_765.field_32767, overlayOverlay, true);
                                }
                                finally
                                {
                                    RenderSystem.defaultBlendFunc();
                                }
                            });
                        }
                        finally
                        {
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();
                        }
                    });
                }

                if (useColorGradeOverlay)
                {
                    Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.method_23760().method_23761()));
                    Matrix3f normalMatrix = new Matrix3f(matrices.method_23760().method_23762());
                    Color colorSnapshot = color.copy();
                    TextureBlend textureBlendSnapshotFinal = textureBlendSnapshot;
                    boolean useShaderBlendFinal = useShaderBlend;
                    Link textureSnapshot = texture;
                    int overlayLight = light;
                    int overlayOverlay = overlay;

                    ModelVAORenderer.submitColorGradeOverlay(() ->
                    {
                        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                        GL11.glPolygonOffset(EXTRUDED_PAINT_OFFSET_FACTOR, EXTRUDED_PAINT_OFFSET_UNITS);

                        try
                        {
                            ModelVAORenderer.setFormColorGrade(gradeBrightnessSnapshot, gradeContrastSnapshot, gradeHueSnapshot, gradeSaturationSnapshot);
                            ModelVAORenderer.setGradeEffectTransforms(storedFormColor);
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();

                            class_4587 overlayStack = new class_4587();

                            overlayStack.method_23760().method_23761().set(positionMatrix);
                            overlayStack.method_23760().method_23762().set(normalMatrix);

                            this.renderExtrudedOverlayPass(useShaderBlendFinal, textureBlendSnapshotFinal, textureSnapshot, overlayStack, colorSnapshot.r, colorSnapshot.g, colorSnapshot.b, colorSnapshot.a, overlayLight, overlayOverlay, true);
                        }
                        finally
                        {
                            ModelVAORenderer.clearFormColorGrade();
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();
                        }
                    });
                }
            }
            finally
            {
                ModelVAORenderer.clearPaintEffectTransform();
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
                ModelVAORenderer.clearFormColorGrade();

                if (forceDepth)
                {
                    ShaderOpacityPatch.setForceLiveDepthWrite(false);
                    RenderSystem.depthMask(savedDepthMask);
                }
                else if (suppressDepth)
                {
                    ShaderOpacityPatch.setSuppressLiveDepthWrite(false);
                    RenderSystem.depthMask(savedDepthMask);
                }
            }
            }
            }

            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
        }
    }

    private void renderExtrudedOverlayPass(boolean useShaderBlend, TextureBlend textureBlendSnapshot, Link texture, class_4587 overlayStack, float cr, float cg, float cb, float ca, int overlayLight, int overlayOverlay, boolean depthBias)
    {
        /* Extruded slabs are ~1/16 thick. Mild bias beats self z-fight with the Iris-lit
         * surface; billboard-scale units (-32) pull fragments through nearby grass/terrain. */
        boolean savedPolygonOffsetFill = false;

        if (depthBias)
        {
            savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(EXTRUDED_PAINT_OFFSET_FACTOR, EXTRUDED_PAINT_OFFSET_UNITS);
        }

        try
        {
            if (useShaderBlend && textureBlendSnapshot != null)
            {
                Link fromTexture = FormTextureBlendRenderer.resolveFrom(textureBlendSnapshot, texture);
                Link toTexture = FormTextureBlendRenderer.resolveTo(textureBlendSnapshot, texture);
                ModelVAO fromData = BBSModClient.getTextures().getExtruder().get(fromTexture);

                if (fromData != null)
                {
                    ModelVAORenderer.setTextureBlend(toTexture, textureBlendSnapshot.blend);

                    try
                    {
                        this.bindFormTexture(fromTexture);
                        ModelVAORenderer.render(BBSShaders.getModel(), fromData, overlayStack, cr, cg, cb, ca, overlayLight, overlayOverlay);
                    }
                    finally
                    {
                        ModelVAORenderer.clearTextureBlend();
                    }
                }
            }
            else
            {
                FormTextureBlendRenderer.draw(textureBlendSnapshot, texture, (link, alphaFactor) ->
                {
                    ModelVAO passData = BBSModClient.getTextures().getExtruder().get(link);

                    if (passData == null)
                    {
                        return;
                    }

                    this.bindFormTexture(link);
                    ModelVAORenderer.render(BBSShaders.getModel(), passData, overlayStack, cr, cg, cb, ca * alphaFactor, overlayLight, overlayOverlay);
                });
            }
        }
        finally
        {
            if (depthBias)
            {
                if (ModelVAORenderer.isPaintOverlayPass() || ModelVAORenderer.isColorGradeOverlayPass())
                {
                    GL11.glPolygonOffset(EXTRUDED_PAINT_OFFSET_FACTOR, EXTRUDED_PAINT_OFFSET_UNITS);
                }
                else
                {
                    GL11.glPolygonOffset(0F, 0F);

                    if (!savedPolygonOffsetFill)
                    {
                        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                    }
                }
            }
        }
    }
}
