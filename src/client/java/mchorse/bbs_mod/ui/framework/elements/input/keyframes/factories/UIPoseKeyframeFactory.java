package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Vector3d;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UIPoseKeyframeFactory extends UIKeyframeFactory<Pose>
{
    public UIPoseFactoryEditor poseEditor;

    /**
     * -1 = not built yet; 0 = narrow stack; 1 = wide two-column.
     * Rebuild only when this flips — every resize used to wipe the tree and
     * orphan open Color/Glow shells so footer buttons vanished.
     */
    private int layoutMode = -1;

    public UIPoseKeyframeFactory(Keyframe<Pose> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.poseEditor = new UIPoseFactoryEditor(editor, keyframe);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

        if (FormUtils.getForm(sheet.property) instanceof ModelForm modelForm)
        {
            ModelInstance model = ((ModelFormRenderer) FormUtilsClient.getRenderer(modelForm)).getModel();

            /* Hacer que el selector de textura del hueso abra la carpeta
             * de la textura base del modelo cuando no haya override */
            this.poseEditor.setDefaultTextureSupplier(() ->
            {
                Link base = modelForm.texture.get();
                if (base != null)
                {
                    return base;
                }

                ModelInstance m = ((ModelFormRenderer) FormUtilsClient.getRenderer(modelForm)).getModel();
                return m != null ? m.texture : null;
            });

            if (model != null)
            {
                this.poseEditor.setPose(keyframe.getValue(), model.poseGroup);
                this.poseEditor.fillGroups(model.model, model.flippedParts, false);

                this.poseEditor.refreshCurrentBone();
            }
        }
        else if (FormUtils.getForm(sheet.property) instanceof MobForm mobForm)
        {
            List<String> bones = FormUtilsClient.getRenderer(mobForm).getBones();

            this.poseEditor.setPose(keyframe.getValue(), "");
            this.poseEditor.fillGroups(bones, false);

        }

        this.scroll.add(this.poseEditor);
    }

    @Override
    public void update()
    {
        super.update();

        if (this.poseEditor != null)
        {
            this.poseEditor.setPose(this.keyframe.getValue(), this.poseEditor.getPoseGroupKey());
            this.poseEditor.refreshCurrentBone();
        }
    }

    @Override
    public void resize()
    {
        int mode = this.resolveLayoutMode();

        if (mode != this.layoutMode || this.poseEditor.getChildren().isEmpty())
        {
            this.rebuildPoseLayout(mode == 1);
            this.layoutMode = mode;
        }

        super.resize();

        /* Width is often still 0 on the first pass; rebuild once area is known. */
        int after = this.resolveLayoutMode();

        if (after != this.layoutMode)
        {
            this.rebuildPoseLayout(after == 1);
            this.layoutMode = after;
            super.resize();
        }
    }

    private int resolveLayoutMode()
    {
        int w = this.getFlex().getW();

        if (w <= 0)
        {
            w = this.area.w;
        }

        return w > 240 ? 1 : 0;
    }

    private void rebuildPoseLayout(boolean wide)
    {
        /* Closing shells before wipe avoids orphan ACTIVE shells after removeAll. */
        if (this.poseEditor.colorSection != null)
        {
            this.poseEditor.colorSection.setExpanded(false);
        }

        if (this.poseEditor.glowSection != null)
        {
            this.poseEditor.glowSection.setExpanded(false);
        }

        this.poseEditor.removeAll();

        /* Rebuilding the pose editor tree while a child trackpad is focused
         * leaves a stale textbox focus that freezes further edits. */
        UIContext context = this.getContext();

        if (context != null && context.activeElement instanceof UIElement focused
            && focused.getParent(UIPoseFactoryEditor.class) == this.poseEditor)
        {
            context.unfocus();
        }

        boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
        /* Wide Film Properties: Pick beside Opacity. Narrow + Model Editor stay stacked. */
        UIElement footer = this.poseEditor.createPoseFooter(wide);

        if (wide)
        {
            /* 5 transform rows at 20px + 4×5px column margins; Fix trackpad sits above. */
            int transformHeight = 20 * 5 + 5 * 4;
            int boneListHeight = 20 + 5 + transformHeight;

            this.poseEditor.transform.h(transformHeight);
            this.poseEditor.groups.h(boneListHeight);
            this.poseEditor.categories.h(Math.max(UIStringList.DEFAULT_HEIGHT, boneListHeight - 20));

            UIElement left = UI.column(
                UI.label(UIKeys.POSE_CONTEXT_FIX),
                this.poseEditor.fix,
                this.poseEditor.transform
            );

            UIElement groupsRow = categoriesEnabled ? UI.row(this.poseEditor.groups, this.poseEditor.categories) : UI.row(this.poseEditor.groups);
            UIElement right = UI.column(
                UI.label(UIKeys.FORMS_EDITOR_BONE),
                groupsRow
            );

            this.poseEditor.add(UI.row(left, right));
            this.poseEditor.add(footer);
        }
        else
        {
            UIElement groupsRow = categoriesEnabled ? UI.row(this.poseEditor.groups, this.poseEditor.categories) : UI.row(this.poseEditor.groups);
            this.poseEditor.add(
                UI.label(UIKeys.FORMS_EDITOR_BONE),
                groupsRow,
                UI.label(UIKeys.POSE_CONTEXT_FIX),
                this.poseEditor.fix,
                this.poseEditor.transform,
                footer
            );
        }

        /* Ew... */
        for (UIElement child : this.scroll.getChildren(UIElement.class))
        {
            child.noCulling();
        }
    }

    public static class UIPoseFactoryEditor extends UIPoseEditor
    {
        private UIKeyframes editor;
        private Keyframe<Pose> keyframe;

        public void refreshCurrentBone()
        {
            String currentBone = this.getCurrentBone();

            if (currentBone == null || currentBone.isEmpty())
            {
                currentBone = this.groups.list.getCurrentFirst();
            }

            this.pickBone(currentBone);
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, Consumer<Pose> consumer)
        {
            for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
            {
                if (sheet.channel.getFactory() != keyframe.getFactory()) continue;

                for (Keyframe kf : sheet.selection.getSelected())
                {
                    if (kf.getValue() instanceof Pose pose)
                    {
                        kf.preNotify();
                        consumer.accept(pose);
                        kf.postNotify();
                    }
                }
            }
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, String group, Consumer<PoseTransform> consumer)
        {
            apply(editor, keyframe, (pose) -> consumer.accept(pose.get(group)));
        }

        public UIPoseFactoryEditor(UIKeyframes editor, Keyframe<Pose> keyframe)
        {
            super();

            this.editor = editor;
            this.keyframe = keyframe;

            ((UIPoseTransforms) this.transform).setKeyframe(this);
        }

        @Override
        protected boolean useModelGizmoDrag()
        {
            /* Film pose uses FilmPoseGizmoDrag axis sign correction instead of setModel(). */
            return false;
        }

        @Override
        protected float getGizmoTranslationScale()
        {
            /* BOBJ bones translate in blocks; cubic groups use model pixels (/16). */
            return ModelFormRenderer.isBobjModel(this.model) ? 1F : 16F;
        }

        private String getGroup(PoseTransform transform)
        {
            return CollectionUtils.getKey(this.getPose().transforms, transform);
        }

        /** Acceso seguro a huesos de la categoría actual del grupo de pose. */
        public List<String> getCategoryBones(String category)
        {
            if (category == null || category.isEmpty())
            {
                return Collections.emptyList();
            }

            return this.boneCategories.getBones(this.getPoseGroupKey(), category);
        }

        public List<String> getLiveMirrorBonesForReplayEditor()
        {
            return this.getLiveMirrorBones();
        }

        public boolean shouldInvertLiveMirrorRotationZForReplayEditor(List<String> targets)
        {
            return this.shouldInvertLiveMirrorRotationZ(targets);
        }

        @Override
        protected UIPropTransform createTransformEditor()
        {
            /* Same ring / translate sign tuning as UIModelPoseEditor; film drag prepare clears
             * trackball euler flips when using the arcball sphere. */
            UIPoseTransforms editor = new UIPoseTransforms();

            editor.enableHotkeys();
            editor.translationScale(this.getGizmoTranslationScale());

            if (ModelFormRenderer.isBobjModel(this.model))
            {
                editor.bobjPoseGizmoTuning();
            }
            else
            {
                editor.poseModelGizmoTuning();
                editor.invertModelPoseTrackballXZ();
                editor.invertModelPoseTrackballDragY();
            }

            return editor;
        }

        @Override
        public void fillGroups(IModel model, Map<String, String> flippedParts, boolean reset)
        {
            super.fillGroups(model, flippedParts, reset);

            if (this.transform != null)
            {
                boolean bobj = ModelFormRenderer.isBobjModel(model);

                this.transform.translationScale(bobj ? 1F : 16F);
                this.transform.setAxisProjectedTranslation(bobj);

                if (bobj)
                {
                    this.transform.configurePoseRingTuning(true);
                }
                else
                {
                    this.transform.configurePoseRingTuning(false);
                }
            }
        }

        @Override
        protected void pastePose(MapType data)
        {
            String current = this.getCurrentBone();

            if (current == null || current.isEmpty())
            {
                current = this.groups.list.getCurrentFirst();
            }

            apply(this.editor, this.keyframe, (pose) -> pose.fromData(data));
            this.pickBone(current);
        }

        @Override
        protected void flipPose()
        {
            String current = this.getCurrentBone();

            if (current == null || current.isEmpty())
            {
                current = this.groups.list.getCurrentFirst();
            }

            apply(this.editor, this.keyframe, (pose) -> pose.flip(this.flippedParts));
            this.pickBone(current);
        }

        @Override
        protected void setFix(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) -> poseT.fix = value);
        }

        @Override
        protected void setColor(PoseTransform transform, int value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) ->
            {
                float intensity = poseT.color.a;

                poseT.color.set(value, false);
                poseT.color.a = intensity;
            });
        }

        @Override
        protected void setBlendIntensity(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) ->
                poseT.color.a = MathUtils.clamp(value, 0F, 1F));
        }

        @Override
        protected void editPoseColor(Consumer<Color> editor)
        {
            String group = this.getCurrentBone();

            if (group == null || group.isEmpty())
            {
                group = this.groups.list.getCurrentFirst();
            }

            if (group == null || group.isEmpty())
            {
                return;
            }

            apply(this.editor, this.keyframe, group, (poseT) -> editor.accept(poseT.color));
        }

        @Override
        protected void editPosePaintColor(Consumer<Color> editor)
        {
            String group = this.getCurrentBone();

            if (group == null || group.isEmpty())
            {
                group = this.groups.list.getCurrentFirst();
            }

            if (group == null || group.isEmpty())
            {
                return;
            }

            apply(this.editor, this.keyframe, group, (poseT) -> editor.accept(poseT.paintColor));
        }

        @Override
        protected void setPaintColor(PoseTransform transform, int value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) ->
            {
                float intensity = poseT.paintColor.a;

                poseT.paintColor.set(value);
                poseT.paintColor.a = intensity;
                poseT.shaderShadow = PaintSettings.resolveAutoShaderShadowForPoseAlpha(poseT.paintColor.a);
            });
        }

        @Override
        protected void setPaintIntensity(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) ->
            {
                poseT.paintColor.a = PaintSettings.clampIntensity(value);
                poseT.shaderShadow = PaintSettings.resolveAutoShaderShadowForPoseAlpha(poseT.paintColor.a);
            });
        }

        @Override
        protected void setGlowingColor(PoseTransform transform, int value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) ->
            {
                poseT.glowingColor.set(value);
                poseT.glowingColor.a = 1F;
            });
        }

        @Override
        protected void setGlowIntensity(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) -> poseT.glowIntensity = value);
        }

        @Override
        protected void setGlowRadius(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) -> poseT.glowRadius = value);
        }

        @Override
        protected void setLighting(PoseTransform poseTransform, boolean value)
        {
            apply(this.editor, this.keyframe, this.getGroup(poseTransform), (poseT) -> poseT.lighting = value ? 0F : 1F);
        }

        @Override
        protected void setTextureBlend(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) -> poseT.textureBlend = 1F);
        }
    }

    public static class UIPoseTransforms extends UIPropTransform
    {
        private UIPoseFactoryEditor editor;

        public void setKeyframe(UIPoseFactoryEditor editor)
        {
            this.editor = editor;
        }

        private void checkAutoKeyframe()
        {
            if (BBSSettings.realtimeKeyframes.get())
            {
                UIFilmPanel filmPanel = this.editor.getParent(UIFilmPanel.class);

                if (filmPanel != null)
                {
                    int cursor = filmPanel.getCursor();

                    if (cursor != this.editor.keyframe.getTick())
                    {
                        UIKeyframeSheet sheet = this.editor.editor.getGraph().getSheet(this.editor.keyframe);

                        if (sheet != null)
                        {
                            // Use interpolated pose at current cursor position instead of copying previous keyframe
                            Pose pose = (Pose) sheet.channel.interpolate(cursor);
                            String currentBone = this.editor.getCurrentBone();

                            if (currentBone == null || currentBone.isEmpty())
                            {
                                currentBone = this.editor.groups.list.getCurrentFirst();
                            }
                            int index = sheet.channel.insert(cursor, pose);
                            Keyframe<Pose> newKeyframe = sheet.channel.get(index);

                            this.editor.keyframe = newKeyframe;
                            this.editor.setPose(newKeyframe.getValue(), this.editor.getPoseGroupKey());

                            if (currentBone != null)
                            {
                                this.editor.groups.list.setCurrentScroll(currentBone);
                            }

                            this.editor.refreshCurrentBone();

                            // Explicitly update the transform editor's reference to the new keyframe's bone data
                            // This prevents the 'accumulation' bug where dx/dy/dz are calculated against the OLD keyframe
                            // but applied to the NEW keyframe, leading to exponential growth.
                            if (currentBone != null)
                            {
                                PoseTransform pt = newKeyframe.getValue().get(currentBone);
                                if (pt != null)
                                {
                                    this.setTransform(pt);
                                }
                            }

                            sheet.selection.clear();
                            sheet.selection.add(newKeyframe);
                        }
                    }
                }
            }
        }

        private void ensureTransformSync()
        {
            String currentBone = this.editor.getCurrentBone();

            if (currentBone == null || currentBone.isEmpty())
            {
                currentBone = this.editor.groups.list.getCurrentFirst();
            }

            if (currentBone != null)
            {
                PoseTransform pt = this.editor.keyframe.getValue().get(currentBone);

                if (pt != null && pt != this.getTransform())
                {
                    this.setTransform(pt);
                }
            }
        }

        /**
         * Targets affected by editing. If a category is selected, return all
         * bones in that category; otherwise return the currently selected group.
         */
        private List<String> targets()
        {
            boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
            String selectedCategory = categoriesEnabled && this.editor.categories != null ? this.editor.categories.getCurrentFirst() : null;
            if (selectedCategory == null || selectedCategory.isEmpty())
            {
                List<String> liveMirror = this.editor.getLiveMirrorBonesForReplayEditor();
                if (!liveMirror.isEmpty())
                {
                    return liveMirror;
                }

                String currentBone = this.editor.getCurrentBone();

                if (currentBone == null || currentBone.isEmpty())
                {
                    currentBone = this.editor.getGroup();
                }

                return Collections.singletonList(currentBone);
            }

            return this.editor.getCategoryBones(selectedCategory);
        }

        @Override
        protected void reset()
        {
            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) ->
                {
                    poseT.translate.set(0F, 0F, 0F);
                    poseT.scale.set(1F, 1F, 1F);
                    poseT.rotate.set(0F, 0F, 0F);
                    poseT.rotate2.set(0F, 0F, 0F);
                    poseT.pivot.set(0F, 0F, 0F);
                });
            }
            this.refillTransform();
        }

        @Override
        public void pasteTranslation(Vector3d translation)
        {
            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) -> poseT.translate.set(translation));
            }
            this.refillTransform();
        }

        @Override
        public void pasteScale(Vector3d scale)
        {
            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) -> poseT.scale.set(scale));
            }
            this.refillTransform();
        }

        @Override
        public void pasteRotation(Vector3d rotation)
        {
            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) -> poseT.rotate.set(Vectors.toRad(rotation)));
            }
            this.refillTransform();
        }

        @Override
        public void pasteRotation2(Vector3d rotation)
        {
            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) -> poseT.rotate2.set(Vectors.toRad(rotation)));
            }
            this.refillTransform();
        }

        @Override
        public void pastePivot(Vector3d pivot)
        {
            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) -> poseT.pivot.set((float) pivot.x, (float) pivot.y, (float) pivot.z));
            }
            this.refillTransform();
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            this.checkAutoKeyframe();
            this.ensureTransformSync();
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) ->
                {
                    poseT.translate.x += dx;
                    poseT.translate.y += dy;
                    poseT.translate.z += dz;
                });
            }
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            this.checkAutoKeyframe();
            this.ensureTransformSync();
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) ->
                {
                    poseT.scale.x += dx;
                    poseT.scale.y += dy;
                    poseT.scale.z += dz;
                });
            }
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            this.checkAutoKeyframe();
            this.ensureTransformSync();
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;
            List<String> targets = this.targets();
            boolean invertAxes = this.editor.shouldInvertLiveMirrorRotationZForReplayEditor(targets);
            String sourceBone = this.editor.getCurrentBone();

            for (String key : targets)
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) ->
                {
                    boolean mirroredBone = invertAxes && !key.equals(sourceBone);
                    poseT.rotate.x += mirroredBone ? -dx : dx;
                    poseT.rotate.y += mirroredBone ? -dy : dy;
                    poseT.rotate.z += mirroredBone ? -dz : dz;
                });
            }
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            this.checkAutoKeyframe();
            this.ensureTransformSync();
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;
            List<String> targets = this.targets();
            boolean invertAxes = this.editor.shouldInvertLiveMirrorRotationZForReplayEditor(targets);
            String sourceBone = this.editor.getCurrentBone();

            for (String key : targets)
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) ->
                {
                    boolean mirroredBone = invertAxes && !key.equals(sourceBone);
                    poseT.rotate2.x += mirroredBone ? -dx : dx;
                    poseT.rotate2.y += mirroredBone ? -dy : dy;
                    poseT.rotate2.z += mirroredBone ? -dz : dz;
                });
            }
        }

        @Override
        public void setP(Axis axis, double x, double y, double z)
        {
            this.checkAutoKeyframe();
            this.ensureTransformSync();
            Transform transform = this.getTransform();
            float dx = (float) x - transform.pivot.x;
            float dy = (float) y - transform.pivot.y;
            float dz = (float) z - transform.pivot.z;

            for (String key : this.targets())
            {
                UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, key, (poseT) ->
                {
                    poseT.pivot.x += dx;
                    poseT.pivot.y += dy;
                    poseT.pivot.z += dz;
                });
            }
        }
    }
}
