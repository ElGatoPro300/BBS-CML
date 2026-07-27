package mchorse.bbs_mod;

import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.events.EventBus;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.packs.DynamicSourcePack;
import mchorse.bbs_mod.resources.packs.ExternalAssetsSourcePack;
import mchorse.bbs_mod.settings.SettingsManager;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.MapFactory;

import java.io.File;

/**
 * BBS utility class that provides easy access to BBS Mod's core components.
 */
public class BBS
{
    public static EventBus getEvents()
    {
        return BBSMod.events;
    }

    public static File getGameFolder()
    {
        return BBSMod.getGameFolder();
    }

    public static File getAssetsFolder()
    {
        return BBSMod.getAssetsFolder();
    }

    public static File getSettingsFolder()
    {
        return BBSMod.getSettingsFolder();
    }

    public static File getWorldFolder()
    {
        return BBSMod.getWorldFolder();
    }

    public static AssetProvider getProvider()
    {
        return BBSMod.getProvider();
    }

    public static DynamicSourcePack getDynamicSourcePack()
    {
        return BBSMod.getDynamicSourcePack();
    }

    public static ExternalAssetsSourcePack getOriginalSourcePack()
    {
        return BBSMod.getOriginalSourcePack();
    }

    public static SettingsManager getSettings()
    {
        return BBSMod.getSettings();
    }

    public static FormArchitect getForms()
    {
        return BBSMod.getForms();
    }

    public static FilmManager getFilms()
    {
        return BBSMod.getFilms();
    }

    public static ActionManager getActions()
    {
        return BBSMod.getActions();
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryCameraClips()
    {
        return BBSMod.getFactoryCameraClips();
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryActionClips()
    {
        return BBSMod.getFactoryActionClips();
    }
}
