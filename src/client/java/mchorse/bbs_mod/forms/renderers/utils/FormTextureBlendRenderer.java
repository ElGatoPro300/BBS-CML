package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;

/**
 * Shared two-pass alpha crossfade for texture bend on non-VAO form renderers.
 */
public final class FormTextureBlendRenderer
{
    @FunctionalInterface
    public interface Pass
    {
        void draw(Link texture, float alphaFactor);
    }

    private FormTextureBlendRenderer()
    {}

    public static boolean isBlending(TextureBlend blend)
    {
        return blend != null && blend.blend > 0F && blend.blend < 1F;
    }

    public static Link resolveFrom(TextureBlend blend, Link defaultTexture)
    {
        if (blend == null || blend.from == null)
        {
            return defaultTexture;
        }

        return blend.from;
    }

    public static Link resolveTo(TextureBlend blend, Link defaultTexture)
    {
        if (blend == null || blend.to == null)
        {
            return defaultTexture;
        }

        return blend.to;
    }

    public static void draw(TextureBlend blend, Link defaultTexture, Pass pass)
    {
        if (blend == null || blend.blend <= 0F)
        {
            pass.draw(defaultTexture, 1F);

            return;
        }

        if (blend.blend >= 1F)
        {
            pass.draw(resolveTo(blend, defaultTexture), 1F);

            return;
        }

        pass.draw(resolveFrom(blend, defaultTexture), 1F - blend.blend);
        pass.draw(resolveTo(blend, defaultTexture), blend.blend);
    }
}
