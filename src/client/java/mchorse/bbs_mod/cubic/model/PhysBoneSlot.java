package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class PhysBoneSlot extends ValueGroup
{
    public final ValueString bone = new ValueString("bone", "");
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueBoolean pitch = new ValueBoolean("pitch", true);
    public final ValueBoolean roll = new ValueBoolean("roll", false);
    public final ValueFloat stiffness = new ValueFloat("stiffness", 8F);
    public final ValueFloat damping = new ValueFloat("damping", 1.75F);
    public final ValueFloat gravity = new ValueFloat("gravity", 0.2F);
    public final ValueFloat inertia = new ValueFloat("inertia", 1F);
    public final ValueFloat simSpeed = new ValueFloat("sim_speed", 1F);
    public final ValueFloat maxAngle = new ValueFloat("max_angle", 45F);
    public final ValueFloat rollFactor = new ValueFloat("roll_factor", 0.5F);
    public final ValueBoolean collision = new ValueBoolean("collision", true);

    /* Advanced properties */
    public final ValueString chainEnd = new ValueString("chain_end", "");
    public final ValueString anchorEnd = new ValueString("anchor_end", "");
    public final ValueFloat gravityStrength = new ValueFloat("gravity_strength", 1F);
    public final ValueFloat gravityDirX = new ValueFloat("gravity_dir_x", 0F);
    public final ValueFloat gravityDirY = new ValueFloat("gravity_dir_y", -1F);
    public final ValueFloat gravityDirZ = new ValueFloat("gravity_dir_z", 0F);
    public final ValueBoolean localForce = new ValueBoolean("local_force", false);
    public final ValueBoolean limitAngles = new ValueBoolean("limit_angles", false);
    public final ValueFloat minPitch = new ValueFloat("min_pitch", -180F);
    public final ValueFloat maxPitch = new ValueFloat("max_pitch", 180F);
    public final ValueFloat minYaw = new ValueFloat("min_yaw", -180F);
    public final ValueFloat maxYaw = new ValueFloat("max_yaw", 180F);
    public final ValueFloat minRoll = new ValueFloat("min_roll", -180F);
    public final ValueFloat maxRoll = new ValueFloat("max_roll", 180F);
    public final ValueInt solverSteps = new ValueInt("solver_steps", 4);
    public final ValueFloat collisionRadius = new ValueFloat("collision_radius", 0.15F);

    public PhysBoneSlot(String id)
    {
        super(id);

        this.add(this.bone);
        this.add(this.enabled);
        this.add(this.pitch);
        this.add(this.roll);
        this.add(this.stiffness);
        this.add(this.damping);
        this.add(this.gravity);
        this.add(this.inertia);
        this.add(this.simSpeed);
        this.add(this.maxAngle);
        this.add(this.rollFactor);
        this.add(this.collision);

        this.add(this.chainEnd);
        this.add(this.anchorEnd);
        this.add(this.gravityStrength);
        this.add(this.gravityDirX);
        this.add(this.gravityDirY);
        this.add(this.gravityDirZ);
        this.add(this.localForce);
        this.add(this.limitAngles);
        this.add(this.minPitch);
        this.add(this.maxPitch);
        this.add(this.minYaw);
        this.add(this.maxYaw);
        this.add(this.minRoll);
        this.add(this.maxRoll);
        this.add(this.solverSteps);
        this.add(this.collisionRadius);
    }
}
