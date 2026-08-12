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

    /**
     * Returns the parent group key for the given bone/group key,
     * or null if the bone is a root (has no parent).
     */
    public String getParentGroupKey(String key);

    /**
     * Returns the keys of all root-level groups (bones with no parent).
     */
    public Collection<String> getRootGroupKeys();

    /**
     * Returns the keys of direct children of the given group/bone.
     */
    public Collection<String> getDirectChildrenKeys(String key);

    /**
     * Returns all group keys in hierarchy order (parents before children).
     * Default implementation performs a depth-first traversal from roots.
     */
    public default List<String> getGroupKeysInHierarchyOrder()
    {
        List<String> out = new ArrayList<>();

        for (String root : this.getRootGroupKeys())
        {
            this.collectDescendants(root, out);
        }

        return out;
    }

    default void collectDescendants(String name, List<String> out)
    {
        out.add(name);

        for (String child : this.getDirectChildrenKeys(name))
        {
            this.collectDescendants(child, out);
        }
    }

    public void apply(IEntity target, Animation action, float tick, float blend, float transition, boolean skipInitial);

    public void postApply(IEntity target, Animation action, float tick, float transition);

    public IModel copy();
}