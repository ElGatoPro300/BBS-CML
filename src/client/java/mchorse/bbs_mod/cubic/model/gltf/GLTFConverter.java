package mchorse.bbs_mod.cubic.model.gltf;

import mchorse.bbs_mod.bobj.BOBJAction;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJChannel;
import mchorse.bbs_mod.bobj.BOBJGroup;
import mchorse.bbs_mod.bobj.BOBJKeyframe;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.bobj.BOBJLoader.BOBJData;
import mchorse.bbs_mod.bobj.BOBJLoader.BOBJMesh;
import mchorse.bbs_mod.bobj.BOBJLoader.Face;
import mchorse.bbs_mod.bobj.BOBJLoader.IndexGroup;
import mchorse.bbs_mod.bobj.BOBJLoader.Vertex;
import mchorse.bbs_mod.bobj.BOBJLoader.Weight;
import mchorse.bbs_mod.cubic.model.gltf.GLTFArmature;
import mchorse.bbs_mod.cubic.model.gltf.data.GLTF;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GLTFConverter
{
    public static BOBJData convert(GLTF gltf)
    {
        System.out.println("GLTFConverter: Starting conversion...");
        
        List<Vertex> vertices = new ArrayList<>();
        List<Vector2d> textures = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<BOBJMesh> meshes = new ArrayList<>();
        Map<String, BOBJAction> actions = new HashMap<>(); // Not parsing animations yet
        Map<String, BOBJArmature> armatures = new HashMap<>();

        // Create Armature from Nodes (Use GLTFArmature)
        GLTFArmature armature = new GLTFArmature("armature");
        armatures.put("armature", armature);

        // Initialize Bones
        if (gltf.nodes != null)
        {
            for (int i = 0; i < gltf.nodes.size(); i++)
            {
                GLTF.GLTFNode node = gltf.nodes.get(i);
                String originalName = node.name == null ? "node_" + i : node.name;
                
                // Parent will be set later
                BOBJBone bone = new BOBJBone(i, originalName, "", new Matrix4f());
                
                // Register bone using GLTFArmature (handles unique naming and node mapping)
                armature.registerBone(i, bone);
            }

            // Build Hierarchy and Transforms
            for (int i = 0; i < gltf.nodes.size(); i++)
            {
                GLTF.GLTFNode node = gltf.nodes.get(i);
                BOBJBone bone = armature.nodeToBone.get(i);
                
                // Set Transform
                Matrix4f localTransform = new Matrix4f();
                if (node.matrix != null && node.matrix.length == 16)
                {
                    localTransform.set(node.matrix);
                }
                else
                {
                    if (node.translation != null) localTransform.translate(node.translation[0], node.translation[1], node.translation[2]);
                    if (node.rotation != null) localTransform.rotate(new Quaternionf(node.rotation[0], node.rotation[1], node.rotation[2], node.rotation[3]));
                    if (node.scale != null) localTransform.scale(node.scale[0], node.scale[1], node.scale[2]);
                }
                
                bone.relBoneMat.set(localTransform);

                // Set Parent
                if (node.children != null)
                {
                    for (Integer childIdx : node.children)
                    {
                        BOBJBone childBone = armature.nodeToBone.get(childIdx);
                        if (childBone != null)
                        {
                            childBone.parent = bone.name;
                            childBone.parentBone = bone;
                        }
                    }
                }
            }
            
            // Compute World Matrices (BoneMat)
            // We need to traverse from root. Find roots (nodes with no parent)
            for (BOBJBone bone : armature.orderedBones)
            {
                if (bone.parentBone == null)
                {
                    computeBoneRecursively(bone.index, new Matrix4f(), armature.nodeToBone, gltf);
                }
            }
        }
        
        // Apply Inverse Bind Matrices from Skin if available
        if (gltf.skins != null)
        {
            for (GLTF.GLTFSkin skin : gltf.skins)
            {
                if (skin.inverseBindMatrices >= 0 && skin.joints != null)
                {
                    Matrix4f[] ibms = getMatrices(gltf, skin.inverseBindMatrices);
                    if (ibms != null && ibms.length == skin.joints.size())
                    {
                        for (int k = 0; k < skin.joints.size(); k++)
                        {
                            int jointNodeIndex = skin.joints.get(k);
                            BOBJBone bone = armature.nodeToBone.get(jointNodeIndex);
                            if (bone != null)
                            {
                                bone.invBoneMat.set(ibms[k]);
                            }
                        }
                    }
                }
            }
        }

        // Process Meshes
        // We create a SINGLE BOBJMesh for all GLTF Primitives because BOBJModelLoader only supports one mesh.
        BOBJMesh mainMesh = new BOBJMesh("MainMesh");
        mainMesh.armature = armature;
        mainMesh.armatureName = "armature";
        meshes.add(mainMesh);

        if (gltf.nodes != null)
        {
            for (int i = 0; i < gltf.nodes.size(); i++)
            {
                GLTF.GLTFNode node = gltf.nodes.get(i);
                if (node.mesh >= 0 && node.mesh < gltf.meshes.size())
                {
                    GLTF.GLTFMesh gltfMesh = gltf.meshes.get(node.mesh);
                    BOBJBone nodeBone = armature.nodeToBone.get(i);
                    Matrix4f transform = nodeBone.boneMat; // World Transform
                    Matrix4f normalTransform = new Matrix4f(transform).invert().transpose();
                    
                    // Check for Skinning
                    GLTF.GLTFSkin skin = null;
                    if (node.skin >= 0 && node.skin < gltf.skins.size())
                    {
                        skin = gltf.skins.get(node.skin);
                    }

                    for (int pIndex = 0; pIndex < gltfMesh.primitives.size(); pIndex++)
                    {
                        GLTF.GLTFPrimitive primitive = gltfMesh.primitives.get(pIndex);
                        
                        // Extract Attributes
                        int posAcc = primitive.attributes.getOrDefault("POSITION", -1);
                        int normAcc = primitive.attributes.getOrDefault("NORMAL", -1);
                        int texAcc = primitive.attributes.getOrDefault("TEXCOORD_0", -1);
                        int jointsAcc = primitive.attributes.getOrDefault("JOINTS_0", -1);
                        int weightsAcc = primitive.attributes.getOrDefault("WEIGHTS_0", -1);
                        int indAcc = primitive.indices;
                        
                        if (posAcc < 0) continue;

                        float[] positions = getFloats(gltf, posAcc);
                        float[] norms = normAcc >= 0 ? getFloats(gltf, normAcc) : null;
                        float[] uvs = texAcc >= 0 ? getFloats(gltf, texAcc) : null;
                        
                        // Skinning Data
                        int[] joints = null;
                        float[] weights = null;
                        
                        if (skin != null && jointsAcc >= 0 && weightsAcc >= 0)
                        {
                            joints = getJoints(gltf, jointsAcc); // Custom method to read shorts/bytes as ints
                            weights = getFloats(gltf, weightsAcc);
                        }

                        // Transform and Add Vertices
                        int vertexOffset = vertices.size();
                        int texOffset = textures.size();
                        int normOffset = normals.size();
                        int vertexCount = positions.length / 3;

                        for (int v = 0; v < vertexCount; v++)
                        {
                            // Position
                            Vector3f pos = new Vector3f(positions[v*3], positions[v*3+1], positions[v*3+2]);
                            
                            Vertex vertex;
                            
                            if (skin != null && joints != null && weights != null)
                            {
                                // Skinned Mesh: Do NOT bake node transform (usually)
                                // But BOBJ might expect model space. 
                                // GLTF Skinned Mesh is in Bind Pose (Mesh Space).
                                // We leave it as is.
                                vertex = new Vertex(pos.x, pos.y, pos.z);
                                
                                // Add Weights
                                for (int k = 0; k < 4; k++)
                                {
                                    int jointIndex = joints[v*4 + k];
                                    float weight = weights[v*4 + k];
                                    
                                    if (weight > 0)
                                    {
                                        // jointIndex is index into skin.joints
                                        if (jointIndex >= 0 && jointIndex < skin.joints.size())
                                        {
                                            int nodeIndex = skin.joints.get(jointIndex);
                                            BOBJBone bone = armature.nodeToBone.get(nodeIndex);
                                            if (bone != null)
                                            {
                                                vertex.weights.add(new Weight(bone.name, weight));
                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                // Static/Rigid Mesh: Bake Transform
                                pos.mulPosition(transform);
                                vertex = new Vertex(pos.x, pos.y, pos.z);
                                // Weight to the node bone (1.0)
                                vertex.weights.add(new Weight(nodeBone.name, 1.0f));
                            }
                            
                            vertices.add(vertex);

                            // Normal
                            if (norms != null)
                            {
                                Vector3f norm = new Vector3f(norms[v*3], norms[v*3+1], norms[v*3+2]);
                                if (skin == null)
                                {
                                    norm.mulDirection(normalTransform);
                                }
                                norm.normalize();
                                normals.add(norm);
                            }
                            else
                            {
                                normals.add(new Vector3f(0, 1, 0));
                            }

                            // UV
                            if (uvs != null)
                            {
                                textures.add(new Vector2d(uvs[v*2], 1F - uvs[v*2+1]));
                            }
                            else
                            {
                                textures.add(new Vector2d(0, 0));
                            }
                        }

                        // Indices
                        int[] indices = getIndices(gltf, indAcc);
                        if (indices == null)
                        {
                            // Generate sequential
                            indices = new int[vertexCount];
                            for(int k=0; k<vertexCount; k++) indices[k] = k;
                        }

                        // Triangulate
                        for (int k = 0; k < indices.length; k += 3)
                        {
                            int i1 = indices[k];
                            int i2 = indices[k+1];
                            int i3 = indices[k+2];
                            
                            Face face = new Face();
                            face.idxGroups[0] = createIndexGroup(vertexOffset + i1, texOffset + i1, normOffset + i1);
                            face.idxGroups[1] = createIndexGroup(vertexOffset + i2, texOffset + i2, normOffset + i2);
                            face.idxGroups[2] = createIndexGroup(vertexOffset + i3, texOffset + i3, normOffset + i3);
                            
                            mainMesh.faces.add(face);
                        }
                    }
                }
            }
        }
        
        // Finalize Armature (Inverse Bind Matrices)
        for (BOBJBone bone : armature.orderedBones)
        {
             // BOBJBone constructor already inverted the initial boneMat (which we set to World Transform)
             // So invBoneMat should be correct for "Bind Pose" which is the current state.
             bone.invBoneMat.set(bone.boneMat).invert();
        }

        BOBJData data = new BOBJData(vertices, textures, normals, meshes, actions, armatures);
        convertAnimations(gltf, data, armature.nodeToBone);

        System.out.println("GLTFConverter: Finished conversion.");
        System.out.println("  Vertices: " + vertices.size());
        System.out.println("  Faces: " + meshes.get(0).faces.size());
        
        if (!vertices.isEmpty()) {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (Vertex v : vertices) {
                if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x;
                if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y;
                if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z;
            }
            System.out.println(String.format("  Bounds: [%.4f, %.4f, %.4f] to [%.4f, %.4f, %.4f]", minX, minY, minZ, maxX, maxY, maxZ));
            System.out.println(String.format("  Size: [%.4f, %.4f, %.4f]", maxX - minX, maxY - minY, maxZ - minZ));
        }
        
        return data;
    }
    
    private static void convertAnimations(GLTF gltf, BOBJData data, Map<Integer, BOBJBone> nodeToBone)
    {
        if (gltf.animations == null) return;

        System.out.println("GLTFConverter: Converting " + gltf.animations.size() + " animations...");

        for (GLTF.GLTFAnimation anim : gltf.animations)
        {
            String name = anim.name == null || anim.name.isEmpty() ? "animation_" + data.actions.size() : anim.name;
            BOBJAction action = new BOBJAction(name);
            data.actions.put(name, action);
            
            System.out.println("  Processing animation: " + name);

            for (GLTF.GLTFChannel channel : anim.channels)
            {
                if (channel.target.node < 0) continue;
                BOBJBone bone = nodeToBone.get(channel.target.node);
                if (bone == null) continue;
                
                GLTF.GLTFNode node = gltf.nodes.get(channel.target.node);

                BOBJGroup group = action.groups.computeIfAbsent(bone.name, k -> new BOBJGroup(k));
                
                GLTF.GLTFSampler sampler = anim.samplers.get(channel.sampler);
                float[] times = getFloats(gltf, sampler.input);
                float[] values = getFloats(gltf, sampler.output);
                
                if (times.length == 0 || values.length == 0) continue;

                String path = channel.target.path;
                
                if (path.equals("translation"))
                {
                    // Calculate Delta Translation (Value - Rest)
                    Vector3f restPos = new Vector3f();
                    if (node.translation != null) restPos.set(node.translation[0], node.translation[1], node.translation[2]);
                    
                    float[] deltaValues = new float[values.length];
                    for (int i = 0; i < times.length; i++)
                    {
                        deltaValues[i * 3] = values[i * 3] - restPos.x;
                        deltaValues[i * 3 + 1] = values[i * 3 + 1] - restPos.y;
                        deltaValues[i * 3 + 2] = values[i * 3 + 2] - restPos.z;
                    }

                    addChannel(group, "location", 0, times, deltaValues, 3, sampler.interpolation);
                    addChannel(group, "location", 1, times, deltaValues, 3, sampler.interpolation);
                    addChannel(group, "location", 2, times, deltaValues, 3, sampler.interpolation);
                }
                else if (path.equals("scale"))
                {
                    // Calculate Delta Scale (Value / Rest)
                    Vector3f restScale = new Vector3f(1, 1, 1);
                    if (node.scale != null) restScale.set(node.scale[0], node.scale[1], node.scale[2]);

                    float[] deltaValues = new float[values.length];
                    for (int i = 0; i < times.length; i++)
                    {
                        deltaValues[i * 3] = restScale.x != 0 ? values[i * 3] / restScale.x : 1;
                        deltaValues[i * 3 + 1] = restScale.y != 0 ? values[i * 3 + 1] / restScale.y : 1;
                        deltaValues[i * 3 + 2] = restScale.z != 0 ? values[i * 3 + 2] / restScale.z : 1;
                    }

                    addChannel(group, "scale", 0, times, deltaValues, 3, sampler.interpolation);
                    addChannel(group, "scale", 1, times, deltaValues, 3, sampler.interpolation);
                    addChannel(group, "scale", 2, times, deltaValues, 3, sampler.interpolation);
                }
                else if (path.equals("rotation"))
                    {
                        // Calculate Delta Rotation (inv(Rest) * Value) and convert to Euler ZYX (Radians)
                        Quaternionf restRot = new Quaternionf();
                        if (node.rotation != null) restRot.set(node.rotation[0], node.rotation[1], node.rotation[2], node.rotation[3]);
                        Quaternionf invRestRot = new Quaternionf(restRot).conjugate();

                        float[] eulerValues = new float[times.length * 3];
                        Vector3f lastEuler = null;

                        for (int i = 0; i < times.length; i++)
                        {
                            float x = values[i * 4];
                            float y = values[i * 4 + 1];
                            float z = values[i * 4 + 2];
                            float w = values[i * 4 + 3];
                            
                            Quaternionf qAnim = new Quaternionf(x, y, z, w);
                            
                            // Delta = inv(Rest) * Anim
                            Quaternionf qDelta = new Quaternionf(invRestRot).mul(qAnim);
                            
                            // Convert to Euler ZYX (for BOBJ: RotZ * RotY * RotX)
                            Vector3f euler = getEulerZYX(qDelta);

                            // Ensure continuity
                            if (lastEuler != null)
                            {
                                euler.x = adjustAngle(euler.x, lastEuler.x);
                                euler.y = adjustAngle(euler.y, lastEuler.y);
                                euler.z = adjustAngle(euler.z, lastEuler.z);
                            }
                            lastEuler = new Vector3f(euler);
                            
                            // Store in Radians
                            eulerValues[i * 3] = euler.x;
                            eulerValues[i * 3 + 1] = euler.y;
                            eulerValues[i * 3 + 2] = euler.z;
                        }
                        
                        addChannel(group, "rotation", 0, times, eulerValues, 3, sampler.interpolation);
                        addChannel(group, "rotation", 1, times, eulerValues, 3, sampler.interpolation);
                        addChannel(group, "rotation", 2, times, eulerValues, 3, sampler.interpolation);
                    }
                }
            }
        }

        private static float adjustAngle(float angle, float lastAngle)
        {
            float diff = angle - lastAngle;
            if (Math.abs(diff) > Math.PI)
            {
                float turns = Math.round(diff / (2 * Math.PI));
                angle -= turns * 2 * (float) Math.PI;
            }
            return angle;
        }

        private static Vector3f getEulerZYX(Quaternionf q)
    {
        // Extract Euler angles for sequence Z-Y-X (RotZ * RotY * RotX)
        // Corresponds to standard Yaw-Pitch-Roll extraction
        // Reference: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
        
        Vector3f euler = new Vector3f();
        
        // roll (x-axis rotation)
        float sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        float cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        euler.x = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        float sinp = 2 * (q.w * q.y - q.z * q.x);
        if (Math.abs(sinp) >= 1)
            euler.y = (float) Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            euler.y = (float) Math.asin(sinp);

        // yaw (z-axis rotation)
        float siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        float cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        euler.z = (float) Math.atan2(siny_cosp, cosy_cosp);
        
        return euler;
    }

    private static void addChannel(BOBJGroup group, String path, int index, float[] times, float[] values, int stride, String interpolation)
    {
        BOBJChannel channel = null;
        for (BOBJChannel c : group.channels)
        {
            if (c.path.equals(path) && c.index == index)
            {
                channel = c;
                break;
            }
        }
        if (channel == null)
        {
            channel = new BOBJChannel(path, index);
            group.channels.add(channel);
        }

        for (int i = 0; i < times.length; i++)
        {
            float time = times[i] * 20.0f; // Seconds to ticks
            float value = values[i * stride + index];
            
            String interp = "LINEAR";
            if (interpolation != null)
            {
                if (interpolation.equals("STEP")) interp = "CONSTANT";
                else if (interpolation.equals("CUBICSPLINE")) interp = "BEZIER";
            }
            
            channel.keyframes.add(new BOBJKeyframe(time, value, interp));
        }
    }
    
    // Helper to traverse hierarchy for matrix computation
    private static void computeBoneRecursively(int nodeIndex, Matrix4f parentMat, Map<Integer, BOBJBone> nodeToBone, GLTF gltf)
    {
        BOBJBone bone = nodeToBone.get(nodeIndex);
        GLTF.GLTFNode node = gltf.nodes.get(nodeIndex);
        
        bone.boneMat = new Matrix4f(parentMat).mul(bone.relBoneMat);
        
        if (node.children != null)
        {
            for (int child : node.children)
            {
                computeBoneRecursively(child, bone.boneMat, nodeToBone, gltf);
            }
        }
    }
    
    private static IndexGroup createIndexGroup(int p, int t, int n)
    {
        IndexGroup g = new IndexGroup();
        g.idxPos = p;
        g.idxTextCoord = t;
        g.idxVecNormal = n;
        return g;
    }

    private static Matrix4f[] getMatrices(GLTF gltf, int accessorIndex)
    {
        if (accessorIndex < 0) return null;
        GLTF.GLTFAccessor accessor = gltf.accessors.get(accessorIndex);
        GLTF.GLTFBufferView view = gltf.bufferViews.get(accessor.bufferView);
        GLTF.GLTFBuffer buffer = gltf.buffers.get(view.buffer);
        
        byte[] data = buffer.data;
        if (data == null) return null;
        
        int count = accessor.count;
        Matrix4f[] result = new Matrix4f[count];
        
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        int stride = view.byteStride > 0 ? view.byteStride : 16 * 4; // 16 floats * 4 bytes
        int offset = view.byteOffset + accessor.byteOffset;
        
        float[] temp = new float[16];
        
        for (int i = 0; i < count; i++)
        {
            int pos = offset + i * stride;
            for (int k = 0; k < 16; k++)
            {
                temp[k] = buf.getFloat(pos + k * 4);
            }
            result[i] = new Matrix4f().set(temp);
        }
        return result;
    }

    private static int[] getJoints(GLTF gltf, int accessorIndex)
    {
        if (accessorIndex < 0) return null;
        GLTF.GLTFAccessor accessor = gltf.accessors.get(accessorIndex);
        GLTF.GLTFBufferView view = gltf.bufferViews.get(accessor.bufferView);
        GLTF.GLTFBuffer buffer = gltf.buffers.get(view.buffer);
        
        byte[] data = buffer.data;
        if (data == null) return null;
        
        int count = accessor.count;
        int numComponents = getNumComponents(accessor.type); // Should be VEC4 (4)
        int[] result = new int[count * numComponents];
        
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        int compSize = getComponentSize(accessor.componentType);
        int stride = view.byteStride > 0 ? view.byteStride : numComponents * compSize;
        int offset = view.byteOffset + accessor.byteOffset;
        
        for (int i = 0; i < count; i++)
        {
            int pos = offset + i * stride;
            for (int c = 0; c < numComponents; c++)
            {
                int val = 0;
                int cPos = pos + c * compSize;
                
                if (accessor.componentType == 5121) // UNSIGNED_BYTE
                {
                    val = buf.get(cPos) & 0xFF;
                }
                else if (accessor.componentType == 5123) // UNSIGNED_SHORT
                {
                    val = buf.getShort(cPos) & 0xFFFF;
                }
                else if (accessor.componentType == 5120) // BYTE
                {
                    val = buf.get(cPos);
                }
                else if (accessor.componentType == 5122) // SHORT
                {
                    val = buf.getShort(cPos);
                }
                
                result[i * numComponents + c] = val;
            }
        }
        return result;
    }

    private static float[] getFloats(GLTF gltf, int accessorIndex)
    {
        GLTF.GLTFAccessor accessor = gltf.accessors.get(accessorIndex);
        GLTF.GLTFBufferView view = gltf.bufferViews.get(accessor.bufferView);
        GLTF.GLTFBuffer buffer = gltf.buffers.get(view.buffer);
        
        byte[] data = buffer.data;
        if (data == null) return new float[0];
        
        int count = accessor.count;
        int numComponents = getNumComponents(accessor.type);
        float[] result = new float[count * numComponents];
        
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN); // GLTF is LE
        
        int compSize = getComponentSize(accessor.componentType);
        int stride = view.byteStride > 0 ? view.byteStride : numComponents * compSize;
        int offset = view.byteOffset + accessor.byteOffset;
        
        for (int i = 0; i < count; i++)
        {
            int pos = offset + i * stride;
            for (int c = 0; c < numComponents; c++)
            {
                int cPos = pos + c * compSize;
                
                if (accessor.componentType == 5126) // FLOAT
                {
                    result[i * numComponents + c] = buf.getFloat(cPos);
                }
                else if (accessor.normalized)
                {
                    if (accessor.componentType == 5121) // UNSIGNED_BYTE
                    {
                        result[i * numComponents + c] = (buf.get(cPos) & 0xFF) / 255.0f;
                    }
                    else if (accessor.componentType == 5123) // UNSIGNED_SHORT
                    {
                        result[i * numComponents + c] = (buf.getShort(cPos) & 0xFFFF) / 65535.0f;
                    }
                    else if (accessor.componentType == 5120) // BYTE
                    {
                        result[i * numComponents + c] = Math.max(buf.get(cPos) / 127.0f, -1.0f);
                    }
                    else if (accessor.componentType == 5122) // SHORT
                    {
                        result[i * numComponents + c] = Math.max(buf.getShort(cPos) / 32767.0f, -1.0f);
                    }
                }
                else
                {
                    // Fallback for non-normalized integers used as floats? (e.g. JOINTS if used wrongly here)
                    result[i * numComponents + c] = 0; 
                }
            }
        }
        return result;
    }
    
    private static int[] getIndices(GLTF gltf, int accessorIndex)
    {
        if (accessorIndex < 0) return null;
        GLTF.GLTFAccessor accessor = gltf.accessors.get(accessorIndex);
        GLTF.GLTFBufferView view = gltf.bufferViews.get(accessor.bufferView);
        GLTF.GLTFBuffer buffer = gltf.buffers.get(view.buffer);
        
        byte[] data = buffer.data;
        if (data == null) return null;
        
        int count = accessor.count;
        int[] result = new int[count];
        
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        int compSize = getComponentSize(accessor.componentType);
        int stride = view.byteStride > 0 ? view.byteStride : compSize; // Scalar
        int offset = view.byteOffset + accessor.byteOffset;
        
        for (int i = 0; i < count; i++)
        {
            int pos = offset + i * stride;
            if (accessor.componentType == 5123) // UNSIGNED_SHORT
            {
                result[i] = buf.getShort(pos) & 0xFFFF;
            }
            else if (accessor.componentType == 5125) // UNSIGNED_INT
            {
                result[i] = buf.getInt(pos);
            }
            else if (accessor.componentType == 5121) // UNSIGNED_BYTE
            {
                result[i] = buf.get(pos) & 0xFF;
            }
        }
        return result;
    }

    private static int getNumComponents(String type)
    {
        switch (type) {
            case "SCALAR": return 1;
            case "VEC2": return 2;
            case "VEC3": return 3;
            case "VEC4": return 4;
            case "MAT2": return 4;
            case "MAT3": return 9;
            case "MAT4": return 16;
        }
        return 1;
    }

    private static int getComponentSize(int componentType)
    {
        switch (componentType) {
            case 5120: return 1; // BYTE
            case 5121: return 1; // UNSIGNED_BYTE
            case 5122: return 2; // SHORT
            case 5123: return 2; // UNSIGNED_SHORT
            case 5125: return 4; // UNSIGNED_INT
            case 5126: return 4; // FLOAT
        }
        return 1;
    }
}
