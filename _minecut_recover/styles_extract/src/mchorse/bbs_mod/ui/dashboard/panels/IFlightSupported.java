package mchorse.bbs_mod.ui.dashboard.panels;

import mchorse.bbs_mod.ui.dashboard.utils.UIOrbitCamera;
import mchorse.bbs_mod.ui.utils.Area;

public interface IFlightSupported
{
    public default boolean supportsRollFOVControl()
    {
        return true;
    }

    /**
     * Screen area within which left/right/middle click-drag may start rotating, rolling or
     * changing the FOV of the free camera (see {@link UIOrbitCamera}).
     *
     * Returning {@code null} (the default) leaves flight control unrestricted, i.e. any
     * otherwise-unhandled click anywhere on screen may start it. Panels with a dedicated 3D
     * viewport should return that viewport's area so flight control can't be triggered by
     * clicking elsewhere in the UI (e.g. the menu bar or a docked panel's empty space).
     */
    public default Area getFlightViewportArea()
    {
        return null;
    }
}