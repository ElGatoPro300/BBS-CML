package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.ui.ValueGizmoToolbar;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public final class GizmoToolbarButtons
{
    private GizmoToolbarButtons()
    {}

    public static Icon getIcon(String id)
    {
        if (ValueGizmoToolbar.MOVE.equals(id))
        {
            return Icons.ALL_DIRECTIONS;
        }
        else if (ValueGizmoToolbar.SCALE.equals(id))
        {
            return Icons.SCALE;
        }
        else if (ValueGizmoToolbar.ROTATE.equals(id))
        {
            return Icons.ARC;
        }
        else if (ValueGizmoToolbar.COMBINED.equals(id))
        {
            return Icons.SHAPES;
        }
        else if (ValueGizmoToolbar.TOP.equals(id))
        {
            return Icons.SPHERE;
        }
        else if (ValueGizmoToolbar.SIZE.equals(id))
        {
            return Icons.MAXIMIZE;
        }
        else if (ValueGizmoToolbar.TRANSLATE_SPEED.equals(id))
        {
            return Icons.FORWARD;
        }

        return Icons.GEAR;
    }

    public static IKey getTooltip(String id)
    {
        if (ValueGizmoToolbar.MOVE.equals(id))
        {
            return UIKeys.FILM_GIZMO_MOVE;
        }
        else if (ValueGizmoToolbar.SCALE.equals(id))
        {
            return UIKeys.FILM_GIZMO_SCALE;
        }
        else if (ValueGizmoToolbar.ROTATE.equals(id))
        {
            return UIKeys.FILM_GIZMO_ROTATE;
        }
        else if (ValueGizmoToolbar.COMBINED.equals(id))
        {
            return UIKeys.FILM_GIZMO_COMBINED;
        }
        else if (ValueGizmoToolbar.TOP.equals(id))
        {
            return UIKeys.FILM_GIZMO_TOP;
        }
        else if (ValueGizmoToolbar.SIZE.equals(id))
        {
            return UIKeys.FILM_GIZMO_SIZE;
        }
        else if (ValueGizmoToolbar.TRANSLATE_SPEED.equals(id))
        {
            return UIKeys.FILM_GIZMO_TRANSLATE_SPEED;
        }

        return IKey.raw(id);
    }
}
