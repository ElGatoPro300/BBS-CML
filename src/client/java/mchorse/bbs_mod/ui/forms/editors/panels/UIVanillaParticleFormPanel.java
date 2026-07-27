package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIParticleSettings;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UIVanillaParticleFormPanel extends UIFormPanel<VanillaParticleForm>
{
    public UIParticleSettings settings;
    public UIToggle paused;
    public UIToggle local;
    public UITrackpad velocity;
    public UITrackpad count;
    public UITrackpad frequency;
    public UITrackpad scatteringYaw;
    public UITrackpad scatteringPitch;
    public UITrackpad offsetX;
    public UITrackpad offsetY;
    public UITrackpad offsetZ;
    public UIElement effectContainer;
    public UIButton effectButton;

    public static boolean isEffectParticle(Identifier id)
    {
        if (id == null)
        {
            return false;
        }

        String path = id.getPath();

        return path.equals("effect") || path.equals("entity_effect") || path.equals("ambient_entity_effect") || path.equals("instant_effect") || path.contains("effect");
    }

    public UIVanillaParticleFormPanel(UIForm editor)
    {
        super(editor);

        this.settings = new UIParticleSettings();
        this.settings.callback((id) -> this.updateEffectVisibility());

        this.paused = new UIToggle(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_PAUSED, (b) -> this.form.paused.set(b.getValue()));
        this.local = new UIToggle(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_LOCAL, (b) -> this.form.local.set(b.getValue()));
        this.velocity = new UITrackpad((v) -> this.form.velocity.set(v.floatValue()));
        this.count = new UITrackpad((v) -> this.form.count.set(v.intValue())).integer();
        this.count.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COUNT);
        this.frequency = new UITrackpad((v) -> this.form.frequency.set(v.intValue())).integer();
        this.frequency.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_FREQUENCY);
        this.scatteringYaw = new UITrackpad((v) -> this.form.scatteringYaw.set(v.floatValue()));
        this.scatteringYaw.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_HORIZONTAL);
        this.scatteringPitch = new UITrackpad((v) -> this.form.scatteringPitch.set(v.floatValue()));
        this.scatteringPitch.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_VERTICAL);
        this.offsetX = new UITrackpad((v) -> this.form.offsetX.set(v.floatValue()));
        this.offsetX.tooltip(UIKeys.GENERAL_X);
        this.offsetY = new UITrackpad((v) -> this.form.offsetY.set(v.floatValue()));
        this.offsetY.tooltip(UIKeys.GENERAL_Y);
        this.offsetZ = new UITrackpad((v) -> this.form.offsetZ.set(v.floatValue()));
        this.offsetZ.tooltip(UIKeys.GENERAL_Z);

        this.effectButton = new UIButton(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_PICK_EFFECT, (b) ->
        {
            UIListOverlayPanel overlayPanel = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EFFECT_TITLE, (l) -> this.setEffect(Identifier.of(l)));
            List<String> strings = new ArrayList<>();

            for (RegistryKey<StatusEffect> key : Registries.STATUS_EFFECT.getKeys())
            {
                strings.add(key.getValue().toString());
            }

            overlayPanel.addValues(strings);
            overlayPanel.list.list.sort();

            UIOverlay.addOverlay(this.getContext(), overlayPanel);
        });

        this.effectContainer = UI.column(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EFFECT).marginTop(6), this.effectButton);

        this.options.add(this.settings, this.paused.marginTop(6), this.local, UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_VELOCITY).marginTop(6), this.velocity);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EMISSION).marginTop(6), this.count, this.frequency);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_SCATTER).marginTop(6), this.scatteringYaw, this.scatteringPitch);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_OFFSET).marginTop(6), this.offsetX, this.offsetY, this.offsetZ);
        this.options.add(this.effectContainer);
    }

    @Override
    public void startEdit(VanillaParticleForm form)
    {
        super.startEdit(form);

        this.settings.setSettings(form.settings.get());
        this.paused.setValue(form.paused.get());
        this.local.setValue(form.local.get());
        this.velocity.setValue(form.velocity.get());
        this.count.setValue(form.count.get());
        this.frequency.setValue(form.frequency.get());
        this.scatteringYaw.setValue(form.scatteringYaw.get());
        this.scatteringPitch.setValue(form.scatteringPitch.get());
        this.offsetX.setValue(form.offsetX.get());
        this.offsetY.setValue(form.offsetY.get());
        this.offsetZ.setValue(form.offsetZ.get());

        this.updateEffectVisibility();
    }

    private void updateEffectVisibility()
    {
        if (this.form == null)
        {
            return;
        }

        Identifier id = this.form.settings.get().particle;
        boolean isEffect = isEffectParticle(id);

        this.effectContainer.setVisible(isEffect);

        if (isEffect)
        {
            Identifier currentEffect = null;
            String args = this.form.settings.get().arguments.trim();

            if (!args.isEmpty())
            {
                for (RegistryKey<StatusEffect> key : Registries.STATUS_EFFECT.getKeys())
                {
                    StatusEffect effect = Registries.STATUS_EFFECT.get(key);

                    if (effect != null)
                    {
                        int color = effect.getColor();
                        float r = ((color >> 16) & 0xFF) / 255F;
                        float g = ((color >> 8) & 0xFF) / 255F;
                        float b = (color & 0xFF) / 255F;
                        float a = 1.0F;

                        String expected = String.format(Locale.ROOT, "%.2f %.2f %.2f %.1f", r, g, b, a);

                        if (args.equals(expected))
                        {
                            currentEffect = key.getValue();
                            break;
                        }
                    }
                }
            }

            if (currentEffect != null)
            {
                this.effectButton.label = IKey.raw(currentEffect.toString());
            }
            else
            {
                this.effectButton.label = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_PICK_EFFECT;
            }
        }
    }

    private void setEffect(Identifier effectId)
    {
        StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);

        if (effect != null)
        {
            int color = effect.getColor();
            float r = ((color >> 16) & 0xFF) / 255F;
            float g = ((color >> 8) & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            float a = 1.0F;

            String argString = String.format(Locale.ROOT, "%.2f %.2f %.2f %.1f", r, g, b, a);

            this.settings.setArgumentsText(argString);
            this.effectButton.label = IKey.raw(effectId.toString());
        }
    }
}