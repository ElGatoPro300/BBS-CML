package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.class_310;
import net.minecraft.class_5498;
import org.lwjgl.glfw.GLFW;

public class UIModelFirstPersonTransformEditor extends UIDashboardPanel
{
    public IUIModelPanelHost host;
    public ModelConfig config;

    public UIPropTransform transform;
    public UILabel handsLabel;
    public UISearchList<String> handsSearch;
    public UIStringList hands;
    public UIIcon back;

    private class_5498 lastPerspective;
    private Form lastForm;
    private boolean changed;
    private ModelInstance cachedModel;

    public UIModelFirstPersonTransformEditor(IUIModelPanelHost host, ModelConfig config)
    {
        super(host.getDashboard());

        this.host = host;
        this.config = config;

        this.handsLabel = UI.label(UIKeys.MODELS_HANDS).background(() -> Colors.A50 | BBSSettings.primaryColor.get());
        this.hands = new UIStringList((l) ->
        {
            int index = this.hands.getCurrentIndices().isEmpty() ? 0 : this.hands.getCurrentIndices().get(0);
            this.setSlot(index == 0 ? this.config.fpMain : this.config.fpOffhand);
        })
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }
        };
        this.hands.background = 0x88000000;
        this.hands.add(UIKeys.MODELS_ITEMS_FP_MAIN.get());
        this.hands.add(UIKeys.MODELS_ITEMS_FP_OFF.get());
        this.hands.setIndex(0);

        this.handsSearch = new UISearchList<>(this.hands);
        this.handsSearch.label(UIKeys.GENERAL_SEARCH);

        this.transform = new UIPropTransform();
        this.transform.callbacks(null, () ->
        {
            this.host.dirty();
            this.syncModel();
        });
        this.transform.relative(this).x(1F, -200).y(0.5F, 10).w(190).h(70);

        this.back = new UIIcon(Icons.CLOSE, (b) ->
        {
            this.host.getModelRenderer().dirty();
            this.host.returnFromSubEditor();
        });
        this.back.relative(this).x(1F, -26).y(6);

        this.handsSearch.relative(this.transform).x(0.5F).y(0F, -5).w(1F).h(80).anchor(0.5F, 1F);
        this.handsLabel.relative(this.handsSearch).y(-12).w(1F).h(12);

        this.add(this.transform, this.handsSearch, this.handsLabel, this.back);

        this.setSlot(this.config.fpMain);
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
            if (this.cachedModel.fpMain != null)
            {
                this.cachedModel.fpMain.transform.copy(this.config.fpMain.transform);
            }
            if (this.cachedModel.fpOffhand != null)
            {
                this.cachedModel.fpOffhand.transform.copy(this.config.fpOffhand.transform);
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

        this.lastPerspective = mc.field_1690.method_31044();
        mc.field_1690.method_31043(class_5498.field_26664);
        mc.field_1690.field_1842 = false;

        BBSModClient.getCameraController().remove(this.dashboard.camera);

        Morph morph = Morph.getMorph(mc.field_1724);

        if (morph != null)
        {
            this.lastForm = morph.getForm();
            this.changed = true;

            ModelForm form = new ModelForm();

            form.model.set(this.config.getId());
            morph.setForm(form);
        }

        this.acquireModel();
    }

    @Override
    public void disappear()
    {
        super.disappear();

        this.host.forceSave();
        this.restore();

        class_310.method_1551().field_1690.field_1842 = true;
        BBSModClient.getCameraController().add(this.dashboard.camera);
    }

    @Override
    public void close()
    {
        super.close();

        this.restore();
    }

    @Override
    public UIDashboardPanel getMainPanel()
    {
        return this.host.getModelPanel() != null ? this.host.getModelPanel() : this;
    }

    private void restore()
    {
        class_310 mc = class_310.method_1551();

        if (this.lastPerspective != null)
        {
            mc.field_1690.method_31043(this.lastPerspective);
            this.lastPerspective = null;
        }

        Morph morph = Morph.getMorph(mc.field_1724);

        if (morph != null && this.changed)
        {
            morph.setForm(this.lastForm);
            this.lastForm = null;
            this.changed = false;
        }

        this.cachedModel = null;
    }
}
