package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class FormOutlineRenderer
{
    private static final float REFERENCE_DISTANCE = 4F;
    private static final float MIN_DISTANCE_SCALE = 0.05F;
    private static final float MAX_DISTANCE_SCALE = 5F;
    private static final float MAX_THICKNESS = 64F;

    private static final ModelOutlineFramebufferCache framebuffers = new ModelOutlineFramebufferCache();

    private static boolean rendering;

    public static final class BodyPartData
    {
        public final Matrix4f relativeTransform;
        public final ModelInstance model;
        public final ShapeKeys shapeKeys;
        public final Function<String, Link> textureResolver;
        public final Pose pose;
        public final List<BodyPartData> children;

        public BodyPartData(Matrix4f relativeTransform, ModelInstance model, ShapeKeys shapeKeys, Function<String, Link> textureResolver, Pose pose, List<BodyPartData> children)
        {
            this.relativeTransform = relativeTransform;
            this.model = model;
            this.shapeKeys = shapeKeys;
            this.textureResolver = textureResolver;
            this.pose = pose;
            this.children = children != null ? children : Collections.emptyList();
        }
    }

    private FormOutlineRenderer()
    {
    }

    /**
     * Draws a single outer-silhouette outline around the given model and its attached body parts.
     */
    public static void render(MatrixStack stack, ModelInstance model, ShapeKeys shapeKeys, Function<String, Link> textureResolver, int light, Color outlineColor, float thickness)
    {
        render(stack, model, shapeKeys, textureResolver, light, outlineColor, thickness, Collections.emptyList());
    }

    /**
     * Draws a single outer-silhouette outline around the given model and its attached body parts.
     */
    public static void render(MatrixStack stack, ModelInstance model, ShapeKeys shapeKeys, Function<String, Link> textureResolver, int light, Color outlineColor, float thickness, List<BodyPartData> bodyParts)
    {
        if (rendering || model == null || thickness <= 0F || outlineColor == null || outlineColor.a <= 0.001F)
        {
            return;
        }

        ShaderProgram maskShader = BBSShaders.getOutlineMask();
        ShaderProgram dilateShader = BBSShaders.getOutlineDilateH();
        ShaderProgram compositeShader = BBSShaders.getOutlineComposite();

        if (maskShader == null || dilateShader == null || compositeShader == null)
        {
            return;
        }

        framebuffers.ensure();

        if (!framebuffers.isReady())
        {
            return;
        }

        Vector3f translation = stack.peek().getPositionMatrix().getTranslation(new Vector3f());
        float distance = translation.length();
        float distanceScale = distance > 0.0001F
            ? MathHelper.clamp(REFERENCE_DISTANCE / distance, MIN_DISTANCE_SCALE, MAX_DISTANCE_SCALE)
            : MAX_DISTANCE_SCALE;

        thickness = MathHelper.clamp(thickness * distanceScale, 0F, MAX_THICKNESS);

        if (thickness <= 0F)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = Math.max(2, client.getWindow().getFramebufferWidth());
        int height = Math.max(2, client.getWindow().getFramebufferHeight());

        framebuffers.updateForScreenSize(width, height);

        Framebuffer framebuffer = framebuffers.getMaskFramebuffer();
        Framebuffer dilateFramebuffer = framebuffers.getDilateFramebuffer();

        rendering = true;

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] previousViewport = new int[4];

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, previousViewport);

        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        try
        {
            framebuffer.bind();
            GL11.glViewport(0, 0, width, height);

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            GL11.glClearColor(0F, 0F, 0F, 0F);
            GL11.glClearDepth(1.0D);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer.id);

            GL30.glBlitFramebuffer(
                0, 0, width, height,
                0, 0, width, height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
            );

            while (GL11.glGetError() != GL11.GL_NO_ERROR)
            {
            }

            framebuffer.bind();
            GL11.glViewport(0, 0, width, height);

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();

            client.gameRenderer.getLightmapTextureManager().enable();
            client.gameRenderer.getOverlayTexture().setupOverlayColor();

            boolean polygonOffsetWasEnabled = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);

            try
            {
                renderMaskGeometry(stack, model, shapeKeys, textureResolver, light, bodyParts);
            }
            finally
            {
                if (!polygonOffsetWasEnabled)
                {
                    GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                }

                GL11.glPolygonOffset(0.0F, 0.0F);

                client.gameRenderer.getLightmapTextureManager().disable();
                client.gameRenderer.getOverlayTexture().teardownOverlayColor();
            }

            Texture maskTexture = framebuffer.getMainTexture();
            float texelX = 1F / Math.max(1, width);
            float texelY = 1F / Math.max(1, height);

            dilateFramebuffer.bind();
            GL11.glViewport(0, 0, width, height);
            GL11.glClearColor(0F, 0F, 0F, -1F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();

            RenderSystem.setShaderTexture(0, maskTexture.id);
            RenderSystem.setShader(BBSShaders::getOutlineDilateH);

            GlUniform hTexelUniform = dilateShader.getUniform("TexelSize");
            GlUniform hThicknessUniform = dilateShader.getUniform("Thickness");

            if (hTexelUniform != null)
            {
                hTexelUniform.set(texelX, texelY);
            }

            if (hThicknessUniform != null)
            {
                hThicknessUniform.set(thickness);
            }

            drawFullscreenQuad(0F);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);

            if (scissorWasEnabled)
            {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }

            Texture dilateTexture = dilateFramebuffer.getMainTexture();

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            RenderSystem.setShaderTexture(0, dilateTexture.id);
            RenderSystem.setShaderTexture(1, maskTexture.id);
            RenderSystem.setShader(BBSShaders::getOutlineComposite);

            GlUniform texelUniform = compositeShader.getUniform("TexelSize");
            GlUniform thicknessUniform = compositeShader.getUniform("Thickness");

            if (texelUniform != null)
            {
                texelUniform.set(texelX, texelY);
            }

            if (thicknessUniform != null)
            {
                thicknessUniform.set(thickness);
            }

            GlUniform colorUniform = compositeShader.getUniform("OutlineMaskColor");

            if (colorUniform != null)
            {
                colorUniform.set(outlineColor.r, outlineColor.g, outlineColor.b, outlineColor.a);
            }

            drawFullscreenQuad(0F);
        }
        finally
        {
            RenderSystem.depthMask(savedDepthMask);

            if (depthWasEnabled)
            {
                RenderSystem.enableDepthTest();
            }
            else
            {
                RenderSystem.disableDepthTest();
            }

            if (blendWasEnabled)
            {
                RenderSystem.enableBlend();
            }
            else
            {
                RenderSystem.disableBlend();
            }

            RenderSystem.enableCull();

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);

            if (scissorWasEnabled)
            {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            else
            {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }

            rendering = false;
        }
    }

    private static void renderMaskGeometry(
        MatrixStack stack,
        ModelInstance model,
        ShapeKeys shapeKeys,
        Function<String, Link> textureResolver,
        int light,
        List<BodyPartData> bodyParts)
    {
        if (model != null)
        {
            model.render(stack, BBSShaders::getOutlineMask, Color.white(), light, OverlayTexture.DEFAULT_UV, (StencilMap) null, shapeKeys, textureResolver);
        }

        if (bodyParts != null && !bodyParts.isEmpty())
        {
            for (BodyPartData part : bodyParts)
            {
                if (part.model == null)
                {
                    continue;
                }

                if (part.pose != null && part.model.model != null)
                {
                    part.model.model.resetPose();
                    part.model.model.applyPose(part.pose);
                }

                MatrixStack partStack = new MatrixStack();

                MatrixStackUtils.multiply(partStack, stack.peek().getPositionMatrix());
                MatrixStackUtils.multiply(partStack, part.relativeTransform);
                partStack.peek().getNormalMatrix().set(stack.peek().getNormalMatrix());

                renderMaskGeometry(partStack, part.model, part.shapeKeys, part.textureResolver, light, part.children);
            }
        }
    }

    private static void drawFullscreenQuad(float z)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

        /* Two triangles covering the full screen in NDC space. */
        builder.vertex(-1F, -1F, z).texture(0F, 0F);
        builder.vertex(1F, -1F, z).texture(1F, 0F);
        builder.vertex(1F, 1F, z).texture(1F, 1F);

        builder.vertex(-1F, -1F, z).texture(0F, 0F);
        builder.vertex(1F, 1F, z).texture(1F, 1F);
        builder.vertex(-1F, 1F, z).texture(0F, 1F);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
}
