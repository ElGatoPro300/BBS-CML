package mchorse.bbs_mod.client;

import mchorse.bbs_mod.film.CrossWorldFilmEntry;
import net.minecraft.class_310;
import net.minecraft.class_32;
import net.minecraft.class_34;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossWorldFilmScanner
{
    private static final String FILMS_EXTENSION = ".dat";

    /**
     * All saved worlds for the Worlds browser, excluding the currently loaded world.
     */
    public static CompletableFuture<List<class_34>> scanWorldsAsync()
    {
        class_310 client = class_310.method_1551();
        class_32 storage = client.method_1586();
        class_32.class_7410 levelList = storage.method_235();

        return storage.method_43417(levelList).thenApply((summaries) ->
        {
            List<class_34> worlds = new ArrayList<>();

            for (class_34 summary : summaries)
            {
                if (WorldLaunchHelper.isCurrentWorld(client, summary.method_248()))
                {
                    continue;
                }

                worlds.add(summary);
            }

            worlds.sort(null);

            return worlds;
        });
    }

    public static CompletableFuture<List<CrossWorldFilmEntry>> scanAsync()
    {
        class_310 client = class_310.method_1551();
        class_32 storage = client.method_1586();
        class_32.class_7410 levelList = storage.method_235();

        return storage.method_43417(levelList).thenApply((summaries) ->
        {
            Map<String, String> labels = new HashMap<>();

            for (class_34 summary : summaries)
            {
                labels.put(summary.method_248(), summary.method_252());
            }

            List<CrossWorldFilmEntry> entries = new ArrayList<>();

            for (class_32.class_7411 save : levelList.comp_731())
            {
                String worldFolder = save.method_43422();

                if (WorldLaunchHelper.isCurrentWorld(client, worldFolder))
                {
                    continue;
                }

                String worldLabel = labels.getOrDefault(worldFolder, worldFolder);
                File filmsFolder = save.comp_732().resolve("bbs/films").toFile();

                if (!filmsFolder.isDirectory())
                {
                    continue;
                }

                CrossWorldFilmScanner.collectFilms(entries, worldFolder, worldLabel, filmsFolder, "");
            }

            entries.sort((a, b) -> a.getDisplayLabel().compareToIgnoreCase(b.getDisplayLabel()));

            return entries;
        });
    }

    private static void collectFilms(List<CrossWorldFilmEntry> entries, String worldFolder, String worldLabel, File folder, String prefix)
    {
        File[] children = folder.listFiles();

        if (children == null)
        {
            return;
        }

        for (File child : children)
        {
            String name = child.getName();

            if (child.isFile() && name.endsWith(FILMS_EXTENSION) && !name.startsWith("_"))
            {
                String filmId = prefix + name.substring(0, name.length() - FILMS_EXTENSION.length());

                entries.add(new CrossWorldFilmEntry(worldFolder, worldLabel, filmId));
            }
            else if (child.isDirectory() && !name.startsWith("_"))
            {
                File[] nested = child.listFiles();
                String nestedPrefix = prefix + name + "/";

                if (nested == null || nested.length == 0)
                {
                    entries.add(new CrossWorldFilmEntry(worldFolder, worldLabel, nestedPrefix));
                }
                else
                {
                    CrossWorldFilmScanner.collectFilms(entries, worldFolder, worldLabel, child, nestedPrefix);
                }
            }
        }
    }
}
