package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.BBSFeatures;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.PerLimbService;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.ui.film.ICursor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIInverseKinematicsKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UILookAtKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UIReplaysEditorUtils
{
        public static UIPropTransform getEditableTransform(UIKeyframeEditor editor)
    {
        if (editor == null || editor.editor == null)
        {
            return null;
        }

        if (editor.editor instanceof UITransformKeyframeFactory transformKeyframeFactory)
        {
            return transformKeyframeFactory.transform;
        }
        else if (editor.editor instanceof UIPoseKeyframeFactory keyframeFactory)
        {
            return keyframeFactory.poseEditor.transform;
        }
        else if (editor.editor instanceof UIAnchorKeyframeFactory anchorFactory)
        {
            return anchorFactory.transform;
        }

        return null;
    }

    public static <T> void forEachSelectedKeyframe(UIKeyframes editor, Keyframe<?> keyframe, Consumer<Keyframe<T>> consumer)
    {
        if (editor == null || keyframe == null)
        {
            return;
        }

        for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
        {
            if (sheet.channel.getFactory() != keyframe.getFactory())
            {
                continue;
            }

            for (Keyframe selected : sheet.selection.getSelected())
            {
                consumer.accept((Keyframe<T>) selected);
            }
        }
    }

    /**
     * Collect ticks of selected Color keyframes (paint companions live on a hidden channel).
     * Color grade uses its own channel and does not drive paint companions.
     */
    public static List<Float> collectSelectedColorTicks(UIKeyframes editor)
    {
        List<Float> ticks = new ArrayList<>();

        if (editor == null || editor.getGraph() == null)
        {
            return ticks;
        }

        for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
        {
            if (!isColorSheet(sheet))
            {
                continue;
            }

            for (Keyframe selected : sheet.selection.getSelected())
            {
                ticks.add(selected.getTick());
            }
        }

        return ticks;
    }

    public static void removeCompanionPaintForColorTicks(UIKeyframes editor, Collection<Float> ticks)
    {
        if (editor == null || ticks == null || ticks.isEmpty())
        {
            return;
        }

        UIReplaysEditor replays = editor.getParent(UIReplaysEditor.class);

        if (replays == null || replays.getReplay() == null)
        {
            return;
        }

        Form form = replays.getReplay().form.get();

        replays.getReplay().properties.removeCompanionPaintAtTicks(form, ticks);
    }

    public static void removeCompanionPaintForSelectedColor(UIKeyframes editor)
    {
        removeCompanionPaintForColorTicks(editor, collectSelectedColorTicks(editor));
    }

    public static void removeCompanionPaintForColorKeyframe(UIKeyframes editor, Keyframe keyframe)
    {
        if (editor == null || keyframe == null)
        {
            return;
        }

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

        if (sheet == null || !isColorSheet(sheet))
        {
            return;
        }

        removeCompanionPaintForColorTicks(editor, Collections.singletonList(keyframe.getTick()));
    }

    private static boolean isColorSheet(UIKeyframeSheet sheet)
    {
        if (sheet == null || sheet.id == null)
        {
            return false;
        }

        String name = StringUtils.fileName(sheet.id);

        return name.equals("color");
    }

    public static void moveCompanionPaintForSelectedColor(UIKeyframes editor, float diff)
    {
        if (editor == null || Math.abs(diff) < 0.0001F)
        {
            return;
        }

        List<Float> ticks = collectSelectedColorTicks(editor);

        if (ticks.isEmpty())
        {
            return;
        }

        UIReplaysEditor replays = editor.getParent(UIReplaysEditor.class);

        if (replays == null || replays.getReplay() == null)
        {
            return;
        }

        replays.getReplay().properties.moveCompanionPaintBy(diff, ticks);
    }

    /* Picking form and form properties */

    private static boolean isBonePickProperty(String propertyId)
    {
        if (propertyId.equals("pose") || propertyId.startsWith("pose_overlay"))
        {
            return true;
        }

        return BBSFeatures.isFormIkLookAtUiEnabled() && BBSFeatures.isFormIkLookAtProperty(propertyId);
    }

    public static void pickFormProperty(UIContext context, UIKeyframeEditor editor, ICursor cursor, Form form, String bone)
    {
        String path = FormUtils.getPath(form);
        boolean shift = Window.isShiftPressed();
        ContextMenuManager manager = new ContextMenuManager();

        manager.autoKeys();

        for (BaseValueBasic formProperty : form.getAllMap().values())
        {
            if (!formProperty.isVisible())
            {
                continue;
            }

            manager.action(UIReplaysEditor.getIcon(formProperty.getId()), IKey.constant(formProperty.getId()), () ->
            {
                pickProperty(editor, cursor, bone, StringUtils.combinePaths(path, formProperty.getId()), shift);
            });
        }

        context.replaceContextMenu(manager.create());
    }

    public static void pickForm(UIKeyframeEditor keyframeEditor, ICursor cursor, Form form, String bone)
    {
        String path = FormUtils.getPath(form);

        if (keyframeEditor == null || bone.isEmpty())
        {
            return;
        }

        Keyframe selected = keyframeEditor.view.getGraph().getSelected();
        String type = "pose";

        if (selected != null)
        {
            String id = selected.getParent().getId();
            int colon = id.indexOf(':');
            String pathWithProperty = colon != -1 ? id.substring(0, colon) : id;

            if (pathWithProperty.startsWith(path))
            {
                String propertyId = StringUtils.fileName(pathWithProperty);

                if (isBonePickProperty(propertyId))
                {
                    type = propertyId;
                }
            }
        }
        else
        {
            UIKeyframeSheet lastSheet = keyframeEditor.view.getGraph().getLastSheet();

            if (lastSheet != null)
            {
                String id = lastSheet.id;
                int colon = id.indexOf(':');
                String pathWithProperty = colon != -1 ? id.substring(0, colon) : id;

                if (pathWithProperty.startsWith(path))
                {
                    String propertyId = StringUtils.fileName(pathWithProperty);

                    if (isBonePickProperty(propertyId))
                    {
                        type = propertyId;
                    }
                }
            }
            else if (keyframeEditor.editor instanceof UILookAtKeyframeFactory)
            {
                type = BBSFeatures.isFormIkLookAtUiEnabled() ? "look_at" : "pose";
            }
            else if (keyframeEditor.editor instanceof UIInverseKinematicsKeyframeFactory)
            {
                type = BBSFeatures.isFormIkLookAtUiEnabled() ? "inverse_kinematics" : "pose";
            }
        }

        pickProperty(keyframeEditor, cursor, bone, StringUtils.combinePaths(path, type), false);
    }

    private static void pickProperty(UIKeyframeEditor keyframeEditor, ICursor cursor, String bone, String key, boolean insert)
    {
        IUIKeyframeGraph graph = keyframeEditor.view.getGraph();
        Keyframe selected = graph.getSelected();
        UIKeyframeSheet activeSheet = selected != null ? graph.getSheet(selected) : null;

        if (activeSheet != null)
        {
            String id = activeSheet.id;
            int colon = id.indexOf(':');
            String baseId = colon != -1 ? id.substring(0, colon) : id;
            String boneId = colon != -1 ? id.substring(colon + 1) : null;

            if (baseId.equals(key))
            {
                if (boneId == null || boneId.equals(bone))
                {
                    pickProperty(keyframeEditor, cursor, bone, activeSheet, insert);

                    return;
                }
            }
        }

        /* Redirect to limb track if it exists */
        if (bone != null && !bone.isEmpty())
        {
            String limbTrackId = key + ":" + bone;
            UIKeyframeSheet limbSheet = keyframeEditor.view.getGraph().getSheet(limbTrackId);

            if (limbSheet != null)
            {
                pickProperty(keyframeEditor, cursor, bone, limbSheet, insert);

                return;
            }
        }

        UIKeyframeSheet sheet = keyframeEditor.view.getGraph().getSheet(key);

        if (sheet != null)
        {
            pickProperty(keyframeEditor, cursor, bone, sheet, insert);
        }
        else if (bone != null && !bone.isEmpty() && keyframeEditor != null)
        {
            if (keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
            {
                if (Window.isCtrlPressed())
                {
                    poseFactory.poseEditor.addBoneToSelection(bone);
                }
                else
                {
                    poseFactory.poseEditor.selectBone(bone);
                }
            }
            else if (keyframeEditor.editor instanceof UILookAtKeyframeFactory lookAtFactory)
            {
                lookAtFactory.lookAtEditor.selectBone(bone);
            }
            else if (keyframeEditor.editor instanceof UIInverseKinematicsKeyframeFactory ikFactory)
            {
                ikFactory.ikEditor.selectBone(bone);
            }
        }
    }

    private static void pickProperty(UIKeyframeEditor keyframeEditor, ICursor filmPanel, String bone, UIKeyframeSheet sheet, boolean insert)
    {
        IUIKeyframeGraph graph = keyframeEditor.view.getGraph();
        int tick = filmPanel.getCursor();

        if (insert)
        {
            Keyframe keyframe = graph.addKeyframe(sheet, tick, null);

            graph.selectKeyframe(keyframe);

            return;
        }

        KeyframeSegment segment = sheet.channel.find(tick);
        Keyframe closest = null;

        if (segment != null)
        {
            closest = segment.getClosest();
        }
        else if (!sheet.channel.isEmpty())
        {
            closest = sheet.channel.get(0);
        }

        if (closest != null)
        {
            if (graph.getSelected() != closest)
            {
                boolean select = true;

                for (UIKeyframeSheet graphSheet : graph.getSheets())
                {
                    if (graphSheet.selection.getSelected().contains(closest))
                    {
                        select = false;

                        break;
                    }
                }

                if (select) graph.selectKeyframe(closest);
                else graph.pickKeyframe(closest);
            }

            if (keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
            {
                if (Window.isCtrlPressed())
                {
                    poseFactory.poseEditor.addBoneToSelection(bone);
                }
                else
                {
                    poseFactory.poseEditor.selectBone(bone);
                }
            }
            else if (keyframeEditor.editor instanceof UILookAtKeyframeFactory lookAtFactory)
            {
                lookAtFactory.lookAtEditor.selectBone(bone);
            }
            else if (keyframeEditor.editor instanceof UIInverseKinematicsKeyframeFactory ikFactory)
            {
                ikFactory.ikEditor.selectBone(bone);
            }

            filmPanel.setCursor((int) closest.getTick());
        }
        else if (keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
        {
            if (Window.isCtrlPressed())
            {
                poseFactory.poseEditor.addBoneToSelection(bone);
            }
            else
            {
                poseFactory.poseEditor.selectBone(bone);
            }
        }
        else if (keyframeEditor.editor instanceof UILookAtKeyframeFactory lookAtFactory)
        {
            lookAtFactory.lookAtEditor.selectBone(bone);
        }
        else if (keyframeEditor.editor instanceof UIInverseKinematicsKeyframeFactory ikFactory)
        {
            ikFactory.ikEditor.selectBone(bone);
        }
    }

    /**
     * One texture track per model material (OBJ material name / BOBJ mesh name), enumerated from
     * the loaded model. Each is a LINK channel layered over the material's static default at
     * playback - mirrors the bone tracks. Lives in the Model category beside the main texture track.
     */
    public static void addMaterialTextureSheets(ModelForm modelForm, FormProperties properties, List<UIKeyframeSheet> out)
    {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model == null)
        {
            return;
        }

        String path = FormUtils.getPath(modelForm);

        for (String material : model.materials)
        {
            if (material == null || material.isEmpty())
            {
                continue;
            }

            String id = PerLimbService.toMaterialTextureKey(path, material);
            String title = path.isEmpty() ? "Texture/" + material : path + "/Texture/" + material;
            KeyframeChannel channel = properties.getOrCreate(modelForm, id);

            /* Seed the sheet's value with the material's current default texture (editor pick, else
             * folder/Kd, else the form/model default) so a new keyframe starts there instead of null -
             * the texture picker then opens at that texture rather than the root. */
            Link materialDefault = modelForm.materialTextures.getLink(material);

            if (materialDefault == null)
            {
                materialDefault = model.getMaterialTexture(material, model.texture);
            }

            ValueLink property = new ValueLink(id, materialDefault);

            out.add(new UIKeyframeSheet(id, IKey.constant(title), Colors.BLUE, false, channel, property).icon(Icons.MATERIAL));
        }
    }

    /* Converting Blockbench model keyframes to pose keyframes */

    public static void animationToPoseKeyframes(
        UIKeyframeEditor keyframeEditor, UIKeyframeSheet sheet,
        ModelForm modelForm, IEntity entity,
        int tick, String animationKey, boolean onlyKeyframes, int length, int step
    ) {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);
        Animation animation = model.animations.get(animationKey);

        if (animation != null)
        {
            keyframeEditor.view.getDopeSheet().clearSelection();

            if (onlyKeyframes)
            {
                List<Float> list = getTicks(animation);

                for (float i : list)
                {
                    fillAnimationPose(sheet, i, model, entity, animation, tick);
                }
            }
            else
            {
                for (int i = 0; i < length; i += step)
                {
                    fillAnimationPose(sheet, i, model, entity, animation, tick);
                }
            }

            keyframeEditor.view.getDopeSheet().pickSelected();
        }
    }

    private static List<Float> getTicks(Animation animation)
    {
        Set<Float> integers = new HashSet<>();

        for (AnimationPart value : animation.parts.values())
        {
            for (KeyframeChannel<MolangExpression> channel : value.channels)
            {
                for (Keyframe<MolangExpression> keyframe : channel.getKeyframes())
                {
                    integers.add(keyframe.getTick());
                }
            }
        }

        ArrayList<Float> ticks = new ArrayList<>(integers);

        Collections.sort(ticks);

        return ticks;
    }

    private static void fillAnimationPose(UIKeyframeSheet sheet, float i, ModelInstance model, IEntity entity, Animation animation, int current)
    {
        model.model.resetPose();
        model.model.apply(entity, animation, i, 1F, 0F, false);

        int insert = sheet.channel.insert(current + i, model.model.createPose());

        sheet.selection.add(insert);
    }

    /* Offer bone hierarchy options */

    public static void offerAdjacent(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getAdjacentGroups(bone))
                {
                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }

    public static void offerHierarchy(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getHierarchyGroups(bone))
                {
                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }
}
