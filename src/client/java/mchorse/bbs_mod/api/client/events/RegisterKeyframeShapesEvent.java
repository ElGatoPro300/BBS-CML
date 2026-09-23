package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.IKeyframeShapeRenderer;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;

import java.util.Map;

public class RegisterKeyframeShapesEvent
{
    private final Map<KeyframeShape, IKeyframeShapeRenderer> shapes;

    public RegisterKeyframeShapesEvent(Map<KeyframeShape, IKeyframeShapeRenderer> shapes)
    {
        this.shapes = shapes;
    }

    public void register(KeyframeShape shape, IKeyframeShapeRenderer renderer)
    {
        this.shapes.put(shape, renderer);
    }
}
