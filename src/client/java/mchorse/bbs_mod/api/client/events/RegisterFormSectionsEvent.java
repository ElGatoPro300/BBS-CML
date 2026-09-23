package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.sections.FormSection;

import java.util.function.Function;

/**
 * Posted on the client before the form palette sections are built.
 */
public class RegisterFormSectionsEvent
{
    public void register(Function<FormCategories, FormSection> factory)
    {
        FormCategories.registerSection(factory);
    }
}
