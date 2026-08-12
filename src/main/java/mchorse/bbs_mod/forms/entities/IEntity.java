package mchorse.bbs_mod.forms.entities;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.AABB;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Interface that provides access to an "Entity" within forms for rendering
 * and updating.
 */
public interface IEntity
{
    public void setWorld(Level world);

    public Level getWorld();

    public Form getForm();

    public void setForm(Form form);

    public ItemStack getEquipmentStack(EquipmentSlot slot);

    public void setEquipmentStack(EquipmentSlot slot, ItemStack stack);

    public int getSelectedSlot();

    public boolean isSneaking();

    public void setSneaking(boolean sneaking);

    public boolean isSprinting();

    public void setSprinting(boolean sprinting);

    public boolean isOnGround();

    public void setOnGround(boolean ground);

    public void swingArm();

    public float getHandSwingProgress(float tickDelta);

    public int getAge();

    public void setAge(int ticks);

    public float getFallDistance();

    public void setFallDistance(float fallDistance);

    public int getHurtTimer();

    public void setHurtTimer(int hurtTimer);

    public int getDeathTime();

    public void setDeathTime(int deathTime);

    public boolean isUsingItem();

    public void setUsingItem(boolean usingItem);

    public int getItemUseTimeLeft();

    public void setItemUseTimeLeft(int itemUseTimeLeft);

    public int getFireTicks();

    public void setFireTicks(int fireTicks);

    public boolean isParticlesEnabled();

    public void setParticlesEnabled(boolean particlesEnabled);

    public InteractionHand getActiveHand();

    public void setActiveHand(InteractionHand hand);

    public double getX();

    public double getPrevX();

    public void setPrevX(double x);

    public double getY();

    public double getPrevY();

    public void setPrevY(double y);

    public double getZ();

    public double getPrevZ();

    public void setPrevZ(double z);

    public void setPosition(double x, double y, double z);

    public double getEyeHeight();

    public Vec3 getVelocity();

    public void setVelocity(float x, float y, float z);

    public float getYaw();

    public float getPrevYaw();

    public void setYaw(float yaw);

    public void setPrevYaw(float prevYaw);

    public float getHeadYaw();

    public float getPrevHeadYaw();

    public void setHeadYaw(float headYaw);

    public void setPrevHeadYaw(float prevHeadYaw);

    public float getPitch();

    public float getPrevPitch();

    public void setPitch(float pitch);

    public void setPrevPitch(float prevPitch);

    public float getBodyYaw();

    public float getPrevBodyYaw();

    public float getPrevPrevBodyYaw();

    public void setBodyYaw(float bodyYaw);

    public void setPrevBodyYaw(float prevBodyYaw);

    public void setPrevPrevBodyYaw(float prevPrevBodyYaw);

    public float[] getExtraVariables();

    public float[] getPrevExtraVariables();

    public AABB getPickingHitbox();

    public void update();

    public default void copy(IEntity entity)
    {
        this.setForm(entity.getForm());

        this.setSneaking(entity.isSneaking());
        this.setSprinting(entity.isSprinting());
        this.setSwimming(entity.isSwimming());
        this.setFlying(entity.isFlying());
        this.setFallFlying(entity.isFallFlying());
        this.setCrawling(entity.isCrawling());
        this.setClimbing(entity.isClimbing());
        this.setBlocking(entity.isBlocking());
        this.setSleeping(entity.isSleeping());
        this.setRiptide(entity.isUsingRiptide());
        this.setOnGround(entity.isOnGround());
        this.setFallDistance(entity.getFallDistance());
        this.setHurtTimer(entity.getHurtTimer());
        this.setDeathTime(entity.getDeathTime());
        this.setUsingItem(entity.isUsingItem());
        this.setItemUseTimeLeft(entity.getItemUseTimeLeft());
        this.setFireTicks(entity.getFireTicks());
        this.setParticlesEnabled(entity.isParticlesEnabled());
        this.setActiveHand(entity.getActiveHand());

        this.setPrevX(entity.getPrevX());
        this.setPrevY(entity.getPrevY());
        this.setPrevZ(entity.getPrevZ());
        this.setPosition(entity.getX(), entity.getY(), entity.getZ());

        this.setPrevYaw(entity.getPrevYaw());
        this.setPrevHeadYaw(entity.getPrevHeadYaw());
        this.setPrevPitch(entity.getPrevPitch());
        this.setPrevBodyYaw(entity.getPrevBodyYaw());
        this.setPrevPrevBodyYaw(entity.getPrevPrevBodyYaw());

        this.setYaw(entity.getYaw());
        this.setHeadYaw(entity.getHeadYaw());
        this.setPitch(entity.getPitch());
        this.setBodyYaw(entity.getBodyYaw());

        this.setVelocity((float) entity.getVelocity().x, (float) entity.getVelocity().y, (float) entity.getVelocity().z);

        float[] extraVariables = this.getExtraVariables();
        float[] prevExtraVariables = this.getPrevExtraVariables();

        for (int i = 0; i < extraVariables.length; i++)
        {
            extraVariables[i] = entity.getExtraVariables()[i];
            prevExtraVariables[i] = entity.getPrevExtraVariables()[i];
        }
    }

    public WalkAnimationState getLimbAnimator();

    public float getLimbPos(float tickDelta);

    public float getLimbSpeed(float tickDelta);

    /* Swimming & Flight */

    public boolean isSwimming();

    public void setSwimming(boolean swimming);

    public boolean isFlying();

    public void setFlying(boolean flying);

    public float getLeaningPitch(float tickDelta);

    public boolean isTouchingWater();

    public Pose getEntityPose();

    public int getRoll();

    public boolean isFallFlying();

    public void setFallFlying(boolean fallFlying);

    public float getFallFlyingProgress(float transition);

    public Vec3 getRotationVec(float transition);

    public Vec3 lerpVelocity(float transition);

    public boolean isUsingRiptide();

    public void setRiptide(boolean riptide);

    public boolean isCrawling();

    public void setCrawling(boolean crawling);

    public boolean isClimbing();

    public void setClimbing(boolean climbing);

    public boolean isBlocking();

    public void setBlocking(boolean blocking);

    public boolean isSleeping();

    public void setSleeping(boolean sleeping);
    
    public IEntity getMountTarget();

    public void setMountTarget(IEntity mountTarget);

    public IEntity getRiderTarget();

    public void setRiderTarget(IEntity riderTarget);

    public boolean isSitting();

    public void setSitting(boolean sitting);

    public default boolean isRiding()
    {
        return this.getMountTarget() != null;
    }
}