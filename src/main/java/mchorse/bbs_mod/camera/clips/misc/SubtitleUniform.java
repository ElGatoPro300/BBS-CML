package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.colors.Color;

/**
 * Static property values used when {@link SubtitleClip#useKeyframes} is disabled.
 * Kept separate from keyframe channels so animation data is never destroyed.
 */
public class SubtitleUniform extends ValueGroup
{
    public static final String LINE_HEIGHT_PRECISE = "lineHeight_precise";
    public static final String MAX_WIDTH_PRECISE = "maxWidth_precise";

    public final ValueString text = new ValueString("text", "");
    public final ValueDouble x = new ValueDouble("x", 0D);
    public final ValueDouble y = new ValueDouble("y", 0D);
    public final ValueDouble size = new ValueDouble("size", 10D);
    public final ValueDouble anchorX = new ValueDouble("anchorX", 0.5D);
    public final ValueDouble anchorY = new ValueDouble("anchorY", 0.5D);
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueBoolean textShadow = new ValueBoolean("textShadow", true);
    public final ValueDouble windowX = new ValueDouble("windowX", 0.5D);
    public final ValueDouble windowY = new ValueDouble("windowY", 0.5D);
    public final ValueColor background = new ValueColor("background", new Color().set(0));
    public final ValueDouble backgroundOffset = new ValueDouble("backgroundOffset", 2D);
    public final ValueDouble shadow = new ValueDouble("shadow", 0D);
    public final ValueBoolean shadowOpaque = new ValueBoolean("shadowOpaque", false);
    public final ValueDouble lineHeight = new ValueDouble("lineHeight", 12D, 0D, Double.POSITIVE_INFINITY);
    public final ValueDouble maxWidth = new ValueDouble("maxWidth", 0D, 0D, Double.POSITIVE_INFINITY);

    public SubtitleUniform(String id)
    {
        super(id);

        this.add(this.text);
        this.add(this.x);
        this.add(this.y);
        this.add(this.size);
        this.add(this.anchorX);
        this.add(this.anchorY);
        this.add(this.color);
        this.add(this.textShadow);
        this.add(this.windowX);
        this.add(this.windowY);
        this.add(this.background);
        this.add(this.backgroundOffset);
        this.add(this.shadow);
        this.add(this.shadowOpaque);
        this.add(this.lineHeight);
        this.add(this.maxWidth);
    }

    @Override
    public BaseType toData()
    {
        MapType data = (MapType) super.toData();

        if (BBSSettings.isSaveAsCompatible())
        {
            double lineHeightValue = this.lineHeight.get();
            double maxWidthValue = this.maxWidth.get();

            /* Integer primary values for older builds that still use ValueInt. */
            data.putInt("lineHeight", (int) Math.round(lineHeightValue));
            data.putInt("maxWidth", (int) Math.round(maxWidthValue));
            /* Dual-write full precision for current builds. */
            data.putDouble(LINE_HEIGHT_PRECISE, lineHeightValue);
            data.putDouble(MAX_WIDTH_PRECISE, maxWidthValue);
        }

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data == null || !data.isMap())
        {
            return;
        }

        MapType map = data.asMap();

        if (map.has(LINE_HEIGHT_PRECISE))
        {
            this.lineHeight.set(map.getDouble(LINE_HEIGHT_PRECISE));
        }

        if (map.has(MAX_WIDTH_PRECISE))
        {
            this.maxWidth.set(map.getDouble(MAX_WIDTH_PRECISE));
        }
    }
}
