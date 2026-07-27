package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import net.minecraft.class_1934;
import net.minecraft.class_310;
import net.minecraft.class_636;
import net.minecraft.class_746;

/**
 * Silent Spectator only for Film and Model Block panels.
 * Any other panel (Morphing, Home, …) or leaving BBS restores the previous gamemode.
 */
public final class EditorSpectatorHelper
{
    private static class_1934 savedMode;
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

        class_636 interactions = class_310.method_1551().field_1761;
        class_746 player = class_310.method_1551().field_1724;

        if (interactions == null || player == null)
        {
            return;
        }

        class_1934 current = interactions.method_2920();

        if (current == class_1934.field_9219)
        {
            if (!spectatorApplied)
            {
                /* Already spectator before opening the editor — keep that on restore. */
                savedMode = class_1934.field_9219;
                spectatorApplied = true;
            }

            return;
        }

        if (!spectatorApplied)
        {
            savedMode = current;
        }

        spectatorApplied = true;
        ClientNetwork.sendSetGameMode(class_1934.field_9219);
    }

    public static void restore()
    {
        if (!spectatorApplied)
        {
            return;
        }

        class_1934 restoreTo = savedMode == null ? class_1934.field_9220 : savedMode;

        spectatorApplied = false;
        savedMode = null;

        class_636 interactions = class_310.method_1551().field_1761;

        if (interactions != null && interactions.method_2920() != restoreTo)
        {
            ClientNetwork.sendSetGameMode(restoreTo);
        }
    }
}
