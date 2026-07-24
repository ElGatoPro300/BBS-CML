package mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;

public interface IUIKeyframeGraph
{
    public static final int TOP_MARGIN = 15;
    public static final int SIDEBAR_WIDTH = 140;

    public default UIKeyframes getHostKeyframes()
    {
        return null;
    }

    public default int getSidebarWidth()
    {
        return SIDEBAR_WIDTH;
    }

    public void resetView();

    public UIKeyframeSheet getLastSheet();

    public List<UIKeyframeSheet> getSheets();

    /* Selection */

    public default void clearSelection()
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.selection.clear();
        }

        this.pickKeyframe(null);
    }

    public default void selectAll()
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.selection.all();
        }

        this.pickSelected();
    }

    public default void selectAfter(float tick, int direction)
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.selection.after(tick, direction);
        }

        this.pickSelected();
    }

    public void selectByX(int mouseX);

    public void selectInArea(Area area);

    public default Keyframe getSelected()
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            Keyframe first = sheet.selection.getFirst();

            if (first != null)
            {
                return first;
            }
        }

        return null;
    }

    /* Keyframe management */

    public default UIKeyframeSheet getSheet(Keyframe keyframe)
    {
        if (keyframe == null)
        {
            return null;
        }

        KeyframeChannel channel = (KeyframeChannel) keyframe.getParent();

        for (UIKeyframeSheet sheet : this.getSheets())
        {
            if (sheet.channel == channel)
            {
                return sheet;
            }
        }

        return null;
    }

    public default UIKeyframeSheet getSheet(String id)
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            if (sheet.id.equals(id))
            {
                return sheet;
            }
        }

        return null;
    }

    public UIKeyframeSheet getSheet(int mouseY);

    public boolean addKeyframe(int mouseX, int mouseY);

    public default Keyframe addKeyframe(UIKeyframeSheet sheet, float tick, Object value)
    {
        KeyframeSegment segment = sheet.channel.find(tick);
        Keyframe extra = null;
        BaseValueBasic property = sheet.property;

        if (value == null)
        {
            if ("shadow".equals(sheet.id) || "shadow_size".equals(sheet.id) || "shadow_size_z".equals(sheet.id))
            {
                value = "shadow".equals(sheet.id)
                    ? sheet.channel.getFactory().createEmpty()
                    : 0.5D;
            }
            else if ("shadow_opacity".equals(sheet.id))
            {
                value = 1D;
            }
            else if (segment != null)
            {
                value = segment.createInterpolated();
                extra = segment.a;
            }
            else if (sheet.defaultInsertValue != null)
            {
                value = sheet.channel.getFactory().copy(sheet.defaultInsertValue);
            }
            else if (property != null)
            {
                /* Empty track: drop leftover runtime from deleted keyframes so the next insert
                 * does not resurrect the previous value (especially Color / Blend-era a=0). */
                if (sheet.channel.isEmpty())
                {
                    property.setRuntimeValue(null);
                }

                /* Color on an empty channel must use factory defaults (opaque white), not the
                 * morph color — Blend-era morphs often still carry a=0 / last edited tint. */
                if (sheet.channel.isEmpty() && sheet.channel.getFactory() == KeyframeFactories.COLOR)
                {
                    value = sheet.channel.getFactory().createEmpty();
                }
                else
                {
                    value = sheet.channel.getFactory().copy(property.get());
                }
            }
            else
            {
                value = sheet.channel.getFactory().createEmpty();
            }
        }

        value = sheet.clampValue(value);

        int index = sheet.channel.insert(tick, value);
        Keyframe keyframe = sheet.channel.get(index);

        if (extra != null)
        {
            keyframe.copyOverExtra(extra);
        }

        this.clearSelection();
        this.pickKeyframe(keyframe);
        sheet.selection.add(index);

        return keyframe;
    }

    public default void removeKeyframe(Keyframe keyframe)
    {
        UIKeyframeSheet sheet = this.getSheet(keyframe);

        UIReplaysEditorUtils.removeCompanionPaintForColorKeyframe(this.getHostKeyframes(), keyframe);

        sheet.remove(keyframe);
        this.clearPropertyRuntimeIfEmpty(sheet);
        this.clearSelection();
        this.pickKeyframe(null);
    }

    public default void removeSelected()
    {
        UIReplaysEditorUtils.removeCompanionPaintForSelectedColor(this.getHostKeyframes());

        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.selection.removeSelected();
            this.clearPropertyRuntimeIfEmpty(sheet);
        }

        this.pickKeyframe(null);
    }

    /**
     * When the last keyframe is gone, clear form property runtime so playback and the next
     * insert fall back to the morph base instead of the deleted keyframe's value.
     */
    public default void clearPropertyRuntimeIfEmpty(UIKeyframeSheet sheet)
    {
        if (sheet != null && sheet.channel.isEmpty() && sheet.property != null)
        {
            sheet.property.setRuntimeValue(null);
        }
    }

    public Pair<Keyframe, KeyframeType> findKeyframe(int mouseX, int mouseY);

    public default void pickSelected()
    {
        this.pickKeyframe(this.getSelected());
    }

    public default void onCallback(Keyframe keyframe)
    {}

    public void pickKeyframe(Keyframe keyframe);

    public void selectKeyframe(Keyframe keyframe);

    public default void setTick(float tick, boolean dirty)
    {
        Keyframe selected = this.getSelected();
        if (selected == null)
        {
            return;
        }

        float diff = tick - selected.getTick();

        UIReplaysEditorUtils.moveCompanionPaintForSelectedColor(this.getHostKeyframes(), diff);

        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.setTickBy(diff, dirty);
        }
    }

    public default void setDuration(float duration)
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.setDuration(duration);
        }
    }

    public default void setInterpolation(Interpolation interpolation)
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            sheet.setInterpolation(interpolation);
        }
    }

    public default void setValue(Object value, boolean unmergeable)
    {
        Keyframe selected = this.getSelected();
        if (selected == null)
        {
            return;
        }

        IKeyframeFactory factory = selected.getFactory();
        Object keyframe = factory.copy(selected.getValue());

        for (UIKeyframeSheet sheet : this.getSheets())
        {
            if (sheet.channel.getFactory() == factory)
            {
                sheet.setValue(value, keyframe, unmergeable);
            }
        }
    }

    public void resize();

    /* Input handling */

    public boolean mouseClicked(UIContext context);

    public void mouseReleased(UIContext context);

    public void mouseScrolled(UIContext context);

    public void handleMouse(UIContext context, int lastX, int lastY);

    public void dragKeyframes(UIContext context, Pair<Keyframe, KeyframeType> type, int originalX, int originalY, float originalT, Object originalV);

    /* Rendering */

    public void render(UIContext context);

    public void postRender(UIContext context);

    /* State recovery */

    public void saveState(MapType extra);

    public void restoreState(MapType extra);
}
