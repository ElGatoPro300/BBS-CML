package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.film.FormRenderDepth;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FlatColorTintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FlatGlowOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class BillboardFormRenderer extends FormRenderer<BillboardForm>
{
    private static final Quad quad = new Quad();
    private static final Quad uvQuad = new Quad();

    private static final Matrix4f matrix = new Matrix4f();
    /* Base billboard faces sit slightly off the mid-plane so front/back do not z-fight. */
    private static final float FACE_Z_BIAS = 0.0005F;

    /* Paint/glow overlays sit further outward along each face normal so back faces are not
     * pushed through the base geometry (a shared +Z translate caused near-camera z-fighting). */
    private static final float PAINT_FACE_Z_BIAS = 0.0015F;
    private static final float PAINT_FACE_Z_BIAS_MAX = 0.08F;
    private static final float GLOW_FACE_Z_BIAS = 0.002F;

    /* Paint/glow sit just outside the camera-facing base face (not mid-plane, not ±dual).
     * Mid-plane lost depth to the nearer base face when close/angled; dual faces split the
     * silhouette. Camera-facing single plane + polygon offset stays in front from either side. */
    private static final float OVERLAY_FACE_EXTRA = 0.0015F;
    private static final Vector3f OVERLAY_TO_CAMERA = new Vector3f();
    private static final Vector3f OVERLAY_LOCAL_Z = new Vector3f();


    public BillboardFormRenderer(BillboardForm form)
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

    private void bindFormTexture(Texture texture)
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
        MatrixStack stack = context.batcher.getContext().getMatrices();

        stack.push();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        this.applyTransforms(uiMatrix, context.getTransition());
        MatrixStackUtils.multiply(stack, uiMatrix);
        stack.translate(0F, 1F, 0F);
        stack.scale(1.5F, 1.5F, 1.5F);
        stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        VertexFormat format = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

        this.renderModel(format, GameRenderer::getRenderTypeEntityTranslucentProgram,
            stack,
            OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, Colors.WHITE,
            context.getTransition(),
            null,
            true,
            false,
            null
        );

        DiffuseLighting.disableGuiDepthLighting();

        stack.pop();
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        /* Do not force shading under Iris — camera-facing normals + pack/BBS lighting make
         * the billboard pulse bright/dark when the orbit camera moves. Respect form.shading. */
        boolean shading = this.form.shading.get();

        VertexFormat format = shading ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR;
        Supplier<ShaderProgram> shader = this.getShader(context,
            shading ? GameRenderer::getRenderTypeEntityTranslucentProgram : GameRenderer::getPositionTexColorProgram,
            shading ? BBSShaders::getPickerBillboardProgram : BBSShaders::getPickerBillboardNoShadingProgram
        );

        this.renderModel(format, shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking(), context);
    }

    private void renderModel(VertexFormat format, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, FormRenderingContext deferContext)
    {
        Link defaultLink = this.form.texture.get();

        if (defaultLink == null)
        {
            return;
        }

        FormTextureBlendRenderer.draw(this.form.textureBlend, defaultLink, (link, alphaFactor) ->
        {
            Texture texture = BBSModClient.getTextures().getTexture(link);

            if (texture == null)
            {
                return;
            }

            this.renderModelPass(format, texture, shader, matrices, overlay, light, overlayColor, transition, camera, invertY, modelRenderer, alphaFactor, deferContext, link);
        });
    }

    private void renderModelPass(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, float alphaFactor, FormRenderingContext deferContext, Link textureLink)
    {
        float w = texture.width;
        float h = texture.height;
        float ow = w;
        float oh = h;

        /* TL = top left, BR = bottom right*/
        Vector4f crop = this.form.crop.get();
        float uvTLx = crop.x / w;
        float uvTLy = crop.y / h;
        float uvBRx = 1 - crop.z / w;
        float uvBRy = 1 - crop.w / h;

        uvQuad.p1.set(uvTLx, uvTLy, 0);
        uvQuad.p2.set(uvBRx, uvTLy, 0);
        uvQuad.p3.set(uvTLx, uvBRy, 0);
        uvQuad.p4.set(uvBRx, uvBRy, 0);

        float uvFinalTLx = uvTLx;
        float uvFinalTLy = uvTLy;
        float uvFinalBRx = uvBRx;
        float uvFinalBRy = uvBRy;

        if (this.form.resizeCrop.get())
        {
            uvFinalTLx = uvFinalTLy = 0F;
            uvFinalBRx = uvFinalBRy = 1F;

            w = w - crop.x - crop.z;
            h = h - crop.y - crop.w;
        }

        /* Calculate quad's size (vertices, not UV) */
        float ratioX = w > h ? h / w : 1F;
        float ratioY = h > w ? w / h : 1F;
        float TLx = (uvFinalTLx - 0.5F) * ratioY;
        float TLy = -(uvFinalTLy - 0.5F) * ratioX;
        float BRx = (uvFinalBRx - 0.5F) * ratioY;
        float BRy = -(uvFinalBRy - 0.5F) * ratioX;

        quad.p1.set(TLx, TLy, 0);
        quad.p2.set(BRx, TLy, 0);
        quad.p3.set(TLx, BRy, 0);
        quad.p4.set(BRx, BRy, 0);

        float offsetX = this.form.offsetX.get();
        float offsetY = this.form.offsetY.get();
        float rotation = this.form.rotation.get();

        if (offsetX != 0F || offsetY != 0F || rotation != 0F)
        {
            float centerX = (crop.x + (ow - crop.z)) / 2F / ow;
            float centerY = (crop.y + (oh - crop.w)) / 2F / ow;

            matrix.identity()
                .translate(centerX, centerY, 0)
                .rotateZ(MathUtils.toRad(rotation))
                .translate(offsetX / ow, offsetY / oh, 0)
                .translate(-centerX, -centerY, 0);

            uvQuad.transform(matrix);
        }

        this.renderQuad(format, texture, shader, matrices, overlay, light, overlayColor, transition, camera, invertY, modelRenderer, alphaFactor, deferContext, textureLink);
    }

    private void renderQuad(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, float alphaFactor, FormRenderingContext deferContext, Link textureLink)
    {
        Color storedFormColor = this.form.color.get();
        boolean hasColorAdjustments = storedFormColor != null && storedFormColor.hasColorAdjustments();
        boolean colorTransformWanted = FormColorBlend.wantsColorTransformMask(storedFormColor);
        Color color = new Color().set(overlayColor, true);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        boolean shadowPassEarly = BBSRendering.isIrisShadowPass()
            || (deferContext != null && deferContext.isShadowPass);
        boolean irisWorld = BBSRendering.isIrisWorldModelPass() && !shadowPassEarly && !modelRenderer;
        /* No-shader: FormColorGrade in model.fsh. Iris: deferred BBS redraw with FormColorGrade
         * (ColorGradeOverlay scene-replace makes thin billboards look invisible). */
        boolean useFormColorGrade = hasColorAdjustments && !irisWorld;
        boolean irisDeferredColorGrade = hasColorAdjustments && irisWorld;
        Color formColor = storedFormColor.copyWithBlendIntensityOnly().copy();

        /* Bake blend into vertices when FlatColorTint will not apply; grade stays in-shader / deferred. */
        if (colorTransformWanted)
        {
            color.r = 1F;
            color.g = 1F;
            color.b = 1F;
        }
        else if (useFormColorGrade || irisDeferredColorGrade)
        {
            color.mul(storedFormColor.copyWithBlendIntensityOnly());
        }
        else
        {
            color.mul(storedFormColor.copyWithBlendIntensity());
        }

        this.form.applyFormOpacity(color);
        this.form.applyFormOpacity(formColor);
        color.a *= alphaFactor;

        boolean shadowPass = shadowPassEarly;

        FormColorBlend.applyShadowPassColorFix(color, this.form.color.get(), this.form.paintSettings.get(), this.form.paintColor.get(), shadowPass);

        if (color.a <= 0.001F && !shadowPass)
        {
            return;
        }

        /* Main pass: negative paint only; positive paint is drawn in a separate overlay pass */
        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        float paintStrength = paintSettings.resolveIntensity(legacyPaint);

        if (paintStrength < 0F)
        {
            FormColorBlend.applyPaintBlend(color, paintSettings, legacyPaint);
        }

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        /* World/entity billboard: face the camera and ignore authored rotation.
         * Form/model editor preview (modelRenderer) must keep the real transform so
         * gizmo handles and General translate/rotate/scale fields match what you see. */
        if (this.form.billboard.get() && (deferContext == null || !deferContext.modelRenderer))
        {
            Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
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

            /* Keep identity normals. Baking camera.view into the normal matrix made Iris/BBS
             * lighting track the orbit camera and pulse the billboard bright/dark. */
            matrices.peek().getNormalMatrix().identity();
            matrices.peek().getNormalMatrix().scale(
                MatrixStackUtils.safeNormalScaleReciprocal(scale.x),
                MatrixStackUtils.safeNormalScaleReciprocal(scale.y),
                MatrixStackUtils.safeNormalScaleReciprocal(scale.z)
            );
        }

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)
        {
            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();
        }

        this.bindFormTexture(texture);
        RenderSystem.setShader(shader);

        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        RenderSystem.disableCull();

        /* Under Iris, billboards must defer to a BBS redraw — live entity_translucent often
         * washes or discards them. needsIrisTranslucentFlatDeferral skips fully opaque (#ff);
         * with the Complementary/BSL opacity patch that live opaque path also vanishes, so
         * defer every world billboard while the patch is active.
         * Color Grade: never use ColorGradeOverlay on billboards — scene capture misses the
         * thin plane and the overlay paints background (looks invisible). Defer + FormColorGrade. */
        boolean opacityPatch = ShaderOpacityPatch.isActive();
        /* Paint / Blend Color overlays must not write into the shadow map (same as Structure/Block). */
        boolean positivePaint = !shadowPass && FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);
        Color resolvedPaint = positivePaint ? FormColorBlend.resolvePaintColor(paintSettings, legacyPaint) : null;
        boolean applyColorTint = colorTransformWanted && !shadowPass;
        boolean deferForColorGrade = hasColorAdjustments && irisWorld;
        boolean deferTranslucent = !modelRenderer && !shadowPass
            && (BBSRendering.needsIrisTranslucentFlatDeferral(color.a)
                || (opacityPatch && BBSRendering.isIrisWorldModelPass())
                || deferForColorGrade);

        if (deferTranslucent)
        {
            Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrix));
            Color colorSnapshot = color.copy();
            Quad localQuad = new Quad();
            Quad localUvQuad = new Quad();

            localQuad.copy(quad);
            localUvQuad.copy(uvQuad);

            boolean linear = this.form.linear.get();
            boolean mipmap = this.form.mipmap.get();
            Link textureLinkSnapshot = textureLink;
            int overlaySnapshot = overlay;
            int lightSnapshot = light;
            float glowIntensitySnapshot = glowIntensity;
            GlowSettings glowSettingsSnapshot = glowSettings;
            Color legacyGlowSnapshot = legacyGlow;
            boolean emitGlowSnapshot = glowIntensity > 0F && !glowSettings.resolvePaintOnly();
            /* Noshading opacity: redraw after paint via BBS translucent queue, not Iris post-deferred. */
            boolean noshadingPaintPath = BBSRendering.needsIrisNoshadingOpacityDeferral(color.a, this.form.noshadingOpacity.get());
            boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(color.a);
            boolean depthWrite = afterFluids
                ? ShaderOpacityPatch.shouldWriteDepthForOpacity(color.a)
                : this.form.renderDepthEnabled.get();
            double sortDepth = FormRenderDepth.resolveSortDepth(this.form, deferContext == null ? null : deferContext.renderDepthFrame);
            double distanceSq = 0D;
            /* Iris deferred: apply FormColorGrade in model.fsh on the post-deferred BBS draw. */
            VertexFormat deferredFormat = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
            boolean gradeOnDeferredDraw = useFormColorGrade || irisDeferredColorGrade;
            Supplier<ShaderProgram> deferredShader = gradeOnDeferredDraw
                ? BBSShaders::getModel
                : GameRenderer::getRenderTypeEntityTranslucentProgram;
            float gradeBrightnessSnapshot = storedFormColor.brightness;
            float gradeContrastSnapshot = storedFormColor.contrast;
            float gradeHueSnapshot = storedFormColor.hue;
            float gradeSaturationSnapshot = storedFormColor.saturation;
            boolean gradeActiveSnapshot = gradeOnDeferredDraw;
            Color gradeSourceSnapshot = storedFormColor;

            if (deferContext != null && deferContext.entity != null && deferContext.camera != null)
            {
                distanceSq = FormRenderDepth.getEntityDistanceSq(deferContext.entity, deferContext.camera, transition);
            }

            Runnable deferredDraw = () ->
            {
                Texture deferredTexture = texture;

                if (textureLinkSnapshot != null)
                {
                    Texture linkedTexture = BBSModClient.getTextures().getTexture(textureLinkSnapshot);

                    if (linkedTexture != null)
                    {
                        deferredTexture = linkedTexture;
                    }
                }

                if (deferredTexture == null)
                {
                    return;
                }

                MatrixStack overlayStack = new MatrixStack();

                overlayStack.peek().getPositionMatrix().set(positionMatrix);
                overlayStack.peek().getNormalMatrix().identity();

                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();

                boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);

                try
                {
                    /* beginDeferredTranslucentModelPass enables cull; camera-facing quads then
                     * vanish. Match the no-shader live path: no cull, both faces, FormColorGrade. */
                    RenderSystem.disableCull();

                    if (gradeActiveSnapshot)
                    {
                        ModelVAORenderer.setFormColorGrade(gradeBrightnessSnapshot, gradeContrastSnapshot, gradeHueSnapshot, gradeSaturationSnapshot);
                        ModelVAORenderer.setGradeEffectTransforms(gradeSourceSnapshot);

                        ShaderProgram gradeShader = BBSShaders.getModel();
                        MatrixStack gradeStack = new MatrixStack();

                        RenderSystem.setShader(() -> gradeShader);
                        ModelVAORenderer.setupUniforms(gradeStack, gradeShader);
                    }


                    /* Dual-sided: FACE_Z_BIAS separates front/back; single-sided + cull left
                     * the reverse face permanently invisible under Iris deferred redraw. */

                    this.drawBillboardFaces(
                        deferredFormat,
                        deferredTexture,
                        deferredShader,
                        overlayStack,
                        colorSnapshot,
                        localQuad,
                        localUvQuad,
                        overlaySnapshot,
                        lightSnapshot,
                        linear,
                        mipmap,
                        false
                    );

                    if (emitGlowSnapshot)
                    {
                        this.renderGlowOverlay(
                            deferredTexture,
                            GameRenderer::getPositionTexColorProgram,
                            overlayStack,
                            glowSettingsSnapshot,
                            legacyGlowSnapshot,
                            colorSnapshot.a,
                            glowIntensitySnapshot,
                            localQuad,
                            localUvQuad
                        );
                    }
                }
                finally
                {
                    if (gradeActiveSnapshot)
                    {
                        ModelVAORenderer.clearFormColorGrade();
                    }

                    GL11.glPolygonOffset(0F, 0F);

                    if (!savedPolygonOffsetFill)
                    {
                        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                    }
                }
            };

            if (opacityPatch && !noshadingPaintPath)
            {
                /* Same sorted post-deferred queue as models — render depth low→high, before VL. */
                ShaderOpacityPatch.submitPostDeferredBbsForm(sortDepth, distanceSq, depthWrite, afterFluids, deferredDraw);
            }
            else
            {
                ModelVAORenderer.submitDeferredTranslucentModel(deferredDraw, depthWrite, false);
            }
        }
        else
        {
            /* Live path — opaque / no-shader / Iris without deferral. */
            if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)
            {
                if (useFormColorGrade || BBSRendering.needsBbsModelForLowOpacity(color.a))
                {
                    RenderSystem.setShader(BBSShaders::getModel);
                }

                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            }

            if (useFormColorGrade)
            {
                ModelVAORenderer.setFormColorGrade(storedFormColor.brightness, storedFormColor.contrast, storedFormColor.hue, storedFormColor.saturation);
                ModelVAORenderer.setGradeEffectTransforms(storedFormColor);
            }

            try
            {
                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, format);

                /* Front */
                this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, 1F);
                this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, 1F);
                this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, entry, 1F);

                this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, 1F);
                this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, entry, 1F);
                this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, 1F);

                /* Back */
                this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, -FACE_Z_BIAS, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, entry, -1F);
                this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, -1F);
                this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, -1F);

                this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, -1F);
                this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, -FACE_Z_BIAS, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, entry, -1F);
                this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, -1F);

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                if (useFormColorGrade)
                {
                    ShaderProgram gradeShader = BBSShaders.getModel();
                    MatrixStack gradeStack = new MatrixStack();

                    /* Vertices already include the model matrix; keep ModelView identity. */
                    ModelVAORenderer.setupUniforms(gradeStack, gradeShader);
                }

                BufferRenderer.drawWithGlobalProgram(builder.end());
            }
            finally
            {
                if (useFormColorGrade)
                {
                    ModelVAORenderer.clearFormColorGrade();
                }
            }
        }

        if (positivePaint)
        {
            if (modelRenderer)
            {
                /* Form editor preview: draw paint immediately (no Iris world deferral). */
                this.renderPaintOverlay(texture, shader, matrices, OverlayTexture.DEFAULT_UV, resolvedPaint, color.a, this.form.paintSettings.get().transform, glowSettings, legacyGlow, glowIntensity);
            }
            else
            {
                /* After Iris base redraw (see BBSRendering.onWorldRenderEnd order) with a
                 * stronger polygon offset so paint stays in front at distance. */
                this.submitDeferredBillboardPaintOverlay(texture, textureLink, shader, matrices, resolvedPaint, color.a, glowSettings, legacyGlow, glowIntensity);
            }
        }

        if (applyColorTint)
        {
            EffectTransform colorTransform = formColor.transform == null ? null : formColor.transform.copy();

            if (deferContext == null || modelRenderer)
            {
                /* UI / form editor preview: draw color tint immediately (no Iris world deferral). */
                this.renderColorTintOverlay(texture, shader, matrices, overlay, formColor, colorTransform);
            }
            else
            {
                this.submitDeferredBillboardColorTintOverlay(texture, textureLink, shader, matrices, formColor, colorTransform);
            }
        }

        /* Color grade with Iris is handled on the deferred BBS redraw above — do not run
         * ColorGradeOverlay (scene-copy replace makes thin billboards look invisible). */

        if (glowIntensity > 0F && !glowSettings.resolvePaintOnly() && !deferTranslucent && !shadowPass)
        {
            this.renderGlowOverlay(texture, shader, matrices, glowSettings, legacyGlow, color.a, glowIntensity);
        }

        RenderSystem.enableCull();

        texture.setFilterMipmap(false, false);
        if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)
        {
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private void drawBillboardFaces(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, Color color, Quad drawQuad, Quad drawUvQuad, int overlay, int light, boolean linear, boolean mipmap, boolean singleSided)
    {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, format);
        /* Dual-sided even during deferred translucent — that pass used to force single-face
         * + cull and made Color Grade redraws vanish. */

        /* Allow both faces during deferred Iris redraw — FACE_Z_BIAS prevents front/back
         * self z-fight. Only skip dual geometry on the paint-overlay pass. */
        boolean dualSided = !singleSided && !ModelVAORenderer.isPaintOverlayPass();

        this.bindFormTexture(texture);
        RenderSystem.setShader(shader);
        texture.bind();
        texture.setFilterMipmap(linear, mipmap);
        /* Never enable cull here — deferred translucent begins with cull on, and a
         * camera-facing billboard with only the front winding then disappears. */
        RenderSystem.disableCull();

        if (dualSided)
        {
            RenderSystem.disableCull();
        }
        else
        {
            RenderSystem.enableCull();
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.fill(format, builder, matrix, drawQuad.p3.x, drawQuad.p3.y, FACE_Z_BIAS, color, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, drawQuad.p2.x, drawQuad.p2.y, FACE_Z_BIAS, color, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, drawQuad.p1.x, drawQuad.p1.y, FACE_Z_BIAS, color, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, light, entry, 1F);

        this.fill(format, builder, matrix, drawQuad.p3.x, drawQuad.p3.y, FACE_Z_BIAS, color, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, drawQuad.p4.x, drawQuad.p4.y, FACE_Z_BIAS, color, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, drawQuad.p2.x, drawQuad.p2.y, FACE_Z_BIAS, color, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, light, entry, 1F);

        if (dualSided)
        {
            this.fill(format, builder, matrix, drawQuad.p1.x, drawQuad.p1.y, -FACE_Z_BIAS, color, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, light, entry, -1F);
            this.fill(format, builder, matrix, drawQuad.p2.x, drawQuad.p2.y, -FACE_Z_BIAS, color, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, light, entry, -1F);
            this.fill(format, builder, matrix, drawQuad.p3.x, drawQuad.p3.y, -FACE_Z_BIAS, color, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, light, entry, -1F);

            this.fill(format, builder, matrix, drawQuad.p2.x, drawQuad.p2.y, -FACE_Z_BIAS, color, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, light, entry, -1F);
            this.fill(format, builder, matrix, drawQuad.p4.x, drawQuad.p4.y, -FACE_Z_BIAS, color, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, light, entry, -1F);
            this.fill(format, builder, matrix, drawQuad.p3.x, drawQuad.p3.y, -FACE_Z_BIAS, color, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, light, entry, -1F);
        }

        ShaderProgram bound = shader.get();

        /* Vertices already include the model matrix; keep ModelView identity for BBS uniforms
         * (FormColorGrade / ColorGradeOverlay) right before draw. */
        if (bound == BBSShaders.getModel())
        {
            ModelVAORenderer.setupUniforms(new MatrixStack(), bound);
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        texture.setFilterMipmap(false, false);
    }

    private VertexConsumer fill(VertexFormat format, VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, Color color, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz)
    {
        if (format == VertexFormats.POSITION_TEXTURE_LIGHT_COLOR)
        {
            return consumer.vertex(matrix, x, y, z).texture(u, v).light(light).color(color.r, color.g, color.b, color.a);
        }

        if (format == VertexFormats.POSITION_TEXTURE_COLOR)
        {
            return consumer.vertex(matrix, x, y, z).texture(u, v).color(color.r, color.g, color.b, color.a);
        }

        return consumer.vertex(matrix, x, y, z).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, nz);
    }

    private void submitDeferredBillboardPaintOverlay(Texture texture, Link textureLink, Supplier<ShaderProgram> shader, MatrixStack matrices, Color resolvedPaint, float alpha, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(matrices.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        Quad localQuad = new Quad();
        Quad localUvQuad = new Quad();

        localQuad.copy(quad);
        localUvQuad.copy(uvQuad);

        EffectTransform paintTransform = this.form.paintSettings.get().transform.copy();

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            Texture deferredTexture = texture;

            if (textureLink != null)
            {
                Texture linkedTexture = BBSModClient.getTextures().getTexture(textureLink);

                if (linkedTexture != null)
                {
                    deferredTexture = linkedTexture;
                }
            }

            if (deferredTexture == null)
            {
                return;
            }

            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderPaintOverlay(deferredTexture, shader, overlayStack, OverlayTexture.DEFAULT_UV, paintOverlay, 1F, localQuad, localUvQuad, paintTransform, glowSettings, legacyGlow, glowIntensity, FlatPaintOverlayPass.DEFERRED_BILLBOARD_FACTOR, FlatPaintOverlayPass.DEFERRED_BILLBOARD_UNITS);
        });
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha, EffectTransform transform)
    {
        this.renderPaintOverlay(texture, shader, matrices, overlay, resolvedPaint, alpha, quad, uvQuad, transform, null, null, 0F, FlatPaintOverlayPass.DEFAULT_FACTOR, FlatPaintOverlayPass.DEFAULT_UNITS);
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        this.renderPaintOverlay(texture, shader, matrices, overlay, resolvedPaint, alpha, quad, uvQuad, transform, glowSettings, legacyGlow, glowIntensity, FlatPaintOverlayPass.DEFAULT_FACTOR, FlatPaintOverlayPass.DEFAULT_UNITS);
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha, Quad drawQuad, Quad drawUvQuad, EffectTransform transform)
    {
        this.renderPaintOverlay(texture, shader, matrices, overlay, resolvedPaint, alpha, drawQuad, drawUvQuad, transform, null, null, 0F, FlatPaintOverlayPass.DEFAULT_FACTOR, FlatPaintOverlayPass.DEFAULT_UNITS);
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha, Quad drawQuad, Quad drawUvQuad, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        this.renderPaintOverlay(texture, shader, matrices, overlay, resolvedPaint, alpha, drawQuad, drawUvQuad, transform, glowSettings, legacyGlow, glowIntensity, FlatPaintOverlayPass.DEFAULT_FACTOR, FlatPaintOverlayPass.DEFAULT_UNITS);
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha, Quad drawQuad, Quad drawUvQuad, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float polygonOffsetFactor, float polygonOffsetUnits)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;
        this.applyPaintOnlyGlow(paintOverlay, glowSettings, legacyGlow, glowIntensity);

        matrices.push();

        Matrix4f paintMatrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        float overlayBias = this.resolveOverlayFaceZBias(paintMatrix);

        this.bindFormTexture(texture);
        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        FlatPaintOverlayPass.render(polygonOffsetFactor, polygonOffsetUnits, () ->
        {
            BufferBuilder paintBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            int paintLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
            float paintZ = this.resolveOverlayFaceZ(paintMatrix);
            float paintNz = paintZ >= 0F ? 1F : -1F;

            /* One camera-facing plane, both sides via disableCull. */
            RenderSystem.disableCull();

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, overlayBias, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, 1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, overlayBias, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, 1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p1.x, drawQuad.p1.y, overlayBias, paintOverlay, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, paintLight, entry, 1F, transform);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, overlayBias, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, 1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p4.x, drawQuad.p4.y, overlayBias, paintOverlay, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, paintLight, entry, 1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, overlayBias, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, 1F, transform);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p1.x, drawQuad.p1.y, -overlayBias, paintOverlay, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, paintLight, entry, -1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, -overlayBias, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, -1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, -overlayBias, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, -1F, transform);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, -overlayBias, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, -1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p4.x, drawQuad.p4.y, -overlayBias, paintOverlay, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, paintLight, entry, -1F, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, -overlayBias, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, -1F, transform);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, paintZ, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, paintNz, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, paintZ, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, paintNz, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p1.x, drawQuad.p1.y, paintZ, paintOverlay, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, paintLight, entry, paintNz, transform);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, paintZ, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, paintNz, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p4.x, drawQuad.p4.y, paintZ, paintOverlay, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, paintLight, entry, paintNz, transform);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, paintZ, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, paintNz, transform);

            BufferRenderer.drawWithGlobalProgram(paintBuilder.end());

            RenderSystem.enableCull();
        });

        texture.setFilterMipmap(false, false);
        RenderSystem.setShader(shader);
        matrices.pop();
    }

    private void fillPaint(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, Color color, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz, EffectTransform transform)
    {
        float mask = EffectTransformMath.maskBillboard(x, y, z, transform);

        builder.vertex(matrix, x, y, z).color(color.r, color.g, color.b, color.a * mask).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, nz);
    }

    /**
     * Fixed 0.0015 world bias collapses into the same depth sample as the base quad when far
     * away. Scale with camera distance so paint / blend overlays stay in front in NDC.
     */
    private float resolveOverlayFaceZBias(Matrix4f modelMatrix)
    {
        float x = modelMatrix.m30();
        float y = modelMatrix.m31();
        float z = modelMatrix.m32();
        float distance = (float) Math.sqrt(x * x + y * y + z * z);

        return MathUtils.clamp(Math.max(PAINT_FACE_Z_BIAS, distance * 0.00025F), PAINT_FACE_Z_BIAS, PAINT_FACE_Z_BIAS_MAX);
    }

    private void submitDeferredBillboardColorTintOverlay(Texture texture, Link textureLink, Supplier<ShaderProgram> shader, MatrixStack matrices, Color formTintColor, EffectTransform colorTransform)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(matrices.peek().getNormalMatrix());
        Color tintSnapshot = new Color(formTintColor.r, formTintColor.g, formTintColor.b, formTintColor.a);

        Quad localQuad = new Quad();
        Quad localUvQuad = new Quad();

        localQuad.copy(quad);
        localUvQuad.copy(uvQuad);

        EffectTransform colorTransformSnapshot = colorTransform == null ? null : colorTransform.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            Texture deferredTexture = texture;

            if (textureLink != null)
            {
                Texture linkedTexture = BBSModClient.getTextures().getTexture(textureLink);

                if (linkedTexture != null)
                {
                    deferredTexture = linkedTexture;
                }
            }

            if (deferredTexture == null)
            {
                return;
            }

            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderColorTintOverlay(deferredTexture, shader, overlayStack, OverlayTexture.DEFAULT_UV, tintSnapshot, localQuad, localUvQuad, colorTransformSnapshot);
        });
    }

    private void submitDeferredBillboardColorGradeOverlay(Texture texture, Link textureLink, MatrixStack matrices, Color drawColor, Color gradeSource)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(matrices.peek().getNormalMatrix());
        Color colorSnapshot = drawColor.copy();
        float gradeBrightness = gradeSource.brightness;
        float gradeContrast = gradeSource.contrast;
        float gradeHue = gradeSource.hue;
        float gradeSaturation = gradeSource.saturation;
        boolean linear = this.form.linear.get();
        boolean mipmap = this.form.mipmap.get();

        Quad localQuad = new Quad();
        Quad localUvQuad = new Quad();

        localQuad.copy(quad);
        localUvQuad.copy(uvQuad);

        ModelVAORenderer.submitColorGradeOverlay(() ->
        {
            Texture deferredTexture = texture;

            if (textureLink != null)
            {
                Texture linkedTexture = BBSModClient.getTextures().getTexture(textureLink);

                if (linkedTexture != null)
                {
                    deferredTexture = linkedTexture;
                }
            }

            if (deferredTexture == null)
            {
                return;
            }

            try
            {
                ModelVAORenderer.setFormColorGrade(gradeBrightness, gradeContrast, gradeHue, gradeSaturation);
                ModelVAORenderer.setGradeEffectTransforms(gradeSource);
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();

                MatrixStack overlayStack = new MatrixStack();

                overlayStack.peek().getPositionMatrix().set(positionMatrix);
                overlayStack.peek().getNormalMatrix().set(normalMatrix);

                ShaderProgram gradeShader = BBSShaders.getModel();
                MatrixStack uniformStack = new MatrixStack();

                RenderSystem.setShader(() -> gradeShader);
                ModelVAORenderer.setupUniforms(uniformStack, gradeShader);

                this.drawBillboardFaces(
                    VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                    deferredTexture,
                    BBSShaders::getModel,
                    overlayStack,
                    colorSnapshot,
                    localQuad,
                    localUvQuad,
                    OverlayTexture.DEFAULT_UV,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    linear,
                    mipmap,
                    true
                );
            }
            finally
            {
                ModelVAORenderer.clearFormColorGrade();
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
            }
        });
    }

    private void renderColorTintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color formTintColor, EffectTransform transform)
    {
        this.renderColorTintOverlay(texture, shader, matrices, overlay, formTintColor, quad, uvQuad, transform);
    }

    private void renderColorTintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color formTintColor, Quad drawQuad, Quad drawUvQuad, EffectTransform transform)
    {
        matrices.push();

        Matrix4f tintMatrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        float overlayBias = this.resolveOverlayFaceZBias(tintMatrix);

        this.bindFormTexture(texture);
        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        FlatColorTintOverlayPass.render(() ->
        {
            BufferBuilder tintBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            int tintLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

            RenderSystem.disableCull();

            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p3.x, drawQuad.p3.y, overlayBias, formTintColor, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, tintLight, entry, 1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p2.x, drawQuad.p2.y, overlayBias, formTintColor, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, tintLight, entry, 1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p1.x, drawQuad.p1.y, overlayBias, formTintColor, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, tintLight, entry, 1F, transform);

            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p3.x, drawQuad.p3.y, overlayBias, formTintColor, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, tintLight, entry, 1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p4.x, drawQuad.p4.y, overlayBias, formTintColor, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, tintLight, entry, 1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p2.x, drawQuad.p2.y, overlayBias, formTintColor, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, tintLight, entry, 1F, transform);

            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p1.x, drawQuad.p1.y, -overlayBias, formTintColor, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, tintLight, entry, -1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p2.x, drawQuad.p2.y, -overlayBias, formTintColor, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, tintLight, entry, -1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p3.x, drawQuad.p3.y, -overlayBias, formTintColor, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, tintLight, entry, -1F, transform);

            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p2.x, drawQuad.p2.y, -overlayBias, formTintColor, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, tintLight, entry, -1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p4.x, drawQuad.p4.y, -overlayBias, formTintColor, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, tintLight, entry, -1F, transform);
            this.fillColorTint(tintBuilder, tintMatrix, drawQuad.p3.x, drawQuad.p3.y, -overlayBias, formTintColor, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, tintLight, entry, -1F, transform);

            BufferRenderer.drawWithGlobalProgram(tintBuilder.end());

            RenderSystem.enableCull();
        });

        texture.setFilterMipmap(false, false);
        RenderSystem.setShader(shader);
        matrices.pop();
    }

    private void fillColorTint(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, Color formColor, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz, EffectTransform transform)
    {
        float mask = EffectTransformMath.maskBillboard(x, y, z, transform);

        if (mask < 0.001F)
        {
            mask = 0F;
        }

        float r = 1F + (formColor.r - 1F) * mask;
        float g = 1F + (formColor.g - 1F) * mask;
        float b = 1F + (formColor.b - 1F) * mask;

        builder.vertex(matrix, x, y, z).color(r, g, b, mask).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, nz);
    }

    /**
     * Local Z just outside the base face that points toward the camera. {@code viewModel}
     * is the same matrix used to transform overlay verts (camera × stack when deferred).
     */
    private float resolveOverlayFaceZ(Matrix4f viewModel)
    {
        /* Translation ≈ billboard origin in view space; toward camera is -origin. */
        OVERLAY_TO_CAMERA.set(-viewModel.m30(), -viewModel.m31(), -viewModel.m32());
        /* Third column = local +Z axis in view space. */
        OVERLAY_LOCAL_Z.set(viewModel.m20(), viewModel.m21(), viewModel.m22());

        float facing = OVERLAY_LOCAL_Z.dot(OVERLAY_TO_CAMERA);
        float sign = facing >= 0F ? 1F : -1F;

        return sign * (FACE_Z_BIAS + OVERLAY_FACE_EXTRA);
    }

    private void renderGlowOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity)
    {
        this.renderGlowOverlay(texture, shader, matrices, glowSettings, legacyGlow, alpha, glowIntensity, quad, uvQuad);
    }

    private void renderGlowOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, Quad drawQuad, Quad drawUvQuad)
    {
        matrices.push();

        Matrix4f glowMatrix = matrices.peek().getPositionMatrix();

        this.bindFormTexture(texture);
        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        FlatGlowOverlayPass.render(glowSettings, legacyGlow, alpha, glowIntensity, (glowColor) ->
        {
            BufferBuilder glowBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            float glowZ = this.resolveOverlayFaceZ(glowMatrix);

            /* One camera-facing plane, both sides via disableCull — same as paint. */
            RenderSystem.disableCull();

            this.fillGlow(glowBuilder, glowMatrix, drawQuad.p3.x, drawQuad.p3.y, glowZ, glowColor, drawUvQuad.p3.x, drawUvQuad.p3.y);
            this.fillGlow(glowBuilder, glowMatrix, drawQuad.p2.x, drawQuad.p2.y, glowZ, glowColor, drawUvQuad.p2.x, drawUvQuad.p2.y);
            this.fillGlow(glowBuilder, glowMatrix, drawQuad.p1.x, drawQuad.p1.y, glowZ, glowColor, drawUvQuad.p1.x, drawUvQuad.p1.y);

            this.fillGlow(glowBuilder, glowMatrix, drawQuad.p3.x, drawQuad.p3.y, glowZ, glowColor, drawUvQuad.p3.x, drawUvQuad.p3.y);
            this.fillGlow(glowBuilder, glowMatrix, drawQuad.p4.x, drawQuad.p4.y, glowZ, glowColor, drawUvQuad.p4.x, drawUvQuad.p4.y);
            this.fillGlow(glowBuilder, glowMatrix, drawQuad.p2.x, drawQuad.p2.y, glowZ, glowColor, drawUvQuad.p2.x, drawUvQuad.p2.y);

            BufferRenderer.drawWithGlobalProgram(glowBuilder.end());

            RenderSystem.enableCull();
        });

        texture.setFilterMipmap(false, false);
        RenderSystem.setShader(shader);
        matrices.pop();
    }

    private void fillGlow(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, Color color, float u, float v)
    {
        builder.vertex(matrix, x, y, z).texture(u, v).color(color.r, color.g, color.b, color.a);
    }

    private void applyPaintOnlyGlow(Color paintOverlay, GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        if (paintOverlay == null || glowSettings == null || !glowSettings.resolvePaintOnly() || glowIntensity <= 0F)
        {
            return;
        }

        Color glowResolved = new Color();

        glowSettings.resolveColor(legacyGlow, glowResolved);
        FormColorBlend.blendEmission(paintOverlay, glowResolved, glowIntensity);
    }
}