package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.forms.values.ValueActionsConfig;
import mchorse.bbs_mod.settings.values.core.ValueData;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.misc.ValueVector3f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;

import org.joml.Vector3f;

public class ModelConfig extends ValueGroup
{
    public final ValueBoolean procedural = new ValueBoolean("procedural");
    public final ValueBoolean culling = new ValueBoolean("culling", true);
    public final ValueString poseGroup = new ValueString("pose_group", "");
    public final ValueString anchorGroup = new ValueString("anchor", "");
    public final ValueGroup lookAt = new ValueGroup("look_at");
    public final ValueString lookAtHead = new ValueString("head_bone", "");
    public final ValueFloat uiScale = new ValueFloat("ui_scale", 1F);
    public final ValueVector3f scale = new ValueVector3f("scale", new Vector3f(1, 1, 1));

    public final ValuePose sneakingPose = new ValuePose("sneaking_pose", new Pose());
    public final ValuePose ridingPose = new ValuePose("riding_pose", new Pose());
    public final ValuePose parts = new ValuePose("parts", new Pose());
    public final ValueInt color = new ValueInt("color", Colors.WHITE);
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueActionsConfig animations = new ValueActionsConfig("animations", new ActionsConfig());
    public final ArmorConfig armorSlots = new ArmorConfig("armor_slots");
    public final ArmorSlot fpMain = new ArmorSlot("fp_main");
    public final ArmorSlot fpOffhand = new ArmorSlot("fp_offhand");

    public final ArmorSlot itemsMainTransform = new ArmorSlot("items_main_transform");
    public final ArmorSlot itemsOffTransform = new ArmorSlot("items_off_transform");

    public final ValueList<ArmorSlot> itemsMain = new ValueList<ArmorSlot>("items_main")
    {
        @Override
        protected ArmorSlot create(String id)
        {
            return new ArmorSlot(id);
        }
    };

    public final ValueList<ArmorSlot> itemsOff = new ValueList<ArmorSlot>("items_off")
    {
        @Override
        protected ArmorSlot create(String id)
        {
            return new ArmorSlot(id);
        }
    };

    /* Limb IK / spring chains / joint limits — MapType blobs consumed by the new solvers */
    public final ValueData ik = new ValueData("ik");
    public final ValueData springs = new ValueData("springs");
    public final ValueData constraints = new ValueData("constraints");

    public ModelConfig(String id)
    {
        super(id);

        this.add(this.procedural);
        this.add(this.culling);
        this.add(this.poseGroup);
        this.add(this.anchorGroup);
        this.lookAt.add(this.lookAtHead);
        this.add(this.lookAt);
        this.add(this.uiScale);
        this.add(this.scale);
        this.add(this.sneakingPose);
        this.add(this.ridingPose);
        this.add(this.parts);
        this.add(this.color);
        this.add(this.texture);
        this.add(this.animations);
        this.add(this.armorSlots);
        this.add(this.fpMain);
        this.add(this.fpOffhand);
        this.add(this.itemsMainTransform);
        this.add(this.itemsOffTransform);
        this.add(this.itemsMain);
        this.add(this.itemsOff);

        this.ik.invisible();
        this.springs.invisible();
        this.constraints.invisible();
        this.add(this.ik);
        this.add(this.springs);
        this.add(this.constraints);
    }
}
