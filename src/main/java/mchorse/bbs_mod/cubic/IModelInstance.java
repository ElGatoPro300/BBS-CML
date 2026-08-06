package mchorse.bbs_mod.cubic;

import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.physics.PhysBoneDefinition;
import mchorse.bbs_mod.utils.pose.Pose;

import java.util.List;

public interface IModelInstance
{
    public IModel getModel();

    public Pose getSneakingPose();

    public Pose getRidingPose();

    public Animations getAnimations();

    public String getHeadBone();

    public List<PhysBoneDefinition> getPhysBones();
}
