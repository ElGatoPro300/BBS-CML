package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.forms.Form;

import java.util.function.Consumer;

/**
 * Posted on both sides right after forms are registered, allowing addons to attach custom
 * {@link mchorse.bbs_mod.settings.values.base.BaseValue} properties to any created form.
 */
public class RegisterFormModifiersEvent
{
    public void register(Consumer<Form> modifier)
    {
        FormArchitect.registerModifier(modifier);
    }
}
