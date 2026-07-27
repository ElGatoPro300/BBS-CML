package mchorse.bbs_mod.ui.utils.gizmo;

import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;

/**
 * CML-native contract for anything that renders a pickable {@link Gizmo}
 * into its own stencil-picking pass. Implemented by the Model, Form, Film/Replays and Animation
 * State editors so a single {@link GizmoController} can drive the pick -> hover -> drag lifecycle
 * for all of them instead of every editor re-implementing that bookkeeping by hand.
 */
public interface GizmoSurface
{
    /** The stencil framebuffer this surface last picked into (used both for gizmo handles and
     *  everything else the surface can pick, e.g. bones/forms). */
    StencilFormFramebuffer getGizmoStencil();

    /** Installs this surface's ray-casting strategy onto the transform right before a drag
     *  starts. Each surface keeps its own {@link UIPropTransform.IGizmoRayProvider} because the
     *  space the gizmo matrix lives in differs (model-local, form-local, camera-view-relative). */
    void prepareGizmoDrag(UIPropTransform transform);
}
