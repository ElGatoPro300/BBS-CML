package mchorse.bbs_mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;

public class WorldLaunchHelper
{
    public static boolean isCurrentWorld(Minecraft client, String worldFolder)
    {
        if (worldFolder == null || worldFolder.isEmpty())
        {
            return false;
        }

        if (!client.hasSingleplayerServer() || client.getSingleplayerServer() == null)
        {
            return false;
        }

        Path currentSave = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT);

        for (LevelStorageSource.LevelDirectory save : client.getLevelSource().findLevelCandidates().levels())
        {
            if (save.directoryName().equals(worldFolder) && currentSave.equals(save.path()))
            {
                return true;
            }
        }

        String current = currentSave.getFileName().toString();

        return worldFolder.equals(current);
    }

    public static void loadWorld(String worldFolder)
    {
        Minecraft client = Minecraft.getInstance();

        if (WorldLaunchHelper.isCurrentWorld(client, worldFolder))
        {
            return;
        }

        if (client.level != null)
        {
            client.disconnectFromWorld(Component.nullToEmpty(""));
        }

        WorldOpenFlows loader = client.createWorldOpenFlows();

        loader.openWorld(worldFolder, PendingFilmLaunch::clear);
    }
}
