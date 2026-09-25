package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.constraints.JointLimitConfig;
import mchorse.bbs_mod.cubic.constraints.JointLimitEnforcer;
import mchorse.bbs_mod.cubic.constraints.JointLimitSerializer;
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
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form editor panel for configuring per-instance bone constraints directly on a {@link ModelForm}.
 */
public class UIModelConstraintsFormPanel extends UIFormPanel<ModelForm>
{
    private static final float DEFAULT_LOWER = -180F;
    private static final float DEFAULT_UPPER = 180F;

    private UIStringList boneList;
    private UILabel noSelectionLabel;
    private UILabel boneNameLabel;
    private UIElement detailContainer;

    private UIToggle activeToggle;
    private UITrackpad minXPad;
    private UITrackpad maxXPad;
    private UITrackpad minYPad;
    private UITrackpad maxYPad;
    private UITrackpad minZPad;
    private UITrackpad maxZPad;

    private final Map<String, JointLimitData> joints = new HashMap<>();
    private final List<String> boneNames = new ArrayList<>();
    private String selectedBone;
    private boolean suppressCommit;

    public UIModelConstraintsFormPanel(UIForm editor)
    {
        super(editor);

        /* Top row: Import defaults, Clear */
        UIButton importDefaults = new UIButton(UIKeys.MODELS_RELOAD, (b) -> this.importModelDefaults());
        importDefaults.tooltip(UIKeys.MODELS_IK_PRESET_PASTE);

        UIButton clearAll = new UIButton(UIKeys.MODELS_IK_PRESET_CLEAR, (b) ->
        {
            this.joints.clear();
            this.commitChanges();
            this.refreshBoneList();
            this.refreshDetailFields();
        });
        clearAll.tooltip(UIKeys.MODELS_IK_PRESET_CLEAR);

        UIElement topRow = UI.row(importDefaults, clearAll);

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

        /* Detail container */
        this.boneNameLabel = UI.label(IKey.raw("-"));
        this.noSelectionLabel = UI.label(UIKeys.MODELS_CONSTRAINTS_NO_SELECTION);

        this.detailContainer = new UIElement();
        this.detailContainer.column().stretch().vertical().height(20).padding(4);

        this.activeToggle = new UIToggle(UIKeys.MODELS_CONSTRAINTS_ACTIVE, (b) -> this.onActiveChanged(b.getValue()));

        this.minXPad = new UITrackpad((v) ->
        {
            JointLimitData data = this.getOrCreateSelectedJoint();

            if (data != null)
            {
                data.minX = v.floatValue();
                this.commitChanges();
            }
        });
        this.minXPad.limit(-180F, 180F);

        this.maxXPad = new UITrackpad((v) ->
        {
            JointLimitData data = this.getOrCreateSelectedJoint();

            if (data != null)
            {
                data.maxX = v.floatValue();
                this.commitChanges();
            }
        });
        this.maxXPad.limit(-180F, 180F);

        this.minYPad = new UITrackpad((v) ->
        {
            JointLimitData data = this.getOrCreateSelectedJoint();

            if (data != null)
            {
                data.minY = v.floatValue();
                this.commitChanges();
            }
        });
        this.minYPad.limit(-180F, 180F);

        this.maxYPad = new UITrackpad((v) ->
        {
            JointLimitData data = this.getOrCreateSelectedJoint();

            if (data != null)
            {
                data.maxY = v.floatValue();
                this.commitChanges();
            }
        });
        this.maxYPad.limit(-180F, 180F);

        this.minZPad = new UITrackpad((v) ->
        {
            JointLimitData data = this.getOrCreateSelectedJoint();

            if (data != null)
            {
                data.minZ = v.floatValue();
                this.commitChanges();
            }
        });
        this.minZPad.limit(-180F, 180F);

        this.maxZPad = new UITrackpad((v) ->
        {
            JointLimitData data = this.getOrCreateSelectedJoint();

            if (data != null)
            {
                data.maxZ = v.floatValue();
                this.commitChanges();
            }
        });
        this.maxZPad.limit(-180F, 180F);

        this.detailContainer.add(
            this.activeToggle,
            UI.label(UIKeys.MODELS_PHYS_BONES_MIN_PITCH),
            this.minXPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_MAX_PITCH),
            this.maxXPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_MIN_YAW),
            this.minYPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_MAX_YAW),
            this.maxYPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_MIN_ROLL),
            this.minZPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_MAX_ROLL),
            this.maxZPad
        );

        this.options.add(
            topRow,
            UI.label(UIKeys.MODELS_CONSTRAINTS_EDITOR),
            this.boneList,
            this.boneNameLabel,
            this.noSelectionLabel,
            this.detailContainer
        );

        this.setDetailVisible(false);
    }

    private void setDetailVisible(boolean visible)
    {
        this.noSelectionLabel.setVisible(!visible);
        this.detailContainer.setVisible(visible);
        /* Visibility before resize so ColumnResizer assigns height to the detail block. */
        this.options.resize();
    }

    private String boneFromDisplay(String display)
    {
        if (display == null)
        {
            return null;
        }

        return display.startsWith("✓ ") ? display.substring(2) : display;
    }

    private void refreshBoneList()
    {
        List<String> displayList = new ArrayList<>();

        for (String bone : this.boneNames)
        {
            if (this.joints.containsKey(bone))
            {
                displayList.add("✓ " + bone);
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
            JointLimitData data = this.joints.get(this.selectedBone);

            if (data != null)
            {
                this.activeToggle.setValue(true);
                this.minXPad.setValue(data.minX);
                this.maxXPad.setValue(data.maxX);
                this.minYPad.setValue(data.minY);
                this.maxYPad.setValue(data.maxY);
                this.minZPad.setValue(data.minZ);
                this.maxZPad.setValue(data.maxZ);
            }
            else
            {
                this.activeToggle.setValue(false);
                this.minXPad.setValue(DEFAULT_LOWER);
                this.maxXPad.setValue(DEFAULT_UPPER);
                this.minYPad.setValue(DEFAULT_LOWER);
                this.maxYPad.setValue(DEFAULT_UPPER);
                this.minZPad.setValue(DEFAULT_LOWER);
                this.maxZPad.setValue(DEFAULT_UPPER);
            }
        }
        finally
        {
            this.suppressCommit = false;
        }
    }

    private JointLimitData getOrCreateSelectedJoint()
    {
        if (this.selectedBone == null || this.selectedBone.isEmpty())
        {
            return null;
        }

        JointLimitData data = this.joints.get(this.selectedBone);

        if (data == null)
        {
            data = JointLimitData.createDefault();
            this.joints.put(this.selectedBone, data);
        }

        return data;
    }

    private void onActiveChanged(boolean active)
    {
        if (this.selectedBone == null)
        {
            return;
        }

        if (active)
        {
            this.getOrCreateSelectedJoint();
        }
        else
        {
            this.joints.remove(this.selectedBone);
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

        Map<String, JointLimitConfig.JointLimit> map = new HashMap<>();

        for (Map.Entry<String, JointLimitData> entry : this.joints.entrySet())
        {
            JointLimitData d = entry.getValue();
            map.put(entry.getKey(), new JointLimitConfig.JointLimit(true, d.minX, d.minY, d.minZ, d.maxX, d.maxY, d.maxZ));
        }

        JointLimitConfig config = map.isEmpty() ? null : new JointLimitConfig(map);
        MapType data = JointLimitSerializer.serialize(config);

        boolean empty = map.isEmpty();
        this.form.constraints.preNotify();
        this.form.constraints.set(empty ? null : data);
        this.form.constraints.postNotify();

        JointLimitEnforcer.clearCache();

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
            if (loaded != null && loaded.constraints.get() instanceof MapType map)
            {
                this.loadFromMap(map);
                this.commitChanges();
                this.refreshBoneList();
                this.refreshDetailFields();
            }
        });
    }

    private void loadFromMap(MapType map)
    {
        this.joints.clear();

        if (map != null)
        {
            JointLimitConfig jointConfig = JointLimitSerializer.deserialize(map);

            if (jointConfig != null && jointConfig.joints() != null)
            {
                for (Map.Entry<String, JointLimitConfig.JointLimit> entry : jointConfig.joints().entrySet())
                {
                    this.joints.put(entry.getKey(), JointLimitData.fromLimit(entry.getValue()));
                }
            }
        }
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);

        this.selectedBone = null;
        this.joints.clear();
        this.boneNames.clear();

        if (form != null)
        {
            BaseType raw = form.constraints.get();

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

    private static class JointLimitData
    {
        float minX = DEFAULT_LOWER;
        float minY = DEFAULT_LOWER;
        float minZ = DEFAULT_LOWER;
        float maxX = DEFAULT_UPPER;
        float maxY = DEFAULT_UPPER;
        float maxZ = DEFAULT_UPPER;

        static JointLimitData createDefault()
        {
            return new JointLimitData();
        }

        static JointLimitData fromLimit(JointLimitConfig.JointLimit limit)
        {
            JointLimitData data = new JointLimitData();
            data.minX = limit.minX();
            data.minY = limit.minY();
            data.minZ = limit.minZ();
            data.maxX = limit.maxX();
            data.maxY = limit.maxY();
            data.maxZ = limit.maxZ();

            return data;
        }
    }
}
