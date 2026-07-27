package mchorse.bbs_mod.forms.entities;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.AABB;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_243;
import net.minecraft.class_4050;
import net.minecraft.class_8080;

public class MCEntity implements IEntity
{
    private class_1297 mcEntity;

    private float prevPrevBodyYaw;
    private class_243 lastVelocity = class_243.field_1353;

    private float[] extraVariables = new float[10];
    private float[] prevExtraVariables = new float[10];
    private boolean particlesEnabled = true;
    private IEntity mountTarget;
    private IEntity riderTarget;
    private boolean sitting;

    public MCEntity(class_1297 mcEntity)
    {
        this.mcEntity = mcEntity;
    }

    public class_1297 getMcEntity()
    {
        return this.mcEntity;
    }

    @Override
    public void setWorld(class_1937 world)
    {}

    @Override
    public class_1937 getWorld()
    {
        return this.mcEntity.method_37908();
    }

    @Override
    public Form getForm()
    {
        Morph morph = Morph.getMorph(this.mcEntity);

        return morph == null ? null : morph.getForm();
    }

    @Override
    public void setForm(Form form)
    {
        Morph morph = Morph.getMorph(this.mcEntity);

        if (morph != null)
        {
            morph.setForm(form);
        }
    }

    @Override
    public class_1799 getEquipmentStack(class_1304 slot)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6118(slot);
        }

        return class_1799.field_8037;
    }

    @Override
    public void setEquipmentStack(class_1304 slot, class_1799 stack)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            living.method_5673(slot, stack == null ? class_1799.field_8037 : stack);
        }
    }

    @Override
    public int getSelectedSlot()
    {
        if (this.mcEntity instanceof class_1657 player)
        {
            return player.method_31548().field_7545;
        }

        return 0;
    }

    @Override
    public boolean isSneaking()
    {
        return this.mcEntity.method_5715();
    }

    @Override
    public void setSneaking(boolean sneaking)
    {
        this.mcEntity.method_5660(sneaking);
    }

    @Override
    public boolean isSprinting()
    {
        return this.mcEntity.method_5624();
    }

    @Override
    public void setSprinting(boolean sprinting)
    {
        this.mcEntity.method_5728(sprinting);
    }

    @Override
    public boolean isOnGround()
    {
        return this.mcEntity.method_24828();
    }

    @Override
    public void setOnGround(boolean ground)
    {
        this.mcEntity.method_24830(ground);
    }

    @Override
    public void swingArm()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            living.method_6104(class_1268.field_5808);
        }
    }

    @Override
    public float getHandSwingProgress(float tickDelta)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6055(tickDelta);
        }

        return 0F;
    }

    @Override
    public int getAge()
    {
        return this.mcEntity.field_6012;
    }

    @Override
    public void setAge(int ticks)
    {
        this.mcEntity.field_6012 = ticks;
    }

    @Override
    public float getFallDistance()
    {
        return this.mcEntity.field_6017;
    }

    @Override
    public void setFallDistance(float fallDistance)
    {
        this.mcEntity.field_6017 = fallDistance;
    }

    @Override
    public int getHurtTimer()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_6235;
        }

        return 0;
    }

    @Override
    public void setHurtTimer(int hurtTimer)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            living.field_6235 = hurtTimer;
        }
    }

    @Override
    public int getDeathTime()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_6213;
        }

        return 0;
    }

    @Override
    public void setDeathTime(int deathTime)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            living.field_6213 = deathTime;
        }
    }

    @Override
    public boolean isUsingItem()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6115();
        }

        return false;
    }

    @Override
    public void setUsingItem(boolean usingItem)
    {
    }

    @Override
    public int getItemUseTimeLeft()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6014();
        }

        return 0;
    }

    @Override
    public void setItemUseTimeLeft(int itemUseTimeLeft)
    {
    }

    @Override
    public int getFireTicks()
    {
        return this.mcEntity.method_20802();
    }

    @Override
    public void setFireTicks(int fireTicks)
    {
        this.mcEntity.method_20803(fireTicks);
    }

    @Override
    public boolean isParticlesEnabled()
    {
        return this.particlesEnabled;
    }

    @Override
    public void setParticlesEnabled(boolean particlesEnabled)
    {
        this.particlesEnabled = particlesEnabled;
    }

    @Override
    public class_1268 getActiveHand()
    {
        if (this.mcEntity instanceof class_1309 living && living.method_6115())
        {
            return living.method_6058();
        }

        return class_1268.field_5808;
    }

    @Override
    public void setActiveHand(class_1268 hand)
    {
    }

    @Override
    public double getX()
    {
        return this.mcEntity.method_23317();
    }

    @Override
    public double getPrevX()
    {
        return this.mcEntity.field_6014;
    }

    @Override
    public void setPrevX(double x)
    {
        this.mcEntity.field_6014 = x;
    }

    @Override
    public double getY()
    {
        return this.mcEntity.method_23318();
    }

    @Override
    public double getPrevY()
    {
        return this.mcEntity.field_6036;
    }

    @Override
    public void setPrevY(double y)
    {
        this.mcEntity.field_6036 = y;
    }

    @Override
    public double getZ()
    {
        return this.mcEntity.method_23321();
    }

    @Override
    public double getPrevZ()
    {
        return this.mcEntity.field_5969;
    }

    @Override
    public void setPrevZ(double z)
    {
        this.mcEntity.field_5969 = z;
    }

    @Override
    public void setPosition(double x, double y, double z)
    {
        this.mcEntity.method_5814(x, y, z);
    }

    @Override
    public double getEyeHeight()
    {
        return this.mcEntity.method_18381(this.mcEntity.method_18376());
    }

    @Override
    public class_243 getVelocity()
    {
        return this.mcEntity.method_18798();
    }

    @Override
    public void setVelocity(float x, float y, float z)
    {
        this.mcEntity.method_18800(x, y, z);
    }

    @Override
    public float getYaw()
    {
        return this.mcEntity.method_36454();
    }

    @Override
    public float getPrevYaw()
    {
        return this.mcEntity.field_5982;
    }

    @Override
    public void setYaw(float yaw)
    {
        this.mcEntity.method_36456(yaw);
    }

    @Override
    public void setPrevYaw(float prevYaw)
    {
        this.mcEntity.field_5982 = prevYaw;
    }

    @Override
    public float getHeadYaw()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_5791();
        }

        return this.mcEntity.method_36454();
    }

    @Override
    public float getPrevHeadYaw()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_6259;
        }

        return this.mcEntity.field_5982;
    }

    @Override
    public void setHeadYaw(float headYaw)
    {
        this.mcEntity.method_5847(headYaw);
    }

    @Override
    public void setPrevHeadYaw(float prevHeadYaw)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            living.field_6259 = prevHeadYaw;
        }
    }

    @Override
    public float getPitch()
    {
        return this.mcEntity.method_36455();
    }

    @Override
    public float getPrevPitch()
    {
        return this.mcEntity.field_6004;
    }

    @Override
    public void setPitch(float pitch)
    {
        this.mcEntity.method_36457(pitch);
    }

    @Override
    public void setPrevPitch(float prevPitch)
    {
        this.mcEntity.field_6004 = prevPitch;
    }

    @Override
    public float getBodyYaw()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_6283;
        }

        return this.getHeadYaw();
    }

    @Override
    public float getPrevBodyYaw()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_6220;
        }

        return this.getPrevHeadYaw();
    }

    @Override
    public float getPrevPrevBodyYaw()
    {
        return this.prevPrevBodyYaw;
    }

    @Override
    public void setBodyYaw(float bodyYaw)
    {
        this.mcEntity.method_5636(bodyYaw);
    }

    @Override
    public void setPrevBodyYaw(float prevBodyYaw)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            living.field_6220 = prevBodyYaw;
        }
    }

    @Override
    public void setPrevPrevBodyYaw(float prevPrevBodyYaw)
    {
        this.prevPrevBodyYaw = prevPrevBodyYaw;
    }

    @Override
    public float[] getExtraVariables()
    {
        return this.extraVariables;
    }

    @Override
    public float[] getPrevExtraVariables()
    {
        return this.prevExtraVariables;
    }

    @Override
    public AABB getPickingHitbox()
    {
        float w = this.mcEntity.method_17681();
        float h = this.mcEntity.method_17682();

        return new AABB(
            this.getX() - w / 2, this.getY(), this.getZ() - w / 2,
            w, h, w
        );
    }

    @Override
    public void update()
    {
        this.lastVelocity = this.mcEntity.method_18798();
        this.prevPrevBodyYaw = this.getPrevBodyYaw();

        for (int i = 0; i < this.extraVariables.length; i++)
        {
            this.prevExtraVariables[i] = this.extraVariables[i];
        }
    }

    @Override
    public class_8080 getLimbAnimator()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_42108;
        }

        return null;
    }

    @Override
    public float getLimbPos(float tickDelta)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_42108.method_48572(tickDelta);
        }

        return 0F;
    }

    @Override
    public float getLimbSpeed(float tickDelta)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.field_42108.method_48570(tickDelta);
        }

        return 0F;
    }

    @Override
    public float getLeaningPitch(float tickDelta)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6024(tickDelta);
        }

        return 0F;
    }

    @Override
    public boolean isTouchingWater()
    {
        return this.mcEntity.method_5799();
    }

    @Override
    public class_4050 getEntityPose()
    {
        if ((this.mountTarget != null || this.sitting) && this.mcEntity.method_18376() == class_4050.field_18076)
        {
            return class_4050.field_40118;
        }

        return this.mcEntity.method_18376();
    }

    @Override
    public IEntity getMountTarget()
    {
        return this.mountTarget;
    }

    @Override
    public void setMountTarget(IEntity mountTarget)
    {
        this.mountTarget = mountTarget;
    }

    @Override
    public boolean isSitting()
    {
        return this.sitting;
    }

    @Override
    public void setSitting(boolean sitting)
    {
        this.sitting = sitting;
    }

    @Override
    public IEntity getRiderTarget()
    {
        return this.riderTarget;
    }

    @Override
    public void setRiderTarget(IEntity riderTarget)
    {
        this.riderTarget = riderTarget;
    }

    @Override
    public int getRoll()
    {
        return 0;
    }

    @Override
    public boolean isSwimming()
    {
        return this.mcEntity.method_5681();
    }

    @Override
    public void setSwimming(boolean swimming)
    {
        this.mcEntity.method_5796(swimming);
    }

    @Override
    public boolean isFlying()
    {
        if (this.mcEntity instanceof class_1657 player)
        {
            return player.method_31549().field_7479;
        }

        return false;
    }

    @Override
    public void setFlying(boolean flying)
    {
        if (this.mcEntity instanceof class_1657 player)
        {
            player.method_31549().field_7479 = flying;
        }
    }

    @Override
    public boolean isFallFlying()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6128();
        }

        return false;
    }

    @Override
    public void setFallFlying(boolean fallFlying)
    {
        /* Flag 7 is fall flying (elytra) in Minecraft */
        this.mcEntity.method_5729(7, fallFlying);
    }

    @Override
    public class_243 getRotationVec(float transition)
    {
        return this.mcEntity.method_5828(transition);
    }

    @Override
    public class_243 lerpVelocity(float transition)
    {
        return this.lastVelocity.method_35590(this.mcEntity.method_18798(), transition);
    }

    @Override
    public boolean isUsingRiptide()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6123();
        }

        return false;
    }

    @Override
    public void setRiptide(boolean riptide)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            /* Flag 4 is Riptide spin attack in LivingEntity */
            living.method_6085(4, riptide);
        }
    }

    @Override
    public boolean isCrawling()
    {
        return this.mcEntity.method_18376() == class_4050.field_18079 && !this.mcEntity.method_5799();
    }

    @Override
    public void setCrawling(boolean crawling)
    {
        if (crawling)
        {
            this.mcEntity.method_18380(class_4050.field_18079);
        }
    }

    @Override
    public boolean isClimbing()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6101();
        }

        return false;
    }

    @Override
    public void setClimbing(boolean climbing)
    {}

    @Override
    public boolean isBlocking()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6039();
        }

        return false;
    }

    @Override
    public void setBlocking(boolean blocking)
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            /* LivingFlag 1 is using item (e.g. blocking with shield) */
            living.method_6085(1, blocking);
        }
    }

    @Override
    public boolean isSleeping()
    {
        if (this.mcEntity instanceof class_1309 living)
        {
            return living.method_6113();
        }

        return false;
    }

    @Override
    public void setSleeping(boolean sleeping)
    {
        if (sleeping)
        {
            this.mcEntity.method_18380(class_4050.field_18078);
        }
    }
}