package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import org.joml.Matrix3x2fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class DiamondKeyframeShapeRenderer implements IKeyframeShapeRenderer
{
    @Override
    public IKey getLabel()
    {
        return UIKeys.KEYFRAMES_SHAPES_DIAMOND;
    }

    @Override
    public Icon getIcon()
    {
        return Icons.DIAMOND;
    }

    @Override
    public void renderKeyframe(UIContext uiContext, VertexConsumer builder, Matrix3x2fc matrix, int x, int y, int offset, int c)
    {
        float fOffset = offset * 1.3F;

        builder.addVertexWith2DPose(matrix, x, y - fOffset).setColor(c);
        builder.addVertexWith2DPose(matrix, x - fOffset, y).setColor(c);
        builder.addVertexWith2DPose(matrix, x, y + fOffset).setColor(c);
        builder.addVertexWith2DPose(matrix, x + fOffset, y).setColor(c);
    }
}