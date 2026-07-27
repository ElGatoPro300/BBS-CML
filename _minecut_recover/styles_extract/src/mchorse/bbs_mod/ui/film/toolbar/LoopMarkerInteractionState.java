package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active loop In/Out marker placement (preview tick column + click to confirm).
 */
public final class LoopMarkerInteractionState
{
    public final boolean setMin;
    public final IKey hint;

    public LoopMarkerInteractionState(boolean setMin, IKey hint)
    {
        this.setMin = setMin;
        this.hint = hint;
    }
}
