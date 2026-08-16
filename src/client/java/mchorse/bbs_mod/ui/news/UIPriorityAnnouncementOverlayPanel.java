package mchorse.bbs_mod.ui.news;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

public class UIPriorityAnnouncementOverlayPanel extends UIOverlayPanel
{
    public UIPriorityAnnouncementOverlayPanel(UIDashboard dashboard, UINewsPanel.PriorityAnnouncement announcement)
    {
        super(UIKeys.PRIORITY_ANNOUNCEMENT_TITLE);

        this.content.removeAll();

        boolean showOpenNews = announcement.shouldShowOpenNewsButton();
        boolean showAction = announcement.shouldShowActionButton();
        boolean hasButtons = showOpenNews || showAction;
        int bottomOffset = hasButtons ? -30 : -6;

        UIScrollView scroll = UI.scrollView(6, 8);
        scroll.relative(this.content).xy(0, 0).w(1F).h(1F, bottomOffset);

        UILabel title = new UILabel(IKey.raw(announcement.title == null ? "" : announcement.title));
        title.color(Colors.WHITE);
        title.h(16);

        UIText body = new UIText(IKey.raw(announcement.body == null ? "" : announcement.body));
        body.color(Colors.LIGHTER_GRAY, true).padding(0, 2).lineHeight(12);

        UIElement column = UI.column(6, title, body).marginTop(6);
        scroll.add(column);

        if (announcement.image != null && !announcement.image.isEmpty())
        {
            scroll.add(new UINewsPanel.UINewsImage(Link.create(announcement.image), 240));
        }

        if (hasButtons)
        {
            List<UIElement> buttons = new ArrayList<>();

            if (showOpenNews)
            {
                UIButton openNews = new UIButton(UIKeys.PRIORITY_ANNOUNCEMENT_OPEN_NEWS, (b) ->
                {
                    dashboard.setPanel(dashboard.getPanel(UINewsPanel.class));
                    this.close();
                });

                buttons.add(openNews);
            }

            if (showAction)
            {
                UIButton action = new UIButton(IKey.raw(announcement.getActionLabel()), (b) ->
                {
                    if (announcement.actionUrl != null && !announcement.actionUrl.isEmpty())
                    {
                        UIUtils.openWebLink(announcement.actionUrl);
                    }

                    this.close();
                });

                buttons.add(action);
            }

            float buttonWidth = buttons.size() == 1 ? 1F : 0.5F;
            int buttonOffset = buttons.size() == 1 ? 0 : -4;

            for (UIElement button : buttons)
            {
                button.w(buttonWidth, buttonOffset);
            }

            UIElement bar = UI.row(8, buttons.toArray(new UIElement[0]));
            bar.relative(this.content).x(6).y(1F, -6).w(1F, -12).anchorY(1F);

            this.content.add(scroll, bar);
        }
        else
        {
            this.content.add(scroll);
        }
    }
}
