package mchorse.bbs_mod.cubic.model.gltf;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GLTFArmature extends BOBJArmature
{
    /**
     * Mapping from GLTF node index to BOBJBone.
     * Essential for linking animations to renamed bones.
     */
    public Map<Integer, BOBJBone> nodeToBone = new HashMap<>();

    private boolean initialized;

    public GLTFArmature(String name)
    {
        super(name);
    }

    /**
     * Registers a bone from a GLTF node, ensuring a unique name in the armature.
     */
    public void registerBone(int nodeIndex, BOBJBone bone)
    {
        String originalName = bone.name;
        String uniqueName = originalName;
        int count = 1;

        // Ensure unique name to prevent collisions in the bones map
        while (this.bones.containsKey(uniqueName))
        {
            uniqueName = originalName + "_" + count++;
        }

        // Update the bone's name if it changed
        if (!uniqueName.equals(originalName))
        {
            bone.name = uniqueName;
        }

        // Add to standard BOBJ structures
        this.addBone(bone);

        // Map node index to this bone for animation lookup
        this.nodeToBone.put(nodeIndex, bone);
    }

    /**
     * Override initArmature to include topological sorting and dynamic matrix sizing.
     * This is required for GLTF models where node order is not guaranteed and indices may be non-sequential.
     */
    @Override
    public void initArmature()
    {
        if (!this.initialized)
        {
            /* "Connect" parent bones to children bones */
            for (BOBJBone bone : this.bones.values())
            {
                if (!bone.parent.isEmpty())
                {
                    bone.parentBone = this.bones.get(bone.parent);
                    bone.relBoneMat.set(bone.parentBone.boneMat);
                    bone.relBoneMat.invert();
                    bone.relBoneMat.mul(bone.boneMat);
                }
                else
                {
                    bone.relBoneMat.set(bone.boneMat);
                }
            }

            /* Sort bones topologically (parents first) to ensure correct matrix calculation order */
            List<BOBJBone> sorted = new ArrayList<>();
            Set<BOBJBone> visited = new HashSet<>();
            
            for (BOBJBone bone : this.bones.values())
            {
                this.sortBone(bone, sorted, visited);
            }
            
            this.orderedBones = sorted;

            int maxIndex = 0;
            for (BOBJBone b : this.orderedBones)
            {
                if (b.index > maxIndex)
                {
                    maxIndex = b.index;
                }
            }
            this.matrices = new Matrix4f[maxIndex + 1];
            this.initialized = true;
        }
    }

    private void sortBone(BOBJBone bone, List<BOBJBone> sorted, Set<BOBJBone> visited)
    {
        if (visited.contains(bone))
        {
            return;
        }

        if (bone.parentBone != null)
        {
            this.sortBone(bone.parentBone, sorted, visited);
        }

        visited.add(bone);
        sorted.add(bone);
    }

    /**
     * Override setupMatrices to handle dynamic resizing if needed.
     */
    @Override
    public void setupMatrices()
    {
        for (BOBJBone bone : this.orderedBones)
        {
            if (bone.index >= this.matrices.length)
            {
                Matrix4f[] newMatrices = new Matrix4f[bone.index + 1];
                
                System.arraycopy(this.matrices, 0, newMatrices, 0, this.matrices.length);
                this.matrices = newMatrices;
            }
            
            this.matrices[bone.index] = bone.compute();
        }
    }
}
