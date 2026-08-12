package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.utils.InverseKinematics;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIInverseKinematicsEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.ArrayList;
import java.util.List;

public class UIInverseKinematicsKeyframeFactory extends UIKeyframeFactory<InverseKinematics>
{
    public UIInverseKinematicsEditor ikEditor;

    public UIInverseKinematicsKeyframeFactory(Keyframe<InverseKinematics> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.ikEditor = new UIInverseKinematicsEditor();
        this.ikEditor.callbacks(
            () -> this.keyframe.getValue(),
            (consumer) -> BaseValue.edit(this.keyframe, (value) -> consumer.accept(value.getValue()))
        );
        this.ikEditor.filmPanel(this::getFilmPanel);
        this.ikEditor.fillBones(this.collectBones());
        this.ikEditor.refresh();
        this.scroll.add(this.ikEditor);

        this.scroll.scroll.setScroll(0);
    }

    private List<String> collectBones()
    {
        List<String> bones = new ArrayList<>();
        UIFilmPanel panel = this.getFilmPanel();

        if (panel == null)
        {
            return bones;
        }

        int index = this.getReplayIndex(panel);

        if (index < 0)
        {
            return bones;
        }

        IEntity entity = panel.getController().getEntities().get(index);

        if (entity == null || entity.getForm() == null)
        {
            return bones;
        }

        FormRenderer renderer = FormUtilsClient.getRenderer(entity.getForm());
        List<String> fromMatrices = new ArrayList<>(renderer.collectMatrices(entity, 0F).keySet());

        fromMatrices.removeIf(String::isEmpty);

        if (fromMatrices.isEmpty())
        {
            bones.addAll(renderer.getBones());
        }
        else
        {
            bones.addAll(fromMatrices);
        }

        return bones;
    }

    private int getReplayIndex(UIFilmPanel panel)
    {
        Replay replay = panel.replayEditor.getReplay();

        if (replay != null)
        {
            int index = panel.getData().replays.getList().indexOf(replay);

            if (index >= 0)
            {
                return index;
            }
        }

        return panel.replayEditor.replays.replays.getIndex();
    }

    private UIFilmPanel getFilmPanel()
    {
        UIElement element = this.editor;

        while (element != null)
        {
            if (element instanceof UIFilmPanel panel)
            {
                return panel;
            }

            element = element.getParent();
        }

        return null;
    }

    @Override
    public void update()
    {
        super.update();

        this.ikEditor.fillBones(this.collectBones());
        this.ikEditor.refresh();
    }

    @Override
    public void resize()
    {
        this.ikEditor.w(1F);
        this.ikEditor.resize();

        for (UIElement child : this.scroll.getChildren(UIElement.class))
        {
            child.noCulling();
        }

        super.resize();
    }
}
