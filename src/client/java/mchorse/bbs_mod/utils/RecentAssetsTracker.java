package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.storage.DataStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.CrossWorldFilmEntry;
import mchorse.bbs_mod.ui.ContentType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class RecentAssetsTracker
{
    public static final List<Entry> RECENT = new ArrayList<>();
    private static final int MAX_SIZE = 10;

    public static void add(ContentType type, String id)
    {
        if (type == null || id == null || id.isEmpty() || RecentAssetsTracker.shouldSkipTracking(type, id))
        {
            return;
        }

        RECENT.removeIf(e -> e.type == type && e.id.equals(id));
        RECENT.add(0, new Entry(type, id));
        if (RECENT.size() > MAX_SIZE)
        {
            RECENT.remove(RECENT.size() - 1);
        }

        save();
    }

    public static boolean existsInCurrentWorld(ContentType type, String id)
    {
        if (type != ContentType.FILMS || id == null || id.isEmpty())
        {
            return true;
        }

        if (CrossWorldFilmEntry.decodeKey(id) != null)
        {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.world == null)
        {
            return true;
        }

        File worldFolder = BBSMod.getWorldFolder();

        if (worldFolder == null)
        {
            return true;
        }

        File filmsRoot = new File(worldFolder, "bbs/films");

        if (id.endsWith("/"))
        {
            return new File(filmsRoot, id).isDirectory();
        }

        return new File(filmsRoot, id + ".dat").exists();
    }

    public static boolean shouldExcludeFromRecent(ContentType type, String id)
    {
        if (type != ContentType.FILMS || id == null || id.isEmpty())
        {
            return false;
        }

        if (CrossWorldFilmEntry.decodeKey(id) != null)
        {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client != null && client.getLevelStorage() != null)
        {
            for (LevelStorage.LevelSave save : client.getLevelStorage().getLevelList().levels())
            {
                String root = save.getRootPath();

                if (id.equals(root) || id.startsWith(root + "/"))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean shouldSkipTracking(ContentType type, String id)
    {
        if (RecentAssetsTracker.shouldExcludeFromRecent(type, id))
        {
            return true;
        }

        if (type != ContentType.FILMS)
        {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        return client == null || client.world == null || client.player == null;
    }

    public static void remove(ContentType type, String id)
    {
        RECENT.removeIf(e -> e.type == type && e.id.equals(id));
        save();
    }

    private static File getFile()
    {
        File worldFolder = BBSMod.getWorldFolder();

        if (worldFolder != null)
        {
            return new File(worldFolder, "bbs/recent_assets.dat");
        }

        return BBSMod.getSettingsPath("recent_assets.dat");
    }

    public static void load()
    {
        File file = getFile();

        if (!file.exists())
        {
            return;
        }

        try (InputStream stream = new FileInputStream(file))
        {
            BaseType type = DataStorage.readFromStream(stream);

            if (type != null && type.isList())
            {
                RECENT.clear();
                for (BaseType entry : type.asList())
                {
                    if (entry.isMap())
                    {
                        MapType map = entry.asMap();
                        String typeId = map.getString("type");
                        String id = map.getString("id");

                        ContentType contentType = ContentType.fromId(typeId);
                        if (contentType != null && !RecentAssetsTracker.shouldExcludeFromRecent(contentType, id))
                        {
                            RECENT.add(new Entry(contentType, id));
                        }
                    }
                }
            }

            RecentAssetsTracker.save();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void save()
    {
        File file = getFile();

        if (file.getParentFile() != null)
        {
            file.getParentFile().mkdirs();
        }
        ListType list = new ListType();

        for (Entry entry : RECENT)
        {
            MapType map = new MapType();
            map.putString("type", entry.type.getId());
            map.putString("id", entry.id);
            list.add(map);
        }

        try (OutputStream stream = new FileOutputStream(file))
        {
            DataStorage.writeToStream(stream, list);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static class Entry
    {
        public ContentType type;
        public String id;

        public Entry(ContentType type, String id)
        {
            this.type = type;
            this.id = id;
        }
    }
}
