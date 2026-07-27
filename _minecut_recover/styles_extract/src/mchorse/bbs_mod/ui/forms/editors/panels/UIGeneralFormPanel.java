package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.forms.forms.utils.InverseKinematics;
import mchorse.bbs_mod.forms.forms.utils.LookAt;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UIInverseKinematicsEditor;
import mchorse.bbs_mod.ui.framework.elements.input.UIKeybind;
import mchorse.bbs_mod.ui.framework.elements.input.UILookAtEditor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIIllusionKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import com.mojang.logging.LogUtils;

import java.util.function.Consumer;

import org.slf4j.Logger;

public class UIGeneralFormPanel extends UIFormPanel
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public UIKeybind hotkey;

    public UIToggle visible;
    public UIToggle animatable;
    public UITextbox trackName;
    public UIToggle lighting;
    public UIToggle noshadingOpacity;
    public UIToggle shaderShadow;
    public UITrackpad renderDepth;
    public UIToggle renderDepthEnabled;
    public UIPoseSectionCollapse renderDepthSection;
    public UILookAtEditor lookAt;
    public UIInverseKinematicsEditor inverseKinematics;
    public UITrackpad illusionCount;
    public UITrackpad illusionSpread;
    public UIToggle illusionFront;
    public UIToggle illusionBack;
    public UIToggle illusionLeft;
    public UIToggle illusionRight;
    public UIToggle illusionUp;
    public UIToggle illusionDown;
    public UIToggle illusionUniform;
    public UITrackpad illusionSpacing;
    public UITrackpad illusionOffset;
    public UITrackpad illusionOpacity;
    public UIToggle illusionOpacityUniform;
    public UIToggle illusionInvert;
    public UIButton illusionTextures;
    public UIButton illusionTexturesClear;
    public UIToggle illusionRandomTextures;
    public UIToggle illusionReal;
    public UITrackpad illusionDelay;
    public UITrackpad illusionDistort;
    public UIToggle illusionDistortUniform;
    public UIToggle illusionDistortInvert;
    public UITrackpad illusionGlow;
    public UIToggle illusionGlowUniform;
    public UIToggle illusionGlowInvert;
    public UIPropTransform illusionTransformEditor;
    public UIToggle illusionGradual;
    public UIToggle illusionGradualInvert;
    public UITrackpad uiScale;
    public UITextbox name;
    public UIPropTransform transform;

    public UIColor color;
    public UITrackpad blendIntensity;
    public UIFormColorAdjustments colorAdjustments;
    public UIEffectTransformCollapse colorTransform;
    public UIColor paintColor;
    public UITrackpad paintIntensity;
    public UIEffectTransformCollapse paintTransform;
    public UIColor glowingColor;
    public UITrackpad glowIntensity;
    public UITrackpad opacity;
    public UIPoseSectionCollapse colorSection;
    public UIPoseSectionCollapse glowSection;

    public UIToggle hitbox;
    public UITrackpad hitboxWidth;
    public UITrackpad hitboxHeight;
    public UITrackpad hitboxSneakMultiplier;
    public UITrackpad hitboxEyeHeight;
    public UIPoseSectionCollapse hitboxSection;

    public UITrackpad hp;
    public UITrackpad speed;
    public UITrackpad stepHeight;

    public UIPoseSectionCollapse lookAtSection;
    public UIPoseSectionCollapse inverseKinematicsSection;
    public UIPoseSectionCollapse illusionSection;

    public UIGeneralFormPanel(UIForm editor)
    {
        super(editor);

        this.hotkey = new UIKeybind((combo) ->
        {
            this.form.hotkey.set(combo.keys.isEmpty() ? 0 : combo.keys.get(0));
        });
        this.hotkey.single().emptyLabel(UIKeys.FORMS_EDITORS_GENERAL_HOTKEY).tooltip(UIKeys.FORMS_EDITORS_GENERAL_HOTKEY);

        this.visible = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_VISIBLE, (b) -> this.form.visible.set(b.getValue()));
        this.animatable = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ANIMATABLE, (b) -> this.form.animatable.set(b.getValue()));
        this.animatable.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ANIMATABLE_TOOLTIP);
        this.trackName = new UITextbox(120, (t) -> this.form.trackName.set(t));
        this.trackName.placeholder(UIKeys.FORMS_EDITORS_GENERAL_TRACK_NAME);
        this.trackName.tooltip(UIKeys.FORMS_EDITORS_GENERAL_TRACK_NAME_TOOLTIP);
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) -> this.form.lighting.set(b.getValue() ? 1F : 0F));
        this.lighting.tooltip(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING_TOOLTIP);
        this.noshadingOpacity = new UIToggle(UIKeys.FORMS_EDITORS_COLOR_NOSHADING_OPACITY, (b) -> this.form.noshadingOpacity.set(b.getValue()));
        this.noshadingOpacity.tooltip(UIKeys.FORMS_EDITORS_COLOR_NOSHADING_OPACITY_TOOLTIP);
        this.shaderShadow = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_SHADER_SHADOW, (b) -> this.form.shaderShadow.set(b.getValue()));
        this.renderDepth = new UITrackpad((v) -> this.form.renderDepth.set(v.floatValue()));
        this.renderDepth.tooltip(UIKeys.FORMS_EDITORS_GENERAL_RENDER_DEPTH_TOOLTIP);
        this.renderDepthEnabled = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_RENDER_DEPTH, (b) -> this.form.renderDepthEnabled.set(b.getValue()));
        this.renderDepthEnabled.tooltip(UIKeys.FORMS_EDITORS_GENERAL_RENDER_DEPTH_TOOLTIP);
        this.lookAt = new UILookAtEditor();
        this.lookAt.callbacks(() -> this.form.lookAt.get(), this::editLookAt);
        this.inverseKinematics = new UIInverseKinematicsEditor();
        this.inverseKinematics.callbacks(() -> this.form.inverseKinematics.get(), this::editInverseKinematics);
        this.illusionCount = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.count = v.intValue()));
        this.illusionCount.limit(0D).integer();
        this.illusionSpread = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.spread = v.floatValue()));
        this.illusionSpread.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPREAD_TOOLTIP);
        this.illusionFront = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_FRONT, (b) -> this.toggleIllusionDirection(Illusion.FRONT, b.getValue()));
        this.illusionBack = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_BACK, (b) -> this.toggleIllusionDirection(Illusion.BACK, b.getValue()));
        this.illusionLeft = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_LEFT, (b) -> this.toggleIllusionDirection(Illusion.LEFT, b.getValue()));
        this.illusionRight = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_RIGHT, (b) -> this.toggleIllusionDirection(Illusion.RIGHT, b.getValue()));
        this.illusionUp = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_UP, (b) -> this.toggleIllusionDirection(Illusion.UP, b.getValue()));
        this.illusionDown = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DOWN, (b) -> this.toggleIllusionDirection(Illusion.DOWN, b.getValue()));
        this.illusionUniform = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_UNIFORM, (b) -> this.editIllusion((illusion) -> illusion.uniform = b.getValue()));
        this.illusionUniform.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_UNIFORM_TOOLTIP);
        this.illusionSpacing = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.spacing = v.floatValue()));
        this.illusionSpacing.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPACING_TOOLTIP);
        this.illusionOffset = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.offset = v.floatValue()));
        this.illusionOffset.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OFFSET_TOOLTIP);
        this.illusionOpacity = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.opacity = v.floatValue() / 100F));
        this.illusionOpacity.limit(0D).tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OPACITY_TOOLTIP);
        this.illusionOpacityUniform = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OPACITY_UNIFORM, (b) -> this.editIllusion((illusion) -> illusion.opacityUniform = b.getValue()));
        this.illusionOpacityUniform.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OPACITY_UNIFORM_TOOLTIP);
        this.illusionInvert = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_INVERT, (b) -> this.editIllusion((illusion) -> illusion.invert = b.getValue()));
        this.illusionTextures = new UIButton(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES, (b) ->
        {
            UIIllusionKeyframeFactory.pickTextures(this.getContext(), () -> this.form.illusion.get().textures, (list) -> this.editIllusion((illusion) ->
            {
                illusion.textures.clear();
                illusion.textures.addAll(list);
            }));
        });
        this.illusionTextures.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_TOOLTIP);
        this.illusionTexturesClear = new UIButton(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_CLEAR, (b) -> this.editIllusion((illusion) -> illusion.textures.clear()));
        this.illusionRandomTextures = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_RANDOM, (b) -> this.editIllusion((illusion) -> illusion.randomTextures = b.getValue()));
        this.illusionRandomTextures.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TEXTURES_RANDOM_TOOLTIP);
        this.illusionReal = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_REAL, (b) -> this.editIllusion((illusion) -> illusion.real = b.getValue()));
        this.illusionReal.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_REAL_TOOLTIP);
        this.illusionDelay = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.delay = v.floatValue()));
        this.illusionDelay.limit(0D).tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DELAY_TOOLTIP);
        this.illusionDistort = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.distort = v.floatValue()));
        this.illusionDistort.limit(0D).tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT_TOOLTIP);
        this.illusionDistortUniform = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT_UNIFORM, (b) -> this.editIllusion((illusion) -> illusion.distortUniform = b.getValue()));
        this.illusionDistortUniform.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT_UNIFORM_TOOLTIP);
        this.illusionDistortInvert = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT_INVERT, (b) -> this.editIllusion((illusion) -> illusion.distortInvert = b.getValue()));
        this.illusionDistortInvert.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT_INVERT_TOOLTIP);
        this.illusionGlow = new UITrackpad((v) -> this.editIllusion((illusion) -> illusion.glow = v.floatValue()));
        this.illusionGlow.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_TOOLTIP);
        this.illusionGlowUniform = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_UNIFORM, (b) -> this.editIllusion((illusion) -> illusion.glowUniform = b.getValue()));
        this.illusionGlowUniform.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_UNIFORM_TOOLTIP);
        this.illusionGlowInvert = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_INVERT, (b) -> this.editIllusion((illusion) -> illusion.glowInvert = b.getValue()));
        this.illusionGlowInvert.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW_INVERT_TOOLTIP);
        this.illusionTransformEditor = new UIPropTransform().callbacks(
            () -> this.form.illusion.preNotify(),
            () ->
            {
                this.form.illusion.postNotify();
                this.editor.startEdit(this.form);
            }
        );
        this.illusionGradual = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GRADUAL, (b) -> this.editIllusion((illusion) -> illusion.gradual = b.getValue()));
        this.illusionGradual.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GRADUAL_TOOLTIP);
        this.illusionGradualInvert = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GRADUAL_INVERT, (b) -> this.editIllusion((illusion) -> illusion.gradualInvert = b.getValue()));
        this.illusionGradualInvert.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GRADUAL_INVERT_TOOLTIP);
        this.uiScale = new UITrackpad((v) -> this.form.uiScale.set(v.floatValue()));
        this.uiScale.limit(0.01D, 100D);
        this.name = new UITextbox(120, (t) ->
        {
            this.form.name.set(t);
            LOGGER.info("Form display name changed: formId={}, name={}", this.form.getFormId(), t);
        });
        this.name.placeholder(UIKeys.FORMS_EDITORS_GENERAL_DISPLAY);

        this.transform = new UIPropTransform().callbacks(() -> this.form.transform);
        this.transform.enableHotkeys().relative(this).x(0.5F).y(1F, -10).anchor(0.5F, 1F);

        this.setupColorAndGlow();

        this.hitbox = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_HITBOX, (b) -> this.form.hitbox.set(b.getValue()));
        this.hitboxWidth = new UITrackpad((v) -> this.form.hitboxWidth.set(v.floatValue()));
        this.hitboxWidth.limit(0).tooltip(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_WIDTH);
        this.hitboxHeight = new UITrackpad((v) -> this.form.hitboxHeight.set(v.floatValue()));
        this.hitboxHeight.limit(0).tooltip(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_HEIGHT);
        this.hitboxSneakMultiplier = new UITrackpad((v) -> this.form.hitboxSneakMultiplier.set(v.floatValue()));
        this.hitboxSneakMultiplier.limit(0, 1);
        this.hitboxEyeHeight = new UITrackpad((v) -> this.form.hitboxEyeHeight.set(v.floatValue()));
        this.hitboxEyeHeight.limit(0, 1);

        this.hp = new UITrackpad((v) -> this.form.hp.set(v.floatValue()));
        this.hp.limit(1F);
        this.speed = new UITrackpad((v) -> this.form.speed.set(v.floatValue()));
        this.speed.limit(0F);
        this.stepHeight = new UITrackpad((v) -> this.form.stepHeight.set(v.floatValue()));
        this.stepHeight.limit(0F);

        this.renderDepthSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GENERAL_RENDER_DEPTH,
            UIReplaysEditor.getColor("render_depth"),
            UI.column(5, 0,
                this.renderDepthEnabled,
                this.renderDepth
            )
        );
        this.hitboxSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GENERAL_HITBOX,
            0x66aa44,
            UI.column(5, 0,
                this.hitbox,
                UI.row(this.hitboxWidth, this.hitboxHeight),
                UI.label(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_SNEAK_MULTIPLIER),
                this.hitboxSneakMultiplier,
                UI.label(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_EYE_HEIGHT),
                this.hitboxEyeHeight
            )
        );

        this.lookAtSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GENERAL_LOOK_AT,
            UIReplaysEditor.getColor("look_at"),
            UI.column(5, 0, this.lookAt),
            this::refreshLookAt
        );
        this.inverseKinematicsSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GENERAL_INVERSE_KINEMATICS,
            UIReplaysEditor.getColor("inverse_kinematics"),
            UI.column(5, 0, this.inverseKinematics),
            this::refreshInverseKinematics
        );

        UIElement illusionContent = UI.column(5, 0,
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_COUNT), this.illusionCount),
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_SPREAD), this.illusionSpread),
            UI.row(this.illusionFront, this.illusionBack),
            UI.row(this.illusionLeft, this.illusionRight),
            UI.row(this.illusionUp, this.illusionDown),
            this.illusionUniform,
            this.illusionSpacing,
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OFFSET), this.illusionOffset),
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_OPACITY), this.illusionOpacity),
            UI.row(this.illusionOpacityUniform, this.illusionInvert),
            UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_TRANSFORM),
            this.illusionTransformEditor,
            UI.row(this.illusionGradual, this.illusionGradualInvert),
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DISTORT), this.illusionDistort),
            UI.row(this.illusionDistortUniform, this.illusionDistortInvert),
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_DELAY), this.illusionDelay),
            UI.row(UI.label(UIKeys.FORMS_EDITORS_GENERAL_ILLUSION_GLOW), this.illusionGlow),
            UI.row(this.illusionGlowUniform, this.illusionGlowInvert),
            UI.row(this.illusionTextures, this.illusionTexturesClear, this.illusionRandomTextures),
            this.illusionReal
        );
        illusionContent.context((menu) -> menu.action(Icons.CLOSE, UIKeys.TRANSFORMS_CONTEXT_RESET, this::resetIllusion));

        this.illusionSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GENERAL_ILLUSION,
            UIReplaysEditor.getColor("illusion"),
            illusionContent,
            () -> this.illusionTransformEditor.resize()
        );

        this.options.add(this.name);
        this.options.add(this.hotkey, this.trackName, this.animatable);
        this.options.add(this.colorSection, this.glowSection);
        this.options.add(this.visible);
        this.options.add(UI.label(UIKeys.FILM_REPLAY_TRACK_OPACITY).marginTop(4), this.opacity);
        this.options.add(this.noshadingOpacity, this.shaderShadow);
        this.options.add(this.transform.marginTop(8));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_UI_SCALE), this.uiScale);
        this.options.add(this.renderDepthSection);
        this.options.add(this.lookAtSection);
        this.options.add(this.illusionSection);
        this.options.add(this.inverseKinematicsSection);
        this.options.add(this.hitboxSection);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HP).marginTop(12), this.hp);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_MOVEMENT_SPEED), this.speed.tooltip(UIKeys.FORMS_EDITORS_GENERAL_MOVEMENT_SPEED_TOOLTIP));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_STEP_HEIGHT), this.stepHeight);
    }

    private ValueColor getFormColorValue()
    {
        if (this.form == null)
        {
            return null;
        }

        BaseValue value = this.form.get("color");

        return value instanceof ValueColor ? (ValueColor) value : null;
    }

    private void setupColorAndGlow()
    {
        this.color = new UIColor((c) ->
        {
            ValueColor valueColor = this.getFormColorValue();

            if (valueColor == null)
            {
                return;
            }

            Color color = valueColor.get().copy();
            Color value = Color.rgb(c);
            float intensity = color.a;

            color.set(value.r, value.g, value.b, intensity);
            valueColor.set(color);
        });
        this.color.direction(Direction.LEFT);
        this.color.context((menu) -> menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, this::resetMainColor));
        this.blendIntensity = new UITrackpad((value) ->
        {
            ValueColor valueColor = this.getFormColorValue();

            if (valueColor == null)
            {
                return;
            }

            Color color = valueColor.get().copy();

            color.a = MathUtils.clamp(value.floatValue(), 0F, 1F);
            valueColor.set(color);
        });
        this.blendIntensity.limit(0F, 1F).values(0.1D, 0.05D, 0.2D);
        this.blendIntensity.tooltip(UIKeys.FORMS_EDITORS_BLEND_INTENSITY);
        this.colorAdjustments = new UIFormColorAdjustments(() ->
        {
            ValueColor valueColor = this.getFormColorValue();

            return valueColor == null ? Color.white() : valueColor.get();
        }, (color) ->
        {
            ValueColor valueColor = this.getFormColorValue();

            if (valueColor == null)
            {
                return;
            }

            valueColor.setRuntimeValue(null);
            valueColor.set(color);
            this.blendIntensity.setValue(MathUtils.clamp(color.a, 0F, 1F));
        });
        this.colorTransform = new UIEffectTransformCollapse((apply) ->
        {
            ValueColor valueColor = this.getFormColorValue();

            if (valueColor == null)
            {
                return;
            }

            Color copy = valueColor.get().copy();

            if (copy.transform == null)
            {
                copy.transform = new EffectTransform();
            }

            apply.accept(copy.transform);
            valueColor.setRuntimeValue(null);
            valueColor.set(copy);
        });
        this.paintColor = new UIColor((c) ->
        {
            Color color = new Color().set(c);
            PaintSettings settings = this.form.paintSettings.get().copy();

            color.a = settings.intensity;
            this.form.paintColor.set(color);

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            settings.applyAutoShaderShadow();
            this.form.paintSettings.set(settings);
        });
        this.paintColor.direction(Direction.LEFT);
        this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);
        this.paintColor.context((menu) -> menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, this::resetPaintColor));
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
        this.paintTransform = new UIEffectTransformCollapse((apply) ->
        {
            PaintSettings settings = this.form.paintSettings.get().copy();

            if (settings.transform == null)
            {
                settings.transform = new EffectTransform();
            }

            apply.accept(settings.transform);
            this.form.paintSettings.set(settings);
        });
        this.glowingColor = new UIColor((c) ->
        {
            Color color = new Color().set(c);

            color.a = 1F;
            this.form.glowingColor.set(color);

            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            this.form.glowSettings.set(settings);
        });
        this.glowingColor.direction(Direction.LEFT);
        this.glowingColor.tooltip(UIKeys.FORMS_EDITORS_GLOW);
        this.glowingColor.context((menu) -> menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, this::resetGlowColor));
        this.glowIntensity = new UITrackpad((value) ->
        {
            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.intensity = value.floatValue();
            this.form.glowSettings.set(settings);
        });
        this.glowIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.glowIntensity.tooltip(UIKeys.FORMS_EDITORS_GLOW_INTENSITY);
        this.opacity = new UITrackpad((value) ->
        {
            this.form.opacity.set(MathUtils.clamp(value.intValue(), 0, 255) / 255F);
        });
        this.opacity.integer().limit(0, 255).plainFormat().values(1D, 1D, 16D);
        this.opacity.tooltip(UIKeys.FILM_REPLAY_TRACK_OPACITY);
        this.colorSection = new UIPoseSectionCollapse(
            UIKeys.FILM_REPLAY_TRACK_COLOR,
            UIReplaysEditor.getColor("color"),
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_BLEND_COLOR).marginTop(4),
                this.color,
                UI.label(UIKeys.FORMS_EDITORS_BLEND_INTENSITY),
                this.blendIntensity,
                this.colorTransform,
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
                this.lighting.marginTop(4),
                UI.label(UIKeys.FORMS_EDITORS_GLOWING_COLOR).marginTop(4),
                this.glowingColor,
                UI.label(UIKeys.FORMS_EDITORS_GLOW_INTENSITY),
                this.glowIntensity
            )
        );
    }

    private void resetMainColor()
    {
        ValueColor valueColor = this.getFormColorValue();

        if (valueColor == null)
        {
            return;
        }

        Color white = Color.white();

        valueColor.set(white.copy());
        this.color.setColor(white.getRGBColor());
        this.blendIntensity.setValue(MathUtils.clamp(white.a, 0F, 1F));
        this.colorTransform.setEffectTransform(new EffectTransform());
        this.colorAdjustments.syncFromForm();
        this.editor.startEdit(this.form);
    }

    private void syncColorSectionVisibility()
    {
        ValueColor valueColor = this.getFormColorValue();

        if (valueColor != null)
        {
            if (!this.colorSection.hasParent())
            {
                this.options.addAfter(this.trackName, this.colorSection);
            }
        }
        else
        {
            this.colorSection.removeFromParent();
        }
    }

    private void resetPaintColor()
    {
        if (this.form == null)
        {
            return;
        }

        Color legacy = new Color().set(1F, 1F, 1F, 0F);
        PaintSettings settings = new PaintSettings();

        this.form.paintColor.set(legacy.copy());
        this.form.paintSettings.set(settings);
        this.paintColor.setColor(legacy.getRGBColor());
        this.paintIntensity.setValue(settings.intensity);
        this.paintTransform.setEffectTransform(new EffectTransform());
        this.editor.startEdit(this.form);
    }

    private void resetGlowColor()
    {
        if (this.form == null)
        {
            return;
        }

        Color legacy = new Color().set(1F, 1F, 1F, 1F);
        GlowSettings settings = new GlowSettings();

        this.form.glowingColor.set(legacy.copy());
        this.form.glowSettings.set(settings);
        this.glowingColor.setColor(legacy.getRGBColor());
        this.glowIntensity.setValue(settings.intensity);
        this.editor.startEdit(this.form);
    }

    private void refreshLookAt()
    {
        if (this.form == null)
        {
            return;
        }

        this.lookAt.fillBones(FormUtilsClient.getRenderer(FormUtils.getRoot(this.form)).collectMatrices(this.editor.editor.renderer.getTargetEntity(), 0F).keySet());
        this.lookAt.refresh();
        this.lookAt.resize();
    }

    private void refreshInverseKinematics()
    {
        if (this.form == null)
        {
            return;
        }

        this.inverseKinematics.fillBones(FormUtilsClient.getRenderer(FormUtils.getRoot(this.form)).collectMatrices(this.editor.editor.renderer.getTargetEntity(), 0F).keySet());
        this.inverseKinematics.refresh();
        this.inverseKinematics.resize();
    }

    private void editIllusion(Consumer<Illusion> consumer)
    {
        Illusion illusion = this.form.illusion.get().copy();

        consumer.accept(illusion);
        this.form.illusion.set(illusion);
    }

    private void resetIllusion()
    {
        if (this.form == null)
        {
            return;
        }

        this.form.illusion.set(new Illusion());
        this.startEdit(this.form);
        this.editor.startEdit(this.form);
    }

    private void toggleIllusionDirection(int bit, boolean enabled)
    {
        this.editIllusion((illusion) -> illusion.directions = enabled ? illusion.directions | bit : illusion.directions & ~bit);
    }

    private void editLookAt(Consumer<LookAt> consumer)
    {
        LookAt lookAt = this.form.lookAt.get().copy();

        consumer.accept(lookAt);
        this.form.lookAt.set(lookAt);
    }

    private void editInverseKinematics(Consumer<InverseKinematics> consumer)
    {
        InverseKinematics ik = this.form.inverseKinematics.get().copy();

        consumer.accept(ik);
        this.form.inverseKinematics.set(ik);
    }

    @Override
    public void startEdit(Form form)
    {
        super.startEdit(form);

        int hotkey = form.hotkey.get();

        if (hotkey == 0)
        {
            KeyCombo empty = new KeyCombo(IKey.EMPTY);
            empty.keys.clear();
            this.hotkey.setKeyCombo(empty);
        }
        else
        {
            this.hotkey.setKeyCombo(new KeyCombo(IKey.EMPTY, hotkey));
        }

        this.visible.setValue(form.visible.get());
        this.animatable.setValue(form.animatable.get());
        this.trackName.setText(form.trackName.get());
        this.lighting.setValue(form.lighting.get() > 0F);
        this.noshadingOpacity.setValue(form.noshadingOpacity.get());
        this.shaderShadow.setValue(form.shaderShadow.get());
        this.renderDepth.setValue(form.renderDepth.get());
        this.renderDepthEnabled.setValue(form.renderDepthEnabled.get());
        this.syncColorSectionVisibility();

        ValueColor valueColor = this.getFormColorValue();

        if (valueColor != null)
        {
            Color formColor = valueColor.get();

            this.color.setColor(formColor.getRGBColor());
            this.blendIntensity.setValue(MathUtils.clamp(formColor.a, 0F, 1F));
            this.colorAdjustments.syncFromForm();
            this.colorTransform.setEffectTransform(formColor.transform == null ? new EffectTransform() : formColor.transform);
        }

        PaintSettings paint = form.paintSettings.get();
        Color paintDisplay = new Color();

        paint.resolveColor(form.paintColor.get(), paintDisplay);
        this.paintColor.setColor(paintDisplay.getRGBColor());
        this.paintIntensity.setValue(paint.intensity);
        this.paintTransform.setEffectTransform(paint.transform == null ? new EffectTransform() : paint.transform);

        GlowSettings glow = form.glowSettings.get();
        Color glowDisplay = new Color();

        glow.resolveColor(form.glowingColor.get(), glowDisplay);
        this.glowingColor.setColor(glowDisplay.getRGBColor());
        this.glowIntensity.setValue(glow.intensity);
        this.opacity.setValue(Math.round(MathUtils.clamp(form.opacity.get(), 0F, 1F) * 255F));

        this.lookAt.fillBones(FormUtilsClient.getRenderer(FormUtils.getRoot(form)).collectMatrices(this.editor.editor.renderer.getTargetEntity(), 0F).keySet());
        this.lookAt.refresh();
        this.inverseKinematics.fillBones(FormUtilsClient.getRenderer(FormUtils.getRoot(form)).collectMatrices(this.editor.editor.renderer.getTargetEntity(), 0F).keySet());
        this.inverseKinematics.refresh();
        this.options.resize();

        Illusion illusion = form.illusion.get();

        this.illusionCount.setValue(illusion.count);
        this.illusionSpread.setValue(illusion.spread);
        this.illusionFront.setValue((illusion.directions & Illusion.FRONT) != 0);
        this.illusionBack.setValue((illusion.directions & Illusion.BACK) != 0);
        this.illusionLeft.setValue((illusion.directions & Illusion.LEFT) != 0);
        this.illusionRight.setValue((illusion.directions & Illusion.RIGHT) != 0);
        this.illusionUp.setValue((illusion.directions & Illusion.UP) != 0);
        this.illusionDown.setValue((illusion.directions & Illusion.DOWN) != 0);
        this.illusionUniform.setValue(illusion.uniform);
        this.illusionSpacing.setValue(illusion.spacing);
        this.illusionOffset.setValue(illusion.offset);
        this.illusionOpacity.setValue(illusion.opacity * 100F);
        this.illusionOpacityUniform.setValue(illusion.opacityUniform);
        this.illusionInvert.setValue(illusion.invert);
        this.illusionRandomTextures.setValue(illusion.randomTextures);
        this.illusionReal.setValue(illusion.real);
        this.illusionDelay.setValue(illusion.delay);
        this.illusionDistort.setValue(illusion.distort);
        this.illusionDistortUniform.setValue(illusion.distortUniform);
        this.illusionDistortInvert.setValue(illusion.distortInvert);
        this.illusionGlow.setValue(illusion.glow);
        this.illusionGlowUniform.setValue(illusion.glowUniform);
        this.illusionGlowInvert.setValue(illusion.glowInvert);
        this.illusionGradual.setValue(illusion.gradual);
        this.illusionGradualInvert.setValue(illusion.gradualInvert);
        this.illusionTransformEditor.setTransform(illusion.transform);
        this.uiScale.setValue(form.uiScale.get());
        this.name.setText(form.name.get());
        this.transform.setTransform(form.transform.get());

        this.hitbox.setValue(form.hitbox.get());
        this.hitboxWidth.setValue(form.hitboxWidth.get());
        this.hitboxHeight.setValue(form.hitboxHeight.get());
        this.hitboxSneakMultiplier.setValue(form.hitboxSneakMultiplier.get());
        this.hitboxEyeHeight.setValue(form.hitboxEyeHeight.get());

        this.hp.setValue(form.hp.get());
        this.speed.setValue(form.speed.get());
        this.stepHeight.setValue(form.stepHeight.get());
    }
}
