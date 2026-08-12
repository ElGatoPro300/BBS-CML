package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.ui.ValueViewportToolbar;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public final class ViewportToolbarButtons
{
    private ViewportToolbarButtons()
    {}

    public static Icon getIcon(String id)
    {
        if (ValueViewportToolbar.HIDE_OVERLAYS.equals(id))
        {
            return Icons.VISIBLE;
        }
        else if (ValueViewportToolbar.ONION_SKIN.equals(id))
        {
            return Icons.ONION_SKIN;
        }
        else if (ValueViewportToolbar.TOGGLE_SHADERS.equals(id))
        {
            return Icons.GLOBE;
        }
        else if (ValueViewportToolbar.PLAYBACK.equals(id))
        {
            return Icons.PLAY;
        }
        else if (ValueViewportToolbar.TELEPORT.equals(id))
        {
            return Icons.MOVE_TO;
        }
        else if (ValueViewportToolbar.FLIGHT.equals(id))
        {
            return Icons.PLANE;
        }
        else if (ValueViewportToolbar.CONTROL.equals(id))
        {
            return Icons.POSE;
        }
        else if (ValueViewportToolbar.PERSPECTIVE.equals(id))
        {
            return Icons.CAMERA;
        }
        else if (ValueViewportToolbar.RECORD_REPLAY.equals(id))
        {
            return Icons.SPHERE;
        }
        else if (ValueViewportToolbar.RECORD_VIDEO.equals(id))
        {
            return Icons.VIDEO_CAMERA;
        }
        else if (ValueViewportToolbar.RENDER_QUEUE.equals(id))
        {
            return Icons.FILM;
        }
        else if (ValueViewportToolbar.RESTORE_BLOCKS.equals(id))
        {
            return Icons.BLOCK;
        }

        return Icons.GEAR;
    }

    public static IKey getTooltip(String id)
    {
        if (ValueViewportToolbar.HIDE_OVERLAYS.equals(id))
        {
            return UIKeys.FILM_PREVIEW_TOGGLE_OVERLAYS;
        }
        else if (ValueViewportToolbar.ONION_SKIN.equals(id))
        {
            return UIKeys.FILM_CONTROLLER_ONION_SKIN_TITLE;
        }
        else if (ValueViewportToolbar.TOGGLE_SHADERS.equals(id))
        {
            return UIKeys.FILM_PREVIEW_TOGGLE_SHADERS;
        }
        else if (ValueViewportToolbar.PLAYBACK.equals(id))
        {
            return UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PLAUSE;
        }
        else if (ValueViewportToolbar.TELEPORT.equals(id))
        {
            return UIKeys.FILM_TELEPORT_TITLE;
        }
        else if (ValueViewportToolbar.FLIGHT.equals(id))
        {
            return UIKeys.CAMERA_EDITOR_KEYS_MODES_FLIGHT;
        }
        else if (ValueViewportToolbar.CONTROL.equals(id))
        {
            return UIKeys.FILM_CONTROLLER_KEYS_TOGGLE_CONTROL;
        }
        else if (ValueViewportToolbar.PERSPECTIVE.equals(id))
        {
            return UIKeys.FILM_CONTROLLER_KEYS_CHANGE_CAMERA_MODE;
        }
        else if (ValueViewportToolbar.RECORD_REPLAY.equals(id))
        {
            return UIKeys.FILM_REPLAY_RECORD;
        }
        else if (ValueViewportToolbar.RECORD_VIDEO.equals(id))
        {
            return UIKeys.CAMERA_TOOLTIPS_RECORD;
        }
        else if (ValueViewportToolbar.RENDER_QUEUE.equals(id))
        {
            return UIKeys.FILM_OPEN_RENDER_QUEUE;
        }
        else if (ValueViewportToolbar.RESTORE_BLOCKS.equals(id))
        {
            return UIKeys.FILM_PREVIEW_RESTORE_BLOCKS;
        }

        return IKey.raw(id);
    }
}
