package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBezierHandles
{
    private UITrackpad lx;
    private UITrackpad ly;
    private UITrackpad rx;
    private UITrackpad ry;

    private Keyframe<?> keyframe;
    private int index = -1;

    public UIBezierHandles(Keyframe<?> keyframe)
    {
        this(keyframe, -1);
    }

    public UIBezierHandles(Keyframe<?> keyframe, int index)
    {
        this.keyframe = keyframe;
        this.index = index;

        this.lx = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.setLx(this.index, (float) TimeUtils.fromTime(v.floatValue()))));
        this.ly = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.setLy(this.index, v.floatValue())));
        this.rx = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.setRx(this.index, (float) TimeUtils.fromTime(v.floatValue()))));
        this.ry = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.setRy(this.index, v.floatValue())));
        
        this.update();
    }

    public UIElement createColumn()
    {
        UIIcon leftIcon = new UIIcon(Icons.LEFT_HANDLE, null);
        leftIcon.tooltip(UIKeys.KEYFRAMES_LEFT_HANDLE);
        leftIcon.wh(UIConstants.CONTROL_HEIGHT, UIConstants.CONTROL_HEIGHT);
        UIIcon rightIcon = new UIIcon(Icons.RIGHT_HANDLE, null);
        rightIcon.tooltip(UIKeys.KEYFRAMES_RIGHT_HANDLE);
        rightIcon.wh(UIConstants.CONTROL_HEIGHT, UIConstants.CONTROL_HEIGHT);
        int rowMargin = 4;
        return UI.column(UIConstants.MARGIN,
            UI.row(rowMargin, 0, UIConstants.CONTROL_HEIGHT, leftIcon, this.lx, this.ly),
            UI.row(rowMargin, 0, UIConstants.CONTROL_HEIGHT, rightIcon, this.rx, this.ry)
        );
    }

    public void update()
    {
        this.lx.setValue(TimeUtils.toTime(this.keyframe.getLx(this.index)));
        this.ly.setValue(this.keyframe.getLy(this.index));
        this.rx.setValue(TimeUtils.toTime(this.keyframe.getRx(this.index)));
        this.ry.setValue(this.keyframe.getRy(this.index));
    }
}
