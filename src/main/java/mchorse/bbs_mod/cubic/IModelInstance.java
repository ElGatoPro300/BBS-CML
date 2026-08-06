package mchorse.bbs_mod.cubic;

import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.utils.pose.Pose;

public interface IModelInstance
{
    public IModel getModel();

    public Pose getSneakingPose();

    public Animations getAnimations();
}