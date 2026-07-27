package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;

/**
 * Captures the active timeline interaction mode so it can survive a keyframe
 * editor rebuild (e.g. expanding/collapsing track groups in the replay timeline).
 */
public final class TimelineInteractionSnapshot
{
    private TimelineInteractionState trackPick;
    private KeyframeInsertInteractionState insert;
    private KeyframeDuplicateInteractionState duplicate;
    private KeyframePasteInteractionState paste;
    private KeyframeSelectNeighborInteractionState selectNeighbor;
    private KeyframeSelectSameInteractionState selectSame;
    private boolean scalingHints;
    private boolean stackingHints;

    public static TimelineInteractionSnapshot capture(UIKeyframes keyframes)
    {
        TimelineInteractionSnapshot snapshot = new TimelineInteractionSnapshot();

        if (keyframes == null)
        {
            return snapshot;
        }

        if (keyframes.getInsertInteractionState() != null)
        {
            snapshot.insert = keyframes.getInsertInteractionState();
        }
        else if (keyframes.getTrackInteractionState() != null)
        {
            snapshot.trackPick = keyframes.getTrackInteractionState();
        }
        else if (keyframes.getDuplicateInteractionState() != null)
        {
            snapshot.duplicate = keyframes.getDuplicateInteractionState();
        }
        else if (keyframes.getPasteInteractionState() != null)
        {
            snapshot.paste = keyframes.getPasteInteractionState();
        }
        else if (keyframes.getSelectNeighborInteractionState() != null)
        {
            snapshot.selectNeighbor = keyframes.getSelectNeighborInteractionState();
        }
        else if (keyframes.getSelectSameInteractionState() != null)
        {
            snapshot.selectSame = keyframes.getSelectSameInteractionState();
        }
        else if (keyframes.isScalingWithInteractionHints())
        {
            snapshot.scalingHints = true;
        }
        else if (keyframes.isStackingWithInteractionHints())
        {
            snapshot.stackingHints = true;
        }

        return snapshot;
    }

    public boolean isEmpty()
    {
        return this.trackPick == null
            && this.insert == null
            && this.duplicate == null
            && this.paste == null
            && this.selectNeighbor == null
            && this.selectSame == null
            && !this.scalingHints
            && !this.stackingHints;
    }

    public void restore(UIKeyframes keyframes)
    {
        if (keyframes == null || this.isEmpty())
        {
            return;
        }

        if (this.insert != null)
        {
            keyframes.enterKeyframeInsert(this.insert);
        }
        else if (this.trackPick != null)
        {
            keyframes.enterTrackInteraction(this.trackPick.hint, this.trackPick.eligible, this.trackPick.onConfirm);
        }
        else if (this.duplicate != null)
        {
            keyframes.enterKeyframeDuplicate(this.duplicate);
        }
        else if (this.paste != null)
        {
            keyframes.enterKeyframePaste(this.paste);
        }
        else if (this.selectNeighbor != null)
        {
            keyframes.enterSelectNeighborInteraction(this.selectNeighbor);
        }
        else if (this.selectSame != null)
        {
            keyframes.enterSelectSameInteraction(this.selectSame);
        }
        else if (this.scalingHints)
        {
            keyframes.restoreScalingInteractionHints();
        }
        else if (this.stackingHints)
        {
            keyframes.restoreStackingInteractionHints();
        }
    }

    private TimelineInteractionSnapshot()
    {}
}
