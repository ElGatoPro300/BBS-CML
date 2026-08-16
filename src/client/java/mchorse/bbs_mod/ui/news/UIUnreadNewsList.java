package mchorse.bbs_mod.ui.news;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UIUnreadNewsList extends UIStringList
{
    private Set<String> unreadIds;
    private List<String> ids;

    public UIUnreadNewsList(Consumer<List<String>> callback)
    {
        super(callback);
    }

    public void bindIds(List<String> ids, Set<String> unreadIds)
    {
        this.ids = ids;
        this.unreadIds = unreadIds;
    }

    @Override
    protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
    {
        int color = hover ? Colors.HIGHLIGHT : Colors.WHITE;
        int bx = x + 10;

        if (this.ids != null && this.unreadIds != null && i >= 0 && i < this.ids.size())
        {
            String id = this.ids.get(i);

            if (this.unreadIds.contains(id))
            {
                context.batcher.icon(Icons.NOTIFICATION, color, bx, y + this.scroll.scrollItemSize / 2F, 0.5F, 0.5F);
                bx += 16;
            }
        }

        context.batcher.textShadow(element, bx, y + 4, color);
    }
}
