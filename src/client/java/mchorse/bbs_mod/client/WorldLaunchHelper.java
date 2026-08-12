package mchorse.bbs_mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;

import java.nio.file.Path;

public class WorldLaunchHelper
{
    public static boolean isCurrentWorld(MinecraftClient client, String worldFolder)
    {
        if (worldFolder == null || worldFolder.isEmpty())
        {
            return false;
        }

        if (!client.isIntegratedServerRunning() || client.getServer() == null)
        {
            return false;
        }

        Path currentSave = client.getServer().getSavePath(WorldSavePath.ROOT);

        for (LevelStorage.LevelSave save : client.getLevelStorage().getLevelList().levels())
        {
            if (save.getRootPath().equals(worldFolder) && currentSave.equals(save.path()))
            {
                return true;
            }
        }

        String current = currentSave.getFileName().toString();

        return worldFolder.equals(current);
    }

    public static void loadWorld(String worldFolder)
    {
        MinecraftClient client = MinecraftClient.getInstance();

        if (WorldLaunchHelper.isCurrentWorld(client, worldFolder))
        {
            return;
        }

        if (client.world != null)
        {
            client.disconnect();
        }

        IntegratedServerLoader loader = client.createIntegratedServerLoader();

        loader.start(worldFolder, PendingFilmLaunch::clear);
    }
}
