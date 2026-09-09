package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import org.joml.Matrix3x2fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class SquareKeyframeShapeRenderer implements IKeyframeShapeRenderer
{
    @Override
    public IKey getLabel()
    {
        return UIKeys.KEYFRAMES_SHAPES_SQUARE;
    }

    @Override
    public Icon getIcon()
    {
        return Icons.SQUARE;
    }

    @Override
    public void renderKeyframe(UIContext uiContext, VertexConsumer builder, Matrix3x2fc matrix4f, int x, int y, int offset, int c)
    {
        uiContext.batcher.fillRect(builder, matrix4f, x - offset, y - offset, offset * 2, offset * 2, c, c, c, c);
    }
}
