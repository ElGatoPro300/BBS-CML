package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.misc.ValueVector3f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

import org.joml.Vector3f;

public class IKChainConfig extends ValueGroup
{
    /** The tip (end-effector) bone name */
    public final ValueString tipBone = new ValueString("tip", "");

    /** The root bone name (where the chain starts) */
    public final ValueString rootBone = new ValueString("root", "");

    /** Whether this IK chain is active */
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);

    /**
     * Target position in model-local space (meters).
     * The IK solver will try to reach this point with the tip bone.
     */
    public final ValueVector3f target = new ValueVector3f("target", new Vector3f(0F, -1F, 0F));

    /**
     * Name of a bone (group) to use as the IK target locator.
     * When not empty, this overrides the manual target XYZ with the
     * world-space pivot position of the named bone, read every frame.
     */
    public final ValueString targetBone = new ValueString("target_bone", "");

    /** Maximum number of FABRIK iterations per frame */
    public final ValueInt iterations = new ValueInt("iterations", 10);

    /** Convergence tolerance in meters */
    public final ValueFloat tolerance = new ValueFloat("tolerance", 0.01F);

    /**
     * Number of bones to include in the chain counted from the tip upward.
     * 0 = auto-detect up to rootBone.
     */
    public final ValueInt chainLength = new ValueInt("chain_length", 0);

    /** Blend weight (0 = no IK, 1 = full IK) */
    public final ValueFloat weight = new ValueFloat("weight", 1F);

    public IKChainConfig(String id)
    {
        super(id);

        this.add(this.tipBone);
        this.add(this.rootBone);
        this.add(this.enabled);
        this.add(this.target);
        this.add(this.targetBone);
        this.add(this.iterations);
        this.add(this.tolerance);
        this.add(this.chainLength);
        this.add(this.weight);
    }

    public boolean isInChain(String bone)
    {
        return this.tipBone.get().equals(bone)
            || this.rootBone.get().equals(bone)
            || this.targetBone.get().equals(bone);
    }
}
