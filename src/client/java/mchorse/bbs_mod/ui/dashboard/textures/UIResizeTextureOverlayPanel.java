package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import org.joml.Vector2i;

import java.util.function.Consumer;

public class UIResizeTextureOverlayPanel extends UIMessageBarOverlayPanel
{
    public UITrackpad x;
    public UITrackpad y;
    public UIButton resize;

    private final Consumer<Vector2i> callback;

    public UIResizeTextureOverlayPanel(int w, int h, Consumer<Vector2i> size)
    {
        super(UIKeys.TEXTURES_RESIZE_TITLE, UIKeys.TEXTURES_RESIZE_DESCRIPTION);

        this.callback = size;

        this.x = new UITrackpad();
        this.x.limit(1, 4096, true).setValue(w);
        this.x.tooltip(UIKeys.SNOWSTORM_APPEARANCE_WIDTH);
        this.y = new UITrackpad();
        this.y.limit(1, 4096, true).setValue(h);
        this.y.tooltip(UIKeys.SNOWSTORM_APPEARANCE_HEIGHT);
        this.resize = new UIButton(UIKeys.TEXTURES_RESIZE, (b) -> this.confirm());
        this.resize.w(100);

        this.bar.remove(this.confirm);
        this.bar.add(this.x, this.y, this.resize);
    }

    @Override
    public void confirm()
    {
        if (this.callback != null)
        {
            this.callback.accept(new Vector2i((int) this.x.getValue(), (int) this.y.getValue()));
        }

        super.confirm();
    }
}