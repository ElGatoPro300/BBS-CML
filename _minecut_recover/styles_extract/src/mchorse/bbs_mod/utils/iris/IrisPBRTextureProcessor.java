package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IrisPBRTextureProcessor
{
    private static final int MAX_CACHE_SIZE = 256;

    private static final Map<CacheKey, Texture> cache = new HashMap<>();
    private static final Color temp = new Color();

    private record CacheKey(int textureId, int intensity, IrisTextureWrapper.PBRMapType type)
    {}

    public static int getTextureId(Texture base, float intensity, IrisTextureWrapper.PBRMapType type)
    {
        if (base == null || !base.isValid() || type == IrisTextureWrapper.PBRMapType.NONE)
        {
            return base == null ? -1 : base.id;
        }

        int quantizedIntensity = quantize(intensity);

        if (quantizedIntensity == 100)
        {
            return base.id;
        }

        CacheKey key = new CacheKey(base.id, quantizedIntensity, type);
        Texture processed = cache.get(key);

        if (processed != null && processed.isValid())
        {
            return processed.id;
        }

        Pixels pixels = Texture.pixelsFromTexture(base);

        if (pixels == null)
        {
            return base.id;
        }

        float factor = quantizedIntensity / 100F;
        processPixels(pixels, factor, type);
        pixels.rewindBuffer();

        Texture processedTexture = Texture.textureFromPixels(pixels, base.getFilter());

        if (cache.size() >= MAX_CACHE_SIZE)
        {
            Iterator<Texture> it = cache.values().iterator();

            while (it.hasNext())
            {
                Texture value = it.next();

                if (value != null)
                {
                    value.delete();
                }
            }

            cache.clear();
        }

        cache.put(key, processedTexture);

        return processedTexture.id;
    }

    private static int quantize(float intensity)
    {
        return Math.round(MathUtils.clamp(intensity, 0F, 4F) * 100F);
    }

    private static void processPixels(Pixels pixels, float factor, IrisTextureWrapper.PBRMapType type)
    {
        if (type == IrisTextureWrapper.PBRMapType.NORMAL)
        {
            processNormalPixels(pixels, factor);

            return;
        }

        if (type == IrisTextureWrapper.PBRMapType.SPECULAR)
        {
            processSpecularPixels(pixels, factor);
        }
    }

    private static void processNormalPixels(Pixels pixels, float factor)
    {
        for (int i = 0, c = pixels.getCount(); i < c; i++)
        {
            Color source = pixels.getColor(i);

            if (source == null)
            {
                continue;
            }

            float nx = (source.r * 2F - 1F) * factor;
            float ny = (source.g * 2F - 1F) * factor;
            float nz = source.b * 2F - 1F;

            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

            if (length <= 0.0001F)
            {
                nx = 0F;
                ny = 0F;
                nz = 1F;
            }
            else
            {
                nx /= length;
                ny /= length;
                nz /= length;
            }

            temp.r = MathUtils.clamp(nx * 0.5F + 0.5F, 0F, 1F);
            temp.g = MathUtils.clamp(ny * 0.5F + 0.5F, 0F, 1F);
            temp.b = MathUtils.clamp(nz * 0.5F + 0.5F, 0F, 1F);
            temp.a = source.a;

            pixels.setColor(i, temp);
        }
    }

    private static void processSpecularPixels(Pixels pixels, float factor)
    {
        for (int i = 0, c = pixels.getCount(); i < c; i++)
        {
            Color source = pixels.getColor(i);

            if (source == null)
            {
                continue;
            }

            temp.r = MathUtils.clamp(source.r * factor, 0F, 1F);
            temp.g = MathUtils.clamp(source.g * factor, 0F, 1F);
            temp.b = MathUtils.clamp(source.b * factor, 0F, 1F);
            temp.a = MathUtils.clamp(source.a * factor, 0F, 1F);

            pixels.setColor(i, temp);
        }
    }
}
