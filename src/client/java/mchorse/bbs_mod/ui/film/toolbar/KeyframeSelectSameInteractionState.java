package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Pick a reference keyframe to select every keyframe that shares its value,
 * either across all tracks or only within the picked track.
 */
public final class KeyframeSelectSameInteractionState
{
    public final IKey hint;
    public final boolean currentTrackOnly;

    public KeyframeSelectSameInteractionState(IKey hint, boolean currentTrackOnly)
    {
        this.hint = hint;
        this.currentTrackOnly = currentTrackOnly;
    }
}
