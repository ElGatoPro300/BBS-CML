package mchorse.bbs_mod.ui.forms.editors.states.keyframes;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.film.replays.overlays.UIAnimationToPoseOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormPropertyTrackSheets;
import mchorse.bbs_mod.ui.forms.editors.utils.UIPickableFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoController;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoMatrixUtils;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoRayFrame;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoSurface;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Matrix4f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIAnimationStateEditor extends UIElement implements GizmoSurface
{
    public UIKeyframeEditor keyframeEditor;

    private final GizmoController gizmoController = new GizmoController(this);
    private StencilFormFramebuffer gizmoStencil;

    public UIFormEditor editor;
    public UIElement editArea;

    private AnimationState state;
    private Set<String> keys = new LinkedHashSet<>();
    private final Map<String, Boolean> collapsedModelTracks = new HashMap<>();

    public UIAnimationStateEditor(UIFormEditor editor)
    {
        this.editor = editor;

        this.editArea = new UIElement();
        this.editArea.relative(this)
            .x(BBSSettings.editorLayoutSettings.getStateEditorSizeH())
            .wTo(this.area, 1F)
            .h(1F);

        UIDraggable draggable = new UIDraggable((context) ->
        {
            float fx = (context.mouseX - this.area.x) / (float) this.area.w;
            float fy = -(context.mouseY - this.getParent().area.ey()) / (float) this.getParent().area.h;

            BBSSettings.editorLayoutSettings.setStateEditorSizeV(fy);
            BBSSettings.editorLayoutSettings.setStateEditorSizeH(fx);

            this.h(BBSSettings.editorLayoutSettings.getStateEditorSizeV());
            this.editArea.x(BBSSettings.editorLayoutSettings.getStateEditorSizeH());
            this.getParent().resize();
        });

        draggable.reference(() -> new Vector2i(this.editArea.area.x, this.area.y));
        draggable.rendering((context) ->
        {
            int size = 5;
            int x = this.editArea.area.x + 3;
            int y = this.editArea.area.y + 3;

            context.batcher.box(x, y, x + 1, y + size, Colors.WHITE);
            context.batcher.box(x, y - 1, x + size, y, Colors.WHITE);

            x = this.editArea.area.x - 3;
            y = this.editArea.area.y + 3;

            context.batcher.box(x - 1, y, x, y + size, Colors.WHITE);
            context.batcher.box(x - size, y - 1, x, y, Colors.WHITE);
        });

        draggable.hoverOnly().relative(this.editArea).w(40).h(6).anchorX(0.5F);

        this.add(this.editArea, draggable);
    }

    public AnimationState getState()
    {
        return this.state;
    }

    public void setState(AnimationState state)
    {
        UIKeyframes lastEditor = null;

        if (this.keyframeEditor != null)
        {
            lastEditor = this.keyframeEditor.view;

            this.keyframeEditor.removeFromParent();
            this.keyframeEditor = null;
        }

        this.state = state;

        if (this.state == null)
        {
            return;
        }

        List<UIKeyframeSheet> sheets = UIFormPropertyTrackSheets.buildAnimationStateSheets(
            this.editor.form,
            this.state.properties,
            this.collapsedModelTracks,
            () -> this.setState(this.state),
            "animation_state"
        );

        this.keys.clear();

        for (UIKeyframeSheet sheet : sheets)
        {
            if (!sheet.groupHeader)
            {
                this.keys.add(StringUtils.fileName(sheet.id));
            }
        }

        if (!sheets.isEmpty())
        {
            this.keyframeEditor = new UIKeyframeEditor((consumer) -> new UIAnimationStateKeyframes(this.editor, consumer)).target(this.editArea);
            this.keyframeEditor.relative(this).h(1F).wTo(this.editArea.area);
            this.keyframeEditor.setUndoId("form_animation_state_keyframe_editor");

            /* Reset */
            if (lastEditor != null)
            {
                this.keyframeEditor.view.copyViewport(lastEditor);
            }

            this.keyframeEditor.view.duration(() -> this.state.duration.get());
            this.keyframeEditor.view.context((menu) ->
            {
                int mouseY = this.getContext().mouseY;
                UIKeyframeSheet sheet = this.keyframeEditor.view.getGraph().getSheet(mouseY);

                if (sheet != null && sheet.channel.getFactory() == KeyframeFactories.POSE)
                {
                    String trackName = StringUtils.fileName(sheet.id);

                    if (trackName.equals("pose") || trackName.startsWith("pose_overlay"))
                    {
                        Form form = sheet.property != null ? FormUtils.getForm(sheet.property) : this.editor.form;

                        if (form instanceof ModelForm modelForm)
                        {
                            menu.action(Icons.POSE, UIKeys.FILM_REPLAY_CONTEXT_ANIMATION_TO_KEYFRAMES, () ->
                            {
                                ModelInstance model = ModelFormRenderer.getModel(modelForm);

                                if (model != null)
                                {
                                    UIOverlay.addOverlay(this.getContext(), new UIAnimationToPoseOverlayPanel((animationKey, onlyKeyframes, length, step) ->
                                    {
                                        int current = this.editor.getCursor();
                                        IEntity entity = this.editor.renderer.getTargetEntity();

                                        UIReplaysEditorUtils.animationToPoseKeyframes(this.keyframeEditor, sheet, modelForm, entity, current, animationKey, onlyKeyframes, length, step);
                                    }, modelForm, sheet), 260, 260);
                                }
                            });
                        }
                        menu.action(Icons.CONVERT, UIKeys.FILM_REPLAY_CONTEXT_POSE_TO_LIMBS, () -> this.convertToLimbs(sheet));
                    }
                    else if (sheet.id.indexOf(':') != -1)
                    {
                        menu.action(Icons.REMOVE, UIKeys.KEYFRAMES_CONTEXT_REMOVE, () ->
                        {
                            this.keyframeEditor.view.getGraph().removeKeyframe(this.keyframeEditor.view.getGraph().getSelected());
                            this.setState(this.state);
                        });
                    }
                }

                if (this.keyframeEditor.view.getGraph() instanceof UIKeyframeDopeSheet && (sheet == null || !sheet.groupHeader))
                {
                    menu.action(Icons.FILTER, UIKeys.FILM_REPLAY_FILTER_SHEETS, () ->
                    {
                        UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(BBSSettings.disabledSheets.get(), this.keys);

                        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);

                        panel.onClose((e) ->
                        {
                            this.setState(this.state);
                            BBSSettings.disabledSheets.set(BBSSettings.disabledSheets.get());
                        });
                    });
                }
            });

            for (UIKeyframeSheet sheet : sheets)
            {
                this.keyframeEditor.view.addSheet(sheet);
            }

            this.addAfter(this.editArea, this.keyframeEditor);
        }

        this.resize();

        if (this.keyframeEditor != null && lastEditor == null)
        {
            this.keyframeEditor.view.resetView();
        }
    }

    public boolean clickViewport(UIContext context, StencilFormFramebuffer stencil)
    {
        this.gizmoStencil = stencil;

        UIPropTransform editableTransform = UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor);

        if (context.mouseButton == 0 && this.gizmoController.tryStartHandleDrag(context, editableTransform))
        {
            return true;
        }

        if (stencil.hasPicked() && this.state != null)
        {
            Pair<Form, String> pair = stencil.getPicked();

            if (pair != null && context.mouseButton < 2)
            {
                if (pair.a == null)
                {
                    return false;
                }

                if (context.mouseButton == 0)
                {
                    if (Window.isShiftPressed()) UIReplaysEditorUtils.offerHierarchy(this.getContext(), pair.a, pair.b, (bone) -> this.pickForm(pair.a, bone));
                    else this.pickForm(pair.a, pair.b);

                    return true;
                }
                else if (context.mouseButton == 1)
                {
                    this.pickFormProperty(pair.a, pair.b);

                    return true;
                }
            }
        }

        return false;
    }

    public void finishGizmoPendingClick()
    {
        Pair<Form, String> pendingPick = this.gizmoController.consumePendingTrackballClick();

        if (pendingPick != null && pendingPick.a != null)
        {
            if (Window.isShiftPressed())
            {
                UIReplaysEditorUtils.offerHierarchy(this.getContext(), pendingPick.a, pendingPick.b, (bone) -> this.pickForm(pendingPick.a, bone));
            }
            else
            {
                this.pickForm(pendingPick.a, pendingPick.b);
            }
        }

        this.gizmoController.stop();
    }

    @Override
    public StencilFormFramebuffer getGizmoStencil()
    {
        return this.gizmoStencil;
    }

    @Override
    public void prepareGizmoDrag(UIPropTransform transform)
    {
        if (transform == null || this.editor == null || this.editor.renderer == null)
        {
            return;
        }

        UIPickableFormRenderer renderer = this.editor.renderer;
        UIAnimationStateEditor self = this;

        transform.setGizmoRayProvider(GizmoRayFrame.fromCamera(
            renderer.camera,
            renderer.area,
            () ->
            {
                UIContext context = self.getContext();

                if (context == null)
                {
                    return null;
                }

                Matrix4f origin = self.getOrigin(context.getTransition());

                if (origin == null || origin == Matrices.EMPTY_4F)
                {
                    return null;
                }

                return origin;
            }
        ));
    }

    public void pickForm(Form form, String bone)
    {
        UIReplaysEditorUtils.pickForm(this.keyframeEditor, this.editor, form, bone);
    }

    public void pickFormProperty(Form form, String bone)
    {
        UIReplaysEditorUtils.pickFormProperty(this.getContext(), this.keyframeEditor, this.editor, form, bone);
    }

    public Matrix4f getOrigin(float transition)
    {
        if (this.keyframeEditor == null)
        {
            return Matrices.EMPTY_4F;
        }

        Pair<String, TransformOrientation> bone = this.keyframeEditor.getBone();

        if (bone == null)
        {
            return Matrices.EMPTY_4F;
        }

        Form root = FormUtils.getRoot(this.editor.form);
        MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(this.editor.renderer.getTargetEntity(), transition);
        
        String key = bone.a;
        boolean forceOrigin = key.endsWith("#origin");
        
        if (forceOrigin) key = key.substring(0, key.length() - 7);
        
        MatrixCacheEntry entry = map.get(key);
        
        if (entry == null)
        {
            return Matrices.EMPTY_4F;
        }

        if (forceOrigin)
        {
            Matrix4f matrix = entry.origin();

            return matrix == null ? Matrices.EMPTY_4F : matrix;
        }

        boolean bobj = root instanceof ModelForm modelForm && ModelFormRenderer.isBobjModel(modelForm);
        Matrix4f matrix = GizmoMatrixUtils.resolveFilmPoseBoneMatrix(entry, bone.b, bobj);

        return matrix == null ? Matrices.EMPTY_4F : matrix;
    }

    private void convertToLimbs(UIKeyframeSheet sheet)
    {
        List<Keyframe> selected = new ArrayList<>(sheet.selection.getSelected());

        if (selected.isEmpty())
        {
            return;
        }

        Form rootForm = FormUtils.getRoot(this.editor.form);

        Set<String> boneNames = new HashSet<>();

        for (Keyframe kf : selected)
        {
            Pose pose = (Pose) kf.getValue();

            if (pose != null)
            {
                boneNames.addAll(pose.transforms.keySet());
            }
        }

        BaseValue.edit(this.state, IValueListener.FLAG_UNMERGEABLE, (s) ->
        {
            Set<Float> convertedTicks = new HashSet<>();

            for (Keyframe kf : selected)
            {
                Pose pose = (Pose) sheet.channel.interpolate(kf.getTick());

                if (pose == null)
                {
                    continue;
                }

                convertedTicks.add(kf.getTick());

                for (String boneName : boneNames)
                {
                    PoseTransform transform = pose.transforms.get(boneName);

                    if (transform == null)
                    {
                        transform = new PoseTransform();
                    }

                    String key = sheet.id + ":" + boneName;

                    KeyframeChannel<Transform> channel = this.state.properties.getOrCreate(rootForm, key);

                    if (channel != null)
                    {
                        int index = channel.insert(kf.getTick(), transform.copy());
                        Keyframe<Transform> newKf = channel.get(index);

                        newKf.copyOverExtra(kf);
                    }
                }
            }

            if (!convertedTicks.isEmpty())
            {
                for (int i = sheet.channel.getList().size() - 1; i >= 0; i--)
                {
                    Keyframe existing = (Keyframe) sheet.channel.getList().get(i);

                    for (Float tick : convertedTicks)
                    {
                        if (Math.abs(existing.getTick() - tick) < 0.0001F)
                        {
                            sheet.channel.remove(i);
                            break;
                        }
                    }
                }
            }

            this.state.properties.cleanUp();
        });

        this.setState(this.state);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.keyframeEditor != null)
        {
            this.editArea.area.render(context.batcher, Colors.A75);
        }

        super.render(context);
    }
}
