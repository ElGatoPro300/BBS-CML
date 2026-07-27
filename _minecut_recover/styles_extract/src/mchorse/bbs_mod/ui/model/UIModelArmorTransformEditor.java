package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.OrbitDistanceCamera;
import mchorse.bbs_mod.camera.controller.OrbitCameraController;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIOrbitCamera;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_5498;
import net.minecraft.class_746;
import org.lwjgl.glfw.GLFW;

public class UIModelArmorTransformEditor extends UIDashboardPanel
{
    private static final class_1799 HELMET = new class_1799(class_1802.field_8805);
    private static final class_1799 CHESTPLATE = new class_1799(class_1802.field_8058);
    private static final class_1799 LEGGINGS = new class_1799(class_1802.field_8348);
    private static final class_1799 BOOTS = new class_1799(class_1802.field_8285);

    public IUIModelPanelHost host;
    public ModelConfig config;

    public UIPropTransform transform;
    public UILabel armorLabel;
    public UISearchList<String> armorSearch;
    public UIStringList armorList;
    public UIIcon back;

    /* Camera */
    public UIOrbitCamera uiOrbitCamera;
    public OrbitCameraController orbitCameraController;

    private class_5498 lastPerspective;
    private Form lastForm;
    private boolean changed;
    private ModelInstance cachedModel;

