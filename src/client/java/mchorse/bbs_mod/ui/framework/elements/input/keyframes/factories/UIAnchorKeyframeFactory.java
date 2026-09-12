package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.netty.util.collection.IntObjectMap;

public class UIAnchorKeyframeFactory extends UIKeyframeFactory<Anchor>
{
    private UIButton actor;
    private UIButton attachment;
    private UIToggle translate;
    private UIToggle scale;
    public UIPropTransform transform;

    public static void displayActors(UIContext context, IntObjectMap<IEntity> entities, int value, Consumer<Integer> callback)
    {
        List<UIFilmPanel> children = context.menu.main.getChildren(UIFilmPanel.class);
        UIFilmPanel panel = children.isEmpty() ? null : children.get(0);
        List<Replay> replays = panel != null ? panel.getData().replays.getList() : null;

        context.replaceContextMenu((menu) ->
        {
            menu.action(Icons.CLOSE, UIKeys.GENERAL_NONE, Colors.NEGATIVE, () -> callback.accept(-1));

            for (int i = 0; i < entities.size(); i++)
            {
                final int actor = i;
                IEntity entity = entities.get(i);

                if (entity == null)
                {
                    continue;
                }

                Replay replay = replays == null ? null : replays.get(i);
                Form form = entity.getForm();
                String stringLabel = i + (replay != null ? " - " + replay.getName() : (form == null ? "" : " - " + form.getFormIdOrName()));
                IKey label = IKey.constant(stringLabel);

                menu.action(Icons.CLOSE, label, actor == value, () -> callback.accept(actor));
            }
        });
    }

    public static void displayAttachments(UIFilmPanel panel, int index, String value, Consumer<String> consumer)
    {
        if (panel == null)
        {
            return;
        }

        displayAttachments(panel.getContext(), panel, index, value, consumer);
    }

    public static void displayAttachments(UIContext context, UIFilmPanel panel, int index, String value, Consumer<String> consumer)
    {
        if (context == null || panel == null)
        {
            return;
        }

        IEntity entity = panel.getController().getEntities().get(index);

        if (entity == null || entity.getForm() == null)
        {
            return;
        }

        Form form = entity.getForm();
        List<String> attachments = new ArrayList<>(FormUtilsClient.getRenderer(form).collectMatrices(entity, 0F).keySet());

        attachments.sort(String::compareToIgnoreCase);

        List<String> labels = new ArrayList<>(attachments);

        for (int i = 0; i < labels.size(); i++)
        {
            String label = labels.get(i);
            Form path = FormUtils.getForm(form, label);

            if (path != null)
            {
                labels.set(i, path.getTrackName(label));
            }
        }

        if (attachments.isEmpty())
        {
            return;
        }

        context.replaceContextMenu((menu) ->
        {
            for (int i = 0; i < attachments.size(); i++)
            {
                String attachment = attachments.get(i);
                String label = labels.get(i);

                menu.action(Icons.LIMB, IKey.constant(label), attachment.equals(value), () -> consumer.accept(attachment));
            }
        });
    }

    public UIAnchorKeyframeFactory(Keyframe<Anchor> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.actor = new UIButton(UIKeys.GENERIC_KEYFRAMES_ANCHOR_PICK_ACTOR, (b) -> this.displayActors());
        this.attachment = new UIButton(UIKeys.GENERIC_KEYFRAMES_ANCHOR_PICK_ATTACHMENT, (b) ->
        {
            displayAttachments(this.getPanel(), this.keyframe.getValue().replay, this.keyframe.getValue().attachment, this::setAttachment);
        });
        this.translate = new UIToggle(UIKeys.TRANSFORMS_TRANSLATE, (b) -> this.setTranslate(b.getValue()));
        this.translate.setValue(keyframe.getValue().translate);
        this.scale = new UIToggle(UIKeys.TRANSFORMS_SCALE, (b) -> this.setScale(b.getValue()));
        this.scale.setValue(keyframe.getValue().scale);
        this.transform = new UIAnchorTransforms(this);
        this.transform.enableHotkeys();
        this.transform.setTransform(keyframe.getValue().transform);
        this.transform.setLocalMode(true);
        this.transform.translationScale(1F);
        this.transform.anchorGizmoTuning();

        this.scroll.add(this.actor, this.attachment, this.translate, this.scale, this.transform);
    }

