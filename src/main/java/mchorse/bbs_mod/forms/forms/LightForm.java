package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class LightForm extends Form
{
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueInt level = new ValueInt("level", 15);

    public LightForm()
    {
        super();

        this.add(this.enabled);
        this.add(this.level);
    }
}

