package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

public class UILightFormPanel extends UIFormPanel<LightForm>
{
    public UIToggle enabled;
    public UITrackpad level;

    public UILightFormPanel(UIForm editor)
    {
        super(editor);

        this.enabled = new UIToggle(UIKeys.RAW_ENABLE, false, (t) ->
        {
            if (this.form != null)
            {
                this.form.enabled.set(t.getValue());
            }
        });

        this.level = new UITrackpad((v) ->
        {
            int lvl = v.intValue();

            if (this.form != null)
            {
                this.form.level.set(Math.max(0, Math.min(15, lvl)));
            }
        }).integer().limit(0, 15);

        this.level.textbox.setColor(Colors.YELLOW);
        this.level.w(1F);
        this.level.tooltip(UIKeys.MODEL_BLOCKS_LIGHT_LEVEL);

        UIElement icon = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                super.render(context);

                context.batcher.icon(Icons.LIGHT, Colors.WHITE, this.area.mx(), this.area.my(), 0.5F, 0.5F);
            }
        }.w(20).h(20);

        this.options.add(this.enabled);
        this.options.add(UI.row(5, 0, 20, icon, this.level));
    }

    @Override
    public void startEdit(LightForm form)
    {
        super.startEdit(form);

        this.enabled.setValue(form.enabled.get());
        this.level.setValue((double) Math.max(0, Math.min(15, form.level.get())));
    }
}
