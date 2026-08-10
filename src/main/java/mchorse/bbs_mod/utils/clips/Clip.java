package mchorse.bbs_mod.utils.clips;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public abstract class Clip extends ValueGroup
{
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString title = new ValueString("title", "");
    public final ValueInt layer = new ValueInt("layer", 0, 0, Integer.MAX_VALUE);
    /** Film time in ticks (float). Sub-tick values (e.g. 1.5) are valid for action/command clips. */
    public final ValueFloat tick = new ValueFloat("tick", 0F, 0F, Float.MAX_VALUE);
    public final ValueInt duration = new ValueInt("duration", 1, 1, Integer.MAX_VALUE);
    public final Envelope envelope = new Envelope("envelope");

    public Clip()
    {
        super("");

        this.add(this.enabled);
        this.add(this.title);
        this.add(this.layer);
        this.add(this.tick);
        this.add(this.duration);
        this.add(this.envelope);
    }

    public boolean isGlobal()
    {
        return false;
    }

    public boolean isInside(int tick)
    {
        return this.isInside((float) tick);
    }

    public boolean isInside(float tick)
    {
        float offset = this.tick.get();

        return tick >= offset && tick < offset + this.duration.get();
    }

    /** Rounded film tick for APIs that still use integer timeline units. */
    public int getTickInt()
    {
        return Math.round(this.tick.get());
    }

    public void shift(double dx, double dy, double dz)
    {}

    public void shiftLeft(int tick)
    {}

    public Clip copy()
    {
        Clip clip = this.create();

        clip.copy(this);

        return clip;
    }

    protected abstract Clip create();

    /**
     * Breakdown this fixture into another piece starting at given offset
     */
    public Clip breakDown(int offset)
    {
        int duration = this.duration.get();

        if (offset <= 0 || offset >= duration)
        {
            return null;
        }

        Clip clip = this.copy();

        clip.duration.set(duration - offset);
        clip.breakDownClip(this, offset);

        return clip;
    }

    protected void breakDownClip(Clip original, int offset)
    {
        this.envelope.breakDown(original, offset);
    }
}