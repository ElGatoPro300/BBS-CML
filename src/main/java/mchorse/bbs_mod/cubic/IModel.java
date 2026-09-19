package mchorse.bbs_mod.cubic;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.utils.pose.Pose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IModel
{
    public Pose createPose();

    public void resetPose();

    public void applyPose(Pose pose);

    public Set<String> getShapeKeys();

    public String getAnchor();

    public Collection<String> getAllGroupKeys();

    public Collection<String> getAllChildrenKeys(String key);

    public Collection<ModelGroup> getAllGroups();

    public Collection<BOBJBone> getAllBOBJBones();

    public Collection<String> getAdjacentGroups(String groupName);

    public Collection<String> getHierarchyGroups(String groupName);

    public Collection<String> getRootGroupKeys();

    public Collection<String> getDirectChildrenKeys(String key);

    public String getParentGroupKey(String key);

    public default List<String> getGroupKeysInHierarchyOrder()
    {
        List<String> out = new ArrayList<>();

        for (String root : this.getRootGroupKeys())
        {
            this.collectGroupAndDescendants(root, out);
        }

        return out;
    }

    default void collectGroupAndDescendants(String name, List<String> out)
    {
        out.add(name);
        for (String child : this.getDirectChildrenKeys(name))
        {
            this.collectGroupAndDescendants(child, out);
        }
    }

    public void apply(IEntity target, Animation action, float tick, float blend, float transition, boolean skipInitial);

    public void postApply(IEntity target, Animation action, float tick, float transition);
}
