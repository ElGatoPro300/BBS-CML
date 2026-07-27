package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.ui.film.FilmUiPanelIds;
import mchorse.bbs_mod.ui.film.IFilmUiWorkspace;
import mchorse.bbs_mod.ui.film.IModelTrackPlacement;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import net.minecraft.class_310;
import java.util.Map;

/**
 * Minecut chrome shells + remount helpers. Dock placement is owned by classic
 * {@link mchorse.bbs_mod.settings.values.ui.EditorLayoutNode} via {@link UIFilmPanel}.
 */
public class UIMinecutFilmWorkspace extends UIElement implements IFilmUiWorkspace
{
    public final UIMinecutMediaPanel replays;
    public final UIMinecutMediaPanel mediaCamera;
    public final UIMinecutMediaPanel mediaActions;
    public final UIMinecutMediaPanel mediaTracks;
    public final UIMinecutPropertiesPanel keyframe;
    public final UIMinecutPropertiesPanel propsCamera;
    public final UIMinecutPropertiesPanel propsAction;
    public final UIMinecutPlayerPanel player;
    public final UIMinecutTimelinePanel timelineReplay;
    public final UIMinecutTimelinePanel timelineCamera;
    public final UIMinecutTimelinePanel timelineAction;

    private final UIFilmPanel film;
    private final UIModelTrackPlacement modelTrackPlacement = new UIModelTrackPlacement();
    private boolean attached;

    public UIMinecutFilmWorkspace(UIFilmPanel film)
    {
        this.film = film;
        this.replays = new UIMinecutMediaPanel(film, UIMinecutMediaPanel.Mode.REPLAYS);
        this.mediaCamera = new UIMinecutMediaPanel(film, UIMinecutMediaPanel.Mode.CAMERA);
        this.mediaActions = new UIMinecutMediaPanel(film, UIMinecutMediaPanel.Mode.ACTIONS);
        this.mediaTracks = new UIMinecutMediaPanel(film, UIMinecutMediaPanel.Mode.TRACKS);
        this.keyframe = new UIMinecutPropertiesPanel("Keyframe");
        this.propsCamera = new UIMinecutPropertiesPanel("Camera");
        this.propsAction = new UIMinecutPropertiesPanel("Action");
        this.player = new UIMinecutPlayerPanel(film);
        this.timelineReplay = new UIMinecutTimelinePanel("Replay");
        this.timelineCamera = new UIMinecutTimelinePanel("Camera");
        this.timelineAction = new UIMinecutTimelinePanel("Action");

        for (UIElement panel : this.allPanels())
        {
            panel.setVisible(true);
        }
    }

    @Override
    public UIElement[] allPanels()
    {
        return new UIElement[] {
            this.replays, this.mediaCamera, this.mediaActions, this.mediaTracks,
            this.keyframe, this.propsCamera, this.propsAction,
            this.player,
            this.timelineReplay, this.timelineCamera, this.timelineAction
        };
    }

    @Override
    public void registerDockPanels(Map<String, UIElement> panelById)
    {
        panelById.put(FilmUiPanelIds.REPLAYS, this.replays);
        panelById.put(FilmUiPanelIds.MEDIA_CAMERA, this.mediaCamera);
        panelById.put(FilmUiPanelIds.MEDIA_ACTIONS, this.mediaActions);
        panelById.put(FilmUiPanelIds.MEDIA_TRACKS, this.mediaTracks);
        panelById.put(FilmUiPanelIds.KEYFRAME, this.keyframe);
        panelById.put(FilmUiPanelIds.PROPS_CAMERA, this.propsCamera);
        panelById.put(FilmUiPanelIds.PROPS_ACTION, this.propsAction);
        panelById.put(FilmUiPanelIds.PLAYER, this.player);
        panelById.put(FilmUiPanelIds.TIMELINE_REPLAY, this.timelineReplay);
        panelById.put(FilmUiPanelIds.TIMELINE_CAMERA, this.timelineCamera);
        panelById.put(FilmUiPanelIds.TIMELINE_ACTION, this.timelineAction);
    }

    @Override
    public UIElement getPlayerViewportHost()
    {
        return this.player.viewportHost;
    }

    @Override
    public UIElement getReplayPropertiesHost()
    {
        return this.replays.getReplayPropertiesHost();
    }

