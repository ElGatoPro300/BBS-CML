package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.BillboardRenderLayers;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.forms.renderers.utils.ModelEffectPass;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.FormColorGradePatch;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;

import mchorse.bbs_mod.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class ExtrudedFormRenderer extends FormRenderer<ExtrudedForm>
{
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

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        PoseStack stack = new PoseStack();

        stack.pushPose();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        this.applyTransforms(uiMatrix, context.getTransition());
        MatrixStackUtils.multiply(stack, uiMatrix);
        stack.translate(0F, 1F, 0F);
        stack.scale(1.5F, 1.5F, 4F);
        stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        /* Shading fix */
        MatrixStackUtils.invertUiNormalY(stack);

        BBSRendering.setupLevelLighting();

        BBSRendering.depthFunc(GL11.GL_LEQUAL);

        GlProgram modelShader = BBSShaders.getModel();

        if (modelShader != null)
        {
            this.renderModel(() -> modelShader,
                stack,
                OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, Colors.WHITE,
                context.getTransition(),
                null,
                true,
                false,
                null,
                null
            );
        }

        BBSRendering.depthFunc(GL11.GL_ALWAYS);

        stack.popPose();
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
        boolean hasColorGrade = this.form.color.get() != null && this.form.color.get().hasColorAdjustments();
        /* PositionTexColor has no PaintColor / FormColorGrade — keep BBS model.fsh when those run. */
        boolean useShadedFormat = shading
            || ((paintStrength != 0F || hasColorGrade) && !irisWorldModelPass);
        Supplier<GlProgram> shader = this.getShader(context,
            useShadedFormat ? (irisWorldModelPass ? BBSRendering::getEntityTranslucentProgram : BBSShaders::getModel) : BBSRendering::getPositionTexColorProgram,
            shading ? BBSShaders::getPickerBillboardProgram : BBSShaders::getPickerBillboardNoShadingProgram
        );

        this.renderModel(shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking(), context.world, context);
    }

    private void renderModel(Supplier<GlProgram> shader, PoseStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, PoseStack world, FormRenderingContext renderContext)
    {
        Link texture = this.form.texture.get();
        ModelVAOData data = BBSModClient.getTextures().getExtruder().getMesh(texture);

        if (data != null)
        {
            /* World/entity billboard: face the camera and ignore authored rotation.
             * Form/model editor preview (modelRenderer) must keep the real transform so
             * gizmo handles and General translate/rotate/scale fields match what you see. */
            if (this.form.billboard.get() && (renderContext == null || !renderContext.modelRenderer))
            {
                Matrix4f modelMatrix = matrices.last().pose();
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

                matrices.last().normal().identity();

                if (camera != null && !modelRenderer)
                {
                    matrices.last().normal().set(camera.view);
                }

                matrices.last().normal().scale(
                    MatrixStackUtils.safeNormalScaleReciprocal(scale.x),
                    MatrixStackUtils.safeNormalScaleReciprocal(scale.y),
                    MatrixStackUtils.safeNormalScaleReciprocal(scale.z)
                );
            }

            Color storedFormColor = this.form.color.get();
            boolean hasColorGrade = storedFormColor != null && storedFormColor.hasColorAdjustments();
            boolean shadowPass = (renderContext != null && renderContext.isShadowPass) || BBSRendering.isIrisShadowPass();
            boolean localPreview = modelRenderer || (renderContext != null && renderContext.isLocalPreview());
            boolean irisWorld = BBSRendering.isIrisWorldModelPass() && !shadowPass && !localPreview;
            boolean useColorGradeOverlay = hasColorGrade && irisWorld;

            this.renderSurface(shader, matrices, overlay, light, overlayColor, invertY || modelRenderer, renderContext);

            if (useColorGradeOverlay)
            {
                Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.last().pose()));
                Matrix3f normalMatrix = new Matrix3f(matrices.last().normal());
                boolean previewFlag = invertY || modelRenderer;

                ModelVAORenderer.submitColorGradeOverlay(() ->
                {
                    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                    GL11.glPolygonOffset(0F, -64F);

                    try
                    {
                        PoseStack overlayStack = new PoseStack();

                        overlayStack.last().pose().set(positionMatrix);
                        overlayStack.last().normal().set(normalMatrix);

                        GlProgram gradeShader = BBSShaders.getModel();

                        this.renderSurface(() -> gradeShader, overlayStack, overlay, light, overlayColor, previewFlag, renderContext);
                    }
                    finally
                    {
                        GL11.glPolygonOffset(0F, 0F);
                        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                    }
                });
            }
        }
    }

    private void renderSurface(Supplier<GlProgram> shader, PoseStack matrices, int overlay, int light, int overlayColor, boolean preview, FormRenderingContext renderContext)
    {
        GlProgram program = shader != null ? shader.get() : null;
        boolean isEffectProgram = program != null && ModelEffectPass.isEffectProgram(program);
        Color storedFormColor = this.form.color.get();
        Color color = new Color().set(overlayColor, true);

        this.form.applyFormOpacity(color);

        if (color.a <= 0.001F)
        {
            return;
        }

        Color formColor = (isEffectProgram || ModelVAORenderer.isColorGradeOverlayPass())
            ? storedFormColor.copyDeferringColorGrade()
            : storedFormColor.copyBakingColorGrade();

        color.mul(formColor);

        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        float paintStrength = paintSettings.resolveIntensity(legacyPaint);

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        FormColorGradePatch.set(storedFormColor.brightness, storedFormColor.contrast, storedFormColor.hue, storedFormColor.saturation);

        if (isEffectProgram)
        {
            Color paintColor = new Color();

            paintSettings.resolveColor(legacyPaint, paintColor);
            ModelVAORenderer.setPaint(paintColor.r, paintColor.g, paintColor.b, paintStrength);

            Color glowColor = new Color();

            glowSettings.resolveColor(legacyGlow, glowColor);
            ModelVAORenderer.setGlow(glowSettings, glowColor.r, glowColor.g, glowColor.b, legacyGlow);

            ModelVAORenderer.setFormColorGrade(storedFormColor.brightness, storedFormColor.contrast, storedFormColor.hue, storedFormColor.saturation);
            ModelVAORenderer.setGradeEffectTransforms(storedFormColor);
            ModelVAORenderer.setupUniformsCpuPretransformed(program, new Matrix4f(matrices.last().pose()).invert());
        }
        else
        {
            if (paintStrength != 0F)
            {
                Color paintColor = new Color();

                paintSettings.resolveColor(legacyPaint, paintColor);
                FormColorEffects.applyPaintBlend(color, paintColor, paintStrength);
            }

            if (glowIntensity < 0F)
            {
                FormColorEffects.blendFormGlowBrighten(color, glowSettings, legacyGlow);
            }
        }

        /* Keep the CPU extrusion, including the side faces along opaque pixel edges.
         * Bake transforms into vertices; RenderLayer supplies the draw-time uniform buffers. */
        boolean shaded = this.form.shading.get();
        VertexFormat format = (shaded || isEffectProgram) ? DefaultVertexFormat.ENTITY : DefaultVertexFormat.POSITION_TEX_COLOR;
        PoseStack.Pose entry = matrices.last();
        Matrix4f position = entry.pose();

        this.applyPBRTextureIntensity();

        try
        {
            FormTextureBlendRenderer.draw(this.form.textureBlend, this.form.texture.get(), (link, alphaFactor) ->
            {
                ModelVAOData mesh = BBSModClient.getTextures().getExtruder().getMesh(link);
                Texture texture = BBSModClient.getTextures().getTexture(link);

                if (mesh == null || texture == null || mesh.vertices().length == 0)
                {
                    return;
                }

                float alpha = color.a * alphaFactor;
                float[] vertices = mesh.vertices();
                float[] normals = mesh.normals();
                float[] uvs = mesh.texCoords();
                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, format);

                for (int vertex = 0; vertex < vertices.length / 3; vertex++)
                {
                    int xyz = vertex * 3;
                    int uv = vertex * 2;
                    VertexConsumer consumer = builder.addVertex(position, vertices[xyz], vertices[xyz + 1], vertices[xyz + 2])
                        .setColor(color.r, color.g, color.b, alpha).setUv(uvs[uv], uvs[uv + 1]);

                    if (shaded || isEffectProgram)
                    {
                        consumer.setOverlay(overlay).setLight(light).setNormal(entry, normals[xyz], normals[xyz + 1], normals[xyz + 2]);
                    }
                }

                texture.bind(0);
                texture.setFilterMipmap(false, false);

                FormColorGradePatch.uploadToCurrentProgram();

                if (isEffectProgram)
                {
                    ModelEffectPass.draw(builder.buildOrThrow(), texture, program, renderContext != null && renderContext.isPicking(),
                        preview || alpha >= ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA, false, ModelVAORenderer.isColorGradeOverlayPass());
                }
                else
                {
                    BillboardRenderLayers.draw(builder.buildOrThrow(), texture, false, false,
                        preview || alpha >= ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA, false);
                }

                if (!isEffectProgram && glowIntensity > 0F && !glowSettings.resolvePaintOnly())
                {
                    Color resolvedGlow = new Color();

                    glowSettings.resolveColor(legacyGlow, resolvedGlow);

                    float glowAlpha = alpha * Math.min(1F, Math.abs(glowIntensity));
                    BufferBuilder glowBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);

                    for (int vertex = 0; vertex < vertices.length / 3; vertex++)
                    {
                        int xyz = vertex * 3;
                        int uv = vertex * 2;

                        glowBuilder.addVertex(position, vertices[xyz], vertices[xyz + 1], vertices[xyz + 2])
                            .setColor(resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, glowAlpha)
                            .setUv(uvs[uv], uvs[uv + 1]);
                    }

                    BillboardRenderLayers.draw(glowBuilder.buildOrThrow(), texture, false, false, false, false, true);
                }
            });
        }
        finally
        {
            this.clearPBRTextureIntensity();

            if (isEffectProgram)
            {
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
                ModelVAORenderer.clearFormColorGrade();
            }

            if (!ModelVAORenderer.isColorGradeOverlayPass())
            {
                FormColorGradePatch.clear();
            }
        }
    }
}
