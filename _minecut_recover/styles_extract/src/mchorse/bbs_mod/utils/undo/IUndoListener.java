package mchorse.bbs_mod.utils.undo;

public interface IUndoListener<T>
{
    public void handleUndo(IUndo<T> undo, boolean redo);
}