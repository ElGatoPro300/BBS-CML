package mchorse.bbs_minecut_ui;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterBBSSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.settings.SettingsBuilder;

/**
 * Common Minecut UI addon entry: settings category + Addons-panel catalog listing.
 */
public class MinecutUiAddon extends BBSAddon
{
    @Subscribe
    @Override
    public void onRegisterSourcePacks(RegisterSourcePacksEvent event)
    {
        event.provider.register(new InternalAssetsSourcePack(
            "bbs_minecut_ui",
            "assets/bbs_minecut_ui/assets",
            MinecutUiAddon.class
        ));
    }

    @Subscribe
    @Override
    public void onRegisterBBSSettings(RegisterBBSSettingsEvent event)
    {
        SettingsBuilder builder = event.getBuilder();

        builder.category("minecut_ui");
        BBSSettings.minecutDefaultTrackPose = builder.getBoolean("default_track_pose", true);
        BBSSettings.minecutDefaultTrackTransform = builder.getBoolean("default_track_transform", true);
        BBSSettings.minecutDefaultTrackVisible = builder.getBoolean("default_track_visible", false);
        BBSSettings.minecutDefaultTrackColor = builder.getBoolean("default_track_color", false);
        BBSSettings.minecutDefaultTrackOpacity = builder.getBoolean("default_track_opacity", false);
        BBSSettings.minecutDefaultTransformOverlays = builder.getInt("default_transform_overlays", 0, 0, 42);
        BBSSettings.minecutDefaultPoseOverlays = builder.getInt("default_pose_overlays", 0, 0, 42);
        BBSSettings.minecutDefaultColorOverlays = builder.getInt("default_color_overlays", 0, 0, 42);
    }
}
