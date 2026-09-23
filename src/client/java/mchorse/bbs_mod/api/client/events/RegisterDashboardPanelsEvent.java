package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;

/**
 * Posted on the client when the dashboard panels are being populated.
 */
public class RegisterDashboardPanelsEvent
{
    public final UIDashboard dashboard;

    public RegisterDashboardPanelsEvent(UIDashboard dashboard)
    {
        this.dashboard = dashboard;
    }
}
