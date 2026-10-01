package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.graphics.texture.Texture;

import com.mojang.blaze3d.platform.GlStateManager;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Framebuffers handed out by size and taken back after the draw. Forms that render into a
 * framebuffer of their own ask here instead of keeping one each, so a hundred of them cost
 * as many buffers as there are sizes on screen at once, not a hundred.
 */
public class FramebufferPool
{
    private static final int MAX_IDLE_BUFFERS = 8;
    private static final long MAX_IDLE_BYTES = 128L * 1024L * 1024L;

    private final Set<Framebuffer> idle = new LinkedHashSet<>();
    private final Set<Framebuffer> active = new HashSet<>();
    private long idleBytes;

    private static long getBytes(Framebuffer framebuffer)
    {
        Texture texture = framebuffer.getMainTexture();

        return (long) texture.width * texture.height * 8L;
    }

    /**
     * {@link Texture} construction / sampling setup binds onto the current active unit.
     * Mid-world that unit is often the lightmap or atlas — leaving the new FBO colour
     * texture there blacks out the world for the rest of the frame (visible when
     * FramebufferForm view extent forces a new size allocation).
     */
    private static Framebuffer create(int width, int height)
    {
        int previousDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        Framebuffer framebuffer = new Framebuffer();

        try
        {
            Texture texture = new Texture();

            texture.setSize(width, height);
            ensurePixelSampling(texture);

            Renderbuffer renderbuffer = new Renderbuffer();

            renderbuffer.resize(width, height);

            framebuffer.deleteTextures().attach(texture, GL30.GL_COLOR_ATTACHMENT0);
            framebuffer.attach(renderbuffer);

            return framebuffer;
        }
        catch (RuntimeException | Error e)
        {
            framebuffer.delete();

            throw e;
        }
        finally
        {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(previousBinding);
            GlStateManager._activeTexture(previousActive);
        }
    }

    /**
     * Form FBOs must stay NEAREST (crisp pixelation at low resolution). Iris / other
     * reload paths can rewrite sampler state on existing texture ids; re-assert here.
     * Always restores the previous active unit and TU0 binding.
     */
    private static void ensurePixelSampling(Texture texture)
    {
        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try
        {
            texture.bind();
            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);
            texture.setParameter(GL30.GL_TEXTURE_MAX_LEVEL, 0);
        }
        finally
        {
            GlStateManager._bindTexture(previousBinding);
            GlStateManager._activeTexture(previousActive);
        }
    }

    public Framebuffer get(int width, int height)
    {
        Iterator<Framebuffer> iterator = this.idle.iterator();

        while (iterator.hasNext())
        {
            Framebuffer framebuffer = iterator.next();
            Texture texture = framebuffer.getMainTexture();

            if (texture.width == width && texture.height == height)
            {
                iterator.remove();
                this.idleBytes -= getBytes(framebuffer);
                this.active.add(framebuffer);
                ensurePixelSampling(texture);

                return framebuffer;
            }
        }

        Framebuffer framebuffer = create(width, height);

        this.active.add(framebuffer);

        return framebuffer;
    }

    public void release(Framebuffer framebuffer)
    {
        if (!this.active.remove(framebuffer))
        {
            return;
        }

        this.idle.add(framebuffer);
        this.idleBytes += getBytes(framebuffer);

        Iterator<Framebuffer> iterator = this.idle.iterator();

        while (this.idle.size() > MAX_IDLE_BUFFERS || this.idleBytes > MAX_IDLE_BYTES)
        {
            Framebuffer oldest = iterator.next();

            iterator.remove();
            this.idleBytes -= getBytes(oldest);
            unbindIfBound(oldest);
            oldest.delete();
        }
    }

    /**
     * Deleting a texture that is still bound to TU0 leaves a stale name until the next
     * bind — scrubbing view extent churns sizes and can hit this path every change.
     */
    private static void unbindIfBound(Framebuffer framebuffer)
    {
        Texture texture = framebuffer.getMainTexture();

        if (texture == null || texture.id <= 0)
        {
            return;
        }

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        if (GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == texture.id)
        {
            GlStateManager._bindTexture(0);
        }

        GlStateManager._activeTexture(previousActive);
    }

    public void delete()
    {
        for (Framebuffer framebuffer : this.idle)
        {
            framebuffer.delete();
        }

        for (Framebuffer framebuffer : this.active)
        {
            framebuffer.delete();
        }

        this.idle.clear();
        this.active.clear();
        this.idleBytes = 0L;
    }
}
