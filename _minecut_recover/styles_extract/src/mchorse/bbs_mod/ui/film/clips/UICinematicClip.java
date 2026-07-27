package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.CinematicClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.HashMap;
import java.util.Map;

public class UICinematicClip extends UIClip<CinematicClip>
{
    private static final int COLOR_GRADE = Colors.MAGENTA;

    public UIButton edit;
    public UIKeyframeEditor keyframes;

    private final Map<String, Boolean> collapsed = new HashMap<>();

    public UICinematicClip(CinematicClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.backgroundRenderer((context) ->
        {
            UIReplaysEditor.renderBackground(context, this.keyframes.view, (Clips) this.clip.getParent(), this.clip.tick.get(), this.clip);
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("cinematic_keyframes");

        this.edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_KEYFRAMES), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.rebuildChannels();
    }

    private void rebuildChannels()
    {
        UIKeyframes view = this.keyframes.view;

        view.removeAllSheets();

        String key = "cinematic";
        boolean expanded = !this.collapsed.getOrDefault(key, false);

        UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
            "__cinematic__" + key,
            UIKeys.CAMERA_CLIPS_BBS_CINEMATIC,
            COLOR_GRADE,
            key,
            expanded,
            () ->
            {
                this.collapsed.put(key, !this.collapsed.getOrDefault(key, false));
                this.rebuildChannels();
            }
        );

        header.level = 0;
        view.addSheet(header);

        if (expanded)
        {
            UIKeyframeSheet shAberration = new UIKeyframeSheet(
                "aberration",
                UIKeys.CAMERA_CLIPS_CHANNEL_ABERRATION,
                Colors.RED,
                false,
                this.clip.aberration,
                null
            );
            shAberration.level = 1;
            shAberration.groupKey = key;
            view.addSheet(shAberration);

            UIKeyframeSheet shVHS = new UIKeyframeSheet(
                "vhs",
                UIKeys.CAMERA_CLIPS_CHANNEL_VHS,
                Colors.GREEN,
                false,
                this.clip.vhs,
                null
            );
            shVHS.level = 1;
            shVHS.groupKey = key;
            view.addSheet(shVHS);

            UIKeyframeSheet shLensDistortion = new UIKeyframeSheet(
                "lensDistortion",
                UIKeys.CAMERA_CLIPS_CHANNEL_LENS_DISTORTION,
                Colors.BLUE,
                false,
                this.clip.lensDistortion,
                null
            );
            shLensDistortion.level = 1;
            shLensDistortion.groupKey = key;
            view.addSheet(shLensDistortion);

            UIKeyframeSheet shVintage = new UIKeyframeSheet(
                "vintage",
                UIKeys.CAMERA_CLIPS_CHANNEL_VINTAGE,
                Colors.YELLOW,
                false,
                this.clip.vintage,
                null
            );
            shVintage.level = 1;
            shVintage.groupKey = key;
            view.addSheet(shVintage);

            UIKeyframeSheet shRadialBlur = new UIKeyframeSheet(
                "radialBlur",
                UIKeys.CAMERA_CLIPS_CHANNEL_RADIAL_BLUR,
                Colors.CYAN,
                false,
                this.clip.radialBlur,
                null
            );
            shRadialBlur.level = 1;
            shRadialBlur.groupKey = key;
            view.addSheet(shRadialBlur);

            UIKeyframeSheet shRain = new UIKeyframeSheet(
                "rain",
                UIKeys.CAMERA_CLIPS_CHANNEL_RAIN,
                0xff5577ff,
                false,
                this.clip.rain,
                null
            );
            shRain.level = 1;
            shRain.groupKey = key;
            view.addSheet(shRain);

            UIKeyframeSheet shDust = new UIKeyframeSheet(
                "dust",
                UIKeys.CAMERA_CLIPS_CHANNEL_DUST,
                0xffcccccc,
                false,
                this.clip.dust,
                null
            );
            shDust.level = 1;
            shDust.groupKey = key;
            view.addSheet(shDust);

            UIKeyframeSheet shLightLeak = new UIKeyframeSheet(
                "lightLeak",
                UIKeys.CAMERA_CLIPS_CHANNEL_LIGHT_LEAK,
                0xffffa033,
                false,
                this.clip.lightLeak,
                null
            );
            shLightLeak.level = 1;
            shLightLeak.groupKey = key;
            view.addSheet(shLightLeak);

            this.addHeatGroup(view, key);
        }

        this.keyframes.view.getGraph().clearSelection();
    }

    private void addHeatGroup(UIKeyframes view, String parentKey)
    {
        String heatKey = "heat";
        boolean expanded = !this.collapsed.getOrDefault(heatKey, false);

        UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
            "__cinematic__" + heatKey,
            UIKeys.CAMERA_CLIPS_CHANNEL_HEAT_DISTORTION,
            0xffff6633,
            heatKey,
            expanded,
            () ->
            {
                this.collapsed.put(heatKey, !this.collapsed.getOrDefault(heatKey, false));
                this.rebuildChannels();
            }
        );

        header.level = 1;
        header.groupKey = parentKey;
        view.addSheet(header);

        if (expanded)
        {
            UIKeyframeSheet shStrength = new UIKeyframeSheet(
                "heat_strength",
                UIKeys.CAMERA_CLIPS_CHANNEL_HEAT_STRENGTH,
                0xffff4422,
                false,
                this.clip.heatStrength,
                null
            );
            shStrength.level = 2;
            shStrength.groupKey = heatKey;
            view.addSheet(shStrength);

            UIKeyframeSheet shSpeed = new UIKeyframeSheet(
                "heat_speed",
                UIKeys.CAMERA_CLIPS_CHANNEL_HEAT_SPEED,
                0xffff8844,
                false,
                this.clip.heatSpeed,
                null
            );
            shSpeed.level = 2;
            shSpeed.groupKey = heatKey;
            view.addSheet(shSpeed);

            UIKeyframeSheet shScale = new UIKeyframeSheet(
                "heat_scale",
                UIKeys.CAMERA_CLIPS_CHANNEL_HEAT_SCALE,
                0xffffaa66,
                false,
                this.clip.heatScale,
                null
            );
            shScale.level = 2;
            shScale.groupKey = heatKey;
            view.addSheet(shScale);
        }
    }

    @Override
    protected UIKeyframeEditor resolveClipEmbeddableView(String undoId)
    {
        return undoId.equals(this.keyframes.getUndoId()) ? this.keyframes : null;
    }
}
