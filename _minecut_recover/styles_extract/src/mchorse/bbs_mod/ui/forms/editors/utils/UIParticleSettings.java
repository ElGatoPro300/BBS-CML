package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import net.minecraft.class_2396;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7923;
import java.util.ArrayList;
import java.util.List;

public class UIParticleSettings extends UIElement
{
    public UIButton particle;
    public UITextbox arguments;

    private ParticleSettings settings;

    public UIParticleSettings()
    {
        this.particle = new UIButton(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EDITOR_PICK, (b) ->
        {
            UIListOverlayPanel overlayPanel = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EDITOR_TITLE, (l) -> this.setParticle(class_2960.method_60654(l)));
            List<String> strings = new ArrayList<>();

            for (class_5321<class_2396<?>> key : class_7923.field_41180.method_42021())
            {
                strings.add(key.method_29177().toString());
            }

            overlayPanel.addValues(strings);
            overlayPanel.list.list.sort();
            overlayPanel.setValue(this.settings.particle.toString());

            UIOverlay.addOverlay(this.getContext(), overlayPanel);
        });

        this.arguments = new UITextbox(1000, this::setArguments);

        this.column().vertical().stretch();
        this.add(this.particle, this.arguments);
    }

    public void setSettings(ParticleSettings settings)
    {
        this.settings = settings;

        this.arguments.setText(settings.arguments);
    }

    protected void setParticle(class_2960 id)
    {
        this.settings.particle = id;
    }

    protected void setArguments(String args)
    {
        this.settings.arguments = args;
    }
}