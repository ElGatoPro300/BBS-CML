package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.settings.ui.UIIconToolbarOrderEditor;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

public class UIFormEditorGizmoToolbarOverlayPanel extends UIOverlayPanel
{
    public UIFormEditorGizmoToolbarOverlayPanel(Runnable onChange)
    {
        super(L10n.lang("bbs.config.axes.form_gizmo_toolbar"));

        UIIconToolbarOrderEditor editor = new UIIconToolbarOrderEditor(
            BBSSettings.editorFormGizmoToolbar,
            FormEditorGizmoToolbarButtons::getIcon,
            FormEditorGizmoToolbarButtons::getTooltip,
            onChange
        );

        editor.w(1F);

        UILabel hint = UI.label(UIKeys.FORMS_EDITOR_GIZMO_TOOLBAR_HINT, 0).color(0x888888);
        hint.relative(editor).w(1F);

        UIElement column = UI.column(4, hint.marginBottom(4), editor);

        column.relative(this.content).w(1F).h(1F);
        this.content.add(column);
    }
}
