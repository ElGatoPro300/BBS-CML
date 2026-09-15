package mchorse.bbs_mod.graphics.texture;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bridges a BBS raw-GL Texture into the vanilla two-phase GUI so
 * DrawContext.drawTexture can sample it. Zero-copy: adopts the GL id.
 */
public final class AdoptedTexture extends AbstractTexture
{
    private static final int USAGE = GpuTexture.USAGE_TEXTURE_BINDING;

    private static final Map<Texture, Cached> REGISTRY = new WeakHashMap<>();
    private static final Map<Integer, Cached> GLID_REGISTRY = new HashMap<>();
    private static int counter;

    private static final class Cached
    {
        private final Identifier id;
        private final int width;
        private final int height;
        private final boolean linear;
        private final boolean mipmap;

        private Cached(Identifier id, int width, int height, boolean linear, boolean mipmap)
        {
            this.id = id;
            this.width = width;
            this.height = height;
            this.linear = linear;
            this.mipmap = mipmap;
        }
    }

    public static Identifier identifier(Texture texture)
    {
        if (texture == null || !texture.isValid())
        {
            return null;
        }

        int width = Math.max(1, texture.width);
        int height = Math.max(1, texture.height);
        boolean linear = texture.isLinear();
        boolean mipmap = texture.isReallyMipmap();
        Cached cached = REGISTRY.get(texture);

        if (cached != null && cached.width == width && cached.height == height && cached.linear == linear && cached.mipmap == mipmap)
        {
            return cached.id;
        }

        Identifier id = cached != null ? cached.id : Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "adopted/" + (counter++));

        Minecraft.getInstance().getTextureManager().register(id,
            new AdoptedTexture(texture.id, "bbs_adopted_" + texture.id, width, height, linear, mipmap));
        REGISTRY.put(texture, new Cached(id, width, height, linear, mipmap));

        return id;
    }

    public static Identifier identifier(int glId, int width, int height, boolean linear)
    {
        return identifier(glId, width, height, linear, false);
    }

    public static Identifier identifier(int glId, int width, int height, boolean linear, boolean mipmap)
    {
        if (glId < 0)
        {
            return null;
        }

        int safeW = Math.max(1, width);
        int safeH = Math.max(1, height);
        Cached cached = GLID_REGISTRY.get(glId);

        if (cached != null && cached.width == safeW && cached.height == safeH && cached.linear == linear && cached.mipmap == mipmap)
        {
            return cached.id;
        }

        Identifier id = cached != null ? cached.id : Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "adopted/glid_" + glId + (linear ? "_lin" : "_nea") + (mipmap ? "_mip" : ""));

        Minecraft.getInstance().getTextureManager().register(id,
            new AdoptedTexture(glId, "bbs_adopted_glid_" + glId, safeW, safeH, linear, mipmap));
        GLID_REGISTRY.put(glId, new Cached(id, safeW, safeH, linear, mipmap));

        return id;
    }

    private AdoptedTexture(int glId, String label, int width, int height, boolean linear, boolean mipmap)
    {
        AdoptedGlTexture glTexture = new AdoptedGlTexture(glId, label, width, height);

        this.texture = glTexture;
        this.textureView = new AdoptedGlTextureView(glTexture);

        FilterMode filter = linear ? FilterMode.LINEAR : FilterMode.NEAREST;

        this.sampler = RenderSystem.getSamplerCache().getSampler(
            AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, mipmap);
    }

    @Override
    public void close()
    {
    }

    private static final class AdoptedGlTexture extends GlTexture
    {
        private AdoptedGlTexture(int glId, String label, int width, int height)
        {
            super(USAGE, label, GpuFormat.RGBA8_UNORM,
                Math.max(1, width), Math.max(1, height), 1, 1, glId, null);
        }

        @Override
        public void close()
        {
        }
    }

    private static final class AdoptedGlTextureView extends GlTextureView
    {
        private AdoptedGlTextureView(AdoptedGlTexture texture)
        {
            super(texture, 0, 1, null);
        }

        @Override
        public void close()
        {
        }
    }
}