    @Override
    public boolean tryReplaysCardZoom(UIContext context)
    {
        return this.replays.tryCardZoom(context);
    }

    @Override
    public void clearTracksPaletteDragUi()
    {
        this.mediaTracks.clearPaletteDragUiOnly();
    }

    @Override
    public void refreshTracksCards()
    {
        this.mediaTracks.refreshCards();
    }

    @Override
    public IModelTrackPlacement getModelTrackPlacement()
    {
        return this.modelTrackPlacement;
    }

    public void attachFilmWidgets()
    {
        UIFilmPreview preview = this.film.preview;
        UIReplaysEditor replayEditor = this.film.replayEditor;
        UIClipsPanel cameraEditor = this.film.cameraEditor;
        UIClipsPanel actionEditor = this.film.actionEditor;
        UIElement editArea = this.film.editArea;
        UIElement cameraEditArea = this.film.cameraEditArea;
        UIElement actionEditArea = this.film.actionEditArea;
        UIReplaysOverlayPanel replaysOverlay = this.film.anchoredReplaysPanel;

        if (preview != null)
        {
            this.player.mountPreview(preview);
        }

        this.mountInto(this.keyframe.getHost(), editArea);
        this.mountInto(this.propsCamera.getHost(), cameraEditArea);
        this.mountInto(this.propsAction.getHost(), actionEditArea);

        if (replaysOverlay != null)
        {
            replaysOverlay.setVisible(false);
            replaysOverlay.attachPropertiesHost(this.replays.getReplayPropertiesHost());
        }

        if (this.film.anchoredReplaysPropertiesPanel != null)
        {
            this.film.anchoredReplaysPropertiesPanel.setVisible(false);
        }

        this.mountTimeline(this.timelineReplay, replayEditor);
        this.mountTimeline(this.timelineCamera, cameraEditor);
        this.mountTimeline(this.timelineAction, actionEditor);

        this.attached = true;
        this.replays.refreshCards();
        this.mediaCamera.refreshCards();
        this.mediaActions.refreshCards();
        this.restoreEmbeddedVisibility();
    }

    /**
     * Classic panels live inside Minecut shells but stay registered in {@code panelById}.
     * Dock layout passes must not leave them hidden / zero-flex.
     */
    public void restoreEmbeddedVisibility()
    {
        if (!this.attached)
        {
            return;
        }

        if (this.film.preview != null && this.film.preview.getParent() == this.player.viewportHost)
        {
            this.film.preview.setVisible(true);
        }

        if (this.film.editArea != null && this.film.editArea.getParent() == this.keyframe.getHost())
        {
            this.film.editArea.setVisible(true);
        }

        if (this.film.cameraEditArea != null && this.film.cameraEditArea.getParent() == this.propsCamera.getHost())
        {
            this.film.cameraEditArea.setVisible(true);
        }

        if (this.film.actionEditArea != null && this.film.actionEditArea.getParent() == this.propsAction.getHost())
        {
            this.film.actionEditArea.setVisible(true);
        }

        this.showTimelineEditor(this.film.replayEditor, this.timelineReplay);
        this.showTimelineEditor(this.film.cameraEditor, this.timelineCamera);
        this.showTimelineEditor(this.film.actionEditor, this.timelineAction);
    }

    private void showTimelineEditor(UIElement editor, UIMinecutTimelinePanel host)
    {
        if (editor == null || editor.getParent() != host.getContent())
        {
            return;
        }

        editor.setVisible(true);

        if (editor instanceof UIClipsPanel)
        {
            ((UIClipsPanel) editor).applyToolbarDockLayout();
        }
        else if (editor instanceof UIReplaysEditor)
        {
            ((UIReplaysEditor) editor).applyToolbarDockLayout();
        }
    }

    private void mountTimeline(UIMinecutTimelinePanel host, UIElement editor)
    {
        if (editor == null)
        {
            return;
        }

        host.getContent().removeAll();
        editor.removeFromParent();
        editor.resetFlex().relative(host.getContent()).w(1F).h(1F);
        host.getContent().add(editor);
        editor.setVisible(true);
    }

    private void mountInto(UIElement host, UIElement child)
    {
        if (host == null || child == null)
        {
            return;
        }

        child.removeFromParent();
        child.resetFlex().relative(host).w(1F).h(1F);
        host.removeAll();
        host.add(child);
        child.setVisible(true);
    }

