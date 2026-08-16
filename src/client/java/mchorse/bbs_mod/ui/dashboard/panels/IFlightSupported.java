package mchorse.bbs_mod.ui.dashboard.panels;

public interface IFlightSupported
{
    public default boolean supportsRollFOVControl()
    {
        return true;
    }
}