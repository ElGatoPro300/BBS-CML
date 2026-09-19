package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;

import java.util.function.BiConsumer;

public class UIAdjustColorsOverlayPanel extends UIMessageBarOverlayPanel
{
    public UITrackpad brightness;
    public UITrackpad contrast;
    public UIButton apply;

    private final BiConsumer<Float, Float> callback;

    public UIAdjustColorsOverlayPanel(BiConsumer<Float, Float> callback)
    {
        super(UIKeys.TEXTURE_PAINTER_ADJUST_COLORS, UIKeys.TEXTURE_PAINTER_OPS_IMAGE);

        this.callback = callback;

        this.brightness = new UITrackpad();
        this.brightness.integer().limit(-100, 100, true).setValue(0);
        this.brightness.tooltip(UIKeys.TEXTURE_PAINTER_BRIGHTNESS);

        this.contrast = new UITrackpad();
        this.contrast.integer().limit(-100, 100, true).setValue(0);
        this.contrast.tooltip(UIKeys.TEXTURE_PAINTER_CONTRAST);

        this.apply = new UIButton(UIKeys.GENERAL_OK, (b) -> this.confirm());
        this.apply.w(60);

        this.bar.remove(this.confirm);
        this.bar.add(this.brightness, this.contrast, this.apply);
    }

    @Override
    public void confirm()
    {
        if (this.callback != null)
        {
            this.callback.accept(
                (float) this.brightness.getValue() / 100F,
                (float) this.contrast.getValue() / 100F
            );
        }

        super.confirm();
    }
}
