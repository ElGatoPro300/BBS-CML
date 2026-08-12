package mchorse.bbs_mod.ui.forms.overlays;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UIFavoriteCategoryOverlayPanel extends UIOverlayPanel
{
    public static class FavoriteCategoryData
    {
        public final String name;
        public final String iconName;
        public final int color;

        public FavoriteCategoryData(String name, String iconName, int color)
        {
            this.name = name;
            this.iconName = iconName;
            this.color = color;
        }
    }

    private final Function<FavoriteCategoryData, Boolean> callback;

    public UITextbox name;
    public UIIcon iconPreview;
    public UIButton iconPicker;
    public UIColor colorInput;
    public UIButton accept;

    private String iconName = "five_star";
    private int color = Colors.WHITE;

    public UIFavoriteCategoryOverlayPanel(Function<FavoriteCategoryData, Boolean> callback)
    {
        this(UIKeys.FORMS_LIST_CATEGORIES_CREATE_TITLE, UIKeys.GENERAL_ADD, null, callback);
    }

    public UIFavoriteCategoryOverlayPanel(IKey title, IKey acceptLabel, FavoriteCategoryData initialData, Function<FavoriteCategoryData, Boolean> callback)
    {
        super(title == null ? UIKeys.FORMS_LIST_CATEGORIES_CREATE_TITLE : title);

        this.callback = callback;

        if (initialData != null)
        {
            if (initialData.iconName != null && !initialData.iconName.isBlank())
            {
                this.iconName = initialData.iconName;
            }

            this.color = initialData.color & Colors.RGB;
        }

        this.name = new UITextbox(20, null).placeholder(UIKeys.FORMS_LIST_CATEGORIES_CREATE_NAME);
        this.iconPreview = new UIIcon(this::getSelectedIcon, null).iconColor(Colors.WHITE);
        this.iconPicker = new UIButton(UIKeys.FORMS_LIST_CATEGORIES_CREATE_ICON, (b) -> this.pickIcon());
        this.colorInput = new UIColor((c) ->
        {
            this.color = c & Colors.RGB;
            this.refreshLabels();
        });
        this.colorInput.setColor(this.color | Colors.A100);
        this.accept = new UIButton(acceptLabel == null ? UIKeys.GENERAL_ADD : acceptLabel, (b) -> this.createCategory());

        UIButton cancel = new UIButton(UIKeys.GENERAL_CLOSE, (b) -> this.close());

        this.name.relative(this.content).x(10).y(10).w(1F, -20).h(20);
        this.iconPreview.relative(this.content).x(10).y(40).wh(20, 20);
        this.iconPicker.relative(this.content).x(36).y(40).w(1F, -46).h(20);
        this.colorInput.relative(this.content).x(10).y(70).w(1F, -20).h(20);
        this.accept.relative(this.content).x(10).y(1F, -30).w(0.5F, -15).h(20);
        cancel.relative(this.content).x(0.5F, 5).y(1F, -30).w(0.5F, -15).h(20);

        this.content.add(this.name, this.iconPreview, this.iconPicker, this.colorInput, this.accept, cancel);

        if (initialData != null && initialData.name != null)
        {
            this.name.setText(initialData.name);
        }

        this.refreshLabels();
    }

    @Override
    protected void onAdd(UIElement parent)
    {
        super.onAdd(parent);
    }

    private void createCategory()
    {
        String categoryName = this.name.getText().trim();

        if (categoryName.isEmpty())
        {
            return;
        }

        boolean created = true;

        if (this.callback != null)
        {
            created = Boolean.TRUE.equals(this.callback.apply(new FavoriteCategoryData(categoryName, this.iconName, this.color)));
        }

        if (created)
        {
            this.close();
        }
    }

    private Icon getSelectedIcon()
    {
        return Icons.ICONS.getOrDefault(this.iconName, Icons.FIVE_STAR);
    }

    private void pickIcon()
    {
        List<String> icons = new ArrayList<>(Icons.ICONS.keySet());

        Collections.sort(icons);

        UIIconPickerOverlayPanel panel = new UIIconPickerOverlayPanel(UIKeys.FORMS_LIST_CATEGORIES_PICK_ICON, icons, this.iconName, (key) ->
        {
            if (key != null && !key.isBlank() && Icons.ICONS.containsKey(key))
            {
                this.iconName = key;
                this.refreshLabels();
            }
        });

        UIOverlay.addOverlay(this.getContext(), panel, 260, 0.75F);
    }

    private void refreshLabels()
    {
        this.iconPicker.label = IKey.constant(UIKeys.FORMS_LIST_CATEGORIES_CREATE_ICON.format(this.iconName).get());
    }

    private static class UIIconPickerOverlayPanel extends UIOverlayPanel
    {
        public UISearchList<String> list;

        public UIIconPickerOverlayPanel(IKey title, List<String> icons, String selected, Consumer<String> callback)
        {
            super(title);

            this.list = new UISearchList<>(new UIStringList((items) ->
            {
                if (items.isEmpty())
                {
                    return;
                }

                if (callback != null)
                {
                    callback.accept(items.get(0));
                }

                this.close();
            })
            {
                @Override
                protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
                {
                    Icon icon = Icons.ICONS.getOrDefault(element, Icons.NONE);
                    int iconColor = Colors.WHITE;
                    int textColor = hover ? Colors.HIGHLIGHT : Colors.WHITE;

                    context.batcher.icon(icon, iconColor, x + 12, y + this.scroll.scrollItemSize / 2, 0.5F, 0.5F);
                    context.batcher.textShadow(element, x + 24, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, textColor);
                }
            });

            this.list.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
            this.content.add(this.list);

            this.list.list.add(icons);
            this.list.list.sort();
            this.list.list.setCurrentScroll(selected);
        }
    }
}
