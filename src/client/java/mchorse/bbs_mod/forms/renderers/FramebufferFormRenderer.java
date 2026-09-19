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
        Framebuffer framebuffer = pool.get(MathUtils.clamp(this.form.width.get(), 2, 4096), MathUtils.clamp(this.form.height.get(), 2, 4096));

        try
        {
            this.renderFramebuffer(context, framebuffer);
        }
        finally
        {
            pool.release(framebuffer);
        }
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
        /* Both lights along Z, one each way. The picture in here is meant to be flat, and the
         * two vanilla lights are what a flat one is made of - but pointing both at the camera
         * lights only the faces that happen to look back at it. The framebuffer renders under a
         * Y-flipped ortho with front faces culled, so a two-sided quad keeps the side whose normal
         * points away. */
        RenderSystem.setShaderLights(new Vector3f(0F, 0F, 1F), new Vector3f(0F, 0F, -1F));
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-1F, 1F, 1F, -1F, -500F, 500F), VertexSorter.BY_Z);
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

        /* Full bright on the way in: the quad that draws the finished picture applies the
         * caller's lightmap once, so letting it shade the parts inside the buffer too would
         * land the very same shading on them twice. */
        int light = context.light;

        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        FormRenderingContext fboContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.entity, context.stack, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, context.getTransition());

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
            BBSRendering.renderOffscreen(() -> super.renderBodyParts(fboContext));
        }
        finally
        {
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
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

        boolean shading = !context.isPicking();
        VertexFormat format = shading ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR;
        Supplier<ShaderProgram> shader = shading ? GameRenderer::getRenderTypeEntityTranslucentProgram : GameRenderer::getPositionTexColorProgram;

        this.renderModel(framebuffer.getMainTexture(), format, shader, context.stack, context.overlay, context.light, context.color, context.getTransition());
    }

    private void renderModel(Texture texture, VertexFormat format, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition)
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

        /* Calculate quad's size (vertices, not UV). The scale sizes the quad the framebuffer is
         * shown on, not what is drawn into it — the body parts always fill the whole texture,
         * so raising it can't push them past the framebuffer's own edges. */
        float scale = this.form.scale.get() * 2F;
        float ratioX = (w > h ? h / w : 1F) * scale;
        float ratioY = (h > w ? w / h : 1F) * scale;
        float TLx = (uvTLx - 0.5F) * ratioY;
        float TLy = -(uvTLy - 0.5F) * ratioX;
        float BRx = (uvBRx - 0.5F) * ratioY;
        float BRy = -(uvBRy - 0.5F) * ratioX;

        quad.p1.set(TLx, TLy, 0F);
        quad.p2.set(BRx, TLy, 0F);
        quad.p3.set(TLx, BRy, 0F);
        quad.p4.set(BRx, BRy, 0F);

        this.renderQuad(format, texture, shader, matrices, overlay, light, overlayColor, transition);
    }

    private void renderQuad(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, format);
        Color color = Color.white();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();

        color.mul(overlayColor);

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableCull();

        BBSModClient.getTextures().bindTexture(texture);
        texture.bind();
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

        gameRenderer.getLightmapTextureManager().disable();
        gameRenderer.getOverlayTexture().teardownOverlayColor();
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

        float width = MathUtils.clamp(this.form.width.get(), 2, 4096);
        float height = MathUtils.clamp(this.form.height.get(), 2, 4096);
        float scale = this.form.scale.get();

        Matrix4f parent = new Matrix4f(stack.peek().getPositionMatrix());
        MatrixStack childStack = new MatrixStack();
        MatrixCache children = new MatrixCache();

        /* The body parts live in the framebuffer's ortho box (-1..1 across the whole texture),
         * and the quad that shows it is that box times the scale and the aspect ratio. */
        float scaleX = scale * (height > width ? width / height : 1F);
        float scaleY = scale * (width > height ? height / width : 1F);

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
}
