package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.factory.MapFactory;

public class FormArchitect extends MapFactory<Form, Void>
{
    @Override
    public String getTypeKey()
    {
        return "id";
    }

    public boolean has(MapType data)
    {
        if (data.has(this.getTypeKey()))
        {
            Link id = Link.create(data.getString(this.getTypeKey()));

            return this.factory.containsKey(id);
        }

        return false;
    }
}