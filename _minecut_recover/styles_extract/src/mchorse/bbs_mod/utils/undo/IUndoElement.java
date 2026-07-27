package mchorse.bbs_mod.utils.undo;

import mchorse.bbs_mod.data.types.MapType;

public interface IUndoElement
{
    public String getUndoId();

    public void applyUndoData(MapType data);

    public void collectUndoData(MapType data);
}