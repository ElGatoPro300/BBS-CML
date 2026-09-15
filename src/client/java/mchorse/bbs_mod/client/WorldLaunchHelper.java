package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.RecordingPauseHelper;
import mchorse.bbs_mod.ui.dashboard.EditorSpectatorHelper;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.utils.VideoRecorder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;

public class WorldLaunchHelper
{
    private static final int DISCONNECT_RETRY_TICKS = 40;
    private static final int DISCONNECT_FAILSAFE_TICKS = 600;

    private static String pendingWorldFolder;
    private static int pendingWaitTicks;

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
            if (!currentSave.equals(save.path()))
            {
                continue;
            }

            if (WorldLaunchHelper.matchesWorldFolder(worldFolder, save.directoryName()))
            {
                return true;
            }
        }

        String current = currentSave.getFileName().toString();

        return WorldLaunchHelper.matchesWorldFolder(worldFolder, current);
    }

    private static boolean matchesWorldFolder(String expected, String actual)
    {
        if (expected == null || actual == null)
        {
            return false;
        }

        if (expected.equals(actual))
        {
            return true;
        }

        return expected.equalsIgnoreCase(actual);
    }

    /**
     * True when the client is already inside a loaded world session.
     */
    public static boolean isInLoadedWorld(Minecraft client)
    {
        return client != null && client.level != null;
    }

    public static void loadWorld(String worldFolder)
    {
        Minecraft client = Minecraft.getInstance();

        if (WorldLaunchHelper.isCurrentWorld(client, worldFolder))
        {
            return;
        }

        WorldLaunchHelper.prepareClientForWorldSwitch(client);

        if (WorldLaunchHelper.needsDisconnect(client))
        {
            WorldLaunchHelper.pendingWorldFolder = worldFolder;
            WorldLaunchHelper.pendingWaitTicks = 0;
            WorldLaunchHelper.requestDisconnect(client);

            return;
        }

        WorldLaunchHelper.startWorldLoad(client, worldFolder);
    }

    public static void tick(Minecraft client)
    {
        if (WorldLaunchHelper.pendingWorldFolder == null)
        {
            return;
        }

        WorldLaunchHelper.ensureRenderTarget(client);

        if (WorldLaunchHelper.needsDisconnect(client))
        {
            WorldLaunchHelper.pendingWaitTicks += 1;

            if (WorldLaunchHelper.pendingWaitTicks >= WorldLaunchHelper.DISCONNECT_FAILSAFE_TICKS)
            {
                WorldLaunchHelper.abortPendingLaunch(client);

                return;
            }

            if (WorldLaunchHelper.pendingWaitTicks % WorldLaunchHelper.DISCONNECT_RETRY_TICKS == 0)
            {
                WorldLaunchHelper.requestDisconnect(client);
            }

            return;
        }

        String folder = WorldLaunchHelper.pendingWorldFolder;

        WorldLaunchHelper.pendingWorldFolder = null;
        WorldLaunchHelper.pendingWaitTicks = 0;
        WorldLaunchHelper.startWorldLoad(client, folder);
    }

    public static void onClientDisconnected(Minecraft client)
    {
        WorldLaunchHelper.ensureRenderTarget(client);
    }

    public static void clearPending()
    {
        WorldLaunchHelper.pendingWorldFolder = null;
        WorldLaunchHelper.pendingWaitTicks = 0;
    }

    private static boolean needsDisconnect(Minecraft client)
    {
        return client.level != null || client.hasSingleplayerServer();
    }

    private static void requestDisconnect(Minecraft client)
    {
        WorldLaunchHelper.prepareClientForWorldSwitch(client);

        ClientLevel world = client.level;

        if (world != null)
        {
            world.disconnect(Component.literal("Disconnecting"));
        }

        client.disconnect(new TitleScreen(), false);
    }

    private static void abortPendingLaunch(Minecraft client)
    {
        WorldLaunchHelper.clearPending();
        WorldLaunchHelper.ensureRenderTarget(client);

        if (client.screen == null)
        {
            client.setScreen(new TitleScreen());
        }
    }

    private static void prepareClientForWorldSwitch(Minecraft client)
    {
        if (client.screen instanceof UIScreen)
        {
            client.setScreen(null);
        }

        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();

        if (videoRecorder.isRecording())
        {
            videoRecorder.stopRecording();
        }

        BBSModClient.getCameraController().reset();
        BBSRendering.setCustomSize(false);
        WorldLaunchHelper.ensureRenderTarget(client);
        RecordingPauseHelper.reset();
        EditorSpectatorHelper.restore();
    }

    private static void ensureRenderTarget(Minecraft client)
    {
        BBSRendering.ensureMainFramebuffer();
    }

    private static void startWorldLoad(Minecraft client, String worldFolder)
    {
        client.execute(() ->
        {
            WorldLaunchHelper.ensureRenderTarget(client);

            WorldOpenFlows loader = client.createWorldOpenFlows();

            loader.openWorld(worldFolder, WorldLaunchHelper::clearPending);
        });
    }
}
