package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.ik.IKPresetManager;
import mchorse.bbs_mod.cubic.ik.LimbConstraintCompiler;
import mchorse.bbs_mod.cubic.ik.LimbConstraintDef;
import mchorse.bbs_mod.cubic.ik.LimbConstraintSerializer;
import mchorse.bbs_mod.cubic.model.ModelRepository;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIDebugOverlayContextMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.model.UIModelIKPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UIDataContextMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Form editor panel for configuring per-instance limb IK directly on a {@link ModelForm}.
 */
public class UIModelIKFormPanel extends UIFormPanel<ModelForm>
{
    private UIStringList boneList;
    private UILabel noSelectionLabel;
    private UILabel boneNameLabel;
    private UIElement detailContainer;

    private UIToggle activeToggle;
    private UIButton controllerButton;
    private UITrackpad depthPad;
    private UIToggle poleEnabledToggle;
    private UIButton poleBoneButton;
    private UITrackpad bendOffsetPad;
    private UITrackpad flexibilityPad;
    private UITrackpad influencePad;
    private UIToggle orientTipToggle;
    private UIToggle extensibleToggle;
    private UIToggle classicToggle;
    private UIToggle debugToggle;

    private final Map<String, LimbData> limbs = new HashMap<>();
    private final Map<String, LimbConstraintDef.JointDoF> joints = new HashMap<>();
    private final List<String> boneNames = new ArrayList<>();
    private String selectedBone;
    private boolean suppressCommit;

