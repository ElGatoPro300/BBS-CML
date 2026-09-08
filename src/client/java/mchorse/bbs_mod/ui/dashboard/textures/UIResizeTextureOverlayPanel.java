package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;

import org.joml.Vector2i;

import java.util.function.Consumer;

public class UIResizeTextureOverlayPanel extends UIMessageBarOverlayPanel
{
    public interface IResizeCallback
    {
        public void accept(int width, int height, boolean rescale, boolean center);
    }

    public UITrackpad width;
    public UITrackpad height;
    public UIToggle rescale;
    public UIToggle center;
    public UIButton apply;

    private final IResizeCallback callback;

    public UIResizeTextureOverlayPanel(int currentWidth, int currentHeight, Consumer<Vector2i> callback)
    {
        this(currentWidth, currentHeight, (w, h, rescale, center) ->
        {
            if (callback != null)
            {
                callback.accept(new Vector2i(w, h));
            }
        });
    }

    public UIResizeTextureOverlayPanel(int currentWidth, int currentHeight, IResizeCallback callback)
    {
        super(UIKeys.TEXTURE_PAINTER_RESIZE_TITLE, UIKeys.TEXTURES_RESIZE);

        this.callback = callback;

        this.width = new UITrackpad();
        this.width.integer().limit(1, 4096, true).setValue(Math.max(1, currentWidth));
        this.width.tooltip(UIKeys.TEXTURE_PAINTER_RESIZE_CANVAS);

        this.height = new UITrackpad();
        this.height.integer().limit(1, 4096, true).setValue(Math.max(1, currentHeight));
        this.height.tooltip(UIKeys.TEXTURE_PAINTER_RESIZE_CANVAS);

        this.rescale = new UIToggle(UIKeys.TEXTURE_PAINTER_RESCALE_IMAGE, (b) ->
        {
            this.center.setVisible(!this.rescale.getValue());
        });
        this.rescale.setValue(false);

        this.center = new UIToggle(UIKeys.TEXTURE_PAINTER_RESIZE_CENTER, (b) -> {});
        this.center.setValue(true);

        this.apply = new UIButton(UIKeys.GENERAL_OK, (b) -> this.confirm());
        this.apply.w(60);

        this.bar.remove(this.confirm);
        this.bar.add(this.width, this.height, this.rescale, this.center, this.apply);
    }

    @Override
    public void confirm()
    {
        if (this.callback != null)
        {
            this.callback.accept(
                (int) this.width.getValue(),
                (int) this.height.getValue(),
                this.rescale.getValue(),
                this.center.getValue()
            );
        }

        super.confirm();
    }
}