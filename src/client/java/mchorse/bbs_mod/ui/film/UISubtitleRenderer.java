package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.ModelPreviewRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

import org.joml.Matrix4f;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class UISubtitleRenderer
{
    private static final ModelPreviewRenderer TEXT_TARGET = new ModelPreviewRenderer();

    static
    {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TEXT_TARGET.close());
    }

    public static void renderSubtitles(Batcher2D batcher, List<Subtitle> subtitles, int width, int height)
    {
        if (subtitles == null || subtitles.isEmpty() || width <= 0 || height <= 0)
        {
            return;
        }

        /* GUI commands must finish before the immediate subtitle pass. */
        BBSRendering.flushGuiRenderState();

        Font font = Minecraft.getInstance().font;
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        /* Legacy subtitles use half-resolution framebuffer pixels, independent of GUI scale. */
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        int subtitleWidth = main.width / 2;
        int subtitleHeight = main.height / 2;
        float depth = Math.max(1000F, Math.max(subtitleWidth, subtitleHeight) * 8F);
        Matrix4f projection = new Matrix4f().ortho(0F, subtitleWidth, subtitleHeight, 0F, -depth, depth);

        try
        {
            for (Subtitle subtitle : subtitles)
            {
                if (Colors.getA(subtitle.color) <= 0F || subtitle.size <= 0F)
                {
                    continue;
                }

                String label = StringUtils.processColoredText(subtitle.label);
                List<String> lines = subtitle.maxWidth <= 10F ? Collections.singletonList(label)
                    : FontRenderer.wrap(font, label, Math.max(0, Math.round(subtitle.maxWidth)));
                int textWidth = 0;

                for (String line : lines)
                {
                    textWidth = Math.max(textWidth, font.width(line.trim()));
                }

                float textHeight = (lines.size() - 1) * subtitle.lineHeight + font.lineHeight - 2;
                int textureWidth = (int) ((textWidth + 10) * subtitle.size);
                int textureHeight = (int) ((textHeight + 10) * subtitle.size);

                if (textureWidth <= 0 || textureHeight <= 0)
                {
                    continue;
                }

                bakeText(font, subtitle, lines, textWidth, textHeight, textureWidth, textureHeight);
                drawSubtitle(subtitle, projection, subtitleWidth, subtitleHeight, textureWidth, textureHeight);
            }
        }
        finally
        {
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }

    private static void bakeText(Font font, Subtitle subtitle, List<String> lines, int width, float height, int textureWidth, int textureHeight)
    {
        /* RenderLayer uses GPU attachment overrides in 1.21.11, not the raw GL binding. */
        TEXT_TARGET.begin(textureWidth, textureHeight, new Matrix4f().ortho(0F, width + 10F, height + 10F, 0F, -100F, 100F));

        try (ByteBufferBuilder allocator = new ByteBufferBuilder(786432))
        {
            MultiBufferSource.BufferSource consumers = MultiBufferSource.immediate(allocator);
            Matrix4f matrix = new Matrix4f();
            float y = 5F;

            for (String line : lines)
            {
                line = line.trim();
                int lineWidth = font.width(line);
                int x = 5 + (width - lineWidth) / 2;

                if (Colors.getA(subtitle.backgroundColor) > 0F)
                {
                    float offset = subtitle.backgroundOffset;
                    VertexConsumer vertices = consumers.getBuffer(Draw.getPositionColorNoDepthLayer());
                    float left = x - offset;
                    float top = y - offset;
                    float right = x + lineWidth + offset - 1;
                    float bottom = y + font.lineHeight - 2 + offset;
                    int color = subtitle.backgroundColor;

                    vertices.addVertex(left, top, 0F).setColor(color);
                    vertices.addVertex(left, bottom, 0F).setColor(color);
                    vertices.addVertex(right, bottom, 0F).setColor(color);
                    vertices.addVertex(left, top, 0F).setColor(color);
                    vertices.addVertex(right, bottom, 0F).setColor(color);
                    vertices.addVertex(right, top, 0F).setColor(color);
                    consumers.endBatch();
                }

                font.drawInBatch(line, x, y, Colors.setA(subtitle.color, 1F), subtitle.textShadow, matrix,
                    consumers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                consumers.endBatch();
                y += subtitle.lineHeight;
            }
        }
        finally
        {
            TEXT_TARGET.end();
        }
    }

    private static void drawSubtitle(Subtitle subtitle, Matrix4f projection, int width, int height, int textureWidth, int textureHeight)
    {
        Matrix4f transform = new Matrix4f(projection)
            .translate(width * subtitle.windowX + subtitle.x, height * subtitle.windowY + subtitle.y, 0F)
            .rotateX((float) Math.toRadians(subtitle.rotationX))
            .rotateY((float) Math.toRadians(subtitle.rotationY))
            .rotateZ((float) Math.toRadians(subtitle.rotation));
        float x = -textureWidth * subtitle.anchorX;
        float y = -textureHeight * subtitle.anchorY;
        int color = Colors.setA(Colors.WHITE, Colors.getA(subtitle.color));
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.DrawMode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        builder.addVertex(x, y, 0F).setUv(0F, 1F).setColor(color);
        builder.addVertex(x, y + textureHeight, 0F).setUv(0F, 0F).setColor(color);
        builder.addVertex(x + textureWidth, y + textureHeight, 0F).setUv(1F, 0F).setColor(color);
        builder.addVertex(x + textureWidth, y, 0F).setUv(1F, 1F).setColor(color);

        try (MeshData buffer = builder.buildOrThrow(); MemoryStack memory = MemoryStack.stackPush())
        {
            ByteBuffer data = Std140Builder.onStack(memory, 80).putMat4f(transform)
                .putFloat(subtitle.shadow).putFloat(subtitle.shadowOpaque ? 1F : 0F)
                .putFloat(textureWidth).putFloat(textureHeight).get();
            GpuBuffer vertices = DefaultVertexFormat.POSITION_TEX_COLOR.uploadImmediateVertexBuffer(buffer.vertexBuffer());
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
            GpuBuffer indexBuffer = indices.getIndexBuffer(6);
            GpuTextureView destination = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : Minecraft.getInstance().getMainRenderTarget().getColorTextureView();

            try (GpuBuffer uniforms = RenderSystem.getDevice().createBuffer(() -> "BBS subtitle parameters", GpuBuffer.USAGE_UNIFORM, data);
                 RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "BBS subtitle", destination, OptionalInt.empty()))
            {
                pass.setPipeline(BBSShaders.subtitlesPipeline);
                pass.setUniform("SubtitleParameters", uniforms);
                pass.bindTexture("Sampler0", TEXT_TARGET.getColorView(), RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, false));
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indexBuffer, indices.getIndexType());
                pass.drawIndexed(0, 0, 6, 1);
            }
        }
    }

    public static void renderSubtitle(Batcher2D batcher, Subtitle subtitle, int width, int height)
    {
        if (subtitle != null)
        {
            renderSubtitles(batcher, Collections.singletonList(subtitle), width, height);
        }
    }

    public static void renderSubtitles(PoseStack stack, Batcher2D batcher, List<Subtitle> subtitles)
    {
        Minecraft client = Minecraft.getInstance();
        renderSubtitles(batcher, subtitles, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }

    public static void renderSubtitle(PoseStack stack, Batcher2D batcher, Subtitle subtitle)
    {
        Minecraft client = Minecraft.getInstance();
        renderSubtitle(batcher, subtitle, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }
}
