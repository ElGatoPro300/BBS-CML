package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UICirculate extends UIClickable<UICirculate>
{
    private static final int MIN_HEIGHT = 20;
    private static final int LINE_HEIGHT = 12;
    private static final int VERTICAL_PADDING = 6;
    private static final int MAX_LABEL_LINES = 3;

    public IKey label;

    public boolean custom;
    public int customColor;

    protected List<IKey> labels = new ArrayList<>();
    protected Set<Integer> disabled = new HashSet<>();
    protected int value = 0;

    private boolean wrapping;
    private List<String> wrappedLines;
    private String lastWrappedText;
    private int lastWrapWidth = -1;
    private int wrappedHeight = MIN_HEIGHT;

    public UICirculate(Consumer<UICirculate> callback)
    {
        super(callback);

        this.h(MIN_HEIGHT);
    }

    public UICirculate color(int color)
    {
        this.custom = true;
        this.customColor = color & Colors.RGB;

        return this;
    }

    /**
     * Wrap long labels onto multiple lines and grow height instead of a single-line ellipsis.
     */
    public UICirculate wrapping()
    {
        return this.wrapping(true);
    }

    public UICirculate wrapping(boolean wrapping)
    {
        this.wrapping = wrapping;
        this.invalidateWrappedLabel();

        return this;
    }

    public List<IKey> getLabels()
    {
        return this.labels;
    }

    public void addLabel(IKey label)
    {
        if (this.labels.isEmpty())
        {
            this.label = label;
        }

        this.labels.add(label);
        this.invalidateWrappedLabel();
    }

    public void disable(int value)
    {
        if (this.disabled.size() < this.labels.size())
        {
            this.disabled.add(value);
        }
    }

    public int getValue()
    {
        return this.value;
    }

    public String getLabel()
    {
        return this.labels.get(this.value).get();
    }

    public void setValue(int value)
    {
        this.setValue(value, 1);
    }

    public void setValue(int value, int direction)
    {
        this.value = value;

        if (this.disabled.contains(value))
        {
            this.setValue(value + direction, direction);

            return;
        }

        if (this.value > this.labels.size() - 1)
        {
            this.value = 0;
        }

        if (this.value < 0)
        {
            this.value = this.labels.size() - 1;
        }

        this.label = this.labels.get(this.value);
        this.invalidateWrappedLabel();
    }

    @Override
    protected boolean isAllowed(int mouseButton)
    {
        return mouseButton == 0 || mouseButton == 1;
    }

    @Override
    protected void click(int mouseButton)
    {
        int direction = mouseButton == 0 ? 1 : -1;

        this.setValue(this.value + direction, direction);

        super.click(mouseButton);
    }

    @Override
    protected UICirculate get()
    {
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

        List<String> lines;

        if (text.isEmpty() || maxWidth <= 0)
        {
            lines = Collections.emptyList();
        }
        else if (this.wrapping)
        {
            lines = this.limitWrappedLines(font, font.wrap(text, maxWidth), maxWidth);
        }
        else
        {
            lines = Collections.singletonList(font.limitToWidth(text, maxWidth));
        }

        int lineCount = Math.max(1, lines.isEmpty() ? 1 : lines.size());
        int textHeight = lineCount * LINE_HEIGHT - (LINE_HEIGHT - font.getHeight());
        int height = Math.max(MIN_HEIGHT, textHeight + VERTICAL_PADDING);

        if (this.wrapping && height != this.wrappedHeight)
        {
            this.wrappedHeight = height;
            this.h(height);

            UIElement container = this.getParentContainer();

            if (container != null)
            {
                container.resize();
            }
        }

        this.wrappedLines = lines;
        this.lastWrappedText = text;
        this.lastWrapWidth = maxWidth;
    }

    private List<String> limitWrappedLines(FontRenderer font, List<String> lines, int maxWidth)
    {
        if (lines.size() <= MAX_LABEL_LINES)
        {
            return lines;
        }

        List<String> limited = new ArrayList<>(lines.subList(0, MAX_LABEL_LINES));

        limited.set(MAX_LABEL_LINES - 1, font.limitToWidth(limited.get(MAX_LABEL_LINES - 1), maxWidth));

        return limited;
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        int color = Colors.A100 | (this.custom ? this.customColor : BBSSettings.primaryColor.get());

        if (this.hover)
        {
            color = Colors.mulRGB(color, 0.85F);
        }

        this.area.render(context.batcher, color);

        FontRenderer font = context.batcher.getFont();
        int maxWidth = Math.max(0, this.area.w - 6);

        this.ensureWrappedLabel(font, maxWidth);

        List<String> lines = this.wrappedLines == null ? Collections.emptyList() : this.wrappedLines;
        int lineCount = Math.max(1, lines.isEmpty() ? 1 : lines.size());
        int textHeight = lineCount * LINE_HEIGHT - (LINE_HEIGHT - font.getHeight());
        int y = this.area.my(textHeight);
        int textColor = Colors.mulRGB(Colors.WHITE, this.hover ? 0.9F : 1F);

        if (lines.isEmpty())
        {
            context.batcher.textShadow("", this.area.mx(0), y, textColor);
        }
        else
        {
            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i);
                int x = this.area.mx(font.getWidth(line));

                context.batcher.textShadow(line, x, y + i * LINE_HEIGHT, textColor);
            }
        }

        this.renderLockedArea(context);
    }
}
