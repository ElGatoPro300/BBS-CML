package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

import java.util.Map;

/**
 * Optional Minecut (or other) film chrome remounted around classic film widgets.
 * Provided by a Fabric addon via {@link mchorse.bbs_mod.ui.film.FilmUiCapabilities}.
 */
public interface IFilmUiWorkspace
{
    UIElement[] allPanels();

    void registerDockPanels(Map<String, UIElement> panelById);

    void attachFilmWidgets();

    void detachFilmWidgets(UIElement editor);

    boolean isAttached();

    void syncMountedBounds();

    void restoreEmbeddedVisibility();

    void tickRecordingUi();

    void refreshReplayCards();

    void renderPaletteDragGhost(UIContext context);

    UIElement getPlayerViewportHost();

    UIElement getReplayPropertiesHost();

    boolean tryReplaysCardZoom(UIContext context);

    void clearTracksPaletteDragUi();

    void refreshTracksCards();

    IModelTrackPlacement getModelTrackPlacement();
}
