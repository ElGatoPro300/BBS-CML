package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;

public class UITextureTabs extends UIElement
{
    public static final int TABS_HEIGHT_PX = 18;

    private static final int TAB_MIN_WIDTH = 110;
    private static final int TAB_MAX_WIDTH = 230;
    private static final int TABS_GAP = 0;

    public final UITexturePainter painter;
    public final UIScrollView scroll;
    public final UIIcon add;
    private final ArrayList<UITextureTabElement> tabs = new ArrayList<>();

    public UITextureTabs(UITexturePainter painter)
    {
        this.painter = painter;

        this.scroll = new UIScrollView(ScrollDirection.HORIZONTAL);
        this.scroll.scroll.scrollSpeed = 20;
        this.scroll.relative(this).x(TABS_GAP).w(1F, -TABS_GAP * 2).h(TABS_HEIGHT_PX);
        this.scroll.column(TABS_GAP).scroll();
        this.scroll.scroll.noScrollbar();

        this.add = new UIIcon(Icons.ADD, (b) -> this.painter.openNewTab());
        this.add.wh(TABS_HEIGHT_PX, TABS_HEIGHT_PX);

        this.add(new UIRenderable(this::renderBackground), this.scroll);
    }

    public void sync()
    {
        double scrollPos = this.scroll.scroll.getScroll();
        int count = this.painter.getTabCount();

        while (this.tabs.size() < count)
        {
            this.tabs.add(new UITextureTabElement(this.painter, TABS_HEIGHT_PX));
        }

        while (this.tabs.size() > count)
        {
            UITextureTabElement removed = this.tabs.remove(this.tabs.size() - 1);

            removed.removeFromParent();
        }

        this.scroll.removeAll();

        FontRenderer font = Batcher2D.getDefaultTextRenderer();

        for (int i = 0; i < count; i++)
        {
            IKey label = this.painter.getTabLabel(i);
            int w = UITextureTabElement.measureWidth(font, label);

            w = Math.max(TAB_MIN_WIDTH, Math.min(TAB_MAX_WIDTH, w));

            UITextureTabElement tabElement = this.tabs.get(i);

            tabElement.setTab(i, label, this.painter.getTabTooltip(i), this.painter.getTabIcon(i));
            tabElement.wh(w, TABS_HEIGHT_PX);
            this.scroll.add(tabElement);
        }

        this.scroll.add(this.add);
        this.scroll.resize();
        this.scroll.scroll.setScroll(scrollPos);
    }

    private void renderBackground(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A100);
    }
}
