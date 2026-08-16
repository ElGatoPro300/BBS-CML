package mchorse.bbs_mod.actions;

public enum ActionState
{
    SEEK, PLAY, PAUSE, RESTART, STOP, RESTORE, PUPPET,
    /**
     * Move the film tick without walking {@code goTo} (no swipe / block /
     * drop re-fire). Used after viewport recording restores the start cursor.
     */
    SYNC;
}