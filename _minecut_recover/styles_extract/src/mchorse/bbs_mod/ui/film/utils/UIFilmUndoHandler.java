package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.utils.undo.UIViewUndo;
import mchorse.bbs_mod.ui.film.utils.undo.ValueChangeUndo;
import mchorse.bbs_mod.ui.forms.editors.UIFormUndoHandler;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.undo.CompoundUndo;
import mchorse.bbs_mod.utils.undo.IUndo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UIFilmUndoHandler extends UIFormUndoHandler
{
    private Timer actionsTimer = new Timer(100);
    private Set<BaseValue> syncData = new HashSet<>();
    private boolean isUndoing;

    /**
     * When an undo/redo would change both the embedded view and film data, the view
     * transition is applied first and the data change waits for the next key press.
     */
    private IUndo<ValueGroup> pendingSplitUndo;
    private int pendingSplitIndex = -1;

    public UIFilmUndoHandler(UIFilmPanel panel)
    {
        super(panel);
    }

    public boolean isUndoing()
    {
        return this.isUndoing;
    }

    /**
     * Records a UI-only undo step (embedded view navigation, scroll, selection, etc.)
     * so it is not bundled with the next data change undo.
     */
    public void pushUIViewUndo(MapType uiBefore, MapType uiAfter)
    {
        if (this.isUndoing || uiBefore == null || uiAfter == null)
        {
            return;
        }

        this.undoManager.pushUndo(new UIViewUndo((MapType) uiBefore.copy(), (MapType) uiAfter.copy()).noMerging());
    }

    public boolean undo(ValueGroup context)
    {
        this.isUndoing = true;

        try
        {
            if (this.pendingSplitUndo != null)
            {
                return this.completeSplitUndo(context);
            }

            List<IUndo<ValueGroup>> undos = this.undoManager.getUndos();
            int index = this.undoManager.getCurrentUndoIndex();

            while (index >= 0)
            {
                IUndo<ValueGroup> undo = undos.get(index);

                if (this.shouldDeferEmbeddedUndo(undo, false))
                {
                    index -= 1;

                    continue;
                }

                if (this.tryBeginSplitUndo(context, undo, index))
                {
                    return true;
                }

                undo.undo(context);
                this.undoManager.setPosition(index - 1);
                this.handleFilmUndos(undo, false);

                return true;
            }

            return false;
        }
        finally
        {
            this.isUndoing = false;
        }
    }

    public boolean redo(ValueGroup context)
    {
        this.isUndoing = true;

        try
        {
            if (this.pendingSplitUndo != null)
            {
                return this.completeSplitRedo(context);
            }

            List<IUndo<ValueGroup>> undos = this.undoManager.getUndos();
            int index = this.undoManager.getCurrentUndoIndex() + 1;

            while (index < undos.size())
            {
                IUndo<ValueGroup> undo = undos.get(index);

                if (this.shouldDeferEmbeddedUndo(undo, true))
                {
                    index += 1;

                    continue;
                }

                if (this.tryBeginSplitRedo(context, undo, index))
                {
                    return true;
                }

                undo.redo(context);
                this.undoManager.setPosition(index);
                this.handleFilmUndos(undo, true);

                return true;
            }

            return false;
        }
        finally
        {
            this.isUndoing = false;
        }
    }

    @Override
    public void reset()
    {
        super.reset();

        this.pendingSplitUndo = null;
        this.pendingSplitIndex = -1;
        this.undoManager.setCallback(this::handleFilmUndos);
    }

    private void handleFilmUndos(IUndo<ValueGroup> undo, boolean redo)
    {
        this.isUndoing = true;

        try
        {
            IUndo<ValueGroup> resolved = this.resolveUndoForUI(undo);
            MapType uiData = this.resolveUndoUIData(resolved, redo);

            if (uiData != null)
            {
                if (this.uiElement instanceof UIFilmPanel panel)
                {
                    uiData = this.preserveEmbeddedContext(uiData, panel, resolved, redo);
                    panel.applyFilmUndoData(uiData);
                }
                else
                {
                    UIElement root = this.uiElement.getRoot();

                    if (root != null)
                    {
                        root.applyAllUndoData(uiData);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            this.isUndoing = false;
        }
    }

    @Override
    public void handlePreValues(BaseValue baseValue, int flag)
    {
        if (this.isUndoing || this.isFilmMetadata(baseValue))
        {
            return;
        }

        super.handlePreValues(baseValue, flag);
    }

    @Override
    public void submitUndo()
    {
        this.pendingSplitUndo = null;
        this.pendingSplitIndex = -1;
        this.cachedValues.keySet().removeIf(this::isFilmMetadata);

        super.submitUndo();
    }

    @Override
    protected void handleValue(BaseValue value)
    {
        if (!this.isUndoing)
        {
            super.handleValue(value);
        }

        if (this.isReplayActions(value))
        {
            this.syncData.add(value);
            this.actionsTimer.mark();
        }
    }

    @Override
    protected void handleTimers()
    {
        super.handleTimers();

        if (this.actionsTimer.checkReset())
        {
            this.submitUndo();

            for (BaseValue syncData : this.syncData)
            {
                ClientNetwork.sendSyncData(((UIFilmPanel) this.uiElement).getData().getId(), syncData);
            }

            this.syncData.clear();
        }
    }

    private boolean completeSplitUndo(ValueGroup context)
    {
        IUndo<ValueGroup> undo = this.pendingSplitUndo;
        int index = this.pendingSplitIndex;

        this.pendingSplitUndo = null;
        this.pendingSplitIndex = -1;

        undo.undo(context);
        this.undoManager.setPosition(index - 1);
        this.handleFilmUndos(undo, false);

        return true;
    }

    private boolean completeSplitRedo(ValueGroup context)
    {
        IUndo<ValueGroup> undo = this.pendingSplitUndo;
        int index = this.pendingSplitIndex;

        this.pendingSplitUndo = null;
        this.pendingSplitIndex = -1;

        undo.redo(context);
        this.undoManager.setPosition(index);
        this.handleFilmUndos(undo, true);

        return true;
    }

    private boolean tryBeginSplitUndo(ValueGroup context, IUndo<ValueGroup> undo, int index)
    {
        if (!this.needsViewDataSplit(undo, false))
        {
            return false;
        }

        this.applySplitViewTransition(undo, false);
        this.pendingSplitUndo = undo;
        this.pendingSplitIndex = index;

        return true;
    }

    private boolean tryBeginSplitRedo(ValueGroup context, IUndo<ValueGroup> undo, int index)
    {
        if (!this.needsViewDataSplit(undo, true))
        {
            return false;
        }

        this.applySplitViewTransition(undo, true);
        this.pendingSplitUndo = undo;
        this.pendingSplitIndex = index;

        return true;
    }

    /**
     * A data undo/redo also changes embedded-view state — split it into view first, data second.
     */
    private boolean needsViewDataSplit(IUndo<ValueGroup> undo, boolean redo)
    {
        if (!(this.uiElement instanceof UIFilmPanel panel))
        {
            return false;
        }

        IUndo<ValueGroup> resolved = this.resolveUndoForUI(undo);

        if (!(resolved instanceof ValueChangeUndo))
        {
            return false;
        }

        MapType uiData = this.resolveUndoUIData(resolved, redo);

        if (uiData == null)
        {
            return false;
        }

        boolean snapshotEmbedded = this.hasEmbeddedClipView(uiData);
        boolean currentEmbedded = panel.hasEmbeddedClipView();

        return snapshotEmbedded != currentEmbedded;
    }

    private void applySplitViewTransition(IUndo<ValueGroup> undo, boolean redo)
    {
        if (this.uiElement instanceof UIFilmPanel panel)
        {
            IUndo<ValueGroup> resolved = this.resolveUndoForUI(undo);
            MapType uiData = this.resolveUndoUIData(resolved, redo);

            if (uiData != null)
            {
                panel.applyFilmUndoData(uiData);
            }
        }
    }

    private IUndo<ValueGroup> resolveUndoForUI(IUndo<ValueGroup> undo)
    {
        if (undo instanceof CompoundUndo)
        {
            IUndo<ValueGroup> resolved = ((CompoundUndo<ValueGroup>) undo).getFirst(ValueChangeUndo.class);

            if (resolved != null)
            {
                return resolved;
            }
        }

        return undo;
    }

    private boolean isFilmMetadata(BaseValue value)
    {
        String path = value.getPath().toString();

        return path.endsWith("/totalTimeWorked") || path.endsWith("/contributors") || path.contains("/contributors/");
    }

    private boolean isReplayActions(BaseValue value)
    {
        String path = value.getPath().toString();

        if (
            path.endsWith("/replays") ||
            path.contains("/keyframes/") ||
            path.contains("/properties/") ||
            path.endsWith("/drop_items_on_death") ||
            path.endsWith("/actor") ||
            path.endsWith("/enabled") ||
            path.endsWith("/form") ||
            path.endsWith("/inventory") ||
            path.contains("/drop_velocity_")
        ) {
            return true;
        }

        /* Specifically for overwriting full replay like what's done when recording
         * data in the world! */
        if (value.getParent() != null && value.getParent().getId().equals("replays"))
        {
            return true;
        }

        while (value != null)
        {
            if (value instanceof Clips clips && clips.getFactory() == BBSMod.getFactoryActionClips())
            {
                return true;
            }

            value = value.getParent();
        }

        return false;
    }

    private MapType resolveUndoUIData(IUndo<ValueGroup> undo, boolean redo)
    {
        if (undo instanceof UIViewUndo viewUndo)
        {
            return viewUndo.getUIData(redo);
        }
        else if (undo instanceof ValueChangeUndo change)
        {
            return change.getUIData(redo);
        }

        return null;
    }

    /**
     * While inside an embedded clip editor, only keep the view open for data undos that
     * belong to embedded content (keyframes, envelopes, etc.) whose snapshots were captured
     * without embedded context.
     */
    private MapType preserveEmbeddedContext(MapType uiData, UIFilmPanel panel, IUndo<ValueGroup> undo, boolean redo)
    {
        if (!(undo instanceof ValueChangeUndo change) || !panel.hasEmbeddedClipView())
        {
            return uiData;
        }

        String editorId = panel.getEmbeddedClipEditorUndoId();

        if (editorId.isEmpty())
        {
            return uiData;
        }

        MapType editorData = uiData.getMap(editorId);

        if (editorData != null && !editorData.getString("embedded_id").isEmpty())
        {
            return uiData;
        }

        if (!this.isEmbeddedContentChange(change.getName()))
        {
            return uiData;
        }

        MapType reference = panel.collectFilmUndoSnapshot();
        MapType referenceEditor = reference.getMap(editorId);

        if (referenceEditor == null || referenceEditor.getString("embedded_id").isEmpty())
        {
            return uiData;
        }

        MapType merged = (MapType) uiData.copy();
        MapType mergedEditor = merged.getMap(editorId);

        if (mergedEditor == null)
        {
            mergedEditor = new MapType();
            merged.put(editorId, mergedEditor);
        }

        MapType embeddedState = referenceEditor.getMap("embedded_state");

        mergedEditor.putString("embedded_id", referenceEditor.getString("embedded_id"));
        mergedEditor.putBool("embedded_stacked", referenceEditor.getBool("embedded_stacked"));

        if (embeddedState != null)
        {
            mergedEditor.put("embedded_state", embeddedState.copy());
        }

        return merged;
    }

    private boolean isEmbeddedContentChange(DataPath path)
    {
        String pathString = path.toString();

        if (pathString.contains("/keyframes/"))
        {
            return true;
        }

        if (pathString.contains("/envelope/") || pathString.contains("/envelopes/"))
        {
            return true;
        }

        if (pathString.contains("/curves/") || pathString.contains("/curve/"))
        {
            return true;
        }

        if (pathString.contains("/remap/") || pathString.contains("/remapper/"))
        {
            return true;
        }

        return false;
    }

    /**
     * Skip legacy "enter embedded view" UI steps while already inside an embedded editor,
     * so data undos can be processed first.
     */
    private boolean shouldDeferEmbeddedUndo(IUndo<ValueGroup> undo, boolean redo)
    {
        if (!(this.uiElement instanceof UIFilmPanel panel))
        {
            return false;
        }

        if (!(undo instanceof UIViewUndo viewUndo))
        {
            return false;
        }

        if (!panel.hasEmbeddedClipView())
        {
            return false;
        }

        MapType uiData = viewUndo.getUIData(!redo);

        return uiData != null && !this.hasEmbeddedClipView(uiData);
    }

    private boolean hasEmbeddedClipView(MapType uiData)
    {
        return this.hasEmbeddedClipView(uiData, "camera_editor") || this.hasEmbeddedClipView(uiData, "action_editor");
    }

    private boolean hasEmbeddedClipView(MapType uiData, String editorId)
    {
        MapType editorData = uiData.getMap(editorId);

        return editorData != null && !editorData.getString("embedded_id").isEmpty();
    }
}
