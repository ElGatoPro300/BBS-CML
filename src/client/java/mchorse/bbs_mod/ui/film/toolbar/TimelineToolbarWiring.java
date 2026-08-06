package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.l10n.keys.LangKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.UIScreenNodeEditor;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.interps.Interpolations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Phase 2 wiring for timeline toolbar actions. Handlers mirror the existing
 * keybind / context-menu behaviour on {@link UIFilmPanel}, {@link UIClips} and
 * {@link UIKeyframes}.
 *
 * <p>Bindings are applied after each {@link TimelineToolbar#setSections(List)}
 * call because the registry returns fresh item trees.</p>
 */
public final class TimelineToolbarWiring
{
    /* Clips timelines (camera / action) */

    public static void wireClipsToolbar(UIClipsPanel panel)
    {
        UIFilmPanel filmPanel = panel.filmPanel;
        UIClips clips = panel.clips;
        TimelineToolbar toolbar = panel.toolbar;

        wireReplayAndHistory(filmPanel, toolbar);
        wireClipsSelection(clips, toolbar);
        wireClipsEditInstant(clips, toolbar);
        wireClipsEditSelection(clips, toolbar);
        wireClipsTransform(clips, toolbar);
        wireClipsOverlays(panel);
        wireClipsAddSubmenus(panel);
        wireClipsAddModes(clips, toolbar);
    }

    /* Keyframe toolbar (embedded in clip panel or replay editor) */

    /* Screen node graph toolbar (embedded in Screen Node clip) */

    public static void wireScreenNodeToolbar(UIFilmPanel filmPanel, UIScreenNodeEditor editor, TimelineToolbar toolbar)
    {
        wireScreenNodeAdd(editor, toolbar);
        wireScreenNodeEdit(editor, toolbar);
        wireScreenNodeHistory(filmPanel, toolbar);
    }

    public static void wireKeyframesToolbar(UIFilmPanel filmPanel, UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        wireReplayAndHistory(filmPanel, toolbar);
        wireKeyframesAdd(keyframes, toolbar);
        wireKeyframesInstant(keyframes, toolbar);
        wireKeyframesEditSelection(keyframes, toolbar);
        wireKeyframesSelect(keyframes, toolbar);
        wireKeyframesGraphSelection(keyframes, toolbar);
        wireTracksInstant(keyframes, toolbar);
        wireKeyframesOverlays(keyframes, toolbar);
    }

    public static void wireReplaysToolbar(UIReplaysEditor editor)
    {
        wireReplayAndHistory(editor.getFilmPanel(), editor.toolbar);
        wireKeyframesAdd(editor, editor.toolbar);
        wireKeyframesInstant(editor, editor.toolbar);
        wireKeyframesEditSelection(editor, editor.toolbar);
        wireKeyframesSelect(editor, editor.toolbar);
        wireKeyframesGraphSelection(editor, editor.toolbar);
        wireTracksInstant(editor, editor.toolbar);
        wireKeyframesOverlays(editor);
        wireKeyframesEditTrack(editor);
        wireTracksOverlays(editor);
        wireActorSection(editor);
    }

    /* Shared replay + undo/redo */

    private static void wireReplayAndHistory(UIFilmPanel filmPanel, TimelineToolbar toolbar)
    {
        BooleanSupplier editorActive = () -> filmPanel.getData() != null && !filmPanel.isFlying();
        BooleanSupplier filmLoaded = () -> filmPanel.getData() != null;

        bindShortcut(toolbar, Keys.PLAUSE, () -> filmPanel.preview.plause.clickItself(), editorActive);
        bindShortcut(toolbar, Keys.NEXT, () -> filmPanel.setCursor(filmPanel.getCursor() + 1), editorActive);
        bindShortcut(toolbar, Keys.PREV, () -> filmPanel.setCursor(filmPanel.getCursor() - 1), editorActive);
        bindShortcut(toolbar, Keys.JUMP_FORWARD,
            () -> filmPanel.setCursor(filmPanel.getCursor() + BBSSettings.editorJump.get()), editorActive);
        bindShortcut(toolbar, Keys.JUMP_BACKWARD,
            () -> filmPanel.setCursor(filmPanel.getCursor() - BBSSettings.editorJump.get()), editorActive);
        bindShortcut(toolbar, Keys.NEXT_CLIP,
            () -> filmPanel.setCursor(filmPanel.getData().camera.findNextTick(filmPanel.getCursor())), editorActive);
        bindShortcut(toolbar, Keys.PREV_CLIP,
            () -> filmPanel.setCursor(filmPanel.getData().camera.findPreviousTick(filmPanel.getCursor())), editorActive);
        bindShortcut(toolbar, Keys.LOOPING, () ->
        {
            BBSSettings.editorLoop.set(!BBSSettings.editorLoop.get());
            UIUtils.playClick();
        }, editorActive);
        bindShortcut(toolbar, Keys.LOOPING_SET_MIN, () -> filmPanel.cameraEditor.clips.toolbarEnterLoopMin(), editorActive);
        bindShortcut(toolbar, Keys.LOOPING_SET_MAX, () -> filmPanel.cameraEditor.clips.toolbarEnterLoopMax(), editorActive);
        bindShortcut(toolbar, Keys.FILM_CONTROLLER_RESTART_ACTIONS, () ->
        {
            filmPanel.notifyServer(ActionState.RESTART);
            filmPanel.getController().createEntities();
        }, filmLoaded);
        bindShortcut(toolbar, Keys.UNDO, filmPanel::undo, filmLoaded);
        bindShortcut(toolbar, Keys.REDO, filmPanel::redo, filmLoaded);
    }

    /* Clip timeline instant actions */

    private static void wireClipsSelection(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasSelection = () -> clips.getDelegateClip() != null && canUse.getAsBoolean();

        bindShortcut(toolbar, Keys.DESELECT, clips::toolbarDeselectAll, hasSelection);
        bindShortcut(toolbar, Keys.CLIP_SELECT_BEFORE, clips::toolbarSelectBefore, canUse);
        bindShortcut(toolbar, Keys.CLIP_SELECT_AFTER, clips::toolbarSelectAfter, canUse);
    }

    private static void wireClipsEditInstant(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasSelection = () -> canUse.getAsBoolean() && !clips.getSelection().isEmpty();

        bindShortcut(toolbar, Keys.CLIP_ENABLE, clips::toolbarToggleEnabled, hasSelection);
    }

    private static void wireClipsEditSelection(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasDelegateClip = () -> clips.getDelegateClip() != null && canUse.getAsBoolean();
        BooleanSupplier hasSelection = () -> canUse.getAsBoolean() && !clips.getSelection().isEmpty();

        bindShortcut(toolbar, Keys.COPY, clips::toolbarCopy, hasDelegateClip);
        bindShortcut(toolbar, Keys.CUT, clips::toolbarCut, hasDelegateClip);
        bindShortcut(toolbar, Keys.DELETE, clips::toolbarRemoveSelected, hasSelection);
        bindShortcut(toolbar, Keys.CLIP_CUT, clips::toolbarCutClipAtCursor, hasDelegateClip);
        bindShortcut(toolbar, Keys.CLIP_SHIFT, clips::toolbarShiftToCursor, hasSelection);
        bindShortcut(toolbar, Keys.CLIP_DURATION, clips::toolbarShiftDurationToCursor, hasSelection);
    }

    private static void wireClipsTransform(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier hasDelegateClip = () -> clips.getDelegateClip() != null && clips.canUseToolbarKeybinds();

        bindShortcut(toolbar, Keys.FADE_IN, clips::toolbarFadeIn, hasDelegateClip);
        bindShortcut(toolbar, Keys.FADE_OUT, clips::toolbarFadeOut, hasDelegateClip);
    }

    /* Clip timeline overlays / submenus (Phase 2c) */

    private static void wireClipsOverlays(UIClipsPanel panel)
    {
        UIClips clips = panel.clips;
        TimelineToolbar toolbar = panel.toolbar;
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();

        bindShortcut(toolbar, Keys.PASTE, clips::toolbarPaste, canUse);
        bindShortcut(toolbar, Keys.PRESETS, clips::toolbarOpenPresets, canUse);

        if (panel.isCameraTimeline())
        {
            bindLabel(toolbar, UIKeys.CAMERA_TIMELINE_CONTEXT_REORGANIZE, clips::toolbarReorganize, canUse);
            wireClipsConvertSubmenu(clips, toolbar);
        }
    }

    private static void wireClipsConvertSubmenu(UIClips clips, TimelineToolbar toolbar)
    {
        ToolbarItem convert = findLabelInSections(toolbar.getSections(), UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT);

        if (convert == null)
        {
            return;
        }

        convert.children.clear();

        Set<Link> targets = new LinkedHashSet<>();

        for (Link key : clips.getFactory().getKeys())
        {
            targets.addAll(clips.getFactory().getData(key).converters.keySet());
        }

        BooleanSupplier hasDelegate = () -> clips.getDelegateClip() != null && clips.canUseToolbarKeybinds();

        for (Link type : targets)
        {
            Link targetType = type;

            ToolbarItem child = ToolbarItem.action(
                    UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT_TO.format(UIKeys.C_CLIP.get(targetType)))
                .icon(Icons.REFRESH)
                .run(() -> clips.toolbarConvertTo(targetType))
                .enabledIf(() ->
                {
                    if (!hasDelegate.getAsBoolean())
                    {
                        return false;
                    }

                    Clip clip = clips.getDelegateClip();
                    ClipFactoryData data = clips.getFactory().getData(clip);

                    return data.converters.containsKey(targetType);
                });

            convert.children.add(child);
        }

        convert.enabledIf(() ->
        {
            if (!hasDelegate.getAsBoolean())
            {
                return false;
            }

            Clip clip = clips.getDelegateClip();

            return !clips.getFactory().getData(clip).converters.isEmpty();
        });
    }

    private static void wireClipsAddSubmenus(UIClipsPanel panel)
    {
        UIClips clips = panel.clips;
        TimelineToolbar toolbar = panel.toolbar;
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();

        if (panel.isCameraTimeline())
        {
            wireClipTypeGroups(clips, toolbar, TimelineClipTypeGroups.forCamera(clips.getFactory()), canUse);
            wireReplayImportSubmenu(clips, toolbar, canUse);
            bindLabel(toolbar, UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE, clips::toolbarRecordMicrophone,
                canUse);
        }
        else
        {
            wireClipTypeGroups(clips, toolbar, TimelineClipTypeGroups.forAction(clips.getFactory()), canUse);
        }
    }

    private static void wireClipsAddModes(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasDelegateClip = () -> clips.getDelegateClip() != null && canUse.getAsBoolean();

        bindShortcut(toolbar, Keys.ADD_AT_CURSOR, clips::toolbarShowAddsAtCursor, canUse);
        bindShortcut(toolbar, Keys.ADD_AT_TICK, clips::toolbarShowAddsAtTick, canUse);
        bindShortcut(toolbar, Keys.ADD_ON_TOP, clips::toolbarShowAddsOnTop, hasDelegateClip);
    }

    private static void wireClipTypeGroups(UIClips clips, TimelineToolbar toolbar,
        List<TimelineClipTypeGroups.ClipGroup> groups, BooleanSupplier canUse)
    {
        for (TimelineClipTypeGroups.ClipGroup group : groups)
        {
            ToolbarItem container = findLabelInSections(toolbar.getSections(), group.label);

            if (container == null)
            {
                continue;
            }

            container.children.clear();

            for (Link type : group.types)
            {
                Link targetType = type;
                ClipFactoryData data = clips.getFactory().getData(targetType);

                container.children.add(ToolbarItem.action(
                        UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_CLIP_TYPE.format(UIKeys.C_CLIP.get(targetType)))
                    .icon(data.icon)
                    .accentColor(data.color)
                    .run(() -> clips.toolbarAddClipType(targetType))
                    .enabledIf(canUse));
            }

            container.enabledIf(canUse);
        }
    }

    private static void wireReplayImportSubmenu(UIClips clips, TimelineToolbar toolbar, BooleanSupplier canUse)
    {
        ToolbarItem container = findLabelInSections(toolbar.getSections(),
            UIKeys.CAMERA_TIMELINE_CONTEXT_FROM_PLAYER_RECORDING);

        if (container == null)
        {
            return;
        }

        container.children.clear();

        Film film = clips.getFilm();

        if (film == null)
        {
            container.enabledIf(() -> false);

            return;
        }

        for (Replay replay : film.replays.getList())
        {
            Replay targetReplay = replay;
            Form form = replay.form.get();
            IKey label = IKey.constant(form == null ? "-" : form.getFormIdOrName());

            container.children.add(ToolbarItem.action(label)
                .icon(Icons.EDITOR)
                .run(() -> clips.toolbarImportReplay(targetReplay))
                .enabledIf(canUse));
        }

        container.enabledIf(() -> canUse.getAsBoolean() && !film.replays.getList().isEmpty());
    }

    /* Keyframe timeline overlays / submenus (Phase 2c) */

    private static void wireInsertKeyframeSubmenu(TimelineToolbar toolbar, BooleanSupplier canModify,
        BooleanSupplier canColumnInsert, Runnable insertAtTimeline, Runnable enterColumnAtCursor,
        Runnable enterSingleAtTimeline, Runnable enterSingleAtCursor)
    {
        ToolbarItem parent = findLabelInSections(toolbar.getSections(), UIKeys.FILM_CONTROLLER_KEYS_INSERT_FRAME);

        if (parent == null)
        {
            return;
        }

        parent.run(insertAtTimeline);
        parent.enabledIf(canModify);

        bindInsertChild(parent, UIKeys.KEYFRAMES_INSERT_AT_TIMELINE, insertAtTimeline, canColumnInsert);
        bindInsertChild(parent, UIKeys.KEYFRAMES_INSERT_AT_CURSOR, enterColumnAtCursor, canColumnInsert);
        bindInsertChild(parent, UIKeys.KEYFRAMES_INSERT_SINGLE_AT_TIMELINE, enterSingleAtTimeline, canModify);
        bindInsertChild(parent, UIKeys.KEYFRAMES_INSERT_SINGLE_AT_CURSOR, enterSingleAtCursor, canModify);
    }

    private static void applyColumnInsertDisabledReason(TimelineToolbar toolbar, Supplier<IKey> reason)
    {
        ToolbarItem parent = findLabelInSections(toolbar.getSections(), UIKeys.FILM_CONTROLLER_KEYS_INSERT_FRAME);

        if (parent == null)
        {
            return;
        }

        for (ToolbarItem child : parent.children)
        {
            if (child.separator)
            {
                continue;
            }

            if (child.label == UIKeys.KEYFRAMES_INSERT_AT_TIMELINE
                || child.label == UIKeys.KEYFRAMES_INSERT_AT_CURSOR
                || labelsMatch(child.label, UIKeys.KEYFRAMES_INSERT_AT_TIMELINE)
                || labelsMatch(child.label, UIKeys.KEYFRAMES_INSERT_AT_CURSOR))
            {
                child.disabledReason(reason);
            }
        }
    }

    private static void wireDuplicateSubmenu(TimelineToolbar toolbar, BooleanSupplier hasSelected,
        Runnable enterAtCursor, Runnable enterAtPlayhead)
    {
        ToolbarItem parent = findLabelInSections(toolbar.getSections(), UIKeys.KEYFRAMES_DUPLICATE);

        if (parent == null)
        {
            return;
        }

        parent.run(enterAtCursor);
        parent.enabledIf(hasSelected);

        bindInsertChild(parent, UIKeys.KEYFRAMES_DUPLICATE_AT_CURSOR, enterAtCursor, hasSelected);
        bindInsertChild(parent, UIKeys.KEYFRAMES_DUPLICATE_AT_TIMELINE, enterAtPlayhead, hasSelected);
    }

    private static void wirePasteSubmenu(TimelineToolbar toolbar, BooleanSupplier canPaste,
        Runnable pasteAtTimeline, Runnable pasteAtCursor)
    {
        ToolbarItem parent = findLabelInSections(toolbar.getSections(), UIKeys.KEYFRAMES_CONTEXT_PASTE);

        if (parent == null)
        {
            return;
        }

        parent.run(pasteAtCursor);
        parent.enabledIf(canPaste);

        bindInsertChild(parent, UIKeys.KEYFRAMES_CONTEXT_PASTE_AT_CURSOR, pasteAtCursor, canPaste);
        bindInsertChild(parent, UIKeys.KEYFRAMES_CONTEXT_PASTE_AT_TIMELINE, pasteAtTimeline, canPaste);
    }

    private static void bindInsertChild(ToolbarItem parent, IKey label, Runnable runnable,
        BooleanSupplier enabled)
    {
        for (ToolbarItem child : parent.children)
        {
            if (child.separator)
            {
                continue;
            }

            if (child.label == label || labelsMatch(child.label, label))
            {
                apply(child, runnable, enabled);

                return;
            }
        }
    }

    private static void wireKeyframesAdd(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean() && keyframes.hasSelectedKeyframes();
        BooleanSupplier canColumnInsert = () -> false;
        Supplier<IKey> columnInsertDisabledReason = () -> UIKeys.TIMELINE_TOOLBAR_DISABLED_COLUMN_INSERT_REPLAY_ONLY;

        wireInsertKeyframeSubmenu(toolbar, canModify, canColumnInsert,
            () -> {},
            () -> {},
            () ->
            {
                int tick = keyframes instanceof UIFilmKeyframes filmKeyframes ? filmKeyframes.getOffset() : 0;

                keyframes.enterKeyframeInsert(KeyframeInsertInteractionState.individualAtPlayhead(
                    UIKeys.TIMELINE_INTERACTION_INSERT_KEYFRAME_SINGLE_TIMELINE,
                    tick,
                    keyframes::toolbarInsertIndividual));
            },
            () -> keyframes.enterKeyframeInsert(KeyframeInsertInteractionState.individualAtCursor(
                UIKeys.TIMELINE_INTERACTION_INSERT_KEYFRAME_SINGLE_CURSOR,
                keyframes::toolbarInsertIndividual)));

        applyColumnInsertDisabledReason(toolbar, columnInsertDisabledReason);
        ToolbarItem parent = findLabelInSections(toolbar.getSections(), UIKeys.FILM_CONTROLLER_KEYS_INSERT_FRAME);

        if (parent != null)
        {
            parent.run(() ->
            {
                int tick = keyframes instanceof UIFilmKeyframes filmKeyframes ? filmKeyframes.getOffset() : 0;

                keyframes.enterKeyframeInsert(KeyframeInsertInteractionState.individualAtPlayhead(
                    UIKeys.TIMELINE_INTERACTION_INSERT_KEYFRAME_SINGLE_TIMELINE,
                    tick,
                    keyframes::toolbarInsertIndividual));
            });
        }

        bindLabel(toolbar, UIKeys.KEYFRAMES_KEYS_SELECT_COLUMN, keyframes::toolbarSelectColumn, canModify);

        wireDuplicateSubmenu(toolbar, hasSelected,
            keyframes::toolbarEnterDuplicateAtCursor,
            keyframes::toolbarEnterDuplicateAtPlayhead);
    }

    private static void wireKeyframesAdd(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canModify = () -> hasEditor.getAsBoolean()
            && editor.keyframeEditor.view.isModifyingKeyframes();
        BooleanSupplier canColumnInsert = editor::canToolbarInsertKeyframeColumn;
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean()
            && editor.keyframeEditor.view.hasSelectedKeyframes();

        wireInsertKeyframeSubmenu(toolbar, canModify, canColumnInsert,
            editor::toolbarInsertKeyframeAtTimeline,
            editor::toolbarEnterInsertColumnAtCursor,
            editor::toolbarEnterInsertSingleAtTimeline,
            editor::toolbarEnterInsertSingleAtCursor);

        wireDuplicateSubmenu(toolbar, hasSelected,
            () ->
            {
                if (editor.keyframeEditor != null)
                {
                    editor.keyframeEditor.view.toolbarEnterDuplicateAtCursor();
                }
            },
            () ->
            {
                if (editor.keyframeEditor != null)
                {
                    editor.keyframeEditor.view.toolbarEnterDuplicateAtPlayhead();
                }
            });
        bindLabel(toolbar, UIKeys.KEYFRAMES_KEYS_SELECT_COLUMN, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectColumn();
            }
        }, canModify);
    }

    private static void wireKeyframesOverlays(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;
        BooleanSupplier canPaste = () -> canModify.getAsBoolean() && keyframes.canToolbarPaste();

        bindShortcut(toolbar, Keys.PASTE, keyframes::toolbarPaste, canPaste);
        wirePasteSubmenu(toolbar, canPaste, keyframes::toolbarEnterPasteAtTimeline, keyframes::toolbarEnterPasteAtCursor);
        bindShortcut(toolbar, Keys.PRESETS, keyframes::toolbarOpenPresets, canModify);
        wireKeyframesEditTrack(keyframes, toolbar);
    }

    private static void wireKeyframesOverlays(UIReplaysEditor editor)
    {
        TimelineToolbar toolbar = editor.toolbar;
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canModify = () -> hasEditor.getAsBoolean()
            && editor.keyframeEditor.view.isModifyingKeyframes();
        BooleanSupplier canPaste = () ->
        {
            if (editor.keyframeEditor == null)
            {
                return false;
            }

            return editor.keyframeEditor.view.isModifyingKeyframes()
                && editor.keyframeEditor.view.canToolbarPaste();
        };

        bindShortcut(toolbar, Keys.PASTE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarPaste();
            }
        }, canPaste);
        wirePasteSubmenu(toolbar, canPaste,
            () ->
            {
                if (editor.keyframeEditor != null)
                {
                    editor.keyframeEditor.view.toolbarEnterPasteAtTimeline();
                }
            },
            () ->
            {
                if (editor.keyframeEditor != null)
                {
                    editor.keyframeEditor.view.toolbarEnterPasteAtCursor();
                }
            });
        bindShortcut(toolbar, Keys.PRESETS, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarOpenPresets();
            }
        }, canModify);
    }

    private static void wireKeyframesEditTrack(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        bindLabel(toolbar, UIKeys.TIMELINE_TOOLBAR_EDIT_TRACK, keyframes::toolbarEditTrack,
            keyframes::canToolbarEditTrack);
    }

    private static void wireKeyframesEditTrack(UIReplaysEditor editor)
    {
        TimelineToolbar toolbar = editor.toolbar;
        BooleanSupplier canEdit = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.canToolbarEditTrack();

        bindLabel(toolbar, UIKeys.TIMELINE_TOOLBAR_EDIT_TRACK, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarEditTrack();
            }
        }, canEdit);
    }

    private static void wireTracksOverlays(UIReplaysEditor editor)
    {
        TimelineToolbar toolbar = editor.toolbar;
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canRename = () -> hasEditor.getAsBoolean()
            && TimelineTrackEligibility.hasRenameableTrack(editor.keyframeEditor.view);

        bindLabel(toolbar, UIKeys.FILM_REPLAY_RENAME_SHEET, editor::toolbarRenameSheet, canRename);
        bindLabel(toolbar, UIKeys.FILM_REPLAY_FILTER_SHEETS, editor::toolbarFilterSheets, hasEditor);
        bindLabel(toolbar, UIKeys.FILM_REPLAY_CONTEXT_ANIMATION_TO_KEYFRAMES, editor::toolbarAnimationToKeyframes,
            () -> TimelineTrackEligibility.hasAnimationToPoseTrack(editor));
        bindLabel(toolbar, UIKeys.FILM_REPLAY_CONTEXT_POSE_TO_LIMBS, editor::toolbarPoseToLimbs,
            () -> TimelineTrackEligibility.hasPoseToLimbsTrack(editor));
    }

    private static void wireActorSection(UIReplaysEditor editor)
    {
        TimelineToolbar toolbar = editor.toolbar;
        UIFilmPanel filmPanel = editor.getFilmPanel();
        BooleanSupplier viewportAvailable = () -> TimelineViewportEligibility.isViewportAvailable(filmPanel);
        BooleanSupplier canAdd = viewportAvailable;
        BooleanSupplier canMove = () -> viewportAvailable.getAsBoolean()
            && TimelineViewportEligibility.canMoveReplay(editor);
        Supplier<IKey> viewportHiddenReason = () -> viewportAvailable.getAsBoolean()
            ? null
            : UIKeys.TIMELINE_TOOLBAR_DISABLED_VIEWPORT_HIDDEN;

        bindActorAction(toolbar, UIKeys.TIMELINE_TOOLBAR_ADD_REPLAY_AT_CURSOR, editor::toolbarAddReplayAtViewport,
            canAdd, viewportHiddenReason);
        bindActorAction(toolbar, UIKeys.FILM_CONTROLLER_KEYS_MOVE_REPLAY_TO_CURSOR, editor::toolbarMoveReplayAtViewport,
            canMove, viewportHiddenReason);
    }

    private static void bindActorAction(TimelineToolbar toolbar, IKey label, Runnable runnable,
        BooleanSupplier enabled, Supplier<IKey> disabledReason)
    {
        ToolbarItem item = findLabelInSections(toolbar.getSections(), label);

        if (item == null)
        {
            return;
        }

        item.run(runnable);
        item.enabledIf(enabled);
        item.disabledReason(disabledReason);
    }

    /* Keyframe timeline instant actions */

    private static void wireKeyframesInstant(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_ALL, () -> keyframes.getGraph().selectAll(), canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_MAXIMIZE, keyframes::resetView, canModify);
    }

    private static void wireKeyframesInstant(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canModify = () -> hasEditor.getAsBoolean()
            && editor.keyframeEditor.view.isModifyingKeyframes();

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_ALL, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.getGraph().selectAll();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_MAXIMIZE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.resetView();
            }
        }, hasEditor);
    }

    private static void wireKeyframesEditSelection(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean() && keyframes.hasSelectedKeyframes();

        bindShortcut(toolbar, Keys.COPY, keyframes::toolbarCopy, canModify);
        bindShortcut(toolbar, Keys.CUT, keyframes::toolbarCut, hasSelected);
        bindShortcut(toolbar, Keys.DELETE, keyframes::toolbarRemoveSelected, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ROUND, keyframes::toolbarRoundSelectedTicks, hasSelected);
    }

    private static void wireKeyframesEditSelection(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canModify = () -> hasEditor.getAsBoolean()
            && editor.keyframeEditor.view.isModifyingKeyframes();
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean()
            && editor.keyframeEditor.view.hasSelectedKeyframes();

        bindShortcut(toolbar, Keys.COPY, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarCopy();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.CUT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarCut();
            }
        }, hasSelected);
        bindShortcut(toolbar, Keys.DELETE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarRemoveSelected();
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ROUND, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarRoundSelectedTicks();
            }
        }, hasSelected);
    }

    private static void wireKeyframesSelect(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_LEFT, keyframes::toolbarSelectLeft, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_RIGHT, keyframes::toolbarSelectRight, canModify);
        wireSelectSameSubmenu(toolbar, canModify, keyframes::toolbarSelectSame, keyframes::toolbarSelectSameCurrentTrack);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_PREV, keyframes::toolbarSelectPrevKeyframe, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_NEXT, keyframes::toolbarSelectNextKeyframe, canModify);
    }

    private static void wireSelectSameSubmenu(TimelineToolbar toolbar, BooleanSupplier canModify,
        Runnable selectSameAll, Runnable selectSameTrack)
    {
        ToolbarItem parent = findLabelInSections(toolbar.getSections(), UIKeys.KEYFRAMES_KEYS_SELECT_SAME);

        if (parent == null)
        {
            return;
        }

        parent.run(selectSameAll);
        parent.enabledIf(canModify);

        bindInsertChild(parent, UIKeys.KEYFRAMES_KEYS_SELECT_SAME_ALL, selectSameAll, canModify);
        bindInsertChild(parent, UIKeys.KEYFRAMES_KEYS_SELECT_SAME_TRACK, selectSameTrack, canModify);
    }

    private static void wireKeyframesSelect(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.isModifyingKeyframes();

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_LEFT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectLeft();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_RIGHT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectRight();
            }
        }, canModify);
        wireSelectSameSubmenu(toolbar, canModify, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectSame();
            }
        }, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectSameCurrentTrack();
            }
        });
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_PREV, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectPrevKeyframe();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_NEXT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectNextKeyframe();
            }
        }, canModify);
    }

    private static void wireKeyframesGraphSelection(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier hasSelected = () -> keyframes.isModifyingKeyframes() && keyframes.hasSelectedKeyframes();
        BooleanSupplier canSpread = keyframes::canSpreadSelectedKeyframes;
        BooleanSupplier canAdjust = keyframes::canAdjustSelectedValues;

        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_LINEAR,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.LINEAR), hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_BEZIER,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.BEZIER), hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_HERMITE,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.HERMITE), hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_STEP,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.STEP), hasSelected);
        bindShortcut(toolbar, Keys.KEYFRAMES_SPREAD, keyframes::toolbarSpreadKeyframes, canSpread);
        bindShortcut(toolbar, Keys.KEYFRAMES_ADJUST_VALUES, keyframes::toolbarShowAdjustValuesMenu, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT,
            () -> keyframes.toolbarAdjustValues(false), canAdjust);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT,
            () -> keyframes.toolbarAdjustValues(true), canAdjust);
        bindShortcut(toolbar, Keys.KEYFRAMES_SCALE_TIME, keyframes::toolbarScaleTime, () -> keyframes.isModifyingKeyframes());
        bindShortcut(toolbar, Keys.KEYFRAMES_STACK_KEYFRAMES, keyframes::toolbarStackKeyframes, hasSelected);
        bindShortcut(toolbar, Keys.KEYFRAMES_INTERP, keyframes::toolbarToggleInterpolation, hasSelected);
    }

    private static void wireKeyframesGraphSelection(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.isModifyingKeyframes();
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean()
            && editor.keyframeEditor.view.hasSelectedKeyframes();
        BooleanSupplier canSpread = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.canSpreadSelectedKeyframes();
        BooleanSupplier canAdjust = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.canAdjustSelectedValues();

        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_LINEAR, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.LINEAR);
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_BEZIER, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.BEZIER);
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_HERMITE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.HERMITE);
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_STEP, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.STEP);
            }
        }, hasSelected);
        bindShortcut(toolbar, Keys.KEYFRAMES_SPREAD, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSpreadKeyframes();
            }
        }, canSpread);
        bindShortcut(toolbar, Keys.KEYFRAMES_ADJUST_VALUES, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarShowAdjustValuesMenu();
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarAdjustValues(false);
            }
        }, canAdjust);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarAdjustValues(true);
            }
        }, canAdjust);
        bindShortcut(toolbar, Keys.KEYFRAMES_SCALE_TIME, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarScaleTime();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_STACK_KEYFRAMES, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarStackKeyframes();
            }
        }, hasSelected);
        bindShortcut(toolbar, Keys.KEYFRAMES_INTERP, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarToggleInterpolation();
            }
        }, hasSelected);
    }

    private static void wireTracksInstant(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK, () -> keyframes.editSheet(null),
            keyframes::isEditing);
    }

    private static void wireTracksInstant(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.editSheet(null);
            }
        }, () -> editor.keyframeEditor != null && editor.keyframeEditor.view.isEditing());
    }

    private static void wireScreenNodeEdit(UIScreenNodeEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasNodeSelection = editor::hasToolbarNodeSelection;
        BooleanSupplier hasClipboard = editor::hasToolbarClipboard;

        bindShortcut(toolbar, Keys.DELETE, editor::toolbarRemoveSelectedNodes, hasNodeSelection);
        bindShortcut(toolbar, Keys.COPY, editor::toolbarCopyNodes, hasNodeSelection);
        bindShortcut(toolbar, Keys.PASTE, editor::toolbarPasteNodes, hasClipboard);
    }

    private static void wireScreenNodeHistory(UIFilmPanel filmPanel, TimelineToolbar toolbar)
    {
        BooleanSupplier filmLoaded = () -> filmPanel.getData() != null;

        bindShortcut(toolbar, Keys.UNDO, filmPanel::undo, filmLoaded);
        bindShortcut(toolbar, Keys.REDO, filmPanel::redo, filmLoaded);
    }

    private static void wireScreenNodeAdd(UIScreenNodeEditor editor, TimelineToolbar toolbar)
    {
        ToolbarSection addSection = findSection(toolbar.getSections(), UIKeys.TIMELINE_TOOLBAR_ADD);

        if (addSection == null)
        {
            return;
        }

        addSection.items.clear();
        editor.populateToolbarAddItems(addSection.items);
    }

    /* Item lookup + binding helpers */

    private static void bindShortcut(TimelineToolbar toolbar, KeyCombo combo, Runnable runnable,
        BooleanSupplier enabled)
    {
        apply(findShortcutInSections(toolbar.getSections(), combo), runnable, enabled);
    }

    private static void bindLabel(TimelineToolbar toolbar, IKey label, Runnable runnable,
        BooleanSupplier enabled)
    {
        apply(findLabelInSections(toolbar.getSections(), label), runnable, enabled);
    }

    private static void apply(ToolbarItem item, Runnable runnable, BooleanSupplier enabled)
    {
        if (item == null)
        {
            return;
        }

        item.run(runnable);

        if (enabled != null)
        {
            item.enabledIf(enabled);
        }
    }

    private static ToolbarItem findShortcutInSections(List<ToolbarSection> sections, KeyCombo combo)
    {
        for (ToolbarSection section : sections)
        {
            ToolbarItem item = findShortcutInItems(section.items, combo);

            if (item != null)
            {
                return item;
            }
        }

        return null;
    }

    private static ToolbarItem findShortcutInItems(List<ToolbarItem> items, KeyCombo combo)
    {
        for (ToolbarItem item : items)
        {
            if (item.separator)
            {
                continue;
            }

            if (item.keyCombo == combo)
            {
                return item;
            }

            if (!item.children.isEmpty())
            {
                ToolbarItem nested = findShortcutInItems(item.children, combo);

                if (nested != null)
                {
                    return nested;
                }
            }
        }

        return null;
    }

    private static ToolbarSection findSection(List<ToolbarSection> sections, IKey label)
    {
        for (ToolbarSection section : sections)
        {
            if (section.label == label || labelsMatch(section.label, label))
            {
                return section;
            }
        }

        return null;
    }

    private static ToolbarItem findLabelInSections(List<ToolbarSection> sections, IKey label)
    {
        for (ToolbarSection section : sections)
        {
            ToolbarItem item = findLabelInItems(section.items, label);

            if (item != null)
            {
                return item;
            }
        }

        return null;
    }

    private static ToolbarItem findLabelInItems(List<ToolbarItem> items, IKey label)
    {
        for (ToolbarItem item : items)
        {
            if (item.separator)
            {
                continue;
            }

            if (item.label == label || labelsMatch(item.label, label))
            {
                return item;
            }

            if (!item.children.isEmpty())
            {
                ToolbarItem nested = findLabelInItems(item.children, label);

                if (nested != null)
                {
                    return nested;
                }
            }
        }

        return null;
    }

    private static boolean labelsMatch(IKey a, IKey b)
    {
        if (a == b)
        {
            return true;
        }

        if (a instanceof LangKey langA && b instanceof LangKey langB)
        {
            return langA.key != null && langA.key.equals(langB.key);
        }

        return false;
    }

    private TimelineToolbarWiring()
    {}
}
