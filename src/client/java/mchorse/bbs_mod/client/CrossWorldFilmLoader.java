package mchorse.bbs_mod.client;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.manager.storage.CompressedDataStorage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.File;
import java.util.function.Consumer;

public class CrossWorldFilmLoader
{
    private static final CompressedDataStorage STORAGE = new CompressedDataStorage();

    public static File getFilmFile(String worldFolder, String filmId)
    {
        if (worldFolder == null || filmId == null || filmId.isEmpty())
        {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        for (LevelStorage.LevelSave save : client.getLevelStorage().getLevelList().levels())
        {
            if (save.getRootPath().equals(worldFolder))
            {
                return save.path().resolve("bbs/films/" + filmId + ".dat").toFile();
            }
        }

        return null;
    }

    public static void load(String worldFolder, String filmId, Consumer<Film> consumer)
    {
        File file = CrossWorldFilmLoader.getFilmFile(worldFolder, filmId);

        if (file == null || !file.exists())
        {
            consumer.accept(null);

            return;
        }

        try
        {
            MapType mapType = CrossWorldFilmLoader.STORAGE.load(file);
            Film film = new Film();

            if (mapType != null)
            {
                film.fromData(mapType);
            }

            film.setId(filmId);
            consumer.accept(film);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            consumer.accept(null);
        }
    }
}
