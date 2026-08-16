package mchorse.bbs_mod.particles;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.manager.BaseManager;
import mchorse.bbs_mod.utils.manager.storage.JSONLikeStorage;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Particle effects from {@code config/bbs/assets/particles} plus built-in jar
 * assets (e.g. {@code default_placeholder}) so ParticleForm always appears in
 * the forms list.
 */
public class ParticleManager extends BaseManager<ParticleScheme>
{
    private static final String PARTICLES_PREFIX = "particles/";
    private static final String JSON_SUFFIX = ".json";

    public ParticleManager(Supplier<File> folder)
    {
        super(folder);

        this.storage = new JSONLikeStorage().json();
    }

    @Override
    protected ParticleScheme createData(String id, MapType data)
    {
        ParticleScheme scheme = new ParticleScheme();

        if (data != null)
        {
            try
            {
                System.out.println("Parsing \"" + id + "\" particle effect.");

                ParticleScheme.PARSER.fromData(scheme, data);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /* Ensure runtime component lists are always initialized, even if parsing failed. */
        scheme.setup();

        return scheme;
    }

    @Override
    public Collection<String> getKeys()
    {
        Set<String> keys = new HashSet<>(super.getKeys());

        try
        {
            for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("particles"), true))
            {
                String key = this.keyFromAssetLink(link);

                if (key != null)
                {
                    keys.add(key);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return keys;
    }

    @Override
    public boolean exists(String name)
    {
        return super.exists(name) || this.hasAsset(name);
    }

    @Override
    public ParticleScheme load(String id)
    {
        ParticleScheme fromDisk = super.load(id);

        if (fromDisk != null)
        {
            return fromDisk;
        }

        return this.loadFromAsset(id);
    }

    private ParticleScheme loadFromAsset(String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }

        Link link = Link.assets(PARTICLES_PREFIX + id + JSON_SUFFIX);

        try (InputStream stream = BBSMod.getProvider().getAsset(link))
        {
            if (stream == null)
            {
                return null;
            }

            BaseType data = DataToString.fromString(IOUtils.readText(stream));

            if (!(data instanceof MapType map))
            {
                return null;
            }

            return this.create(id, map);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private boolean hasAsset(String id)
    {
        if (id == null || id.isEmpty())
        {
            return false;
        }

        try (InputStream stream = BBSMod.getProvider().getAsset(Link.assets(PARTICLES_PREFIX + id + JSON_SUFFIX)))
        {
            return stream != null;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private String keyFromAssetLink(Link link)
    {
        if (link == null || link.path == null)
        {
            return null;
        }

        String path = link.path;

        if (!path.startsWith(PARTICLES_PREFIX) || !path.endsWith(JSON_SUFFIX))
        {
            return null;
        }

        String key = path.substring(PARTICLES_PREFIX.length(), path.length() - JSON_SUFFIX.length());

        return key.isEmpty() ? null : key;
    }
}
