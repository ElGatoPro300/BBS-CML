package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

import org.joml.Vector3d;

import java.util.function.Consumer;

/**
 * Active viewport interaction: click a point in the 3D preview to confirm.
 */
public final class ViewportInteractionState
{
    public final IKey hint;
    public final Consumer<Vector3d> onConfirm;

    public ViewportInteractionState(IKey hint, Consumer<Vector3d> onConfirm)
    {
        this.hint = hint;
        this.onConfirm = onConfirm;
    }
}
