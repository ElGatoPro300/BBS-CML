package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.utils.Gizmo;

public class RegisterGizmoEvent
{
    public void register(int index, Gizmo.IGizmoHandler handler)
    {
        Gizmo.INSTANCE.register(index, handler);
    }
}
