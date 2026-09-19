package mchorse.bbs_mod.cubic.animation.gecko.config;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

public class GeckoLimbAnimationConfig implements IMapSerializable
{
    public boolean swinging;
    public boolean swiping;
    public boolean lookX;
    public boolean lookY;
    public boolean idle;
    public boolean invert;
    public boolean wheel;
    public String wheelAxis = "x";
    public float wheelSpeed = 1F;
    public String idleAnimation = "idle";
    public String walkAnimation = "walk";
    public String runAnimation = "run";
    public String jumpAnimation = "jump";
    public String fallAnimation = "fall";
    public String attackAnimation = "attack";
    public String swimAnimation = "swim";
    public String flyAnimation = "fly";
    public String wheelAnimation = "wheel";

    public boolean isEmpty()
    {
        return !this.swiping
            && !this.swinging
            && !this.lookX
            && !this.lookY
            && !this.idle
            && !this.invert
            && !this.wheel;
    }

    public GeckoLimbAnimationConfig copy()
    {
        GeckoLimbAnimationConfig config = new GeckoLimbAnimationConfig();

        config.swinging = this.swinging;
        config.swiping = this.swiping;
        config.lookX = this.lookX;
        config.lookY = this.lookY;
        config.idle = this.idle;
        config.invert = this.invert;
        config.wheel = this.wheel;
        config.wheelAxis = this.wheelAxis;
        config.wheelSpeed = this.wheelSpeed;
        config.idleAnimation = this.idleAnimation;
        config.walkAnimation = this.walkAnimation;
        config.runAnimation = this.runAnimation;
        config.jumpAnimation = this.jumpAnimation;
        config.fallAnimation = this.fallAnimation;
        config.attackAnimation = this.attackAnimation;
        config.swimAnimation = this.swimAnimation;
        config.flyAnimation = this.flyAnimation;
        config.wheelAnimation = this.wheelAnimation;

        return config;
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("swinging", this.swinging);
        data.putBool("swiping", this.swiping);
        data.putBool("look_x", this.lookX);
        data.putBool("look_y", this.lookY);
        data.putBool("idle", this.idle);
        data.putBool("invert", this.invert);
        data.putBool("wheel", this.wheel);
        data.putString("wheel_axis", this.wheelAxis);
        data.putFloat("wheel_speed", this.wheelSpeed);
        data.putString("idle_animation", this.idleAnimation);
        data.putString("walk_animation", this.walkAnimation);
        data.putString("run_animation", this.runAnimation);
        data.putString("jump_animation", this.jumpAnimation);
        data.putString("fall_animation", this.fallAnimation);
        data.putString("attack_animation", this.attackAnimation);
        data.putString("swim_animation", this.swimAnimation);
        data.putString("fly_animation", this.flyAnimation);
        data.putString("wheel_animation", this.wheelAnimation);
    }

    @Override
    public void fromData(MapType data)
    {
        this.swinging = data.getBool("swinging");
        this.swiping = data.getBool("swiping");
        this.lookX = data.getBool("look_x");
        this.lookY = data.getBool("look_y");
        this.idle = data.getBool("idle");
        this.invert = data.getBool("invert");
        this.wheel = data.getBool("wheel");
        this.wheelAxis = data.getString("wheel_axis", "x");
        this.wheelSpeed = data.getFloat("wheel_speed", 1F);
        this.idleAnimation = data.getString("idle_animation", "idle");
        this.walkAnimation = data.getString("walk_animation", "walk");
        this.runAnimation = data.getString("run_animation", "run");
        this.jumpAnimation = data.getString("jump_animation", "jump");
        this.fallAnimation = data.getString("fall_animation", "fall");
        this.attackAnimation = data.getString("attack_animation", "attack");
        this.swimAnimation = data.getString("swim_animation", "swim");
        this.flyAnimation = data.getString("fly_animation", "fly");
        this.wheelAnimation = data.getString("wheel_animation", "wheel");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof GeckoLimbAnimationConfig config))
        {
            return false;
        }

        return this.swinging == config.swinging
            && this.swiping == config.swiping
            && this.lookX == config.lookX
            && this.lookY == config.lookY
            && this.idle == config.idle
            && this.invert == config.invert
            && this.wheel == config.wheel
            && Float.compare(this.wheelSpeed, config.wheelSpeed) == 0
            && Objects.equals(this.wheelAxis, config.wheelAxis)
            && Objects.equals(this.idleAnimation, config.idleAnimation)
            && Objects.equals(this.walkAnimation, config.walkAnimation)
            && Objects.equals(this.runAnimation, config.runAnimation)
            && Objects.equals(this.jumpAnimation, config.jumpAnimation)
            && Objects.equals(this.fallAnimation, config.fallAnimation)
            && Objects.equals(this.attackAnimation, config.attackAnimation)
            && Objects.equals(this.swimAnimation, config.swimAnimation)
            && Objects.equals(this.flyAnimation, config.flyAnimation)
            && Objects.equals(this.wheelAnimation, config.wheelAnimation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
            this.swinging,
            this.swiping,
            this.lookX,
            this.lookY,
            this.idle,
            this.invert,
            this.wheel,
            this.wheelAxis,
            this.wheelSpeed,
            this.idleAnimation,
            this.walkAnimation,
            this.runAnimation,
            this.jumpAnimation,
            this.fallAnimation,
            this.attackAnimation,
            this.swimAnimation,
            this.flyAnimation,
            this.wheelAnimation
        );
    }
}
