package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.ui.ValueMobCaptureConditions;

import java.util.HashSet;
import java.util.Set;

/**
 * Pending mob-capture choices made in the pre-recording overlay.
 */
public class MobCaptureRecordingSetup
{
    public static MobCaptureRecordingSetup pending;

    public boolean captureMobs = true;
    public boolean capturePlayers = true;
    public boolean playerNametags = false;
    public boolean playerModelForms = false;
    public double areaSize = 32D;
    public boolean usePlayerOrigin = true;
    public boolean includeHeight = false;
    public double originX;
    public double originY;
    public double originZ;
    public final Set<String> selectedTypeIds = new HashSet<>();
    public final Set<Integer> selectedEntityIds = new HashSet<>();
    public final Set<Integer> vanillaPlaybackEntityIds = new HashSet<>();
    public final Set<String> vanillaPlaybackTypeIds = new HashSet<>();

    public boolean shouldCapture()
    {
        return this.captureMobs && !this.selectedEntityIds.isEmpty();
    }

    public void loadFromPreferences()
    {
        ValueMobCaptureConditions prefs = BBSSettings.recordingMobCaptureConditions;

        if (prefs == null)
        {
            return;
        }

        this.capturePlayers = prefs.capturePlayers.get();
        this.playerNametags = prefs.playerNametags.get();
        this.playerModelForms = prefs.playerModelForms.get();
        this.areaSize = prefs.areaSize.get();
        this.usePlayerOrigin = prefs.usePlayerOrigin.get();
        this.includeHeight = prefs.includeHeight.get();
        this.originX = prefs.originX.get();
        this.originY = prefs.originY.get();
        this.originZ = prefs.originZ.get();
    }

    public void saveToPreferences()
    {
        ValueMobCaptureConditions prefs = BBSSettings.recordingMobCaptureConditions;

        if (prefs == null)
        {
            return;
        }

        prefs.capturePlayers.set(this.capturePlayers);
        prefs.playerNametags.set(this.playerNametags);
        prefs.playerModelForms.set(this.playerModelForms);
        prefs.areaSize.set(this.areaSize);
        prefs.usePlayerOrigin.set(this.usePlayerOrigin);
        prefs.includeHeight.set(this.includeHeight);

        /* Player-relative origin overwrites coords each open; keep the last
         * custom point so switching back to coordinates restores it. */
        if (!this.usePlayerOrigin)
        {
            prefs.originX.set(this.originX);
            prefs.originY.set(this.originY);
            prefs.originZ.set(this.originZ);
        }
    }
}
