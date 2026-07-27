package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.FluidForm;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;

public class UIFluidFormPanel extends UIFormPanel<FluidForm>
{
    public UICirculate presets;
    public UICirculate mode;
    
    public UITrackpad flowSpeed;
    public UITrackpad turbulence;
    public UITrackpad physicsSensitivity;
    public UITrackpad subdivisions;
    public UIToggle smoothShading;
    public UIToggle debug;
    public UIColor color;
    public UITrackpad opacity;
    public UIButton texture;

    // Ocean
    public UITrackpad sizeX;
    public UITrackpad sizeY;
    public UITrackpad sizeZ;
    public UIToggle fillBlock;
    public UITrackpad waveAmplitude;
    public UITrackpad waveFrequency;

    // Drop
    public UITrackpad dropSize;
    public UITrackpad surfaceTension;
    public UITrackpad viscosity;

    public UIFluidFormPanel(UIForm editor)
    {
        super(editor);

        this.presets = new UICirculate((b) -> this.applyPreset(this.presets.getValue()));
        this.presets.addLabel(UIKeys.FLUID_PRESET_CUSTOM);
        this.presets.addLabel(UIKeys.FLUID_PRESET_CALM_OCEAN);
        this.presets.addLabel(UIKeys.FLUID_PRESET_STORMY_OCEAN);
        this.presets.addLabel(UIKeys.FLUID_PRESET_VISCOUS_DROP);

        this.mode = new UICirculate((b) -> {
            this.form.mode.set(FluidForm.FluidMode.values()[this.mode.getValue()]);
            this.updateVisibility();
        });
        
        this.mode.addLabel(UIKeys.FLUID_MODE_FULL_OCEAN);
        this.mode.addLabel(UIKeys.FLUID_MODE_WATER_DROP);
        this.mode.addLabel(UIKeys.FLUID_MODE_PROCEDURAL);
        
        this.flowSpeed = new UITrackpad((v) -> this.form.flowSpeed.set(v.floatValue()));
        this.flowSpeed.tooltip(UIKeys.FLUID_FLOW_SPEED);
        
        this.turbulence = new UITrackpad((v) -> this.form.turbulence.set(v.floatValue()));
        this.turbulence.tooltip(UIKeys.FLUID_TURBULENCE);
        
        this.physicsSensitivity = new UITrackpad((v) -> this.form.physicsSensitivity.set(v.floatValue()));
        this.physicsSensitivity.tooltip(UIKeys.FLUID_PHYSICS_SENSITIVITY);

        this.subdivisions = new UITrackpad((v) -> this.form.subdivisions.set(v.intValue()));
        this.subdivisions.integer().limit(1, 8);
        this.subdivisions.tooltip(UIKeys.FLUID_SUBDIVISIONS);

        this.smoothShading = new UIToggle(UIKeys.FLUID_SMOOTH_SHADING, true, (b) -> this.form.smoothShading.set(b.getValue()));
        this.smoothShading.tooltip(UIKeys.FLUID_SMOOTH_SHADING);

        this.debug = new UIToggle(UIKeys.FLUID_DEBUG, false, (b) -> this.form.debug.set(b.getValue()));
        this.debug.tooltip(UIKeys.FLUID_DEBUG);

        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).direction(Direction.LEFT).withAlpha();
        
        this.opacity = new UITrackpad((v) -> this.form.opacity.set(v.floatValue()));
        this.opacity.tooltip(UIKeys.FLUID_OPACITY);
        
        this.texture = new UIButton(UIKeys.FORMS_EDITORS_BILLBOARD_PICK_TEXTURE, (b) -> {
            UITexturePicker.open(this.getContext(), this.form.texture.get(), (l) -> this.form.texture.set(l));
        });

        this.sizeX = new UITrackpad((v) -> this.form.sizeX.set(v.floatValue()));
        this.sizeX.tooltip(UIKeys.FLUID_SIZE_X);
        
        this.sizeY = new UITrackpad((v) -> this.form.sizeY.set(v.floatValue()));
        this.sizeY.tooltip(UIKeys.FLUID_SIZE_Y);
        
        this.sizeZ = new UITrackpad((v) -> this.form.sizeZ.set(v.floatValue()));
        this.sizeZ.tooltip(UIKeys.FLUID_SIZE_Z);
        
        this.fillBlock = new UIToggle(UIKeys.FLUID_FILL_BLOCK, false, (b) -> this.form.fillBlock.set(b.getValue()));
        
        this.waveAmplitude = new UITrackpad((v) -> this.form.waveAmplitude.set(v.floatValue()));
        this.waveAmplitude.tooltip(UIKeys.FLUID_WAVE_AMPLITUDE);
        
        this.waveFrequency = new UITrackpad((v) -> this.form.waveFrequency.set(v.floatValue()));
        this.waveFrequency.tooltip(UIKeys.FLUID_WAVE_FREQUENCY);
        
        this.dropSize = new UITrackpad((v) -> this.form.dropSize.set(v.floatValue()));
        this.dropSize.tooltip(UIKeys.FLUID_DROP_SIZE);
        
