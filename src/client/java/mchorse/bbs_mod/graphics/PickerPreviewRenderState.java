package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.jspecify.annotations.Nullable;

public class PickerPreviewRenderState implements GuiElementRenderState
{
    /* UV1 carries the pick ID so differently highlighted previews can share a GUI batch. */
    private static final VertexFormat FORMAT = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RGB32_FLOAT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .addAttribute("UV0", GpuFormat.RG32_FLOAT)
        .addAttribute("UV1", GpuFormat.RG16_SINT)
        .build();

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipelineUtils.withUniforms(RenderPipelines.GUI_TEXTURED)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/picker_preview"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_preview"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_preview"))
            .withVertexBinding(0, FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build());

    private final TextureSetup textureSetup;
    private final Matrix3x2f matrix;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int target;
    private final int highlight;
    private final ScreenRectangle scissor;
    private final ScreenRectangle bounds;

    public PickerPreviewRenderState(TextureSetup textureSetup, Matrix3x2fc matrix, int x, int y, int width, int height, int target, int highlight, @Nullable ScreenRectangle scissor)
    {
        this.textureSetup = textureSetup;
        this.matrix = new Matrix3x2f(matrix);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.target = target;
        this.highlight = highlight;
        this.scissor = scissor;

        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(this.matrix);

        this.bounds = scissor == null ? bounds : bounds.intersection(scissor);
    }

    @Override
    public RenderPipeline pipeline()
    {
        return PIPELINE;
    }

    @Override
    public TextureSetup textureSetup()
    {
        return this.textureSetup;
    }

    @Override
    @Nullable
    public ScreenRectangle scissorArea()
    {
        return this.scissor;
    }

    @Override
    @Nullable
    public ScreenRectangle bounds()
    {
        return this.bounds;
    }

    @Override
    public void buildVertices(VertexConsumer vertices)
    {
        /* Picker targets use framebuffer coordinates, whose Y axis is opposite to GUI coordinates. */
        this.vertex(vertices, this.x, this.y, 0F, 1F);
        this.vertex(vertices, this.x, this.y + this.height, 0F, 0F);
        this.vertex(vertices, this.x + this.width, this.y + this.height, 1F, 0F);
        this.vertex(vertices, this.x + this.width, this.y, 1F, 1F);
    }

    private void vertex(VertexConsumer vertices, int x, int y, float u, float v)
    {
        vertices.addVertexWith2DPose(this.matrix, x, y)
            .setColor(this.highlight)
            .setUv(u, v)
            .setUv1(this.target & 0xFFFF, (this.target >>> 16) & 0xFF);
    }
}
