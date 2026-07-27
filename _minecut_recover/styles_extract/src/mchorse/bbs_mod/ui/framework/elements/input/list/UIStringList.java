package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.NaturalOrderComparator;

import java.util.List;
import java.util.function.Consumer;

public class UIStringList extends UIList<String>
{
    public static final int DEFAULT_HEIGHT = 16;

    public UIStringList(Consumer<List<String>> callback)
    {
        super(callback);

        this.scroll.scrollItemSize = DEFAULT_HEIGHT;
    }

    @Override
    protected boolean sortElements()
    {
        this.list.sort((a, b) -> NaturalOrderComparator.compare(true, a, b));

        return true;
    }

    @Override
    protected String elementToString(UIContext context, int i, String element)
    {
        return element;
    }
}