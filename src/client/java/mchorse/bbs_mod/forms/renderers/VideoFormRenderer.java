package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.video.VideoFormPlayback;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.VideoForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Supplier;

/**
 * Renders {@link VideoForm} as a dual-faced textured quad.
 * Frames come from ffmpeg ({@link VideoFormPlayback}), not WaterMedia.
 */
public class VideoFormRenderer extends FormRenderer<VideoForm> implements ITickable
{
    private static final Quad QUAD = new Quad();
    private static final float FACE_Z_BIAS = 0.0005F;
    private static final int PLACEHOLDER_COLOR = 0xFF33CCFF;

    public VideoFormRenderer(VideoForm form)
    {
        super(form);
    }

    @Override
    public void tick(IEntity entity)
    {}

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

        this.renderModel(stack, Colors.WHITE, context.getTransition(), null, true, true, null);

        DiffuseLighting.disableGuiDepthLighting();
        stack.pop();
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        if (context.isShadowPass || BBSRendering.isIrisShadowPass())
        {
            return;
        }

        this.renderModel(context.stack, context.color, context.getTransition(), context.camera,
            false, context.modelRenderer || context.isPicking(), context);
    }

    private void renderModel(MatrixStack matrices, int overlayColor, float transition, Camera camera,
        boolean invertY, boolean modelRenderer, FormRenderingContext deferContext)
    {
        String path = this.form.video.get();

        if (path == null || path.isEmpty() || path.equalsIgnoreCase("none") || path.startsWith("<"))
        {
            return;
        }

        IEntity entity = deferContext != null ? deferContext.entity : null;
        float age = entity != null
            ? entity.getAge() + transition
            : (float) (System.currentTimeMillis() / 50.0D);
        long tickPosition = (long) (age * this.form.speed.get()) + this.form.offset.get();

        VideoFormPlayback playback = VideoFormPlayback.get(path);
        Texture texture = playback == null ? null : playback.ensureFrame(tickPosition, 1F, this.form.loop.get());

        float w = 16F;
        float h = 9F;

        if (playback != null && playback.getWidth() > 0 && playback.getHeight() > 0)
        {
            w = playback.getWidth();
            h = playback.getHeight();
        }

        float ratioX = w > h ? h / w : 1F;
        float ratioY = h > w ? w / h : 1F;
        float halfW = 0.5F * ratioY;
        float fullH = ratioX;

        QUAD.p1.set(-halfW, fullH, 0F);
        QUAD.p2.set(halfW, fullH, 0F);
        QUAD.p3.set(-halfW, 0F, 0F);
        QUAD.p4.set(halfW, 0F, 0F);

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
            matrices.peek().getNormalMatrix().identity();
        }

        Color tint = new Color().set(overlayColor, true);
        Color formColor = this.form.getFormColor().copyWithBlendIntensity();

        tint.mul(formColor);
        this.form.applyFormOpacity(tint);

        if (tint.a <= 0.001F)
        {
            return;
        }

        Matrix4f positionMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
        Color tintSnapshot = tint.copy();
        Quad localQuad = new Quad();

        localQuad.copy(QUAD);

        Texture textureSnapshot = texture;
        boolean linear = this.form.linear.get();
        boolean picking = deferContext != null && deferContext.isPicking();
        Supplier<ShaderProgram> pickShader = picking
            ? this.getShader(deferContext, GameRenderer::getPositionTexColorProgram, BBSShaders::getPickerBillboardNoShadingProgram)
            : null;

        Runnable draw = () ->
        {
            if (textureSnapshot != null && textureSnapshot.isValid() && !picking)
            {
                this.drawVideoTexture(positionMatrix, tintSnapshot, localQuad, textureSnapshot, linear);
            }
            else if (picking && pickShader != null)
            {
                this.drawSolidQuad(positionMatrix, tintSnapshot, localQuad, pickShader);
            }
            else
            {
                Color placeholder = Color.rgba(PLACEHOLDER_COLOR);

                placeholder.a = tintSnapshot.a;
                this.drawSolidQuad(positionMatrix, placeholder, localQuad, GameRenderer::getPositionColorProgram);
            }
        };

        boolean irisPass = deferContext != null
            && BBSRendering.isIrisWorldModelPass()
            && !BBSRendering.isIrisShadowPass();

        if (irisPass)
        {
            ModelVAORenderer.submitDeferredTranslucentModel(draw, false, false);
            return;
        }

        draw.run();
    }

    private void drawVideoTexture(Matrix4f matrix, Color tint, Quad quad, Texture texture, boolean linear)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(tint.r, tint.g, tint.b, tint.a);

        texture.bind();
        texture.setFilter(linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        RenderSystem.setShaderTexture(0, texture.id);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

        this.tex(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F);
        this.tex(buffer, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, 1F, 1F);
        this.tex(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F);

        this.tex(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F);
        this.tex(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F);
        this.tex(buffer, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, 0F, 0F);

        this.tex(buffer, matrix, quad.p1.x, quad.p1.y, -FACE_Z_BIAS, 0F, 0F);
        this.tex(buffer, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, 1F, 0F);
        this.tex(buffer, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, 0F, 1F);

        this.tex(buffer, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, 1F, 0F);
        this.tex(buffer, matrix, quad.p4.x, quad.p4.y, -FACE_Z_BIAS, 1F, 1F);
        this.tex(buffer, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, 0F, 1F);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        texture.unbind();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void drawSolidQuad(Matrix4f matrix, Color color, Quad quad, Supplier<ShaderProgram> shader)
    {
        RenderSystem.setShader(shader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        this.col(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, color);

        this.col(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color);

        this.col(buffer, matrix, quad.p1.x, quad.p1.y, -FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, color);

        this.col(buffer, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p4.x, quad.p4.y, -FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
    }

    private void tex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float u, float v)
    {
        buffer.vertex(matrix, x, y, z).texture(u, v);
    }

    private void col(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color color)
    {
        buffer.vertex(matrix, x, y, z).color(color.r, color.g, color.b, color.a);
    }
}
