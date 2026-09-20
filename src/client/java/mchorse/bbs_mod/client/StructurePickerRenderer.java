package mchorse.bbs_mod.client;

import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.items.StructurePickerRegionMerger;
import mchorse.bbs_mod.items.StructurePickerSelection;
import mchorse.bbs_mod.ui.items.UIStructurePickerPanel;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Structure picker world overlay: yellow selection volumes that respect depth
 * (occluded by terrain), plus visual corner handles on volume selections.
 */
public class StructurePickerRenderer
{
    private static final float CORNER_HANDLE = 0.28F;
    private static final double VOLUME_EXPAND = 0.005D;
    private static final float EDGE_ALPHA = 0.95F;
    private static final float FILL_ALPHA = 0.42F;

    public static void render(WorldRenderContext context)
    {
        if (!StructurePickerClient.isActive() && !UIStructurePickerPanel.isOpened())
        {
            return;
        }

        if (!StructurePickerClient.hasAnySelection())
        {
            return;
        }

        if (context.matrixStack() == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        /* Depth on: selection must not paint through buried blocks / walls. */
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        MatrixStack stack = context.matrixStack();

        stack.push();
        stack.translate(-camera.x, -camera.y, -camera.z);

        Set<BlockPos> blockPositions = new LinkedHashSet<>();

        if (StructurePickerClient.getMode() == StructurePickerMode.BLOCK)
        {
            blockPositions.addAll(StructurePickerClient.getAllRegionBlocks());

            for (StructurePickerRegionMerger.MergedRegion merged : StructurePickerRegionMerger.merge(blockPositions))
            {
                StructurePickerRenderer.renderMergedBlockBox(stack, merged.min(), merged.max(), 1F, 1F, 0F);
            }
        }
        else
        {
            for (StructurePickerClient.Region region : StructurePickerClient.getRegions())
            {
                StructurePickerRenderer.renderRegionBox(stack, region.first(), region.second(), region.mode(), region.triangleFacing(), 1F, 1F, 0F);
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

        /* Corner handles always on top so they stay readable. */
        RenderSystem.disableDepthTest();
        StructurePickerRenderer.renderCornerGizmos(stack, camera);
        RenderSystem.enableDepthTest();

        stack.pop();

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderCornerGizmos(MatrixStack stack, Vec3d camera)
    {
        /* BLOCK is paint-style (many tiny cells); corner handles stay on AABB volume modes. */
        if (StructurePickerClient.getMode() == StructurePickerMode.BLOCK)
        {
            return;
        }

        for (StructurePickerClient.Region region : StructurePickerClient.getRegions())
        {
            if (region.mode() != StructurePickerMode.BLOCK)
            {
                StructurePickerRenderer.renderSelectionCorners(stack, region.first(), region.second(), region.mode(), camera);
            }
        }

        if (StructurePickerClient.hasInProgress()
            && StructurePickerClient.getMode() != StructurePickerMode.BLOCK
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

    private static void renderRegionBox(MatrixStack stack, BlockPos first, BlockPos second, StructurePickerMode mode, Direction triangleFacing, float r, float g, float b)
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

    private static void renderMergedBlockBox(MatrixStack stack, BlockPos min, BlockPos max, float r, float g, float b)
    {
        double sizeX = max.getX() - min.getX() + 1D;
        double sizeY = max.getY() - min.getY() + 1D;
        double sizeZ = max.getZ() - min.getZ() + 1D;

        StructurePickerRenderer.renderExpandedVolume(stack, min.getX(), min.getY(), min.getZ(), sizeX, sizeY, sizeZ, r, g, b);
    }

    private static void renderExpandedVolume(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        double e = VOLUME_EXPAND;

        StructurePickerRenderer.renderVolumeFill(stack, x - e, y - e, z - e, w + e * 2D, h + e * 2D, d + e * 2D, r, g, b, FILL_ALPHA);
        /* Draw edges immediately (not Draw.renderBox Iris queue) so depth test is honored. */
        StructurePickerRenderer.renderBoxEdges(stack, x - e, y - e, z - e, w + e * 2D, h + e * 2D, d + e * 2D, r, g, b, EDGE_ALPHA);
    }

    private static void renderVolumeFill(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        if (a <= 0.001F)
        {
            return;
        }

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        stack.push();
        stack.translate(x, y, z);
        Draw.fillBox(builder, stack, 0F, 0F, 0F, (float) w, (float) h, (float) d, r, g, b, a);
        stack.pop();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void renderBoxEdges(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1F / 96F + (float) (Math.sqrt(w * w + h * h + d * d) / 2000D);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        stack.push();
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

        stack.pop();
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void renderSelectionCorners(MatrixStack stack, BlockPos first, BlockPos second, StructurePickerMode mode, Vec3d camera)
    {
        BlockPos adjusted = StructurePickerSelection.adjustSecond(first, second, mode);
        BlockPos min = StructurePickerSelection.min(first, adjusted);
        BlockPos max = StructurePickerSelection.max(first, adjusted);
        float pulse = 0.85F + 0.15F * (0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() * 0.004D));
        float hMin = CORNER_HANDLE * StructurePickerRenderer.handleScale(camera, min.getX(), min.getY(), min.getZ());
        float hMax = CORNER_HANDLE * StructurePickerRenderer.handleScale(camera, max.getX() + 1, max.getY() + 1, max.getZ() + 1);

        StructurePickerRenderer.renderCornerHandle(stack, min.getX(), min.getY(), min.getZ(), hMin, pulse);
        StructurePickerRenderer.renderCornerHandle(stack, max.getX() + 1, max.getY() + 1, max.getZ() + 1, hMax, pulse);
    }

    private static void renderCornerHandle(MatrixStack stack, double x, double y, double z, float h, float alpha)
    {
        float rim = h * 1.18F;

        StructurePickerRenderer.renderVolumeFill(stack, x - rim * 0.5D, y - rim * 0.5D, z - rim * 0.5D, rim, rim, rim, 1F, 1F, 1F, alpha * 0.55F);
        StructurePickerRenderer.renderVolumeFill(stack, x - h * 0.5D, y - h * 0.5D, z - h * 0.5D, h, h, h, 1F, 1F, 1F, alpha);
    }

    private static float handleScale(Vec3d camera, double x, double y, double z)
    {
        double dx = camera.x - x;
        double dy = camera.y - y;
        double dz = camera.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        return (float) Math.max(0.75D, Math.min(3.5D, dist * 0.08D));
    }
}
