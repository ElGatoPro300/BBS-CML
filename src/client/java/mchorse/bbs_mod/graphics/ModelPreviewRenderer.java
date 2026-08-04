package mchorse.bbs_mod.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.util.Identifier;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

/**
 * Off-screen 3D model preview target for the in-panel viewports (1.21.11 port).
 */
public class ModelPreviewRenderer
{
    private static final int USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
        | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;

    public static boolean ACTIVE = false;
    public static Identifier TEXTURE = null;

    private final RawProjectionMatrix projection = new RawProjectionMatrix("bbs_model_preview");

    private GpuTexture color;
    private GpuTexture depth;
    private GpuTextureView colorView;
    private GpuTextureView depthView;
    private int width = -1;
    private int height = -1;

    private void resize(int w, int h)
    {
        if (this.color != null && this.width == w && this.height == h)
        {
            return;
        }

        this.releaseTextures();

        GpuDevice device = RenderSystem.getDevice();

        this.color = device.createTexture("bbs_preview_color", USAGE, TextureFormat.RGBA8, w, h, 1, 1);
        this.depth = device.createTexture("bbs_preview_depth", USAGE, TextureFormat.DEPTH32, w, h, 1, 1);
        this.colorView = device.createTextureView(this.color);
        this.depthView = device.createTextureView(this.depth);

        this.width = w;
        this.height = h;
    }

    public void begin(int w, int h, Matrix4f projectionMatrix)
    {
        this.resize(w, h);

        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(this.color, 0x00000000, this.depth, 1.0D);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(this.projection.set(projectionMatrix), ProjectionType.PERSPECTIVE);

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        RenderSystem.outputColorTextureOverride = this.colorView;
        RenderSystem.outputDepthTextureOverride = this.depthView;
    }

    public void end()
    {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    public int getColorGlId()
    {
        return ((GlTexture) this.color).getGlId();
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    private void releaseTextures()
    {
        if (this.colorView != null)
        {
            this.colorView.close();
            this.colorView = null;
        }

        if (this.depthView != null)
        {
            this.depthView.close();
            this.depthView = null;
        }

        if (this.color != null)
        {
            this.color.close();
            this.color = null;
        }

        if (this.depth != null)
        {
            this.depth.close();
            this.depth = null;
        }
    }

    public void close()
    {
        this.releaseTextures();
        this.projection.close();
        this.width = this.height = -1;
    }
}
