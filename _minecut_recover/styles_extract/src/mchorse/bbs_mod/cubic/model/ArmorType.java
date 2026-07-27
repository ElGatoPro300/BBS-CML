package mchorse.bbs_mod.cubic.model;

import net.minecraft.class_1304;

public enum ArmorType
{
    HELMET(class_1304.field_6169), CHEST(class_1304.field_6174), LEGGINGS(class_1304.field_6172), LEFT_ARM(class_1304.field_6174), RIGHT_ARM(class_1304.field_6174), LEFT_LEG(class_1304.field_6172), RIGHT_LEG(class_1304.field_6172), LEFT_BOOT(class_1304.field_6166), RIGHT_BOOT(class_1304.field_6166);

    public final class_1304 slot;

    ArmorType(class_1304 slot)
    {
        this.slot = slot;
    }
}