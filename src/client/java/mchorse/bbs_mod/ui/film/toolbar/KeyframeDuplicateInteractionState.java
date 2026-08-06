package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * Active keyframe duplicate interaction (preview + click to confirm).
 */
public final class KeyframeDuplicateInteractionState
{
    public final IKey hint;
    /**
     * When {@code >= 0}, the anchor tick is fixed (playhead). When {@code < 0},
     * the anchor tick follows the cursor horizontally.
     */
    public final float lockedTick;

    public KeyframeDuplicateInteractionState(IKey hint, float lockedTick)
    {
        this.hint = hint;
        this.lockedTick = lockedTick;
    }

    public static KeyframeDuplicateInteractionState atCursor(IKey hint)
    {
        return new KeyframeDuplicateInteractionState(hint, -1F);
    }

    public static KeyframeDuplicateInteractionState atPlayhead(IKey hint, float tick)
    {
        return new KeyframeDuplicateInteractionState(hint, tick);
    }
}
