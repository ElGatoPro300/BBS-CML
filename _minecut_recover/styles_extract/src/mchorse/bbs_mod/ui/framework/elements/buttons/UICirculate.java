package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UICirculate extends UIClickable<UICirculate>
{
    public IKey label;

    public boolean custom;
    public int customColor;

    protected List<IKey> labels = new ArrayList<>();
    protected Set<Integer> disabled = new HashSet<>();
    protected int value = 0;

    public UICirculate(Consumer<UICirculate> callback)
    {
        super(callback);

        this.h(20);
    }

    public UICirculate color(int color)
    {
        this.custom = true;
        this.customColor = color & Colors.RGB;

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
    protected void renderSkin(UIContext context)
    {
        mchorse.bbs_mod.ui.framework.styles.UIStyle style = mchorse.bbs_mod.ui.framework.styles.UIStyle.active();
        int color = Colors.A100 | (this.custom ? this.customColor : (style.accent() & Colors.RGB));

        if (this.hover)
        {
            color = Colors.mulRGB(color, 0.85F);
        }

        if (mchorse.bbs_mod.ui.framework.styles.UIStyle.isMinecut() && !this.custom)
        {
            style.drawSoftRect(context.batcher, this.area.x, this.area.y, this.area.w, this.area.h,
                this.hover ? style.inner() : style.elevated());
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), style.borderSoft());
        }
        else
        {
            this.area.render(context.batcher, color);
        }

        FontRenderer font = context.batcher.getFont();
        String label = font.limitToWidth(this.label.get(), this.area.w - 4);
        int x = this.area.mx(font.getWidth(label));
        int y = this.area.my(font.getHeight());
        int textColor = mchorse.bbs_mod.ui.framework.styles.UIStyle.isMinecut()
            ? (this.hover ? style.text() : style.textDim())
            : Colors.mulRGB(Colors.WHITE, this.hover ? 0.9F : 1F);

        context.batcher.textShadow(label, x, y, textColor);

        this.renderLockedArea(context);
    }
}