    public UIModelArmorTransformEditor(IUIModelPanelHost host, ModelConfig config)
    {
        super(host.getDashboard());

        this.host = host;
        this.config = config;

        class_746 player = class_310.method_1551().field_1724;
        OrbitDistanceCamera orbit = new OrbitDistanceCamera();

        orbit.distance.setX(30);
        orbit.setFovRoll(false);
        this.uiOrbitCamera = new UIOrbitCamera();
        this.uiOrbitCamera.setControl(true);
        this.uiOrbitCamera.orbit = orbit;

        this.orbitCameraController = new OrbitCameraController(this.uiOrbitCamera.orbit);
        this.orbitCameraController.camera.position.set(player.method_19538().field_1352, player.method_19538().field_1351 + 1D, player.method_19538().field_1350);
        this.orbitCameraController.camera.rotation.set(0, MathUtils.toRad(player.field_6283), 0);

        this.armorLabel = UI.label(UIKeys.MODELS_ARMOR).background(() -> Colors.A50 | BBSSettings.primaryColor.get());
        this.armorList = new UIStringList((l) ->
        {
            int index = this.armorList.getCurrentIndices().isEmpty() ? 0 : this.armorList.getCurrentIndices().get(0);
            
            if (index >= 0 && index < ArmorType.values().length)
            {
                this.setSlot(this.config.armorSlots.get(ArmorType.values()[index]));
            }
        })
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }
        };
        this.armorList.background = 0x88000000;
        
        for (ArmorType type : ArmorType.values())
        {
            this.armorList.add(type.name().toLowerCase());
        }
        
        this.armorList.setIndex(0);

        this.armorSearch = new UISearchList<>(this.armorList);
        this.armorSearch.label(UIKeys.GENERAL_SEARCH);

        this.transform = new UIPropTransform();
        this.transform.callbacks(null, () ->
        {
            this.syncModel();
        });
        this.transform.relative(this).x(1F, -200).y(0.5F, 10).w(190).h(70);

        this.back = UIModelTransformEditorSupport.createBackButton(this.host, this, () ->
        {
            this.host.getModelRenderer().dirty();
            this.host.returnFromSubEditor();
        });

        this.armorSearch.relative(this.transform).x(0.5F).y(0F, -5).w(1F).h(80).anchor(0.5F, 1F);
        this.armorLabel.relative(this.armorSearch).y(-12).w(1F).h(12);

        this.add(this.uiOrbitCamera, this.transform, this.armorSearch, this.armorLabel, this.back);

        this.setSlot(this.config.armorSlots.get(ArmorType.values()[0]));
    }

    private void setSlot(ArmorSlot slot)
    {
        this.transform.setTransform(slot.transform);
    }

    private void acquireModel()
    {
        Morph morph = Morph.getMorph(class_310.method_1551().field_1724);

        if (morph != null && morph.getForm() instanceof ModelForm)
        {
            FormRenderer renderer = FormUtilsClient.getRenderer(morph.getForm());

            if (renderer instanceof ModelFormRenderer)
            {
                this.cachedModel = ((ModelFormRenderer) renderer).getModel();
                this.syncModel();
            }
        }
    }

    private void syncModel()
    {
        if (this.cachedModel != null)
        {
            for (ArmorType type : ArmorType.values())
            {
                ArmorSlot slot = this.cachedModel.armorSlots.get(type);
                ArmorSlot configSlot = this.config.armorSlots.get(type);

                if (slot != null && configSlot != null)
                {
                    slot.transform.copy(configSlot.transform);
                }
            }
        }
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public boolean canHideHUD()
    {
        return false;
    }

    @Override
    public UIDashboardPanel getMainPanel()
    {
        return this.host.getModelPanel() != null ? this.host.getModelPanel() : this;
    }

    @Override
    public void render(UIContext context)
    {
        if (this.cachedModel == null)
        {
            this.acquireModel();
        }

        super.render(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.getKeyCode() == GLFW.GLFW_KEY_ESCAPE)
        {
            this.host.getModelRenderer().dirty();
            this.host.returnFromSubEditor();
            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void appear()
    {
        super.appear();

        class_310 mc = class_310.method_1551();
        class_746 player = mc.field_1724;

        this.lastPerspective = mc.field_1690.method_31044();
        mc.field_1690.method_31043(class_5498.field_26665);
        mc.field_1690.field_1842 = false;

        BBSModClient.getCameraController().remove(this.dashboard.camera);
        BBSModClient.getCameraController().add(this.orbitCameraController);

        this.orbitCameraController.camera.position.set(player.method_19538().field_1352, player.method_19538().field_1351 + 1D, player.method_19538().field_1350);
        this.orbitCameraController.camera.rotation.set(0, MathUtils.toRad(player.field_6283), 0);
        ((OrbitDistanceCamera) this.uiOrbitCamera.orbit).distance.setX(14);

        Morph morph = Morph.getMorph(mc.field_1724);

        if (morph != null)
        {
            this.lastForm = morph.getForm();
            this.changed = true;

            ModelForm form = new ModelForm();

            form.model.set(this.config.getId());
            morph.setForm(form);

            morph.entity.setEquipmentStack(class_1304.field_6169, HELMET);
            morph.entity.setEquipmentStack(class_1304.field_6174, CHESTPLATE);
            morph.entity.setEquipmentStack(class_1304.field_6172, LEGGINGS);
            morph.entity.setEquipmentStack(class_1304.field_6166, BOOTS);
        }

        this.acquireModel();
    }

    @Override
    public void disappear()
    {
        super.disappear();

        Morph morph = Morph.getMorph(class_310.method_1551().field_1724);

        if (morph != null)
        {
            morph.entity.setEquipmentStack(class_1304.field_6169, class_1799.field_8037);
            morph.entity.setEquipmentStack(class_1304.field_6174, class_1799.field_8037);
            morph.entity.setEquipmentStack(class_1304.field_6172, class_1799.field_8037);
            morph.entity.setEquipmentStack(class_1304.field_6166, class_1799.field_8037);
        }

        this.host.forceSave();
        this.restore();

        class_310.method_1551().field_1690.field_1842 = true;

        BBSModClient.getCameraController().remove(this.orbitCameraController);
        BBSModClient.getCameraController().add(this.dashboard.camera);
    }

    private void restore()
    {
        if (this.changed)
        {
            Morph morph = Morph.getMorph(class_310.method_1551().field_1724);

            if (morph != null)
            {
                morph.setForm(this.lastForm);
            }
        }

        class_310.method_1551().field_1690.method_31043(this.lastPerspective);
    }
}
