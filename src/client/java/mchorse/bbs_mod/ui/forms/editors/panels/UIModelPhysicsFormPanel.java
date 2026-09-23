package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ModelRepository;
import mchorse.bbs_mod.cubic.physics.DynamicBoneOrchestrator;
import mchorse.bbs_mod.cubic.physics.SpringChainCompiler;
import mchorse.bbs_mod.cubic.physics.SpringChainDef;
import mchorse.bbs_mod.cubic.physics.SpringChainSerializer;
import mchorse.bbs_mod.cubic.physics.SpringChainsConfig;
import mchorse.bbs_mod.cubic.physics.WindDef;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Form editor panel for configuring per-instance dynamic bones / physics directly on a {@link ModelForm}.
 */
public class UIModelPhysicsFormPanel extends UIFormPanel<ModelForm>
{
    private UIStringList boneList;
    private UILabel noSelectionLabel;
    private UILabel boneNameLabel;
    private UIElement detailContainer;

    private UIToggle enabledToggle;
    private UIButton chainEndButton;
    private UIButton anchorEndButton;
    private UITrackpad pullStrengthPad;
    private UITrackpad dragPad;
    private UITrackpad springReturnPad;
    private UITrackpad pullRotXPad;
    private UITrackpad pullRotYPad;
    private UITrackpad pullRotZPad;
    private UITrackpad influencePad;
    private UIToggle bodyRelativePullToggle;
    private UIToggle hitDetectionToggle;
    private UITrackpad hitRadiusPad;
    private UITrackpad relaxStepsPad;

    /* Wind fields */
    private UITrackpad windPowerPad;
    private UITrackpad windDirXPad;
    private UITrackpad windDirYPad;
    private UITrackpad windDirZPad;
    private UITrackpad windGustinessPad;
    private UITrackpad windGustSpeedPad;
    private UITrackpad windGustScalePad;
    private UIToggle windModelRelativeToggle;

    private UIToggle debugToggle;

    private final Map<String, SpringChainData> chains = new HashMap<>();
    private WindData wind = WindData.createDefault();
    private final List<String> boneNames = new ArrayList<>();
    private String selectedBone;
    private boolean suppressCommit;

