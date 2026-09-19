package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active keyframe paste interaction (preview + click to confirm).
 */
public final class KeyframePasteInteractionState
{
    public final IKey hint;
    /**
     * When {@code >= 0}, the anchor tick is fixed (playhead). When {@code < 0},
     * the anchor tick follows the cursor horizontally.
     */
    public final float lockedTick;

    public KeyframePasteInteractionState(IKey hint, float lockedTick)
    {
        this.hint = hint;
        this.lockedTick = lockedTick;
    }

    public static KeyframePasteInteractionState atCursor(IKey hint)
    {
        return new KeyframePasteInteractionState(hint, -1F);
    }

    public static KeyframePasteInteractionState atPlayhead(IKey hint, float tick)
    {
        return new KeyframePasteInteractionState(hint, tick);
    }
}
