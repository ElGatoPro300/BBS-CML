package mchorse.bbs_mod.client;

import mchorse.bbs_mod.film.CrossWorldFilmEntry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;

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
    public static CompletableFuture<List<LevelSummary>> scanWorldsAsync()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        LevelStorage storage = client.getLevelStorage();
        LevelStorage.LevelList levelList = storage.getLevelList();

        return storage.loadSummaries(levelList).thenApply((summaries) ->
        {
            List<LevelSummary> worlds = new ArrayList<>();

            for (LevelSummary summary : summaries)
            {
                if (WorldLaunchHelper.isCurrentWorld(client, summary.getName()))
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
        MinecraftClient client = MinecraftClient.getInstance();
        LevelStorage storage = client.getLevelStorage();
        LevelStorage.LevelList levelList = storage.getLevelList();

        return storage.loadSummaries(levelList).thenApply((summaries) ->
        {
            Map<String, String> labels = new HashMap<>();

            for (LevelSummary summary : summaries)
            {
                labels.put(summary.getName(), summary.getDisplayName());
            }

            List<CrossWorldFilmEntry> entries = new ArrayList<>();

            for (LevelStorage.LevelSave save : levelList.levels())
            {
                String worldFolder = save.getRootPath();

                if (WorldLaunchHelper.isCurrentWorld(client, worldFolder))
                {
                    continue;
                }

                String worldLabel = labels.getOrDefault(worldFolder, worldFolder);
                File filmsFolder = save.path().resolve("bbs/films").toFile();

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
