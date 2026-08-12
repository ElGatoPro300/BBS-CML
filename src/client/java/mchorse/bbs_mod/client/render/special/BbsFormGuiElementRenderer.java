package mchorse.bbs_mod.client.render.special;

import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.graphics.ModelPreviewRenderer;

import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderPipelines;

import org.joml.Vector3f;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders BBS form thumbnails into off-screen FBOs and composites them into form-list cells.
 */
public class BbsFormGuiElementRenderer extends PictureInPictureRenderer<BbsFormGuiElementRenderState>
{
    private static int errorLog;

    private final CachedOrthoProjectionMatrixBuffer projection = new CachedOrthoProjectionMatrixBuffer("PIP - bbs form", -1000.0F, 1000.0F, true);

    private final Map<String, Target> targets = new HashMap<>();

    private GpuBuffer lightsBuffer;
    private GpuBufferSlice lights;

    public BbsFormGuiElementRenderer(BufferSource vertexConsumers)
    {
        super(vertexConsumers);
    }

    @Override
    public Class<BbsFormGuiElementRenderState> getRenderStateClass()
    {
        return BbsFormGuiElementRenderState.class;
    }

    @Override
    public void render(BbsFormGuiElementRenderState state, GuiRenderState guiState, int windowScaleFactor)
    {
        int w = (state.x1() - state.x0()) * windowScaleFactor;
        int h = (state.y1() - state.y0()) * windowScaleFactor;

        if (w <= 0 || h <= 0)
        {
            return;
        }

        Target target = this.acquire(state.renderer(), w, h);

        RenderSystem.outputColorTextureOverride = target.colorView;
        RenderSystem.outputDepthTextureOverride = target.depthView;
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.color, 0, target.depth, 1.0);
        RenderSystem.setProjectionMatrix(this.projection.getBuffer(w, h), ProjectionType.ORTHOGRAPHIC);

        PoseStack matrices = new PoseStack();

        matrices.translate(w / 2.0F, this.getTranslateY(h, windowScaleFactor), 0.0F);

        float f = windowScaleFactor * state.scale();

        matrices.scale(f, f, -f);

        this.render(state, matrices);
        this.bufferSource.endBatch();

        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;

        guiState.submitBlitToCurrentLayer(new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(target.colorView, RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST)),
            state.pose(),
            state.x0(), state.y0(), state.x1(), state.y1(),
            0.0F, 1.0F, 1.0F, 0.0F,
            -1,
            state.scissorArea()));
    }

    @Override
    protected void render(BbsFormGuiElementRenderState state, PoseStack matrices)
    {
        RenderSystem.setShaderLights(this.lights());

        boolean prevActive = ModelPreviewRenderer.ACTIVE;

        ModelPreviewRenderer.ACTIVE = true;

        /* 1.21.11: FormRenderer has no renderUIPreview method yet */
        // try
        // {
        //     state.renderer().renderUIPreview(matrices, state.angle(), state.transition(),
        //         state.x1(), state.y1(), state.x2(), state.y2());
        // }
        // catch (Exception e)
        // {
        //     if (errorLog++ % 120 == 0)
        //     {
        //         System.out.println("[BBS list preview] renderUIPreview failed: " + e);
        //     }
        // }

        ModelPreviewRenderer.TEXTURE = null;
        ModelPreviewRenderer.ACTIVE = prevActive;
    }

    private GpuBufferSlice lights()
    {
        if (this.lights == null)
        {
            Vector3f lightA = new Vector3f(0F, 1F, -0.2F).normalize();
            Vector3f lightB = new Vector3f(-0.85F, 0.85F, 1F).normalize();

            try (MemoryStack stack = MemoryStack.stackPush())
            {
                ByteBuffer data = Std140Builder.onStack(stack, Lighting.UBO_SIZE)
                    .putVec3(lightA)
                    .putVec3(lightB)
                    .get();

                this.lightsBuffer = RenderSystem.getDevice().createBuffer(() -> "BBS form preview lights UBO", 136, data);
                this.lights = this.lightsBuffer.slice(0, Lighting.UBO_SIZE);
            }
        }

        return this.lights;
    }

    private Target acquire(FormRenderer<?> key, int w, int h)
    {
        String id = System.identityHashCode(key) + "_" + w + "x" + h;
        Target target = this.targets.get(id);

        if (target != null)
        {
            return target;
        }

        GpuDevice device = RenderSystem.getDevice();

        target = new Target();
        target.color = device.createTexture(() -> "BBS form thumbnail", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, w, h, 1, 1);
        target.colorView = device.createTextureView(target.color);
        target.depth = device.createTexture(() -> "BBS form thumbnail depth", GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, w, h, 1, 1);
        target.depthView = device.createTextureView(target.depth);

        this.targets.put(id, target);

        return target;
    }

    @Override
    protected float getTranslateY(int height, int windowScaleFactor)
    {
        return 0.85F * height;
    }

    @Override
    protected String getTextureLabel()
    {
        return "bbs form";
    }

    @Override
    public void close()
    {
        super.close();

        for (Target target : this.targets.values())
        {
            target.colorView.close();
            target.color.close();
            target.depthView.close();
            target.depth.close();
        }

        this.targets.clear();
        this.projection.close();

        if (this.lightsBuffer != null)
        {
            this.lightsBuffer.close();
            this.lightsBuffer = null;
            this.lights = null;
        }
    }

    private static final class Target
    {
        private GpuTexture color;
        private GpuTextureView colorView;
        private GpuTexture depth;
        private GpuTextureView depthView;
    }
}
