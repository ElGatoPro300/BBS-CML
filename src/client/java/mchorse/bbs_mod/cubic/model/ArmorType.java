package mchorse.bbs_mod.cubic.model;

import net.minecraft.entity.EquipmentSlot;

public enum ArmorType
{
    HELMET(EquipmentSlot.HEAD), CHEST(EquipmentSlot.CHEST), LEGGINGS(EquipmentSlot.LEGS), LEFT_ARM(EquipmentSlot.CHEST), RIGHT_ARM(EquipmentSlot.CHEST), LEFT_LEG(EquipmentSlot.LEGS), RIGHT_LEG(EquipmentSlot.LEGS), LEFT_BOOT(EquipmentSlot.FEET), RIGHT_BOOT(EquipmentSlot.FEET);

    public final EquipmentSlot slot;

    ArmorType(EquipmentSlot slot)
    {
        this.slot = slot;
    }
}