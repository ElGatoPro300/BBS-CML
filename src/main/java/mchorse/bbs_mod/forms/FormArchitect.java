package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.factory.MapFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FormArchitect extends MapFactory<Form, Void>
{
    private static final List<Consumer<Form>> MODIFIERS = new ArrayList<>();

    public static void registerModifier(Consumer<Form> modifier)
    {
        MODIFIERS.add(modifier);
    }

    @Override
    public String getTypeKey()
    {
        return "id";
    }

    @Override
    public Form create(Link type)
    {
        Form form = super.create(type);

        if (form != null)
        {
            for (int i = 0; i < MODIFIERS.size(); i++)
            {
                MODIFIERS.get(i).accept(form);
            }
        }

        return form;
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
