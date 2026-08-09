package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.mc.ValueItemStack;
import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

import org.joml.Vector4f;

/**
 * Static property values used when {@link HotbarClip#useKeyframes} is disabled.
 * Kept separate from keyframe channels so animation data is never destroyed.
 */
public class HotbarUniform extends ValueGroup
{
    public final ValueVector4f layout = new ValueVector4f("layout", new Vector4f(0F, 0F, 1F, 0F));
    public final ValueInt selectedSlot = new ValueInt("selected_slot", 0);
    public final ValueItemStack slot0 = new ValueItemStack("slot_0");
    public final ValueItemStack slot1 = new ValueItemStack("slot_1");
    public final ValueItemStack slot2 = new ValueItemStack("slot_2");
    public final ValueItemStack slot3 = new ValueItemStack("slot_3");
    public final ValueItemStack slot4 = new ValueItemStack("slot_4");
    public final ValueItemStack slot5 = new ValueItemStack("slot_5");
    public final ValueItemStack slot6 = new ValueItemStack("slot_6");
    public final ValueItemStack slot7 = new ValueItemStack("slot_7");
    public final ValueItemStack slot8 = new ValueItemStack("slot_8");
    public final ValueItemStack offhandSlot = new ValueItemStack("offhand_slot");
    public final ValueBoolean rightOffhand = new ValueBoolean("right_offhand", false);
    public final ValueDouble health = new ValueDouble("health", 20D);
    public final ValueDouble healthContainer = new ValueDouble("health_container", 20D);
    public final ValueDouble absorption = new ValueDouble("absorption", 0D);
    public final ValueDouble absorptionContainer = new ValueDouble("absorption_container", 0D);
    public final ValueInt heartType = new ValueInt("heart_type", HotbarState.HEART_NORMAL);
    public final ValueBoolean hardcore = new ValueBoolean("hardcore", false);
    public final ValueBoolean heartRegeneration = new ValueBoolean("heart_regeneration", false);
    public final ValueDouble armor = new ValueDouble("armor", 0D);
    public final ValueDouble hunger = new ValueDouble("hunger", 20D);
    public final ValueBoolean hungerEffect = new ValueBoolean("hunger_effect", false);
    public final ValueDouble air = new ValueDouble("air", 300D);
    public final ValueDouble experience = new ValueDouble("experience", 0D);
    public final ValueInt experienceLevel = new ValueInt("experience_level", 0);
    public final ValueDouble mountHealth = new ValueDouble("mount_health", 0D);
    public final ValueDouble mountHealthContainer = new ValueDouble("mount_health_container", 0D);
    public final ValueDouble horseJump = new ValueDouble("horse_jump", 0D);
    public final ValueBoolean showHorseJump = new ValueBoolean("show_horse_jump", false);
    public final ValueDouble attackCooldown = new ValueDouble("attack_cooldown", 0D);
    public final ValueBoolean showAttackCooldown = new ValueBoolean("show_attack_cooldown", false);
    public final ValueBoolean showHotbar = new ValueBoolean("show_hotbar", true);
    public final ValueBoolean showHealth = new ValueBoolean("show_health", true);
    public final ValueBoolean showArmor = new ValueBoolean("show_armor", true);
    public final ValueBoolean showHunger = new ValueBoolean("show_hunger", true);
    public final ValueBoolean showAir = new ValueBoolean("show_air", true);
    public final ValueBoolean showExperience = new ValueBoolean("show_experience", true);

    public HotbarUniform(String id)
    {
        super(id);

        this.add(this.layout);
        this.add(this.selectedSlot);
        this.add(this.slot0);
        this.add(this.slot1);
        this.add(this.slot2);
        this.add(this.slot3);
        this.add(this.slot4);
        this.add(this.slot5);
        this.add(this.slot6);
        this.add(this.slot7);
        this.add(this.slot8);
        this.add(this.offhandSlot);
        this.add(this.rightOffhand);
        this.add(this.health);
        this.add(this.healthContainer);
        this.add(this.absorption);
        this.add(this.absorptionContainer);
        this.add(this.heartType);
        this.add(this.hardcore);
        this.add(this.heartRegeneration);
        this.add(this.armor);
        this.add(this.hunger);
        this.add(this.hungerEffect);
        this.add(this.air);
        this.add(this.experience);
        this.add(this.experienceLevel);
        this.add(this.mountHealth);
        this.add(this.mountHealthContainer);
        this.add(this.horseJump);
        this.add(this.showHorseJump);
        this.add(this.attackCooldown);
        this.add(this.showAttackCooldown);
        this.add(this.showHotbar);
        this.add(this.showHealth);
        this.add(this.showArmor);
        this.add(this.showHunger);
        this.add(this.showAir);
        this.add(this.showExperience);
    }
}
