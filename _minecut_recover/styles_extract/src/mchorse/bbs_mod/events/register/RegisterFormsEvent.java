package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.forms.FormArchitect;

public class RegisterFormsEvent
{
    private final FormArchitect forms;

    public RegisterFormsEvent(FormArchitect forms)
    {
        this.forms = forms;
    }

    public FormArchitect getForms()
    {
        return this.forms;
    }
}