    private void displayActors()
    {
        UIFilmPanel panel = this.getPanel();

        displayActors(this.getContext(), panel.getController().getEntities(), this.keyframe.getValue().replay, this::setActor);
    }

    private void setActor(int actor)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().replay = actor);
    }

    private void setAttachment(String attachment)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().attachment = attachment);
    }

    private void setTranslate(boolean translate)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().translate = translate);
    }

    private void setScale(boolean scale)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().scale = scale);
    }

    private UIFilmPanel getPanel()
    {
        return this.getParent(UIFilmPanel.class);
    }

    public static class UIAnchorTransforms extends UIKeyframePropTransform
    {
        private final UIAnchorKeyframeFactory editor;

        public UIAnchorTransforms(UIAnchorKeyframeFactory editor)
        {
            this.editor = editor;
        }

        /** Re-syncs the cached editing transform with the currently active keyframe's
         *  transform, exactly like the film Transform track's {@code getReferenceTransform()},
         *  so ring/gizmo deltas are always computed against live data. */
        private Transform getReferenceTransform()
        {
            Anchor anchor = this.editor.keyframe.getValue();
            Transform active = anchor == null ? null : anchor.transform;

            if (active != null && active != this.getTransform())
            {
                this.setTransform(active);
            }

            return this.getTransform();
        }

        @Override
        protected void applyToSelection(Consumer<Transform> consumer)
        {
            UIAnchorTransforms.apply(this.editor.editor, this.editor.keyframe, consumer);
        }

        public static void apply(UIKeyframes editor, Keyframe<?> keyframe, Consumer<Transform> consumer)
        {
            UIReplaysEditorUtils.forEachSelectedKeyframe(editor, keyframe, (selected) ->
            {
                Anchor anchor = (Anchor) selected.getValue();

                selected.preNotify();
                consumer.accept(anchor.transform);
                selected.postNotify();
            });
        }

        @Override
        public void pasteTranslation(Vector3d translation)
        {
            apply(this.editor.editor, this.editor.keyframe, (t) -> t.translate.set(translation));
            this.refillTransform();
        }

        @Override
        public void pasteScale(Vector3d scale)
        {
            apply(this.editor.editor, this.editor.keyframe, (t) -> t.scale.set(scale));
            this.refillTransform();
        }

        @Override
        public void pasteRotation(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (t) -> t.rotate.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void pasteRotation2(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (t) -> t.rotate2.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void pastePivot(Vector3d pivot)
        {
            apply(this.editor.editor, this.editor.keyframe, (t) -> t.pivot.set((float) pivot.x, (float) pivot.y, (float) pivot.z));
            this.refillTransform();
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (t) ->
            {
                t.translate.x += dx;
                t.translate.y += dy;
                t.translate.z += dz;
            });
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (t) ->
            {
                t.scale.x += dx;
                t.scale.y += dy;
                t.scale.z += dz;
            });
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (t) ->
            {
                t.rotate.x += dx;
                t.rotate.y += dy;
                t.rotate.z += dz;
            });
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (t) ->
            {
                t.rotate2.x += dx;
                t.rotate2.y += dy;
                t.rotate2.z += dz;
            });
        }

        @Override
        public void setP(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = (float) x - transform.pivot.x;
            float dy = (float) y - transform.pivot.y;
            float dz = (float) z - transform.pivot.z;

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (t) ->
            {
                t.pivot.x += dx;
                t.pivot.y += dy;
                t.pivot.z += dz;
            });
        }
    }
}
