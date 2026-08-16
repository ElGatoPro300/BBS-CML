package mchorse.bbs_mod.ui.forms.overlays;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;

import java.util.function.Consumer;

public class UIRemoveFavoriteCategoryOverlayPanel extends UIConfirmOverlayPanel
{
    public UIRemoveFavoriteCategoryOverlayPanel(String categoryName, Consumer<Boolean> callback)
    {
        super(
            UIKeys.FORMS_LIST_CATEGORIES_REMOVE_TITLE.format(categoryName),
            UIKeys.FORMS_LIST_CATEGORIES_REMOVE_MESSAGE.format(categoryName),
            callback
        );

        this.confirm.label = IKey.constant(UIKeys.GENERAL_REMOVE.get());
    }
}
