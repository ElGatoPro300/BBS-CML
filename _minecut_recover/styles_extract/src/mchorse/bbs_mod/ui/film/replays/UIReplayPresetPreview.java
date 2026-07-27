package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class UIReplayPresetPreview implements UICopyPasteController.IPresetPreview
{
    private final Supplier<Replay> replaySupplier;

    private final UIElement root;
    private final UIFormRenderer renderer;

    private final Map<String, CachedPreview> cachedPreviews = new LinkedHashMap<>();

    private Replay cachedReplay;
    private Form cachedSourceForm;
    private Form basePreviewForm;
    private CachedPreview activePreview;
    private float activePreviewStartTime = Float.NaN;
    private float lastAnimatedTime = Float.NaN;

    public UIReplayPresetPreview(Supplier<Replay> replaySupplier)
    {
        this.replaySupplier = replaySupplier;

        this.root = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                UIReplayPresetPreview.this.updateAnimation(context.getTickTransition());

                super.render(context);
            }
        };
        this.renderer = new UIFormRenderer();
        this.renderer.relative(this.root).full(this.root);
        this.renderer.grid = false;
        this.renderer.setDistance(17);
        this.renderer.setPosition(0F, 1F, 0F);
        this.renderer.setRotation(32F, 8F);
        this.root.add(this.renderer);

        this.reset();
    }

    @Override
    public UIElement createElement()
    {
        return this.root;
    }

    public UIReplayPresetPreview setPreviewDistance(int distance)
    {
        this.renderer.setDistance(distance);

        return this;
    }

    @Override
    public UICopyPasteController.IPresetPreview fork()
    {
        return new UIReplayPresetPreview(this.replaySupplier);
    }

    @Override
    public void preview(String presetId, MapType presetData)
    {
        this.refreshBaseState();

        if (this.basePreviewForm == null || presetData == null)
        {
            this.activePreview = null;
            this.activePreviewStartTime = Float.NaN;
            this.lastAnimatedTime = Float.NaN;
            this.renderer.form = this.basePreviewForm == null ? null : FormUtils.copy(this.basePreviewForm);

            return;
        }

        CachedPreview preview = this.cachedPreviews.get(presetId);

        if (preview == null)
        {
            Form previewForm = FormUtils.copy(this.basePreviewForm);

            if (previewForm != null)
            {
                preview = this.buildPreview(previewForm, presetData);
                this.cachePreview(presetId, preview);
            }
        }

        this.activePreview = preview;
        this.activePreviewStartTime = Float.NaN;
        this.lastAnimatedTime = Float.NaN;
        this.renderer.form = preview == null ? null : preview.form;
    }

    @Override
    public void reset()
    {
        this.refreshBaseState();
        this.activePreview = null;
        this.activePreviewStartTime = Float.NaN;
        this.lastAnimatedTime = Float.NaN;
        this.renderer.form = this.basePreviewForm == null ? null : FormUtils.copy(this.basePreviewForm);
    }

    private void refreshBaseState()
    {
        Replay replay = this.replaySupplier.get();
        Form source = replay == null ? null : replay.form.get();

        if (replay == this.cachedReplay && source == this.cachedSourceForm)
        {
            return;
        }

        this.cachedReplay = replay;
        this.cachedSourceForm = source;
        this.basePreviewForm = source == null ? null : FormUtils.copy(source);
        this.cachedPreviews.clear();
        this.activePreview = null;
        this.activePreviewStartTime = Float.NaN;
        this.lastAnimatedTime = Float.NaN;
    }

    private CachedPreview buildPreview(Form form, MapType presetData)
    {
        CachedPreview preview = new CachedPreview(form);
        Map<String, UIKeyframes.PastedKeyframes> keyframes = UIKeyframes.parseKeyframes(presetData);

        for (Map.Entry<String, UIKeyframes.PastedKeyframes> entry : keyframes.entrySet())
        {
            UIKeyframes.PastedKeyframes pasted = entry.getValue();

            if (pasted == null || pasted.keyframes.isEmpty())
            {
                continue;
            }

            int colon = entry.getKey().indexOf(':');
            String propertyPath = colon == -1 ? entry.getKey() : entry.getKey().substring(0, colon);
            String boneName = colon == -1 ? "" : entry.getKey().substring(colon + 1);

            BaseValueBasic property = FormUtils.getProperty(form, propertyPath);

            if (property == null)
            {
                continue;
            }

            KeyframeChannel channel = new KeyframeChannel("", pasted.factory);

            for (Object object : pasted.keyframes)
            {
                Keyframe sourceKeyframe = (Keyframe) object;
                int index = channel.insert(sourceKeyframe.getTick(), pasted.factory.copy(sourceKeyframe.getValue()));
                Keyframe inserted = channel.get(index);

                inserted.copy(sourceKeyframe);
                preview.duration = Math.max(preview.duration, sourceKeyframe.getTick() + sourceKeyframe.getDuration());
            }

            if (boneName.isEmpty())
            {
                preview.tracks.add(new PreviewTrack(property, "", channel));
            }
            else
            {
                if (property.get() instanceof Pose pose)
                {
                    preview.basePoses.putIfAbsent(property, pose.copy());
                }

                preview.tracks.add(new PreviewTrack(property, boneName, channel));
            }

            preview.animatedProperties.add(property);
        }

        return preview;
    }

    private void cachePreview(String presetId, CachedPreview preview)
    {
        if (preview == null)
        {
            return;
        }

        this.cachedPreviews.put(presetId, preview);

        while (this.cachedPreviews.size() > 32)
        {
            Iterator<String> iterator = this.cachedPreviews.keySet().iterator();

            if (!iterator.hasNext())
            {
                break;
            }

            this.cachedPreviews.remove(iterator.next());
        }
    }

    private void updateAnimation(float time)
    {
        if (this.activePreview == null)
        {
            return;
        }

        if (!Float.isNaN(this.lastAnimatedTime) && Math.abs(this.lastAnimatedTime - time) < 0.0001F)
        {
            return;
        }

        if (Float.isNaN(this.activePreviewStartTime))
        {
            this.activePreviewStartTime = time;
        }

        float elapsed = time - this.activePreviewStartTime;
        float localTick = elapsed;

        if (this.activePreview.duration > 0F)
        {
            localTick = elapsed % (this.activePreview.duration + 1F);
        }

        this.lastAnimatedTime = time;

        for (BaseValueBasic property : this.activePreview.animatedProperties)
        {
            property.setRuntimeValue(null);
        }

        this.activePreview.runtimePoses.clear();

        for (Map.Entry<BaseValueBasic, Pose> entry : this.activePreview.basePoses.entrySet())
        {
            Pose workingPose = this.activePreview.runtimePoses.computeIfAbsent(entry.getKey(), (__) -> new Pose());

            workingPose.copy(entry.getValue());
        }

        for (PreviewTrack track : this.activePreview.tracks)
        {
            Object value = track.channel.interpolate(localTick);

            if (value == null)
            {
                continue;
            }

            if (track.boneName.isEmpty())
            {
                track.property.setRuntimeValue(value);
            }
            else if (value instanceof Transform transform)
            {
                Pose pose = this.activePreview.runtimePoses.get(track.property);

                if (pose == null)
                {
                    continue;
                }

                PoseTransform target = pose.get(track.boneName);
                target.copy(transform);
            }
        }

        for (Map.Entry<BaseValueBasic, Pose> entry : this.activePreview.runtimePoses.entrySet())
        {
            entry.getKey().setRuntimeValue(entry.getValue());
        }
    }

    private static class PreviewTrack
    {
        public final BaseValueBasic property;
        public final String boneName;
        public final KeyframeChannel channel;

        public PreviewTrack(BaseValueBasic property, String boneName, KeyframeChannel channel)
        {
            this.property = property;
            this.boneName = boneName;
            this.channel = channel;
        }
    }

    private static class CachedPreview
    {
        public final Form form;
        public final List<PreviewTrack> tracks = new ArrayList<>();
        public final Set<BaseValueBasic> animatedProperties = new LinkedHashSet<>();
        public final Map<BaseValueBasic, Pose> basePoses = new LinkedHashMap<>();
        public final Map<BaseValueBasic, Pose> runtimePoses = new LinkedHashMap<>();
        public float duration;

        public CachedPreview(Form form)
        {
            this.form = form;
        }
    }
}
