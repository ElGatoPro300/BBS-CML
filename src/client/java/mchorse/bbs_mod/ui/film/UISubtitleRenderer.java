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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
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

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        /* Legacy subtitles use half-resolution framebuffer pixels, independent of GUI scale. */
        net.minecraft.client.gl.Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int subtitleWidth = main.textureWidth / 2;
        int subtitleHeight = main.textureHeight / 2;
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
                    textWidth = Math.max(textWidth, font.getWidth(line.trim()));
                }

                float textHeight = (lines.size() - 1) * subtitle.lineHeight + font.fontHeight - 2;
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

    private static void bakeText(TextRenderer font, Subtitle subtitle, List<String> lines, int width, float height, int textureWidth, int textureHeight)
    {
        /* RenderLayer uses GPU attachment overrides in 1.21.11, not the raw GL binding. */
        TEXT_TARGET.begin(textureWidth, textureHeight, new Matrix4f().ortho(0F, width + 10F, height + 10F, 0F, -100F, 100F));

        try (BufferAllocator allocator = new BufferAllocator(786432))
        {
            VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);
            Matrix4f matrix = new Matrix4f();
            float y = 5F;

            for (String line : lines)
            {
                line = line.trim();
                int lineWidth = font.getWidth(line);
                int x = 5 + (width - lineWidth) / 2;

                if (Colors.getA(subtitle.backgroundColor) > 0F)
                {
                    float offset = subtitle.backgroundOffset;
                    VertexConsumer vertices = consumers.getBuffer(Draw.getPositionColorNoDepthLayer());
                    float left = x - offset;
                    float top = y - offset;
                    float right = x + lineWidth + offset - 1;
                    float bottom = y + font.fontHeight - 2 + offset;
                    int color = subtitle.backgroundColor;

                    vertices.vertex(left, top, 0F).color(color);
                    vertices.vertex(left, bottom, 0F).color(color);
                    vertices.vertex(right, bottom, 0F).color(color);
                    vertices.vertex(left, top, 0F).color(color);
                    vertices.vertex(right, bottom, 0F).color(color);
                    vertices.vertex(right, top, 0F).color(color);
                    consumers.draw();
                }

                font.draw(line, x, y, Colors.setA(subtitle.color, 1F), subtitle.textShadow, matrix,
                    consumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                consumers.draw();
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
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        builder.vertex(x, y, 0F).texture(0F, 1F).color(color);
        builder.vertex(x, y + textureHeight, 0F).texture(0F, 0F).color(color);
        builder.vertex(x + textureWidth, y + textureHeight, 0F).texture(1F, 0F).color(color);
        builder.vertex(x + textureWidth, y, 0F).texture(1F, 1F).color(color);

        try (BuiltBuffer buffer = builder.end(); MemoryStack memory = MemoryStack.stackPush())
        {
            ByteBuffer data = Std140Builder.onStack(memory, 80).putMat4f(transform)
                .putFloat(subtitle.shadow).putFloat(subtitle.shadowOpaque ? 1F : 0F)
                .putFloat(textureWidth).putFloat(textureHeight).get();
            GpuBuffer vertices = VertexFormats.POSITION_TEXTURE_COLOR.uploadImmediateVertexBuffer(buffer.getBuffer());
            RenderSystem.ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
            GpuBuffer indexBuffer = indices.getIndexBuffer(6);
            GpuTextureView destination = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();

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

    public static void renderSubtitles(MatrixStack stack, Batcher2D batcher, List<Subtitle> subtitles)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        renderSubtitles(batcher, subtitles, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    public static void renderSubtitle(MatrixStack stack, Batcher2D batcher, Subtitle subtitle)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        renderSubtitle(batcher, subtitle, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }
}
