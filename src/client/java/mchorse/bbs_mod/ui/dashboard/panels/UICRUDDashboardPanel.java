package mchorse.bbs_mod.ui.dashboard.panels;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UICRUDOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIOpenAssetOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.List;

public abstract class UICRUDDashboardPanel extends UISidebarDashboardPanel
{
    public UIIcon openOverlay;

    public final UICRUDOverlayPanel overlay;

    public UICRUDDashboardPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.overlay = this.createOverlayPanel();
        this.openOverlay = new UIIcon(Icons.MORE, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), this.overlay, 260, 0.9F);
        });

        this.iconBar.prepend(this.openOverlay);

        this.keys().register(Keys.OPEN_DATA_MANAGER, () ->
        {
            if (this.getContext() != null)
            {
                List<UIOpenAssetOverlayPanel> overlays = this.getContext().menu.overlay.getChildren(UIOpenAssetOverlayPanel.class);

                if (!overlays.isEmpty())
                {
                    for (UIOpenAssetOverlayPanel overlay : overlays)
                    {
                        overlay.close();
                    }
                }
                else
                {
                    UIOverlay.addOverlay(this.getContext(), new UIOpenAssetOverlayPanel(UIKeys.RAW_OPEN_ASSET, this.dashboard), 520, 320);
                }
            }
        });
    }

    protected abstract UICRUDOverlayPanel createOverlayPanel();

    protected abstract IKey getTitle();

    public abstract void pickData(String id);
}
