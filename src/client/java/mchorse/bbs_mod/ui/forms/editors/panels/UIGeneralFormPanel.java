package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.forms.forms.utils.InverseKinematics;
import mchorse.bbs_mod.forms.forms.utils.LookAt;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
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
    public UIToggle shaderShadow;
    public UITrackpad renderDepth;
    public UIToggle renderDepthEnabled;
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

    public UIToggle hitbox;
    public UITrackpad hitboxWidth;
    public UITrackpad hitboxHeight;
    public UITrackpad hitboxSneakMultiplier;
    public UITrackpad hitboxEyeHeight;

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
        this.hotkey.single().tooltip(UIKeys.FORMS_EDITORS_GENERAL_HOTKEY);

        this.visible = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_VISIBLE, (b) -> this.form.visible.set(b.getValue()));
        this.animatable = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ANIMATABLE, (b) -> this.form.animatable.set(b.getValue()));
        this.animatable.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ANIMATABLE_TOOLTIP);
        this.trackName = new UITextbox(120, (t) -> this.form.trackName.set(t));
        this.trackName.tooltip(UIKeys.FORMS_EDITORS_GENERAL_TRACK_NAME_TOOLTIP);
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) -> this.form.lighting.set(b.getValue() ? 1F : 0F));
        this.lighting.tooltip(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING_TOOLTIP);
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

        this.transform = new UIPropTransform().callbacks(() -> this.form.transform);
        this.transform.enableHotkeys().relative(this).x(0.5F).y(1F, -10).anchor(0.5F, 1F);

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

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_DISPLAY), this.name);
        this.options.add(this.hotkey, this.visible, this.animatable, this.trackName, this.lighting, this.shaderShadow);
        this.options.add(this.renderDepthEnabled, this.renderDepth);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_UI_SCALE), this.uiScale);
        this.options.add(this.transform.marginTop(8));
        this.options.add(this.lookAtSection);
        this.options.add(this.inverseKinematicsSection);
        this.options.add(this.illusionSection);
        this.options.add(this.hitbox.marginTop(12), UI.row(this.hitboxWidth, this.hitboxHeight));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_SNEAK_MULTIPLIER), this.hitboxSneakMultiplier);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_EYE_HEIGHT), this.hitboxEyeHeight);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HP).marginTop(12), this.hp);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_MOVEMENT_SPEED), this.speed.tooltip(UIKeys.FORMS_EDITORS_GENERAL_MOVEMENT_SPEED_TOOLTIP));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_STEP_HEIGHT), this.stepHeight);
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

        this.hotkey.setKeyCombo(new KeyCombo(IKey.EMPTY, form.hotkey.get()));

        this.visible.setValue(form.visible.get());
        this.animatable.setValue(form.animatable.get());
        this.trackName.setText(form.trackName.get());
        this.lighting.setValue(form.lighting.get() > 0F);
        this.shaderShadow.setValue(form.shaderShadow.get());
        this.renderDepth.setValue(form.renderDepth.get());
        this.renderDepthEnabled.setValue(form.renderDepthEnabled.get());
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
