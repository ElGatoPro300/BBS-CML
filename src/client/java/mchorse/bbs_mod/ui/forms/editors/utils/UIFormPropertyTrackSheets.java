package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSFeatures;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIVisibleRenderKeyframeUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Builds form-property keyframe sheets with the same filtering / grouping conventions as
 * {@link UIReplaysEditor} (color children, illusion overlays, pose overlay limbs, color grade),
 * without film entity/world channels (x/y/z, yaw, …).
 */
public final class UIFormPropertyTrackSheets
{
    private static final Set<String> HIDDEN_MODEL_PROPERTIES = Set.of(
        "glowing_color", "glow_settings", "glow_intensity", "paint_color"
    );

    /** Film-entity targeting tracks that do not apply to form animation states / model blocks. */
    private static final Set<String> ANIMATION_STATE_HIDDEN_PROPERTIES = Set.of(
        "look_at", "inverse_kinematics"
    );

    private static final List<String> MODEL_PROPERTIES = Arrays.asList(
        "visible", "render", "lighting", "transform", "transform_overlay", "pose", "pose_overlay",
        "anchor", "look_at", "inverse_kinematics", "illusion", "illusion_transform", "color",
        "color2", "color_mode", "color_grade", "paint", "paint_color", "glow", "texture",
        "pbr_normal_intensity", "pbr_specular_intensity", "model", "actions", "shape_keys",
        "block_state", "item_stack", "modelTransform", "same_animation_when_dropped", "settings",
        "paused", "frequency", "count", "structure_file", "biome_id", "emit_light",
        "light_intensity", "structure_light", "enabled", "level", "effect"
    );

    private UIFormPropertyTrackSheets()
    {
    }

    public static boolean isHiddenModelProperty(String key)
    {
        if (key == null || key.isEmpty())
        {
            return false;
        }

        String name = propertyName(key);

        if ("using_item".equals(name) || "item_use_time".equals(name))
        {
            return false;
        }

        String path = propertyPath(key);

        return path.endsWith("tint_block_entities")
            || path.endsWith("_item")
            || HIDDEN_MODEL_PROPERTIES.contains(name)
            || name.startsWith("illusion_transform")
            || (!BBSFeatures.isFormIkLookAtUiEnabled() && BBSFeatures.isFormIkLookAtProperty(name));
    }

    public static boolean isAnimationStateHiddenProperty(String key)
    {
        if (isHiddenModelProperty(key))
        {
            return true;
        }

        return ANIMATION_STATE_HIDDEN_PROPERTIES.contains(propertyName(key));
    }

    /**
     * Collects, filters, creates, sorts and groups form-property sheets for animation-state
     * editors. Reuses {@link FormProperties} channel creation / migration (same as films).
     */
    public static List<UIKeyframeSheet> buildAnimationStateSheets(Form form, FormProperties properties, Map<String, Boolean> collapsed, Runnable onRefresh, String collapseScope)
    {
        Set<String> propertyPaths = collectAnimationStatePropertyPaths(form, properties);
        List<UIKeyframeSheet> sheets = createSheets(form, properties, propertyPaths);

        sortByFormPathAndPriority(sheets);

        sheets = groupModelTracks(sheets, form, properties, collapsed, onRefresh, collapseScope);

        UIBlockRepeatKeyframeUtils.groupRepeatSheets(sheets, collapsed, onRefresh);

        sheets.removeIf((sheet) ->
        {
            if (sheet.id.equals("anchor") || sheet.id.endsWith("/anchor"))
            {
                return true;
            }

            for (String disabled : BBSSettings.disabledSheets.get())
            {
                if (sheet.id.equals(disabled) || sheet.id.endsWith("/" + disabled))
                {
                    return true;
                }
            }

            return false;
        });

        markFormSeparators(sheets);

        return sheets;
    }

    public static Set<String> collectAnimationStatePropertyPaths(Form form, FormProperties properties)
    {
        Set<String> propertyPaths = new LinkedHashSet<>(FormUtils.collectPropertyPaths(form));

        FormUtils.addPairedRenderPropertyPaths(propertyPaths, propertyPaths);
        collectLimbTracks(form, propertyPaths);

        if (properties != null)
        {
            for (String key : properties.properties.keySet())
            {
                if (isCompatiblePropertyPath(form, key))
                {
                    propertyPaths.add(key);
                }
            }
        }

        propertyPaths.removeIf(UIFormPropertyTrackSheets::isAnimationStateHiddenProperty);
        propertyPaths.removeIf(UIVisibleRenderKeyframeUtils::isRenderTimelineHidden);

        return propertyPaths;
    }

