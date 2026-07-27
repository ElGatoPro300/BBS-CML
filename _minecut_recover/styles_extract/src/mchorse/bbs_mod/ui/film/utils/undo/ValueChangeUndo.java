package mchorse.bbs_mod.ui.film.utils.undo;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.undo.IUndo;

public class ValueChangeUndo extends FilmEditorUndo
{
    public DataPath name;
    public BaseType oldValue;
    public BaseType newValue;
    public MapType uiBefore;
    public MapType uiAfter;

    private boolean mergable = true;

    public ValueChangeUndo(DataPath name, BaseType oldValue, BaseType newValue)
    {
        this.name = name;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public MapType getUIData(boolean redo)
    {
        return redo ? this.uiAfter : this.uiBefore;
    }

    public void cacheBefore(MapType uiData)
    {
        this.uiBefore = uiData == null ? new MapType() : (MapType) uiData.copy();
    }

    public void cacheAfter(UIElement editor)
    {
        this.uiAfter = editor.getRoot() == null ? new MapType() : (MapType) editor.getRoot().collectAllUndoData().copy();
    }

    public DataPath getName()
    {
        return this.name;
    }

    @Override
    public IUndo<ValueGroup> noMerging()
    {
        this.mergable = false;

        return this;
    }

    @Override
    public boolean isMergeable(IUndo<ValueGroup> undo)
    {
        if (!this.mergable)
        {
            return false;
        }

        if (undo instanceof ValueChangeUndo)
        {
            ValueChangeUndo valueUndo = (ValueChangeUndo) undo;

            return this.name.equals(valueUndo.getName());
        }

        return false;
    }

    @Override
    public void merge(IUndo<ValueGroup> undo)
    {
        if (undo instanceof ValueChangeUndo)
        {
            ValueChangeUndo prop = (ValueChangeUndo) undo;

            this.newValue = prop.newValue;
        }
    }

    @Override
    public void undo(ValueGroup context)
    {
        this.apply(context, this.oldValue);
    }

    @Override
    public void redo(ValueGroup context)
    {
        this.apply(context, this.newValue);
    }

    private void apply(ValueGroup context, BaseType data)
    {
        BaseValue value = context.getRecursively(this.name);

        if (value.getPath().equals(this.name))
        {
            BaseType normalizedData = this.normalizeData(value, data);

            if (normalizedData == null)
            {
                return;
            }

            value.preNotify();
            value.fromData(normalizedData);
            value.postNotify();
        }
    }

    private BaseType normalizeData(BaseValue value, BaseType data)
    {
        if (data != null)
        {
            return data;
        }

        if (value instanceof ValueList || value instanceof Clips)
        {
            return new ListType();
        }
        else if (value instanceof ValueGroup)
        {
            return new MapType();
        }

        return null;
    }
}