    public UIModelPhysicsFormPanel(UIForm editor)
    {
        super(editor);

        /* Top row: Import defaults, Clear */
        UIButton importDefaults = new UIButton(UIKeys.MODELS_RELOAD, (b) -> this.importModelDefaults());
        importDefaults.tooltip(UIKeys.MODELS_IK_PRESET_PASTE);

        UIButton clearAll = new UIButton(UIKeys.MODELS_IK_PRESET_CLEAR, (b) ->
        {
            this.chains.clear();
            this.wind = WindData.createDefault();
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

        /* Debug row */
        this.debugToggle = new UIToggle(UIKeys.MODELS_DEBUG_SHOW, (b) -> BBSSettings.physicsDebug.enabled.set(b.getValue()));
        this.debugToggle.setValue(BBSSettings.physicsDebug.enabled.get());
        this.debugToggle.tooltip(UIKeys.MODELS_PHYS_BONES_DEBUG_TOOLTIP);
        this.debugToggle.context(() -> new UIDebugOverlayContextMenu(BBSSettings.physicsDebug));

        UIIcon debugSettings = new UIIcon(Icons.GEAR, (b) -> this.getContext().replaceContextMenu(new UIDebugOverlayContextMenu(BBSSettings.physicsDebug)));
        debugSettings.tooltip(UIKeys.MODELS_DEBUG_CONFIGURE);
        UIElement debugRow = UI.row(this.debugToggle, debugSettings);

        /* Detail container */
        this.boneNameLabel = UI.label(IKey.raw("-"));
        this.noSelectionLabel = UI.label(UIKeys.MODELS_PHYS_BONES_NO_SELECTION);

        this.detailContainer = new UIElement();
        this.detailContainer.column().stretch().vertical().height(20).padding(4);

        this.enabledToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_ENABLED, (b) -> this.onEnabledChanged(b.getValue()));
        this.enabledToggle.tooltip(UIKeys.MODELS_PHYS_BONES_ENABLED_TOOLTIP);

        this.chainEndButton = new UIButton(UIKeys.MODELS_PHYS_BONES_CHAIN_END, (b) ->
            this.openBonePicker((bone) ->
            {
                SpringChainData chain = this.getOrCreateSelectedChain();

                if (chain != null)
                {
                    chain.endBone = bone;
                    this.chainEndButton.label = IKey.raw(bone == null || bone.isEmpty() ? "-" : bone);
                    this.commitChanges();
                }
            })
        );
        this.chainEndButton.tooltip(UIKeys.MODELS_PHYS_BONES_CHAIN_END_TOOLTIP);

        this.anchorEndButton = new UIButton(UIKeys.MODELS_PHYS_BONES_ANCHOR_END, (b) ->
            this.openBonePicker((bone) ->
            {
                SpringChainData chain = this.getOrCreateSelectedChain();

                if (chain != null)
                {
                    chain.pinTarget = bone;
                    this.anchorEndButton.label = IKey.raw(bone == null || bone.isEmpty() ? "-" : bone);
                    this.commitChanges();
                }
            })
        );
        this.anchorEndButton.tooltip(UIKeys.MODELS_PHYS_BONES_ANCHOR_END_TOOLTIP);

        this.pullStrengthPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.pullStrength = v.floatValue();
                this.commitChanges();
            }
        });
        this.pullStrengthPad.limit(0F, 10F);
        this.pullStrengthPad.tooltip(UIKeys.MODELS_PHYS_BONES_PULL_STRENGTH);

        this.dragPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.drag = v.floatValue();
                this.commitChanges();
            }
        });
        this.dragPad.limit(0F, 1F);
        this.dragPad.tooltip(UIKeys.MODELS_PHYS_BONES_DRAG);

        this.springReturnPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.springReturn = v.floatValue();
                this.commitChanges();
            }
        });
        this.springReturnPad.limit(0F, 1F);
        this.springReturnPad.tooltip(UIKeys.MODELS_PHYS_BONES_SPRING_RETURN);

        this.pullRotXPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.pullRotX = v.floatValue();
                this.commitChanges();
            }
        });
        this.pullRotXPad.limit(-180F, 180F);

        this.pullRotYPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.pullRotY = v.floatValue();
                this.commitChanges();
            }
        });
        this.pullRotYPad.limit(-180F, 180F);

        this.pullRotZPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.pullRotZ = v.floatValue();
                this.commitChanges();
            }
        });
        this.pullRotZPad.limit(-180F, 180F);

        this.influencePad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.influence = v.floatValue();
                this.commitChanges();
            }
        });
        this.influencePad.limit(0F, 1F);
        this.influencePad.tooltip(UIKeys.MODELS_PHYS_BONES_INFLUENCE);

        this.bodyRelativePullToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_BODY_RELATIVE_PULL, (b) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.bodyRelativePull = b.getValue();
                this.commitChanges();
            }
        });

        this.hitDetectionToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_COLLISION, (b) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.hitDetection = b.getValue();
                this.hitRadiusPad.setEnabled(b.getValue());
                this.commitChanges();
            }
        });

        this.hitRadiusPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.hitRadius = v.floatValue();
                this.commitChanges();
            }
        });
        this.hitRadiusPad.limit(0.01F, 2F);
        this.hitRadiusPad.tooltip(UIKeys.MODELS_PHYS_BONES_COLLISION_RADIUS_TOOLTIP);

        this.relaxStepsPad = new UITrackpad((v) ->
        {
            SpringChainData chain = this.getOrCreateSelectedChain();

            if (chain != null)
            {
                chain.relaxSteps = Math.max(1, v.intValue());
                this.commitChanges();
            }
        });
        this.relaxStepsPad.limit(1, 16, true);
        this.relaxStepsPad.tooltip(UIKeys.MODELS_PHYS_BONES_SOLVER_STEPS_TOOLTIP);

        this.detailContainer.add(
            this.enabledToggle,
            UI.label(UIKeys.MODELS_PHYS_BONES_CHAIN_END),
            this.chainEndButton,
            UI.label(UIKeys.MODELS_PHYS_BONES_ANCHOR_END),
            this.anchorEndButton,
            UI.label(UIKeys.MODELS_PHYS_BONES_PULL_STRENGTH),
            this.pullStrengthPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_DRAG),
            this.dragPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_SPRING_RETURN),
            this.springReturnPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_PULL_ROTATION),
            UI.row(this.pullRotXPad, this.pullRotYPad, this.pullRotZPad),
            UI.label(UIKeys.MODELS_PHYS_BONES_INFLUENCE),
            this.influencePad,
            this.bodyRelativePullToggle,
            this.hitDetectionToggle,
            this.hitRadiusPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_SOLVER_STEPS),
            this.relaxStepsPad
        );

        /* Wind Section */
        UILabel windTitle = UI.label(UIKeys.MODELS_PHYS_BONES_WIND).background();

        this.windPowerPad = new UITrackpad((v) ->
        {
            this.wind.power = v.floatValue();
            this.commitChanges();
        });
        this.windPowerPad.limit(0F, 5F);

        this.windDirXPad = new UITrackpad((v) ->
        {
            this.wind.dirX = v.floatValue();
            this.commitChanges();
        });
        this.windDirXPad.limit(-1F, 1F);

        this.windDirYPad = new UITrackpad((v) ->
        {
            this.wind.dirY = v.floatValue();
            this.commitChanges();
        });
        this.windDirYPad.limit(-1F, 1F);

        this.windDirZPad = new UITrackpad((v) ->
        {
            this.wind.dirZ = v.floatValue();
            this.commitChanges();
        });
        this.windDirZPad.limit(-1F, 1F);

        this.windGustinessPad = new UITrackpad((v) ->
        {
            this.wind.gustiness = v.floatValue();
            this.commitChanges();
        });
        this.windGustinessPad.limit(0F, 2F);

        this.windGustSpeedPad = new UITrackpad((v) ->
        {
            this.wind.gustSpeed = v.floatValue();
            this.commitChanges();
        });
        this.windGustSpeedPad.limit(0F, 5F);

        this.windGustScalePad = new UITrackpad((v) ->
        {
            this.wind.gustScale = v.floatValue();
            this.commitChanges();
        });
        this.windGustScalePad.limit(0.1F, 10F);

        this.windModelRelativeToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_WIND_MODEL_RELATIVE, (b) ->
        {
            this.wind.modelRelative = b.getValue();
            this.commitChanges();
        });

        UIElement windFields = new UIElement();
        windFields.column().stretch().vertical().height(20).padding(4);
        windFields.add(
            windTitle,
            UI.label(UIKeys.MODELS_PHYS_BONES_WIND_POWER),
            this.windPowerPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_WIND_DIRECTION),
            UI.row(this.windDirXPad, this.windDirYPad, this.windDirZPad),
            UI.label(UIKeys.MODELS_PHYS_BONES_WIND_GUSTINESS),
            this.windGustinessPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_WIND_GUST_SPEED),
            this.windGustSpeedPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_WIND_GUST_SCALE),
            this.windGustScalePad,
            this.windModelRelativeToggle
        );

        this.options.add(
            topRow,
            UI.label(UIKeys.MODELS_PHYS_BONES_EDITOR),
            this.boneList,
            debugRow,
            this.boneNameLabel,
            this.noSelectionLabel,
            this.detailContainer,
            windFields
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

        return display.startsWith("✓ ") ? display.substring(2) : display;
    }

    private void refreshBoneList()
    {
        List<String> displayList = new ArrayList<>();

        for (String bone : this.boneNames)
        {
            if (this.chains.containsKey(bone))
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
            SpringChainData chain = this.chains.get(this.selectedBone);

            if (chain != null)
            {
                this.enabledToggle.setValue(true);
                this.chainEndButton.label = IKey.raw(chain.endBone == null || chain.endBone.isEmpty() ? "-" : chain.endBone);
                this.anchorEndButton.label = IKey.raw(chain.pinTarget == null || chain.pinTarget.isEmpty() ? "-" : chain.pinTarget);
                this.pullStrengthPad.setValue(chain.pullStrength);
                this.dragPad.setValue(chain.drag);
                this.springReturnPad.setValue(chain.springReturn);
                this.pullRotXPad.setValue(chain.pullRotX);
                this.pullRotYPad.setValue(chain.pullRotY);
                this.pullRotZPad.setValue(chain.pullRotZ);
                this.influencePad.setValue(chain.influence);
                this.bodyRelativePullToggle.setValue(chain.bodyRelativePull);
                this.hitDetectionToggle.setValue(chain.hitDetection);
                this.hitRadiusPad.setValue(chain.hitRadius);
                this.hitRadiusPad.setEnabled(chain.hitDetection);
                this.relaxStepsPad.setValue(chain.relaxSteps);
            }
            else
            {
                this.enabledToggle.setValue(false);
                this.chainEndButton.label = IKey.raw("-");
                this.anchorEndButton.label = IKey.raw("-");
                this.pullStrengthPad.setValue(1F);
                this.dragPad.setValue(0.15F);
                this.springReturnPad.setValue(SpringChainDef.DEFAULT_SPRING_RETURN);
                this.pullRotXPad.setValue(0F);
                this.pullRotYPad.setValue(0F);
                this.pullRotZPad.setValue(0F);
                this.influencePad.setValue(SpringChainDef.DEFAULT_INFLUENCE);
                this.bodyRelativePullToggle.setValue(false);
                this.hitDetectionToggle.setValue(false);
                this.hitRadiusPad.setValue(0.1F);
                this.hitRadiusPad.setEnabled(false);
                this.relaxStepsPad.setValue(4);
            }

            this.windPowerPad.setValue(this.wind.power);
            this.windDirXPad.setValue(this.wind.dirX);
            this.windDirYPad.setValue(this.wind.dirY);
            this.windDirZPad.setValue(this.wind.dirZ);
            this.windGustinessPad.setValue(this.wind.gustiness);
            this.windGustSpeedPad.setValue(this.wind.gustSpeed);
            this.windGustScalePad.setValue(this.wind.gustScale);
            this.windModelRelativeToggle.setValue(this.wind.modelRelative);
        }
        finally
        {
            this.suppressCommit = false;
        }
    }

    private SpringChainData getOrCreateSelectedChain()
    {
        if (this.selectedBone == null || this.selectedBone.isEmpty())
        {
            return null;
        }

        SpringChainData chain = this.chains.get(this.selectedBone);

        if (chain == null)
        {
            chain = SpringChainData.createDefault();
            this.chains.put(this.selectedBone, chain);
        }

        return chain;
    }

    private void onEnabledChanged(boolean enabled)
    {
        if (this.selectedBone == null)
        {
            return;
        }

        if (enabled)
        {
            this.getOrCreateSelectedChain();
        }
        else
        {
            this.chains.remove(this.selectedBone);
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

        Map<String, SpringChainDef> out = new HashMap<>();

        for (Map.Entry<String, SpringChainData> entry : this.chains.entrySet())
        {
            SpringChainData d = entry.getValue();
            out.put(entry.getKey(), new SpringChainDef(
                d.endBone,
                d.pinTarget,
                d.pullStrength,
                d.drag,
                d.springReturn,
                d.relaxSteps,
                d.bodyRelativePull,
                d.pullRotX,
                d.pullRotY,
                d.pullRotZ,
                d.hitDetection,
                d.hitRadius,
                d.influence
            ));
        }

        WindDef windDef = new WindDef(
            this.wind.power,
            this.wind.dirX,
            this.wind.dirY,
            this.wind.dirZ,
            this.wind.gustiness,
            this.wind.gustSpeed,
            this.wind.gustScale,
            this.wind.modelRelative
        );

        SpringChainsConfig springsConfig = new SpringChainsConfig(out, windDef);
        MapType map = SpringChainSerializer.toData(springsConfig);

        boolean empty = out.isEmpty() && windDef.isDefault();
        this.form.springs.preNotify();
        this.form.springs.set(empty ? null : map);
        this.form.springs.postNotify();

        SpringChainCompiler.clear();
        DynamicBoneOrchestrator.clearCache();

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
            if (loaded != null && loaded.springs.get() instanceof MapType map)
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
        this.chains.clear();
        this.wind = WindData.createDefault();

        if (map != null)
        {
            SpringChainsConfig springsConfig = SpringChainSerializer.fromData(map);

            if (springsConfig != null)
            {
                if (springsConfig.chains() != null)
                {
                    for (Map.Entry<String, SpringChainDef> entry : springsConfig.chains().entrySet())
                    {
                        this.chains.put(entry.getKey(), SpringChainData.fromDef(entry.getValue()));
                    }
                }

                this.wind = WindData.fromDef(springsConfig.wind());
            }
        }
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);

        this.selectedBone = null;
        this.chains.clear();
        this.wind = WindData.createDefault();
        this.boneNames.clear();

        if (form != null)
        {
            BaseType raw = form.springs.get();

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

    private static class SpringChainData
    {
        String endBone = "";
        String pinTarget = "";
        float pullStrength = 1F;
        float drag = 0.15F;
        float springReturn = SpringChainDef.DEFAULT_SPRING_RETURN;
        int relaxSteps = 4;
        boolean bodyRelativePull;
        float pullRotX;
        float pullRotY;
        float pullRotZ;
        boolean hitDetection;
        float hitRadius = 0.1F;
        float influence = SpringChainDef.DEFAULT_INFLUENCE;

        static SpringChainData createDefault()
        {
            return new SpringChainData();
        }

        static SpringChainData fromDef(SpringChainDef def)
        {
            SpringChainData data = new SpringChainData();
            data.endBone = def.endBone() == null ? "" : def.endBone();
            data.pinTarget = def.pinTarget() == null ? "" : def.pinTarget();
            data.pullStrength = def.pullStrength();
            data.drag = def.drag();
            data.springReturn = def.springReturn();
            data.relaxSteps = def.relaxSteps();
            data.bodyRelativePull = def.bodyRelativePull();
            data.pullRotX = def.pullRotX();
            data.pullRotY = def.pullRotY();
            data.pullRotZ = def.pullRotZ();
            data.hitDetection = def.hitDetection();
            data.hitRadius = def.hitRadius();
            data.influence = def.influence();

            return data;
        }
    }

    private static class WindData
    {
        float power = WindDef.NONE.power();
        float dirX = WindDef.NONE.dirX();
        float dirY = WindDef.NONE.dirY();
        float dirZ = WindDef.NONE.dirZ();
        float gustiness = WindDef.NONE.gustiness();
        float gustSpeed = WindDef.NONE.gustSpeed();
        float gustScale = WindDef.NONE.gustScale();
        boolean modelRelative = WindDef.NONE.modelRelative();

        static WindData createDefault()
        {
            return new WindData();
        }

        static WindData fromDef(WindDef def)
        {
            if (def == null)
            {
                return createDefault();
            }

            WindData data = new WindData();
            data.power = def.power();
            data.dirX = def.dirX();
            data.dirY = def.dirY();
            data.dirZ = def.dirZ();
            data.gustiness = def.gustiness();
            data.gustSpeed = def.gustSpeed();
            data.gustScale = def.gustScale();
            data.modelRelative = def.modelRelative();

            return data;
        }
    }
}
