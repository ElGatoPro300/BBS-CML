package mchorse.bbs_mod.client;

import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.items.StructurePickerAxis;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.items.StructurePickerRegionMerger;
import mchorse.bbs_mod.items.StructurePickerSelection;
import mchorse.bbs_mod.ui.items.UIStructurePickerPanel;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.client.renderer.Tesselator;

import org.lwjgl.opengl.GL11;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Structure picker world overlay: selection volumes that respect depth
 * (occluded by terrain), plus visual corner handles and scale gizmos.
 */
public class StructurePickerRenderer
{
    private static final float CORNER_HANDLE = 0.28F;
    private static final float GIZMO_AXIS_LENGTH = 2.15F;
    private static final float GIZMO_AXIS_HALF = 0.028F;
    private static final float GIZMO_KNOB = 0.20F;
    private static final float GIZMO_HUB = 0.10F;
    private static final double VOLUME_EXPAND = 0.005D;
    private static final float EDGE_ALPHA = 0.95F;
    private static final float FILL_ALPHA = 0.42F;

    public static void render(LevelRenderContext context)
    {
        if (!StructurePickerClient.isActive() && !UIStructurePickerPanel.isOpened())
        {
            return;
        }

        boolean brushPreview = StructurePickerClient.getMode() == StructurePickerMode.BRUSH
            && !StructurePickerClient.getBrushPreviewRegions().isEmpty();

        if (!StructurePickerClient.hasAnySelection() && !brushPreview && !StructurePickerScaleGizmo.isActive())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (context.poseStack() == null)
        {
            return;
        }

        Vec3 camera = mc.gameRenderer.mainCamera().position();

        GlStateManager._enableBlend(0);
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        /* Depth on: selection must not paint through buried blocks / walls. */
        GlStateManager._disableCull();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._depthFunc(GL11.GL_LEQUAL);

        PoseStack stack = context.poseStack();

        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);

        Set<BlockPos> blockPositions = new LinkedHashSet<>();

        if (StructurePickerClient.getMode().isPaintMode())
        {
            blockPositions.addAll(StructurePickerClient.getAllRegionBlocks());

            for (StructurePickerRegionMerger.MergedRegion merged : StructurePickerRegionMerger.merge(blockPositions))
            {
                StructurePickerRenderer.renderMergedBlockBox(stack, merged.min(), merged.max(), 1F, 1F, 0F);
            }
        }
        else
        {
            List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();

            for (int i = 0; i < regions.size(); i++)
            {
                StructurePickerClient.Region region = regions.get(i);
                float[] color = StructurePickerRenderer.resolveRegionColor(i);

                StructurePickerRenderer.renderRegionBox(stack, region.first(), region.second(), region.mode(), region.triangleFacing(), color[0], color[1], color[2]);
            }
        }

        if (StructurePickerClient.hasInProgress())
        {
            if (StructurePickerClient.isSubtractMode())
            {
                StructurePickerRenderer.renderRegionBox(stack, StructurePickerClient.getFirstCorner(), StructurePickerClient.getSecondCorner(), StructurePickerClient.getMode(), StructurePickerClient.getTriangleFacing(), 1F, 0.25F, 0.25F);
            }
            else
            {
                StructurePickerRenderer.renderRegionBox(stack, StructurePickerClient.getFirstCorner(), StructurePickerClient.getSecondCorner(), StructurePickerClient.getMode(), StructurePickerClient.getTriangleFacing(), 1F, 1F, 0F);
            }
        }

        StructurePickerRenderer.renderBrushPreview(stack);

        /* Corner handles + scale axes always on top so they stay readable. */
        GlStateManager._disableDepthTest();
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        StructurePickerRenderer.renderCornerGizmos(stack, camera);

        if (StructurePickerScaleGizmo.isActive())
        {
            StructurePickerRenderer.renderResizeGizmo(stack, StructurePickerScaleGizmo.getFreeCorner());
        }

