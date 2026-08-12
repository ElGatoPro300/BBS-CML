package mchorse.bbs_mod.film;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class FilmContributor extends ValueGroup
{
    public final ValueString name = new ValueString("name", "");
    public final ValueInt time = new ValueInt("time", 0);

    public FilmContributor(String id)
    {
        super(id);

        this.add(this.name);
        this.add(this.time);
    }
}
