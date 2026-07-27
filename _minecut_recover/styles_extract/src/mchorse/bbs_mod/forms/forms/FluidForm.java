package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueEnum;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.LinkUtils;

public class FluidForm extends Form
{
    public enum FluidMode
    {
        FULL_OCEAN, WATER_DROP, PROCEDURAL
    }

    public final ValueEnum<FluidMode> mode = new ValueEnum<>("mode", FluidMode.class, FluidMode.WATER_DROP);

    /* Common */
    public final ValueFloat flowSpeed = new ValueFloat("flowSpeed", 1F);
    public final ValueFloat turbulence = new ValueFloat("turbulence", 0.1F);
    public final ValueColor color = new ValueColor("color", new Color(0.0f, 0.5f, 1.0f, 0.8f));
    public final ValueLink texture = new ValueLink("texture", LinkUtils.create("minecraft:textures/block/water_still.png"));
    public final ValueFloat physicsSensitivity = new ValueFloat("physicsSensitivity", 1F);
    public final ValueInt subdivisions = new ValueInt("subdivisions", 1, 1, 8);
    public final ValueBoolean smoothShading = new ValueBoolean("smoothShading", true);
    public final ValueBoolean debug = new ValueBoolean("debug", false);

    /* Full Ocean */
    public final ValueFloat sizeX = new ValueFloat("sizeX", 10F);
    public final ValueFloat sizeY = new ValueFloat("sizeY", 1F);
    public final ValueFloat sizeZ = new ValueFloat("sizeZ", 10F);
    public final ValueBoolean fillBlock = new ValueBoolean("fillBlock", false);
    public final ValueFloat waveAmplitude = new ValueFloat("waveAmplitude", 0.5F);
    public final ValueFloat waveFrequency = new ValueFloat("waveFrequency", 1F);

    /* Water Drop */
    public final ValueFloat surfaceTension = new ValueFloat("surfaceTension", 0.5F);
    public final ValueFloat dropSize = new ValueFloat("dropSize", 1F);
    public final ValueFloat viscosity = new ValueFloat("viscosity", 0.5F);

    public FluidForm()
    {
        super();

        this.add(this.mode);
        this.add(this.flowSpeed);
        this.add(this.turbulence);
        this.add(this.color);
        this.registerColorOverlays();
        this.opacity.set(0.8F);
        this.add(this.texture);
        this.add(this.physicsSensitivity);
        this.add(this.subdivisions);
        this.add(this.smoothShading);
        this.add(this.debug);

        this.add(this.sizeX);
        this.add(this.sizeY);
        this.add(this.sizeZ);
        this.add(this.fillBlock);
        this.add(this.waveAmplitude);
        this.add(this.waveFrequency);

        this.add(this.surfaceTension);
        this.add(this.dropSize);
        this.add(this.viscosity);
    }
}
