package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;

import java.util.List;
import java.util.function.Consumer;

public class UIRenderQueueList extends UIList<String>
{
    public UIRenderQueueList(Consumer<List<String>> callback)
    {
        super(callback);

        this.scroll.scrollItemSize = 16;
    }

    @Override
    protected boolean sortElements()
    {
        /* Keep insertion order — render queue is ordered intentionally */
        return false;
    }

    @Override
    protected String elementToString(UIContext context, int i, String element)
    {
        return (i + 1) + ". " + element;
    }
}