    public UIModelIKFormPanel(UIForm editor)
    {
        super(editor);

        /* Top row: Presets, Import defaults */
        UIButton presetsButton = new UIButton(UIKeys.FILM_LAYOUT_PRESETS, (b) -> this.getContext().replaceContextMenu(this.createPresetsMenu()));
        UIButton importDefaults = new UIButton(UIKeys.MODELS_RELOAD, (b) -> this.importModelDefaults());
        importDefaults.tooltip(UIKeys.MODELS_IK_PRESET_PASTE);

        UIElement topRow = UI.row(presetsButton, importDefaults);

        /* Bone selection list */
        this.boneList = new UIStringList((items) ->
        {
            if (items != null && !items.isEmpty())
            {
                this.selectBone(this.boneFromDisplay(items.get(0)));
            }
        });
        this.boneList.background();
        this.boneList.scroll.scrollItemSize = 18;
        this.boneList.h(120);

        /* Debug row */
        this.debugToggle = new UIToggle(UIKeys.MODELS_DEBUG_SHOW, (b) -> BBSSettings.ikDebug.enabled.set(b.getValue()));
        this.debugToggle.setValue(BBSSettings.ikDebug.enabled.get());
        this.debugToggle.tooltip(UIKeys.MODELS_IK_DEBUG_TOOLTIP);
        this.debugToggle.context(() -> new UIDebugOverlayContextMenu(BBSSettings.ikDebug));

        UIIcon debugSettings = new UIIcon(Icons.GEAR, (b) -> this.getContext().replaceContextMenu(new UIDebugOverlayContextMenu(BBSSettings.ikDebug)));
        debugSettings.tooltip(UIKeys.MODELS_DEBUG_CONFIGURE);
        UIElement debugRow = UI.row(this.debugToggle, debugSettings);

        /* Detail container */
        this.boneNameLabel = UI.label(IKey.raw("-"));
        this.noSelectionLabel = UI.label(UIKeys.MODELS_IK_NO_SELECTION);

        this.detailContainer = new UIElement();
        this.detailContainer.column().stretch().vertical().height(20).padding(4);

        this.activeToggle = new UIToggle(UIKeys.MODELS_IK_ENABLED, (b) -> this.onActiveChanged(b.getValue()));
        this.activeToggle.tooltip(UIKeys.MODELS_IK_ENABLED_TOOLTIP);

        this.controllerButton = new UIButton(UIKeys.MODELS_IK_TARGET_BONE, (b) ->
            this.openBonePicker((bone) ->
            {
                LimbData limb = this.getOrCreateSelectedLimb();

                if (limb != null)
                {
                    limb.controller = bone;
                    this.controllerButton.label = IKey.raw(bone == null || bone.isEmpty() ? "-" : bone);
                    this.commitChanges();
                }
            })
        );
        this.controllerButton.tooltip(UIKeys.MODELS_IK_TARGET_BONE_TOOLTIP);

        this.depthPad = new UITrackpad((v) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.depth = Math.max(1, v.intValue());
                this.commitChanges();
            }
        });
        this.depthPad.limit(1, 32, true);
        this.depthPad.tooltip(UIKeys.MODELS_IK_CHAIN_LENGTH_TOOLTIP);

        this.poleEnabledToggle = new UIToggle(UIKeys.MODELS_IK_POLE_ENABLED, (b) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.poleEnabled = b.getValue();
                this.poleBoneButton.setEnabled(b.getValue());
                this.commitChanges();
            }
        });

        this.poleBoneButton = new UIButton(UIKeys.MODELS_IK_POLE_BONE, (b) ->
            this.openBonePicker((bone) ->
            {
                LimbData limb = this.getOrCreateSelectedLimb();

                if (limb != null)
                {
                    limb.poleBone = bone;
                    this.poleBoneButton.label = IKey.raw(bone == null || bone.isEmpty() ? "-" : bone);
                    this.commitChanges();
                }
            })
        );

        this.bendOffsetPad = new UITrackpad((v) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.bendOffset = v.floatValue();
                this.commitChanges();
            }
        });
        this.bendOffsetPad.limit(-180F, 180F);
        this.bendOffsetPad.tooltip(UIKeys.MODELS_IK_BEND_OFFSET);

        this.flexibilityPad = new UITrackpad((v) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.flexibility = v.floatValue();
                this.commitChanges();
            }
        });
        this.flexibilityPad.limit(0F, 2F);
        this.flexibilityPad.tooltip(UIKeys.MODELS_IK_FLEXIBILITY);

        this.influencePad = new UITrackpad((v) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.influence = v.floatValue();
                this.commitChanges();
            }
        });
        this.influencePad.limit(0F, 1F);
        this.influencePad.tooltip(UIKeys.MODELS_IK_WEIGHT_TOOLTIP);

        this.orientTipToggle = new UIToggle(UIKeys.MODELS_IK_ORIENT_TIP, (b) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.orientTip = b.getValue();
                this.commitChanges();
            }
        });

        this.extensibleToggle = new UIToggle(UIKeys.MODELS_IK_EXTENSIBLE, (b) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.extensible = b.getValue();
                this.commitChanges();
            }
        });

        this.classicToggle = new UIToggle(UIKeys.MODELS_IK_CLASSIC_SOLVER, (b) ->
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.classic = b.getValue();
                this.commitChanges();
            }
        });
        this.classicToggle.tooltip(UIKeys.MODELS_IK_CLASSIC_SOLVER_TOOLTIP);

        this.detailContainer.add(
            this.activeToggle,
            UI.label(UIKeys.MODELS_IK_TARGET_BONE),
            this.controllerButton,
            UI.label(UIKeys.MODELS_IK_CHAIN_LENGTH),
            this.depthPad,
            this.poleEnabledToggle,
            this.poleBoneButton,
            UI.label(UIKeys.MODELS_IK_BEND_OFFSET),
            this.bendOffsetPad,
            UI.label(UIKeys.MODELS_IK_FLEXIBILITY),
            this.flexibilityPad,
            UI.label(UIKeys.MODELS_IK_WEIGHT),
            this.influencePad,
            this.orientTipToggle,
            this.extensibleToggle,
            this.classicToggle
        );

        this.options.add(
            topRow,
            UI.label(UIKeys.MODELS_IK_EDITOR),
            this.boneList,
            debugRow,
            this.boneNameLabel,
            this.noSelectionLabel,
            this.detailContainer
        );

        this.setDetailVisible(false);
    }

    private void openBonePicker(Consumer<String> callback)
    {
        List<String> list = new ArrayList<>();
        list.add("");
        list.addAll(this.boneNames);

        this.getContext().replaceContextMenu(new UIModelIKPanel.UIBonePickerContextMenu(list, callback));
    }

    private void setDetailVisible(boolean visible)
    {
        this.noSelectionLabel.setVisible(!visible);
        this.detailContainer.setVisible(visible);
    }

    private String boneFromDisplay(String display)
    {
        if (display == null)
        {
            return null;
        }

        return display.startsWith("✓ ") || display.startsWith("✗ ") ? display.substring(2) : display;
    }

    private void refreshBoneList()
    {
        List<String> displayList = new ArrayList<>();

        for (String bone : this.boneNames)
        {
            LimbData limb = this.limbs.get(bone);

            if (limb != null)
            {
                displayList.add((limb.active ? "✓ " : "✗ ") + bone);
            }
            else
            {
                displayList.add(bone);
            }
        }

        this.boneList.setList(displayList);

        if (this.selectedBone != null)
        {
            for (String item : displayList)
            {
                if (this.boneFromDisplay(item).equals(this.selectedBone))
                {
                    this.boneList.setCurrentScroll(item);

                    break;
                }
            }
        }
    }

    public void selectBone(String bone)
    {
        this.selectedBone = bone;

        if (bone == null || bone.isEmpty())
        {
            this.boneNameLabel.label = IKey.raw("-");
            this.setDetailVisible(false);

            return;
        }

        this.boneNameLabel.label = IKey.raw(bone);
        this.setDetailVisible(true);
        this.refreshDetailFields();
    }

    @Override
    public void pickBone(String bone)
    {
        if (bone != null && !bone.isEmpty())
        {
            this.selectBone(bone);
            this.refreshBoneList();
        }
    }

    private void refreshDetailFields()
    {
        if (this.selectedBone == null)
        {
            return;
        }

        this.suppressCommit = true;

        try
        {
            LimbData limb = this.limbs.get(this.selectedBone);

            if (limb != null)
            {
                this.activeToggle.setValue(limb.active);
                this.controllerButton.label = IKey.raw(limb.controller == null || limb.controller.isEmpty() ? "-" : limb.controller);
                this.depthPad.setValue(limb.depth);
                this.poleEnabledToggle.setValue(limb.poleEnabled);
                this.poleBoneButton.label = IKey.raw(limb.poleBone == null || limb.poleBone.isEmpty() ? "-" : limb.poleBone);
                this.poleBoneButton.setEnabled(limb.poleEnabled);
                this.bendOffsetPad.setValue(limb.bendOffset);
                this.flexibilityPad.setValue(limb.flexibility);
                this.influencePad.setValue(limb.influence);
                this.orientTipToggle.setValue(limb.orientTip);
                this.extensibleToggle.setValue(limb.extensible);
                this.classicToggle.setValue(limb.classic);
            }
            else
            {
                this.activeToggle.setValue(false);
                this.controllerButton.label = IKey.raw("-");
                this.depthPad.setValue(LimbConstraintDef.DEFAULT_DEPTH);
                this.poleEnabledToggle.setValue(true);
                this.poleBoneButton.label = IKey.raw("-");
                this.poleBoneButton.setEnabled(true);
                this.bendOffsetPad.setValue(LimbConstraintDef.DEFAULT_BEND_OFFSET);
                this.flexibilityPad.setValue(LimbConstraintDef.DEFAULT_FLEXIBILITY);
                this.influencePad.setValue(LimbConstraintDef.DEFAULT_INFLUENCE);
                this.orientTipToggle.setValue(LimbConstraintDef.DEFAULT_ORIENT_TIP);
                this.extensibleToggle.setValue(LimbConstraintDef.DEFAULT_EXTENSIBLE);
                this.classicToggle.setValue(LimbConstraintDef.DEFAULT_CLASSIC);
            }
        }
        finally
        {
            this.suppressCommit = false;
        }
    }

    private LimbData getOrCreateSelectedLimb()
    {
        if (this.selectedBone == null || this.selectedBone.isEmpty())
        {
            return null;
        }

        LimbData limb = this.limbs.get(this.selectedBone);

        if (limb == null)
        {
            limb = LimbData.createDefault();
            this.limbs.put(this.selectedBone, limb);
        }

        return limb;
    }

    private void onActiveChanged(boolean active)
    {
        if (this.selectedBone == null)
        {
            return;
        }

        if (active)
        {
            LimbData limb = this.getOrCreateSelectedLimb();

            if (limb != null)
            {
                limb.active = true;
            }
        }
        else
        {
            this.limbs.remove(this.selectedBone);
        }

        this.commitChanges();
        this.refreshBoneList();
        this.refreshDetailFields();
    }

    private void commitChanges()
    {
        if (this.suppressCommit || this.form == null)
        {
            return;
        }

        List<LimbConstraintDef.Limb> list = new ArrayList<>();

        for (Map.Entry<String, LimbData> entry : this.limbs.entrySet())
        {
            list.add(entry.getValue().toLimb(entry.getKey()));
        }

        LimbConstraintDef def = list.isEmpty() && this.joints.isEmpty() ? null : new LimbConstraintDef(list, this.joints);
        MapType map = LimbConstraintSerializer.toData(def);

        this.form.ik.preNotify();
        this.form.ik.set(map == null || map.isEmpty() ? null : map);
        this.form.ik.postNotify();

        LimbConstraintCompiler.clear();

        if (FormUtilsClient.getRenderer(this.form) instanceof ModelFormRenderer renderer)
        {
            renderer.resetAnimator();
        }
    }

    private void importModelDefaults()
    {
        if (this.form == null)
        {
            return;
        }

        String modelId = this.form.model.get();

        if (modelId == null || modelId.isEmpty())
        {
            return;
        }

        ModelRepository repository = (ModelRepository) ContentType.MODELS.getRepository();

        repository.load(modelId, (loaded) ->
        {
            if (loaded != null && loaded.ik.get() instanceof MapType map)
            {
                this.loadFromMap(map);
                this.commitChanges();
                this.refreshBoneList();
                this.refreshDetailFields();
            }
        });
    }

    private UIContextMenu createPresetsMenu()
    {
        String group = this.form != null ? this.form.model.get() : "";

        return new UIDataContextMenu(IKPresetManager.INSTANCE, group, this::currentPreset, this::applyPreset)
            .tooltips(IKPresetManager.CLIPBOARD,
                UIKeys.MODELS_IK_PRESET_COPY,
                UIKeys.MODELS_IK_PRESET_PASTE,
                UIKeys.MODELS_IK_PRESET_CLEAR,
                UIKeys.MODELS_IK_PRESET_SAVE,
                UIKeys.MODELS_IK_PRESET_NAME);
    }

    private MapType currentPreset()
    {
        List<LimbConstraintDef.Limb> list = new ArrayList<>();

        for (Map.Entry<String, LimbData> entry : this.limbs.entrySet())
        {
            list.add(entry.getValue().toLimb(entry.getKey()));
        }

        return LimbConstraintSerializer.toData(new LimbConstraintDef(list, this.joints));
    }

    private void applyPreset(MapType map)
    {
        this.loadFromMap(map);
        this.commitChanges();
        this.refreshBoneList();
        this.refreshDetailFields();
    }

    private void loadFromMap(MapType map)
    {
        this.limbs.clear();
        this.joints.clear();

        if (map != null)
        {
            LimbConstraintDef def = LimbConstraintSerializer.fromData(map);

            if (def != null && def.limbs() != null)
            {
                for (LimbConstraintDef.Limb limb : def.limbs())
                {
                    this.limbs.put(limb.tipBone(), LimbData.fromLimb(limb));
                }
            }

            if (def != null && def.joints() != null)
            {
                this.joints.putAll(def.joints());
            }
        }
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);

        this.selectedBone = null;
        this.limbs.clear();
        this.joints.clear();
        this.boneNames.clear();

        if (form != null)
        {
            BaseType raw = form.ik.get();

            if (raw instanceof MapType map)
            {
                this.loadFromMap(map);
            }

            ModelInstance instance = ModelFormRenderer.getModel(form);

            if (instance == null && !form.model.get().isEmpty())
            {
                instance = BBSModClient.getModels().getModel(form.model.get());
            }

            if (instance != null && instance.getModel() != null)
            {
                this.boneNames.addAll(instance.getModel().getGroupKeysInHierarchyOrder());
            }
        }

        this.refreshBoneList();
        this.setDetailVisible(false);
    }

    private static class LimbData
    {
        String controller = "";
        int depth = LimbConstraintDef.DEFAULT_DEPTH;
        boolean poleEnabled = true;
        String poleBone = "";
        float bendOffset = LimbConstraintDef.DEFAULT_BEND_OFFSET;
        float flexibility = LimbConstraintDef.DEFAULT_FLEXIBILITY;
        float influence = LimbConstraintDef.DEFAULT_INFLUENCE;
        boolean active = true;
        boolean orientTip = LimbConstraintDef.DEFAULT_ORIENT_TIP;
        boolean extensible = LimbConstraintDef.DEFAULT_EXTENSIBLE;
        boolean classic = LimbConstraintDef.DEFAULT_CLASSIC;

        static LimbData createDefault()
        {
            return new LimbData();
        }

        static LimbData fromLimb(LimbConstraintDef.Limb limb)
        {
            LimbData data = new LimbData();
            data.controller = limb.controllerBone() == null ? "" : limb.controllerBone();
            data.depth = limb.depth();
            data.poleEnabled = limb.poleEnabled();
            data.poleBone = limb.poleBone() == null ? "" : limb.poleBone();
            data.bendOffset = limb.bendOffset();
            data.flexibility = limb.flexibility();
            data.influence = limb.influence();
            data.active = limb.active();
            data.orientTip = limb.orientTip();
            data.extensible = limb.extensible();
            data.classic = limb.classic();

            return data;
        }

        LimbConstraintDef.Limb toLimb(String tipBone)
        {
            return new LimbConstraintDef.Limb(
                tipBone,
                this.controller,
                this.depth,
                this.poleEnabled,
                this.poleBone,
                this.bendOffset,
                this.flexibility,
                this.influence,
                this.active,
                this.orientTip,
                this.extensible,
                this.classic
            );
        }
    }
}
