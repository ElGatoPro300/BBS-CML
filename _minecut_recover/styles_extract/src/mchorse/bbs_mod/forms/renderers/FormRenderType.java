package mchorse.bbs_mod.forms.renderers;

import net.minecraft.class_811;

public enum FormRenderType
{
    MODEL_BLOCK, ENTITY, ITEM_FP, ITEM_TP, ITEM_INVENTORY, ITEM, PREVIEW;

    public static FormRenderType fromModelMode(class_811 mode)
    {
        if (mode.method_29998())
        {
            return ITEM_FP;
        }
        else if (mode == class_811.field_4323 || mode == class_811.field_4320)
        {
            return ITEM_TP;
        }
        else if (mode == class_811.field_4318)
        {
            return ITEM;
        }
        else if (mode == class_811.field_4317)
        {
            return ITEM_INVENTORY;
        }

        return ENTITY;
    }
}