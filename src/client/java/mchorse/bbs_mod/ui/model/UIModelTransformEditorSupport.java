package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public final class UIModelTransformEditorSupport
{
    private UIModelTransformEditorSupport()
    {}

    public static boolean isEmbedded(IUIModelPanelHost host)
    {
        return host.getModelPanel() == null;
    }

    public static ModelInstance getPreviewModel(IUIModelPanelHost host)
    {
        UIModelEditorRenderer renderer = host.getModelRenderer();

        return renderer != null ? renderer.getPreviewModelInstance() : null;
    }

    public static IEntity getPreviewEntity(IUIModelPanelHost host)
    {
        UIModelEditorRenderer renderer = host.getModelRenderer();

        return renderer != null ? renderer.getEntity() : null;
    }

    public static UIIcon createBackButton(IUIModelPanelHost host, UIElement parent, Runnable onBack)
    {
        boolean embedded = isEmbedded(host);
        UIIcon back = new UIIcon(embedded ? Icons.LEFTLOAD : Icons.CLOSE, (b) -> onBack.run());

        if (embedded)
        {
            back.relative(parent).x(6).y(6);
        }
        else
        {
            back.relative(parent).x(1F, -26).y(6);
        }

        return back;
    }

    public static void beginEmbeddedPreview(IUIModelPanelHost host, UIPropTransform transform)
    {
        UIModelEditorRenderer renderer = host.getModelRenderer();

        if (renderer != null && transform != null)
        {
            renderer.transform = transform;
            renderer.dirty();
        }
    }

    public static void beginEmbeddedFpHandPreview(IUIModelPanelHost host, ArmorSlot slot, UIPropTransform transform, boolean mainHand)
    {
        UIModelEditorRenderer renderer = host.getModelRenderer();

        if (renderer == null || slot == null)
        {
            return;
        }

        beginEmbeddedPreview(host, transform);
        renderer.beginFpHandPreview(slot, mainHand);

        String group = slot.group.get();

        if (!group.isEmpty())
        {
            host.setSelectedBone(group);
        }
    }

    public static void endEmbeddedPreview(IUIModelPanelHost host)
    {
        UIPoseEditor poseEditor = host.getPoseEditor();
        UIModelEditorRenderer renderer = host.getModelRenderer();

        if (renderer != null && poseEditor != null)
        {
            renderer.transform = poseEditor.transform;
            renderer.dirty();
        }
    }

    public static void clearEquipment(IEntity entity)
    {
        if (entity == null)
        {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            entity.setEquipmentStack(slot, ItemStack.EMPTY);
        }
    }
}
