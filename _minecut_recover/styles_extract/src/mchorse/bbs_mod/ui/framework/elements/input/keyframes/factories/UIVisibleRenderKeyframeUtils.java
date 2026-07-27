package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

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

        Keyframe<Boolean> first = render.get(0);

        if (first != null && tick < first.getTick())
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

    public static void syncRenderOnVisibleInsert(KeyframeChannel<?> visible, float tick)
    {
        if (visible == null || !FormUtils.isVisiblePropertyPath(visible.getId()))
        {
            return;
        }

        KeyframeChannel<Boolean> render = getRenderChannel(visible);

        if (render == null || findExact(render, tick) != null)
        {
            return;
        }

        render.insert(tick, getRenderValue(render, tick));
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
}
