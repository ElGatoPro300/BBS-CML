package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.ui.ValueFormEditorGizmoToolbar;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public final class FormEditorGizmoToolbarButtons
{
    private FormEditorGizmoToolbarButtons()
    {}

    public static Icon getIcon(String id)
    {
        if (ValueFormEditorGizmoToolbar.BODY_PART.equals(id))
        {
            return Icons.LIMB;
        }
        else if (ValueFormEditorGizmoToolbar.TRANSFORM.equals(id))
        {
            return Icons.GEAR;
        }
        else if (ValueFormEditorGizmoToolbar.MOVE.equals(id))
        {
            return Icons.ALL_DIRECTIONS;
        }
        else if (ValueFormEditorGizmoToolbar.SCALE.equals(id))
        {
            return Icons.SCALE;
        }
        else if (ValueFormEditorGizmoToolbar.ROTATE.equals(id))
        {
            return Icons.ARC;
        }
        else if (ValueFormEditorGizmoToolbar.COMBINED.equals(id))
        {
            return Icons.SHAPES;
        }
        else if (ValueFormEditorGizmoToolbar.TOP.equals(id))
        {
            return Icons.SPHERE;
        }
        else if (ValueFormEditorGizmoToolbar.SIZE.equals(id))
        {
            return Icons.MAXIMIZE;
        }
        else if (ValueFormEditorGizmoToolbar.TRANSLATE_SPEED.equals(id))
        {
            return Icons.FORWARD;
        }

        return Icons.GEAR;
    }

    public static IKey getTooltip(String id)
    {
        if (ValueFormEditorGizmoToolbar.BODY_PART.equals(id))
        {
            return UIKeys.FILM_GIZMO_BODY_PART;
        }
        else if (ValueFormEditorGizmoToolbar.TRANSFORM.equals(id))
        {
            return UIKeys.FILM_GIZMO_TRANSFORM;
        }
        else if (ValueFormEditorGizmoToolbar.MOVE.equals(id))
        {
            return UIKeys.FILM_GIZMO_MOVE;
        }
        else if (ValueFormEditorGizmoToolbar.SCALE.equals(id))
        {
            return UIKeys.FILM_GIZMO_SCALE;
        }
        else if (ValueFormEditorGizmoToolbar.ROTATE.equals(id))
        {
            return UIKeys.FILM_GIZMO_ROTATE;
        }
        else if (ValueFormEditorGizmoToolbar.COMBINED.equals(id))
        {
            return UIKeys.FILM_GIZMO_COMBINED;
        }
        else if (ValueFormEditorGizmoToolbar.TOP.equals(id))
        {
            return UIKeys.FILM_GIZMO_TOP;
        }
        else if (ValueFormEditorGizmoToolbar.SIZE.equals(id))
        {
            return UIKeys.FILM_GIZMO_SIZE;
        }
        else if (ValueFormEditorGizmoToolbar.TRANSLATE_SPEED.equals(id))
        {
            return UIKeys.FILM_GIZMO_TRANSLATE_SPEED;
        }

        return IKey.raw(id);
    }
}
