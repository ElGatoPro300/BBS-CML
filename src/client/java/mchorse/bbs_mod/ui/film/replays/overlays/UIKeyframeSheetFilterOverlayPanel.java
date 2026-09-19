package mchorse.bbs_mod.ui.film.replays.overlays;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UIKeyframeSheetFilterOverlayPanel extends UIOverlayPanel
{
    public UIKeyframeSheetFilterOverlayPanel(Set<String> disabled, Set<String> keys)
    {
        this(disabled, keys, null);
    }

    public UIKeyframeSheetFilterOverlayPanel(Set<String> disabled, Set<String> keys, Map<String, Integer> keyToColor)
    {
        super(UIKeys.FILM_REPLAY_FILTER_SHEETS_TITLE);

        UIScrollView scrollView = UI.scrollView(4, 6);

        scrollView.full(this.content);
        this.content.add(scrollView);

        for (String key : keys)
        {
            int color = keyToColor != null && keyToColor.containsKey(key) ? keyToColor.get(key) : UIReplaysEditor.getColor(key);
            UIToggle toggle = new UICoolToggle(key, IKey.constant(key), color, (b) ->
            {
                if (disabled.contains(key))
                {
                    disabled.remove(key);
                }
                else
                {
                    disabled.add(key);
                }
            });

            toggle.h(UIConstants.CONTROL_HEIGHT);
            toggle.setValue(!disabled.contains(key));
            scrollView.add(toggle);
        }
    }

    public static class UICoolToggle extends UIToggle
    {
        private String key;
        private int color;

        public UICoolToggle(String key, IKey label, int color, Consumer<UIToggle> callback)
        {
            super(label, callback);

            this.key = key;
            this.color = color;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            int x = this.area.x;
            int y = this.area.y;
            int w = this.area.w;
            int h = this.area.h;
            Icon icon = UIReplaysEditor.getIcon(this.key);

            context.batcher.box(x, y, x + 2, y + h, Colors.A100 | color);
            context.batcher.gradientHBox(x + 2, y, x + 24, y + h, Colors.A25 | color, color);
            context.batcher.icon(icon, x + 2, y + h / 2, 0F, 0.5F);

            this.area.x += 20;
            this.area.w -= 20;

            super.renderSkin(context);

            this.area.x = x;
            this.area.w = w;
        }
    }
}