    public static void collectLimbTracks(Form form, Set<String> propertyPaths)
    {
        if (form == null || !form.animatable.get())
        {
            return;
        }

        if (form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model != null)
            {
                String path = FormUtils.getPath(modelForm);
                List<Pair<String, Integer>> orderedBones = collectBoneOrder(model.model);

                for (Pair<String, Integer> bone : orderedBones)
                {
                    if (bone.a.startsWith("armor_") || bone.a.endsWith("_item"))
                    {
                        continue;
                    }

                    propertyPaths.add(StringUtils.combinePaths(path, "pose") + ":" + bone.a);
                    propertyPaths.add(StringUtils.combinePaths(path, "pose_overlay") + ":" + bone.a);

                    for (int i = 0, c = modelForm.additionalOverlays.size(); i < c; i++)
                    {
                        propertyPaths.add(StringUtils.combinePaths(path, "pose_overlay" + i) + ":" + bone.a);
                    }
                }
            }
        }

        for (BodyPart part : form.parts.getAllTyped())
        {
            collectLimbTracks(part.getForm(), propertyPaths);
        }
    }

    public static boolean isCompatiblePropertyPath(Form rootForm, String key)
    {
        if (rootForm == null || key == null || key.isEmpty())
        {
            return false;
        }

        int colon = key.indexOf(':');
        String path = colon == -1 ? key : key.substring(0, colon);

        if (FormProperties.isColorGradeChannelKey(path))
        {
            String colorPath = FormProperties.colorPropertyPathForGrade(path);
            BaseValueBasic colorProperty = FormUtils.getProperty(rootForm, colorPath);

            return colorProperty instanceof ValueColor;
        }

        BaseValueBasic property = FormUtils.getProperty(rootForm, path);

        if (property == null)
        {
            return false;
        }

        if (colon == -1)
        {
            return true;
        }

        return isCompatibleBoneProperty(property, key.substring(colon + 1));
    }

    private static List<UIKeyframeSheet> createSheets(Form form, FormProperties properties, Set<String> propertyPaths)
    {
        List<UIKeyframeSheet> sheets = new ArrayList<>();

        for (String key : propertyPaths)
        {
            KeyframeChannel channel = properties.getOrCreate(form, key);

            if (channel == null)
            {
                continue;
            }

            BaseValueBasic formProperty = FormUtils.getProperty(form, key);

            if (formProperty == null && FormProperties.isColorGradeChannelKey(key))
            {
                formProperty = FormUtils.getProperty(form, FormProperties.colorPropertyPathForGrade(key));
            }

            String title = key;
            int colon = key.indexOf(':');

            if (colon != -1)
            {
                title = key.substring(colon + 1);
            }
            else
            {
                IKey resolved = UIReplaysEditor.resolvePropertyTrackTitle(StringUtils.fileName(key));

                if (resolved != null)
                {
                    UIKeyframeSheet sheet = new UIKeyframeSheet(key, resolved, UIReplaysEditor.getColor(key), false, channel, formProperty);

                    withTrackIcon(sheet, key);
                    sheets.add(sheet);
                    continue;
                }
            }

            UIKeyframeSheet sheet = new UIKeyframeSheet(key, IKey.constant(title), UIReplaysEditor.getColor(key), false, channel, formProperty);

            withTrackIcon(sheet, key);
            sheets.add(sheet);
        }

        return sheets;
    }

    private static void withTrackIcon(UIKeyframeSheet sheet, String key)
    {
        Icon icon = UIReplaysEditor.getIcon(key);

        if (icon != null)
        {
            sheet.icon(icon);
        }
    }

    private static List<UIKeyframeSheet> groupModelTracks(List<UIKeyframeSheet> sheets, Form rootForm, FormProperties properties, Map<String, Boolean> collapsed, Runnable onRefresh, String collapseScope)
    {
        if (rootForm == null || sheets.isEmpty())
        {
            return sheets;
        }

        String rootPath = FormUtils.getPath(rootForm);
        List<UIKeyframeSheet> before = new ArrayList<>();
        List<UIKeyframeSheet> pose = new ArrayList<>();
        List<UIKeyframeSheet> limbs = new ArrayList<>();
        List<UIKeyframeSheet> overlayRoots = new ArrayList<>();
        List<UIKeyframeSheet> overlayLimbs = new ArrayList<>();
        List<UIKeyframeSheet> after = new ArrayList<>();
        Map<String, FormTracks> subForms = new LinkedHashMap<>();

        for (UIKeyframeSheet sheet : sheets)
        {
            Form form = sheet.property == null ? null : FormUtils.getForm(sheet.property);

            if (form == null)
            {
                int colon = sheet.id.indexOf(':');

                if (colon != -1)
                {
                    String propertyPath = sheet.id.substring(0, colon);
                    int lastSlash = propertyPath.lastIndexOf('/');
                    String path = lastSlash == -1 ? "" : propertyPath.substring(0, lastSlash);

                    form = FormUtils.getForm(rootForm, path);
                }
            }

            if (form == null)
            {
                processTrack(sheet, collapseScope + ":__model__", 0, before, pose, limbs, overlayRoots, overlayLimbs, after, collapsed, onRefresh, collapseScope);
                continue;
            }

            String path = FormUtils.getPath(form);

            if (path.equals(rootPath) || path.isEmpty())
            {
                processTrack(sheet, collapseScope + ":__model__", 0, before, pose, limbs, overlayRoots, overlayLimbs, after, collapsed, onRefresh, collapseScope);
            }
            else
            {
                Form sheetForm = form;
                FormTracks tracks = subForms.get(path);

                if (tracks == null)
                {
                    tracks = new FormTracks(sheetForm);
                    subForms.put(path, tracks);
                }

                processTrack(sheet, collapseScope + ":" + path, path.split("/").length, tracks.before, tracks.pose, tracks.limbs, tracks.overlayRoots, tracks.overlayLimbs, tracks.after, collapsed, onRefresh, collapseScope);
            }
        }

        orderLimbTracks(rootForm, limbs, collapsed, onRefresh, collapseScope);
        List<UIKeyframeSheet> orderedOverlays = orderOverlayTracks(rootForm, overlayRoots, overlayLimbs, collapsed, onRefresh, collapseScope);

        injectColorGradeSheets(before, rootForm, properties);
        injectColorGradeSheets(after, rootForm, properties);

        List<UIKeyframeSheet> grouped = new ArrayList<>();

        grouped.addAll(before);
        grouped.addAll(pose);
        grouped.addAll(limbs);
        grouped.addAll(orderedOverlays);
        grouped.addAll(after);

        List<String> subFormPaths = new ArrayList<>(subForms.keySet());

        subFormPaths.sort(UIFormPropertyTrackSheets::comparePathsNaturally);

        for (String path : subFormPaths)
        {
            FormTracks tracks = subForms.get(path);
            String groupKey = collapseScope + ":" + path;
            int level = path.split("/").length;
            boolean expanded = !collapsed.getOrDefault(groupKey, false);

            UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
                "__group__" + groupKey,
                IKey.constant(tracks.form.getDisplayName()),
                Colors.LIGHTEST_GRAY & Colors.RGB,
                groupKey,
                expanded,
                () ->
                {
                    collapsed.put(groupKey, !collapsed.getOrDefault(groupKey, false));
                    onRefresh.run();
                }
            );

            header.level = Math.max(0, level - 1);
            grouped.add(header);

            if (expanded)
            {
                orderLimbTracks(tracks.form, tracks.limbs, collapsed, onRefresh, collapseScope);
                List<UIKeyframeSheet> orderedSubOverlays = orderOverlayTracks(tracks.form, tracks.overlayRoots, tracks.overlayLimbs, collapsed, onRefresh, collapseScope);

                injectColorGradeSheets(tracks.before, rootForm, properties);
                injectColorGradeSheets(tracks.after, rootForm, properties);

                grouped.addAll(tracks.before);
                grouped.addAll(tracks.pose);
                grouped.addAll(tracks.limbs);
                grouped.addAll(orderedSubOverlays);
                grouped.addAll(tracks.after);
            }
        }

        return grouped;
    }

    private static void processTrack(UIKeyframeSheet sheet, String groupKey, int level, List<UIKeyframeSheet> before, List<UIKeyframeSheet> pose, List<UIKeyframeSheet> limbs, List<UIKeyframeSheet> overlayRoots, List<UIKeyframeSheet> overlayLimbs, List<UIKeyframeSheet> after, Map<String, Boolean> collapsed, Runnable onRefresh, String collapseScope)
    {
        sheet.level = level;
        int colon = sheet.id.indexOf(':');
        String trackName = StringUtils.fileName(sheet.id);

        if (colon != -1)
        {
            sheet.title = IKey.constant(sheet.id.substring(colon + 1));
        }
        else
        {
            IKey propertyTitle = UIReplaysEditor.resolvePropertyTrackTitle(trackName);

            if (propertyTitle != null)
            {
                sheet.title = propertyTitle;
            }
            else if (sheet.property != null)
            {
                Form trackForm = FormUtils.getForm(sheet.property);

                if (trackForm != null)
                {
                    sheet.title = IKey.constant(trackForm.getTrackName(sheet.channel.getId()));
                }
            }
        }

        String scopeKey = groupKey == null || groupKey.isEmpty() ? collapseScope + ":__model__" : groupKey;
        String textureParentKey = scopeKey + ":texture";
        String itemStackParentKey = scopeKey + ":item_stack";
        String illusionParentKey = scopeKey + ":illusion";
        String colorParentKey = scopeKey + ":color";
        boolean isPbrTrack = trackName.equals("pbr_normal_intensity") || trackName.equals("pbr_specular_intensity");
        boolean isColorChildTrack = trackName.equals("paint") || trackName.equals("paint_color")
            || trackName.equals("glow") || trackName.equals("glow_settings")
            || trackName.equals("color_grade")
            || trackName.equals("color2") || trackName.equals("color_mode");

        if (isPbrTrack)
        {
            if (collapsed.getOrDefault(textureParentKey, true))
            {
                return;
            }

            sheet.level += 1;

            if (trackName.equals("pbr_normal_intensity"))
            {
                sheet.title = UIKeys.FILM_REPLAY_TRACK_PBR_NORMAL_INTENSITY;
            }
            else if (trackName.equals("pbr_specular_intensity"))
            {
                sheet.title = UIKeys.FILM_REPLAY_TRACK_PBR_SPECULAR_INTENSITY;
            }
        }

        if (isColorChildTrack)
        {
            if (collapsed.getOrDefault(colorParentKey, true))
            {
                return;
            }

            sheet.level += 1;

            if (trackName.equals("color_grade"))
            {
                sheet.title = UIKeys.FORMS_EDITORS_COLOR_GRADE;
            }
            else if (trackName.equals("color2"))
            {
                sheet.title = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR2;
            }
            else if (trackName.equals("color_mode"))
            {
                sheet.title = UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COLOR_MODE;
            }
        }

        if (colon != -1)
        {
            String parentId = sheet.id.substring(0, colon);
            String parentKey = collapseScope + ":" + parentId;

            if (collapsed.getOrDefault(parentKey, true))
            {
                return;
            }

            sheet.level += 1;

            String parentTrackName = StringUtils.fileName(parentId);

            if (parentTrackName.startsWith("pose_overlay"))
            {
                overlayLimbs.add(sheet);
            }
            else
            {
                limbs.add(sheet);
            }
        }
        else if (trackName.equals("pose"))
        {
            String parentKey = collapseScope + ":" + sheet.id;
            boolean expanded = !collapsed.getOrDefault(parentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                collapsed.put(parentKey, !collapsed.getOrDefault(parentKey, true));
                onRefresh.run();
            };

            pose.add(sheet);
        }
        else if (trackName.startsWith("pose_overlay"))
        {
            String parentKey = collapseScope + ":" + sheet.id;
            boolean expanded = !collapsed.getOrDefault(parentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                collapsed.put(parentKey, !collapsed.getOrDefault(parentKey, true));
                onRefresh.run();
            };

            overlayRoots.add(sheet);
        }
        else if (trackName.equals("texture"))
        {
            boolean expanded = !collapsed.getOrDefault(textureParentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                collapsed.put(textureParentKey, !collapsed.getOrDefault(textureParentKey, true));
                onRefresh.run();
            };

            addTrackByPriority(trackName, before, after, sheet);
        }
        else if (trackName.equals("color"))
        {
            boolean expanded = !collapsed.getOrDefault(colorParentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                collapsed.put(colorParentKey, !collapsed.getOrDefault(colorParentKey, true));
                onRefresh.run();
            };

            addTrackByPriority(trackName, before, after, sheet);
        }
        else if (trackName.equals("item_stack"))
        {
            boolean expanded = !collapsed.getOrDefault(itemStackParentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                collapsed.put(itemStackParentKey, !collapsed.getOrDefault(itemStackParentKey, true));
                onRefresh.run();
            };

            addTrackByPriority(trackName, before, after, sheet);
        }
        else if ("item_use_time".equals(sheet.id) && sheet.property != null)
        {
            if (collapsed.getOrDefault(itemStackParentKey, true))
            {
                return;
            }

            sheet.level += 1;
            sheet.title = UIKeys.FILM_REPLAY_TRACK_ITEM_USE_TIME_LABEL;
            after.add(sheet);
        }
        else if (trackName.startsWith("transform_overlay") || trackName.equals("transform"))
        {
            before.add(sheet);
        }
        else if (trackName.equals("illusion"))
        {
            boolean expanded = !collapsed.getOrDefault(illusionParentKey, true);

            sheet.expanded = expanded;
            sheet.toggleExpanded = () ->
            {
                collapsed.put(illusionParentKey, !collapsed.getOrDefault(illusionParentKey, true));
                onRefresh.run();
            };

            after.add(sheet);
        }
        else if (isIllusionOverlayTrack(trackName))
        {
            if (collapsed.getOrDefault(illusionParentKey, true))
            {
                return;
            }

            sheet.level += 1;
            after.add(sheet);
        }
        else
        {
            addTrackByPriority(trackName, before, after, sheet);
        }
    }

    private static void injectColorGradeSheets(List<UIKeyframeSheet> tracks, Form rootForm, FormProperties properties)
    {
        for (int i = 0; i < tracks.size(); i++)
        {
            UIKeyframeSheet sheet = tracks.get(i);
            String name = StringUtils.fileName(sheet.id);

            if (!name.equals("color") || sheet.groupHeader || !sheet.expanded)
            {
                continue;
            }

            Form form = sheet.property == null ? null : FormUtils.getForm(sheet.property);

            if (form instanceof LabelForm || form instanceof TrailForm)
            {
                continue;
            }

            int insertAt = i + 1;
            boolean hasGrade = false;

            while (insertAt < tracks.size())
            {
                String child = StringUtils.fileName(tracks.get(insertAt).id);

                if (child.equals("paint") || child.equals("paint_color") || child.equals("glow") || child.equals("glow_settings"))
                {
                    insertAt++;
                }
                else if (child.equals("color_grade"))
                {
                    hasGrade = true;
                    insertAt++;
                }
                else
                {
                    break;
                }
            }

            if (!hasGrade)
            {
                tracks.add(insertAt, createColorGradeSheet(sheet, rootForm, properties));
                insertAt++;
            }

            String scopePrefix = sheet.id.equals("color") ? "" : sheet.id.substring(0, sheet.id.length() - "color".length());

            for (int j = insertAt; j < tracks.size(); j++)
            {
                UIKeyframeSheet candidate = tracks.get(j);
                String childName = StringUtils.fileName(candidate.id);

                if (childName.equals("color2") || childName.equals("color_mode"))
                {
                    String candidatePrefix = candidate.id.equals(childName) ? "" : candidate.id.substring(0, candidate.id.length() - childName.length());

                    if (scopePrefix.equals(candidatePrefix))
                    {
                        tracks.remove(j);
                        tracks.add(insertAt, candidate);
                        insertAt++;
                        j--;
                    }
                }
            }

            i = insertAt - 1;
        }
    }

    private static UIKeyframeSheet createColorGradeSheet(UIKeyframeSheet colorSheet, Form rootForm, FormProperties properties)
    {
        String gradeId;

        if (colorSheet.id.equals("color"))
        {
            gradeId = "color_grade";
        }
        else if (colorSheet.id.endsWith("/color"))
        {
            gradeId = colorSheet.id.substring(0, colorSheet.id.length() - "color".length()) + "color_grade";
        }
        else
        {
            gradeId = colorSheet.id + "/color_grade";
        }

        KeyframeChannel gradeChannel = properties.getOrCreate(rootForm, gradeId);

        if (gradeChannel == null)
        {
            gradeChannel = colorSheet.channel;
        }

        UIKeyframeSheet grade = new UIKeyframeSheet(
            gradeId,
            UIKeys.FORMS_EDITORS_COLOR_GRADE,
            UIReplaysEditor.getColor("color_grade"),
            false,
            gradeChannel,
            colorSheet.property
        );

        grade.level = colorSheet.level + 1;
        grade.icon(Icons.FAVORITE);

        return grade;
    }

    private static void addTrackByPriority(String trackName, List<UIKeyframeSheet> before, List<UIKeyframeSheet> after, UIKeyframeSheet sheet)
    {
        int poseIndex = MODEL_PROPERTIES.indexOf("pose");
        int currentIndex = MODEL_PROPERTIES.indexOf(trackName);

        if (currentIndex != -1 && poseIndex != -1 && currentIndex < poseIndex)
        {
            before.add(sheet);
        }
        else
        {
            after.add(sheet);
        }
    }

    private static boolean isIllusionOverlayTrack(String trackName)
    {
        if (trackName.equals("illusion_overlay"))
        {
            return true;
        }

        return trackName.startsWith("illusion_overlay") && trackName.length() > "illusion_overlay".length();
    }

    private static void orderLimbTracks(Form form, List<UIKeyframeSheet> limbs, Map<String, Boolean> collapsed, Runnable onRefresh, String collapseScope)
    {
        if (form == null || limbs.isEmpty() || !(form instanceof ModelForm modelForm))
        {
            return;
        }

        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model == null)
        {
            return;
        }

        List<Pair<String, Integer>> orderedBones = collectBoneOrder(model.model);

        if (orderedBones.isEmpty())
        {
            return;
        }

        Map<String, UIKeyframeSheet> limbByBone = new HashMap<>();

        for (UIKeyframeSheet limb : limbs)
        {
            int colon = limb.id.indexOf(':');

            if (colon == -1)
            {
                continue;
            }

            limbByBone.put(limb.id.substring(colon + 1), limb);
        }

        Map<String, String> parentByBone = collectBoneParents(model.model);
        Map<String, List<String>> childrenByBone = collectChildren(parentByBone);
        int baseLevel = limbs.get(0).level;
        List<UIKeyframeSheet> reordered = new ArrayList<>();
        Set<UIKeyframeSheet> used = new HashSet<>();

        for (Pair<String, Integer> bone : orderedBones)
        {
            UIKeyframeSheet limb = limbByBone.get(bone.a);

            if (limb == null)
            {
                continue;
            }

            if (isAncestorCollapsed(limb, parentByBone, collapsed, collapseScope))
            {
                used.add(limb);
                continue;
            }

            limb.level = baseLevel + bone.b;
            applyLimbExpandState(limb, bone.a, childrenByBone, limbByBone, collapsed, onRefresh, collapseScope);
            reordered.add(limb);
            used.add(limb);
        }

        for (UIKeyframeSheet limb : limbs)
        {
            if (used.contains(limb))
            {
                continue;
            }

            limb.level = baseLevel;
            limb.toggleExpanded = null;
            reordered.add(limb);
        }

        limbs.clear();
        limbs.addAll(reordered);
    }

    private static List<UIKeyframeSheet> orderOverlayTracks(Form form, List<UIKeyframeSheet> overlayRoots, List<UIKeyframeSheet> overlayLimbs, Map<String, Boolean> collapsed, Runnable onRefresh, String collapseScope)
    {
        List<UIKeyframeSheet> ordered = new ArrayList<>();

        if (overlayRoots.isEmpty() && overlayLimbs.isEmpty())
        {
            return ordered;
        }

        Set<UIKeyframeSheet> used = new HashSet<>();

        for (UIKeyframeSheet root : overlayRoots)
        {
            ordered.add(root);
            used.add(root);

            List<UIKeyframeSheet> limbs = new ArrayList<>();
            String prefix = root.id + ":";

            for (UIKeyframeSheet limb : overlayLimbs)
            {
                if (limb.id.startsWith(prefix))
                {
                    limbs.add(limb);
                    used.add(limb);
                }
            }

            orderLimbTracks(form, limbs, collapsed, onRefresh, collapseScope);
            ordered.addAll(limbs);
        }

        for (UIKeyframeSheet limb : overlayLimbs)
        {
            if (!used.contains(limb))
            {
                ordered.add(limb);
            }
        }

        return ordered;
    }

    private static void applyLimbExpandState(UIKeyframeSheet limb, String boneName, Map<String, List<String>> childrenByBone, Map<String, UIKeyframeSheet> limbByBone, Map<String, Boolean> collapsed, Runnable onRefresh, String collapseScope)
    {
        if (!hasChildTrack(boneName, childrenByBone, limbByBone))
        {
            limb.toggleExpanded = null;
            return;
        }

        String key = collapseScope + ":" + limb.id;
        boolean expanded = !collapsed.getOrDefault(key, false);

        limb.expanded = expanded;
        limb.toggleExpanded = () ->
        {
            collapsed.put(key, !collapsed.getOrDefault(key, false));
            onRefresh.run();
        };
    }

    private static boolean hasChildTrack(String boneName, Map<String, List<String>> childrenByBone, Map<String, UIKeyframeSheet> limbByBone)
    {
        List<String> children = childrenByBone.get(boneName);

        if (children == null || children.isEmpty())
        {
            return false;
        }

        for (String child : children)
        {
            if (limbByBone.containsKey(child))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean isAncestorCollapsed(UIKeyframeSheet limb, Map<String, String> parentByBone, Map<String, Boolean> collapsed, String collapseScope)
    {
        int colon = limb.id.indexOf(':');

        if (colon == -1)
        {
            return false;
        }

        String poseTrackId = limb.id.substring(0, colon);
        String boneName = limb.id.substring(colon + 1);
        String parent = parentByBone.get(boneName);

        while (parent != null)
        {
            String key = collapseScope + ":" + poseTrackId + ":" + parent;

            if (collapsed.getOrDefault(key, false))
            {
                return true;
            }

            parent = parentByBone.get(parent);
        }

        return false;
    }

    private static boolean isCompatibleBoneProperty(BaseValueBasic property, String boneName)
    {
        if (boneName == null || boneName.isEmpty() || !(property.getParent() instanceof Form parentForm))
        {
            return false;
        }

        if (!(parentForm instanceof ModelForm modelForm))
        {
            return false;
        }

        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model == null)
        {
            return false;
        }

        IModel modelDef = model.model;

        if (modelDef instanceof Model cubicModel)
        {
            return cubicModel.getAllGroupKeys().contains(boneName);
        }

        Collection<BOBJBone> bones = modelDef.getAllBOBJBones();

        if (bones != null)
        {
            for (BOBJBone bone : bones)
            {
                if (boneName.equals(bone.name))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static void sortByFormPathAndPriority(List<UIKeyframeSheet> sheets)
    {
        sheets.sort((a, b) ->
        {
            Form formA = a.property == null ? null : FormUtils.getForm(a.property);
            Form formB = b.property == null ? null : FormUtils.getForm(b.property);
            String pathA = formA == null ? "" : FormUtils.getPath(formA);
            String pathB = formB == null ? "" : FormUtils.getPath(formB);
            int pathComp = comparePathsNaturally(pathA, pathB);

            if (pathComp != 0)
            {
                return pathComp;
            }

            ToIntFunction<UIKeyframeSheet> getPriority = (sheet) ->
            {
                String name = StringUtils.fileName(sheet.id);

                if (name.equals("visible")) return 0;
                if (name.equals("render")) return 1;
                if (name.equals("lighting")) return 2;
                if (name.equals("transform")) return 10;
                if (name.startsWith("transform_overlay"))
                {
                    String suffix = name.substring("transform_overlay".length());

                    if (suffix.isEmpty()) return 11;

                    try { return 12 + Integer.parseInt(suffix); } catch (Exception e) { return 19; }
                }

                if (name.equals("pose")) return 20;
                if (name.startsWith("pose_overlay"))
                {
                    if (name.indexOf(':') != -1) return 29;
                    String suffix = name.substring("pose_overlay".length());

                    if (suffix.isEmpty()) return 21;

                    try { return 22 + Integer.parseInt(suffix); } catch (Exception e) { return 28; }
                }

                if (name.indexOf(':') != -1) return 29;
                if (name.equals("anchor")) return 30;
                if (name.equals("look_at")) return 31;
                if (name.equals("inverse_kinematics")) return 32;
                if (name.equals("illusion")) return 33;
                if (name.equals("illusion_overlay")) return 34;
                if (name.startsWith("illusion_overlay") && name.length() > "illusion_overlay".length())
                {
                    String suffix = name.substring("illusion_overlay".length());

                    try { return 35 + Integer.parseInt(suffix); } catch (Exception e) { return 50; }
                }

                if (name.equals("structure_file")) return 60;
                if (name.equals("pivot")) return 61;
                if (name.equals("biome_id")) return 62;
                if (name.equals("structure_light")) return 63;
                if (name.equals("color")) return 64;
                if (name.equals("color_grade")) return 65;
                if (name.equals("paint_color") || name.equals("paint")) return 66;
                if (name.equals("glow") || name.equals("glow_settings")) return 67;
                if (name.equals("texture")) return 68;
                if (name.equals("pbr_normal_intensity")) return 69;
                if (name.equals("pbr_specular_intensity")) return 70;
                if (name.equals("model")) return 71;
                if (name.equals("item_stack")) return 72;
                if (name.equals("block_state")) return 73;
                if (name.equals("breaking")) return 74;
                if (name.equals("repeat_x") || name.equals("repeat_y") || name.equals("repeat_z")
                    || name.equals("repeat_center_x") || name.equals("repeat_center_y") || name.equals("repeat_center_z")) return 75;

                return 500;
            };

            int priorityA = getPriority.applyAsInt(a);
            int priorityB = getPriority.applyAsInt(b);

            if (priorityA != priorityB)
            {
                return Integer.compare(priorityA, priorityB);
            }

            if (priorityA == 29)
            {
                String boneA = a.id.substring(a.id.indexOf(':') + 1);
                String boneB = b.id.substring(b.id.indexOf(':') + 1);

                return compareNaturally(boneA, boneB);
            }

            return 0;
        });
    }

    private static void markFormSeparators(List<UIKeyframeSheet> sheets)
    {
        Object lastForm = null;

        for (UIKeyframeSheet sheet : sheets)
        {
            if (sheet.groupHeader)
            {
                sheet.separator = false;
                lastForm = null;
                continue;
            }

            Object form = sheet.property == null ? null : FormUtils.getForm(sheet.property);

            if (!Objects.equals(lastForm, form))
            {
                sheet.separator = true;
            }

            lastForm = form;
        }
    }

    private static Map<String, String> collectBoneParents(IModel model)
    {
        Map<String, String> parentByBone = new HashMap<>();

        if (model instanceof Model cubicModel)
        {
            collectBoneParentsFromGroups(cubicModel.topGroups, null, parentByBone);
        }
        else
        {
            Collection<BOBJBone> bones = model.getAllBOBJBones();

            if (bones != null && !bones.isEmpty())
            {
                for (BOBJBone bone : bones)
                {
                    if (bone.parentBone != null)
                    {
                        parentByBone.put(bone.name, bone.parentBone.name);
                    }
                }
            }
        }

        return parentByBone;
    }

    private static void collectBoneParentsFromGroups(List<ModelGroup> groups, String parent, Map<String, String> parentByBone)
    {
        for (ModelGroup group : groups)
        {
            if (parent != null)
            {
                parentByBone.put(group.id, parent);
            }

            if (!group.children.isEmpty())
            {
                collectBoneParentsFromGroups(group.children, group.id, parentByBone);
            }
        }
    }

    private static Map<String, List<String>> collectChildren(Map<String, String> parentByBone)
    {
        Map<String, List<String>> childrenByBone = new HashMap<>();

        for (Map.Entry<String, String> entry : parentByBone.entrySet())
        {
            childrenByBone.computeIfAbsent(entry.getValue(), (key) -> new ArrayList<>()).add(entry.getKey());
        }

        return childrenByBone;
    }

    private static List<Pair<String, Integer>> collectBoneOrder(IModel model)
    {
        List<Pair<String, Integer>> orderedBones = new ArrayList<>();

        if (model instanceof Model cubicModel)
        {
            collectBonesFromGroups(cubicModel.topGroups, 0, orderedBones);
        }
        else
        {
            Collection<BOBJBone> bones = model.getAllBOBJBones();

            if (bones != null && !bones.isEmpty())
            {
                for (BOBJBone bone : bones)
                {
                    int depth = 0;
                    BOBJBone parent = bone.parentBone;

                    while (parent != null)
                    {
                        depth += 1;
                        parent = parent.parentBone;
                    }

                    orderedBones.add(new Pair<>(bone.name, depth));
                }
            }
            else
            {
                for (String bone : model.getAllGroupKeys())
                {
                    orderedBones.add(new Pair<>(bone, 0));
                }
            }
        }

        return orderedBones;
    }

    private static void collectBonesFromGroups(List<ModelGroup> groups, int depth, List<Pair<String, Integer>> orderedBones)
    {
        for (ModelGroup group : groups)
        {
            orderedBones.add(new Pair<>(group.id, depth));

            if (!group.children.isEmpty())
            {
                collectBonesFromGroups(group.children, depth + 1, orderedBones);
            }
        }
    }

    private static String propertyPath(String key)
    {
        int colon = key.indexOf(':');

        return colon == -1 ? key : key.substring(0, colon);
    }

    private static String propertyName(String key)
    {
        String path = propertyPath(key);
        int slash = path.lastIndexOf('/');

        return slash == -1 ? path : path.substring(slash + 1);
    }

    private static int comparePathsNaturally(String a, String b)
    {
        if (a.equals(b))
        {
            return 0;
        }

        String[] left = a.split("/");
        String[] right = b.split("/");
        int min = Math.min(left.length, right.length);

        for (int i = 0; i < min; i++)
        {
            int cmp = compareNaturally(left[i], right[i]);

            if (cmp != 0)
            {
                return cmp;
            }
        }

        return Integer.compare(left.length, right.length);
    }

    private static int compareNaturally(String a, String b)
    {
        int i = 0;
        int j = 0;

        while (i < a.length() && j < b.length())
        {
            char ca = a.charAt(i);
            char cb = b.charAt(j);

            if (Character.isDigit(ca) && Character.isDigit(cb))
            {
                int startI = i;
                int startJ = j;

                while (i < a.length() && Character.isDigit(a.charAt(i)))
                {
                    i++;
                }

                while (j < b.length() && Character.isDigit(b.charAt(j)))
                {
                    j++;
                }

                long na = Long.parseLong(a.substring(startI, i));
                long nb = Long.parseLong(b.substring(startJ, j));

                if (na != nb)
                {
                    return Long.compare(na, nb);
                }
            }
            else
            {
                if (ca != cb)
                {
                    return Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
                }

                i++;
                j++;
            }
        }

        return Integer.compare(a.length() - i, b.length() - j);
    }

    private static class FormTracks
    {
        public final Form form;
        public final List<UIKeyframeSheet> before = new ArrayList<>();
        public final List<UIKeyframeSheet> pose = new ArrayList<>();
        public final List<UIKeyframeSheet> limbs = new ArrayList<>();
        public final List<UIKeyframeSheet> overlayRoots = new ArrayList<>();
        public final List<UIKeyframeSheet> overlayLimbs = new ArrayList<>();
        public final List<UIKeyframeSheet> after = new ArrayList<>();

        public FormTracks(Form form)
        {
            this.form = form;
        }
    }
}
