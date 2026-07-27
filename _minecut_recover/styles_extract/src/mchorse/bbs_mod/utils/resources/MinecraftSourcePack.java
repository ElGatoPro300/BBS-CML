package mchorse.bbs_mod.utils.resources;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.StringUtils;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MinecraftSourcePack implements ISourcePack
{
    private final class_3300 manager;
    private Map<String, Object> links = new HashMap<>();

    public MinecraftSourcePack()
    {
        this.manager = class_310.method_1551().method_1478();

        this.setupPaths();
    }
    
    private class_3300 getEffectiveManager(Link link)
    {
        class_310 mc = class_310.method_1551();
        if (mc.method_1576() != null && (link.path.startsWith("structure/") || link.path.endsWith(".nbt")))
        {
            return mc.method_1576().method_34864();
        }
        
        return this.manager;
    }

    public void setupPaths()
    {
        Map<class_2960, List<class_3298>> map = this.manager.method_41265("textures", (l) -> l.method_12836().equals("minecraft") && l.method_12832().endsWith(".png"));

        for (class_2960 id : map.keySet())
        {
            DataPath path = new DataPath(id.method_12832());

            this.insert(path);
        }
    }

    private void insert(DataPath path)
    {
        Map<String, Object> links = this.links;

        for (String string : path.strings)
        {
            if (string.endsWith(".png"))
            {
                links.put(string, string);

                return;
            }
            else
            {
                if (!links.containsKey(string))
                {
                    links.put(string, new HashMap<>());
                }

                links = (Map<String, Object>) links.get(string);
            }
        }
    }


    @Override
    public String getPrefix()
    {
        return "minecraft";
    }

    @Override
    public boolean hasAsset(Link link)
    {
        class_2960 id = class_2960.method_60655(link.source, link.path);
        class_3300 effectiveManager = this.getEffectiveManager(link);
        
        if (effectiveManager.method_14486(id).isPresent())
        {
            return true;
        }
        
        if (!link.path.startsWith("structure/") && link.path.endsWith(".nbt"))
        {
             class_2960 structureId = class_2960.method_60655(link.source, "structure/" + link.path);
             if (effectiveManager.method_14486(structureId).isPresent())
             {
                 return true;
             }
        }
        
        return false;
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        class_2960 id = class_2960.method_60655(link.source, link.path);
        class_3300 effectiveManager = this.getEffectiveManager(link);
        
        Optional<class_3298> resource = effectiveManager.method_14486(id);

        if (resource.isEmpty() && !link.path.startsWith("structure/") && link.path.endsWith(".nbt"))
        {
             class_2960 structureId = class_2960.method_60655(link.source, "structure/" + link.path);
             resource = effectiveManager.method_14486(structureId);
        }

        if (resource.isPresent())
        {
            return resource.get().method_14482();
        }

        return null;
    }

    @Override
    public File getFile(Link link)
    {
        return null;
    }

    @Override
    public Link getLink(File file)
    {
        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        String path = link.path.endsWith("/") ? link.path.substring(0, link.path.length() - 1) : link.path;
        Map<String, Object> allLinks = this.findBasePath(path);

        if (allLinks != null)
        {
            this.traverse(links, path, allLinks, recursive);
        }
    }

    private Map<String, Object> findBasePath(String path)
    {
        if (path.isEmpty())
        {
            return this.links;
        }

        DataPath dataPath = new DataPath(path);
        Map<String, Object> map = this.links;

        for (String next : dataPath.strings)
        {
            Object o = map.get(next);

            if (o instanceof Map)
            {
                map = (Map<String, Object>) o;
            }
            else
            {
                return null;
            }
        }

        return map;
    }

    private void traverse(Collection<Link> links, String path, Map<String, Object> allLinks, boolean recursive)
    {
        for (Map.Entry<String, Object> entry : allLinks.entrySet())
        {
            if (entry.getValue() instanceof Map)
            {
                if (recursive)
                {
                    this.traverse(links, StringUtils.combinePaths(path, entry.getKey()), (Map<String, Object>) entry.getValue(), recursive);
                }

                links.add(new Link(this.getPrefix(), StringUtils.combinePaths(path, entry.getKey()) + "/"));
            }
            else
            {
                links.add(new Link(this.getPrefix(), StringUtils.combinePaths(path, entry.getKey())));
            }
        }
    }
}