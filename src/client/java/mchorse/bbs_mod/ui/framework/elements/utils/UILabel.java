package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class UILabel extends UIElement implements ITextColoring
{
    private static final int LINE_HEIGHT = 12;

    public IKey label;
    public int color;
    public boolean textShadow = true;
    public float anchorX;
    public float anchorY;
    public int background;
    public int textOffsetY;
    public Supplier<Integer> backgroundColor;

    private boolean wrapping;
    private List<String> wrappedLines;
    private String lastWrappedText;
    private int lastWrapWidth = -1;
    private int wrappedHeight = -1;

    public UILabel(IKey label)
    {
        this(label, Colors.WHITE);
    }

    public UILabel(IKey label, int color)
    {
        super();

        this.label = label;
        this.color = color;
    }

    @Override
    public void setColor(int color, boolean shadow)
    {
        this.color(color, shadow);
    }

    public UILabel color(int color)
    {
        return this.color(color, true);
    }

    public UILabel color(int color, boolean textShadow)
    {
        this.textShadow = textShadow;
        this.color = color;

        return this;
    }

    public UILabel background()
    {
        return this.background(Colors.A50);
    }

    public UILabel background(int color)
    {
        this.background = color;

        return this;
    }

    public UILabel background(Supplier<Integer> color)
    {
        this.backgroundColor = color;

        return this;
    }

    public UILabel labelAnchor(float x, float y)
    {
        this.anchorX = x;
        this.anchorY = y;

        return this;
    }

    /**
     * Extra pixels added to the text draw Y (e.g. keep accents inside a taller label box).
     */
    public UILabel textOffsetY(int offset)
    {
        this.textOffsetY = offset;

        return this;
    }

    /**
     * Wrap long labels onto multiple lines instead of truncating with ellipsis.
     */
    public UILabel wrapping()
    {
        return this.wrapping(true);
    }

    public UILabel wrapping(boolean wrapping)
    {
        this.wrapping = wrapping;
        this.invalidateWrappedLabel();

        return this;
    }

    public UILabel label(IKey label)
    {
        this.label = label;
        this.invalidateWrappedLabel();

        return this;
    }

    @Override
    public void resize()
    {
        super.resize();

        this.invalidateWrappedLabel();
    }

    private void invalidateWrappedLabel()
    {
        this.wrappedLines = null;
        this.lastWrappedText = null;
        this.lastWrapWidth = -1;
    }

    private void ensureWrappedLabel(FontRenderer font, int maxWidth)
    {
        String text = this.label == null ? "" : this.label.get();

        if (this.wrappedLines != null && text.equals(this.lastWrappedText) && maxWidth == this.lastWrapWidth)
        {
            return;
        }

        List<String> lines = text.isEmpty() || maxWidth <= 0
            ? Collections.emptyList()
            : font.wrap(text, maxWidth);
        int lineCount = Math.max(1, lines.isEmpty() ? 1 : lines.size());
        int height = Math.max(font.getHeight(), lineCount * LINE_HEIGHT - (LINE_HEIGHT - font.getHeight()));

        if (height != this.wrappedHeight)
        {
            this.wrappedHeight = height;
            this.h(height);

            UIElement container = this.getParentContainer();

            if (container != null)
            {
                /* Parent resize clears caches via resize(); restore lines afterward. */
                container.resize();
            }
        }

        this.wrappedLines = lines;
        this.lastWrappedText = text;
        this.lastWrapWidth = maxWidth;
    }

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        int background = this.backgroundColor == null ? this.background : this.backgroundColor.get();

        if (this.wrapping && this.area.w > 0)
        {
            this.ensureWrappedLabel(font, Math.max(0, this.area.w - 4));

            if (!this.wrappedLines.isEmpty())
            {
                int maxLineWidth = 0;

                for (String line : this.wrappedLines)
                {
                    maxLineWidth = Math.max(maxLineWidth, font.getWidth(line));
                }

                int textHeight = this.wrappedLines.size() * LINE_HEIGHT - (LINE_HEIGHT - font.getHeight());
                int x = this.area.x(this.anchorX, maxLineWidth);
                int y = this.area.y(this.anchorY, textHeight) + this.textOffsetY;
                int a = background >> 24 & 0xff;

                if (a != 0)
                {
                    context.batcher.box(x - 3, y - 3, x + maxLineWidth + 3 - 1, y + textHeight + 3, background);
                }

                for (String line : this.wrappedLines)
                {
                    context.batcher.text(line, x, y, this.color, this.textShadow);
                    y += LINE_HEIGHT;
                }
            }
        }
        else
        {
            String label = font.limitToWidth(this.label.get(), this.area.w - 4);
            int x = this.area.x(this.anchorX, font.getWidth(label));
            int y = this.area.y(this.anchorY, font.getHeight()) + this.textOffsetY;

            context.batcher.textCard(label, x, y, this.color, background, 3, this.textShadow);
        }

        super.render(context);
    }
}
