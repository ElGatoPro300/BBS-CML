package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Active timeline interaction session (track pick, future tick pick, etc.).
 * Created when the user invokes a toolbar action that needs a target click
 * on the timeline before running.
 */
public final class TimelineInteractionState
{
    public final IKey hint;
    public final Predicate<UIKeyframeSheet> eligible;
    public final Consumer<UIKeyframeSheet> onConfirm;

    public TimelineInteractionState(IKey hint, Predicate<UIKeyframeSheet> eligible,
        Consumer<UIKeyframeSheet> onConfirm)
    {
        this.hint = hint;
        this.eligible = eligible;
        this.onConfirm = onConfirm;
    }
}
