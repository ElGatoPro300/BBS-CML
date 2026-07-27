package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.ui.film.IModelTrackPlacement;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIContext;

/**
 * Drag-drop placement of Model timeline tracks from the Minecut Tracks palette.
 * Preview is a real gap between dope-sheet rows — only while the cursor is over the Replay timeline.
 */
public class UIModelTrackPlacement implements IModelTrackPlacement
{
    private String paletteType;
    private int previewIndex = -1;
    private boolean overTimeline;

    public boolean isActive()
    {
        return this.paletteType != null;
    }

    public String getPaletteType()
    {
        return this.paletteType;
    }

    public int getPreviewIndex()
    {
        return this.previewIndex;
    }

    public boolean isOverTimeline()
    {
        return this.overTimeline;
    }

    public void begin(String paletteType)
    {
        this.paletteType = paletteType;
        this.previewIndex = -1;
        this.overTimeline = false;
    }

    public void cancel()
    {
        this.paletteType = null;
        this.previewIndex = -1;
        this.overTimeline = false;
    }

    public void updatePreview(UIFilmPanel film, UIContext context)
    {
        if (this.paletteType == null || film == null || film.replayEditor == null)
        {
            return;
        }

        UIReplaysEditor editor = film.replayEditor;
        int[] hit = editor.hitTestModelTrackInsert(context.mouseX, context.mouseY);

        if (hit == null)
        {
            this.previewIndex = -1;
            this.overTimeline = false;
            editor.clearModelTrackInsertGapPreview();

            return;
        }

        this.previewIndex = hit[0];
        this.overTimeline = true;

        int gapBeforeSheet = hit.length > 1 ? hit[1] : -1;
        int gapH = hit.length > 2 ? hit[2] : 14;

        editor.applyModelTrackInsertGapPreview(gapBeforeSheet, gapH);
    }

    public boolean confirm(UIFilmPanel film)
    {
        this.cancel();

        return false;
    }

    public void renderPreview(UIContext context)
    {
        /* Gap is drawn by shifting dope-sheet rows — no overlay bar. */
    }
}
