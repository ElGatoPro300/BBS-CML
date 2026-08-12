package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.List;

public class UIVisibleRenderKeyframeUtils
{
    public static boolean isRenderTimelineHidden(String key)
    {
        return FormUtils.isRenderPropertyPath(key);
    }

    public static KeyframeChannel<Boolean> getRenderChannel(KeyframeChannel<?> sourceChannel)
    {
        return getRenderChannel(sourceChannel, null);
    }

    public static KeyframeChannel<Boolean> getRenderChannel(KeyframeChannel<?> sourceChannel, Form form)
    {
        if (sourceChannel == null)
        {
            return null;
        }

        BaseValue parent = sourceChannel.getParent();

        if (!(parent instanceof FormProperties formProperties))
        {
            return null;
        }

        String renderKey = FormUtils.getRenderPropertyPath(sourceChannel.getId());
        KeyframeChannel<?> render = formProperties.properties.get(renderKey);

        if (render == null)
        {
            BaseValue child = formProperties.get(renderKey);

            if (child instanceof KeyframeChannel<?> channel)
            {
                render = channel;
            }
        }

        if (render == null && form != null)
        {
            render = formProperties.getOrCreate(form, renderKey);
        }

        if (render != null && render.getFactory() == KeyframeFactories.BOOLEAN)
        {
            @SuppressWarnings("unchecked")
            KeyframeChannel<Boolean> booleanChannel = (KeyframeChannel<Boolean>) render;

            return booleanChannel;
        }

        return null;
    }

    public static boolean getRenderValue(KeyframeChannel<Boolean> render, float tick)
    {
        if (render == null || render.isEmpty())
        {
            return true;
        }

        Boolean value = render.interpolate(tick, true);

        return value == null || value;
    }

    public static Keyframe<Boolean> findExact(KeyframeChannel<Boolean> channel, float tick)
    {
        if (channel == null)
        {
            return null;
        }

        for (Keyframe<Boolean> keyframe : channel.getKeyframes())
        {
            if (Math.abs(keyframe.getTick() - tick) < 0.0001F)
            {
                return keyframe;
            }
        }

        return null;
    }

    public static void setRenderValue(UIKeyframes editor, KeyframeChannel<Boolean> render, float tick, boolean value)
    {
        if (render == null || editor == null || editor.isDraggingKeyframes())
        {
            return;
        }

        editor.cacheKeyframes();

        Keyframe<Boolean> existing = findExact(render, tick);

        if (existing != null)
        {
            existing.setValue(value);
        }
        else
        {
            render.insert(tick, value);
        }

        editor.submitKeyframes();
    }

    /**
     * Keep a paired {@code render} keyframe with each {@code visible} insert.
     * Default Enabled is {@code true}. Only inherit mid-timeline render state when
     * other visible keyframes already exist (so recreating the sole keyframe after
     * deleting an Activado=off key does not revive a stale {@code false}).
     */
    public static void syncRenderOnVisibleInsert(KeyframeChannel<?> visible, float tick)
    {
        if (visible == null || !FormUtils.isVisiblePropertyPath(visible.getId()))
        {
            return;
        }

        KeyframeChannel<Boolean> render = getRenderChannel(visible);

        if (render == null)
        {
            return;
        }

        boolean soleVisible = visible.getKeyframes().size() <= 1;
        boolean value = soleVisible || render.isEmpty() ? true : getRenderValue(render, tick);
        Keyframe<Boolean> existing = findExact(render, tick);

        if (existing != null)
        {
            existing.setValue(value);
        }
        else
        {
            render.insert(tick, value);
        }

        pruneRenderToMatchVisible(visible);
    }

