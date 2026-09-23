package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.resources.Link;

/**
 * Posted on both sides once BBS has registered its built-in form types.
 */
public class RegisterFormsEvent
{
    public final FormArchitect forms;

    public RegisterFormsEvent(FormArchitect forms)
    {
        this.forms = forms;
    }

    public RegisterFormsEvent register(Link id, Class<? extends Form> clazz)
    {
        this.forms.register(id, clazz, null);

        return this;
    }
}
