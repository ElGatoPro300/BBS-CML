package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIParticleSettings;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
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
    public UIColor color;
    public UIColor color2;
    public UIElement color2Container;
    public UIButton colorModeButton;
    public UIElement colorContainer;
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

    public static boolean isColorableParticle(Identifier id)
    {
        return id != null;
    }

    public UIVanillaParticleFormPanel(UIForm editor)
    {
        super(editor);

        this.settings = new UIParticleSettings();
        this.settings.callback((id) -> this.updateEffectVisibility());

        this.color = new UIColor((c) ->
        {
            if (this.form != null)
            {
                this.form.color.get().set(c);
            }

            float r = ((c >> 16) & 0xFF) / 255F;
            float g = ((c >> 8) & 0xFF) / 255F;
            float b = (c & 0xFF) / 255F;
            float a = ((c >> 24) & 0xFF) / 255F;

            if (a <= 0F)
            {
                a = 1.0F;
            }

            String argString = String.format(Locale.ROOT, "%.2f %.2f %.2f %.1f", r, g, b, a);

            this.settings.setArgumentsText(argString);
            this.updateEffectLabelForColor(argString);
        }).withAlpha();

        this.color2 = new UIColor((c) ->
        {
            if (this.form != null)
            {
                this.form.color2.get().set(c);
            }
        }).withAlpha();

        this.colorModeButton = new UIButton(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR_MODE_SOLID, (b) ->
        {
            if (this.form != null)
            {
                int mode = (this.form.colorMode.get() + 1) % 3;

                this.form.colorMode.set(mode);
                this.updateColorModeButton();
            }
        });

        this.color2Container = UI.column(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR2).marginTop(4), this.color2);

        this.colorContainer = UI.column(
            UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR_MODE).marginTop(6),
            this.colorModeButton,
            UI.row(UI.column(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR).marginTop(4), this.color), this.color2Container)
        );

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

        this.effectButton = new UIButton(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_NONE, (b) ->
        {
            UIListOverlayPanel overlayPanel = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EFFECT_TITLE, (l) ->
            {
                if (l.equals("none"))
                {
                    this.setEffectNone();
                }
                else
                {
                    this.setEffect(Identifier.of(l));
                }
            });

            List<String> strings = new ArrayList<>();

            strings.add("none");

            for (RegistryKey<StatusEffect> key : Registries.STATUS_EFFECT.getKeys())
            {
                strings.add(key.getValue().toString());
            }

            overlayPanel.addValues(strings);
            overlayPanel.list.list.sort();

            UIOverlay.addOverlay(this.getContext(), overlayPanel);
        });

        this.effectContainer = UI.column(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EFFECT).marginTop(6), this.effectButton);

        this.options.add(this.settings, this.colorContainer, this.paused.marginTop(6), this.local, UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_VELOCITY).marginTop(6), this.velocity);
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
        this.color.setColor(form.color.get().getARGBColor());
        this.color2.setColor(form.color2.get().getARGBColor());
        this.updateColorModeButton();
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

    private void updateColorModeButton()
    {
        if (this.form == null)
        {
            return;
        }

        int mode = this.form.colorMode.get();

        if (mode == 1)
        {
            this.colorModeButton.label = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR_MODE_LIFETIME;
            this.color2Container.setVisible(true);
        }
        else if (mode == 2)
        {
            this.colorModeButton.label = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR_MODE_RANDOM;
            this.color2Container.setVisible(true);
        }
        else
        {
            this.colorModeButton.label = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR_MODE_SOLID;
            this.color2Container.setVisible(false);
        }
    }

    private void updateEffectVisibility()
    {
        if (this.form == null)
        {
            return;
        }

        Identifier id = this.form.settings.get().particle;
        boolean isColorable = isColorableParticle(id);
        boolean isEffect = isEffectParticle(id);

        this.colorContainer.setVisible(isColorable);
        this.effectContainer.setVisible(isEffect);

        String args = this.form.settings.get().arguments.trim();

        if (isColorable)
        {
            int colorInt = 0xFFFFFFFF;

            if (!args.isEmpty())
            {
                try
                {
                    String[] split = args.split("\\s+");

                    if (split.length >= 3)
                    {
                        float r = Float.parseFloat(split[0]);
                        float g = Float.parseFloat(split[1]);
                        float b = Float.parseFloat(split[2]);
                        float a = split.length >= 4 ? Float.parseFloat(split[3]) : 1.0F;

                        int ir = (int) (r * 255F);
                        int ig = (int) (g * 255F);
                        int ib = (int) (b * 255F);
                        int ia = (int) (a * 255F);

                        colorInt = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                    }
                }
                catch (Exception e)
                {}
            }

            this.color.setColor(colorInt);
        }

        if (isEffect)
        {
            this.updateEffectLabelForColor(args);
        }
    }

    private void setEffectNone()
    {
        String argString = "1.00 1.00 1.00 1.0";

        if (this.form != null)
        {
            this.form.color.get().set(1F, 1F, 1F, 1F);
        }

        this.settings.setArgumentsText(argString);
        this.color.setColor(0xFFFFFFFF);
        this.effectButton.label = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_NONE;
    }

    private void setEffect(Identifier effectId)
    {
        StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);

        if (effect != null)
        {
            int colorInt = effect.getColor();
            float r = ((colorInt >> 16) & 0xFF) / 255F;
            float g = ((colorInt >> 8) & 0xFF) / 255F;
            float b = (colorInt & 0xFF) / 255F;
            float a = 1.0F;

            if (this.form != null)
            {
                this.form.color.get().set(r, g, b, a);
            }

            String argString = String.format(Locale.ROOT, "%.2f %.2f %.2f %.1f", r, g, b, a);

            this.settings.setArgumentsText(argString);
            this.color.setColor((0xFF << 24) | (colorInt & 0xFFFFFF));
            this.effectButton.label = IKey.raw(effectId.toString());
        }
    }

    private void updateEffectLabelForColor(String args)
    {
        Identifier currentEffect = null;
        String trimmed = args.trim();

        if (!trimmed.isEmpty())
        {
            for (RegistryKey<StatusEffect> key : Registries.STATUS_EFFECT.getKeys())
            {
                StatusEffect effect = Registries.STATUS_EFFECT.get(key);

                if (effect != null)
                {
                    int colorInt = effect.getColor();
                    float r = ((colorInt >> 16) & 0xFF) / 255F;
                    float g = ((colorInt >> 8) & 0xFF) / 255F;
                    float b = (colorInt & 0xFF) / 255F;
                    float a = 1.0F;

                    String expected = String.format(Locale.ROOT, "%.2f %.2f %.2f %.1f", r, g, b, a);

                    if (trimmed.equals(expected))
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
            this.effectButton.label = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_NONE;
        }
    }
}