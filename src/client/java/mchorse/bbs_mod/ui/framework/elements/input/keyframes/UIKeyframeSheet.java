package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.List;

public class UIKeyframeSheet
{
    /* Meta data */
    public final String id;
    public IKey title;
    public final int color;
    public boolean separator;

    private Icon icon;

    public final KeyframeChannel channel;
    public final KeyframeSelection selection;
    public final BaseValueBasic property;
    public Object defaultInsertValue;

    /** Optional numeric bounds for double/float/int keyframe values (inclusive). */
    public Double minValue;
    public Double maxValue;

    public boolean groupHeader;
    public boolean groupExpanded = true;
    public boolean expanded = false;
    public int level = 0;
    public String groupKey;
    public Runnable toggleGroup;
    public Runnable toggleExpanded;

    /** Optional boolean sub-track rendered inside the same timeline row (e.g. repeat center). */
    public UIKeyframeSheet companion;

    public UIKeyframeSheet(int color, boolean separator, KeyframeChannel channel, BaseValueBasic property)
    {
        this(channel.getId(), IKey.constant(property != null ? FormUtils.getForm(property).getTrackName(channel.getId()) : channel.getId()), color, separator, channel, property);
    }

    public UIKeyframeSheet(String id, IKey title, int color, boolean separator, KeyframeChannel channel, BaseValueBasic property)
    {
        this.id = id;
        this.title = title;
        this.color = color;
        this.separator = separator;

        this.channel = channel;
        this.selection = new KeyframeSelection(channel);
        this.property = property;

        this.groupHeader = false;
        this.groupExpanded = true;
        this.groupKey = null;
        this.toggleGroup = null;
    }

    public static UIKeyframeSheet groupHeader(String id, IKey title, int color, String groupKey, boolean expanded, Runnable toggleGroup)
    {
        UIKeyframeSheet sheet = new UIKeyframeSheet(id, title, color, false, new KeyframeChannel<>(id, KeyframeFactories.DOUBLE), null);

        sheet.groupHeader = true;
        sheet.groupExpanded = expanded;
        sheet.groupKey = groupKey;
        sheet.toggleGroup = toggleGroup;

        return sheet;
    }

    public UIKeyframeSheet icon(Icon icon)
    {
        this.icon = icon;

        return this;
    }

    public UIKeyframeSheet limit(Double min, Double max)
    {
        this.minValue = min;
        this.maxValue = max;

        return this;
    }

    public Icon getIcon()
    {
        return this.icon;
    }

    public Object clampValue(Object value)
    {
        if (!(value instanceof Number) || (this.minValue == null && this.maxValue == null))
        {
            return value;
        }

        double v = ((Number) value).doubleValue();

        if (this.minValue != null)
        {
            v = Math.max(this.minValue, v);
        }

        if (this.maxValue != null)
        {
            v = Math.min(this.maxValue, v);
        }

        if (value instanceof Double)
        {
            return v;
        }

        if (value instanceof Float)
        {
            return (float) v;
        }

        if (value instanceof Integer)
        {
            return (int) Math.round(v);
        }

        return v;
    }

    public List<Integer> sort()
    {
        List<Keyframe> selected = this.selection.getSelected();
        List<Integer> lastSelection = new ArrayList<>(this.selection.getIndices());

        this.channel.sort();
        this.selection.clear();

        List keyframes = this.channel.getKeyframes();

        for (Keyframe keyframe : selected)
        {
            this.selection.add(keyframes.indexOf(keyframe));
        }

        return lastSelection;
    }

    public void setTickBy(float diff, boolean dirty)
    {
        for (Keyframe keyframe : this.selection.getSelected())
        {
            keyframe.setTick(keyframe.getTick() + diff, dirty);
        }
    }

    public void setDuration(float duration)
    {
        for (Keyframe keyframe : this.selection.getSelected())
        {
            keyframe.setDuration(duration);
        }
    }

    public void setValue(Object value, Object selectedValue, boolean dirty)
    {
        Number valueNumber = value instanceof Number ? (Number) value : 0D;

        for (Keyframe keyframe : this.selection.getSelected())
        {
            if (selectedValue instanceof Double)
            {
                keyframe.setValue(this.clampValue((double) keyframe.getValue() + valueNumber.doubleValue() - (double) selectedValue), dirty);
            }
            else if (selectedValue instanceof Float)
            {
                keyframe.setValue(this.clampValue((float) keyframe.getValue() + valueNumber.floatValue() - (float) selectedValue), dirty);
            }
            else if (selectedValue instanceof Integer)
            {
                keyframe.setValue(this.clampValue((int) keyframe.getValue() + valueNumber.intValue() - (int) selectedValue), dirty);
            }
            else
            {
                keyframe.setValue(this.channel.getFactory().copy(this.clampValue(value)), dirty);
            }
        }
    }

    public void setInterpolation(Interpolation interpolation)
    {
        for (Keyframe keyframe : this.selection.getSelected())
        {
            keyframe.getInterpolation().copy(interpolation);
        }
    }

    public void remove(Keyframe keyframe)
    {
        int index = this.channel.getKeyframes().indexOf(keyframe);

        if (index >= 0)
        {
            this.selection.remove(index);
            this.channel.remove(index);
        }
    }
}
