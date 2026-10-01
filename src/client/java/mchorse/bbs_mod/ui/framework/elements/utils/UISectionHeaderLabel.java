package mchorse.bbs_mod.ui.framework.elements.utils;

import java.util.Collections;
import java.util.List;

/**
 * Shared 1–2 line title layout for foldable section headers
 * ({@code UISection}, {@code UITrackStyleSectionHeader}).
 */
public final class UISectionHeaderLabel
{
    public static final int MAX_LINES = 2;
    public static final int LINE_HEIGHT = 11;

    private UISectionHeaderLabel()
    {
    }

    public static List<String> wrap(FontRenderer font, String text, int maxWidth)
    {
        if (font == null || text == null || text.isEmpty() || maxWidth <= 0)
        {
            return Collections.emptyList();
        }

        return font.wrapToMaxLines(text, maxWidth, MAX_LINES);
    }

    /**
     * Pixel height of the wrapped block (tight to glyph height on the last line).
     */
    public static int textHeight(FontRenderer font, int lineCount)
    {
        int lines = Math.max(1, lineCount);
        int glyph = font == null ? 9 : font.getHeight();

        return lines * LINE_HEIGHT - (LINE_HEIGHT - glyph);
    }

    public static int barHeight(FontRenderer font, int lineCount, int minHeight, int verticalPadding)
    {
        return Math.max(minHeight, textHeight(font, lineCount) + verticalPadding);
    }
}
