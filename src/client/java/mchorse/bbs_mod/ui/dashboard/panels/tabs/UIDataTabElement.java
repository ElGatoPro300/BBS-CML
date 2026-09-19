package mchorse.bbs_mod.ui.dashboard.panels.tabs;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIDataTabElement extends UIClickable<UIDataTabElement>
{
    private static final int RIGHT_GAP = 0;
    private static final int ICON_X = 4;
    private static final int ICON_SIZE = 12;
    private static final int ICON_GAP = 4;
    private static final int TEXT_X = ICON_X + ICON_SIZE + ICON_GAP;
    private static final int TEXT_RIGHT_PADDING = 6;
    private static final int CLOSE_SIZE = 12;
    private static final int CLOSE_GAP = 4;
    private static final int CLOSE_ZONE = CLOSE_SIZE + TEXT_RIGHT_PADDING;

    private IKey label;
    private Icon icon;
    private DataTab tab;
    private final UIDataDashboardPanel<?> panel;
    private final UIIcon close;

    public UIDataTabElement(UIDataDashboardPanel<?> panel, int h)
    {
        super(null);

        this.panel = panel;
        this.h(h);
        this.label = IKey.raw("");
        this.icon = Icons.SEARCH;

        this.callback = (b) -> this.panel.switchTab(this.tab);

        this.close = new UIIcon(Icons.CLOSE, (b) -> this.panel.closeTab(this.tab));
        this.close.relative(this).x(1F, -(CLOSE_SIZE + CLOSE_GAP + RIGHT_GAP)).y(0.5F).w(CLOSE_SIZE).h(CLOSE_SIZE).anchor(0, 0.5F);

        this.add(this.close);

        this.context((menu) ->
        {
            menu.action(Icons.CLOSE, UIKeys.PANELS_TABS_CONTEXT_CLOSE_OTHERS, this::closeOtherTabs);
            menu.action(Icons.ARROW_LEFT, UIKeys.PANELS_TABS_CONTEXT_CLOSE_LEFT, this::closeTabsLeft);
            menu.action(Icons.ARROW_RIGHT, UIKeys.PANELS_TABS_CONTEXT_CLOSE_RIGHT, this::closeTabsRight);
        });
    }

    public static int measureWidth(FontRenderer font, IKey label)
    {
        return TEXT_X + font.getWidth(label.get()) + CLOSE_ZONE + RIGHT_GAP;
    }

    public void setTab(DataTab tab, IKey label, Icon icon)
    {
        this.tab = tab;
        this.label = label;
        this.icon = icon;
    }

    private void closeOtherTabs()
    {
        this.panel.closeOtherTabs(this.tab);
    }

    private void closeTabsLeft()
    {
        this.panel.closeTabsLeft(this.tab);
    }

    private void closeTabsRight()
    {
        this.panel.closeTabsRight(this.tab);
    }

    @Override
    protected UIDataTabElement get()
    {
        return this;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 2)
        {
            this.panel.closeTab(this.tab);

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        boolean active = this.tab != null && this.tab == this.panel.getCurrentDataTab();
        boolean hover = this.hover;

        boolean showClose = active || hover;
        this.close.setVisible(showClose);

        int ex = this.area.ex() - RIGHT_GAP;

        if (active)
        {
            int color = Colors.mulRGB(BBSSettings.primaryColor(Colors.A100), 0.2F);

            context.batcher.box(this.area.x, this.area.y, ex, this.area.ey(), color);
        }

        FontRenderer font = context.batcher.getFont();
        int iconColor = active ? Colors.WHITE : Colors.setA(Colors.WHITE, 0.7F);
        context.batcher.icon(this.icon, iconColor, this.area.x + ICON_X, this.area.my(), 0F, 0.5F);

        int right = showClose ? CLOSE_ZONE : TEXT_RIGHT_PADDING;
        String text = font.limitToWidth(this.label.get(), this.area.w - RIGHT_GAP - TEXT_X - right);
        int textColor = active ? Colors.WHITE : Colors.setA(Colors.WHITE, 0.7F);

        context.batcher.text(text, this.area.x + TEXT_X, this.area.my() - font.getHeight() / 2, textColor);
    }
}
