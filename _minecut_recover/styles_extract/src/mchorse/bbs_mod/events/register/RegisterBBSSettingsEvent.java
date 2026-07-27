package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.settings.SettingsBuilder;

public class RegisterBBSSettingsEvent
{
    private final SettingsBuilder builder;

    public RegisterBBSSettingsEvent(SettingsBuilder builder)
    {
        this.builder = builder;
    }

    public SettingsBuilder getBuilder()
    {
        return this.builder;
    }
}
