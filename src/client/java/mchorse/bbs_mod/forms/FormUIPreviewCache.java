package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.Renderbuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;

import net.minecraft.client.MinecraftClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caches form UI previews (color + depth) keyed by form edit revision and orbit angle.
 * Avoids re-rendering every replay list row every frame while preserving mouse-orbit behavior.
 */
public final class FormUIPreviewCache
{
    private static final int MAX_ENTRIES = 96;
    private static final int ANGLE_BUCKETS = 64;

    private static final Map<Long, CacheEntry> CACHE = new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CacheEntry> eldest)
        {
            if (this.size() > MAX_ENTRIES)
            {
                eldest.getValue().delete();

                return true;
            }

            return false;
        }
    };

    private static Framebuffer scratchFramebuffer;
    private static Texture scratchTexture;
    private static Renderbuffer scratchDepth;
    private static int scratchWidth;
    private static int scratchHeight;

    private FormUIPreviewCache()
    {}

    public static void clear()
    {
        for (CacheEntry entry : CACHE.values())
        {
            entry.delete();
        }

        CACHE.clear();
    }

    public static void render(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        if (form == null)
        {
            return;
        }

        int width = x2 - x1;
        int height = y2 - y1;

        if (width <= 0 || height <= 0)
        {
            return;
        }

        int revision = form.getEditRevision();
        int angleBucket = getAngleBucket(context, x1, x2);
        long key = buildKey(form, width, height, angleBucket);
        CacheEntry entry = CACHE.get(key);

        if (entry != null && entry.revision == revision && entry.texture != null && entry.texture.isValid())
        {
            context.batcher.flush();
            context.batcher.fullTexturedBox(entry.texture, x1, y1, width, height);
            context.batcher.flush();
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            return;
        }

        if (entry == null)
        {
            entry = new CacheEntry();
            CACHE.put(key, entry);
        }

        thisRenderToEntry(form, context, entry, width, height, revision, angleBucket);

        context.batcher.flush();
        context.batcher.fullTexturedBox(entry.texture, x1, y1, width, height);
        context.batcher.flush();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    private static void thisRenderToEntry(Form form, UIContext context, CacheEntry entry, int width, int height, int revision, int angleBucket)
    {
        ensureScratchFramebuffer(width, height);

        MinecraftClient client = MinecraftClient.getInstance();
        int[] viewport = new int[4];

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        context.batcher.flush();
        scratchFramebuffer.bind();
        scratchFramebuffer.applyClear();
        FormUtilsClient.renderUI(form, context, 0, 0, width, height);
        context.batcher.flush();

        entry.ensure(width, height);
        entry.texture.bind();
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        entry.texture.unbind();

        scratchFramebuffer.unbind();

        if (client != null && client.getFramebuffer() != null)
        {
            client.getFramebuffer().beginWrite(true);
        }

        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        entry.revision = revision;
        entry.angleBucket = angleBucket;
        entry.width = width;
        entry.height = height;
    }

    private static void ensureScratchFramebuffer(int width, int height)
    {
        if (scratchFramebuffer != null && scratchWidth == width && scratchHeight == height)
        {
            return;
        }

        if (scratchFramebuffer == null)
        {
            scratchFramebuffer = BBSModClient.getFramebuffers().getFramebuffer(Link.bbs("form_ui_preview_scratch"), (framebuffer) ->
            {
                scratchTexture = new Texture();

                scratchTexture.setSize(width, height);
                scratchTexture.setFilter(GL11.GL_LINEAR);
                scratchTexture.setWrap(GL13.GL_CLAMP_TO_EDGE);

                scratchDepth = new Renderbuffer();

                scratchDepth.resize(width, height);

                framebuffer.deleteTextures().attach(scratchTexture, GL30.GL_COLOR_ATTACHMENT0);
                framebuffer.attach(scratchDepth);
                framebuffer.unbind();
            });
        }
        else
        {
            scratchFramebuffer.resize(width, height);
        }

        scratchWidth = width;
        scratchHeight = height;
    }

    private static int getAngleBucket(UIContext context, int x1, int x2)
    {
        float angle = MathUtils.toRad(context.mouseX - (x1 + x2) / 2) + MathUtils.PI;

        if (BBSSettings.freezeModels.get())
        {
            angle = -MathUtils.PI + MathUtils.PI / 8F;
        }

        int bucket = (int) (angle * ANGLE_BUCKETS / (MathUtils.PI * 2F));

        return Math.floorMod(bucket, ANGLE_BUCKETS);
    }

    private static long buildKey(Form form, int width, int height, int angleBucket)
    {
        /* Key by form identity so duplicated ModelForms with the same model id
         * do not share thumbnails / animated pose previews. */
        int formKey = System.identityHashCode(form);

        return ((long) formKey << 32)
            ^ ((long) width << 16)
            ^ (long) height
            ^ ((long) angleBucket << 48);
    }

    private static final class CacheEntry
    {
        public Texture texture;
        public int revision = -1;
        public int angleBucket = Integer.MIN_VALUE;
        public int width;
        public int height;

        public void ensure(int w, int h)
        {
            if (this.texture == null)
            {
                this.texture = new Texture();
                this.texture.setFilter(GL11.GL_LINEAR);
                this.texture.setWrap(GL13.GL_CLAMP_TO_EDGE);
            }

            if (this.texture.width != w || this.texture.height != h)
            {
                this.texture.setSize(w, h);
            }
        }

        public void delete()
        {
            if (this.texture != null)
            {
                this.texture.delete();
                this.texture = null;
            }
        }
    }
}
