package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;

/**
 * Silent Spectator only for Film and Model Block panels.
 * Any other panel (Morphing, Home, …) or leaving BBS restores the previous gamemode.
 */
public final class EditorSpectatorHelper
{
    private static GameMode savedMode;
    private static boolean spectatorApplied;

    private EditorSpectatorHelper()
    {}

    public static boolean isSpectatorEditorPanel(UIDashboardPanel panel)
    {
        if (panel == null)
        {
            return false;
        }

        UIDashboardPanel main = panel.getMainPanel();

        return main instanceof UIFilmPanel || main instanceof UIModelBlockPanel;
    }

    public static void syncForPanel(UIDashboardPanel panel)
    {
        if (BBSSettings.autoSpectatorInEditors == null || !BBSSettings.autoSpectatorInEditors.get())
        {
            restore();

            return;
        }

        if (isSpectatorEditorPanel(panel))
        {
            enterSpectator();
        }
        else
        {
            restore();
        }
    }

    public static void enterSpectator()
    {
        if (BBSSettings.autoSpectatorInEditors == null || !BBSSettings.autoSpectatorInEditors.get())
        {
            return;
        }

        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (interactions == null || player == null)
        {
            return;
        }

        GameMode current = interactions.getCurrentGameMode();

        if (current == GameMode.SPECTATOR)
        {
            if (!spectatorApplied)
            {
                /* Already spectator before opening the editor — keep that on restore. */
                savedMode = GameMode.SPECTATOR;
                spectatorApplied = true;
            }

            return;
        }

        if (!spectatorApplied)
        {
            savedMode = current;
        }

        spectatorApplied = true;
        ClientNetwork.sendSetGameMode(GameMode.SPECTATOR);
    }

    public static void restore()
    {
        if (!spectatorApplied)
        {
            return;
        }

        GameMode restoreTo = savedMode == null ? GameMode.CREATIVE : savedMode;

        spectatorApplied = false;
        savedMode = null;

        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;

        if (interactions != null && interactions.getCurrentGameMode() != restoreTo)
        {
            ClientNetwork.sendSetGameMode(restoreTo);
        }
    }
}
