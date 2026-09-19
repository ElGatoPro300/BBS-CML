package mchorse.bbs_mod.cubic.animation;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.utils.MathUtils;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;

/**
 * Vanilla third-person item-use arm poses for procedural player models.
 * Angles are BBS degrees (positive X = arm forward), matching shield / item-hold.
 */
public final class ProceduralItemUsePoses
{
    public static boolean applyModel(IEntity target, ModelGroup left, ModelGroup right, ItemStack main, ItemStack offhand, float pitchDeg, float yawDeg, float transition)
    {
        return apply(target, wrap(left), wrap(right), main, offhand, pitchDeg, yawDeg, transition);
    }

    public static boolean applyBobj(IEntity target, BOBJBone left, BOBJBone right, ItemStack main, ItemStack offhand, float pitchDeg, float yawDeg, float transition)
    {
        return apply(target, wrap(left), wrap(right), main, offhand, pitchDeg, yawDeg, transition);
    }

    public static boolean apply(IEntity target, ArmRotations left, ArmRotations right, ItemStack main, ItemStack offhand, float pitchDeg, float yawDeg, float transition)
    {
        if (target == null || left == null || right == null)
        {
            return false;
        }

        /* Shield / swim / elytra already posed this frame — leave them alone. */
        if (target.isBlocking() || target.isFallFlying() || target.isSwimming() || target.isUsingRiptide())
        {
            return false;
        }

        boolean rightHand = target.getActiveHand() != InteractionHand.OFF_HAND;
        ItemStack active = rightHand ? main : offhand;

        if (target.isUsingItem() && active != null && !active.isEmpty())
        {
            return applyUseAction(target, left, right, active, rightHand, pitchDeg, yawDeg, transition);
        }

        /* Charged hold only while idle so walk + swipe stay identical. */
        if (target.getHandSwingProgress(transition) > 0F)
        {
            return false;
        }

        if (isChargedCrossbow(main))
        {
            applyCrossbowHold(left, right, pitchDeg, yawDeg, true);

            return true;
        }

        if (isChargedCrossbow(offhand))
        {
            applyCrossbowHold(left, right, pitchDeg, yawDeg, false);

            return true;
        }

        return false;
    }

    private static boolean applyUseAction(IEntity target, ArmRotations left, ArmRotations right, ItemStack active, boolean rightHand, float pitchDeg, float yawDeg, float transition)
    {
        ItemUseAnimation action = active.getUseAnimation();

        if (action == ItemUseAnimation.BOW)
        {
            applyBow(left, right, target, pitchDeg, yawDeg, rightHand, transition);
        }
        else if (action == ItemUseAnimation.CROSSBOW)
        {
            applyCrossbowCharge(target, left, right, active, rightHand, transition);
        }
        else if (action == ItemUseAnimation.SPEAR)
        {
            applySpear(activeArm(left, right, rightHand), target, rightHand, transition);
        }
        else if (action == ItemUseAnimation.SPYGLASS)
        {
            applySpyglass(activeArm(left, right, rightHand), pitchDeg, yawDeg, rightHand, target.isSneaking());
        }
        else if (action == ItemUseAnimation.TOOT_HORN)
        {
            applyHorn(activeArm(left, right, rightHand), pitchDeg, yawDeg, rightHand);
        }
        else if (action == ItemUseAnimation.BRUSH)
        {
            applyBrush(target, activeArm(left, right, rightHand), active, rightHand, transition);
        }
        else if (action == ItemUseAnimation.EAT || action == ItemUseAnimation.DRINK)
        {
            if (!BBSSettings.shouldAnimateEatingArm())
            {
                return false;
            }

            applyConsume(target, activeArm(left, right, rightHand), active, rightHand, transition);
        }
        else
        {
            return false;
        }

        return true;
    }

    private static void applyBow(ArmRotations left, ArmRotations right, IEntity target, float pitchDeg, float yawDeg, boolean rightHand, float transition)
    {
        float pitch = 90F - pitchDeg;
        float pull = rightHand ? 22.918F : 0F;
        float other = rightHand ? 0F : 22.918F;
        float age = target.getAge() + transition;

        /* Vanilla swingArm runs after BOW_AND_ARROW on both arms (pitch + roll). */
        right.set(pitch + idlePitchX(true, age), 5.73F - yawDeg + other, 0F);
        left.set(pitch + idlePitchX(false, age), -5.73F - yawDeg - pull, 0F);
    }

    private static void applyCrossbowHold(ArmRotations left, ArmRotations right, float pitchDeg, float yawDeg, boolean rightHand)
    {
        float holdingYaw = (rightHand ? 17.189F : -17.189F) - yawDeg;
        float otherYaw = (rightHand ? -34.377F : 34.377F) - yawDeg;
        float holdingPitch = 90F - pitchDeg - 5.73F;
        float otherPitch = 85.944F - pitchDeg;

        if (rightHand)
        {
            right.set(holdingPitch, holdingYaw, 0F);
            left.set(otherPitch, otherYaw, 0F);
        }
        else
        {
            left.set(holdingPitch, holdingYaw, 0F);
            right.set(otherPitch, otherYaw, 0F);
        }
    }

    private static void applyCrossbowCharge(IEntity target, ArmRotations left, ArmRotations right, ItemStack stack, boolean rightHand, float transition)
    {
        float pull = crossbowPull(target, stack, transition);
        float holdingYaw = rightHand ? 45.837F : -45.837F;
        float holdingPitch = 55.62F;
        float pullingYaw = Mth.lerp(pull, 22.918F, 48.701F) * (rightHand ? -1F : 1F);
        float pullingPitch = Mth.lerp(pull, holdingPitch, 90F);

        if (rightHand)
        {
            right.set(holdingPitch, holdingYaw, 0F);
            left.set(pullingPitch, pullingYaw, 0F);
        }
        else
        {
            left.set(holdingPitch, holdingYaw, 0F);
            right.set(pullingPitch, pullingYaw, 0F);
        }
    }

