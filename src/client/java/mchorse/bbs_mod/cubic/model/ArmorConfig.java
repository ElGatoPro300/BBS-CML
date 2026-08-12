package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.settings.values.core.ValueGroup;

import java.util.HashMap;
import java.util.Map;

public class ArmorConfig extends ValueGroup
{
    public final ArmorSlot helmet = new ArmorSlot("helmet");
    public final ArmorSlot chest = new ArmorSlot("chest");
    public final ArmorSlot leggings = new ArmorSlot("leggings");
    public final ArmorSlot leftArm = new ArmorSlot("left_arm");
    public final ArmorSlot rightArm = new ArmorSlot("right_arm");
    public final ArmorSlot leftLeg = new ArmorSlot("left_leg");
    public final ArmorSlot rightLeg = new ArmorSlot("right_leg");
    public final ArmorSlot leftBoot = new ArmorSlot("left_boot");
    public final ArmorSlot rightBoot = new ArmorSlot("right_boot");

    private final Map<ArmorType, ArmorSlot> slots = new HashMap<>();

    public ArmorConfig(String id)
    {
        super(id);

        this.add(this.helmet);
        this.add(this.chest);
        this.add(this.leggings);
        this.add(this.leftArm);
        this.add(this.rightArm);
        this.add(this.leftLeg);
        this.add(this.rightLeg);
        this.add(this.leftBoot);
        this.add(this.rightBoot);

        this.slots.put(ArmorType.HELMET, this.helmet);
        this.slots.put(ArmorType.CHEST, this.chest);
        this.slots.put(ArmorType.LEGGINGS, this.leggings);
        this.slots.put(ArmorType.LEFT_ARM, this.leftArm);
        this.slots.put(ArmorType.RIGHT_ARM, this.rightArm);
        this.slots.put(ArmorType.LEFT_LEG, this.leftLeg);
        this.slots.put(ArmorType.RIGHT_LEG, this.rightLeg);
        this.slots.put(ArmorType.LEFT_BOOT, this.leftBoot);
        this.slots.put(ArmorType.RIGHT_BOOT, this.rightBoot);
    }

    public ArmorSlot get(ArmorType type)
    {
        return this.slots.get(type);
    }
}