    public static void moveRenderKeyframe(UIKeyframes editor, KeyframeChannel<Boolean> render, float oldTick, float newTick)
    {
        if (render == null || editor == null || Math.abs(oldTick - newTick) < 0.0001F)
        {
            return;
        }

        Keyframe<Boolean> keyframe = findExact(render, oldTick);

        if (keyframe == null)
        {
            return;
        }

        if (editor.isDraggingKeyframes())
        {
            keyframe.setTick(newTick);

            return;
        }

        editor.cacheKeyframes();
        keyframe.setTick(newTick);
        editor.submitKeyframes();
    }

    public static void removeRenderAtTick(KeyframeChannel<?> visible, float tick)
    {
        if (visible == null || !FormUtils.isVisiblePropertyPath(visible.getId()))
        {
            return;
        }

        KeyframeChannel<Boolean> render = getRenderChannel(visible);
        Keyframe<Boolean> keyframe = findExact(render, tick);

        if (keyframe != null)
        {
            int index = render.getKeyframes().indexOf(keyframe);

            if (index >= 0)
            {
                render.remove(index);
            }
        }

        pruneRenderToMatchVisible(visible);
    }

    /**
     * Drop orphan {@code render} keys that no longer have a {@code visible} twin, and
     * clear the whole render channel when visible is empty (default Enabled again).
     */
    public static void pruneRenderToMatchVisible(KeyframeChannel<?> visible)
    {
        if (visible == null || !FormUtils.isVisiblePropertyPath(visible.getId()))
        {
            return;
        }

        KeyframeChannel<Boolean> render = getRenderChannel(visible);

        if (render == null || render.isEmpty())
        {
            return;
        }

        if (visible.isEmpty())
        {
            render.removeAll();

            return;
        }

        List<Integer> remove = new ArrayList<>();

        for (int i = 0; i < render.getKeyframes().size(); i++)
        {
            Keyframe<Boolean> renderKeyframe = render.get(i);

            if (renderKeyframe == null || findVisibleAtTick(visible, renderKeyframe.getTick()) == null)
            {
                remove.add(i);
            }
        }

        for (int i = remove.size() - 1; i >= 0; i--)
        {
            render.remove(remove.get(i));
        }
    }

    private static Keyframe<?> findVisibleAtTick(KeyframeChannel<?> visible, float tick)
    {
        for (Keyframe<?> keyframe : visible.getKeyframes())
        {
            if (Math.abs(keyframe.getTick() - tick) < 0.0001F)
            {
                return keyframe;
            }
        }

        return null;
    }

    public static void removeRenderForVisibleKeyframe(UIKeyframes editor, Keyframe<?> keyframe)
    {
        if (editor == null || keyframe == null || !(keyframe.getParent() instanceof KeyframeChannel<?> channel))
        {
            return;
        }

        if (!FormUtils.isVisiblePropertyPath(channel.getId()))
        {
            return;
        }

        removeRenderAtTick(channel, keyframe.getTick());
    }

    public static void removeRenderForSelectedVisible(UIKeyframes editor)
    {
        if (editor == null)
        {
            return;
        }

        for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
        {
            if (!FormUtils.isVisiblePropertyPath(sheet.id))
            {
                continue;
            }

            List<Float> ticks = new ArrayList<>();

            for (Keyframe keyframe : sheet.selection.getSelected())
            {
                ticks.add(keyframe.getTick());
            }

            for (Float tick : ticks)
            {
                KeyframeChannel<Boolean> render = getRenderChannel(sheet.channel);
                Keyframe<Boolean> renderKeyframe = findExact(render, tick);

                if (renderKeyframe != null)
                {
                    int index = render.getKeyframes().indexOf(renderKeyframe);

                    if (index >= 0)
                    {
                        render.remove(index);
                    }
                }
            }
        }
    }

    public static void pruneRenderAfterVisibleEdit(UIKeyframes editor)
    {
        if (editor == null)
        {
            return;
        }

        for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
        {
            if (FormUtils.isVisiblePropertyPath(sheet.id))
            {
                pruneRenderToMatchVisible(sheet.channel);
            }
        }
    }
}