    public void detachFilmWidgets(UIElement editor)
    {
        if (!this.attached || editor == null)
        {
            return;
        }

        if (this.film.preview != null)
        {
            this.film.preview.restoreIconsHome();
            this.film.preview.setViewportButtonsHidden(false);
        }

        this.remountToEditor(editor, this.film.preview);
        this.remountToEditor(editor, this.film.replayEditor);
        this.remountToEditor(editor, this.film.cameraEditor);
        this.remountToEditor(editor, this.film.actionEditor);
        this.remountToEditor(editor, this.film.editArea);
        this.remountToEditor(editor, this.film.cameraEditArea);
        this.remountToEditor(editor, this.film.actionEditArea);
        this.remountToEditor(editor, this.film.anchoredReplaysPropertiesPanel);
        this.remountToEditor(editor, this.film.anchoredReplaysPanel);

        if (this.film.anchoredReplaysPanel != null)
        {
            this.film.anchoredReplaysPanel.setPropertiesExternal(false);
        }

        this.attached = false;
    }

    private void remountToEditor(UIElement editor, UIElement child)
    {
        if (child == null)
        {
            return;
        }

        child.removeFromParent();
        editor.add(child);
    }

    /**
     * Keep mounted BBS widgets glued to shell hosts (bounds only — no remount).
     */
    public void syncMountedBounds()
    {
        if (!this.attached)
        {
            return;
        }

        UIFilmPreview preview = this.film.preview;

        if (preview != null && preview.getParent() == this.player.viewportHost
            && this.player.viewportHost.area.w > 0 && this.player.viewportHost.area.h > 0)
        {
            preview.resetFlex().relative(this.player.viewportHost).x(0).y(0).w(1F).h(1F);

            if (preview.icons.getParent() != this.player.toolsHost)
            {
                preview.attachIconsTo(this.player.toolsHost);
            }
        }

        this.pinIntoHost(this.keyframe.getHost(), this.film.editArea);
        this.pinIntoHost(this.propsCamera.getHost(), this.film.cameraEditArea);
        this.pinIntoHost(this.propsAction.getHost(), this.film.actionEditArea);

        this.pinTimelineEditor(this.film.replayEditor, this.timelineReplay);
        this.pinTimelineEditor(this.film.cameraEditor, this.timelineCamera);
        this.pinTimelineEditor(this.film.actionEditor, this.timelineAction);
    }

    private void pinIntoHost(UIElement host, UIElement child)
    {
        if (host == null || child == null || child.getParent() != host)
        {
            return;
        }

        child.resetFlex().relative(host).x(0).y(0).w(1F).h(1F);
        child.setVisible(true);
    }

    private void pinTimelineEditor(UIElement editor, UIMinecutTimelinePanel host)
    {
        if (editor == null || editor.getParent() != host.getContent())
        {
            return;
        }

        editor.resetFlex().relative(host.getContent()).x(0).y(0).w(1F).h(1F);
        editor.setVisible(true);
    }

    public boolean isAttached()
    {
        return this.attached;
    }

    public void refreshReplayCards()
    {
        this.replays.refreshCards();
    }

    public boolean isPaletteDragging()
    {
        return this.mediaCamera.isPaletteDragging()
            || this.mediaActions.isPaletteDragging()
            || this.mediaTracks.isPaletteDragging();
    }

    public void renderPaletteDragGhost(UIContext context)
    {
        if (this.mediaCamera.isPaletteDragging())
        {
            this.mediaCamera.renderPaletteDragGhost(context);
        }
        else if (this.mediaActions.isPaletteDragging())
        {
            this.mediaActions.renderPaletteDragGhost(context);
        }
        else if (this.mediaTracks.isPaletteDragging())
        {
            this.mediaTracks.renderPaletteDragGhost(context);
        }
    }

    public void tickRecordingUi()
    {
        /* Recording state is shown by the video recorder overlay / viewport icons. */
    }

    @Override
    public void resize()
    {
        super.resize();
        this.syncMountedBounds();
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (class_310.method_1551().field_1690.field_1903.method_1417(context.getKeyCode(), context.getScanCode()))
        {
            this.film.togglePlayback();

            return true;
        }

        return super.subKeyPressed(context);
    }
}
