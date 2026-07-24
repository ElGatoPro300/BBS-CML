package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UIModelArmorSection extends UIModelSection
{
    public UIButton pickArmor;

    private String currentBone;

    public UIModelArmorSection(IUIModelPanelHost editor)
    {
        super(editor);

        this.pickArmor = new UIButton(IKey.constant("<none>"), (b) -> this.openArmorMenu());

        this.section.onToggle(() -> this.updateUI(this.editor.getSelectedBone()));

        this.fields.add(this.pickArmor, new UIButton(UIKeys.MODELS_HANDS_EDIT, (b) ->
        {
            this.editor.openTransformEditor(new UIModelArmorTransformEditor(this.editor, this.config));
        }));
    }

    @Override
    public void onBoneSelected(String bone)
    {
        this.updateUI(bone);
    }

    private void openArmorMenu()
    {
        if (this.config == null || this.currentBone == null)
        {
            return;
        }

        List<String> armorTypes = new ArrayList<>();
        armorTypes.add("<none>");
        for (ArmorType type : ArmorType.values())
        {
            armorTypes.add(type.name().toLowerCase());
        }

        UIModelItemsSection.UIStringListContextMenu menu = new UIModelItemsSection.UIStringListContextMenu(armorTypes, () ->
        {
            ArmorSlot slot = this.getSlotForBone(this.currentBone);
            ArmorType type = this.getTypeForSlot(slot);
            String label = type == null ? "<none>" : type.name().toLowerCase();
            
            return Collections.singleton(label);
        }, (selected) ->
        {
            for (ArmorType type : ArmorType.values())
            {
                ArmorSlot slot = this.config.armorSlots.get(type);

                if (slot != null && slot.group.get().equals(this.currentBone))
                {
                    slot.group.set("");
                }
            }

            if (!selected.equals("<none>"))
            {
                ArmorType type = ArmorType.valueOf(selected.toUpperCase());
                ArmorSlot slot = this.config.armorSlots.get(type);
                
                if (slot != null)
                {
                    slot.group.set(this.currentBone);
                }
            }

            this.editor.dirty();
            this.updateUI(this.currentBone);
        });

        this.getContext().replaceContextMenu(menu);
        menu.xy(this.pickArmor.area.x, this.pickArmor.area.ey()).w(this.pickArmor.area.w).h(200).bounds(this.getContext().menu.overlay, 5);
    }

    private ArmorSlot getSlotForBone(String bone)
    {
        if (this.config == null) return null;
        
        for (ArmorType type : ArmorType.values())
        {
            ArmorSlot slot = this.config.armorSlots.get(type);
            if (slot != null && slot.group.get().equals(bone))
            {
                return slot;
            }
        }
        
        return null;
    }

    private ArmorType getTypeForSlot(ArmorSlot slot)
    {
        if (this.config == null || slot == null) return null;

        for (ArmorType type : ArmorType.values())
        {
            if (this.config.armorSlots.get(type) == slot)
            {
                return type;
            }
        }
        
        return null;
    }

    private void updateUI(String bone)
    {
        this.currentBone = bone;

        if (this.currentBone == null)
        {
            this.pickArmor.label = IKey.constant("<none>");
            return;
        }
        
        ArmorSlot slot = this.getSlotForBone(this.currentBone);
        ArmorType type = this.getTypeForSlot(slot);
        
        this.pickArmor.label = IKey.constant(type == null ? "<none>" : type.name().toLowerCase());
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_ARMOR;
    }

    @Override
    public void setConfig(ModelConfig config)
    {
        super.setConfig(config);
        
        this.updateUI(this.editor.getSelectedBone());
    }
}
