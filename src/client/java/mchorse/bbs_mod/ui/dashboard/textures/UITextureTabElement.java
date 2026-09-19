package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

public class UITextureTabElement extends UIClickable<UITextureTabElement>
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

    private int index;
    private IKey label;
    private IKey tooltip;
    private Icon icon;
    private final UITexturePainter painter;
    private final UIIcon close;

    public UITextureTabElement(UITexturePainter painter, int h)
    {
        super(null);

        this.painter = painter;
        this.h(h);
        this.label = IKey.raw("");
        this.tooltip = IKey.raw("");
        this.icon = Icons.MATERIAL;

        this.callback = (b) -> this.painter.switchTab(this.index);

        this.close = new UIIcon(Icons.CLOSE, (b) -> this.painter.closeTab(this.index));
        this.close.relative(this).x(1F, -(CLOSE_SIZE + CLOSE_GAP + RIGHT_GAP)).y(0.5F).w(CLOSE_SIZE).h(CLOSE_SIZE).anchor(0, 0.5F);

        this.add(this.close);

        this.context((menu) ->
        {
            if (!this.painter.canCloseTab(this.index))
            {
                return;
            }

            menu.action(Icons.REMOVE, UIKeys.GENERAL_REMOVE, () -> this.painter.closeTab(this.index));
            menu.action(Icons.CLOSE, UIKeys.PANELS_TABS_CONTEXT_CLOSE_OTHERS, () -> this.painter.closeOtherTabs(this.index));
            menu.action(Icons.ARROW_LEFT, UIKeys.PANELS_TABS_CONTEXT_CLOSE_LEFT, () -> this.painter.closeTabsLeft(this.index));
            menu.action(Icons.ARROW_RIGHT, UIKeys.PANELS_TABS_CONTEXT_CLOSE_RIGHT, () -> this.painter.closeTabsRight(this.index));
        });
    }

    public static int measureWidth(FontRenderer font, IKey label)
    {
        return TEXT_X + font.getWidth(label.get()) + CLOSE_ZONE + RIGHT_GAP;
    }

    public void setTab(int index, IKey label, IKey tooltip, Icon icon)
    {
        this.index = index;
        this.label = label;
        this.tooltip = tooltip;
        this.icon = icon;

        this.tooltip(tooltip, Direction.BOTTOM);
    }

    @Override
    protected UITextureTabElement get()
    {
        return this;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 2 && this.painter.canCloseTab(this.index))
        {
            this.painter.closeTab(this.index);

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        boolean active = this.index == this.painter.getCurrentTabIndex();
        boolean hover = this.hover;

        boolean canClose = this.painter.canCloseTab(this.index);
        boolean showClose = canClose && (active || hover);

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
