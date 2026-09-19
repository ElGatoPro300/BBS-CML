package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.NaturalOrderComparator;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.FilteredLink;

import java.util.List;
import java.util.function.Consumer;

public class UIFilteredLinkList extends UIList<FilteredLink>
{
    public UIFilteredLinkList(Consumer<List<FilteredLink>> callback)
    {
        super(callback);

        this.scroll.scrollItemSize = 20;
    }

    @Override
    protected boolean sortElements()
    {
        this.list.sort((a, b) -> NaturalOrderComparator.compare(true, a.toString(), b.toString()));

        return true;
    }

    @Override
    protected void renderElementPart(UIContext context, FilteredLink element, int i, int x, int y, boolean hover, boolean selected)
    {
        int size = this.scroll.scrollItemSize;
        int preview = Math.max(12, size - 4);
        int previewX = x + 2;
        int previewY = y + (size - preview) / 2;

        context.batcher.box(previewX, previewY, previewX + preview, previewY + preview, Colors.A25);
        context.batcher.outline(previewX, previewY, previewX + preview, previewY + preview, Colors.A50);

        Texture texture = element == null || element.path == null ? null : BBSModClient.getTextures().getTexture(element.path);

        if (texture != null)
        {
            int width = texture.width;
            int height = texture.height;

            if (width > 0 && height > 0)
            {
                float scale = Math.min(preview / (float) width, preview / (float) height);
                int drawWidth = Math.max(1, Math.round(width * scale));
                int drawHeight = Math.max(1, Math.round(height * scale));
                int drawX = previewX + (preview - drawWidth) / 2;
                int drawY = previewY + (preview - drawHeight) / 2;

                context.batcher.fullTexturedBox(texture, drawX, drawY, drawWidth, drawHeight);
            }
            else
            {
                context.batcher.fullTexturedBox(texture, previewX, previewY, preview, preview);
            }
        }

        String name = element == null || element.path == null ? "" : StringUtils.fileName(element.path.path);

        if (name == null || name.isEmpty())
        {
            name = element == null ? "" : element.toString();
        }

        int textColor = hover ? Colors.HIGHLIGHT : Colors.WHITE;
        int textX = previewX + preview + 6;
        int textY = y + (size - context.batcher.getFont().getHeight()) / 2;
        int maxWidth = Math.max(1, this.area.w - (textX - this.area.x) - 6 - this.scroll.getScrollbarWidth());

        context.batcher.textShadow(context.batcher.getFont().limitToWidth(name, maxWidth), textX, textY, textColor);
    }
}
