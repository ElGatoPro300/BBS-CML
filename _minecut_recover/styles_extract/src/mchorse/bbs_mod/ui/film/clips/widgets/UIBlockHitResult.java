package mchorse.bbs_mod.ui.film.clips.widgets;

import mchorse.bbs_mod.actions.values.ValueBlockHitResult;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class UIBlockHitResult
{
    public UITrackpad x;
    public UITrackpad y;
    public UITrackpad z;
    public UITrackpad hitX;
    public UITrackpad hitY;
    public UITrackpad hitZ;
    public UICirculate direction;
    public UIToggle inside;

    private IUIClipsDelegate editor;
    private ValueBlockHitResult result;

    public UIBlockHitResult(IUIClipsDelegate editor)
    {
        this.editor = editor;

        this.x = new UITrackpad((v) -> this.editor.editMultiple(this.result.x, (x) -> x.set(v.intValue())));
        this.x.integer();
        this.y = new UITrackpad((v) -> this.editor.editMultiple(this.result.y, (y) -> y.set(v.intValue())));
        this.y.integer();
        this.z = new UITrackpad((v) -> this.editor.editMultiple(this.result.z, (z) -> z.set(v.intValue())));
        this.z.integer();
        this.hitX = new UITrackpad((v) -> this.editor.editMultiple(this.result.hitX, (hitX) -> hitX.set(v)));
        this.hitY = new UITrackpad((v) -> this.editor.editMultiple(this.result.hitY, (hitY) -> hitY.set(v)));
        this.hitZ = new UITrackpad((v) -> this.editor.editMultiple(this.result.hitZ, (hitZ) -> hitZ.set(v)));
        this.direction = new UICirculate((b) -> this.result.direction.set(b.getValue()));
        this.direction.addLabel(UIKeys.ACTIONS_BLOCK_DIRECTION_DOWN);
        this.direction.addLabel(UIKeys.ACTIONS_BLOCK_DIRECTION_UP);
        this.direction.addLabel(UIKeys.ACTIONS_BLOCK_DIRECTION_NORTH);
        this.direction.addLabel(UIKeys.ACTIONS_BLOCK_DIRECTION_SOUTH);
        this.direction.addLabel(UIKeys.ACTIONS_BLOCK_DIRECTION_WEST);
        this.direction.addLabel(UIKeys.ACTIONS_BLOCK_DIRECTION_EAST);
        this.inside = new UIToggle(UIKeys.ACTIONS_BLOCK_INSIDE, (b) -> this.result.inside.set(b.getValue()));
    }

    public void fill(ValueBlockHitResult result)
    {
        this.result = result;

        this.x.setValue(this.result.x.get());
        this.y.setValue(this.result.y.get());
        this.z.setValue(this.result.z.get());
        this.hitX.setValue(this.result.hitX.get());
        this.hitY.setValue(this.result.hitY.get());
        this.hitZ.setValue(this.result.hitZ.get());
        this.direction.setValue(this.result.direction.get());
        this.inside.setValue(this.result.inside.get());
    }
}