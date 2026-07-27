package mchorse.bbs_mod.camera.clips.misc;

import net.minecraft.class_1799;

public class HotbarState
{
    public static final int HEART_NORMAL = 0;
    public static final int HEART_POISONED = 1;
    public static final int HEART_WITHERED = 2;
    public static final int HEART_ABSORBING = 3;
    public static final int HEART_FROZEN = 4;

    public final class_1799[] items = new class_1799[9];
    public class_1799 offhandItem = class_1799.field_8037;
    public int selectedSlot;
    public int heartType;
    public boolean hardcore;
    public boolean heartRegeneration;
    public boolean hungerEffect;
    public float health;
    public float healthContainer;
    public float absorption;
    public float absorptionContainer;
    public float armor;
    public float hunger;
    public float air;
    public float experience;
    public int experienceLevel;
    public float x;
    public float y;
    public float scale;
    public float alpha;
    public int renderOrder;
}
