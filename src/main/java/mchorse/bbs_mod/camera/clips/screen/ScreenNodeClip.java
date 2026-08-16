package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.forms.forms.shape.ValueShapeGraph;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

public class ScreenNodeClip extends CameraClip
{
    public final ValueShapeGraph graph = new ValueShapeGraph("graph", new ScreenNodeGraph());

    private final ScreenNodeEffect effect = new ScreenNodeEffect();

    public ScreenNodeClip()
    {
        this.add(this.graph);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        ScreenNodeEvaluator evaluator = new ScreenNodeEvaluator(this.graph.get());

        evaluator.computeEffect(t, factor, this.effect);

        ScreenNodeEffect.getEffects(context).add(this.effect);
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new ScreenNodeClip();
    }
}
