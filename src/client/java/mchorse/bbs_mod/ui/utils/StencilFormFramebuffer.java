package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.Renderbuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.Pair;

import net.minecraft.client.texture.GlTexture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * The off-screen colour target the picker shaders render the per-form/per-bone index colours into.
 */
public class StencilFormFramebuffer
{
    private Framebuffer framebuffer;

    private int index;
    private Map<Integer, Pair<Form, String>> indexMap = new HashMap<>();

    private GpuTexture colorTexture;
    private GpuTextureView colorView;
    private GpuTexture depthTexture;
    private GpuTextureView depthView;
    private int gpuWidth = -1;
    private int gpuHeight = -1;

    private int readFbo = -1;

    public Framebuffer getFramebuffer()
    {
        return this.framebuffer;
    }

    public int getIndex()
    {
        return this.index;
    }

    public Map<Integer, Pair<Form, String>> getIndexMap()
    {
        return this.indexMap;
    }

    public Pair<Form, String> getPicked()
    {
        return this.indexMap.get(this.index);
    }

    public void setup(Link id)
    {
        if (this.framebuffer != null)
        {
            return;
        }

        this.framebuffer = BBSModClient.getFramebuffers().getFramebuffer(id, (framebuffer) ->
        {
            Texture texture = new Texture();

            texture.setSize(2, 2);
            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

            Renderbuffer renderbuffer = new Renderbuffer();

            renderbuffer.resize(2, 2);

            framebuffer.deleteTextures().attach(texture, GL30.GL_COLOR_ATTACHMENT0);
            framebuffer.attach(renderbuffer);
            framebuffer.unbind();
        });
    }

    public void resizeGUI(int w, int h)
    {
        this.resize(w, h, BBSModClient.getGUIScale());
    }

    public void resize(int w, int h, int scale)
    {
        this.resize(w * scale, h * scale);
    }

    public void resize(int w, int h)
    {
        if (this.framebuffer != null)
        {
            this.framebuffer.resize(w, h);
        }
    }

    private void ensureGpuTargets()
    {
        Texture texture = this.framebuffer.getMainTexture();
        int w = Math.max(1, texture.width);
        int h = Math.max(1, texture.height);

        if (this.colorView != null && this.gpuWidth == w && this.gpuHeight == h)
        {
            return;
        }

        this.releaseGpuTargets();

        this.colorTexture = RenderSystem.getDevice().createTexture("bbs_stencil_color",
            GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC,
            TextureFormat.RGBA8, w, h, 1, 1);
        this.colorView = RenderSystem.getDevice().createTextureView(this.colorTexture);

        this.depthTexture = RenderSystem.getDevice().createTexture("bbs_stencil_depth",
            GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, w, h, 1, 1);
        this.depthView = RenderSystem.getDevice().createTextureView(this.depthTexture);

        this.gpuWidth = w;
        this.gpuHeight = h;
    }

    public void apply()
    {
        this.ensureGpuTargets();

        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(this.colorTexture, 0x00000000, this.depthTexture, 1.0D);

        BBSPickerRenderer.setRenderTarget(this.colorView, this.depthView);
    }

    public void pickGUI(UIContext context, Area area)
    {
        this.pickGUI(context, area, BBSModClient.getGUIScale(), BBSModClient.getGUIScale());
    }

    public void pickGUI(UIContext context, Area area, float scaleX, float scaleY)
    {
        int localX = context.mouseX - context.globalX(area.x);
        int localY = context.mouseY - context.globalY(area.y);

        if (localX < 0 || localY < 0 || localX >= area.w || localY >= area.h)
        {
            this.index = 0;

            return;
        }

        this.pick(Math.round(localX * scaleX), Math.round((area.h - localY) * scaleY));
    }

    public void pickGUI(int x, int y)
    {
        this.pick(x * BBSModClient.getGUIScale(), y * BBSModClient.getGUIScale());
    }

    public void pick(int x, int y)
    {
        if (this.colorTexture == null)
        {
            this.index = 0;

            return;
        }

        if (this.readFbo < 0)
        {
            this.readFbo = GL30.glGenFramebuffers();
        }

        int glId = ((GlTexture) this.colorTexture).getGlId();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.readFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, glId, 0);

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            FloatBuffer floats = stack.mallocFloat(4);

            GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, floats);

            int r = (int) (floats.get() * 255F);
            int g = (int) (floats.get() * 255F);
            int b = (int) (floats.get() * 255F);
            int a = (int) (floats.get() * 255F);

            this.index = a < 1F ? 0 : r | (g << 8) | (b << 16);
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void unbind(StencilMap map)
    {
        this.unbind();

        this.indexMap.clear();
        this.indexMap.putAll(map.indexMap);
    }

    public void unbind()
    {
        BBSPickerRenderer.clearRenderTarget();
    }

    public void clearPicking()
    {
        this.index = 0;
        this.indexMap.clear();
    }

    public boolean hasPicked()
    {
        return this.index > 0;
    }


    private void releaseGpuTargets()
    {
        if (this.colorView != null)
        {
            this.colorView.close();
            this.colorView = null;
        }

        if (this.colorTexture != null)
        {
            this.colorTexture.close();
            this.colorTexture = null;
        }

        if (this.depthView != null)
        {
            this.depthView.close();
            this.depthView = null;
        }

        if (this.depthTexture != null)
        {
            this.depthTexture.close();
            this.depthTexture = null;
        }
    }
}
