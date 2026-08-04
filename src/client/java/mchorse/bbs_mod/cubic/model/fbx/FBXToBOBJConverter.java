package mchorse.bbs_mod.cubic.model.fbx;

import mchorse.bbs_mod.bobj.BOBJAction;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJChannel;
import mchorse.bbs_mod.bobj.BOBJGroup;
import mchorse.bbs_mod.bobj.BOBJKeyframe;
import mchorse.bbs_mod.bobj.BOBJLoader;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FBXToBOBJConverter
{
    private final FBXNode root;
    private final File fbxFile;

    private final Map<Long, FBXNode> objectsById = new HashMap<Long, FBXNode>();
    private final Map<Long, String> objectClassById = new HashMap<Long, String>();

    /* parent id list of child ids, from Connections "C" (child, parent) pairs */
    private final Map<Long, List<Long>> childrenOf = new HashMap<Long, List<Long>>();
    private final Map<Long, List<Long>> parentsOf = new HashMap<Long, List<Long>>();
    /* connection property name, keyed by "childId:parentId", used for material/texture channel (e.g. "DiffuseColor") or something */
    private final Map<String, String> connectionProperty = new HashMap<String, String>();

    public String resolvedTexturePath;


    private Matrix4f axisCorrection = new Matrix4f().identity();

    public FBXToBOBJConverter(FBXNode root, File fbxFile)
    {
        this.root = root;
        this.fbxFile = fbxFile;
    }

    public BOBJLoader.BOBJData convert()
    {
        this.indexObjects();
        this.indexConnections();
        this.axisCorrection = this.readAxisCorrection();

        List<BOBJLoader.Vertex> vertices = new ArrayList<BOBJLoader.Vertex>();
        List<Vector2d> textures = new ArrayList<Vector2d>();
        List<Vector3f> normals = new ArrayList<Vector3f>();
        List<BOBJLoader.BOBJMesh> meshes = new ArrayList<BOBJLoader.BOBJMesh>();
        Map<String, BOBJArmature> armatures = new HashMap<String, BOBJArmature>();


        List<FBXNode> meshModels = this.findAllModelsOfType("Mesh");

        if (meshModels.isEmpty())
        {
            return new BOBJLoader.BOBJData(vertices, textures, normals, meshes, new HashMap<>(), armatures);
        }

        BOBJArmature armature = this.buildSharedArmature(meshModels);

        if (armature != null)
        {
            armatures.put(armature.name, armature);
        }

        for (FBXNode meshModel : meshModels)
        {
            long meshModelId = meshModel.getLong(0);
            FBXNode geometry = this.findConnectedGeometry(meshModelId);

            if (geometry == null)
            {
                continue;
            }

            FBXNode skin = this.findConnectedDeformer(geometry.getLong(0), "Skin");
            Matrix4f vertexTransform = armature != null
                    ? this.axisCorrection.mul(this.computeMeshGlobal(meshModel), new Matrix4f())
                    : new Matrix4f(this.axisCorrection);

            MeshBuild build = this.extractMesh(geometry, meshModel, skin, armature, vertexTransform);

            BOBJLoader.BOBJMesh mesh = new BOBJLoader.BOBJMesh(this.nameOf(meshModel, "Model"));
            mesh.faces = build.faces;

            if (armature != null)
            {
                mesh.armatureName = armature.name;
                mesh.armature = armature;
            }


            mesh = mesh.add(vertices.size(), normals.size(), textures.size());

            vertices.addAll(build.vertices);
            textures.addAll(build.uvs);
            normals.addAll(build.normals);

            meshes.add(mesh);

            if (this.resolvedTexturePath == null)
            {
                this.resolvedTexturePath = this.resolveTexture(meshModel);
            }
        }

        Map<String, BOBJAction> actions = armature != null ? this.extractAnimations(armature) : new HashMap<String, BOBJAction>();

        return new BOBJLoader.BOBJData(vertices, textures, normals, meshes, actions, armatures);
    }


    private static final double FBX_TIME_UNITS_PER_SECOND = 46186158000.0;


    private Map<String, BOBJAction> extractAnimations(BOBJArmature armature)
    {
        Map<String, BOBJAction> actions = new HashMap<String, BOBJAction>();

        for (FBXNode node : this.objectsById.values())
        {
            if (!"AnimationStack".equals(node.name))
            {
                continue;
            }

            String actionName = this.stripActionName(this.nameOf(node, "AnimStack"));
            FBXNode layer = this.findFirstChildOfClass(node.getLong(0), "AnimationLayer");

            if (layer == null)
            {
                continue;
            }

            BOBJAction action = new BOBJAction(actionName);

            this.populateAction(action, layer, armature);

            if (!action.groups.isEmpty())
            {
                actions.put(actionName, action);
            }
        }

        return actions;
    }


    private String stripActionName(String raw)
    {
        int bar = raw.indexOf('|');

        return bar >= 0 ? raw.substring(bar + 1) : raw;
    }

    private FBXNode findFirstChildOfClass(long parentId, String subtype)
    {
        List<Long> children = this.childrenOf.get(parentId);

        if (children == null)
        {
            return null;
        }

        for (long id : children)
        {
            if (subtype.equals(this.objectClassById.get(id)))
            {
                return this.objectsById.get(id);
            }
        }

        return null;
    }

    private void populateAction(BOBJAction action, FBXNode layer, BOBJArmature armature)
    {
        List<Long> curveNodeIds = this.childrenOf.get(layer.getLong(0));

        if (curveNodeIds == null)
        {
            return;
        }

        for (long curveNodeId : curveNodeIds)
        {
            if (!"AnimationCurveNode".equals(this.objectClassById.get(curveNodeId)))
            {
                continue;
            }

            FBXNode model = null;
            String propName = null;

            List<Long> parents = this.parentsOf.get(curveNodeId);

            if (parents != null)
            {
                for (long parentId : parents)
                {
                    if ("Model".equals(this.objectClassById.get(parentId)))
                    {
                        model = this.objectsById.get(parentId);
                        propName = this.connectionProperty.get(curveNodeId + ":" + parentId);
                        break;
                    }
                }
            }

            if (model == null || propName == null)
            {
                continue;
            }

            String boneName = this.nameOf(model, "Model");

            /* this also naturally skips camera/root/mesh-node animation curves, which BOBJBone has no concept of.  */
            if (!armature.bones.containsKey(boneName))
            {
                continue;
            }

            FBXNode curveX = null, curveY = null, curveZ = null;
            List<Long> curveIds = this.childrenOf.get(curveNodeId);

            if (curveIds != null)
            {
                for (long curveId : curveIds)
                {
                    if (!"AnimationCurve".equals(this.objectClassById.get(curveId)))
                    {
                        continue;
                    }

                    String axis = this.connectionProperty.get(curveId + ":" + curveNodeId);
                    FBXNode curve = this.objectsById.get(curveId);

                    if ("d|X".equals(axis))
                    {
                        curveX = curve;
                    } else if ("d|Y".equals(axis))
                    {
                        curveY = curve;
                    } else if ("d|Z".equals(axis))
                    {
                        curveZ = curve;
                    }
                }
            }

            BOBJGroup group = action.groups.computeIfAbsent(boneName, k -> new BOBJGroup(k));

            if ("Lcl Translation".equals(propName))
            {
                Vector3f rest = this.readVecProperty70(model, "Lcl Translation", new Vector3f(0, 0, 0));

                this.addDeltaChannel(group, "location", 0, curveX, rest.x, false);
                this.addDeltaChannel(group, "location", 1, curveY, rest.y, false);
                this.addDeltaChannel(group, "location", 2, curveZ, rest.z, false);
            } else if ("Lcl Scaling".equals(propName))
            {
                Vector3f rest = this.readVecProperty70(model, "Lcl Scaling", new Vector3f(1, 1, 1));

                this.addDeltaChannel(group, "scale", 0, curveX, rest.x, true);
                this.addDeltaChannel(group, "scale", 1, curveY, rest.y, true);
                this.addDeltaChannel(group, "scale", 2, curveZ, rest.z, true);
            } else if ("Lcl Rotation".equals(propName))
            {
                this.addRotationChannels(group, model, curveX, curveY, curveZ);
            }
        }
    }


    private void addDeltaChannel(BOBJGroup group, String path, int index, FBXNode curve, float rest, boolean divide)
    {
        if (curve == null)
        {
            return;
        }

        float[] times = this.readCurveTimesSeconds(curve);
        float[] values = this.readCurveValues(curve);

        if (times.length == 0)
        {
            return;
        }

        BOBJChannel channel = new BOBJChannel(path, index);

        for (int i = 0; i < times.length && i < values.length; i++)
        {
            float delta = divide ? (rest != 0 ? values[i] / rest : 1F) : (values[i] - rest);

            channel.keyframes.add(new BOBJKeyframe(times[i] * 20F, delta));
        }

        group.channels.add(channel);
    }


    private void addRotationChannels(BOBJGroup group, FBXNode model, FBXNode curveX, FBXNode curveY, FBXNode curveZ)
    {
        if (curveX == null && curveY == null && curveZ == null)
        {
            return;
        }

        float[] timesX = this.readCurveTimesSeconds(curveX);
        float[] valuesX = this.readCurveValues(curveX);
        float[] timesY = this.readCurveTimesSeconds(curveY);
        float[] valuesY = this.readCurveValues(curveY);
        float[] timesZ = this.readCurveTimesSeconds(curveZ);
        float[] valuesZ = this.readCurveValues(curveZ);

        TreeSet<Float> union = new TreeSet<Float>();

        for (float t : timesX) union.add(t);
        for (float t : timesY) union.add(t);
        for (float t : timesZ) union.add(t);

        if (union.isEmpty())
        {
            return;
        }

        int rotationOrder = this.readRotationOrder(model);
        Vector3f preDeg = this.readVecProperty70(model, "PreRotation", new Vector3f(0, 0, 0));
        Vector3f postDeg = this.readVecProperty70(model, "PostRotation", new Vector3f(0, 0, 0));
        Vector3f restDeg = this.readVecProperty70(model, "Lcl Rotation", new Vector3f(0, 0, 0));

        Quaternionf restQ = this.composeFBXRotation(preDeg, restDeg, postDeg, rotationOrder);
        Quaternionf restQInv = new Quaternionf(restQ).conjugate();

        BOBJChannel rx = new BOBJChannel("rotation", 0);
        BOBJChannel ry = new BOBJChannel("rotation", 1);
        BOBJChannel rz = new BOBJChannel("rotation", 2);

        Vector3f lastEuler = null;

        for (float t : union)
        {
            float xDeg = this.sampleCurve(timesX, valuesX, t, restDeg.x);
            float yDeg = this.sampleCurve(timesY, valuesY, t, restDeg.y);
            float zDeg = this.sampleCurve(timesZ, valuesZ, t, restDeg.z);

            Quaternionf animQ = this.composeFBXRotation(preDeg, new Vector3f(xDeg, yDeg, zDeg), postDeg, rotationOrder);
            Quaternionf deltaQ = new Quaternionf(restQInv).mul(animQ);

            Vector3f euler = this.quatToEulerZYXRadians(deltaQ);

            if (lastEuler != null)
            {
                euler.x = this.adjustAngle(euler.x, lastEuler.x);
                euler.y = this.adjustAngle(euler.y, lastEuler.y);
                euler.z = this.adjustAngle(euler.z, lastEuler.z);
            }

            lastEuler = new Vector3f(euler);

            float frame = t * 20F;

            rx.keyframes.add(new BOBJKeyframe(frame, euler.x));
            ry.keyframes.add(new BOBJKeyframe(frame, euler.y));
            rz.keyframes.add(new BOBJKeyframe(frame, euler.z));
        }

        group.channels.add(rx);
        group.channels.add(ry);
        group.channels.add(rz);
    }

    private static float adjustAngle(float angle, float lastAngle)
    {
        float diff = angle - lastAngle;

        if (Math.abs(diff) > Math.PI)
        {
            float turns = Math.round(diff / (2 * (float) Math.PI));

            angle -= turns * 2 * (float) Math.PI;
        }

        return angle;
    }


    private Quaternionf composeFBXRotation(Vector3f preDeg, Vector3f rotDeg, Vector3f postDeg, int rotationOrder)
    {
        Quaternionf pre = this.eulerToQuat(preDeg, 0);
        Quaternionf rot = this.eulerToQuat(rotDeg, rotationOrder);
        Quaternionf postInv = this.eulerToQuat(postDeg, 0).conjugate();

        return new Quaternionf(pre).mul(rot).mul(postInv);
    }


    private Quaternionf eulerToQuat(Vector3f deg, int rotationOrder)
    {
        float x = (float) Math.toRadians(deg.x);
        float y = (float) Math.toRadians(deg.y);
        float z = (float) Math.toRadians(deg.z);

        Quaternionf q = new Quaternionf();

        switch (rotationOrder)
        {
            case 1: /* XZY */
                q.rotateY(y).rotateZ(z).rotateX(x);
                break;
            case 2: /* YZX */
                q.rotateX(x).rotateZ(z).rotateY(y);
                break;
            case 3: /* YXZ */
                q.rotateZ(z).rotateX(x).rotateY(y);
                break;
            case 4: /* ZXY */
                q.rotateY(y).rotateX(x).rotateZ(z);
                break;
            case 5: /* ZYX */
                q.rotateX(x).rotateY(y).rotateZ(z);
                break;
            default: /* XYZ */
                q.rotateZ(z).rotateY(y).rotateX(x);
                break;
        }

        return q;
    }


    private Vector3f quatToEulerZYXRadians(Quaternionf q)
    {
        float sinrCosp = 2 * (q.w * q.x + q.y * q.z);
        float cosrCosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        float roll = (float) Math.atan2(sinrCosp, cosrCosp);

        float sinp = 2 * (q.w * q.y - q.z * q.x);
        float pitch = Math.abs(sinp) >= 1 ? (float) Math.copySign(Math.PI / 2, sinp) : (float) Math.asin(sinp);

        float sinyCosp = 2 * (q.w * q.z + q.x * q.y);
        float cosyCosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        float yaw = (float) Math.atan2(sinyCosp, cosyCosp);

        return new Vector3f(roll, pitch, yaw);
    }


    private int readRotationOrder(FBXNode model)
    {
        return this.readIntProperty70(model, "RotationOrder", 0);
    }


    private int readIntProperty70(FBXNode node, String propName, int fallback)
    {
        FBXNode props = node.child("Properties70");

        if (props == null)
        {
            return fallback;
        }

        for (FBXNode p : props.childrenNamed("P"))
        {
            if (p.properties.size() > 0 && propName.equals(p.getString(0)))
            {
                int n = p.properties.size();

                if (n >= 1)
                {
                    Object last = p.properties.get(n - 1);

                    if (last instanceof Number)
                    {
                        return ((Number) last).intValue();
                    }
                }
            }
        }

        return fallback;
    }


    private Matrix4f readAxisCorrection()
    {
        FBXNode settings = this.root.child("GlobalSettings");

        if (settings == null)
        {
            return new Matrix4f().identity();
        }

        int upAxis = this.readIntProperty70(settings, "UpAxis", 1);
        int upSign = this.readIntProperty70(settings, "UpAxisSign", 1);
        int frontAxis = this.readIntProperty70(settings, "FrontAxis", 2);
        int frontSign = this.readIntProperty70(settings, "FrontAxisSign", 1);
        int coordAxis = this.readIntProperty70(settings, "CoordAxis", 0);
        int coordSign = this.readIntProperty70(settings, "CoordAxisSign", 1);

        Matrix4f correction;

        if (upAxis == 1 && upSign == 1 && frontAxis == 2 && frontSign == 1 && coordAxis == 0 && coordSign == 1)
        {
            correction = new Matrix4f().identity();
        }
        else
        {
            float[] m = new float[16];

            for (int rawAxis = 0; rawAxis < 3; rawAxis++)
            {
                int col = rawAxis * 4;

                m[col] = coordAxis == rawAxis ? coordSign : 0;
                m[col + 1] = upAxis == rawAxis ? upSign : 0;
                m[col + 2] = frontAxis == rawAxis ? frontSign : 0;
                m[col + 3] = 0;
            }

            m[12] = 0;
            m[13] = 0;
            m[14] = 0;
            m[15] = 1;

            correction = new Matrix4f();
            correction.set(m);
        }


        float bakedRootScale = this.detectBakedRootScale();

        if (bakedRootScale != 1F)
        {
            correction.scale(1F / bakedRootScale);
        }

        return correction;
    }


    private float detectBakedRootScale()
    {
        List<Float> found = new ArrayList<Float>();

        for (FBXNode node : this.objectsById.values())
        {
            if (!"Model".equals(node.name))
            {
                continue;
            }

            long id = node.getLong(0);
            List<Long> parents = this.parentsOf.get(id);
            boolean isRoot = parents == null || parents.isEmpty() || parents.contains(0L);

            if (!isRoot)
            {
                continue;
            }

            Vector3f scale = this.readVecProperty70(node, "Lcl Scaling", new Vector3f(1, 1, 1));

            float sx = Math.abs(scale.x);
            float sy = Math.abs(scale.y);
            float sz = Math.abs(scale.z);

            if (sx <= 0F || sy <= 0F || sz <= 0F)
            {
                continue;
            }

            float maxC = Math.max(sx, Math.max(sy, sz));
            float minC = Math.min(sx, Math.min(sy, sz));

            boolean uniform = (maxC / minC) < 1.01F;
            boolean farFromOne = sx > 5F || sx < 0.2F;

            if (uniform && farFromOne)
            {
                found.add(sx);
            }
        }

        if (found.isEmpty())
        {
            return 1F;
        }


        float best = 1F;

        for (float f : found)
        {
            if (Math.abs(Math.log(f)) > Math.abs(Math.log(best)))
            {
                best = f;
            }
        }

        return best;
    }

    private double readDoubleProperty70(FBXNode node, String propName, double fallback)
    {
        FBXNode props = node.child("Properties70");

        if (props == null)
        {
            return fallback;
        }

        for (FBXNode p : props.childrenNamed("P"))
        {
            if (p.properties.size() > 0 && propName.equals(p.getString(0)))
            {
                int n = p.properties.size();

                if (n >= 1)
                {
                    Object last = p.properties.get(n - 1);

                    if (last instanceof Number)
                    {
                        return ((Number) last).doubleValue();
                    }
                }
            }
        }

        return fallback;
    }

    private float[] readCurveTimesSeconds(FBXNode curve)
    {
        if (curve == null)
        {
            return new float[0];
        }

        FBXNode keyTime = curve.child("KeyTime");

        if (keyTime == null)
        {
            return new float[0];
        }

        double[] raw = keyTime.asDoubleArray();
        float[] out = new float[raw.length];

        for (int i = 0; i < raw.length; i++)
        {
            out[i] = (float) (raw[i] / FBX_TIME_UNITS_PER_SECOND);
        }

        return out;
    }

    private float[] readCurveValues(FBXNode curve)
    {
        if (curve == null)
        {
            return new float[0];
        }

        FBXNode keyValue = curve.child("KeyValueFloat");

        if (keyValue == null)
        {
            return new float[0];
        }

        double[] raw = keyValue.asDoubleArray();
        float[] out = new float[raw.length];

        for (int i = 0; i < raw.length; i++)
        {
            out[i] = (float) raw[i];
        }

        return out;
    }

    private float sampleCurve(float[] times, float[] values, float t, float fallback)
    {
        if (times.length == 0)
        {
            return fallback;
        }

        if (t <= times[0])
        {
            return values[0];
        }

        if (t >= times[times.length - 1])
        {
            return values[values.length - 1];
        }

        for (int i = 1; i < times.length; i++)
        {
            if (t <= times[i])
            {
                float t0 = times[i - 1];
                float t1 = times[i];
                float v0 = values[i - 1];
                float v1 = values[i];
                float f = t1 == t0 ? 0F : (t - t0) / (t1 - t0);

                return v0 + (v1 - v0) * f;
            }
        }

        return values[values.length - 1];
    }

    private BOBJArmature buildSharedArmature(List<FBXNode> meshModels)
    {
        List<FBXNode> allClusters = new ArrayList<FBXNode>();
        FBXNode representativeMeshModel = null;

        for (FBXNode meshModel : meshModels)
        {
            long meshModelId = meshModel.getLong(0);
            FBXNode geometry = this.findConnectedGeometry(meshModelId);

            if (geometry == null)
            {
                continue;
            }

            FBXNode skin = this.findConnectedDeformer(geometry.getLong(0), "Skin");

            if (skin == null)
            {
                continue;
            }

            if (representativeMeshModel == null)
            {
                representativeMeshModel = meshModel;
            }

            allClusters.addAll(this.findClusters(skin.getLong(0)));
        }

        if (allClusters.isEmpty())
        {
            return null;
        }

        return this.buildArmatureFromClusters(allClusters, representativeMeshModel);
    }

    private void indexObjects()
    {
        FBXNode objects = this.root.child("Objects");

        if (objects == null)
        {
            return;
        }

        for (FBXNode node : objects.children)
        {
            if (node.properties.isEmpty())
            {
                continue;
            }

            long id = node.getLong(0);

            this.objectsById.put(id, node);
            this.objectClassById.put(id, node.name);
        }
    }

    private void indexConnections()
    {
        FBXNode connections = this.root.child("Connections");

        if (connections == null)
        {
            return;
        }

        for (FBXNode c : connections.childrenNamed("C"))
        {
            if (c.properties.size() < 3)
            {
                continue;
            }

            long child = c.getLong(1);
            long parent = c.getLong(2);

            this.childrenOf.computeIfAbsent(parent, k -> new ArrayList<Long>()).add(child);
            this.parentsOf.computeIfAbsent(child, k -> new ArrayList<Long>()).add(parent);

            if (c.properties.size() > 3)
            {
                this.connectionProperty.put(child + ":" + parent, c.getString(3));
            }
        }
    }

    private List<FBXNode> findAllModelsOfType(String subtype)
    {
        List<FBXNode> result = new ArrayList<FBXNode>();

        for (FBXNode node : this.objectsById.values())
        {
            if (node.name.equals("Model") && node.properties.size() > 2 && subtype.equals(node.getString(2)))
            {
                result.add(node);
            }
        }

        return result;
    }

    private FBXNode findConnectedGeometry(long modelId)
    {
        List<Long> children = this.childrenOf.get(modelId);

        if (children == null)
        {
            return null;
        }

        for (long id : children)
        {
            if ("Geometry".equals(this.objectClassById.get(id)))
            {
                return this.objectsById.get(id);
            }
        }

        return null;
    }

    private FBXNode findConnectedDeformer(long geometryId, String subtype)
    {
        List<Long> children = this.childrenOf.get(geometryId);

        if (children == null)
        {
            return null;
        }

        for (long id : children)
        {
            if ("Deformer".equals(this.objectClassById.get(id)))
            {
                FBXNode node = this.objectsById.get(id);

                if (node.properties.size() > 2 && subtype.equals(node.getString(2)))
                {
                    return node;
                }
            }
        }

        return null;
    }

    private List<FBXNode> findClusters(long skinId)
    {
        List<FBXNode> clusters = new ArrayList<FBXNode>();
        List<Long> children = this.childrenOf.get(skinId);

        if (children == null)
        {
            return clusters;
        }

        for (long id : children)
        {
            if ("Deformer".equals(this.objectClassById.get(id)))
            {
                FBXNode node = this.objectsById.get(id);

                if (node.properties.size() > 2 && "Cluster".equals(node.getString(2)))
                {
                    clusters.add(node);
                }
            }
        }

        return clusters;
    }

    private FBXNode findClusterLimb(FBXNode cluster)
    {
        long clusterId = cluster.getLong(0);
        List<Long> children = this.childrenOf.get(clusterId);

        if (children == null)
        {
            return null;
        }

        for (long id : children)
        {
            if ("Model".equals(this.objectClassById.get(id)))
            {
                FBXNode model = this.objectsById.get(id);

                if (model.properties.size() > 2 && "LimbNode".equals(model.getString(2)))
                {
                    return model;
                }
            }
        }

        return null;
    }

    private String nameOf(FBXNode node, String category)
    {
        if (node.properties.size() > 1)
        {
            String raw = node.getString(1);
            int sep = raw.indexOf("::");

            if (sep >= 0)
            {
                return raw.substring(sep + 2);
            }

            int nul = raw.indexOf('\u0000');

            if (nul >= 0)
            {
                return raw.substring(0, nul);
            }

            return raw;
        }

        return category;
    }

    private BOBJArmature buildArmatureFromClusters(List<FBXNode> clusters, FBXNode meshModel)
    {
        String armatureName = this.nameOf(meshModel, "Model") + "_armature";
        BOBJArmature armature = new BOBJArmature(armatureName);

        Map<String, FBXNode> limbByName = new HashMap<String, FBXNode>();
        Map<String, FBXNode> clusterByLimbName = new HashMap<String, FBXNode>();
        Map<String, Matrix4f> bindMatByLimbName = new HashMap<String, Matrix4f>();

        for (FBXNode cluster : clusters)
        {
            FBXNode limb = this.findClusterLimb(cluster);

            if (limb == null)
            {
                continue;
            }

            String limbName = this.nameOf(limb, "LimbNode");

            limbByName.put(limbName, limb);
            clusterByLimbName.put(limbName, cluster);

            FBXNode transformLink = cluster.child("TransformLink");

            if (transformLink != null)
            {
                bindMatByLimbName.put(limbName, this.readMatrix(transformLink));
            }
        }


        Set<String> allLimbNames = new HashSet<String>(limbByName.keySet());
        Map<String, FBXNode> allLimbs = new HashMap<String, FBXNode>(limbByName);

        for (FBXNode limb : new ArrayList<FBXNode>(limbByName.values()))
        {
            this.collectAncestorLimbs(limb, allLimbs, allLimbNames);
        }


        Map<String, String> parentNameOf = new HashMap<String, String>();

        for (String name : allLimbNames)
        {
            parentNameOf.put(name, this.findParentLimbName(allLimbs.get(name), allLimbNames));
        }

        Map<String, List<String>> childrenOfLimb = new HashMap<String, List<String>>();
        List<String> roots = new ArrayList<String>();

        for (String name : allLimbNames)
        {
            String parentName = parentNameOf.get(name);

            if (parentName == null)
            {
                roots.add(name);
            } else
            {
                childrenOfLimb.computeIfAbsent(parentName, k -> new ArrayList<String>()).add(name);
            }
        }

        Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
        List<String> queue = new ArrayList<String>(roots);
        int index = 0;

        while (index < queue.size())
        {
            String current = queue.get(index);

            nameToIndex.put(current, index);
            index++;

            List<String> children = childrenOfLimb.get(current);

            if (children != null)
            {
                queue.addAll(children);
            }
        }


        for (String name : allLimbNames)
        {
            FBXNode limb = allLimbs.get(name);
            String parentName = parentNameOf.get(name);

            Matrix4f boneMat = bindMatByLimbName.get(name);

            if (boneMat == null)
            {
                boneMat = this.computeGlobalTransform(limb);
            }

            boneMat = this.axisCorrection.mul(boneMat, new Matrix4f());

            BOBJBone bone = new BOBJBone(nameToIndex.get(name), name, parentName == null ? "" : parentName, boneMat);
            armature.addBone(bone);
        }

        return armature;
    }


    private void collectAncestorLimbs(FBXNode limb, Map<String, FBXNode> allLimbs, Set<String> allLimbNames)
    {
        long limbId = limb.getLong(0);
        List<Long> parents = this.parentsOf.get(limbId);

        if (parents == null)
        {
            return;
        }

        for (long parentId : parents)
        {
            if (!"Model".equals(this.objectClassById.get(parentId)))
            {
                continue;
            }

            FBXNode parentModel = this.objectsById.get(parentId);

            if (parentModel.properties.size() <= 2 || !"LimbNode".equals(parentModel.getString(2)))
            {
                continue;
            }

            String parentName = this.nameOf(parentModel, "LimbNode");

            if (allLimbNames.add(parentName))
            {
                allLimbs.put(parentName, parentModel);
                this.collectAncestorLimbs(parentModel, allLimbs, allLimbNames);
            }
        }
    }

    private String findParentLimbName(FBXNode limb, Set<String> allLimbNames)
    {
        long limbId = limb.getLong(0);
        List<Long> parents = this.parentsOf.get(limbId);

        if (parents == null)
        {
            return null;
        }

        for (long parentId : parents)
        {
            if (!"Model".equals(this.objectClassById.get(parentId)))
            {
                continue;
            }

            FBXNode parentModel = this.objectsById.get(parentId);

            if (parentModel.properties.size() > 2 && "LimbNode".equals(parentModel.getString(2)))
            {
                String parentName = this.nameOf(parentModel, "LimbNode");

                if (allLimbNames.contains(parentName))
                {
                    return parentName;
                }
            }
        }

        return null;
    }


    private Matrix4f computeGlobalTransform(FBXNode model)
    {
        Matrix4f local = this.readLocalTransform(model);
        long modelId = model.getLong(0);
        List<Long> parents = this.parentsOf.get(modelId);

        if (parents != null)
        {
            for (long parentId : parents)
            {
                if ("Model".equals(this.objectClassById.get(parentId)))
                {
                    Matrix4f parentGlobal = this.computeGlobalTransform(this.objectsById.get(parentId));

                    return parentGlobal.mul(local);
                }
            }
        }

        return local;
    }


    private Matrix4f readLocalTransform(FBXNode model)
    {
        Vector3f translation = this.readVecProperty70(model, "Lcl Translation", new Vector3f(0, 0, 0));
        Vector3f rotationDeg = this.readVecProperty70(model, "Lcl Rotation", new Vector3f(0, 0, 0));
        Vector3f scale = this.readVecProperty70(model, "Lcl Scaling", new Vector3f(1, 1, 1));
        Vector3f preDeg = this.readVecProperty70(model, "PreRotation", new Vector3f(0, 0, 0));
        Vector3f postDeg = this.readVecProperty70(model, "PostRotation", new Vector3f(0, 0, 0));
        int rotationOrder = this.readRotationOrder(model);

        Quaternionf rotationQ = this.composeFBXRotation(preDeg, rotationDeg, postDeg, rotationOrder);

        Matrix4f mat = new Matrix4f().identity();

        mat.translate(translation);
        mat.rotate(rotationQ);
        mat.scale(scale);

        return mat;
    }


    private Matrix4f readGeometricTransform(FBXNode model)
    {
        Vector3f translation = this.readVecProperty70(model, "GeometricTranslation", new Vector3f(0, 0, 0));
        Vector3f rotationDeg = this.readVecProperty70(model, "GeometricRotation", new Vector3f(0, 0, 0));
        Vector3f scale = this.readVecProperty70(model, "GeometricScaling", new Vector3f(1, 1, 1));

        Matrix4f mat = new Matrix4f().identity();

        mat.translate(translation);
        mat.rotateZ((float) Math.toRadians(rotationDeg.z));
        mat.rotateY((float) Math.toRadians(rotationDeg.y));
        mat.rotateX((float) Math.toRadians(rotationDeg.x));
        mat.scale(scale);

        return mat;
    }


    private Matrix4f computeMeshGlobal(FBXNode meshModel)
    {
        return this.computeGlobalTransform(meshModel)
                .mul(this.readGeometricTransform(meshModel), new Matrix4f());
    }

    private Vector3f readVecProperty70(FBXNode model, String propName, Vector3f fallback)
    {
        FBXNode props = model.child("Properties70");

        if (props == null)
        {
            return fallback;
        }

        for (FBXNode p : props.childrenNamed("P"))
        {
            if (p.properties.size() > 0 && propName.equals(p.getString(0)))
            {
                int n = p.properties.size();

                if (n >= 3)
                {
                    double x = ((Number) p.properties.get(n - 3)).doubleValue();
                    double y = ((Number) p.properties.get(n - 2)).doubleValue();
                    double z = ((Number) p.properties.get(n - 1)).doubleValue();

                    return new Vector3f((float) x, (float) y, (float) z);
                }
            }
        }

        return fallback;
    }

    private Matrix4f readMatrix(FBXNode matrixNode)
    {
        double[] d = matrixNode.asDoubleArray();
        float[] m = new float[16];

        for (int i = 0; i < 16 && i < d.length; i++)
        {
            m[i] = (float) d[i];
        }

        Matrix4f mat = new Matrix4f();

        mat.set(m);

        return mat;
    }

    private static class MeshBuild
    {
        List<BOBJLoader.Vertex> vertices = new ArrayList<BOBJLoader.Vertex>();
        List<Vector2d> uvs = new ArrayList<Vector2d>();
        List<Vector3f> normals = new ArrayList<Vector3f>();
        List<BOBJLoader.Face> faces = new ArrayList<BOBJLoader.Face>();
    }

    private MeshBuild extractMesh(FBXNode geometry, FBXNode meshModel, FBXNode skin, BOBJArmature armature, Matrix4f vertexTransform)
    {
        MeshBuild build = new MeshBuild();

        FBXNode verticesNode = geometry.child("Vertices");
        double[] rawVerts = verticesNode == null ? new double[0] : verticesNode.asDoubleArray();

        for (int i = 0; i + 2 < rawVerts.length; i += 3)
        {
            Vector3f pos = new Vector3f((float) rawVerts[i], (float) rawVerts[i + 1], (float) rawVerts[i + 2]);


            vertexTransform.transformPosition(pos);

            BOBJLoader.Vertex v = new BOBJLoader.Vertex(pos.x, pos.y, pos.z);
            build.vertices.add(v);
        }

        Map<Integer, List<WeightPair>> weightsByVertex = new HashMap<Integer, List<WeightPair>>();

        if (skin != null && armature != null)
        {
            this.collectWeights(skin, armature, weightsByVertex);
        }

        for (int vi = 0; vi < build.vertices.size(); vi++)
        {
            BOBJLoader.Vertex v = build.vertices.get(vi);
            List<WeightPair> pairs = weightsByVertex.get(vi);

            if (pairs != null)
            {
                pairs.sort((a, b) -> Double.compare(b.weight, a.weight));

                for (int i = 0, c = Math.min(pairs.size(), 4); i < c; i++)
                {
                    WeightPair p = pairs.get(i);
                    v.weights.add(new BOBJLoader.Weight(p.boneName, (float) p.weight));
                }
            }

            v.eliminateTinyWeights();
        }

        FBXNode polyIndexNode = geometry.child("PolygonVertexIndex");
        int[] rawIndices = polyIndexNode == null ? new int[0] : polyIndexNode.asIntArray();

        FBXNode normalLayer = geometry.child("LayerElementNormal");
        FBXNode uvLayer = geometry.child("LayerElementUV");

        LayerData normalData = normalLayer == null ? null : this.readLayer(normalLayer, "Normals", 3);
        LayerData uvData = uvLayer == null ? null : this.readLayer(uvLayer, "UV", 2);

        List<Integer> currentPolygon = new ArrayList<Integer>();
        int polygonVertexCounter = 0;

        for (int i = 0; i < rawIndices.length; i++)
        {
            int raw = rawIndices[i];
            boolean lastOfPolygon = raw < 0;
            int vertexIndex = lastOfPolygon ? (-raw - 1) : raw;

            currentPolygon.add(vertexIndex);

            int polyVertLocalIndex = currentPolygon.size() - 1;
            int controlPointForLayer = vertexIndex;
            int polygonVertexForLayer = polygonVertexCounter;

            if (normalData != null)
            {
                Vector3f n = this.readLayerVector3(normalData, controlPointForLayer, polygonVertexForLayer);

                vertexTransform.transformDirection(n).normalize();
                normalData.perPolyVertexNormals.put(polygonVertexForLayer, n);
            }

            if (uvData != null)
            {
                Vector2d uv = this.readLayerVector2(uvData, controlPointForLayer, polygonVertexForLayer);
                uvData.perPolyVertexUVs.put(polygonVertexForLayer, uv);
            }

            polygonVertexCounter++;

            if (lastOfPolygon)
            {
                this.triangulateFan(currentPolygon, polygonVertexCounter - currentPolygon.size(), build, normalData, uvData);
                currentPolygon.clear();
            }
        }

        return build;
    }

    private static class WeightPair
    {
        String boneName;
        double weight;

        WeightPair(String boneName, double weight)
        {
            this.boneName = boneName;
            this.weight = weight;
        }
    }

    private void collectWeights(FBXNode skin, BOBJArmature armature, Map<Integer, List<WeightPair>> out)
    {
        for (FBXNode cluster : this.findClusters(skin.getLong(0)))
        {
            FBXNode limb = this.findClusterLimb(cluster);

            if (limb == null)
            {
                continue;
            }

            String boneName = this.nameOf(limb, "LimbNode");

            if (!armature.bones.containsKey(boneName))
            {
                continue;
            }

            FBXNode indexesNode = cluster.child("Indexes");
            FBXNode weightsNode = cluster.child("Weights");

            if (indexesNode == null || weightsNode == null)
            {
                continue;
            }

            int[] indexes = indexesNode.asIntArray();
            double[] weights = weightsNode.asDoubleArray();

            for (int i = 0; i < indexes.length && i < weights.length; i++)
            {
                double w = weights[i];

                if (w <= 0)
                {
                    continue;
                }

                out.computeIfAbsent(indexes[i], k -> new ArrayList<WeightPair>()).add(new WeightPair(boneName, w));
            }
        }
    }

    private static class LayerData
    {
        double[] direct;
        int[] indexToDirect;
        int componentCount;
        boolean byControlPoint;
        boolean directRef;

        Map<Integer, Vector3f> perPolyVertexNormals = new HashMap<Integer, Vector3f>();
        Map<Integer, Vector2d> perPolyVertexUVs = new HashMap<Integer, Vector2d>();
    }

    private LayerData readLayer(FBXNode layerNode, String dataNodeName, int componentCount)
    {
        LayerData data = new LayerData();
        data.componentCount = componentCount;

        FBXNode mapping = layerNode.child("MappingInformationType");
        FBXNode reference = layerNode.child("ReferenceInformationType");

        String mappingType = mapping != null ? mapping.getString(0) : "ByPolygonVertex";
        String referenceType = reference != null ? reference.getString(0) : "Direct";

        data.byControlPoint = "ByControlPoint".equals(mappingType) || "ByVertice".equals(mappingType) || "ByVertex".equals(mappingType);
        data.directRef = "Direct".equals(referenceType);

        FBXNode dataNode = layerNode.child(dataNodeName);
        data.direct = dataNode == null ? new double[0] : dataNode.asDoubleArray();

        if (!data.directRef)
        {
            FBXNode indexNode = layerNode.child(dataNodeName + "Index");
            data.indexToDirect = indexNode == null ? new int[0] : indexNode.asIntArray();
        }

        return data;
    }

    private Vector3f readLayerVector3(LayerData data, int controlPointIndex, int polygonVertexIndex)
    {
        int lookup = data.byControlPoint ? controlPointIndex : polygonVertexIndex;
        int directIndex = data.directRef ? lookup : (lookup < data.indexToDirect.length ? data.indexToDirect[lookup] : 0);
        int base = directIndex * 3;

        if (base + 2 >= data.direct.length)
        {
            return new Vector3f(0, 0, 1);
        }

        return new Vector3f((float) data.direct[base], (float) data.direct[base + 1], (float) data.direct[base + 2]);
    }

    private Vector2d readLayerVector2(LayerData data, int controlPointIndex, int polygonVertexIndex)
    {
        int lookup = data.byControlPoint ? controlPointIndex : polygonVertexIndex;
        int directIndex = data.directRef ? lookup : (lookup < data.indexToDirect.length ? data.indexToDirect[lookup] : 0);
        int base = directIndex * 2;

        if (base + 1 >= data.direct.length)
        {
            return new Vector2d(0, 0);
        }

        return new Vector2d(data.direct[base], data.direct[base + 1]);
    }

    private void triangulateFan(List<Integer> polygonVertexIndices, int firstPolyVertexGlobalIndex, MeshBuild build, LayerData normalData, LayerData uvData)
    {
        int n = polygonVertexIndices.size();

        if (n < 3)
        {
            return;
        }

        for (int i = 1; i < n - 1; i++)
        {
            int[] localCorners = new int[]{0, i, i + 1};
            BOBJLoader.Face face = new BOBJLoader.Face();
            face.idxGroups = new BOBJLoader.IndexGroup[3];

            for (int corner = 0; corner < 3; corner++)
            {
                int localIdx = localCorners[corner];
                int vertexIndex = polygonVertexIndices.get(localIdx);
                int polyVertexGlobalIndex = firstPolyVertexGlobalIndex + localIdx;

                BOBJLoader.IndexGroup group = new BOBJLoader.IndexGroup();
                group.idxPos = vertexIndex;

                if (normalData != null)
                {
                    Vector3f normal = normalData.perPolyVertexNormals.get(polyVertexGlobalIndex);

                    if (normal != null)
                    {
                        build.normals.add(normal);
                        group.idxVecNormal = build.normals.size() - 1;
                    }
                }

                if (uvData != null)
                {
                    Vector2d uv = uvData.perPolyVertexUVs.get(polyVertexGlobalIndex);

                    if (uv != null)
                    {
                        build.uvs.add(uv);
                        group.idxTextCoord = build.uvs.size() - 1;
                    }
                }


                face.idxGroups[corner] = group;
            }

            build.faces.add(face);
        }
    }

    private String resolveTexture(FBXNode meshModel)
    {
        long modelId = meshModel.getLong(0);
        List<Long> children = this.childrenOf.get(modelId);

        if (children == null)
        {
            return null;
        }

        for (long childId : children)
        {
            if (!"Material".equals(this.objectClassById.get(childId)))
            {
                continue;
            }

            List<Long> matChildren = this.childrenOf.get(childId);

            if (matChildren == null)
            {
                continue;
            }

            for (long texId : matChildren)
            {
                if ("Texture".equals(this.objectClassById.get(texId)))
                {
                    FBXNode texture = this.objectsById.get(texId);
                    String result = this.resolveTextureFile(texture);

                    if (result != null)
                    {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    private String resolveTextureFile(FBXNode texture)
    {
        FBXNode relative = texture.child("RelativeFilename");
        FBXNode absolute = texture.child("FileName");

        String relPath = relative != null && !relative.properties.isEmpty() ? relative.getString(0) : null;
        String absPath = absolute != null && !absolute.properties.isEmpty() ? absolute.getString(0) : null;

        if (relPath != null && this.fbxFile != null)
        {
            File candidate = new File(this.fbxFile.getParentFile(), relPath.replace('\\', '/'));

            if (candidate.exists())
            {
                return candidate.getAbsolutePath();
            }
        }

        if (absPath != null)
        {
            File candidate = new File(absPath);

            if (candidate.exists())
            {
                return candidate.getAbsolutePath();
            }
        }

        return relPath != null ? relPath : absPath;
    }
}