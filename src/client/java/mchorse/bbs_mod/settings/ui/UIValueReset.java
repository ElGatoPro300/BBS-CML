package mchorse.bbs_mod.settings.ui;

import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.function.Consumer;
import java.util.function.Function;

public class UIValueReset
{
    public static void attach(UIElement element, BaseValue value)
    {
        attach(element, value, null);
    }

    public static void attach(UIElement element, BaseValue value, Runnable afterReset)
    {
        if (element == null || value == null || !value.isResettable())
        {
            return;
        }

        element.context((menu) ->
        {
            if (!value.isResettable())
            {
                return;
            }

            menu.action(Icons.REFRESH, UIKeys.PROPERTY_RESET_TO_DEFAULT, () ->
            {
                value.resetToDefault();

                if (afterReset != null)
                {
                    afterReset.run();
                }
            });
        });
    }

    public static void attach(UIElement element, Runnable resetAction)
    {
        if (element == null || resetAction == null)
        {
            return;
        }

        element.context((menu) -> menu.action(Icons.REFRESH, UIKeys.PROPERTY_RESET_TO_DEFAULT, resetAction));
    }

    public static <O> void attachEdit(UIElement element, Consumer<Consumer<O>> edit, Function<O, ? extends BaseValue> getter, Runnable syncUI)
    {
        if (element == null || edit == null || getter == null)
        {
            return;
        }

        attach(element, () ->
        {
            edit.accept((obj) -> getter.apply(obj).resetToDefault());

            if (syncUI != null)
            {
                syncUI.run();
            }
        });
    }

    public static void attachClip(UIElement element, IUIClipsDelegate editor, BaseValue value, Runnable syncUI)
    {
        if (element == null || editor == null || value == null)
        {
            return;
        }

        attach(element, value, () ->
        {
            editor.editMultiple(value, (v) -> v.resetToDefault());

            if (syncUI != null)
            {
                syncUI.run();
            }
        });
    }
}
