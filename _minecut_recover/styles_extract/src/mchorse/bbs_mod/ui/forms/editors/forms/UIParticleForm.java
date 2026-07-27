package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.renderers.ParticleFormRenderer;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;

public class UIParticleForm extends UIForm<ParticleForm>
{
    public UIColor glowingColor;
    public UITrackpad glowIntensity;

    public UIParticleForm()
    {
        super();

        UIElement button = new UIButton(UIKeys.FORMS_EDITORS_BILLBOARD_PICK_TEXTURE, (b) ->
        {
            Link texture = this.form.texture.get();
            ParticleEmitter emitter = ((ParticleFormRenderer) FormUtilsClient.getRenderer(this.form)).getEmitter();

            if (emitter != null && texture == null)
            {
                texture = emitter.scheme.texture;
            }

            UITexturePicker.open(this.getContext(), texture, (l) -> this.form.texture.set(l));
        }).marginBottom(6);

        this.glowingColor = new UIColor((value) ->
        {
            Color color = Color.rgba(value);

            color.a = 1F;
            this.form.glowingColor.set(color);

            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            this.form.glowSettings.set(settings);
        }).direction(Direction.LEFT);
        this.glowingColor.tooltip(UIKeys.FORMS_EDITORS_GLOW);
        this.glowIntensity = new UITrackpad((value) ->
        {
            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.intensity = value.floatValue();
            this.form.glowSettings.set(settings);
        });
        this.glowIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.glowIntensity.tooltip(UIKeys.FORMS_EDITORS_GLOW_INTENSITY);

        this.registerDefaultPanels();

        this.defaultPanel = this.panels.get(this.panels.size() - 1);
        this.defaultPanel.options.prepend(this.glowIntensity);
        this.defaultPanel.options.prepend(this.glowingColor);
        this.defaultPanel.options.prepend(button);

        this.defaultPanel.keys().register(Keys.FORMS_PICK_TEXTURE, button::clickItself);
    }

    @Override
    public void startEdit(ParticleForm form)
    {
        super.startEdit(form);

        GlowSettings glow = form.glowSettings.get();
        Color glowDisplay = new Color();

        glow.resolveColor(form.glowingColor.get(), glowDisplay);
        this.glowingColor.setColor(glowDisplay.getRGBColor());
        this.glowIntensity.setValue(glow.intensity);
    }
}