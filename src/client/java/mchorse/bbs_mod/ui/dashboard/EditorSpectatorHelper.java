package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;

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
    private static GameType savedMode;
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

        MultiPlayerGameMode interactions = Minecraft.getInstance().gameMode;
        LocalPlayer player = Minecraft.getInstance().player;

        if (interactions == null || player == null)
        {
            return;
        }

        GameType current = interactions.getPlayerMode();

        if (current == GameType.SPECTATOR)
        {
            if (!spectatorApplied)
            {
                /* Already spectator before opening the editor — keep that on restore. */
                savedMode = GameType.SPECTATOR;
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
        applyGameMode(GameType.SPECTATOR);
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

        GameType playable = savedMode == null ? GameType.CREATIVE : savedMode;

        if (playable == GameType.SPECTATOR)
        {
            playable = GameType.CREATIVE;
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

        MultiPlayerGameMode interactions = Minecraft.getInstance().gameMode;

        if (interactions == null)
        {
            return;
        }

        GameType current = interactions.getPlayerMode();

        if (current != GameType.SPECTATOR)
        {
            return;
        }

        GameType playable = savedMode == null ? GameType.CREATIVE : savedMode;

        if (playable == GameType.SPECTATOR)
        {
            playable = GameType.CREATIVE;
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
            applyGameMode(GameType.SPECTATOR);
        }
    }

    public static void restore()
    {
        if (!spectatorApplied && !controlSuspended)
        {
            return;
        }

        GameType restoreTo = savedMode == null ? GameType.CREATIVE : savedMode;

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
    private static void applyGameMode(GameType mode)
    {
        MultiPlayerGameMode interactions = Minecraft.getInstance().gameMode;

        if (interactions != null && interactions.getPlayerMode() != mode)
        {
            interactions.setLocalMode(mode);
        }

        ClientNetwork.sendSetGameMode(mode);
    }
}
