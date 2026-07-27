package mchorse.bbs_mod.ui.utils.shapes;

import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.presets.UIDataContextMenu;
import mchorse.bbs_mod.utils.pose.ShapeKeysManager;

import java.util.Set;

public class UIShapeKeys extends UIElement
{
    public UIStringList list;
    public UITrackpad value;

    private String group = "";
    private ShapeKeys shapeKeys;

    public UIShapeKeys()
    {
        this.list = new UIStringList((l) -> this.pick(l.get(0), false));
        this.list.background().h(this.list.scroll.scrollItemSize * 6);
        this.list.cancelScrollEdge();
        this.list.context(() -> new UIDataContextMenu(ShapeKeysManager.INSTANCE, group, () -> this.shapeKeys.toData(), (data) ->
        {
            String current = this.list.getCurrentFirst();

            this.changedShapeKeys(() -> this.shapeKeys.fromData(data));
            this.pick(current, true);
        }).tooltips("_CopyShapeKeys",
            UIKeys.SHAPE_KEYS_CONTEXT_COPY,
            UIKeys.SHAPE_KEYS_CONTEXT_PASTE,
            UIKeys.SHAPE_KEYS_CONTEXT_RESET,
            UIKeys.SHAPE_KEYS_CONTEXT_SAVE,
            UIKeys.SHAPE_KEYS_CONTEXT_NAME
        ));
        this.value = new UITrackpad((v) -> this.setValue(v.floatValue()));

        this.column().vertical().stretch();

        this.add(UI.label(UIKeys.SHAPE_KEYS_TITLE), this.list, this.value);
    }

    public void setShapeKeys(String group, Set<String> keys, ShapeKeys shapeKeys)
    {
        this.group = group;
        this.shapeKeys = shapeKeys;

        this.list.add(keys);
        this.list.sort();
        this.pick(this.list.getList().get(0), true);
    }

    protected void changedShapeKeys(Runnable runnable)
    {
        if (runnable != null)
        {
            runnable.run();
        }
    }

    protected void setValue(float v)
    {
        this.shapeKeys.shapeKeys.put(this.list.getCurrentFirst(), v);
    }

    private void pick(String key, boolean select)
    {
        this.value.setValue(this.shapeKeys.shapeKeys.computeIfAbsent(key, (k) -> 0F));

        if (select)
        {
            this.list.setCurrentScroll(key);
        }
    }
}