    private static void applySpear(ArmRotations arm, IEntity target, boolean rightHand, float transition)
    {
        /* Vanilla applies CrossbowPosing.swingArm after THROW_SPEAR, so idle
         * pitch (X) and roll (Z) both remain. Z is already on the arm from walk. */
        float age = target.getAge() + transition;

        arm.set(180F + idlePitchX(rightHand, age), 0F, 0F);
    }

    private static void applySpyglass(ArmRotations arm, float pitchDeg, float yawDeg, boolean rightHand, boolean sneaking)
    {
        /* Vanilla BipedEntityModel (radians):
         *   arm.pitch = clamp(head.pitch - 1.9198622 - sneak15°, -2.4, 3.3)
         *   arm.yaw   = head.yaw ∓ 0.2617994   (right subtracts, left adds)
         * BBS arm X is opposite ModelPart pitch (positive X = arm forward). */
        float sneak = sneaking ? 15F : 0F;
        float vanillaArmPitchDeg = Mth.clamp(pitchDeg - 110F - sneak, -137.5F, 189F);
        float yawOffset = rightHand ? 15F : -15F;

        /* Vanilla skips swingArm for SPYGLASS — no idle on that arm. */
        arm.lock(-vanillaArmPitchDeg, yawOffset - yawDeg, 0F);
    }

    private static void applyHorn(ArmRotations arm, float pitchDeg, float yawDeg, boolean rightHand)
    {
        float pitch = 85F - Mth.clamp(pitchDeg, -68.75F, 68.75F);

        arm.set(pitch, (rightHand ? 30F : -30F) - yawDeg, 0F);
    }

    private static void applyBrush(IEntity target, ArmRotations arm, ItemStack stack, boolean rightHand, float transition)
    {
        float stroke = Mth.sin((elapsedUse(target, stack) + transition) * 1.2F) * 22F;

        arm.set(36F + stroke, rightHand ? 10F : -10F, rightHand ? 25F : -25F);
    }

    private static void applyConsume(IEntity target, ArmRotations arm, ItemStack stack, boolean rightHand, float transition)
    {
        float chew = Mth.sin((elapsedUse(target, stack) + transition) * 1.8F) * 8F;

        arm.set(72F + chew, rightHand ? 22F : -22F, rightHand ? 40F : -40F);
    }

    private static ArmRotations activeArm(ArmRotations left, ArmRotations right, boolean rightHand)
    {
        return rightHand ? right : left;
    }

    private static float idlePitchX(boolean rightHand, float age)
    {
        float sigma = rightHand ? 1F : -1F;

        return MathUtils.toDeg(sigma * Mth.sin(-age * 0.067F) * 0.05F);
    }

    private static ArmRotations wrap(ModelGroup group)
    {
        return group == null ? null : new ModelArm(group);
    }

    private static ArmRotations wrap(BOBJBone bone)
    {
        return bone == null ? null : new BobjArm(bone);
    }

    private static boolean isChargedCrossbow(ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }

    private static float crossbowPull(IEntity target, ItemStack stack, float transition)
    {
        float pullTime = 25F;

        if (target instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living)
        {
            pullTime = Math.max(1F, CrossbowItem.getChargeDuration(stack, living));
        }

        return Mth.clamp((elapsedUse(target, stack) + transition) / pullTime, 0F, 1F);
    }

    /**
     * {@link StubEntity} stores elapsed use ticks in {@code itemUseTimeLeft};
     * living entities store remaining ticks.
     */
    private static float elapsedUse(IEntity target, ItemStack stack)
    {
        int stored = target.getItemUseTimeLeft();

        if (target instanceof StubEntity)
        {
            return Math.max(0, stored);
        }

        int max = 20;

        if (target instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living)
        {
            max = Math.max(1, stack.getUseDuration(living));
        }

        return Math.max(0, max - stored);
    }

    private ProceduralItemUsePoses()
    {}

    public interface ArmRotations
    {
        public void set(float xDeg, float yDeg, float zDeg);

        public void lock(float xDeg, float yDeg, float zDeg);
    }

    /**
     * Vanilla item poses overwrite pitch/yaw and leave arm roll, so idle Z stays.
     * {@link #lock} also replaces roll (spyglass skips idle entirely).
     */
    private static class ModelArm implements ArmRotations
    {
        private final ModelGroup group;

        private ModelArm(ModelGroup group)
        {
            this.group = group;
        }

        @Override
        public void set(float xDeg, float yDeg, float zDeg)
        {
            this.group.current.rotate.x = xDeg;
            this.group.current.rotate.y = yDeg;
            this.group.current.rotate.z += zDeg;
        }

        @Override
        public void lock(float xDeg, float yDeg, float zDeg)
        {
            this.group.current.rotate.set(xDeg, yDeg, zDeg);
        }
    }

    private static class BobjArm implements ArmRotations
    {
        private final BOBJBone bone;

        private BobjArm(BOBJBone bone)
        {
            this.bone = bone;
        }

        @Override
        public void set(float xDeg, float yDeg, float zDeg)
        {
            this.bone.transform.rotate.x = MathUtils.toRad(xDeg);
            this.bone.transform.rotate.y = MathUtils.toRad(yDeg);
            this.bone.transform.rotate.z += MathUtils.toRad(zDeg);
        }

        @Override
        public void lock(float xDeg, float yDeg, float zDeg)
        {
            this.bone.transform.rotate.set(MathUtils.toRad(xDeg), MathUtils.toRad(yDeg), MathUtils.toRad(zDeg));
        }
    }
}
