package mchorse.bbs_mod.forms.entities;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.AABB;
import net.minecraft.class_1268;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_4050;
import net.minecraft.class_8080;
import java.util.HashMap;
import java.util.Map;

public class StubEntity implements IEntity
{
    private class_1937 world;
    private int age;

    private Form form;
    private boolean sneaking;
    private boolean sprinting;
    private boolean swimming;
    private boolean flying;
    private boolean fallFlying;
    private boolean crawling;
    private boolean climbing;
    private boolean blocking;
    private boolean sleeping;
    private boolean riptide;
    private boolean onGround = true;
    private float fallDistance;
    private int hurtTimer;
    private int deathTime;
    private boolean usingItem;
    private int itemUseTimeLeft;
    private int fireTicks;
    private boolean particlesEnabled = true;
    private class_1268 activeHand = class_1268.field_5808;

    private double prevX;
    private double prevY;
    private double prevZ;
    private double x;
    private double y;
    private double z;

    private float prevYaw;
    private float prevHeadYaw;
    private float prevPitch;
    private float prevBodyYaw;
    private float prevPrevBodyYaw;

    private float yaw;
    private float headYaw;
    private float pitch;
    private float bodyYaw;

    private int armSwing;

    private class_243 velocity = class_243.field_1353;

    private float[] extraVariables = new float[10];
    private float[] prevExtraVariables = new float[10];
    private boolean externalPrevPosition;
    private boolean externalPrevRotation;

    private class_8080 limbAnimator = new class_8080();
    private final Map<class_1304, class_1799> items = new HashMap<>();
    private IEntity mountTarget;
    private IEntity riderTarget;
    private boolean sitting;

    public StubEntity(class_1937 world)
    {
        this.world = world;

        for (class_1304 value : class_1304.values())
        {
            this.items.put(value, class_1799.field_8037);
        }
    }

    public StubEntity()
    {
        for (class_1304 value : class_1304.values())
        {
            this.items.put(value, class_1799.field_8037);
        }
    }

    @Override
    public void setWorld(class_1937 world)
    {
        this.world = world;
    }

    @Override
    public class_1937 getWorld()
    {
        return this.world;
    }

    @Override
    public Form getForm()
    {
        return this.form;
    }

    @Override
    public void setForm(Form form)
    {
        this.form = form;
    }

    @Override
    public class_1799 getEquipmentStack(class_1304 slot)
    {
        return this.items.getOrDefault(slot, class_1799.field_8037);
    }

    @Override
    public void setEquipmentStack(class_1304 slot, class_1799 stack)
    {
        if (stack == null)
        {
            stack = class_1799.field_8037;
        }

        this.items.put(slot, stack);
    }

    @Override
    public int getSelectedSlot()
    {
        return 0;
    }

    @Override
    public boolean isSneaking()
    {
        return this.sneaking;
    }

    @Override
    public void setSneaking(boolean sneaking)
    {
        this.sneaking = sneaking;
    }

    @Override
    public boolean isSprinting()
    {
        return this.sprinting;
    }

    @Override
    public void setSprinting(boolean sprinting)
    {
        this.sprinting = sprinting;
    }

    @Override
    public boolean isOnGround()
    {
        return this.onGround;
    }

    @Override
    public void setOnGround(boolean ground)
    {
        this.onGround = ground;
    }

    @Override
    public void swingArm()
    {
        this.armSwing = 6;
    }

    @Override
    public float getHandSwingProgress(float tickDelta)
    {
        return this.armSwing <= 0 ? 0F : 1F - (this.armSwing - tickDelta) / 6F;
    }

    @Override
    public int getAge()
    {
        return this.age;
    }

    @Override
    public void setAge(int ticks)
    {
        this.age = ticks;
    }

    @Override
    public float getFallDistance()
    {
        return this.fallDistance;
    }

    @Override
    public void setFallDistance(float fallDistance)
    {
        this.fallDistance = fallDistance;
    }

    @Override
    public int getHurtTimer()
    {
        return this.hurtTimer;
    }

    @Override
    public void setHurtTimer(int hurtTimer)
    {
        this.hurtTimer = hurtTimer;
    }

