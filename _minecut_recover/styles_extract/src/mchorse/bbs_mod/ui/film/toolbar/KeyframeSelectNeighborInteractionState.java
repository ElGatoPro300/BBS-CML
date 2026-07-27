package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Pick a track position to select the previous or next keyframe when none is selected.
 */
public final class KeyframeSelectNeighborInteractionState
{
    public final IKey hint;
    public final int direction;

    public KeyframeSelectNeighborInteractionState(IKey hint, int direction)
    {
        this.hint = hint;
        this.direction = direction;
    }
}
