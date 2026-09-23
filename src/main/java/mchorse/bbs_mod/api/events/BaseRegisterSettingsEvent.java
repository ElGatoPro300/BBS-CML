package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.ui.utils.icons.Icon;

import java.io.File;
import java.util.function.Consumer;

/**
 * Base class for settings registration events.
 */
public abstract class BaseRegisterSettingsEvent
{
    public Settings register(Icon icon, String id, File file, Consumer<SettingsBuilder> consumer)
    {
        return BBSMod.setupConfig(icon, id, file, consumer);
    }
}
