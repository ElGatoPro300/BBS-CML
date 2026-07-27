package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.ScreenNodeClip;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.utils.UI;

public class UIScreenNodeClip extends UIClip<ScreenNodeClip>
{
    public UIButton edit;
    private UIScreenNodeEditor nodeEditor;

    public UIScreenNodeClip(ScreenNodeClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.nodeEditor = new UIScreenNodeEditor();
        this.nodeEditor.setGraph(this.clip.graph.get());
        this.nodeEditor.setValue(this.clip.graph);
        this.nodeEditor.setUndoId("screen_node_editor");

        this.edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
        {
            this.editor.embedView(this.nodeEditor);
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_NODE_GRAPH), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.nodeEditor.setGraph(this.clip.graph.get());
        this.nodeEditor.setValue(this.clip.graph);
    }

    @Override
    protected UIScreenNodeEditor resolveClipEmbeddableView(String undoId)
    {
        return undoId.equals(this.nodeEditor.getUndoId()) ? this.nodeEditor : null;
    }
}
