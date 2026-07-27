package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.joml.QuaternionMath;
import net.minecraft.class_287;
import net.minecraft.class_4587;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CubicRenderer
{
    public static final List<String> STENCIL_PICK_PRIORITY_BONES = Arrays.asList(
        "low_body", "head", "headwear", "armor_helmet",
        "left_arm", "right_arm", "left_leg", "right_leg",
        "left_arm_item", "right_arm_item"
    );

    /**
     * Process/render given model
     *
     * This method recursively goes through all groups in the model, and
     * applies given render processor. Processor may return true from its
     * sole method which means that iteration should be halted.
     */
    public static boolean processRenderModel(ICubicRenderer renderProcessor, class_287 builder, class_4587 stack, Model model)
    {
        for (ModelGroup group : model.topGroups)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, group))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Re-render specific groups after the main pass so coplanar / z-tied stencil picks
     * prefer these bones over parents/siblings drawn earlier (e.g. low_body over torso).
     * Depth testing stays on: closer geometry (e.g. head in front of torso) must keep winning.
     */
    public static void renderStencilPickPriority(ICubicRenderer renderProcessor, class_287 builder, class_4587 stack, Model model, Collection<String> boneIds)
    {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        for (String boneId : boneIds)
        {
            ModelGroup group = model.getGroup(boneId);

            if (group != null)
            {
                renderGroupBranch(renderProcessor, builder, stack, model, group);
            }
        }
    }

    public static void renderGroupBranch(ICubicRenderer renderProcessor, class_287 builder, class_4587 stack, Model model, ModelGroup target)
    {
        List<ModelGroup> path = new ArrayList<>();
        ModelGroup current = target;

        while (current != null)
        {
            path.add(0, current);
            current = current.parent;
        }

        for (ModelGroup group : path)
        {
            stack.method_22903();
            renderProcessor.applyGroupTransformations(stack, group);
        }

        if (target.visible)
        {
            renderProcessor.renderGroup(builder, stack, target, model);
        }

        for (int i = 0, c = path.size(); i < c; i++)
        {
            stack.method_22909();
        }
    }

    /**
     * Apply the render processor, recursively
     */
    private static boolean processRenderRecursively(ICubicRenderer renderProcessor, class_287 builder, class_4587 stack, Model model, ModelGroup group)
    {
        stack.method_22903();
        renderProcessor.applyGroupTransformations(stack, group);

        if (group.visible)
        {
            if (renderProcessor.renderGroup(builder, stack, group, model))
            {
                stack.method_22909();

                return true;
            }
        }

        for (ModelGroup childGroup : group.children)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, childGroup))
            {
                stack.method_22909();

                return true;
            }
        }

        stack.method_22909();

        return false;
    }

    public static record PivotFrame(Vector3f position, Quaternionf parentRotation, Quaternionf worldRotation)
    {
    }

    public static void collectPivotFrames(Model model, Set<String> wanted, Map<String, PivotFrame> out)
    {
        collectPivotFrames(model, wanted, out, null, false);
    }

    public static void collectPivotFrames(Model model, Set<String> wanted, Map<String, PivotFrame> out, Matrix4f baseTransform)
    {
        collectPivotFrames(model, wanted, out, baseTransform, false);
    }

    /**
     * @param applyStretch when true, each bone's transient {@link ModelGroup#offset}
     * is folded into its frame like the renderer does — so a chain collected after
     * an ancestor stretch sees the shifted position.
     */
    public static void collectPivotFrames(Model model, Set<String> wanted, Map<String, PivotFrame> out, Matrix4f baseTransform, boolean applyStretch)
    {
        if (model == null || wanted == null || wanted.isEmpty() || out == null)
        {
            return;
        }

        class_4587 stack = new class_4587();

        if (baseTransform != null)
        {
            Vector3f t = baseTransform.getTranslation(new Vector3f());
            Quaternionf r = baseTransform.getNormalizedRotation(new Quaternionf());
            Matrix4f rigid = new Matrix4f().rotation(r).setTranslation(t);

            stack.method_23760().method_23761().set(rigid);
        }

        for (ModelGroup group : model.topGroups)
        {
            collectPivotFramesRec(stack, group, wanted, out, applyStretch);
        }
    }

    private static void collectPivotFramesRec(class_4587 stack, ModelGroup group, Set<String> wanted, Map<String, PivotFrame> out, boolean applyStretch)
    {
        stack.method_22903();

        if (applyStretch)
        {
            ICubicRenderer.offsetGroup(stack, group);
        }

        ICubicRenderer.translateGroup(stack, group);
        ICubicRenderer.moveToGroupPivot(stack, group);

        boolean store = wanted.contains(group.id);
        Vector3f pos;
        Quaternionf parentRot;

        if (store)
        {
            Matrix4f mat = stack.method_23760().method_23761();

            pos = mat.getTranslation(new Vector3f());
            parentRot = mat.getNormalizedRotation(new Quaternionf());
        }
        else
        {
            pos = null;
            parentRot = null;
        }

        ICubicRenderer.rotateGroup(stack, group);

        if (store)
        {
            Matrix4f mat = stack.method_23760().method_23761();
            Quaternionf worldRot = mat.getNormalizedRotation(new Quaternionf());

            out.put(group.id, new PivotFrame(pos, parentRot, worldRot));
        }

        ICubicRenderer.scaleGroup(stack, group);
        ICubicRenderer.moveBackFromGroupPivot(stack, group);

        for (ModelGroup child : group.children)
        {
            collectPivotFramesRec(stack, child, wanted, out, applyStretch);
        }

        stack.method_22909();
    }

    private static final float EPS = 1.0e-6f;

    public static void applyRotations(Model model, Quaternionf rootParentRotation, List<String> ids, Vector3f[] positions)
    {
        if (model == null || rootParentRotation == null || ids == null || positions == null || ids.isEmpty() || positions.length < 2)
        {
            return;
        }

        Quaternionf parentWorld = new Quaternionf(rootParentRotation);
        int boneCount = ids.size();
        boolean hasTip = positions.length >= boneCount + 1;
        int rotCount = boneCount - 1 + (hasTip ? 1 : 0);

        for (int i = 0; i < rotCount; i++)
        {
            ModelGroup bone = model.getGroup(ids.get(i));
            ModelGroup child = i + 1 < boneCount ? model.getGroup(ids.get(i + 1)) : null;

            if (bone == null)
            {
                return;
            }

            Vector3f restDirLocal;

            if (child != null)
            {
                restDirLocal = new Vector3f(child.initial.translate).sub(bone.initial.translate).mul(1.0f / 16.0f);
            }
            else if (boneCount >= 2)
            {
                ModelGroup parent = model.getGroup(ids.get(i - 1));

                if (parent == null)
                {
                    return;
                }

                restDirLocal = new Vector3f(bone.initial.translate).sub(parent.initial.translate).mul(1.0f / 16.0f);
            }
            else if (bone.children != null && !bone.children.isEmpty())
            {
                ModelGroup firstChild = bone.children.get(0);

                restDirLocal = new Vector3f(firstChild.initial.translate).sub(bone.initial.translate).mul(1.0f / 16.0f);
            }
            else
            {
                restDirLocal = new Vector3f(0F, -1F, 0F);
            }

            Vector3f desiredDirWorld = new Vector3f(positions[i + 1]).sub(positions[i]);

            if (restDirLocal.lengthSquared() < EPS * EPS || desiredDirWorld.lengthSquared() < EPS * EPS)
            {
                continue;
            }

            restDirLocal.normalize();
            desiredDirWorld.normalize();

            Quaternionf invParent = new Quaternionf(parentWorld).invert();
            Vector3f desiredDirLocal = new Vector3f(desiredDirWorld);

            invParent.transform(desiredDirLocal);

            if (desiredDirLocal.lengthSquared() < EPS * EPS)
            {
                continue;
            }

            desiredDirLocal.normalize();

            Quaternionf localRot = QuaternionMath.fromToWithMirror(restDirLocal, desiredDirLocal);
            Quaternionf fkLocal = QuaternionMath.composeFromEulerZYX(bone.current.rotate.x, bone.current.rotate.y, bone.current.rotate.z);

            if (bone.current.rotate2.x != 0F || bone.current.rotate2.y != 0F || bone.current.rotate2.z != 0F)
            {
                fkLocal.mul(QuaternionMath.composeFromEulerZYX(bone.current.rotate2.x, bone.current.rotate2.y, bone.current.rotate2.z));
            }

            localRot.mul(QuaternionMath.extractTwistComponent(fkLocal, restDirLocal));

            Vector3f eulerDeg = QuaternionMath.decomposeEulerZYX(localRot);

            eulerDeg.x = wrapDegreesNear(eulerDeg.x, bone.current.rotate.x);
            eulerDeg.y = wrapDegreesNear(eulerDeg.y, bone.current.rotate.y);
            eulerDeg.z = wrapDegreesNear(eulerDeg.z, bone.current.rotate.z);

            bone.current.rotate.set(eulerDeg);
            bone.current.rotate2.set(0F, 0F, 0F);
            bone.orient = null;

            parentWorld.mul(QuaternionMath.composeFromEulerZYX(eulerDeg.x, eulerDeg.y, eulerDeg.z));
        }
    }

    private static float wrapDegreesNear(float angle, float reference)
    {
        float delta = angle - reference;

        while (delta > 180F)
        {
            angle -= 360F;
            delta -= 360F;
        }

        while (delta < -180F)
        {
            angle += 360F;
            delta += 360F;
        }

        return angle;
    }
}