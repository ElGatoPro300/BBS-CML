package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.presets.UIDataContextMenu;
import mchorse.bbs_mod.utils.pose.PoseManager;

public class UIModelSneakingSection extends UIModelSection
{
    public UIButton menu;

    public UIModelSneakingSection(IUIModelPanelHost editor)
    {
        super(editor);

        this.menu = new UIButton(UIKeys.MODELS_PICK_SNEAKING_POSE, (b) ->
            {
                if (this.config == null)
                {
                    return;
                }

                String group = this.config.poseGroup.get();

                if (group.isEmpty())
                {
                    group = this.config.getId();
                }

                UIDataContextMenu menu = new UIDataContextMenu(PoseManager.INSTANCE, group, () ->
                {
                    BaseType data = this.config.sneakingPose.toData();
                    return data.isMap() ? data.asMap() : new MapType();
                }, (data) ->
                {
                    this.config.sneakingPose.fromData(data);
                    this.editor.dirty();
                });

                menu.remove(menu.row);
                menu.entries.relative(menu).y(5).h(1F, -10);

                this.getContext().setContextMenu(menu);
            });

        this.fields.add(this.menu);
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_SNEAKING_TITLE;
    }
}
