package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.VideoForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormPaintTransform;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIVideoOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;

public class UIVideoFormPanel extends UIFormPanel<VideoForm>
{
    public UIButton pick;
    public UIToggle billboard;
    public UIToggle linear;
    public UIToggle loop;
    public UITrackpad speed;
    public UITrackpad offset;

    public UIColor color;
    public UIFormColorAdjustments colorAdjustments;
    public UIColor paintColor;
    public UITrackpad paintIntensity;
    public UIFormPaintTransform paintTransform;
    public UIColor glowingColor;
    public UITrackpad glowIntensity;
    public UIPoseSectionCollapse colorSection;
    public UIPoseSectionCollapse glowSection;

    public UITrackpad offsetX;
    public UITrackpad offsetY;
    public UITrackpad rotation;
    public UIToggle shading;

    public UIVideoFormPanel(UIForm editor)
    {
        super(editor);

        this.pick = new UIButton(UIKeys.FORMS_EDITORS_VIDEO_PICK_VIDEO, (b) ->
        {
            UIVideoOverlayPanel panel = new UIVideoOverlayPanel((value) ->
            {
                String next = value == null ? "" : value;

                if (next.equals(UIKeys.GENERAL_NONE.get()) || next.equalsIgnoreCase("none"))
                {
                    next = "";
                }

                this.form.video.set(next);
                this.refreshPickLabel();
            }, this.getContext());

            UIOverlay.addOverlay(this.getContext(), panel.set(this.form.video.get()));
        });
        this.billboard = new UIToggle(UIKeys.FORMS_EDITORS_VIDEO_BILLBOARD, false, (b) -> this.form.billboard.set(b.getValue()));
        this.linear = new UIToggle(UIKeys.TEXTURES_LINEAR, true, (b) -> this.form.linear.set(b.getValue()));
        this.loop = new UIToggle(UIKeys.FORMS_EDITORS_VIDEO_LOOP, true, (b) -> this.form.loop.set(b.getValue()));
        this.speed = new UITrackpad((value) -> this.form.speed.set(value.floatValue()));
        this.speed.limit(0.01D, 8D).values(0.25D, 0.05D, 1D);
        this.speed.tooltip(UIKeys.FORMS_EDITORS_VIDEO_SPEED);
        this.offset = new UITrackpad((value) -> this.form.offset.set(value.intValue()));
        this.offset.integer();
        this.offset.tooltip(UIKeys.FORMS_EDITORS_VIDEO_OFFSET);

        this.color = new UIColor((value) ->
        {
            Color color = this.form.color.get().copy();
            Color next = Color.rgba(value);

            color.set(next.r, next.g, next.b, next.a);
            this.form.color.set(color);
        }).direction(Direction.LEFT).withAlpha();
        this.colorAdjustments = new UIFormColorAdjustments(() -> this.form.color.get(), (color) ->
        {
            this.form.color.setRuntimeValue(null);
            this.form.color.set(color);
        });
        this.paintColor = new UIColor((value) ->
        {
            Color color = Color.rgba(value);
            PaintSettings settings = this.form.paintSettings.get().copy();

            color.a = settings.intensity;
            this.form.paintColor.set(color);

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            settings.applyAutoShaderShadow();
            this.form.paintSettings.set(settings);
        }).direction(Direction.LEFT);
        this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);
        this.paintIntensity = new UITrackpad((value) ->
        {
            PaintSettings settings = this.form.paintSettings.get().copy();
            float intensity = PaintSettings.clampIntensity(value.floatValue());

            settings.intensity = intensity;
            settings.applyAutoShaderShadow();
            this.form.paintSettings.set(settings);

            Color legacy = this.form.paintColor.get().copy();

            legacy.a = intensity;
            this.form.paintColor.set(legacy);
        });
        this.paintIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D).limit(PaintSettings.MIN_INTENSITY, PaintSettings.MAX_INTENSITY);
        this.paintIntensity.tooltip(UIKeys.FORMS_EDITORS_PAINT_INTENSITY);
        this.paintTransform = new UIFormPaintTransform(() -> this.form.paintSettings.get(), (settings) -> this.form.paintSettings.set(settings));
        this.glowingColor = new UIColor((value) ->
        {
            Color color = Color.rgba(value);

            color.a = 1F;
            this.form.glowingColor.set(color);

            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            this.form.glowSettings.set(settings);
        }).direction(Direction.LEFT);
        this.glowingColor.tooltip(UIKeys.FORMS_EDITORS_GLOW);
        this.glowIntensity = new UITrackpad((value) ->
        {
            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.intensity = value.floatValue();
            this.form.glowSettings.set(settings);
        });
        this.glowIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.glowIntensity.tooltip(UIKeys.FORMS_EDITORS_GLOW_INTENSITY);
        this.colorSection = new UIPoseSectionCollapse(
            UIKeys.FILM_REPLAY_TRACK_COLOR,
            UIReplaysEditor.getColor("color"),
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_BLEND_COLOR).marginTop(4),
                this.color,
                UI.label(UIKeys.FORMS_EDITORS_PAINT_COLOR).marginTop(4),
                this.paintColor,
                UI.label(UIKeys.FORMS_EDITORS_PAINT_INTENSITY),
                this.paintIntensity,
                this.paintTransform,
                this.colorAdjustments.marginTop(4)
            )
        );
        this.glowSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GLOW,
            Colors.ORANGE,
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_GLOWING_COLOR).marginTop(4),
                this.glowingColor,
                UI.label(UIKeys.FORMS_EDITORS_GLOW_INTENSITY),
                this.glowIntensity
            )
        );

        this.offsetX = new UITrackpad((value) -> this.form.offsetX.set(value.floatValue()));
        this.offsetX.tooltip(UIKeys.FORMS_EDITORS_BILLBOARD_OFFSET_X);
        this.offsetY = new UITrackpad((value) -> this.form.offsetY.set(value.floatValue()));
        this.offsetY.tooltip(UIKeys.FORMS_EDITORS_BILLBOARD_OFFSET_Y);
        this.rotation = new UITrackpad((value) -> this.form.rotation.set(value.floatValue()));
        this.rotation.tooltip(UIKeys.FORMS_EDITORS_BILLBOARD_ROTATION);
        this.shading = new UIToggle(UIKeys.FORMS_EDITORS_BILLBOARD_SHADING, false, (b) -> this.form.shading.set(b.getValue()));

        this.options.add(this.pick, this.colorSection, this.glowSection, this.billboard, this.linear, this.loop);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VIDEO_SPEED).marginTop(8), this.speed);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VIDEO_OFFSET).marginTop(8), this.offset);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BILLBOARD_UV_SHIFT).marginTop(8), UI.row(this.offsetX, this.offsetY), this.rotation, this.shading);
    }

    @Override
    public void startEdit(VideoForm form)
    {
        super.startEdit(form);

        this.billboard.setValue(form.billboard.get());
        this.linear.setValue(form.linear.get());
        this.loop.setValue(form.loop.get());
        this.speed.setValue(form.speed.get());
        this.offset.setValue(form.offset.get());

        this.color.setColor(form.color.get().getARGBColor());
        this.colorAdjustments.syncFromForm();
        PaintSettings paint = form.paintSettings.get();
        Color paintDisplay = new Color();

        paint.resolveColor(form.paintColor.get(), paintDisplay);
        this.paintColor.setColor(paintDisplay.getRGBColor());
        this.paintIntensity.setValue(paint.intensity);
        this.paintTransform.syncFromForm();
        GlowSettings glow = form.glowSettings.get();
        Color glowDisplay = new Color();

        glow.resolveColor(form.glowingColor.get(), glowDisplay);
        this.glowingColor.setColor(glowDisplay.getRGBColor());
        this.glowIntensity.setValue(glow.intensity);

        this.offsetX.setValue(form.offsetX.get());
        this.offsetY.setValue(form.offsetY.get());
        this.rotation.setValue(form.rotation.get());
        this.shading.setValue(form.shading.get());
        this.refreshPickLabel();
    }

    private void refreshPickLabel()
    {
        String path = this.form == null ? "" : this.form.video.get();

        if (path == null || path.isEmpty())
        {
            this.pick.label = UIKeys.FORMS_EDITORS_VIDEO_PICK_VIDEO;

            return;
        }

        String name = path;

        if (path.startsWith("external:"))
        {
            name = new File(path.substring("external:".length())).getName();
        }
        else
        {
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

            if (slash >= 0 && slash + 1 < path.length())
            {
                name = path.substring(slash + 1);
            }
        }

        this.pick.label = IKey.constant(name);
    }
}
