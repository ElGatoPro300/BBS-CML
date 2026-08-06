package mchorse.bbs_mod.ui.utils.presets;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;

public class UIPresetsOverlayPanel extends UIListOverlayPanel
{
    public UIPresetsOverlayPanel(UICopyPasteController controller, int mouseX, int mouseY)
    {
        super(UIKeys.PRESETS_TITLE, null);

        this.callback = (l) ->
        {
            MapType load = controller.manager.load(l.get(0));

            if (load != null)
            {
                controller.getConsumer().paste(load, mouseX, mouseY);
                this.close();
            }
        };

        this.addValues(controller.manager.getKeys());

        UIIcon save = new UIIcon(Icons.SAVED, (b) ->
        {
            MapType type = controller.getSupplier().get();

            if (type != null)
            {
                UIPromptOverlayPanel pane = new UIPromptOverlayPanel(UIKeys.PRESETS_SAVE_TITLE, UIKeys.PRESETS_SAVE_DESCRIPTION, (t) ->
                {
                    controller.manager.save(t, type);
                    this.list.list.clear();
                    this.addValues(controller.manager.getKeys());
                });

                pane.text.filename();
                UIOverlay.addOverlay(this.getContext(), pane);
            }
        });

        save.setEnabled(controller.canCopy());

        UIIcon folder = new UIIcon(Icons.FOLDER, (b) ->
        {
            UIUtils.openFolder(controller.manager.getFolder());
        });

        save.tooltip(UIKeys.PRESETS_SAVE, Direction.LEFT);
        folder.tooltip(UIKeys.PRESETS_OPEN, Direction.LEFT);
        this.icons.add(save, folder);
    }
}