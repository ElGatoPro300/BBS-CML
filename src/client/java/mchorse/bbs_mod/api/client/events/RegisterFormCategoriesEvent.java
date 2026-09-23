package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.forms.FormCategories;

public class RegisterFormCategoriesEvent
{
    private final FormCategories categories;

    public RegisterFormCategoriesEvent(FormCategories categories)
    {
        this.categories = categories;
    }

    public FormCategories getCategories()
    {
        return this.categories;
    }
}
