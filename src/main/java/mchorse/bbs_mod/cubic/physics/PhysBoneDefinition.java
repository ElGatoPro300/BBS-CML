package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import org.joml.Vector3f;

public class PhysBoneDefinition implements IMapSerializable
{
    public String bone = "";
    public boolean enabled = true;
    public boolean affectPitch = true;
    public boolean affectRoll = false;
    public float stiffness = 8F;
    public float damping = 1.75F;
    public float gravity = 0.2F;
    public float inertia = 1F;
    public float simSpeed = 1F;
    public float maxAngle = 45F;
    public float rollFactor = 0.5F;
    public boolean collisionEnabled = true;

    /* Advanced physical bone properties */
    public String chainEnd = "";
    public String anchorEnd = "";
    public float gravityStrength = 1F;
    public Vector3f gravityDir = new Vector3f(0F, -1F, 0F);
    public boolean localForce = false;
    public boolean limitAngles = false;
    public float minPitch = -180F;
    public float maxPitch = 180F;
    public float minYaw = -180F;
    public float maxYaw = 180F;
    public float minRoll = -180F;
    public float maxRoll = 180F;
    public int solverSteps = 4;
    public float collisionRadius = 0.15F;

    @Override
    public void toData(MapType data)
    {
        data.putString("bone", this.bone);
        data.putBool("enabled", this.enabled);
        data.putBool("pitch", this.affectPitch);
        data.putBool("roll", this.affectRoll);
        data.putFloat("stiffness", this.stiffness);
        data.putFloat("damping", this.damping);
        data.putFloat("gravity", this.gravity);
        data.putFloat("inertia", this.inertia);
        data.putFloat("sim_speed", this.simSpeed);
        data.putFloat("max_angle", this.maxAngle);
        data.putFloat("roll_factor", this.rollFactor);
        data.putBool("collision", this.collisionEnabled);

        data.putString("chain_end", this.chainEnd);
        data.putString("anchor_end", this.anchorEnd);
        data.putFloat("gravity_strength", this.gravityStrength);
        data.putFloat("gravity_dir_x", this.gravityDir.x);
        data.putFloat("gravity_dir_y", this.gravityDir.y);
        data.putFloat("gravity_dir_z", this.gravityDir.z);
        data.putBool("local_force", this.localForce);
        data.putBool("limit_angles", this.limitAngles);
        data.putFloat("min_pitch", this.minPitch);
        data.putFloat("max_pitch", this.maxPitch);
        data.putFloat("min_yaw", this.minYaw);
        data.putFloat("max_yaw", this.maxYaw);
        data.putFloat("min_roll", this.minRoll);
        data.putFloat("max_roll", this.maxRoll);
        data.putInt("solver_steps", this.solverSteps);
        data.putFloat("collision_radius", this.collisionRadius);
    }

    @Override
    public void fromData(MapType data)
    {
        this.bone = data.getString("bone", this.bone);
        this.enabled = data.getBool("enabled", this.enabled);
        this.affectPitch = data.getBool("pitch", this.affectPitch);
        this.affectRoll = data.getBool("roll", this.affectRoll);
        this.stiffness = data.getFloat("stiffness", this.stiffness);
        this.damping = data.getFloat("damping", this.damping);
        this.gravity = data.getFloat("gravity", this.gravity);
        this.inertia = data.getFloat("inertia", this.inertia);
        this.simSpeed = data.getFloat("sim_speed", this.simSpeed);
        this.maxAngle = data.getFloat("max_angle", this.maxAngle);
        this.rollFactor = data.getFloat("roll_factor", this.rollFactor);
        this.collisionEnabled = data.getBool("collision", this.collisionEnabled);

        this.chainEnd = data.getString("chain_end", this.chainEnd);
        this.anchorEnd = data.getString("anchor_end", this.anchorEnd);
        if (data.has("gravity_strength"))
        {
            this.gravityStrength = data.getFloat("gravity_strength", this.gravityStrength);
        }
        else if (data.has("gravity"))
        {
            /* Backward compatibility with old gravity value */
            this.gravityStrength = data.getFloat("gravity", this.gravity);
        }
        this.gravityDir.x = data.getFloat("gravity_dir_x", this.gravityDir.x);
        this.gravityDir.y = data.getFloat("gravity_dir_y", this.gravityDir.y);
        this.gravityDir.z = data.getFloat("gravity_dir_z", this.gravityDir.z);
        this.localForce = data.getBool("local_force", this.localForce);
        this.limitAngles = data.getBool("limit_angles", this.limitAngles);
        this.minPitch = data.getFloat("min_pitch", this.minPitch);
        this.maxPitch = data.getFloat("max_pitch", this.maxPitch);
        this.minYaw = data.getFloat("min_yaw", this.minYaw);
        this.maxYaw = data.getFloat("max_yaw", this.maxYaw);
        this.minRoll = data.getFloat("min_roll", this.minRoll);
        this.maxRoll = data.getFloat("max_roll", this.maxRoll);
        this.solverSteps = data.getInt("solver_steps", this.solverSteps);
        this.collisionRadius = data.getFloat("collision_radius", this.collisionRadius);
    }

    public PhysBoneDefinition copy()
    {
        PhysBoneDefinition copy = new PhysBoneDefinition();

        copy.bone = this.bone;
        copy.enabled = this.enabled;
        copy.affectPitch = this.affectPitch;
        copy.affectRoll = this.affectRoll;
        copy.stiffness = this.stiffness;
        copy.damping = this.damping;
        copy.gravity = this.gravity;
        copy.inertia = this.inertia;
        copy.simSpeed = this.simSpeed;
        copy.maxAngle = this.maxAngle;
        copy.rollFactor = this.rollFactor;
        copy.collisionEnabled = this.collisionEnabled;

        copy.chainEnd = this.chainEnd;
        copy.anchorEnd = this.anchorEnd;
        copy.gravityStrength = this.gravityStrength;
        copy.gravityDir.set(this.gravityDir);
        copy.localForce = this.localForce;
        copy.limitAngles = this.limitAngles;
        copy.minPitch = this.minPitch;
        copy.maxPitch = this.maxPitch;
        copy.minYaw = this.minYaw;
        copy.maxYaw = this.maxYaw;
        copy.minRoll = this.minRoll;
        copy.maxRoll = this.maxRoll;
        copy.solverSteps = this.solverSteps;
        copy.collisionRadius = this.collisionRadius;

        return copy;
    }
}
