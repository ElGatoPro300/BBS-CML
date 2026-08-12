package mchorse.bbs_mod.ui.film.clips.modules;

import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.values.ValueAngle;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.utils.UICameraUtils;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIAngleModule extends UIAbstractModule
{
    public UITrackpad yaw;
    public UITrackpad pitch;
    public UITrackpad roll;
    public UITrackpad fov;
    public UITrackpad distance;

    public ValueAngle angle;

    public UIAngleModule(IUIClipsDelegate editor)
    {
        this(editor, false);
    }

    public UIAngleModule(IUIClipsDelegate editor, boolean includeDistance)
    {
        super(editor);

        this.yaw = new UITrackpad((v) -> BaseValue.edit(this.angle, (value) -> value.get().yaw = v.floatValue()));
        this.yaw.tooltip(UIKeys.CAMERA_PANELS_YAW);

        this.pitch = new UITrackpad((v) -> BaseValue.edit(this.angle, (value) -> value.get().pitch = v.floatValue()));
        this.pitch.tooltip(UIKeys.CAMERA_PANELS_PITCH);

        this.roll = new UITrackpad((v) -> BaseValue.edit(this.angle, (value) -> value.get().roll = v.floatValue()));
        this.roll.tooltip(UIKeys.CAMERA_PANELS_ROLL);

        this.fov = new UITrackpad((v) -> BaseValue.edit(this.angle, (value) -> value.get().fov = v.floatValue()));
        this.fov.tooltip(UIKeys.CAMERA_PANELS_FOV);
        
        if (includeDistance)
        {
            this.distance = new UITrackpad((v) -> BaseValue.edit(this.angle, (value) -> value.get().distance = v.floatValue()));
            this.distance.tooltip(UIKeys.CAMERA_PANELS_DISTANCE);
        }

        this.column().vertical().stretch().height(20);
        if (includeDistance)
        {
            this.add(UIClip.label(UIKeys.CAMERA_PANELS_ANGLE), this.yaw, this.pitch, this.roll, this.fov, this.distance);
        }
        else
        {
            this.add(UIClip.label(UIKeys.CAMERA_PANELS_ANGLE), this.yaw, this.pitch, this.roll, this.fov);
        }
    }

    public UIAngleModule contextMenu()
    {
        this.context((menu) -> UICameraUtils.angleContextMenu(menu, this.editor, this.angle));

        return this;
    }

    public void fill(ValueAngle angle)
    {
        this.angle = angle;

        this.yaw.setValue(angle.get().yaw);
        this.pitch.setValue(angle.get().pitch);
        this.roll.setValue(angle.get().roll);
        this.fov.setValue(angle.get().fov);
        if (this.distance != null)
        {
            this.distance.setValue(angle.get().distance);
        }
    }
}
