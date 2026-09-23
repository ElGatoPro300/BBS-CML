package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.utils.Gizmo;

public class RegisterGizmoEvent
{
    public void register(int index, Gizmo.IGizmoHandler handler)
    {
        Gizmo.INSTANCE.register(index, handler);
    }
}
