package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_minecut_ui.styles.MinecutTokens;
import mchorse.bbs_mod.ui.framework.styles.UIStyle;

/**
 * Player chrome around {@link UIFilmPreview}. Viewport tools sit in the bottom
 * strip under the preview. Classic dock supplies the card title bar.
 */
public class UIMinecutPlayerPanel extends UIMinecutRegion
{
    public static final int TRANSPORT_H = 36;

    private final UIFilmPanel film;
    public final UIElement viewportHost;
    public final UIElement toolsHost;

    public UIMinecutPlayerPanel(UIFilmPanel film)
    {
        super("Player");
        this.noHeader();
        this.film = film;

        this.viewportHost = new UIElement();
        this.viewportHost.relative(this).x(1).y(0).w(1F, -2).h(1F, -TRANSPORT_H);
        this.add(this.viewportHost);

        this.toolsHost = new UIElement();
        this.toolsHost.relative(this).x(0).y(1F, -TRANSPORT_H).w(1F).h(TRANSPORT_H);
        this.add(this.toolsHost);
    }

    public void mountPreview(UIFilmPreview preview)
    {
        preview.removeFromParent();
        preview.resetFlex().relative(this.viewportHost).x(0).y(0).w(1F).h(1F);
        this.viewportHost.removeAll();
        this.viewportHost.add(preview);
        preview.setVisible(true);
        preview.attachIconsTo(this.toolsHost);
    }

    @Override
    public void render(UIContext context)
    {
        UIStyle style = UIStyle.active();

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), style.panel());
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), style.borderSoft());

        if (this.viewportHost.area.w > 0 && this.viewportHost.area.h > 0)
        {
            context.batcher.clip(this.viewportHost.area, context);

            if (this.viewportHost.isVisible())
            {
                this.viewportHost.render(context);
            }

            context.batcher.unclip(context);
        }

        int ty = this.area.ey() - TRANSPORT_H;

        context.batcher.box(this.area.x, ty, this.area.ex(), this.area.ey(), style.elevated());
        context.batcher.box(this.area.x, ty, this.area.ex(), ty + 1, style.borderSoft());

        int cursor = this.film.getCursor();
        int duration = this.film.getData() != null ? this.film.getData().camera.calculateDuration() : 0;
        String time = TimeUtils.formatTime(cursor) + " / " + TimeUtils.formatTime(duration);

        context.batcher.textShadow(time, this.area.x + 8, ty + 12, MinecutTokens.TEXT_DIM);

        if (this.toolsHost.isVisible())
        {
            this.toolsHost.render(context);
        }
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.toolsHost.area.isInside(context) && this.toolsHost.isVisible())
        {
            IUIElement hit = this.toolsHost.mouseClicked(context);

            if (hit != null)
            {
                return true;
            }
        }

        if (this.viewportHost.area.isInside(context) && this.viewportHost.isVisible())
        {
            /* Alt+LMB morph pick must work even if a child swallows or misses the letterbox. */
            if (Window.isAltPressed() && context.mouseButton == 0
                && this.film.getController().tryPickHoveredReplay(context))
            {
                return true;
            }

            IUIElement hit = this.viewportHost.mouseClicked(context);

            return hit != null || (Window.isAltPressed() && context.mouseButton == 0);
        }

        return false;
    }
}
