package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.forms.values.ValueParticleSettings;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Color;

public class VanillaParticleForm extends Form
{
    public final ValueParticleSettings settings = new ValueParticleSettings("settings", new ParticleSettings());
    public final ValueColor color = new ValueColor("color", new Color(1F, 1F, 1F, 1F));
    public final ValueColor color2 = new ValueColor("color2", new Color(1F, 1F, 1F, 1F));
    public final ValueInt colorMode = new ValueInt("color_mode", 0);
    public final ValueBoolean paused = new ValueBoolean("paused", false);
    public final ValueBoolean local = new ValueBoolean("local", false);
    public final ValueFloat velocity = new ValueFloat("velocity", 0.1F);
    public final ValueInt count = new ValueInt("count", 5);
    public final ValueInt frequency = new ValueInt("frequency", 5);
    public final ValueFloat scatteringYaw = new ValueFloat("scattering_yaw", 0F);
    public final ValueFloat scatteringPitch = new ValueFloat("scattering_pitch", 0F);
    public final ValueFloat offsetX = new ValueFloat("offset_x", 0F);
    public final ValueFloat offsetY = new ValueFloat("offset_y", 0F);
    public final ValueFloat offsetZ = new ValueFloat("offset_z", 0F);

    public VanillaParticleForm()
    {
        super();

        this.local.invisible();

        this.add(this.settings);
        this.add(this.color);
        this.add(this.color2);
        this.add(this.colorMode);
        this.add(this.paused);
        this.add(this.local);
        this.add(this.velocity);
        this.add(this.count);
        this.add(this.frequency);
        this.add(this.scatteringYaw);
        this.add(this.scatteringPitch);
        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.offsetZ);
    }
}