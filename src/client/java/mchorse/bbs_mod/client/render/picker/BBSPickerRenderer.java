package mchorse.bbs_mod.client.render.picker;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.GlTexture;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class BBSPickerRenderer
{
    private static final int UBO_SIZE = 32;
    private static int target;
    private static int highlightColor = Colors.WHITE;
    private static final int PROJECTION_UBO_SIZE = 64;
    private static MappableRingBuffer uboRing;
    private static MappableRingBuffer projectionRing;
    private static GpuTextureView targetColor;
    private static GpuTextureView targetDepth;
    private static GpuTextureView lastPickColorView;
    private static GpuSampler pickSampler;
    private static GpuTexture highlightColorTex;
    private static GpuTextureView highlightColorView;
    private static int highlightWidth = -1;
    private static int highlightHeight = -1;
    private static GpuTextureView sampler0View;
    private static GpuSampler sampler0;

    private BBSPickerRenderer()
    {}

    public static void setRenderTarget(GpuTextureView color, GpuTextureView depth)
    {
        BBSPickerRenderer.targetColor = color;
        BBSPickerRenderer.targetDepth = depth;

        if (color != null)
        {
            BBSPickerRenderer.lastPickColorView = color;
        }
    }

    public static void clearRenderTarget()
    {
        BBSPickerRenderer.targetColor = null;
        BBSPickerRenderer.targetDepth = null;
    }

    public static void setSampler0(GpuTextureView view, GpuSampler sampler)
    {
        BBSPickerRenderer.sampler0View = view;
        BBSPickerRenderer.sampler0 = sampler;
    }

    public static void setTarget(int target)
    {
        BBSPickerRenderer.target = target;
    }

    public static int getTarget()
    {
        return target;
    }

    /** Set the ARGB highlight colour picker_preview paints matched pixels with. */
    public static void setHighlightColor(int highlightColor)
    {
        BBSPickerRenderer.highlightColor = highlightColor;
    }

    private static GpuBuffer writeUniform(GpuDevice device, CommandEncoder encoder)
    {
        if (uboRing == null)
        {
            uboRing = new MappableRingBuffer(() -> "bbs:picker_ubo", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, UBO_SIZE);
        }

        uboRing.rotate();

        GpuBuffer ubo = uboRing.getBlocking();

        try (GpuBuffer.MappedView view = encoder.mapBuffer(ubo, false, true))
        {
            Std140Builder.intoBuffer(view.data())
                .putVec4(Colors.getR(highlightColor), Colors.getG(highlightColor), Colors.getB(highlightColor), Colors.getA(highlightColor))
                .putInt(target);
        }

        return ubo;
    }

    public static void bind(RenderPass pass)
    {
        GpuDevice device = RenderSystem.getDevice();

        pass.setUniform(BBSShaders.PICKER_UNIFORM, writeUniform(device, device.createCommandEncoder()));
    }

    public static void draw(RenderPipeline pipeline, BuiltBuffer buffer, Matrix4f modelView)
    {
        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();

        /* DynamicTransforms: modelView + identity colorModulator/offset/textureMatrix, like RenderLayer.draw. */
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .write(modelView, new Vector4f(1F, 1F, 1F, 1F), new Vector3f(), new Matrix4f());

        /* BBSPicker: the Target/HighlightColor block. */
        GpuBuffer pickerUniform = writeUniform(device, encoder);

        VertexFormat format = pipeline.getVertexFormat();
        GpuBuffer vertexBuffer = format.uploadImmediateVertexBuffer(buffer.getBuffer());

        GpuBuffer indexBuffer;
        VertexFormat.IndexType indexType;

        if (buffer.getSortedBuffer() == null)
        {
            RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());

            indexBuffer = sequential.getIndexBuffer(buffer.getDrawParameters().indexCount());
            indexType = sequential.getIndexType();
        }
        else
        {
            indexBuffer = format.uploadImmediateIndexBuffer(buffer.getSortedBuffer());
            indexType = buffer.getDrawParameters().indexType();
        }

        GpuTextureView color;
        GpuTextureView depth;

        if (targetColor != null)
        {
            color = targetColor;
            depth = targetDepth;
        }
        else
        {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

            color = framebuffer.getColorAttachmentView();
            depth = framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null;
        }

        try (RenderPass pass = encoder.createRenderPass(() -> "bbs:picker_draw", color, OptionalInt.empty(), depth, OptionalDouble.empty()))
        {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setUniform(BBSShaders.PICKER_UNIFORM, pickerUniform);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.bindTexture("Sampler0", sampler0View, sampler0);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
        }
        finally
        {
            buffer.close();
        }
    }

    public static void drawColorId(RenderPipeline pipeline, BuiltBuffer buffer, Matrix4f modelView)
    {
        if (targetColor == null)
        {
            buffer.close();

            return;
        }

        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .write(modelView, new Vector4f(1F, 1F, 1F, 1F), new Vector3f(), new Matrix4f());

        VertexFormat format = pipeline.getVertexFormat();
        GpuBuffer vertexBuffer = format.uploadImmediateVertexBuffer(buffer.getBuffer());

        GpuBuffer indexBuffer;
        VertexFormat.IndexType indexType;

        if (buffer.getSortedBuffer() == null)
        {
            RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());

            indexBuffer = sequential.getIndexBuffer(buffer.getDrawParameters().indexCount());
            indexType = sequential.getIndexType();
        }
        else
        {
            indexBuffer = format.uploadImmediateIndexBuffer(buffer.getSortedBuffer());
            indexType = buffer.getDrawParameters().indexType();
        }

        try (RenderPass pass = encoder.createRenderPass(() -> "bbs:gizmo_pick", targetColor, OptionalInt.empty()))
        {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
        }
        finally
        {
            buffer.close();
        }
    }

    private static GpuBufferSlice writeProjection(CommandEncoder encoder, Matrix4f projection)
    {
        if (projectionRing == null)
        {
            projectionRing = new MappableRingBuffer(() -> "bbs:picker_projection_ubo", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, PROJECTION_UBO_SIZE);
        }

        projectionRing.rotate();

        GpuBuffer ubo = projectionRing.getBlocking();

        try (GpuBuffer.MappedView view = encoder.mapBuffer(ubo, false, true))
        {
            Std140Builder.intoBuffer(view.data()).putMat4f(projection);
        }

        return ubo.slice(0L, PROJECTION_UBO_SIZE);
    }

    /** (Re)build the off-screen highlight output target to {@code w x h}. Cheap no-op while unchanged. */
    private static void ensureHighlightTarget(int w, int h)
    {
        if (highlightColorView != null && highlightWidth == w && highlightHeight == h)
        {
            return;
        }

        if (highlightColorView != null)
        {
            highlightColorView.close();
            highlightColorView = null;
        }

        if (highlightColorTex != null)
        {
            highlightColorTex.close();
            highlightColorTex = null;
        }

        highlightColorTex = RenderSystem.getDevice().createTexture("bbs_picker_highlight",
            GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC,
            TextureFormat.RGBA8, w, h, 1, 1);
        highlightColorView = RenderSystem.getDevice().createTextureView(highlightColorTex);

        highlightWidth = w;
        highlightHeight = h;
    }

    public static boolean drawHighlight(int index, int highlightColor, int w, int h)
    {
        if (lastPickColorView == null || w <= 0 || h <= 0)
        {
            return false;
        }

        if (pickSampler == null)
        {
            pickSampler = RenderSystem.getSamplerCache().get(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, false);
        }

        ensureHighlightTarget(w, h);

        setTarget(index);
        setHighlightColor(highlightColor);

        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();

        RenderPipeline pipeline = BBSShaders.getPickerPreviewProgram();

        Matrix4f projection = new Matrix4f().ortho(0F, (float) w, (float) h, 0F, -1000F, 1000F);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .write(new Matrix4f(), new Vector4f(1F, 1F, 1F, 1F), new Vector3f(), new Matrix4f());

        GpuBufferSlice projectionUniform = writeProjection(encoder, projection);
        GpuBuffer pickerUniform = writeUniform(device, encoder);

        int color = Colors.WHITE;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        builder.vertex(0F, (float) h, 0F).texture(0F, 0F).color(color);
        builder.vertex((float) w, (float) h, 0F).texture(1F, 0F).color(color);
        builder.vertex((float) w, 0F, 0F).texture(1F, 1F).color(color);
        builder.vertex(0F, 0F, 0F).texture(0F, 1F).color(color);

        BuiltBuffer buffer = builder.endNullable();

        if (buffer == null)
        {
            return false;
        }

        VertexFormat format = pipeline.getVertexFormat();
        GpuBuffer vertexBuffer = format.uploadImmediateVertexBuffer(buffer.getBuffer());

        RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());
        GpuBuffer indexBuffer = sequential.getIndexBuffer(buffer.getDrawParameters().indexCount());
        VertexFormat.IndexType indexType = sequential.getIndexType();

        try (RenderPass pass = encoder.createRenderPass(() -> "bbs:picker_highlight", highlightColorView, OptionalInt.of(0x00000000)))
        {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Projection", projectionUniform);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setUniform(BBSShaders.PICKER_UNIFORM, pickerUniform);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.bindTexture("Sampler0", lastPickColorView, pickSampler);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
        }
        finally
        {
            buffer.close();
        }

        return true;
    }

    /** Raw GL id of the off-screen highlight colour texture, for the recorded {@code texturedBox(int,...)} blit. */
    public static int getHighlightGlId()
    {
        return highlightColorTex == null ? -1 : ((GlTexture) highlightColorTex).getGlId();
    }

    public static int getHighlightWidth()
    {
        return highlightWidth;
    }

    public static int getHighlightHeight()
    {
        return highlightHeight;
    }
}
