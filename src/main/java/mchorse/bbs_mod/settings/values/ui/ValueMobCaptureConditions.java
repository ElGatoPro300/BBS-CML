package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;

/**
 * Last Conditions-tab choices from the mob autocapture overlay.
 * Hidden from the settings UI; persisted in the recording config.
 */
public class ValueMobCaptureConditions extends ValueGroup
{
    public final ValueBoolean capturePlayers = new ValueBoolean("capture_players", true);
    public final ValueBoolean playerNametags = new ValueBoolean("player_nametags", false);
    public final ValueBoolean playerModelForms = new ValueBoolean("player_model_forms", false);
    public final ValueDouble areaSize = new ValueDouble("area_size", 32D, 16D, 256D);
    public final ValueBoolean usePlayerOrigin = new ValueBoolean("use_player_origin", true);
    public final ValueBoolean includeHeight = new ValueBoolean("include_height", false);
    public final ValueDouble originX = new ValueDouble("origin_x", 0D);
    public final ValueDouble originY = new ValueDouble("origin_y", 0D);
    public final ValueDouble originZ = new ValueDouble("origin_z", 0D);

    public ValueMobCaptureConditions(String id)
    {
        super(id);

        this.add(this.capturePlayers);
        this.add(this.playerNametags);
        this.add(this.playerModelForms);
        this.add(this.areaSize);
        this.add(this.usePlayerOrigin);
        this.add(this.includeHeight);
        this.add(this.originX);
        this.add(this.originY);
        this.add(this.originZ);
    }
}
