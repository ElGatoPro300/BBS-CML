package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.film.replays.Replay;

/**
 * Row in {@link UIReplayList}: category header (expand/collapse) or a replay.
 */
public final class ReplayListEntry
{
    public enum Kind
    {
        FOLDER,
        REPLAY
    }

    public final Kind kind;
    public final String folderName;
    public final Replay replay;
    /** Horizontal inset for replay rows under a category header. */
    public final int indent;

    private ReplayListEntry(Kind kind, String folderName, Replay replay, int indent)
    {
        this.kind = kind;
        this.folderName = folderName == null ? "" : folderName;
        this.replay = replay;
        this.indent = indent;
    }

    public static ReplayListEntry folder(String name)
    {
        return new ReplayListEntry(Kind.FOLDER, name, null, 0);
    }

    public static ReplayListEntry replay(Replay replay)
    {
        return replay(replay, 0);
    }

    public static ReplayListEntry replay(Replay replay, int indent)
    {
        return new ReplayListEntry(Kind.REPLAY, "", replay, indent);
    }

    public boolean isReplay()
    {
        return this.kind == Kind.REPLAY;
    }

    public boolean isFolder()
    {
        return this.kind == Kind.FOLDER;
    }
}
