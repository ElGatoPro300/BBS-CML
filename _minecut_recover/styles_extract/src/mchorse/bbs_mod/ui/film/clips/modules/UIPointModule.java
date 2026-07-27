package mchorse.bbs_mod.ui.film.clips.modules;

import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.utils.UICameraUtils;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIPointModule extends UIAbstractModule
{
    public UITrackpad x;
    public UITrackpad y;
    public UITrackpad z;

    public ValuePoint point;

    public UIPointModule(IUIClipsDelegate editor)
    {
        this(editor, UIKeys.CAMERA_PANELS_POSITION);
    }

    public UIPointModule(IUIClipsDelegate editor, IKey title)
    {
        super(editor);

        this.x = new UITrackpad((value) -> BaseValue.edit(this.point, (point) -> point.get().x = value));
        this.x.tooltip(UIKeys.GENERAL_X);

        this.y = new UITrackpad((value) -> BaseValue.edit(this.point, (point) -> point.get().y = value));
        this.y.tooltip(UIKeys.GENERAL_Y);

        this.z = new UITrackpad((value) -> BaseValue.edit(this.point, (point) -> point.get().z = value));
        this.z.tooltip(UIKeys.GENERAL_Z);

        this.x.values(0.1F);
        this.y.values(0.1F);
        this.z.values(0.1F);

        this.column().vertical().stretch().height(20);
        this.add(UIClip.label(title), this.x, this.y, this.z);
    }

    public UIPointModule contextMenu()
    {
        this.context((menu) -> UICameraUtils.pointContextMenu(menu, this.editor, this.point));

        return this;
    }

    public void fill(ValuePoint point)
    {
        this.point = point;

        this.x.setValue((float) point.get().x);
        this.y.setValue((float) point.get().y);
        this.z.setValue((float) point.get().z);
    }
}