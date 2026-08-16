package mchorse.bbs_mod.triggers;

import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class Trigger extends ValueGroup
{
    public final ValueString type = new ValueString("type", "command");
    public final ValueString command = new ValueString("command", "");
    public final ValueForm form = new ValueForm("form");
    public final ValueInt x = new ValueInt("x", 0);
    public final ValueInt y = new ValueInt("y", 0);
    public final ValueInt z = new ValueInt("z", 0);
    public final ValueForm blockForm = new ValueForm("block_form");
    public final ValueString film = new ValueString("film", "");
    public final ValueBoolean playCamera = new ValueBoolean("play_camera", true);

    public Trigger(String id)
    {
        super(id);
        
        this.add(this.type);
        this.add(this.command);
        this.add(this.form);
        this.add(this.x);
        this.add(this.y);
        this.add(this.z);
        this.add(this.blockForm);
        this.add(this.film);
        this.add(this.playCamera);
    }
}
