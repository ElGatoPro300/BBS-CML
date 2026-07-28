package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterDockLayoutEvent
{
    public final UIDashboard dashboard;
    private final List<Consumer<UIDashboard>> layoutCustomizers = new ArrayList<>();

    public RegisterDockLayoutEvent(UIDashboard dashboard)
    {
        this.dashboard = dashboard;
    }

    public void registerCustomizer(Consumer<UIDashboard> customizer)
    {
        if (customizer != null)
        {
            this.layoutCustomizers.add(customizer);
        }
    }

    public List<Consumer<UIDashboard>> getLayoutCustomizers()
    {
        return this.layoutCustomizers;
    }
}
