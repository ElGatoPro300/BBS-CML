package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ValueFormEditorGizmoToolbar extends BaseValue implements IIconToolbarValue
{
    public static final String BODY_PART = "body_part";
    public static final String TRANSFORM = "transform";
    public static final String MOVE = "move";
    public static final String SCALE = "scale";
    public static final String ROTATE = "rotate";
    public static final String COMBINED = "combined";
    public static final String TOP = "top";
    public static final String SIZE = "size";
    public static final String TRANSLATE_SPEED = "translate_speed";

    public static final List<String> DEFAULT_ORDER = Collections.unmodifiableList(Arrays.asList(
        BODY_PART,
        TRANSFORM,
        MOVE,
        SCALE,
        ROTATE,
        COMBINED,
        TOP,
        SIZE,
        TRANSLATE_SPEED
    ));

    private final List<String> order = new ArrayList<>(DEFAULT_ORDER);
    private final Set<String> hidden = new HashSet<>();

    public ValueFormEditorGizmoToolbar(String id)
    {
        super(id);
    }

    @Override
    public List<String> getOrder()
    {
        return Collections.unmodifiableList(this.order);
    }

    public Set<String> getHidden()
    {
        return Collections.unmodifiableSet(this.hidden);
    }

    @Override
    public List<String> getVisibleOrder()
    {
        List<String> visible = new ArrayList<>();

        for (String id : this.order)
        {
            if (!this.hidden.contains(id))
            {
                visible.add(id);
            }
        }

        return visible;
    }

    @Override
    public boolean isHidden(String id)
    {
        return this.hidden.contains(id);
    }

    @Override
    public boolean canHide(String id)
    {
        return DEFAULT_ORDER.contains(id);
    }

    @Override
    public void moveButton(int from, int to)
    {
        if (from == to || from < 0 || to < 0 || from >= this.order.size() || to >= this.order.size())
        {
            return;
        }

        BaseValue.edit(this, (v) ->
        {
            String buttonId = this.order.remove(from);

            this.order.add(to, buttonId);
        });
    }

    @Override
    public void toggleHidden(String id)
    {
        if (!this.canHide(id))
        {
            return;
        }

        BaseValue.edit(this, (v) ->
        {
            if (this.hidden.contains(id))
            {
                this.hidden.remove(id);
            }
            else
            {
                this.hidden.add(id);
            }
        });
    }

    public void resetToDefaults()
    {
        BaseValue.edit(this, (v) ->
        {
            this.order.clear();
            this.order.addAll(DEFAULT_ORDER);
            this.hidden.clear();
        });
    }

    public boolean isResettable()
    {
        return true;
    }

    @Override
    public void resetToDefault()
    {
        this.resetToDefaults();
    }

    private void normalizeOrder()
    {
        this.order.removeIf((id) -> !DEFAULT_ORDER.contains(id));

        for (String id : DEFAULT_ORDER)
        {
            if (!this.order.contains(id))
            {
                this.order.add(id);
            }
        }
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();
        ListType orderList = new ListType();

        for (String id : this.order)
        {
            orderList.addString(id);
        }

        ListType hiddenList = new ListType();

        for (String id : this.hidden)
        {
            hiddenList.addString(id);
        }

        data.put("order", orderList);
        data.put("hidden", hiddenList);

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.order.clear();
        this.hidden.clear();

        if (!data.isMap())
        {
            this.order.addAll(DEFAULT_ORDER);

            return;
        }

        MapType map = data.asMap();

        if (map.has("order") && map.get("order").isList())
        {
            for (BaseType entry : map.get("order").asList())
            {
                if (entry.isString())
                {
                    this.order.add(entry.asString());
                }
            }
        }

        if (map.has("hidden") && map.get("hidden").isList())
        {
            for (BaseType entry : map.get("hidden").asList())
            {
                if (entry.isString())
                {
                    this.hidden.add(entry.asString());
                }
            }
        }

        this.normalizeOrder();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ValueFormEditorGizmoToolbar)
        {
            ValueFormEditorGizmoToolbar other = (ValueFormEditorGizmoToolbar) obj;

            return this.order.equals(other.order) && this.hidden.equals(other.hidden);
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.order, this.hidden);
    }
}