        stack.popPose();

        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
        GlStateManager._enableCull();
        GlStateManager._disableBlend(0);
    }

    private static float[] resolveRegionColor(int regionIndex)
    {
        boolean selected = StructurePickerClient.isRegionSelected(regionIndex);
        boolean hovered = StructurePickerClient.getHoveredRegionIndex() == regionIndex;
        float r = 1F;
        float g = 1F;
        float b = selected ? 0F : 1F;

        if (hovered)
        {
            if (StructurePickerClient.getMode().isEraseMode())
            {
                r = r * 0.4F + 1F * 0.6F;
                g = g * 0.4F + 0.2F * 0.6F;
                b = b * 0.4F + 0.2F * 0.6F;
            }
            else
            {
                /* Soft yellow tint while looking at a region. */
                r = 1F;
                g = 1F;
                b = selected ? 0.12F : 0.45F;
            }
        }

        return new float[] {r, g, b};
    }

    private static void renderCornerGizmos(PoseStack stack, Vec3 camera)
    {
        /* BLOCK/SAME/BRUSH are paint-style; corner handles stay on AABB volume modes. */
        if (StructurePickerClient.getMode().isPaintMode() || StructurePickerClient.getMode().isEraseMode())
        {
            return;
        }

        int activeIndex = StructurePickerClient.getActiveRegionIndex();
        List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();

        if (activeIndex >= 0 && activeIndex < regions.size())
        {
            StructurePickerClient.Region region = regions.get(activeIndex);

            if (StructurePickerScaleGizmo.isScalableMode(region.mode()))
            {
                StructurePickerRenderer.renderSelectionCorners(stack, region.first(), region.second(), region.mode(), camera);
            }
        }

        if (StructurePickerClient.hasInProgress()
            && !StructurePickerClient.getMode().isSingleClick()
            && !StructurePickerClient.isSubtractMode())
        {
            StructurePickerRenderer.renderSelectionCorners(
                stack,
                StructurePickerClient.getFirstCorner(),
                StructurePickerClient.getSecondCorner(),
                StructurePickerClient.getMode(),
                camera
            );
        }
    }

    private static void renderBrushPreview(PoseStack stack)
    {
        if (StructurePickerClient.getMode() != StructurePickerMode.BRUSH)
        {
            return;
        }

        List<StructurePickerRegionMerger.MergedRegion> preview = StructurePickerClient.getBrushPreviewRegions();

        if (preview.isEmpty())
        {
            return;
        }

        float pulse = 0.5F + 0.5F * (0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() * 0.004D));
        float r = StructurePickerClient.isSubtractMode() ? 1F : 0.35F;
        float g = StructurePickerClient.isSubtractMode() ? 0.35F : 0.85F;
        float b = StructurePickerClient.isSubtractMode() ? 0.35F : 1F;
        float edge = 0.45F + 0.55F * pulse;
        float fill = 0.12F + 0.22F * pulse;

        for (StructurePickerRegionMerger.MergedRegion region : preview)
        {
            double sizeX = region.max().getX() - region.min().getX() + 1D;
            double sizeY = region.max().getY() - region.min().getY() + 1D;
            double sizeZ = region.max().getZ() - region.min().getZ() + 1D;
            double e = VOLUME_EXPAND;

            StructurePickerRenderer.renderVolumeFill(stack, region.min().getX() - e, region.min().getY() - e, region.min().getZ() - e, sizeX + e * 2D, sizeY + e * 2D, sizeZ + e * 2D, r, g, b, fill);
            StructurePickerRenderer.renderBoxEdges(stack, region.min().getX() - e, region.min().getY() - e, region.min().getZ() - e, sizeX + e * 2D, sizeY + e * 2D, sizeZ + e * 2D, r, g, b, edge);
        }
    }

    private static void renderRegionBox(PoseStack stack, BlockPos first, BlockPos second, StructurePickerMode mode, Direction triangleFacing, float r, float g, float b)
    {
        BlockPos adjusted = StructurePickerSelection.adjustSecond(first, second, mode);
        BlockPos min = StructurePickerSelection.min(first, adjusted);
        BlockPos max = StructurePickerSelection.max(first, adjusted);
        double sizeX = max.getX() - min.getX() + 1D;
        double sizeY = max.getY() - min.getY() + 1D;
        double sizeZ = max.getZ() - min.getZ() + 1D;

        if (mode.hasShapeOutline())
        {
            StructurePickerShapeOutline.render(stack, first, second, mode, triangleFacing, r, g, b, EDGE_ALPHA);
        }

        StructurePickerRenderer.renderExpandedVolume(stack, min.getX(), min.getY(), min.getZ(), sizeX, sizeY, sizeZ, r, g, b);
    }

    private static void renderMergedBlockBox(PoseStack stack, BlockPos min, BlockPos max, float r, float g, float b)
    {
        double sizeX = max.getX() - min.getX() + 1D;
        double sizeY = max.getY() - min.getY() + 1D;
        double sizeZ = max.getZ() - min.getZ() + 1D;

        StructurePickerRenderer.renderExpandedVolume(stack, min.getX(), min.getY(), min.getZ(), sizeX, sizeY, sizeZ, r, g, b);
    }

    private static void renderExpandedVolume(PoseStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        double e = VOLUME_EXPAND;

        StructurePickerRenderer.renderVolumeFill(stack, x - e, y - e, z - e, w + e * 2D, h + e * 2D, d + e * 2D, r, g, b, FILL_ALPHA);
        /* Draw edges immediately (not Draw.renderBox Iris queue) so depth test is honored. */
        StructurePickerRenderer.renderBoxEdges(stack, x - e, y - e, z - e, w + e * 2D, h + e * 2D, d + e * 2D, r, g, b, EDGE_ALPHA);
    }

    private static void renderVolumeFill(PoseStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        if (a <= 0.001F)
        {
            return;
        }

        BufferBuilder builder = Tesselator.getInstance().begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        stack.pushPose();
        stack.translate(x, y, z);
        Draw.fillBox(builder, stack, 0F, 0F, 0F, (float) w, (float) h, (float) d, r, g, b, a);
        stack.popPose();

        Draw.flush(builder, Draw.getPositionColorLayer());
    }

    private static void renderBoxEdges(PoseStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1F / 96F + (float) (Math.sqrt(w * w + h * h + d * d) / 2000D);
        BufferBuilder builder = Tesselator.getInstance().begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        stack.pushPose();
        stack.translate(x, y, z);

        Draw.fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, a);
        Draw.fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, a);
        Draw.fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, a);
        Draw.fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);

        Draw.fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, a);
        Draw.fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);
        Draw.fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, a);
        Draw.fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, a);

        Draw.fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, a);
        Draw.fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, a);
        Draw.fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, a);
        Draw.fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, a);

        stack.popPose();
        Draw.flush(builder, Draw.getPositionColorLayer());
    }

    private static void renderSelectionCorners(PoseStack stack, BlockPos first, BlockPos second, StructurePickerMode mode, Vec3 camera)
    {
        BlockPos adjusted = StructurePickerSelection.adjustSecond(first, second, mode);
        BlockPos min = StructurePickerSelection.min(first, adjusted);
        BlockPos max = StructurePickerSelection.max(first, adjusted);
        float pulse = 0.85F + 0.15F * (0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() * 0.004D));
        float hMin;
        float hMax;

        if (StructurePickerScaleGizmo.hasScalableSelection())
        {
            hMin = CORNER_HANDLE * StructurePickerScaleGizmo.getHandleVisualScale(min.getX(), min.getY(), min.getZ());
            hMax = CORNER_HANDLE * StructurePickerScaleGizmo.getHandleVisualScale(max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }
        else
        {
            hMin = CORNER_HANDLE * StructurePickerRenderer.handleScale(camera, min.getX(), min.getY(), min.getZ());
            hMax = CORNER_HANDLE * StructurePickerRenderer.handleScale(camera, max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }

        StructurePickerRenderer.renderCornerHandle(stack, min.getX(), min.getY(), min.getZ(), hMin, pulse);
        StructurePickerRenderer.renderCornerHandle(stack, max.getX() + 1, max.getY() + 1, max.getZ() + 1, hMax, pulse);
    }

    private static void renderCornerHandle(PoseStack stack, double x, double y, double z, float h, float alpha)
    {
        float rim = h * 1.18F;

        StructurePickerRenderer.renderVolumeFill(stack, x - rim * 0.5D, y - rim * 0.5D, z - rim * 0.5D, rim, rim, rim, 1F, 1F, 1F, alpha * 0.55F);
        StructurePickerRenderer.renderVolumeFill(stack, x - h * 0.5D, y - h * 0.5D, z - h * 0.5D, h, h, h, 1F, 1F, 1F, alpha);
    }

    private static void renderResizeGizmo(PoseStack stack, BlockPos freeCorner)
    {
        if (freeCorner == null)
        {
            return;
        }

        double ox = freeCorner.getX();
        double oy = freeCorner.getY();
        double oz = freeCorner.getZ();

        if (StructurePickerScaleGizmo.isUsingMaxCorner())
        {
            ox += 1D;
            oy += 1D;
            oz += 1D;
        }

        boolean positive = StructurePickerScaleGizmo.isScalePositive();
        float scale = StructurePickerScaleGizmo.getHandleVisualScale(ox, oy, oz);
        float len = (positive ? GIZMO_AXIS_LENGTH : -GIZMO_AXIS_LENGTH) * scale;
        float t = GIZMO_AXIS_HALF * scale;
        float knob = GIZMO_KNOB * scale;
        float hub = GIZMO_HUB * scale;
        StructurePickerAxis hover = StructurePickerScaleGizmo.getResizeDragAxis();

        StructurePickerRenderer.renderVolumeFill(stack, ox - hub * 0.5D, oy - hub * 0.5D, oz - hub * 0.5D, hub, hub, hub, 1F, 1F, 1F, 1F);
        StructurePickerRenderer.renderScaleAxis(stack, ox, oy, oz, StructurePickerAxis.X, len, t, knob, 1F, 0.22F, 0.22F, hover == StructurePickerAxis.X);
        StructurePickerRenderer.renderScaleAxis(stack, ox, oy, oz, StructurePickerAxis.Y, len, t, knob, 0.22F, 1F, 0.22F, hover == StructurePickerAxis.Y);
        StructurePickerRenderer.renderScaleAxis(stack, ox, oy, oz, StructurePickerAxis.Z, len, t, knob, 0.25F, 0.5F, 1F, hover == StructurePickerAxis.Z);
    }

    private static void renderScaleAxis(PoseStack stack, double ox, double oy, double oz, StructurePickerAxis axis, float len, float t, float knob, float r, float g, float b, boolean highlight)
    {
        StructurePickerRenderer.renderAxisArm(stack, ox, oy, oz, axis, len, t, r, g, b, highlight);

        double ex = ox;
        double ey = oy;
        double ez = oz;

        if (axis == StructurePickerAxis.X)
        {
            ex += len;
        }
        else if (axis == StructurePickerAxis.Y)
        {
            ey += len;
        }
        else
        {
            ez += len;
        }

        float a = highlight ? 1F : 0.95F;
        float s = knob * (highlight ? 1.25F : 1F);
        float rim = s * 1.2F;

        StructurePickerRenderer.renderVolumeFill(stack, ex - rim * 0.5D, ey - rim * 0.5D, ez - rim * 0.5D, rim, rim, rim, 1F, 1F, 1F, a * 0.45F);
        StructurePickerRenderer.renderVolumeFill(stack, ex - s * 0.5D, ey - s * 0.5D, ez - s * 0.5D, s, s, s, r, g, b, a);
    }

    private static void renderAxisArm(PoseStack stack, double ox, double oy, double oz, StructurePickerAxis axis, float len, float t, float r, float g, float b, boolean highlight)
    {
        float thick = t * (highlight ? 1.45F : 1F);
        float a = highlight ? 1F : 0.92F;
        double w = Math.abs(len);
        double h = Math.abs(len);
        double d = Math.abs(len);

        if (axis == StructurePickerAxis.X)
        {
            double x0 = len >= 0F ? ox : ox + len;

            StructurePickerRenderer.renderVolumeFill(stack, x0, oy - thick, oz - thick, w, thick * 2F, thick * 2F, r, g, b, a);
        }
        else if (axis == StructurePickerAxis.Y)
        {
            double y0 = len >= 0F ? oy : oy + len;

            StructurePickerRenderer.renderVolumeFill(stack, ox - thick, y0, oz - thick, thick * 2F, h, thick * 2F, r, g, b, a);
        }
        else
        {
            double z0 = len >= 0F ? oz : oz + len;

            StructurePickerRenderer.renderVolumeFill(stack, ox - thick, oy - thick, z0, thick * 2F, thick * 2F, d, r, g, b, a);
        }
    }

    private static float handleScale(Vec3 camera, double x, double y, double z)
    {
        double dx = camera.x - x;
        double dy = camera.y - y;
        double dz = camera.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        return (float) Math.max(0.75D, Math.min(3.5D, dist * 0.08D));
    }
}
