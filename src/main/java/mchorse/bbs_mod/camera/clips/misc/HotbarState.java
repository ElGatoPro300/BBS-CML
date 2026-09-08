package mchorse.bbs_mod.camera.clips.misc;

import net.minecraft.world.item.ItemStack;

public class HotbarState
{
    public static final int HEART_NORMAL = 0;
    public static final int HEART_POISONED = 1;
    public static final int HEART_WITHERED = 2;
    public static final int HEART_ABSORBING = 3;
    public static final int HEART_FROZEN = 4;

    public final ItemStack[] items = new ItemStack[9];
    public ItemStack offhandItem = ItemStack.EMPTY;
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
    public boolean rightOffhand;
    public boolean showHotbar = true;
    public boolean showHealth = true;
    public boolean showArmor = true;
    public boolean showHunger = true;
    public boolean showAir = true;
    public boolean showExperience = true;
    public float mountHealth;
    public float mountHealthContainer;
    public float horseJump;
    public boolean showHorseJump;
    public float attackCooldown;
    public boolean showAttackCooldown;
    public float x;
    public float y;
    public float scale;
    public float alpha;
    public int renderOrder;
}
