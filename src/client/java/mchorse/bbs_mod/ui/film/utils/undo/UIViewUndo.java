package mchorse.bbs_mod.ui.film.utils.undo;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.undo.IUndo;

/**
 * Undo entry that only restores film editor UI state (embedded views, scroll,
 * selection, etc.) without changing film data.
 */
public class UIViewUndo extends FilmEditorUndo
{
    private final MapType uiBefore;
    private final MapType uiAfter;

    public UIViewUndo(MapType uiBefore, MapType uiAfter)
    {
        this.uiBefore = uiBefore;
        this.uiAfter = uiAfter;
    }

    public MapType getUIData(boolean redo)
    {
        return redo ? this.uiAfter : this.uiBefore;
    }

    @Override
    public IUndo<ValueGroup> noMerging()
    {
        return this;
    }

    @Override
    public boolean isMergeable(IUndo<ValueGroup> undo)
    {
        return false;
    }

    @Override
    public void merge(IUndo<ValueGroup> undo)
    {}

    @Override
    public void undo(ValueGroup context)
    {}

    @Override
    public void redo(ValueGroup context)
    {}
}
