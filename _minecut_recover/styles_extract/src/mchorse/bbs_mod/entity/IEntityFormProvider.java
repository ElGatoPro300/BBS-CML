package mchorse.bbs_mod.entity;

import mchorse.bbs_mod.forms.forms.Form;

public interface IEntityFormProvider
{
    public int getEntityId();

    public Form getForm();

    public void setForm(Form form);
}