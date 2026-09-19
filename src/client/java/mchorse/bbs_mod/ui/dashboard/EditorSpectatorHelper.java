package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;

/**
 * Silent Spectator for the Film panel (noclip while editing).
 * <p>
 * Model Block is intentionally excluded: it already uses the orbit camera, and
 * auto-spectator left the local player body invisible (armor/items still visible)
 * when switching to Transformaciones / other world-backed panels.
 * Leaving BBS or any non-film panel restores the previous gamemode.
 */
public final class EditorSpectatorHelper
{
    private static GameMode savedMode;
    private static boolean spectatorApplied;
    /**
     * Actor-control needs a playable mode (swipe / shield / damage). Keep the
     * spectator session armed but temporarily apply {@link #savedMode}.
     */
    private static boolean controlSuspended;

    private EditorSpectatorHelper()
    {}

    public static boolean isSpectatorEditorPanel(UIDashboardPanel panel)
    {
        if (panel == null)
        {
            return false;
        }

        UIDashboardPanel main = panel.getMainPanel();

        return main instanceof UIFilmPanel;
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
            if (controlSuspended)
            {
                return;
            }

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
        controlSuspended = false;
        applyGameMode(GameMode.SPECTATOR);
    }

    /**
     * Leave spectator for actor-control combat without forgetting the editor
     * spectator session (so {@link #resumeAfterControl()} can put it back).
     */
    public static void suspendForControl()
    {
        if (!spectatorApplied || controlSuspended)
        {
            if (controlSuspended)
            {
                ensurePlayableForControl();
            }

            return;
        }

        GameMode playable = savedMode == null ? GameMode.CREATIVE : savedMode;

        if (playable == GameMode.SPECTATOR)
        {
            playable = GameMode.CREATIVE;
        }

        controlSuspended = true;
        applyGameMode(playable);
    }

    /**
     * Re-assert a playable mode while actor-control is active (server sync lag
     * or a stray spectator restore must not leave combat disabled).
     */
    public static void ensurePlayableForControl()
    {
        if (!controlSuspended)
        {
            return;
        }

        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;

        if (interactions == null)
        {
            return;
        }

        GameMode current = interactions.getCurrentGameMode();

        if (current != GameMode.SPECTATOR)
        {
            return;
        }

        GameMode playable = savedMode == null ? GameMode.CREATIVE : savedMode;

        if (playable == GameMode.SPECTATOR)
        {
            playable = GameMode.CREATIVE;
        }

        applyGameMode(playable);
    }

    /**
     * Re-enter editor spectator after actor-control ends.
     */
    public static void resumeAfterControl()
    {
        if (!controlSuspended)
        {
            return;
        }

        controlSuspended = false;

        if (spectatorApplied)
        {
            applyGameMode(GameMode.SPECTATOR);
        }
    }

    public static void restore()
    {
        if (!spectatorApplied && !controlSuspended)
        {
            return;
        }

        GameMode restoreTo = savedMode == null ? GameMode.CREATIVE : savedMode;

        spectatorApplied = false;
        controlSuspended = false;
        savedMode = null;

        /* Always re-assert on client + server. Skipping when the local interaction
         * manager already matches restoreTo left the server (and entity invisible
         * flag) stuck in spectator after leaving a spectator editor panel. */
        applyGameMode(restoreTo);
    }

    /**
     * Apply locally first so the next click can attack immediately, then sync server.
     */
    private static void applyGameMode(GameMode mode)
    {
        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;

        if (interactions != null && interactions.getCurrentGameMode() != mode)
        {
            interactions.setGameMode(mode);
        }

        ClientNetwork.sendSetGameMode(mode);
    }
}
