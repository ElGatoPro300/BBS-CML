package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bone list for {@link UIPoseEditor}: supports multi-selection with the default list behavior
 * (Shift = range selection, Ctrl = toggle).
 */
public class UIPoseBoneStringList extends UIStringList
{
    public UIPoseBoneStringList(Consumer<List<String>> callback)
    {
        super(callback);

        this.multi();
    }
}
