package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;

import java.util.function.Consumer;

public class UIColorOverlayPanel extends UIOverlayPanel
{
    public UIColorPicker picker;

    public UIColorOverlayPanel(IKey title, Consumer<Integer> callback)
    {
        super(title);

        this.picker = new UIColorPicker(callback);
        this.picker.relative(this.content).xy(0.5F, 0.5F).wh(160, 260).anchor(0.5F);
        
        this.content.add(this.picker);
    }
}
