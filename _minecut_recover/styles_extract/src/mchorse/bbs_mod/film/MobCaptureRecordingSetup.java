package mchorse.bbs_mod.film;

import java.util.HashSet;
import java.util.Set;

/**
 * Pending mob-capture choices made in the pre-recording overlay.
 */
public class MobCaptureRecordingSetup
{
    public static MobCaptureRecordingSetup pending;

    public boolean captureMobs = true;
    public double areaSize = 32D;
    public final Set<String> selectedTypeIds = new HashSet<>();
    public final Set<Integer> selectedEntityIds = new HashSet<>();
    public final Set<Integer> vanillaPlaybackEntityIds = new HashSet<>();
    public final Set<String> vanillaPlaybackTypeIds = new HashSet<>();

    public boolean shouldCapture()
    {
        return this.captureMobs && !this.selectedEntityIds.isEmpty();
    }
}
