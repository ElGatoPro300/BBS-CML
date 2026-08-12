package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;

/**
 * Screen-edge aware positioning for timeline toolbar hover cards (collapsed
 * section labels, drag handle, menu disabled reasons).
 */
public final class TimelineToolbarTooltips
{
    /**
     * Default {@link Batcher2D#textCard}
     * background/text padding.
     */
    private static final int TEXT_CARD_PADDING = 3;

    public static void drawForeground(UIContext context, String text, int mouseX, int mouseY, int color,
        int background, TimelineToolbarDock dock, boolean anchorLeftOfCursor)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }

        FontRenderer font = context.batcher.getFont();
        int[] pos = computeTextPosition(context, font, text, mouseX, mouseY, dock, anchorLeftOfCursor);

        context.drawForegroundTextCard(text, pos[0], pos[1], color, background);
    }

    public static int[] computeTextPosition(UIContext context, FontRenderer font, String text, int mouseX,
        int mouseY, TimelineToolbarDock dock, boolean anchorLeftOfCursor)
    {
        int offsetX = TimelineToolbarSettings.TOOLTIP_CURSOR_OFFSET_X;
        int offsetXLeft = TimelineToolbarSettings.TOOLTIP_CURSOR_OFFSET_X_LEFT;
        int offsetY = TimelineToolbarSettings.TOOLTIP_CURSOR_OFFSET_Y;
        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int textW = font.getWidth(text);
        int textH = font.getHeight();
        int screenW = context.menu.width;
        int screenH = context.menu.height;
        int x;
        int y = mouseY + offsetY;

        if (dock == TimelineToolbarDock.LEFT && anchorLeftOfCursor)
        {
            /* Below-left of the cursor so the card opens away from open menus on the right. */
            x = mouseX - offsetXLeft - textW;
        }
        else
        {
            /* Below-right of the cursor hotspot (the pointer tip). */
            x = mouseX + offsetX;
        }

        x = shiftIntoHorizontalBounds(x, textW, margin, screenW);
        y = shiftIntoVerticalBounds(y, textH, margin, screenH);

        return new int[] {x, y};
    }

    private static int getCardRight(int x, int textW)
    {
        return x + textW + TEXT_CARD_PADDING - 1;
    }

    private static int getCardBottom(int y, int textH)
    {
        return y + textH + TEXT_CARD_PADDING;
    }

    /**
     * Slides the card the minimum amount needed to stay on screen while keeping
     * the default below-right anchor. Avoids mirroring to the opposite side of
     * the cursor, which doubles the perceived gap (~2x offset).
     */
    private static int shiftIntoHorizontalBounds(int x, int textW, int margin, int screenW)
    {
        int pad = TEXT_CARD_PADDING;
        int cardRight = getCardRight(x, textW);

        if (cardRight > screenW - margin)
        {
            x -= cardRight - (screenW - margin);
        }

        int cardLeft = x - pad;

        if (cardLeft < margin)
        {
            x += margin - cardLeft;
        }

        return x;
    }

    private static int shiftIntoVerticalBounds(int y, int textH, int margin, int screenH)
    {
        int pad = TEXT_CARD_PADDING;
        int cardBottom = getCardBottom(y, textH);

        if (cardBottom > screenH - margin)
        {
            y -= cardBottom - (screenH - margin);
        }

        int cardTop = y - pad;

        if (cardTop < margin)
        {
            y += margin - cardTop;
        }

        return y;
    }

    private TimelineToolbarTooltips()
    {}
}