        this.surfaceTension = new UITrackpad((v) -> this.form.surfaceTension.set(v.floatValue()));
        this.surfaceTension.tooltip(UIKeys.FLUID_SURFACE_TENSION);

        this.viscosity = new UITrackpad((v) -> this.form.viscosity.set(v.floatValue()));
        this.viscosity.tooltip(UIKeys.FLUID_VISCOSITY);

        // Labels
        IKey categoryCommon = UIKeys.FLUID_CATEGORY_COMMON;
        IKey categoryOcean = UIKeys.FLUID_CATEGORY_OCEAN;
        IKey categoryDrop = UIKeys.FLUID_CATEGORY_DROP;

        this.options.add(UI.label(UIKeys.FLUID_PRESETS), this.presets);
        this.options.add(UI.label(UIKeys.FLUID_MODE_LABEL).marginTop(8), this.mode);
        
        this.options.add(UI.label(categoryCommon).marginTop(8));
        this.options.add(this.texture, this.color);
        this.options.add(UI.row(this.flowSpeed, this.turbulence, this.physicsSensitivity));
        this.options.add(UI.row(this.opacity, this.subdivisions));
        this.options.add(UI.row(this.smoothShading, this.debug));

        this.options.add(UI.label(categoryOcean).marginTop(8));
        this.options.add(UI.row(this.sizeX, this.sizeY, this.sizeZ));
        this.options.add(this.fillBlock);
        this.options.add(UI.row(this.waveAmplitude, this.waveFrequency));

        this.options.add(UI.label(categoryDrop).marginTop(8));
        this.options.add(this.dropSize);
        this.options.add(UI.row(this.surfaceTension, this.viscosity));
    }

    private void applyPreset(int value)
    {
        if (value == 1) // Calm Ocean
        {
             this.form.mode.set(FluidForm.FluidMode.FULL_OCEAN);
             this.form.waveAmplitude.set(0.2f);
             this.form.turbulence.set(0.0f);
             this.form.flowSpeed.set(0.5f);
             this.form.color.set(new Color(0.0f, 0.6f, 0.8f, 0.6f));
        }
        else if (value == 2) // Stormy
        {
             this.form.mode.set(FluidForm.FluidMode.FULL_OCEAN);
             this.form.waveAmplitude.set(1.2f);
             this.form.turbulence.set(0.8f);
             this.form.flowSpeed.set(2.0f);
             this.form.color.set(new Color(0.1f, 0.2f, 0.4f, 0.9f));
        }
        else if (value == 3) // Viscous Drop
        {
             this.form.mode.set(FluidForm.FluidMode.WATER_DROP);
             this.form.color.set(new Color(0.8f, 0.6f, 0.2f, 0.9f));
             this.form.viscosity.set(0.8f);
             this.form.surfaceTension.set(0.6f);
        }
        
        this.startEdit(this.form);
    }

    private void updateVisibility()
    {
        FluidForm.FluidMode m = this.form.mode.get();
        boolean ocean = m == FluidForm.FluidMode.FULL_OCEAN;
        boolean procedural = m == FluidForm.FluidMode.PROCEDURAL;
        boolean anyOcean = ocean || procedural;
        boolean isDrop = !anyOcean;
        
        this.sizeX.setEnabled(anyOcean);
        this.sizeY.setEnabled(anyOcean);
        this.sizeZ.setEnabled(anyOcean);
        this.fillBlock.setEnabled(ocean);
        this.waveAmplitude.setEnabled(anyOcean);
        this.waveFrequency.setEnabled(procedural);

        this.dropSize.setEnabled(isDrop);
        this.surfaceTension.setEnabled(isDrop);
        this.viscosity.setEnabled(isDrop);
    }

    @Override
    public void startEdit(FluidForm form)
    {
        super.startEdit(form);

        this.presets.setValue(0);
        this.mode.setValue(form.mode.get().ordinal());
        
        this.flowSpeed.setValue(form.flowSpeed.get());
        this.turbulence.setValue(form.turbulence.get());
        this.physicsSensitivity.setValue(form.physicsSensitivity.get());
        this.subdivisions.setValue(form.subdivisions.get());
        
        this.smoothShading.setValue(form.smoothShading.get());
        this.debug.setValue(form.debug.get());
        this.color.setColor(form.color.get().getARGBColor());
        this.opacity.setValue(form.opacity.get());
        
        this.sizeX.setValue(form.sizeX.get());
        this.sizeY.setValue(form.sizeY.get());
        this.sizeZ.setValue(form.sizeZ.get());
        this.fillBlock.setValue(form.fillBlock.get());
        this.waveAmplitude.setValue(form.waveAmplitude.get());
        this.waveFrequency.setValue(form.waveFrequency.get());

        this.dropSize.setValue(form.dropSize.get());
        this.surfaceTension.setValue(form.surfaceTension.get());
        this.viscosity.setValue(form.viscosity.get());
        
        this.updateVisibility();
    }
}
