package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;

public class RegisterActionConfigsEvent
{
    private final List<Object> actionConfigs = new ArrayList<>();

    public void register(Object actionConfig)
    {
        if (actionConfig != null)
        {
            this.actionConfigs.add(actionConfig);
        }
    }

    public List<Object> getActionConfigs()
    {
        return this.actionConfigs;
    }
}
