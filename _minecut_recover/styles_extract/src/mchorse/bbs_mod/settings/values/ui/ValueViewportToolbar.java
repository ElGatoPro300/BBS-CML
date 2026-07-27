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

public class ValueViewportToolbar extends BaseValue implements IIconToolbarValue
{
    public static final String HIDE_OVERLAYS = "hide_overlays";
    public static final String ONION_SKIN = "onion_skin";
    public static final String TOGGLE_SHADERS = "toggle_shaders";
    public static final String PLAYBACK = "playback";
    public static final String TELEPORT = "teleport";
    public static final String FLIGHT = "flight";
    public static final String CONTROL = "control";
    public static final String PERSPECTIVE = "perspective";
    public static final String RECORD_REPLAY = "record_replay";
    public static final String RECORD_VIDEO = "record_video";
    public static final String RENDER_QUEUE = "render_queue";
    public static final String RESTORE_BLOCKS = "restore_blocks";

    public static final List<String> DEFAULT_ORDER = Collections.unmodifiableList(Arrays.asList(
        HIDE_OVERLAYS,
        ONION_SKIN,
        TOGGLE_SHADERS,
        PLAYBACK,
        RESTORE_BLOCKS,
        TELEPORT,
        FLIGHT,
        CONTROL,
        PERSPECTIVE,
        RECORD_REPLAY,
        RECORD_VIDEO,
        RENDER_QUEUE
    ));

    private final List<String> order = new ArrayList<>(DEFAULT_ORDER);
    private final Set<String> hidden = new HashSet<>();

    public ValueViewportToolbar(String id)
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
        return !HIDE_OVERLAYS.equals(id);
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
            String id = this.order.remove(from);

            this.order.add(to, id);
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
        if (obj instanceof ValueViewportToolbar)
        {
            ValueViewportToolbar other = (ValueViewportToolbar) obj;

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
