package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import org.joml.Vector3d;

import java.util.function.Consumer;

public class UIFilmMoveOverlayPanel extends UIMessageOverlayPanel
{
    private Consumer<Vector3d> callbackVector;
    private UIElement secondBar;
    private UIButton confirm;
    private UITrackpad x;
    private UITrackpad y;
    private UITrackpad z;

    public UIFilmMoveOverlayPanel(Consumer<Vector3d> callbackVector)
    {
        super(UIKeys.FILM_MOVE_TITLE, UIKeys.FILM_MOVE_DESCRIPTION);

        this.callbackVector = callbackVector;

        this.confirm = new UIButton(UIKeys.GENERAL_OK, (b) ->
        {
            this.close();

            if (this.callbackVector != null)
            {
                this.callbackVector.accept(new Vector3d(
                    this.x.getValue(),
                    this.y.getValue(),
                    this.z.getValue()
                ));
            }
        });
        this.x = new UITrackpad();
        this.y = new UITrackpad();
        this.z = new UITrackpad();

        this.secondBar = UI.column(UI.row(this.x, this.y, this.z), this.confirm);
        this.secondBar.relative(this.content).x(6).y(1F, -6).w(1F, -12).anchor(0, 1F);

        this.content.add(this.secondBar);
    }
}