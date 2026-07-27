package mchorse.bbs_mod.ui.framework.elements.utils;

public interface ITextColoring
{
    public default void setColor(int color)
    {
        this.setColor(color, true);
    }

    public void setColor(int color, boolean shadow);
}
