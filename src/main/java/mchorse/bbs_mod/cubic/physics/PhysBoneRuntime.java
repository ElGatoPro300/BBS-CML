package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.IModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhysBoneRuntime
{
    private static final float MOTION_EPSILON = 0.00075F;
    private static final float ROTATION_EPSILON = 0.04F;
    private static final long LAG_SPIKE_THRESHOLD_NANOS = 80_000_000L;
    private static final float STABILITY_LIMIT = 0.45F;
    private static final int MAX_SUB_STEPS = 8;

    public static void update(IEntity entity, IModelInstance modelInstance, Map<String, PhysBoneState> physStates)
    {
        if (entity == null || modelInstance == null)
        {
            return;
        }

        List<PhysBoneDefinition> definitions = modelInstance.getPhysBones();

        if (definitions == null || definitions.isEmpty())
        {
            physStates.clear();

            return;
        }

        double motionX = entity.getX() - entity.getPrevX();
        double motionY = entity.getY() - entity.getPrevY();
        double motionZ = entity.getZ() - entity.getPrevZ();
        float bodyYaw = entity.getBodyYaw();
        float prevBodyYaw = entity.getPrevBodyYaw();
        float bodyTurn = sanitizeRotation(MathHelper.wrapDegrees(bodyYaw - prevBodyYaw));
        float yawSin = MathHelper.sin(MathUtils.toRad(bodyYaw));
        float yawCos = MathHelper.cos(MathUtils.toRad(bodyYaw));
        float localStrafe = sanitizeMotion((float) (motionX * yawCos + motionZ * yawSin));
        float localForward = sanitizeMotion((float) (-motionX * yawSin + motionZ * yawCos));
        float verticalVelocity = sanitizeMotion((float) motionY);
        IModel model = modelInstance.getModel();
        Map<String, ModelGroup> modelGroups = getModelGroupsById(model);
        Map<String, BOBJBone> bobjBones = getBOBJBonesById(model);
        Map<String, Float> collisionLimits = getPhysCollisionLimits(model, modelGroups, bobjBones);
        Set<String> enabledPhysBones = new HashSet<>();
        Set<String> activeBones = new HashSet<>();

        /* Expand automatic bone chains */
        List<PhysBoneDefinition> expandedDefinitions = new ArrayList<>();
        Set<String> explicitBones = new HashSet<>();

        for (PhysBoneDefinition def : definitions)
        {
            if (def != null && def.enabled && def.bone != null && !def.bone.isEmpty())
            {
                explicitBones.add(resolveBoneName(def.bone, modelGroups, bobjBones));
            }
        }

        for (PhysBoneDefinition def : definitions)
        {
            if (def == null || !def.enabled || def.bone == null || def.bone.isEmpty())
            {
                continue;
            }

            expandedDefinitions.add(def);

            String startBone = resolveBoneName(def.bone, modelGroups, bobjBones);
            String endBone = def.chainEnd == null ? "" : resolveBoneName(def.chainEnd, modelGroups, bobjBones);

            if (!endBone.isEmpty() && !endBone.equals(startBone))
            {
                List<String> chainPath = getChainPath(startBone, endBone, modelGroups, bobjBones);

                if (chainPath != null && chainPath.size() > 1)
                {
                    for (int i = 1; i < chainPath.size(); i++)
                    {
                        String chainBone = chainPath.get(i);

                        if (!explicitBones.contains(chainBone))
                        {
                            PhysBoneDefinition virtualDef = def.copy();

                            virtualDef.bone = chainBone;
                            virtualDef.chainEnd = "";
                            expandedDefinitions.add(virtualDef);
                        }
                    }
                }
            }
        }

        for (PhysBoneDefinition definition : expandedDefinitions)
        {
            if (definition != null && definition.enabled && definition.bone != null && !definition.bone.isEmpty())
            {
                enabledPhysBones.add(resolveBoneName(definition.bone, modelGroups, bobjBones));
            }
        }

        List<CollisionSphere> collisionSpheres = getCollisionSpheres(model, modelGroups, bobjBones, enabledPhysBones);

        /* Sort definitions by hierarchy depth (parents first) for chain propagation */
        List<PhysBoneDefinition> sorted = sortByHierarchyDepth(expandedDefinitions, modelGroups, bobjBones);

        for (PhysBoneDefinition definition : sorted)
        {
            if (definition == null || definition.bone == null || definition.bone.isEmpty())
            {
                continue;
            }

            String bone = resolveBoneName(definition.bone, modelGroups, bobjBones);

            if (!definition.enabled)
            {
                physStates.remove(bone);

                continue;
            }

            activeBones.add(bone);

            PhysBoneState state = physStates.computeIfAbsent(bone, (key) -> new PhysBoneState());

            /* Lag spike detection: reset smoothly if too much time has passed */
            long nowNanos = System.nanoTime();

            if (state.lastUpdateNanos != 0L && (nowNanos - state.lastUpdateNanos) > LAG_SPIKE_THRESHOLD_NANOS)
            {
                state.yawVelocity *= 0.15F;
                state.pitchVelocity *= 0.15F;
                state.rollVelocity *= 0.15F;
                state.resetMotionHistory();
            }

            state.lastUpdateNanos = nowNanos;

            /* Record and smooth motion */
            state.recordMotion(localStrafe, localForward, verticalVelocity);

            float smoothStrafe = state.getSmoothedStrafe();
            float smoothForward = state.getSmoothedForward();
            float smoothVertical = state.getSmoothedVertical();

            float maxAngle = Math.max(0F, definition.maxAngle);
            float collisionLimit = getMapFloatIgnoreCase(collisionLimits, bone, maxAngle);
            float chainFactor = (float) Math.pow(0.72F, getDynamicChainDepth(bone, modelGroups, enabledPhysBones));
            float safeAngle = Math.max(6F, Math.min(maxAngle, collisionLimit) * chainFactor);
            float inertia = Math.max(0F, definition.inertia);
            float strafeAccel = sanitizeMotion(smoothStrafe - state.prevStrafeMotion);
            float forwardAccel = sanitizeMotion(smoothForward - state.prevForwardMotion);
            float verticalAccel = sanitizeMotion(smoothVertical - state.prevVerticalVelocity);

            /* Chain-aware parent delta: include dynamic parent rotation */
            Vector2f parentDelta = getParentPoseDelta(entity, bone, modelGroups, bobjBones, state);
            float chainParentYaw = getChainParentDynamicYaw(bone, physStates, modelGroups, bobjBones, enabledPhysBones);
            float chainParentPitch = getChainParentDynamicPitch(bone, physStates, modelGroups, bobjBones, enabledPhysBones);

            String parent = getParentBoneName(bone, modelGroups, bobjBones);
            Vector3f parentGravity = getParentPoseGravity(entity, model, bone, modelGroups, bobjBones, definition.gravityDir, definition.localForce);
            float parentYawTurn = sanitizeRotation(parentDelta.x) + chainParentYaw * 0.6F;
            float parentPitchTurn = sanitizeRotation(parentDelta.y) + chainParentPitch * 0.6F;
            float gravityStrength = definition.gravityStrength;
            float gravityYaw = computeGravityTilt(parentGravity.x, parentGravity.y) * gravityStrength;
            float gravityPitch = computeGravityTilt(parentGravity.z, parentGravity.y) * gravityStrength;
            float fallWindPitch = definition.affectPitch ? Math.max(0F, -smoothVertical) * 45F * inertia : 0F;

            state.prevStrafeMotion = smoothStrafe;
            state.prevForwardMotion = smoothForward;
            state.prevVerticalVelocity = smoothVertical;

            float swayYaw = (-strafeAccel * 220F - bodyTurn * 0.9F - parentYawTurn * 1.35F) * inertia + gravityYaw;
            float swayPitch = (definition.affectPitch ? ((forwardAccel * 180F - verticalAccel * 120F - parentPitchTurn * 1.2F) * inertia - fallWindPitch) : 0F) + gravityPitch;
            float targetYaw = MathHelper.clamp(swayYaw, -safeAngle, safeAngle);
            float targetPitch = MathHelper.clamp(swayPitch, -safeAngle, safeAngle);

            /* Anchor attraction force if anchorEnd is specified */
            if (definition.anchorEnd != null && !definition.anchorEnd.isEmpty())
            {
                String resolvedAnchor = resolveBoneName(definition.anchorEnd, modelGroups, bobjBones);
                Vector3f anchorPivot = getBonePivotTransformed(model, resolvedAnchor, physStates, modelGroups, bobjBones, null, 0F, 0F);
                Vector3f bonePivot = getBonePivotTransformed(model, bone, physStates, modelGroups, bobjBones, null, 0F, 0F);

                if (anchorPivot != null && bonePivot != null)
                {
                    Vector3f toAnchor = new Vector3f(anchorPivot).sub(bonePivot);

                    if (toAnchor.lengthSquared() > 0.0001F)
                    {
                        toAnchor.normalize();
                        Matrix3f parentRot = getPoseRotationMatrix(entity, parent, modelGroups, bobjBones);

                        if (parentRot == null)
                        {
                            parentRot = getCurrentRotationMatrix(model, parent, modelGroups, bobjBones);
                        }

                        if (parentRot != null)
                        {
                            Matrix3f invRot = new Matrix3f(parentRot).transpose();
                            Vector3f localToAnchor = invRot.transform(toAnchor).normalize();
                            float anchorYaw = computeGravityTilt(localToAnchor.x, localToAnchor.y);
                            float anchorPitch = computeGravityTilt(localToAnchor.z, localToAnchor.y);

                            targetYaw = MathHelper.lerp(0.35F, targetYaw, anchorYaw);
                            targetPitch = MathHelper.lerp(0.35F, targetPitch, anchorPitch);
                        }
                    }
                }
            }

            /* Roll target: derived from lateral motion and body turning */
            float targetRoll = 0F;

            if (definition.affectRoll)
            {
                float rollFactor = Math.max(0F, definition.rollFactor);
                float rollSway = (strafeAccel * 160F + bodyTurn * 1.2F) * inertia * rollFactor;

                targetRoll = MathHelper.clamp(rollSway, -safeAngle * 0.7F, safeAngle * 0.7F);
            }

            if (!state.initialized)
            {
                state.yaw = targetYaw;
                state.pitch = targetPitch;
                state.roll = targetRoll;
                state.initialized = true;
            }

            float dt = Math.max(0.0001F, (1F / 20F) * Math.max(0.01F, definition.simSpeed));
            float stiffness = Math.max(0F, definition.stiffness);
            float damping = Math.max(0F, definition.damping);
            float prevYaw = state.yaw;
            float prevPitch = state.pitch;

            /* Symplectic Euler with customizable sub-stepping for stability */
            integrateSpring(state, definition, targetYaw, targetPitch, targetRoll, stiffness, damping, definition.gravityStrength, dt, safeAngle);

            if (definition.collisionEnabled)
            {
                solveCollision(entity, model, bone, state, prevYaw, prevPitch, physStates, modelGroups, bobjBones, collisionSpheres, definition.collisionRadius);
            }
        }

        physStates.entrySet().removeIf((entry) -> !activeBones.contains(entry.getKey()));
    }

    private static void integrateSpring(PhysBoneState state, PhysBoneDefinition definition, float targetYaw, float targetPitch, float targetRoll, float stiffness, float damping, float gravity, float dt, float safeAngle)
    {
        int subSteps = Math.max(1, definition.solverSteps);
        float subDt = dt / subSteps;

        float limitMinYaw = definition.limitAngles ? definition.minYaw : -safeAngle;
        float limitMaxYaw = definition.limitAngles ? definition.maxYaw : safeAngle;
        float limitMinPitch = definition.limitAngles ? definition.minPitch : -safeAngle;
        float limitMaxPitch = definition.limitAngles ? definition.maxPitch : safeAngle;
        float limitMinRoll = definition.limitAngles ? definition.minRoll : -safeAngle * 0.7F;
        float limitMaxRoll = definition.limitAngles ? definition.maxRoll : safeAngle * 0.7F;

        for (int step = 0; step < subSteps; step++)
        {
            /* Yaw spring */
            float yawForce = (targetYaw - state.yaw) * stiffness - state.yawVelocity * damping;
            state.yawVelocity += yawForce * subDt;
            state.yaw = MathHelper.clamp(state.yaw + state.yawVelocity * subDt, limitMinYaw, limitMaxYaw);

            /* Pitch spring with gravity bias */
            float pitchForce = (targetPitch - state.pitch) * stiffness - state.pitchVelocity * damping + gravity;
            state.pitchVelocity += pitchForce * subDt;
            state.pitch = MathHelper.clamp(state.pitch + state.pitchVelocity * subDt, limitMinPitch, limitMaxPitch);

            /* Roll spring */
            float rollForce = (targetRoll - state.roll) * stiffness - state.rollVelocity * damping;
            state.rollVelocity += rollForce * subDt;
            state.roll = MathHelper.clamp(state.roll + state.rollVelocity * subDt, limitMinRoll, limitMaxRoll);
        }

        state.yaw = MathHelper.clamp(state.yaw, limitMinYaw, limitMaxYaw);
        state.pitch = MathHelper.clamp(state.pitch, limitMinPitch, limitMaxPitch);
        state.roll = MathHelper.clamp(state.roll, limitMinRoll, limitMaxRoll);
    }

    /**
     * Tethers/tracks parent-to-child bone path for auto chains.
     */
    private static List<String> getChainPath(String startBone, String endBone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        List<String> path = new ArrayList<>();
        String current = endBone;

        while (current != null && !current.equals(startBone))
        {
            path.add(current);
            current = getParentBoneName(current, groupsById, bobjBones);
        }

        if (current != null && current.equals(startBone))
        {
            path.add(startBone);
            Collections.reverse(path);

            return path;
        }

        return null;
    }

    /**
     * Sort definitions by bone hierarchy depth so parent dynamic bones
     * are processed before their children, enabling proper chain propagation.
     */
    private static List<PhysBoneDefinition> sortByHierarchyDepth(List<PhysBoneDefinition> definitions, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        if (definitions.size() <= 1)
        {
            return definitions;
        }

        List<PhysBoneDefinition> sorted = new ArrayList<>(definitions);

        sorted.sort((a, b) ->
        {
            int depthA = getBoneDepth(a.bone, groupsById, bobjBones);
            int depthB = getBoneDepth(b.bone, groupsById, bobjBones);

            return Integer.compare(depthA, depthB);
        });

        return sorted;
    }

    private static int getBoneDepth(String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        if (bone == null || bone.isEmpty())
        {
            return 0;
        }

        ModelGroup group = findModelGroup(groupsById, bone);

        if (group != null)
        {
            int depth = 0;
            ModelGroup current = group;

            while (current.parent != null)
            {
                depth++;
                current = current.parent;
            }

            return depth;
        }

        BOBJBone bobjBone = findBOBJBone(bobjBones, bone);

        if (bobjBone != null)
        {
            int depth = 0;
            BOBJBone current = bobjBone;

            while (current.parentBone != null)
            {
                depth++;
                current = current.parentBone;
            }

            return depth;
        }

        return 0;
    }

    /**
     * Get dynamic yaw rotation from parent bone in the physics chain.
     * Used for chain propagation — child bones inherit parent motion.
     */
    private static float getChainParentDynamicYaw(String bone, Map<String, PhysBoneState> physStates, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, Set<String> dynamicBones)
    {
        String parent = getParentBoneName(bone, groupsById, bobjBones);

        if (parent == null || !dynamicBones.contains(parent))
        {
            return 0F;
        }

        PhysBoneState parentState = getPhysState(physStates, parent);

        return parentState == null ? 0F : parentState.yaw;
    }

    /**
     * Get dynamic pitch rotation from parent bone in the physics chain.
     */
    private static float getChainParentDynamicPitch(String bone, Map<String, PhysBoneState> physStates, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, Set<String> dynamicBones)
    {
        String parent = getParentBoneName(bone, groupsById, bobjBones);

        if (parent == null || !dynamicBones.contains(parent))
        {
            return 0F;
        }

        PhysBoneState parentState = getPhysState(physStates, parent);

        return parentState == null ? 0F : parentState.pitch;
    }

    public static void apply(IModel model, Map<String, PhysBoneState> physStates)
    {
        if (model == null || physStates.isEmpty())
        {
            return;
        }

        if (model instanceof Model)
        {
            for (ModelGroup group : model.getAllGroups())
            {
                PhysBoneState state = getPhysState(physStates, group.id);

                if (state != null)
                {
                    group.current.rotate.y += state.yaw;
                    group.current.rotate.x += state.pitch;
                    group.current.rotate.z += state.roll;
                }
            }
        }
        else
        {
            for (BOBJBone bone : model.getAllBOBJBones())
            {
                PhysBoneState state = getPhysState(physStates, bone.name);

                if (state != null)
                {
                    bone.transform.rotate.y += MathUtils.toRad(state.yaw);
                    bone.transform.rotate.x += MathUtils.toRad(state.pitch);
                    bone.transform.rotate.z += MathUtils.toRad(state.roll);
                }
            }
        }
    }

    private static Map<String, Float> getPhysCollisionLimits(IModel model, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        Map<String, Float> limits = new HashMap<>();

        if (model instanceof Model)
        {
            ModelGroup torso = findCollisionRoot(model);
            ModelGroup head = findHeadCollisionRoot(model);

            for (ModelGroup group : model.getAllGroups())
            {
                if (torso != null && isSoftBodyCandidate(group))
                {
                    Vector3f delta = new Vector3f(group.initial.pivot).sub(torso.initial.pivot);
                    float horizontal = Math.max(Math.abs(delta.x), Math.abs(delta.z));
                    float vertical = Math.abs(delta.y);
                    float limit = 18F + horizontal * 3F + vertical * 0.2F;

                    limits.put(group.id, MathHelper.clamp(limit, 12F, 45F));
                }
                else if (head != null && isHairCandidate(group))
                {
                    int headDepth = getHierarchyDepth(group, head);

                    if (headDepth >= 0)
                    {
                        Vector3f delta = new Vector3f(group.initial.pivot).sub(head.initial.pivot);
                        float spread = Math.abs(delta.x) + Math.abs(delta.z);
                        float vertical = Math.abs(delta.y);
                        float limit = 20F + spread * 1.2F + vertical * 0.2F - headDepth * 5F;

                        limits.put(group.id, MathHelper.clamp(limit, 8F, 28F));
                    }
                }
                else if (torso != null && isBodyCollisionCandidate(group, torso))
                {
                    int torsoDepth = getHierarchyDepth(group, torso);
                    Vector3f delta = new Vector3f(group.initial.pivot).sub(torso.initial.pivot);
                    float spread = Math.abs(delta.x) + Math.abs(delta.z);
                    float vertical = Math.abs(delta.y);
                    float limit = 13F + spread * 1.4F + vertical * 0.15F - torsoDepth * 2.25F;

                    limits.put(group.id, MathHelper.clamp(limit, 8F, 24F));
                }
            }
        }
        else if (!bobjBones.isEmpty())
        {
            for (BOBJBone bone : bobjBones.values())
            {
                String id = bone.name.toLowerCase();

                if (isHairName(id))
                {
                    limits.put(bone.name, 24F);
                }
                else if (isBOBJBodyCollisionCandidate(bone))
                {
                    limits.put(bone.name, 16F);
                }
            }
        }

        return limits;
    }

    private static Map<String, ModelGroup> getModelGroupsById(IModel model)
    {
        Map<String, ModelGroup> groups = new HashMap<>();

        if (!(model instanceof Model))
        {
            return groups;
        }

        for (ModelGroup group : model.getAllGroups())
        {
            groups.put(group.id, group);
        }

        return groups;
    }

    private static Map<String, BOBJBone> getBOBJBonesById(IModel model)
    {
        Map<String, BOBJBone> bones = new HashMap<>();

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            bones.put(bone.name, bone);
        }

        return bones;
    }

    private static Vector2f getParentPoseDelta(IEntity entity, String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, PhysBoneState state)
    {
        String parent = getParentBoneName(bone, groupsById, bobjBones);

        if (parent == null || parent.isEmpty())
        {
            state.parentInitialized = false;

            return new Vector2f();
        }

        Vector2f parentRotation = getPoseRotation(entity, parent, groupsById, bobjBones);

        if (parentRotation == null)
        {
            state.parentInitialized = false;

            return new Vector2f();
        }

        if (!state.parentInitialized)
        {
            state.prevParentYaw = parentRotation.x;
            state.prevParentPitch = parentRotation.y;
            state.parentInitialized = true;

            return new Vector2f();
        }

        float yawTurn = MathHelper.wrapDegrees(parentRotation.x - state.prevParentYaw);
        float pitchTurn = MathHelper.wrapDegrees(parentRotation.y - state.prevParentPitch);

        state.prevParentYaw = parentRotation.x;
        state.prevParentPitch = parentRotation.y;

        return new Vector2f(yawTurn, pitchTurn);
    }

    private static String getParentBoneName(String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        ModelGroup modelGroup = findModelGroup(groupsById, bone);

        if (modelGroup != null && modelGroup.parent != null)
        {
            return modelGroup.parent.id;
        }

        BOBJBone bobjBone = findBOBJBone(bobjBones, bone);

        if (bobjBone != null && bobjBone.parentBone != null)
        {
            return bobjBone.parentBone.name;
        }

        return null;
    }

    private static Vector3f getParentPoseGravity(IEntity entity, IModel model, String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, Vector3f gravityDir, boolean localForce)
    {
        if (localForce)
        {
            return new Vector3f(gravityDir).normalize();
        }

        String parent = getParentBoneName(bone, groupsById, bobjBones);

        if (parent == null || parent.isEmpty())
        {
            return new Vector3f(gravityDir).normalize();
        }

        Matrix3f rotation = getPoseRotationMatrix(entity, parent, groupsById, bobjBones);

        if (rotation == null)
        {
            rotation = getCurrentRotationMatrix(model, parent, groupsById, bobjBones);
        }

        if (rotation == null)
        {
            return new Vector3f(gravityDir).normalize();
        }

        Matrix3f inverseRotation = new Matrix3f(rotation).transpose();
        Vector3f localGravity = inverseRotation.transform(new Vector3f(gravityDir)).normalize();

        return localGravity;
    }

    private static Vector2f getPoseRotation(IEntity entity, String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        Form form = entity == null ? null : entity.getForm();

        if (form == null)
        {
            return null;
        }

        Vector2f totalRotation = new Vector2f();
        Set<String> visited = new HashSet<>();
        String current = bone;
        boolean hasTransform = false;

        while (current != null && visited.add(current))
        {
            PoseTransform transform = getCombinedPoseTransform(form, current);
            boolean hasCurrentTransform = transform != null;

            if (hasCurrentTransform)
            {
                hasTransform = true;
                totalRotation.x += MathUtils.toDeg(transform.rotate.y + transform.rotate2.y);
                totalRotation.y += MathUtils.toDeg(transform.rotate.x + transform.rotate2.x);
            }

            current = getParentBoneName(current, groupsById, bobjBones);
        }

        if (!hasTransform)
        {
            return null;
        }

        return totalRotation;
    }

    private static Matrix3f getPoseRotationMatrix(IEntity entity, String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        Form form = entity == null ? null : entity.getForm();

        if (form == null)
        {
            return null;
        }

        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = bone;
        Matrix3f matrix = new Matrix3f().identity();
        boolean hasTransform = false;

        while (current != null && visited.add(current))
        {
            chain.add(current);
            current = getParentBoneName(current, groupsById, bobjBones);
        }

        for (int i = chain.size() - 1; i >= 0; i--)
        {
            PoseTransform transform = getCombinedPoseTransform(form, chain.get(i));

            if (transform != null)
            {
                hasTransform = true;
                matrix.mul(transform.createRotationMatrix());
            }
        }

        return hasTransform ? matrix : null;
    }

    private static Matrix3f getCurrentRotationMatrix(IModel model, String bone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        if (model == null || bone == null || bone.isEmpty())
        {
            return null;
        }

        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = bone;
        Matrix3f matrix = new Matrix3f().identity();
        boolean hasRotation = false;

        while (current != null && visited.add(current))
        {
            chain.add(current);
            current = getParentBoneName(current, groupsById, bobjBones);
        }

        for (int i = chain.size() - 1; i >= 0; i--)
        {
            String boneName = chain.get(i);
            ModelGroup group = findModelGroup(groupsById, boneName);

            if (group != null)
            {
                hasRotation |= applyModelRotation(matrix, group);
                continue;
            }

            BOBJBone bobjBone = findBOBJBone(bobjBones, boneName);

            if (bobjBone != null)
            {
                hasRotation |= applyBOBJRotation(matrix, bobjBone);
            }
        }

        return hasRotation ? matrix : null;
    }

    private static PoseTransform getCombinedPoseTransform(Form form, String bone)
    {
        PoseTransform transform = new PoseTransform();
        boolean hasTransform = false;

        hasTransform |= applyPoseRotation(form, "pose", bone, transform);
        hasTransform |= applyPoseRotation(form, "pose_overlay", bone, transform);

        if (form instanceof ModelForm modelForm)
        {
            for (ValuePose overlay : modelForm.additionalOverlays)
            {
                Pose pose = overlay.get();

                if (pose != null)
                {
                    PoseTransform entry = findPoseTransform(pose, bone);

                    if (entry != null)
                    {
                        hasTransform = true;
                        mergePoseRotation(transform, entry);
                    }
                }
            }
        }

        return hasTransform ? transform : null;
    }

    private static boolean applyPoseRotation(Form form, String propertyId, String bone, PoseTransform target)
    {
        BaseValueBasic property = form.getAllMap().get(propertyId);

        if (!(property instanceof ValuePose valuePose))
        {
            return false;
        }

        Pose pose = valuePose.get();

        if (pose == null)
        {
            return false;
        }

        PoseTransform entry = findPoseTransform(pose, bone);

        if (entry == null)
        {
            return false;
        }

        mergePoseRotation(target, entry);

        return true;
    }

    private static PoseTransform findPoseTransform(Pose pose, String bone)
    {
        PoseTransform entry = pose.transforms.get(bone);

        if (entry != null)
        {
            return entry;
        }

        for (Map.Entry<String, PoseTransform> transformEntry : pose.transforms.entrySet())
        {
            if (transformEntry.getKey().equalsIgnoreCase(bone))
            {
                return transformEntry.getValue();
            }
        }

        return null;
    }

    private static void mergePoseRotation(PoseTransform target, PoseTransform value)
    {
        if (value.fix != 0F)
        {
            target.rotate.lerp(value.rotate, value.fix);
            target.rotate2.lerp(value.rotate2, value.fix);
        }
        else
        {
            target.rotate.add(value.rotate);
            target.rotate2.add(value.rotate2);
        }
    }

    private static float sanitizeMotion(float value)
    {
        return Math.abs(value) < MOTION_EPSILON ? 0F : value;
    }

    private static float sanitizeRotation(float value)
    {
        return Math.abs(value) < ROTATION_EPSILON ? 0F : value;
    }

    private static float computeGravityTilt(float axis, float downY)
    {
        float tilt = MathUtils.toDeg((float) Math.atan2(axis, -downY));

        return MathHelper.clamp(tilt * 1.4F, -55F, 55F);
    }

    private static List<CollisionSphere> getCollisionSpheres(IModel model, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, Set<String> dynamicBones)
    {
        List<CollisionSphere> spheres = new ArrayList<>();

        if (model instanceof Model)
        {
            ModelGroup torso = findCollisionRoot(model);
            ModelGroup head = findHeadCollisionRoot(model);

            if (torso != null)
            {
                float radius = estimateModelRootRadius(torso, groupsById, dynamicBones, 5.5F, 4F, 13F);

                spheres.add(new CollisionSphere(torso.id, radius));
            }

            if (head != null)
            {
                float radius = estimateModelRootRadius(head, groupsById, dynamicBones, 4.25F, 3F, 9F);

                spheres.add(new CollisionSphere(head.id, radius));
            }
        }
        else if (!bobjBones.isEmpty())
        {
            BOBJBone torso = null;
            BOBJBone head = null;

            for (BOBJBone bone : bobjBones.values())
            {
                String id = bone.name.toLowerCase();

                if (torso == null && isTorsoRootName(id))
                {
                    torso = bone;
                }

                if (head == null && (id.equals("head") || id.contains("head")))
                {
                    head = bone;
                }
            }

            if (torso != null)
            {
                float radius = estimateBOBJRootRadius(torso, bobjBones, dynamicBones, 5.5F, 4F, 13F);

                spheres.add(new CollisionSphere(torso.name, radius));
            }

            if (head != null)
            {
                float radius = estimateBOBJRootRadius(head, bobjBones, dynamicBones, 4.25F, 3F, 9F);

                spheres.add(new CollisionSphere(head.name, radius));
            }
        }

        return spheres;
    }

    private static float estimateModelRootRadius(ModelGroup root, Map<String, ModelGroup> groupsById, Set<String> dynamicBones, float fallback, float min, float max)
    {
        float radius = fallback;

        for (ModelGroup group : groupsById.values())
        {
            if (dynamicBones.contains(group.id))
            {
                continue;
            }

            int depth = getHierarchyDepth(group, root);

            if (depth < 0 || depth > 2)
            {
                continue;
            }

            String id = group.id.toLowerCase();

            if (isExcludedBodyPart(id) || isBodyAppendageName(id) || isHairName(id))
            {
                continue;
            }

            Vector3f delta = new Vector3f(group.initial.pivot).sub(root.initial.pivot);
            float horizontal = (float) Math.sqrt(delta.x * delta.x + delta.z * delta.z);

            radius = Math.max(radius, horizontal + Math.abs(delta.y) * 0.2F + 1.1F);
        }

        return MathHelper.clamp(radius, min, max);
    }

    private static float estimateBOBJRootRadius(BOBJBone root, Map<String, BOBJBone> bobjBones, Set<String> dynamicBones, float fallback, float min, float max)
    {
        float radius = fallback;
        Vector3f rootPivot = root.boneMat.getTranslation(new Vector3f());

        for (BOBJBone bone : bobjBones.values())
        {
            if (dynamicBones.contains(bone.name))
            {
                continue;
            }

            int depth = getHierarchyDepth(bone, root);

            if (depth < 0 || depth > 2)
            {
                continue;
            }

            String id = bone.name.toLowerCase();

            if (isExcludedBodyPart(id) || isBodyAppendageName(id) || isHairName(id))
            {
                continue;
            }

            Vector3f delta = bone.boneMat.getTranslation(new Vector3f()).sub(rootPivot);
            float horizontal = (float) Math.sqrt(delta.x * delta.x + delta.z * delta.z);

            radius = Math.max(radius, horizontal + Math.abs(delta.y) * 0.2F + 1.1F);
        }

        return MathHelper.clamp(radius, min, max);
    }

    private static void solveCollision(IEntity entity, IModel model, String bone, PhysBoneState state, float prevYaw, float prevPitch, Map<String, PhysBoneState> physStates, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, List<CollisionSphere> collisionSpheres, float collisionRadius)
    {
        String parent = getParentBoneName(bone, groupsById, bobjBones);

        if (parent == null || parent.isEmpty())
        {
            return;
        }

        boolean prevCollides = collides(entity, model, bone, parent, prevYaw, prevPitch, physStates, groupsById, bobjBones, collisionSpheres, collisionRadius);
        boolean currentCollides = collides(entity, model, bone, parent, state.yaw, state.pitch, physStates, groupsById, bobjBones, collisionSpheres, collisionRadius);

        if (!currentCollides)
        {
            return;
        }

        if (prevCollides)
        {
            float resolvedYaw = state.yaw;
            float resolvedPitch = state.pitch;
            boolean solved = false;

            for (int i = 0; i < 8; i++)
            {
                resolvedYaw *= 0.55F;
                resolvedPitch *= 0.55F;

                if (!collides(entity, model, bone, parent, resolvedYaw, resolvedPitch, physStates, groupsById, bobjBones, collisionSpheres, collisionRadius))
                {
                    solved = true;
                    break;
                }
            }

            if (!solved)
            {
                resolvedYaw = 0F;
                resolvedPitch = 0F;
            }

            state.yaw = resolvedYaw;
            state.pitch = resolvedPitch;
            state.yawVelocity *= 0.2F;
            state.pitchVelocity *= 0.2F;

            return;
        }

        float low = 0F;
        float high = 1F;

        for (int i = 0; i < 6; i++)
        {
            float mid = (low + high) * 0.5F;
            float yaw = MathHelper.lerp(mid, prevYaw, state.yaw);
            float pitch = MathHelper.lerp(mid, prevPitch, state.pitch);

            if (collides(entity, model, bone, parent, yaw, pitch, physStates, groupsById, bobjBones, collisionSpheres, collisionRadius))
            {
                high = mid;
            }
            else
            {
                low = mid;
            }
        }

        state.yaw = MathHelper.lerp(low, prevYaw, state.yaw);
        state.pitch = MathHelper.lerp(low, prevPitch, state.pitch);
        state.yawVelocity *= 0.35F;
        state.pitchVelocity *= 0.35F;
    }

    private static boolean collides(IEntity entity, IModel model, String bone, String parent, float yaw, float pitch, Map<String, PhysBoneState> physStates, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, List<CollisionSphere> collisionSpheres, float collisionRadius)
    {
        Vector3f parentPivot = getBonePivotTransformed(model, parent, physStates, groupsById, bobjBones, null, 0F, 0F);
        Vector3f tip = getBonePivotTransformed(model, bone, physStates, groupsById, bobjBones, bone, yaw, pitch);

        if (parentPivot == null || tip == null)
        {
            return false;
        }

        for (CollisionSphere sphere : collisionSpheres)
        {
            Vector3f center = getBonePivotTransformed(model, sphere.anchorBone, physStates, groupsById, bobjBones, bone, yaw, pitch);
            float totalRadius = sphere.radius + Math.max(0.01F, collisionRadius);

            if (center != null && tip.distanceSquared(center) < totalRadius * totalRadius)
            {
                return true;
            }
        }

        World world = entity == null ? null : entity.getWorld();

        if (world == null)
        {
            return false;
        }

        Vec3d worldParent = toWorldSpace(entity, parentPivot);
        Vec3d worldTip = toWorldSpace(entity, tip);
        float thickness = Math.max(0.01F, collisionRadius);

        return hasEnvironmentCollision(world, worldParent, worldTip, thickness);
    }

    private static Vector3f getBonePivotTransformed(IModel model, String bone, Map<String, PhysBoneState> physStates, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, String overrideBone, float overrideYaw, float overridePitch)
    {
        if (bone == null || bone.isEmpty())
        {
            return null;
        }

        if (model instanceof Model)
        {
            return getModelBonePivotTransformed(bone, physStates, groupsById, bobjBones, overrideBone, overrideYaw, overridePitch);
        }

        BOBJBone bobjBone = findBOBJBone(bobjBones, bone);

        return bobjBone == null ? null : bobjBone.boneMat.getTranslation(new Vector3f());
    }

    private static Vector3f getModelBonePivotTransformed(String bone, Map<String, PhysBoneState> physStates, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones, String overrideBone, float overrideYaw, float overridePitch)
    {
        ModelGroup group = findModelGroup(groupsById, bone);

        if (group == null)
        {
            return null;
        }

        List<ModelGroup> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        ModelGroup current = group;

        while (current != null && visited.add(current.id))
        {
            chain.add(current);
            current = current.parent;
        }

        Matrix4f matrix = new Matrix4f().identity();

        for (int i = chain.size() - 1; i >= 0; i--)
        {
            ModelGroup node = chain.get(i);
            float extraYaw = 0F;
            float extraPitch = 0F;

            if (overrideBone != null && overrideBone.equalsIgnoreCase(node.id))
            {
                extraYaw = overrideYaw;
                extraPitch = overridePitch;
            }
            else
            {
                extraYaw = getPhysYaw(physStates, node.id);
                extraPitch = getPhysPitch(physStates, node.id);
            }

            applyModelGroupMatrix(matrix, node, extraYaw, extraPitch);
        }

        return matrix.transformPosition(new Vector3f(group.current.pivot));
    }

    private static Vec3d toWorldSpace(IEntity entity, Vector3f local)
    {
        float yaw = entity.getBodyYaw() + 180F;
        double sin = MathHelper.sin(MathUtils.toRad(yaw));
        double cos = MathHelper.cos(MathUtils.toRad(yaw));
        double x = (local.x * cos - local.z * sin) / 16D;
        double z = (local.x * sin + local.z * cos) / 16D;
        double y = local.y / 16D;

        return new Vec3d(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
    }

    private static void applyModelGroupMatrix(Matrix4f matrix, ModelGroup group, float extraYaw, float extraPitch)
    {
        Vector3f translate = group.current.translate;
        Vector3f pivot = group.current.pivot;

        matrix.translate(-(translate.x - pivot.x), (translate.y - pivot.y), (translate.z - pivot.z));
        matrix.translate(pivot.x, pivot.y, pivot.z);

        float z = group.current.rotate.z + group.current.rotate2.z;
        float y = group.current.rotate.y + group.current.rotate2.y + extraYaw;
        float x = group.current.rotate.x + group.current.rotate2.x + extraPitch;

        if (z != 0F) matrix.rotateZ(MathUtils.toRad(z));
        if (y != 0F) matrix.rotateY(MathUtils.toRad(y));
        if (x != 0F) matrix.rotateX(MathUtils.toRad(x));

        Vector3f scale = group.current.scale;

        matrix.scale(scale.x, scale.y, scale.z);
        matrix.translate(-pivot.x, -pivot.y, -pivot.z);
    }

    private static float getPhysYaw(Map<String, PhysBoneState> physStates, String bone)
    {
        PhysBoneState state = getPhysState(physStates, bone);

        return state == null ? 0F : state.yaw;
    }

    private static float getPhysPitch(Map<String, PhysBoneState> physStates, String bone)
    {
        PhysBoneState state = getPhysState(physStates, bone);

        return state == null ? 0F : state.pitch;
    }

    private static boolean hasEnvironmentCollision(World world, Vec3d from, Vec3d to, float radius)
    {
        if (world == null || from == null || to == null || from.squaredDistanceTo(to) < 0.00001D)
        {
            return false;
        }

        Vec3d delta = to.subtract(from);
        Vec3d adjusted = Entity.adjustMovementForCollisions(null, delta, new Box(
            from.x - radius,
            from.y - radius,
            from.z - radius,
            from.x + radius,
            from.y + radius,
            from.z + radius
        ), world, Collections.emptyList());

        if (adjusted.squaredDistanceTo(delta) > 0.00000025D)
        {
            return true;
        }

        double length = Math.sqrt(from.squaredDistanceTo(to));
        int steps = MathHelper.clamp((int) Math.ceil(length / Math.max(0.02D, radius * 0.45D)), 3, 26);

        for (int i = 0; i <= steps; i++)
        {
            double factor = i / (double) steps;
            Vec3d point = from.add(delta.multiply(factor));

            if (intersectsWorldCollision(world, point, radius))
            {
                return true;
            }
        }

        HitResult result = world.raycast(new RaycastContext(
            from,
            to,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            null
        ));

        return result != null && result.getType() != HitResult.Type.MISS;
    }

    private static boolean intersectsWorldCollision(World world, Vec3d point, float radius)
    {
        int minX = MathHelper.floor(point.x - radius) - 1;
        int maxX = MathHelper.floor(point.x + radius) + 1;
        int minY = MathHelper.floor(point.y - radius) - 1;
        int maxY = MathHelper.floor(point.y + radius) + 1;
        int minZ = MathHelper.floor(point.z - radius) - 1;
        int maxZ = MathHelper.floor(point.z + radius) + 1;
        ShapeContext shape = ShapeContext.absent();
        double radiusSq = radius * radius;

        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (state.isAir())
                    {
                        continue;
                    }

                    VoxelShape collision = state.getCollisionShape(world, pos, shape);

                    if (collision == null || collision.isEmpty())
                    {
                        continue;
                    }

                    for (Box box : collision.getBoundingBoxes())
                    {
                        Box worldBox = box.offset(pos);
                        double nx = MathHelper.clamp(point.x, worldBox.minX, worldBox.maxX);
                        double ny = MathHelper.clamp(point.y, worldBox.minY, worldBox.maxY);
                        double nz = MathHelper.clamp(point.z, worldBox.minZ, worldBox.maxZ);
                        double dx = point.x - nx;
                        double dy = point.y - ny;
                        double dz = point.z - nz;

                        if (dx * dx + dy * dy + dz * dz <= radiusSq)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static int getHierarchyDepth(BOBJBone bone, BOBJBone ancestor)
    {
        int depth = 0;
        BOBJBone current = bone;

        while (current != null)
        {
            if (current == ancestor)
            {
                return depth;
            }

            current = current.parentBone;
            depth += 1;
        }

        return -1;
    }

    private static class CollisionSphere
    {
        public final String anchorBone;
        public final float radius;

        public CollisionSphere(String anchorBone, float radius)
        {
            this.anchorBone = anchorBone;
            this.radius = radius;
        }
    }

    private static ModelGroup findCollisionRoot(IModel model)
    {
        ModelGroup fallback = null;

        for (ModelGroup group : model.getAllGroups())
        {
            String id = group.id.toLowerCase();

            if (id.equals("torso") || id.equals("body") || id.contains("chest"))
            {
                return group;
            }

            if (fallback == null && id.equals("low_body"))
            {
                fallback = group;
            }
        }

        return fallback;
    }

    private static ModelGroup findHeadCollisionRoot(IModel model)
    {
        ModelGroup fallback = null;

        for (ModelGroup group : model.getAllGroups())
        {
            String id = group.id.toLowerCase();

            if (id.equals("head") || id.contains("head"))
            {
                return group;
            }

            if (fallback == null && id.contains("neck"))
            {
                fallback = group;
            }
        }

        return fallback;
    }

    private static boolean isSoftBodyCandidate(ModelGroup group)
    {
        String id = group.id.toLowerCase();

        if (id.contains("breast") || id.contains("boob") || id.contains("chest"))
        {
            return true;
        }

        if (group.parent == null)
        {
            return false;
        }

        String parentId = group.parent.id.toLowerCase();

        return parentId.contains("torso") || parentId.contains("body") || parentId.contains("chest");
    }

    private static boolean isHairCandidate(ModelGroup group)
    {
        String id = group.id.toLowerCase();

        if (
            id.contains("hair") ||
            id.contains("bang") ||
            id.contains("fringe") ||
            id.contains("strand") ||
            id.contains("lock") ||
            id.contains("ponytail")
        )
        {
            return true;
        }

        if (group.parent == null)
        {
            return false;
        }

        String parentId = group.parent.id.toLowerCase();

        return parentId.contains("hair") || parentId.contains("bang") || parentId.contains("head");
    }

    private static boolean isBodyCollisionCandidate(ModelGroup group, ModelGroup torso)
    {
        int depth = getHierarchyDepth(group, torso);

        if (depth <= 0)
        {
            return false;
        }

        String id = group.id.toLowerCase();

        if (isExcludedBodyPart(id))
        {
            return false;
        }

        if (isBodyAppendageName(id))
        {
            return true;
        }

        ModelGroup current = group.parent;

        while (current != null && current != torso)
        {
            String parentId = current.id.toLowerCase();

            if (isExcludedBodyPart(parentId))
            {
                return false;
            }

            if (isBodyAppendageName(parentId))
            {
                return true;
            }

            current = current.parent;
        }

        return depth <= 3;
    }

    private static boolean isBOBJBodyCollisionCandidate(BOBJBone bone)
    {
        String id = bone.name.toLowerCase();

        if (isExcludedBodyPart(id))
        {
            return false;
        }

        if (isBodyAppendageName(id))
        {
            return true;
        }

        int depth = 0;
        BOBJBone current = bone.parentBone;

        while (current != null)
        {
            String parentId = current.name.toLowerCase();

            if (isExcludedBodyPart(parentId))
            {
                return false;
            }

            if (isBodyAppendageName(parentId))
            {
                return true;
            }

            if (isTorsoRootName(parentId))
            {
                return depth <= 3;
            }

            current = current.parentBone;
            depth += 1;
        }

        return false;
    }

    private static boolean isExcludedBodyPart(String id)
    {
        return id.contains("arm")
            || id.contains("leg")
            || id.contains("head")
            || id.contains("neck")
            || id.contains("hand")
            || id.contains("foot")
            || id.contains("finger");
    }

    private static boolean isBodyAppendageName(String id)
    {
        return id.contains("tail")
            || id.contains("skirt")
            || id.contains("cape")
            || id.contains("cloth")
            || id.contains("ribbon")
            || id.contains("strap");
    }

    private static boolean isHairName(String id)
    {
        return id.contains("hair")
            || id.contains("bang")
            || id.contains("fringe")
            || id.contains("strand")
            || id.contains("lock")
            || id.contains("ponytail");
    }

    private static boolean isTorsoRootName(String id)
    {
        return id.equals("torso")
            || id.equals("body")
            || id.contains("chest")
            || id.contains("spine")
            || id.contains("waist")
            || id.contains("pelvis")
            || id.contains("hip");
    }

    private static int getHierarchyDepth(ModelGroup group, ModelGroup ancestor)
    {
        int depth = 0;
        ModelGroup current = group;

        while (current != null)
        {
            if (current == ancestor)
            {
                return depth;
            }

            current = current.parent;
            depth += 1;
        }

        return -1;
    }

    private static int getDynamicChainDepth(String bone, Map<String, ModelGroup> groupsById, Set<String> dynamicBones)
    {
        ModelGroup group = findModelGroup(groupsById, bone);
        int depth = 0;

        while (group != null && group.parent != null)
        {
            group = group.parent;

            if (group != null && dynamicBones.contains(group.id))
            {
                depth += 1;
            }
        }

        return depth;
    }

    private static ModelGroup findModelGroup(Map<String, ModelGroup> groupsById, String bone)
    {
        ModelGroup group = groupsById.get(bone);

        if (group != null)
        {
            return group;
        }

        for (Map.Entry<String, ModelGroup> entry : groupsById.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(bone))
            {
                return entry.getValue();
            }
        }

        return null;
    }

    private static BOBJBone findBOBJBone(Map<String, BOBJBone> bobjBones, String bone)
    {
        BOBJBone bobjBone = bobjBones.get(bone);

        if (bobjBone != null)
        {
            return bobjBone;
        }

        for (Map.Entry<String, BOBJBone> entry : bobjBones.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(bone))
            {
                return entry.getValue();
            }
        }

        return null;
    }

    private static float getMapFloatIgnoreCase(Map<String, Float> values, String key, float fallback)
    {
        Float value = values.get(key);

        if (value != null)
        {
            return value;
        }

        for (Map.Entry<String, Float> entry : values.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(key))
            {
                return entry.getValue();
            }
        }

        return fallback;
    }

    private static String resolveBoneName(String requestedBone, Map<String, ModelGroup> groupsById, Map<String, BOBJBone> bobjBones)
    {
        if (requestedBone == null || requestedBone.isEmpty())
        {
            return requestedBone;
        }

        ModelGroup group = findModelGroup(groupsById, requestedBone);

        if (group != null)
        {
            return group.id;
        }

        BOBJBone bone = findBOBJBone(bobjBones, requestedBone);

        if (bone != null)
        {
            return bone.name;
        }

        return requestedBone;
    }

    private static PhysBoneState getPhysState(Map<String, PhysBoneState> physStates, String bone)
    {
        PhysBoneState state = physStates.get(bone);

        if (state != null)
        {
            return state;
        }

        for (Map.Entry<String, PhysBoneState> entry : physStates.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(bone))
            {
                return entry.getValue();
            }
        }

        return null;
    }

    private static boolean applyModelRotation(Matrix3f matrix, ModelGroup group)
    {
        float z = MathUtils.toRad(group.current.rotate.z + group.current.rotate2.z);
        float y = MathUtils.toRad(group.current.rotate.y + group.current.rotate2.y);
        float x = MathUtils.toRad(group.current.rotate.x + group.current.rotate2.x);

        if (z == 0F && y == 0F && x == 0F)
        {
            return false;
        }

        if (z != 0F) matrix.rotateZ(z);
        if (y != 0F) matrix.rotateY(y);
        if (x != 0F) matrix.rotateX(x);

        return true;
    }

    private static boolean applyBOBJRotation(Matrix3f matrix, BOBJBone bone)
    {
        float z = bone.transform.rotate.z + bone.transform.rotate2.z;
        float y = bone.transform.rotate.y + bone.transform.rotate2.y;
        float x = bone.transform.rotate.x + bone.transform.rotate2.x;

        if (z == 0F && y == 0F && x == 0F)
        {
            return false;
        }

        if (z != 0F) matrix.rotateZ(z);
        if (y != 0F) matrix.rotateY(y);
        if (x != 0F) matrix.rotateX(x);

        return true;
    }
}
