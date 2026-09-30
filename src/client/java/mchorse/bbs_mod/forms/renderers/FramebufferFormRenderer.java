package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.FramebufferPool;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FramebufferFormRenderer extends FormRenderer<FramebufferForm>
{
    private static final Quad quad = new Quad();
    private static final Quad uvQuad = new Quad();

    private IEntity entity = new StubEntity();

    /**
     * Last non-UI look-at capture. {@link #collectMatrices} has no render context, so it
     * replays these (or a bound preview camera) so gizmos / bone origins match the
     * billboard + camera-content bake used while filling / blitting the FBO.
     */
    private boolean captureLookAtBillboard;
    private boolean captureLookAtContent;
    private boolean lookAtSampleValid;
    /**
     * True when the last look-at sample came from a pass whose MatrixStack already includes
     * {@code camera.view} (form-editor orbit preview). Billboard must not bake view again —
     * identity rotation already faces the viewer, and gizmos get view applied when drawn.
     */
    private boolean lookAtViewSpaceAlready;
    private final Camera lookAtCamera = new Camera();
    private final Matrix4f lookAtWorld = new Matrix4f();
    private final Vector3f lookAtTarget = new Vector3f();
    private boolean lookAtWorldValid;

    /**
     * Same model matrix the highlight blit uses (form-editor orbit already includes
     * {@code camera.view}). Capture pre/post billboard so gizmos reconstruct
     * {@code parent = F·inv(pre)·post} with {@code S0·parent = post} — matching stencil.
     */
    private final Matrix4f capturedPreBillboard = new Matrix4f();
    private final Matrix4f capturedBillboardMV = new Matrix4f();
    private boolean hasBillboardCapture;

    /**
     * Form-editor / film preview camera for the current UI frame. Set while the pickable
     * viewport renders so {@link #collectMatrices} can resolve look-at even before a world
     * draw has populated {@link #lookAtSampleValid}.
     */
    private static final ThreadLocal<Camera> PREVIEW_CAMERA = new ThreadLocal<>();

    public FramebufferFormRenderer(FramebufferForm form)
    {
        super(form);
    }

    public static void bindPreviewCamera(Camera camera)
    {
        PREVIEW_CAMERA.set(camera);
    }

    public static void unbindPreviewCamera()
    {
        PREVIEW_CAMERA.remove();
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        if (this.form.parts.getAll().isEmpty())
        {
            /* Nothing in it yet, so there is no picture to show - stand a figure in the cell
             * instead, at the size the video form draws its own placeholder at. */
            context.batcher.icon(Icons.CAMERA, (x1 + x2 - Icons.CAMERA.w) / 2F, (y1 + y2 - Icons.CAMERA.h) / 2F);
        }
        else
        {
            MatrixStack stack = context.batcher.getContext().getMatrices();
            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            stack.push();

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);
            stack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180F));
            stack.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
            stack.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

            this.renderBodyParts(new FormRenderingContext()
                .set(FormRenderType.ENTITY, this.entity, stack, LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
                .inUI());

            stack.pop();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }

    @Override
    public void renderBodyParts(FormRenderingContext context)
    {
        FramebufferPool pool = BBSModClient.getFramebuffers().getFormFramebuffers();
        int[] size = this.resolveFramebufferSize();
        Framebuffer framebuffer = pool.get(size[0], size[1]);

        try
        {
            this.renderFramebuffer(context, framebuffer);
        }
        finally
        {
            pool.release(framebuffer);
        }
    }

    /**
     * Soft cap for allocated FBO edges. Ideal size is {@code resolution × extent};
     * past this the buffer stops growing and the picture pixelates instead of
     * allocating huge (laggy) textures.
     */
    private static final int MAX_FRAMEBUFFER_EDGE = 4096;

    /**
     * FBO pixels ≈ base resolution × view extent, capped at
     * {@link #MAX_FRAMEBUFFER_EDGE}. Ortho and the world quad always follow the
     * full view extent so crop area keeps expanding; density only drops after
     * the pixel budget is exhausted.
     */
    private int[] resolveFramebufferSize()
    {
        int baseW = MathUtils.clamp(this.form.width.get(), 2, MAX_FRAMEBUFFER_EDGE);
        int baseH = MathUtils.clamp(this.form.height.get(), 2, MAX_FRAMEBUFFER_EDGE);
        float halfX = safeViewExtent(this.form.viewExtentX.get());
        float halfY = safeViewExtent(this.form.viewExtentY.get());
        int fboW = MathUtils.clamp(Math.round(baseW * halfX), 2, MAX_FRAMEBUFFER_EDGE);
        int fboH = MathUtils.clamp(Math.round(baseH * halfY), 2, MAX_FRAMEBUFFER_EDGE);

        return new int[] {fboW, fboH};
    }

    private float[] resolveViewExtents()
    {
        return new float[] {
            safeViewExtent(this.form.viewExtentX.get()),
            safeViewExtent(this.form.viewExtentY.get())
        };
    }

    private void renderFramebuffer(FormRenderingContext context, Framebuffer framebuffer)
    {
        int x;
        int y;
        int width;
        int height;

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            IntBuffer viewport = stack.mallocInt(4);

            GL30.glGetIntegerv(GL30.GL_VIEWPORT, viewport);

            x = viewport.get(0);
            y = viewport.get(1);
            width = viewport.get(2);
            height = viewport.get(3);
        }

        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] scissorBox = new int[4];
        float[] clearColor = new float[4];

        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);

        Vector3f light0 = RenderSystem.shaderLightDirections[0];
        Vector3f light1 = RenderSystem.shaderLightDirections[1];
        Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorter vertexSorter = RenderSystem.getVertexSorting();
        int cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);

        GL30.glCullFace(GL30.GL_FRONT);
        /* Iris pack: bake flat albedo (Unlit) — pack state can black out limbs if we mix_light
         * inside the FBO; the lit display quad applies world shading once.
         * No pack: bake matching world diffuse + caller lightmap into the texture (same basis
         * as model-block / form previews), then blit unlit so shading is not applied twice. */
        boolean irisPack = BBSRendering.isIrisShadersEnabled();
        boolean bakeWorldLighting = !irisPack;

        if (bakeWorldLighting)
        {
            BBSRendering.setupMatchingWorldDiffuseLighting();
        }
        else
        {
            /* Flat ±Z only matters if a non-Unlit path still samples diffuse inside the FBO. */
            RenderSystem.setShaderLights(new Vector3f(0F, 0F, 1F), new Vector3f(0F, 0F, -1F));
        }

        float[] extents = this.resolveViewExtents();
        float halfX = extents[0];
        float halfY = extents[1];

        /* Y is flipped like the legacy [-1, 1] / [1, -1] ortho. Frustum follows
         * view extent even when the FBO is capped — then density drops (pixelates)
         * instead of allocating past {@link #MAX_FRAMEBUFFER_EDGE}. */
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-halfX, halfX, halfY, -halfY, -500F, 500F), VertexSorter.BY_Z);
        RenderSystem.getModelViewStack().push();
        RenderSystem.getModelViewStack().loadIdentity();
        RenderSystem.applyModelViewMatrix();

        framebuffer.apply();

        /* Whoever was drawing before us may have left a scissor box — the UI clips its
         * viewport that way — and it would clip this framebuffer's own pixels too. */
        RenderSystem.disableScissor();

        /* Transparent clear: whatever was drawn before us may have left an opaque clear colour,
         * and clearing this buffer with it would give the finished picture a solid background. */
        RenderSystem.clearColor(0F, 0F, 0F, 0F);
        framebuffer.clear();

        context.stack.push();
        context.stack.peek().getPositionMatrix().identity();
        context.stack.peek().getNormalMatrix().identity();

        this.captureLookAtState(context);

        if (this.captureLookAtContent)
        {
            /* Pitch in the view (see form from above), form stays world-upright. */
            this.applyLookAtContentOrientation(context.stack, this.lookAtCamera, this.lookAtTarget, this.lookAtWorldValid ? this.lookAtWorld : null);
        }

        int savedLight = context.light;

        /* Iris Unlit bake: MAX_LIGHT for any residual lightmap sample. Vanilla bake: keep
         * the caller's block/sky light so interior limbs match nearby world forms. */
        if (!bakeWorldLighting)
        {
            context.light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }

        /* Blending as GL really holds it, not as GlStateManager's cache believes. A shader pack's
         * per-draw-buffer blend modes are set by Iris with indexed GL calls the cache never sees,
         * and put back through the cache - which skips the real call when it already thinks the
         * default is in place. After a pack entity program, alpha factors can leave dst alpha at 0
         * while RGB paints — black-looking texels that vanish underwater under Complementary. */
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        /* Prefer the outer host (player, actor, parent form entity) so body-part
         * "use target" animates against that entity. Fall back to this FBO's stub
         * when there is no host (e.g. some UI cells). PREVIEW keeps Iris soft queues off. */
        IEntity host = context.entity != null ? context.entity : this.entity;
        FormRenderingContext fboContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, host, context.stack, context.light, OverlayTexture.DEFAULT_UV, context.getTransition());

        if (context.isPicking())
        {
            fboContext.stencilMap(context.stencilMap);
            fboContext.color(context.color);
        }

        if (context.isShadowPass)
        {
            fboContext.isShadowPass = true;
        }

        fboContext.renderEquipment = context.renderEquipment;

        try
        {
            Runnable fillParts = () -> this.renderIsolatedFboBodyParts(fboContext, bakeWorldLighting);

            if (bakeWorldLighting)
            {
                BBSRendering.renderOffscreen(fillParts);
            }
            else
            {
                BBSRendering.runFramebufferContentUnlit(() -> BBSRendering.renderOffscreen(fillParts));
            }
        }
        finally
        {
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            context.light = savedLight;
            /* ModelForm leaves lightmap off inside offscreen; re-arm before the blit. */
            BBSRendering.restoreWorldRenderState();
            BBSRendering.prepareVanillaEntityLighting();
        }

        context.stack.pop();

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glViewport(x, y, width, height);

        if (scissorEnabled)
        {
            RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        }
        else
        {
            RenderSystem.disableScissor();
        }

        RenderSystem.setShaderLights(light0, light1);
        RenderSystem.getModelViewStack().pop();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorter);
        GL11.glCullFace(cullFace);

        /* Vanilla: content already lit → unlit blit. Iris: flat albedo → lit entity_translucent
         * so the postcard gets world light once (and underwater alpha composites correctly). */
        boolean shading = !bakeWorldLighting && !context.isPicking();
        VertexFormat format = shading
            ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
            : VertexFormats.POSITION_TEXTURE_COLOR;
        Supplier<ShaderProgram> shader = shading
            ? GameRenderer::getRenderTypeEntityTranslucentProgram
            : GameRenderer::getPositionTexColorProgram;

        this.renderModel(framebuffer.getMainTexture(), format, shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context);
    }

    /**
     * Draw FBO body parts with per-part lightmap isolation. {@link BBSRendering#renderOffscreen}
     * clears {@code isRenderingWorld}, so {@link ModelFormRenderer} disables the lightmap and
     * skips {@link BBSRendering#restoreWorldRenderState()} — under Iris/NeoForge the next limb
     * then samples a dead lightmap and draws black. Same idea as film replay isolation.
     *
     * @param bakeWorldLighting when true, re-assert matching world diffuse after each prepare
     *                          (vanilla path); when false, keep flat ±Z under Iris Unlit fill.
     */
    private void renderIsolatedFboBodyParts(FormRenderingContext context, boolean bakeWorldLighting)
    {
        if (this.form.parts.getAllTyped().isEmpty())
        {
            return;
        }

        List<BodyPart> parts = this.getSortedBodyParts(context);

        this.prepareFboContentLighting(bakeWorldLighting);

        if (ItemBodyPartBatch.renderBodyParts(this, parts, context))
        {
            BBSRendering.restoreWorldRenderState();
            this.prepareFboContentLighting(bakeWorldLighting);

            return;
        }

        for (BodyPart part : parts)
        {
            this.prepareFboContentLighting(bakeWorldLighting);

            try
            {
                this.renderBodyPart(part, context);
            }
            finally
            {
                BBSRendering.restoreWorldRenderState();
            }
        }

        this.prepareFboContentLighting(bakeWorldLighting);
    }

    /**
     * Lightmap + overlay for FBO content. Vanilla bake uses matching world diffuse;
     * Iris Unlit fill keeps ±Z only as a safe fallback if Unlit is skipped.
     */
    private void prepareFboContentLighting(boolean bakeWorldLighting)
    {
        BBSRendering.prepareVanillaEntityLighting();

        if (bakeWorldLighting)
        {
            BBSRendering.setupMatchingWorldDiffuseLighting();
        }
        else
        {
            RenderSystem.setShaderLights(new Vector3f(0F, 0F, 1F), new Vector3f(0F, 0F, -1F));
        }
    }

    private void renderModel(Texture texture, VertexFormat format, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, FormRenderingContext context)
    {
        float w = texture.width;
        float h = texture.height;

        /* TL = top left, BR = bottom right*/
        Vector4f crop = new Vector4f(0F, 0F, 0F, 0F);
        float uvTLx = crop.x / w;
        float uvTLy = crop.y / h;
        float uvBRx = 1F - crop.z / w;
        float uvBRy = 1F - crop.w / h;

        uvQuad.p1.set(uvTLx, uvTLy, 0F);
        uvQuad.p2.set(uvBRx, uvTLy, 0F);
        uvQuad.p3.set(uvTLx, uvBRy, 0F);
        uvQuad.p4.set(uvBRx, uvBRy, 0F);

        /* World quad grows with view extent (base aspect × scale × extent) so
         * content keeps the same world size while more crop area becomes visible.
         * FBO pixels already grew with extent, so density stays on width/height. */
        float baseW = MathUtils.clamp(this.form.width.get(), 2, MAX_FRAMEBUFFER_EDGE);
        float baseH = MathUtils.clamp(this.form.height.get(), 2, MAX_FRAMEBUFFER_EDGE);
        float[] extents = this.resolveViewExtents();
        float halfX = extents[0];
        float halfY = extents[1];
        float scale = this.form.scale.get() * 2F;
        float ratioX = (baseW > baseH ? baseH / baseW : 1F) * scale * halfY;
        float ratioY = (baseH > baseW ? baseW / baseH : 1F) * scale * halfX;
        float TLx = (uvTLx - 0.5F) * ratioY;
        float TLy = -(uvTLy - 0.5F) * ratioX;
        float BRx = (uvBRx - 0.5F) * ratioY;
        float BRy = -(uvBRy - 0.5F) * ratioX;

        quad.p1.set(TLx, TLy, 0F);
        quad.p2.set(BRx, TLy, 0F);
        quad.p3.set(TLx, BRy, 0F);
        quad.p4.set(BRx, BRy, 0F);

        this.renderQuad(format, texture, shader, matrices, overlay, light, overlayColor, transition, context);
    }

    private void renderQuad(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, FormRenderingContext context)
    {
        this.hasBillboardCapture = false;

        if (this.captureLookAtBillboard)
        {
            /* Capture the exact matrices the highlight/stencil blit uses. */
            if (this.lookAtViewSpaceAlready)
            {
                this.capturedPreBillboard.set(matrices.peek().getPositionMatrix());
            }

            this.applyLookAtBillboard(matrices);

            if (this.lookAtViewSpaceAlready)
            {
                this.capturedBillboardMV.set(matrices.peek().getPositionMatrix());
                this.hasBillboardCapture = true;
            }
        }

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, format);
        Color color = Color.white();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();

        color.mul(overlayColor);

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        boolean litQuad = format != VertexFormats.POSITION_TEXTURE_COLOR;

        if (litQuad)
        {
            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableCull();

        BBSModClient.getTextures().bindTexture(texture);
        texture.bind();
        /* Re-assert every draw: join/Iris/reload can leave LINEAR on the FBO id,
         * which turns intentional low-res pixelation into blur until the size changes. */
        texture.setFilter(GL11.GL_NEAREST);
        texture.setParameter(GL30.GL_TEXTURE_MAX_LEVEL, 0);
        RenderSystem.setShaderTexture(0, texture.id);
        RenderSystem.setShader(shader);

        /* Front */
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, entry, 1F);

        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, 1F);

        /* Back */
        this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, -1F);

        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, -1F);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        if (litQuad)
        {
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    /**
     * Look-at applies in world / film / form-editor preview (any pass with a real camera).
     * Skipped only for UI thumbnails ({@code context.ui}), where there is no orbit camera.
     */
    private boolean isLookAtPass(FormRenderingContext context)
    {
        return context != null && !context.ui && context.camera != null;
    }

    private boolean shouldLookAtBillboard(FormRenderingContext context)
    {
        return this.form.billboard.get() && this.isLookAtPass(context);
    }

    private boolean shouldLookAtCameraContent(FormRenderingContext context)
    {
        return this.form.cameraContent.get() && this.isLookAtPass(context);
    }

    /**
     * Snapshot look-at inputs from this draw so {@link #collectMatrices} can match the
     * billboard / camera-content bake. Non-UI passes with toggles off clear the sample so
     * matrices do not keep a stale look-at after the user disables the toggles.
     */
    private void captureLookAtState(FormRenderingContext context)
    {
        this.captureLookAtBillboard = this.shouldLookAtBillboard(context);
        this.captureLookAtContent = this.shouldLookAtCameraContent(context);

        if (!this.isLookAtPass(context))
        {
            return;
        }

        if (!this.captureLookAtBillboard && !this.captureLookAtContent)
        {
            this.lookAtSampleValid = false;
            this.lookAtWorldValid = false;
            this.lookAtViewSpaceAlready = false;

            return;
        }

        this.lookAtCamera.copy(context.camera);
        this.lookAtTarget.set(this.resolveFormWorldPosition(context));
        this.lookAtSampleValid = true;
        /* Form-editor orbit already multiplies camera.view onto the draw stack. */
        this.lookAtViewSpaceAlready = context.modelRenderer;

        /* F7 world gizmos: keep orbit viewSpaceAlready for the display quad, but drive
         * camera-content look-at from the game camera so nested origins match the live morph. */
        if (context.matchWorldLookAt)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && mc.gameRenderer != null)
            {
                FormRenderingContext tmp = new FormRenderingContext();

                tmp.camera(mc.gameRenderer.getCamera());
                this.lookAtCamera.copy(tmp.camera);
            }
        }

        if (context.world != null)
        {
            /* Orientation only — General/transform scale must not enter the FBO ortho
             * (would zoom content and look like view extent shrinking). Scale belongs on
             * the display quad via context.stack. */
            this.lookAtWorld.set(MatrixStackUtils.stripScale(context.world.peek().getPositionMatrix()));
            this.lookAtWorldValid = true;
        }
        else
        {
            this.lookAtWorld.identity();
            this.lookAtWorldValid = false;
        }
    }

    private Vector3f resolveFormWorldPosition(FormRenderingContext context)
    {
        if (context.world != null)
        {
            return context.world.peek().getPositionMatrix().getTranslation(new Vector3f());
        }

        if (context.entity != null)
        {
            float transition = context.getTransition();

            return new Vector3f(
                (float) Lerps.lerp(context.entity.getPrevX(), context.entity.getX(), transition),
                (float) Lerps.lerp(context.entity.getPrevY(), context.entity.getY(), transition),
                (float) Lerps.lerp(context.entity.getPrevZ(), context.entity.getZ(), transition)
            );
        }

        return new Vector3f(
            (float) context.camera.position.x,
            (float) context.camera.position.y,
            (float) context.camera.position.z
        );
    }

    /**
     * Face the display quad at the camera (yaw + pitch), same bake as Billboard/Label.
     * Keep translate/scale; reset rotation to identity in camera view space.
     */
    private void applyLookAtBillboard(MatrixStack matrices)
    {
        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        Vector3f scale = new Vector3f();

        modelMatrix.getScale(scale);
        modelMatrix.m00(1).m01(0).m02(0);
        modelMatrix.m10(0).m11(1).m12(0);
        modelMatrix.m20(0).m21(0).m22(1);

        modelMatrix.scale(scale);

        /* Do not bake camera.view into normals (Iris lighting pulse on orbit). */
        matrices.peek().getNormalMatrix().identity();
        matrices.peek().getNormalMatrix().scale(
            MatrixStackUtils.safeNormalScaleReciprocal(scale.x),
            MatrixStackUtils.safeNormalScaleReciprocal(scale.y),
            MatrixStackUtils.safeNormalScaleReciprocal(scale.z)
        );
    }

    /**
     * FBO content = camera looking at the form with <b>world up</b>, so pitch changes
     * the viewing angle (top of head when looking down) without tipping the subject.
     * Camera-up would roll with pitch; flattening the eye would leave only a tilted
     * postcard via the billboard. Degenerate zenith/nadir uses camera yaw for screen-up.
     */
    private void applyLookAtContentOrientation(MatrixStack stack, Camera camera, Vector3f target, Matrix4f worldMatrix)
    {
        float eyeX = (float) camera.position.x;
        float eyeY = (float) camera.position.y;
        float eyeZ = (float) camera.position.z;
        float dx = target.x - eyeX;
        float dy = target.y - eyeY;
        float dz = target.z - eyeZ;

        if (dx * dx + dy * dy + dz * dz < 1.0E-8F)
        {
            return;
        }

        Vector3f up = new Vector3f(0F, 1F, 0F);
        float horizSq = dx * dx + dz * dz;

        if (horizSq < 1.0E-6F)
        {
            float yaw = camera.rotation.y;

            up.set((float) Math.sin(yaw), 0F, (float) -Math.cos(yaw));

            if (up.lengthSquared() < 1.0E-8F)
            {
                up.set(0F, 0F, -1F);
            }
        }

        Matrix4f content = new Matrix4f().lookAt(eyeX, eyeY, eyeZ, target.x, target.y, target.z, up.x, up.y, up.z);

        if (worldMatrix != null)
        {
            content.mul(MatrixStackUtils.stripScale(worldMatrix));
        }

        content.m30(0F).m31(0F).m32(0F);
        stack.peek().getPositionMatrix().mul(content);

        Matrix3f normal = new Matrix3f();

        content.normal(normal);
        stack.peek().getNormalMatrix().set(normal);
    }

    private void fill(VertexFormat format, VertexConsumer consumer, Matrix4f matrix, float x, float y, Color color, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz)
    {
        if (format == VertexFormats.POSITION_TEXTURE_LIGHT_COLOR)
        {
            consumer.vertex(matrix, x, y, 0F).texture(u, v).light(light).color(color.r, color.g, color.b, color.a).next();
            return;
        }
        if (format == VertexFormats.POSITION_TEXTURE_COLOR)
        {
            consumer.vertex(matrix, x, y, 0F).texture(u, v).color(color.r, color.g, color.b, color.a).next();
            return;
        }

        consumer.vertex(matrix, x, y, 0F).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(entry.getNormalMatrix(), 0F, 0F, nz).next();
    }

    @Override
    public void collectMatrices(IEntity entity, MatrixStack stack, MatrixCache matrices, String prefix, float transition)
    {
        boolean billboard = this.form.billboard.get();
        boolean cameraContent = this.form.cameraContent.get();
        boolean applyLookAt = (billboard || cameraContent) && this.ensureLookAtSample(entity, transition);
        /* Prefer the look-at capture's own view-space flag. PREVIEW_CAMERA only tips
         * orbit mesh mode when no capture exists yet — never override a world sample. */
        boolean viewSpaceAlready = this.lookAtViewSpaceAlready
            || (PREVIEW_CAMERA.get() != null && !this.lookAtSampleValid);

        stack.push();
        this.applyTransforms(stack, true, transition);
        Matrix4f origin = new Matrix4f(stack.peek().getPositionMatrix());
        stack.pop();

        stack.push();
        this.applyTransforms(stack, false, transition);

        if (applyLookAt && billboard)
        {
            if (viewSpaceAlready && this.hasBillboardCapture)
            {
                /*
                 * Highlight blit matrix is capturedBillboardMV (= post). Gizmo draw does
                 * S0·parent; choose parent = F·inv(pre)·post so S0·parent = post.
                 * Do not touch camera/target/world from the content capture.
                 */
                Matrix4f formMatrix = new Matrix4f(stack.peek().getPositionMatrix());
                Matrix4f invPre = new Matrix4f(this.capturedPreBillboard);

                if (Math.abs(invPre.determinant()) > 1.0E-8F)
                {
                    invPre.invert();
                    stack.peek().getPositionMatrix().set(formMatrix).mul(invPre).mul(this.capturedBillboardMV);
                }
            }
            else if (!viewSpaceAlready)
            {
                /* World/film: model matrix has no orbit view yet. */
                this.applyLookAtBillboard(stack);
            }
            /* Editor without capture yet: keep form matrix (one frame). */
        }

        matrices.put(prefix, new Matrix4f(stack.peek().getPositionMatrix()), origin);

        float scale = this.form.scale.get();
        float baseW = MathUtils.clamp(this.form.width.get(), 2, MAX_FRAMEBUFFER_EDGE);
        float baseH = MathUtils.clamp(this.form.height.get(), 2, MAX_FRAMEBUFFER_EDGE);

        Matrix4f parent = new Matrix4f(stack.peek().getPositionMatrix());
        MatrixStack childStack = new MatrixStack();
        MatrixCache children = new MatrixCache();

        /* Same camera-content bake as the FBO fill (highlight path) — uses render capture. */
        if (applyLookAt && cameraContent)
        {
            this.applyLookAtContentOrientation(childStack, this.lookAtCamera, this.lookAtTarget, this.lookAtWorldValid ? this.lookAtWorld : null);
        }

        /* Quad grows with extent; 1 FBO unit stays one world unit (base aspect × scale). */
        float scaleX = scale * (baseH > baseW ? baseW / baseH : 1F);
        float scaleY = scale * (baseW > baseH ? baseH / baseW : 1F);

        for (BodyPart part : this.form.parts.getAllTyped())
        {
            Form form = part.getForm();

            if (form != null)
            {
                childStack.push();
                MatrixStackUtils.applyTransform(childStack, part.transform.get());

                FormUtilsClient.getRenderer(form).collectMatrices(entity, childStack, children, StringUtils.combinePaths(prefix, part.getId()), transition);

                childStack.pop();
            }
        }

        stack.pop();

        for (Map.Entry<String, MatrixCacheEntry> entry : children.entrySet())
        {
            MatrixCacheEntry child = entry.getValue();

            matrices.put(entry.getKey(), this.projectOrigin(parent, child.matrix(), scaleX, scaleY), this.projectOrigin(parent, child.origin(), scaleX, scaleY));
        }
    }

    /**
     * Keep the render-pass look-at capture intact (camera content depends on it).
     * Only fall back to preview/game camera when nothing was drawn yet this frame.
     */
    private boolean ensureLookAtSample(IEntity entity, float transition)
    {
        if (this.lookAtSampleValid)
        {
            return true;
        }

        Camera preview = PREVIEW_CAMERA.get();

        if (preview != null)
        {
            this.lookAtCamera.copy(preview);
            this.lookAtViewSpaceAlready = true;

            return this.fillLookAtTargetFromEntity(entity, transition);
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.gameRenderer == null)
        {
            return false;
        }

        FormRenderingContext tmp = new FormRenderingContext();

        tmp.camera(mc.gameRenderer.getCamera());
        this.lookAtCamera.copy(tmp.camera);
        this.lookAtViewSpaceAlready = false;

        return this.fillLookAtTargetFromEntity(entity, transition);
    }

    private boolean fillLookAtTargetFromEntity(IEntity entity, float transition)
    {
        if (entity != null)
        {
            this.lookAtTarget.set(
                (float) Lerps.lerp(entity.getPrevX(), entity.getX(), transition),
                (float) Lerps.lerp(entity.getPrevY(), entity.getY(), transition),
                (float) Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition)
            );

            this.lookAtWorld.identity();
            this.lookAtWorld.translate(this.lookAtTarget.x, this.lookAtTarget.y, this.lookAtTarget.z);
            this.lookAtWorld.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(
                -Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), transition)));
            this.lookAtWorldValid = true;
        }
        else
        {
            this.lookAtTarget.set(
                (float) this.lookAtCamera.position.x,
                (float) this.lookAtCamera.position.y,
                (float) this.lookAtCamera.position.z
            );
            this.lookAtWorld.identity();
            this.lookAtWorldValid = false;
        }

        return true;
    }

    private Matrix4f projectOrigin(Matrix4f parent, Matrix4f child, float scaleX, float scaleY)
    {
        if (child == null)
        {
            return null;
        }

        /* Flatten positions only: gizmo orientation and rotation sampling need a full basis. */
        Matrix4f projected = new Matrix4f(child).setTranslation(child.m30() * scaleX, child.m31() * scaleY, 0F);

        return new Matrix4f(parent).mul(projected);
    }

    /**
     * Keep the ortho matrix invertible. FBO edge budget is capped separately;
     * extents themselves are not hard-capped so crop can grow freely.
     */
    private static float safeViewExtent(float extent)
    {
        return extent < 0.01F ? 0.01F : extent;
    }
}