    @Override
    public int getDeathTime()
    {
        return this.deathTime;
    }

    @Override
    public void setDeathTime(int deathTime)
    {
        this.deathTime = deathTime;
    }

    @Override
    public boolean isUsingItem()
    {
        return this.usingItem;
    }

    @Override
    public void setUsingItem(boolean usingItem)
    {
        this.usingItem = usingItem;
    }

    @Override
    public int getItemUseTimeLeft()
    {
        return this.itemUseTimeLeft;
    }

    @Override
    public void setItemUseTimeLeft(int itemUseTimeLeft)
    {
        this.itemUseTimeLeft = itemUseTimeLeft;
    }

    @Override
    public int getFireTicks()
    {
        return this.fireTicks;
    }

    @Override
    public void setFireTicks(int fireTicks)
    {
        this.fireTicks = fireTicks;
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
        return this.activeHand;
    }

    @Override
    public void setActiveHand(class_1268 hand)
    {
        this.activeHand = hand == null ? class_1268.field_5808 : hand;
    }

    @Override
    public double getX()
    {
        return this.x;
    }

    @Override
    public double getPrevX()
    {
        return this.prevX;
    }

    @Override
    public void setPrevX(double x)
    {
        this.prevX = x;
        this.externalPrevPosition = true;
    }

    @Override
    public double getY()
    {
        return this.y;
    }

    @Override
    public double getPrevY()
    {
        return this.prevY;
    }

    @Override
    public void setPrevY(double y)
    {
        this.prevY = y;
        this.externalPrevPosition = true;
    }

    @Override
    public double getZ()
    {
        return this.z;
    }

    @Override
    public double getPrevZ()
    {
        return this.prevZ;
    }

    @Override
    public void setPrevZ(double z)
    {
        this.prevZ = z;
        this.externalPrevPosition = true;
    }

    @Override
    public void setPosition(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double getEyeHeight()
    {
        return 1.8F * 0.9F;
    }

    @Override
    public class_243 getVelocity()
    {
        return this.velocity;
    }

    @Override
    public void setVelocity(float x, float y, float z)
    {
        this.velocity = new class_243(x, y, z);
    }

    @Override
    public float getYaw()
    {
        return this.yaw;
    }

    @Override
    public float getPrevYaw()
    {
        return this.prevYaw;
    }

    @Override
    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }

    @Override
    public void setPrevYaw(float prevYaw)
    {
        this.prevYaw = prevYaw;
        this.externalPrevRotation = true;
    }

    @Override
    public float getHeadYaw()
    {
        return this.headYaw;
    }

    @Override
    public float getPrevHeadYaw()
    {
        return this.prevHeadYaw;
    }

    @Override
    public void setHeadYaw(float headYaw)
    {
        this.headYaw = headYaw;
    }

    @Override
    public void setPrevHeadYaw(float prevHeadYaw)
    {
        this.prevHeadYaw = prevHeadYaw;
        this.externalPrevRotation = true;
    }

    @Override
    public float getPitch()
    {
        return this.pitch;
    }

    @Override
    public float getPrevPitch()
    {
        return this.prevPitch;
    }

    @Override
    public void setPitch(float pitch)
    {
        this.pitch = pitch;
    }

    @Override
    public void setPrevPitch(float prevPitch)
    {
        this.prevPitch = prevPitch;
        this.externalPrevRotation = true;
    }

    @Override
    public float getBodyYaw()
    {
        return this.bodyYaw;
    }

    @Override
    public float getPrevBodyYaw()
    {
        return this.prevBodyYaw;
    }

    @Override
    public float getPrevPrevBodyYaw()
    {
        return this.prevPrevBodyYaw;
    }

    @Override
    public void setBodyYaw(float bodyYaw)
    {
        this.bodyYaw = bodyYaw;
    }

    @Override
    public void setPrevBodyYaw(float prevBodyYaw)
    {
        this.prevBodyYaw = prevBodyYaw;
        this.externalPrevRotation = true;
    }

