package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegisterDockLayoutEvent
{
    private final Supplier<UIDashboard> dashboardSupplier;
    private final List<Consumer<UIDashboard>> layoutCustomizers = new ArrayList<>();

    public RegisterDockLayoutEvent(Supplier<UIDashboard> dashboardSupplier)
    {
        this.dashboardSupplier = dashboardSupplier;
    }

    public RegisterDockLayoutEvent(UIDashboard dashboard)
    {
        this.dashboardSupplier = () -> dashboard;
    }

    public UIDashboard getDashboard()
    {
        return this.dashboardSupplier == null ? null : this.dashboardSupplier.get();
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
