package mchorse.bbs_mod.ui.utils.presets;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIPresetContextMenu extends UISimpleContextMenu
{
    public UIElement row;
    public UIIcon copy;
    public UIIcon paste;

    private final UICopyPasteController controller;
    private final int mouseX;
    private final int mouseY;

    public UIPresetContextMenu(UICopyPasteController controller)
    {
        this(controller, 0, 0);
    }

    public UIPresetContextMenu(UICopyPasteController controller, int mouseX, int mouseY)
    {
        super();

        this.controller = controller;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        this.copy = new UIIcon(Icons.COPY, (b) -> this.copy());
        this.copy.setEnabled(controller.canCopy());
        this.paste = new UIIcon(Icons.PASTE, (b) -> this.paste());
        this.paste.setEnabled(controller.canPaste());

        UIIcon presets = new UIIcon(Icons.MORE, (b) -> this.openPresets(this.mouseX, this.mouseY));

        presets.setEnabled(controller.canPreviewPresets());
        presets.tooltip(UIKeys.GENERAL_PRESETS);

        this.row = UI.row(0, this.copy, this.paste, presets);
        this.row.relative(this).row().resize();
        this.actions.y(21).h(1F, -21);

        this.add(this.row);
    }

    public UIPresetContextMenu labels(IKey copy, IKey paste)
    {
        this.copy.tooltip(copy);
        this.paste.tooltip(paste);

        return this;
    }

    private void copy()
    {
        this.controller.copy();
        this.removeFromParent();
    }

    private void paste()
    {
        this.controller.paste(this.mouseX, this.mouseY);
        this.removeFromParent();
    }

    private void openPresets(int mouseX, int mouseY)
    {
        this.controller.openPresets(this.getContext(), mouseX, mouseY);
        this.removeFromParent();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        super.setMouse(context);

        this.w(Math.max(60, this.getFlex().w.offset));
        this.h(this.getFlex().h.offset + 21);
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        context.batcher.box(this.area.x + 2, this.area.y + 20, this.area.ex() - 2, this.area.y + 21, Colors.mulRGB(Colors.WHITE, 0.1F));
    }
}