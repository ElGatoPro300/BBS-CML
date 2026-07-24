package mchorse.bbs_mod.ui.morphing;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.EditorSpectatorHelper;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.morphing.camera.ImmersiveMorphingCameraController;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

public class UIMorphingPanel extends UIDashboardPanel
{
    public UIFormPalette palette;
    public UIIcon morph;
    public UIIcon demorph;
    public UIIcon fromMob;

    private ImmersiveMorphingCameraController controller;

    public UIMorphingPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.palette = new UIFormPalette((form) ->
        {
            if (BBSSettings.morphingAutoMorph.get())
            {
                this.setForm(form);
            }
        });
        this.palette.updatable().cantExit();
        this.palette.immersive();
        this.palette.full(this);
        this.palette.editor.renderer.full(dashboard.getRoot());
        this.palette.canModify();

        this.morph = new UIIcon(Icons.USER, (b) ->
        {
            Form form = this.palette.list.getSelected();

            if (form != null)
            {
                this.setForm(form);
            }
        });
        this.morph.tooltip(UIKeys.MORPHING_MORPH, Direction.TOP);

        this.demorph = new UIIcon(Icons.POSE, (b) ->
        {
            this.palette.setSelected(null);
            this.setForm(null);
        });
        this.demorph.tooltip(UIKeys.MORPHING_DEMORPH, Direction.TOP);
        this.fromMob = new UIIcon(Icons.MORPH, (b) ->
        {
            Form form = Morph.getMobForm(MinecraftClient.getInstance().player);

            if (form != null)
            {
                this.palette.setSelected(form);
                this.setForm(form);
            }
        });
        this.fromMob.tooltip(UIKeys.MORPHING_FROM_MOB, Direction.TOP);

        this.palette.list.bar.add(this.fromMob, this.morph, this.demorph);

        this.add(this.palette);

        this.controller = new ImmersiveMorphingCameraController(() -> this.palette.editor.isEditing() ? this.palette.editor.renderer : null);
    }

    private void setForm(Form form)
    {
        ClientNetwork.sendPlayerForm(form);

        /* Keep the clicked form selected in the list (do not jump/clear highlight). */
        if (form != null)
        {
            this.palette.setSelected(form);
        }
    }

    @Override
    public boolean needsBackground()
    {
        /* Nested form editor uses its own orbit view; otherwise keep the world
         * behind a dark palette scrim (see UIFormPalette). */
        return this.palette.editor.isEditing();
    }

    @Override
    public boolean needsWorldRender()
    {
        return !this.palette.editor.isEditing();
    }

    @Override
    public boolean canPause()
    {
        /* Same as Film / Model Block morph pickers: keep the world ticking so
         * selected form thumbnails can advance idle (UI anim uses world time). */
        return false;
    }

    @Override
    public void appear()
    {
        super.appear();

        /* Morphing must never force Spectator — restore if Film/Model Block left it on. */
        EditorSpectatorHelper.restore();

        if (MinecraftClient.getInstance().player == null)
        {
            return;
        }

        Morph morph = ((IMorphProvider) MinecraftClient.getInstance().player).getMorph();

        this.palette.list.setupForms(BBSModClient.getFormCategories());
        this.palette.setSelected(morph.getForm());

        boolean autoMorph = BBSSettings.morphingAutoMorph.get();

        this.morph.setVisible(!autoMorph);
        this.demorph.setVisible(true);
        this.fromMob.setVisible(true);
        this.palette.list.refreshActionBar();

        BBSModClient.getCameraController().add(this.controller);
        MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    public void disappear()
    {
        super.disappear();

        BBSModClient.getCameraController().remove(this.controller);
        MinecraftClient.getInstance().options.setPerspective(Perspective.FIRST_PERSON);
    }

    @Override
    public void close()
    {
        super.close();

        BBSModClient.getCameraController().remove(this.controller);
    }
}