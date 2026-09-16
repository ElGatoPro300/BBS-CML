package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureFormat;
import mchorse.bbs_mod.resources.Link;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

public class ModelOutlineFramebufferCache
{
    private static final Link FRAMEBUFFER_ID = Link.bbs("outline_mask_32f");
    private static final Link DILATE_FRAMEBUFFER_ID = Link.bbs("outline_mask_dilate_h_32f");

    private Framebuffer maskFramebuffer;
    private Framebuffer dilateFramebuffer;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public void ensure()
    {
        if (this.maskFramebuffer == null)
        {
            this.maskFramebuffer = BBSModClient.getFramebuffers().getFramebuffer(FRAMEBUFFER_ID, (fb) ->
            {
                Texture texture = new Texture();

                texture.setFormat(TextureFormat.RG_F32);
                texture.setSize(2, 2);
                texture.setFilter(GL11.GL_NEAREST);
                texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

                Texture depth = new Texture();

                depth.setFormat(TextureFormat.DEPTH_F24);
                depth.setSize(2, 2);
                depth.setFilter(GL11.GL_NEAREST);
                depth.setWrap(GL13.GL_CLAMP_TO_EDGE);

                fb.deleteTextures().attach(texture, GL30.GL_COLOR_ATTACHMENT0);
                fb.attach(depth, GL30.GL_DEPTH_ATTACHMENT);
                fb.unbind();
            });
        }

        if (this.dilateFramebuffer == null)
        {
            this.dilateFramebuffer = BBSModClient.getFramebuffers().getFramebuffer(DILATE_FRAMEBUFFER_ID, (fb) ->
            {
                Texture texture = new Texture();

                texture.bind();
                texture.setFormat(TextureFormat.RG_F32);
                texture.setSize(2, 2);
                texture.setFilter(GL11.GL_NEAREST);
                texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

                fb.deleteTextures().attach(texture, GL30.GL_COLOR_ATTACHMENT0);
                fb.unbind();
            });
        }
    }

    public boolean isReady()
    {
        return this.maskFramebuffer != null && this.dilateFramebuffer != null;
    }

    public Framebuffer getMaskFramebuffer()
    {
        return this.maskFramebuffer;
    }

    public Framebuffer getDilateFramebuffer()
    {
        return this.dilateFramebuffer;
    }

    /**
     * Resizes the buffers
     */
    public void updateForScreenSize(int width, int height)
    {
        if (width == this.lastWidth && height == this.lastHeight)
        {
            return;
        }

        this.lastWidth = width;
        this.lastHeight = height;

        this.maskFramebuffer.resize(width, height);
        this.dilateFramebuffer.resize(width, height);
    }
}
