package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridges BBS links to vanilla Identifiers so RenderLayer-based rendering
 * (including Iris shader pipelines) can use custom mob textures reliably.
 */
public class MobTextureOverride
{
    private static final ThreadLocal<Link> ACTIVE_LINK = new ThreadLocal<>();
    private static final Map<Link, class_2960> CACHE = new HashMap<>();

    public static void begin(Link link)
    {
        if (link == null)
        {
            ACTIVE_LINK.remove();
        }
        else
        {
            ACTIVE_LINK.set(link);
        }
    }

    public static void end()
    {
        ACTIVE_LINK.remove();
    }

    public static class_2960 getOverridden(class_2960 fallback)
    {
        Link link = ACTIVE_LINK.get();

        if (link == null)
        {
            return fallback;
        }

        class_2960 id = CACHE.computeIfAbsent(link, MobTextureOverride::registerDynamicTexture);

        return id == null ? fallback : id;
    }

    private static class_2960 registerDynamicTexture(Link link)
    {
        try (InputStream stream = BBSMod.getProvider().getAsset(link))
        {
            class_1011 image = class_1011.method_4309(stream);
            class_1043 texture = new class_1043(image);
            String key = "bbs_mob_override_" + Integer.toUnsignedString(link.toString().hashCode());

            return class_310.method_1551().method_1531().method_4617(key, texture);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
