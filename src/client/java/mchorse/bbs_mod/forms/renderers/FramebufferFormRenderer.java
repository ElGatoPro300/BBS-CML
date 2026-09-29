package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
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

    public FramebufferFormRenderer(FramebufferForm form)
    {
        super(form);
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
        /* Both lights along Z, one each way. Y-flipped ortho + front-face cull leaves
         * away-facing limbs on ambient-only (~40%) under Iris/Complementary — that baked
         * black then punches through underwater translucency. ±Z lights both sides flat. */
        RenderSystem.setShaderLights(new Vector3f(0F, 0F, 1F), new Vector3f(0F, 0F, -1F));
        float[] extents = this.resolveViewExtents();
        float halfX = extents[0];
        float halfY = extents[1];

        /* Y is flipped like the legacy [-1, 1] / [1, -1] ortho. Frustum follows
         * view extent even when the FBO is capped — then density drops (pixelates)
         * instead of allocating past {@link #MAX_FRAMEBUFFER_EDGE}. */
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-halfX, halfX, halfY, -halfY, -500F, 500F), VertexSorter.BY_Z);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
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

        if (this.shouldLookAtCamera(context))
        {
            /* Pitch in the view (see form from above), form stays world-upright. */
            this.applyLookAtContentOrientation(context.stack, context);
        }

        /* Full bright inside the buffer: the display quad applies the caller's lightmap once.
         * Baking world light here + dark Iris limbs made Complementary underwater treat those
         * texels as holes. */
        int savedLight = context.light;

        context.light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

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
            BBSRendering.renderOffscreen(() -> this.renderIsolatedFboBodyParts(fboContext));
        }
        finally
        {
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            context.light = savedLight;
            /* ModelForm leaves lightmap off inside offscreen; re-arm before the lit blit. */
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
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorter);
        GL11.glCullFace(cullFace);

        /* Lit entity_translucent: content was MAX_LIGHT; caller lightmap shades the postcard
         * once. Complementary underwater composites this path; unlit position_tex_color
         * punched dark/low-alpha FBO texels into holes. Picking stays unlit. */
        boolean shading = !context.isPicking();
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
     * Re-assert ±Z diffuse after each prepare — {@link BBSRendering#prepareVanillaEntityLighting}
     * restores world lights and would undo the flat FBO lighting.
     */
    private void renderIsolatedFboBodyParts(FormRenderingContext context)
    {
        if (this.form.parts.getAllTyped().isEmpty())
        {
            return;
        }

        List<BodyPart> parts = this.getSortedBodyParts(context);

        this.prepareFboContentLighting();

        if (ItemBodyPartBatch.renderBodyParts(this, parts, context))
        {
            BBSRendering.restoreWorldRenderState();
            this.prepareFboContentLighting();

            return;
        }

        for (BodyPart part : parts)
        {
            this.prepareFboContentLighting();

            try
            {
                this.renderBodyPart(part, context);
            }
            finally
            {
                BBSRendering.restoreWorldRenderState();
            }
        }

        this.prepareFboContentLighting();
    }

    /** Lightmap + overlay + flat ±Z diffuse for FBO content (not world entity lights). */
    private void prepareFboContentLighting()
    {
        BBSRendering.prepareVanillaEntityLighting();
        RenderSystem.setShaderLights(new Vector3f(0F, 0F, 1F), new Vector3f(0F, 0F, -1F));
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
        if (this.shouldLookAtCamera(context))
        {
            this.applyLookAtBillboard(matrices, context);
        }

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, format);
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

    private boolean isWorldBillboardPass(FormRenderingContext context)
    {
        return context != null && !context.ui && !context.modelRenderer;
    }

    private boolean shouldLookAtCamera(FormRenderingContext context)
    {
        return this.form.lookAt.get() && this.isWorldBillboardPass(context) && context.camera != null;
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
     * Face the display quad at the camera (yaw + pitch), same bake as Video/Label.
     * Keep translate/scale; replace rotation with {@code camera.view}.
     */
    private void applyLookAtBillboard(MatrixStack matrices, FormRenderingContext context)
    {
        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        Vector3f scale = new Vector3f();

        modelMatrix.getScale(scale);
        modelMatrix.m00(1).m01(0).m02(0);
        modelMatrix.m10(0).m11(1).m12(0);
        modelMatrix.m20(0).m21(0).m22(1);
        modelMatrix.mul(context.camera.view);
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
    private void applyLookAtContentOrientation(MatrixStack stack, FormRenderingContext context)
    {
        Vector3f target = this.resolveFormWorldPosition(context);
        float eyeX = (float) context.camera.position.x;
        float eyeY = (float) context.camera.position.y;
        float eyeZ = (float) context.camera.position.z;
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
            float yaw = context.camera.rotation.y;

            up.set((float) Math.sin(yaw), 0F, (float) -Math.cos(yaw));

            if (up.lengthSquared() < 1.0E-8F)
            {
                up.set(0F, 0F, -1F);
            }
        }

        Matrix4f content = new Matrix4f().lookAt(eyeX, eyeY, eyeZ, target.x, target.y, target.z, up.x, up.y, up.z);

        if (context.world != null)
        {
            content.mul(new Matrix4f(context.world.peek().getPositionMatrix()));
        }

        content.m30(0F).m31(0F).m32(0F);
        stack.peek().getPositionMatrix().mul(content);

        Matrix3f normal = new Matrix3f();

        content.normal(normal);
        stack.peek().getNormalMatrix().set(normal);
    }

    private VertexConsumer fill(VertexFormat format, VertexConsumer consumer, Matrix4f matrix, float x, float y, Color color, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz)
    {
        if (format == VertexFormats.POSITION_TEXTURE_COLOR)
        {
            return consumer.vertex(matrix, x, y, 0F).texture(u, v).color(color.r, color.g, color.b, color.a);
        }

        return consumer.vertex(matrix, x, y, 0F).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, nz);
    }

    @Override
    public void collectMatrices(IEntity entity, MatrixStack stack, MatrixCache matrices, String prefix, float transition)
    {
        stack.push();
        this.applyTransforms(stack, true, transition);
        Matrix4f origin = new Matrix4f(stack.peek().getPositionMatrix());
        stack.pop();

        stack.push();
        this.applyTransforms(stack, false, transition);
        matrices.put(prefix, new Matrix4f(stack.peek().getPositionMatrix()), origin);

        float scale = this.form.scale.get();
        float baseW = MathUtils.clamp(this.form.width.get(), 2, MAX_FRAMEBUFFER_EDGE);
        float baseH = MathUtils.clamp(this.form.height.get(), 2, MAX_FRAMEBUFFER_EDGE);

        Matrix4f parent = new Matrix4f(stack.peek().getPositionMatrix());
        MatrixStack childStack = new MatrixStack();
        MatrixCache children = new MatrixCache();

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
