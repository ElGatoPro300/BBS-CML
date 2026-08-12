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

    private static final List<Consumer<UIDashboard>> dashboardOpenHandlers = new ArrayList<>();
    private static final List<Consumer<UIDashboard>> dashboardCloseHandlers = new ArrayList<>();

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

    public void registerDashboardOpen(Consumer<UIDashboard> handler)
    {
        if (handler != null)
        {
            dashboardOpenHandlers.add(handler);
        }
    }

    public void registerDashboardClose(Consumer<UIDashboard> handler)
    {
        if (handler != null)
        {
            dashboardCloseHandlers.add(handler);
        }
    }

    public List<Consumer<UIDashboard>> getLayoutCustomizers()
    {
        return this.layoutCustomizers;
    }

    public List<Consumer<UIDashboard>> getDashboardOpenHandlers()
    {
        return dashboardOpenHandlers;
    }

    public List<Consumer<UIDashboard>> getDashboardCloseHandlers()
    {
        return dashboardCloseHandlers;
    }

    public static void postDashboardOpen(UIDashboard dashboard)
    {
        for (Consumer<UIDashboard> handler : dashboardOpenHandlers)
        {
            try
            {
                handler.accept(dashboard);
            }
            catch (Throwable ignored)
            {}
        }
    }

    public static void postDashboardClose(UIDashboard dashboard)
    {
        for (Consumer<UIDashboard> handler : dashboardCloseHandlers)
        {
            try
            {
                handler.accept(dashboard);
            }
            catch (Throwable ignored)
            {}
        }
    }
}