    @Override
    public void setPrevPrevBodyYaw(float prevPrevBodyYaw)
    {
        this.prevPrevBodyYaw = prevPrevBodyYaw;
        this.externalPrevRotation = true;
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
        Form form = this.getForm();
        float w = 0.6F;
        float h = 1.8F;

        if (form != null && form.hitbox.get())
        {
            w = form.hitboxWidth.get();
            h = form.hitboxHeight.get();
        }

        return new AABB(
            this.getX() - w / 2, this.getY(), this.getZ() - w / 2,
            w, h, w
        );
    }

    @Override
    public void update()
    {
        float delta = (float) class_3532.method_33825(this.x - this.prevX, 0D, this.z - this.prevZ);
        float speed = Math.min(delta * 4F, 1F);

        /*
         * Teleport / seek-sized jumps must not drive LimbAnimator: one huge delta looks like
         * a walk kick, then the next idle ticks decay back to stand, then real walk starts.
         */
        if (delta > 0.45F)
        {
            speed = 0F;
        }

        this.limbAnimator.method_48568(speed, 0.4F);

        this.armSwing -= 1;
        this.age += 1;

        if (!this.externalPrevPosition)
        {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
        }

        if (!this.externalPrevRotation && this.mountTarget == null)
        {
            this.prevPrevBodyYaw = this.prevBodyYaw;
            this.prevYaw = this.yaw;
            this.prevHeadYaw = this.headYaw;
            this.prevPitch = this.pitch;
            this.prevBodyYaw = this.bodyYaw;
        }

        this.externalPrevPosition = false;
        this.externalPrevRotation = false;

        for (int i = 0; i < this.extraVariables.length; i++)
        {
            this.prevExtraVariables[i] = this.extraVariables[i];
        }
    }

    @Override
    public class_8080 getLimbAnimator()
    {
        return this.limbAnimator;
    }

    @Override
    public float getLimbPos(float tickDelta)
    {
        return this.limbAnimator.method_48572(tickDelta);
    }

    @Override
    public float getLimbSpeed(float tickDelta)
    {
        return this.limbAnimator.method_48570(tickDelta);
    }

    @Override
    public float getLeaningPitch(float tickDelta)
    {
        return 0;
    }

    @Override
    public boolean isTouchingWater()
    {
        return false;
    }

    @Override
    public class_4050 getEntityPose()
    {
        if (this.mountTarget != null || this.sitting)
        {
            return class_4050.field_40118;
        }

        if (this.sneaking)
        {
            return class_4050.field_18081;
        }

        return class_4050.field_18076;
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
    public int getRoll()
    {
        return 0;
    }

    @Override
    public boolean isSwimming()
    {
        return this.swimming;
    }

    @Override
    public void setSwimming(boolean swimming)
    {
        this.swimming = swimming;
    }

    @Override
    public boolean isFlying()
    {
        return this.flying;
    }

    @Override
    public void setFlying(boolean flying)
    {
        this.flying = flying;
    }

    @Override
    public boolean isFallFlying()
    {
        return this.fallFlying;
    }

    @Override
    public void setFallFlying(boolean fallFlying)
    {
        this.fallFlying = fallFlying;
    }

    @Override
    public class_243 getRotationVec(float transition)
    {
        return class_243.field_1353;
    }

    @Override
    public class_243 lerpVelocity(float transition)
    {
        return class_243.field_1353;
    }

    @Override
    public boolean isUsingRiptide()
    {
        return this.riptide;
    }

    @Override
    public void setRiptide(boolean riptide)
    {
        this.riptide = riptide;
    }

    @Override
    public boolean isCrawling()
    {
        return this.crawling;
    }

    @Override
    public void setCrawling(boolean crawling)
    {
        this.crawling = crawling;
    }

    @Override
    public boolean isClimbing()
    {
        return this.climbing;
    }

    @Override
    public void setClimbing(boolean climbing)
    {
        this.climbing = climbing;
    }

    @Override
    public boolean isBlocking()
    {
        return this.blocking;
    }

    @Override
    public void setBlocking(boolean blocking)
    {
        this.blocking = blocking;
    }

    @Override
    public boolean isSleeping()
    {
        return this.sleeping;
    }

    @Override
    public void setSleeping(boolean sleeping)
    {
        this.sleeping = sleeping;
    }
}
