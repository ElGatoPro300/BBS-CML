package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.Collections;
import java.util.List;

/**
 * Shared hint card and pulse animation helpers for timeline interaction modes.
 */
public final class TimelineInteractionHints
{
    /**
     * Precomputed layout for an interaction hint card (position, size, wrapped lines).
     */
    public static final class HintCard
    {
        public final int x;
        public final int y;
        public final int w;
        public final int h;
        public final int textX;
        public final int textY;
        public final int lineHeight;
        public final List<String> lines;

        public HintCard(int x, int y, int w, int h, int textX, int textY, int lineHeight, List<String> lines)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.textX = textX;
            this.textY = textY;
            this.lineHeight = lineHeight;
            this.lines = lines;
        }
    }

    public static float getPulseAlpha(float minAlpha, float maxAlpha)
    {
        float t = (System.currentTimeMillis() % TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS)
            / (float) TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS;
        float wave = 0.5F + 0.5F * (float) Math.sin(t * (float) (Math.PI * 2D));

        return MathUtils.clamp(minAlpha + wave * (maxAlpha - minAlpha), minAlpha, maxAlpha);
    }

    public static float getPulseAlpha(float maxAlpha)
    {
        return getPulseAlpha(0F, maxAlpha);
    }

    public static void renderPulsingTickColumn(UIContext context, int x, int y, int ey)
    {
        float alpha = getPulseAlpha(
            TimelineToolbarSettings.INTERACTION_TICK_PULSE_MIN_ALPHA,
            TimelineToolbarSettings.INTERACTION_TICK_PULSE_MAX_ALPHA);
        int color = Colors.setA(Colors.WHITE, alpha);
        int w = TimelineToolbarSettings.INTERACTION_TICK_COLUMN_WIDTH;

        context.batcher.box(x, y, x + w, ey, color);
    }

    public static void renderHint(UIContext context, Area area, IKey hint, UIElement source)
    {
        renderHint(context, area, hint, TimelineToolbarDockLayout.findDockFor(source));
    }

    /**
     * Viewport interaction hint: always bottom-left of the preview area.
     * <p>
     * When no overlay popup is open, the card is deferred to
     * {@link UIContext#postRender()} so it draws above the 3D viewport chrome.
     * While a toolbar menu or context menu is open, it is drawn during panel
     * rendering so those popups stay on top.
     */
    public static void renderViewportHint(UIContext context, Area viewport, IKey hint, int bottomReserve)
    {
        if (hint == null)
        {
            return;
        }

        HintCard card = layoutHint(context.batcher.getFont(), viewport, null, bottomReserve, hint.get());

        if (context.shouldDeferInteractionHintToForeground())
        {
            context.drawForegroundInteractionHint(card);
        }
        else
        {
            drawHintCard(context.batcher, card);
        }
    }

    public static void renderHint(UIContext context, Area area, IKey hint, TimelineToolbarDock dock)
    {
        if (hint == null)
        {
            return;
        }

        HintCard card = layoutHint(context.batcher.getFont(), area, dock, 0, hint.get());

        drawHintCard(context.batcher, card);
    }

    /**
     * Draw a full interaction hint card (primary glow, background, optional border,
     * wrapped text). Used directly and from {@link UIContext#postRender()}.
     */
    public static void drawHintCard(Batcher2D batcher, HintCard card)
    {
        int cardEx = card.x + card.w;
        int cardEy = card.y + card.h;

        batcher.dropShadow(
            card.x,
            card.y,
            cardEx,
            cardEy,
            TimelineToolbarSettings.MENU_SHADOW_OFFSET,
            TimelineToolbarSettings.getMenuShadowInner(),
            TimelineToolbarSettings.getMenuShadowOuter());
        batcher.box(card.x, card.y, cardEx, cardEy, TimelineToolbarSettings.INTERACTION_HINT_BACKGROUND);

        if (TimelineToolbarSettings.INTERACTION_HINT_DRAW_BORDER)
        {
            batcher.outline(card.x, card.y, cardEx, cardEy, TimelineToolbarSettings.INTERACTION_HINT_BORDER);
        }

        int lineY = card.textY;

        for (String line : card.lines)
        {
            batcher.text(line, card.textX, lineY, TimelineToolbarSettings.INTERACTION_HINT_FG, false);
            lineY += card.lineHeight;
        }
    }

    public static HintCard layoutHint(FontRenderer font, Area bounds, TimelineToolbarDock dock, int bottomReserve,
        String text)
    {
        int margin = TimelineToolbarSettings.INTERACTION_HINT_MARGIN;
        int padX = TimelineToolbarSettings.INTERACTION_HINT_PADDING_X;
        int padY = TimelineToolbarSettings.INTERACTION_HINT_PADDING_Y;
        int maxTextW = Math.max(8, bounds.w - margin * 2 - padX * 2);
        List<String> lines = font.wrap(text, maxTextW);

        if (lines.isEmpty())
        {
            lines = Collections.singletonList(text == null ? "" : text);
        }

        int contentW = 0;

        for (String line : lines)
        {
            contentW = Math.max(contentW, font.getWidth(line));
        }

        int lineHeight = font.getRenderer().fontHeight;
        int textBlockH = lines.size() <= 1 ? lineHeight : (lines.size() - 1) * lineHeight + lineHeight;
        int cardW = contentW + padX * 2;
        int cardH = textBlockH + padY * 2 - TimelineToolbarSettings.INTERACTION_HINT_BOTTOM_TRIM;
        int textX = 0;
        int textY = 0;
        int cardX;
        int cardY;

        if (dock == null)
        {
            cardX = bounds.x + margin;
            cardY = bounds.ey() - cardH - margin - bottomReserve;
        }
        else
        {
            switch (dock)
            {
                case TOP:
                    cardX = bounds.x + margin;
                    cardY = bounds.y + margin;
                    break;
                case LEFT:
                    cardX = bounds.x + margin;
                    cardY = bounds.ey() - cardH - margin;
                    break;
                case RIGHT:
                    cardX = bounds.ex() - cardW - margin;
                    cardY = bounds.ey() - cardH - margin;
                    break;
                case BOTTOM:
                default:
                    cardX = bounds.x + margin;
                    cardY = bounds.ey() - cardH - margin;
                    break;
            }
        }

        textX = cardX + padX;
        textY = cardY + padY + TimelineToolbarSettings.INTERACTION_HINT_TEXT_Y_OFFSET;

        return new HintCard(cardX, cardY, cardW, cardH, textX, textY, lineHeight, lines);
    }

    private TimelineInteractionHints()
    {}
}
