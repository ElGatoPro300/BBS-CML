package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.camera.clips.misc.BossBarState;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

public class UIBossBarRenderer
{
    private static final float REFERENCE_WIDTH = 1920F;
    private static final float REFERENCE_HEIGHT = 1080F;
    private static final int BASE_BAR_WIDTH = 182;
    private static final int BASE_BAR_HEIGHT = 5;
    private static final int TEXT_GAP = 2;
    private static final class_2960 BOSS_BAR_BACKGROUND = class_2960.method_60655("minecraft", "boss_bar/white_background");
    private static final class_2960 BOSS_BAR_PROGRESS = class_2960.method_60655("minecraft", "boss_bar/white_progress");

    public static void renderBossBars(class_4587 stack, Batcher2D batcher, List<BossBarState> bossBars, int originX, int originY, int width, int height)
    {
        if (bossBars == null || bossBars.isEmpty())
        {
            return;
        }

        float resolutionScale = getResolutionScale(width, height);

        for (BossBarState bossBar : bossBars)
        {
            renderBossBar(stack, batcher, bossBar, originX, originY, width, height, resolutionScale);
        }
    }

    public static void renderBossBar(class_4587 stack, Batcher2D batcher, BossBarState bossBar, int originX, int originY, int width, int height)
    {
        renderBossBar(stack, batcher, bossBar, originX, originY, width, height, getResolutionScale(width, height));
    }

    private static void renderBossBar(class_4587 stack, Batcher2D batcher, BossBarState bossBar, int originX, int originY, int width, int height, float resolutionScale)
    {
        float alpha = class_3532.method_15363(bossBar.alpha, 0F, 1F);

        if (alpha <= 0F)
        {
            return;
        }

        float zoom = Math.max(0.05F, bossBar.zoom);
        float widthFactor = Math.max(0.05F, bossBar.width);
        float heightFactor = Math.max(0.05F, bossBar.height);
        float scaleX = resolutionScale * zoom * widthFactor;
        float scaleY = resolutionScale * zoom * heightFactor;
        int displayWidth = Math.max(1, Math.round(BASE_BAR_WIDTH * scaleX));
        int displayHeight = Math.max(1, Math.round(BASE_BAR_HEIGHT * scaleY));
        int x = originX + Math.round(width / 2F + bossBar.x * resolutionScale - displayWidth / 2F);
        int anchorY = originY + Math.round(bossBar.y * resolutionScale);
        float progress = class_3532.method_15363(bossBar.progress, 0F, 1F);
        int progressWidth = class_3532.method_15386(progress * displayWidth);
        boolean hasText = bossBar.text != null && !bossBar.text.isEmpty();
        float textScale = Math.max(0.05F, bossBar.textSize * zoom * resolutionScale);
        int fontHeight = batcher.getFont().getHeight();
        int textBlockHeight = hasText ? Math.round(fontHeight * textScale) : 0;
        int textY = anchorY;
        int barY = anchorY + textBlockHeight + (hasText ? TEXT_GAP : 0);
        float blockCenterX = x + displayWidth / 2F;

        batcher.flush();
        stack.method_22903();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        class_332 context = batcher.getContext();

        setShaderColor(context, 1F, 1F, 1F, alpha);
        context.method_52706(BOSS_BAR_BACKGROUND, x, barY, displayWidth, displayHeight);

        if (progressWidth > 0)
        {
            int color = bossBar.color;

            setShaderColor(
                context,
                ((color >> 16) & 0xFF) / 255F,
                ((color >> 8) & 0xFF) / 255F,
                (color & 0xFF) / 255F,
                alpha
            );
            context.method_52706(BOSS_BAR_PROGRESS, x, barY, progressWidth, displayHeight);
        }

        if (hasText)
        {
            int textWidth = batcher.getFont().getWidth(bossBar.text);
            int textX = Math.round(blockCenterX - textWidth / 2F);
            int textColor = applyAlpha(bossBar.textColor, alpha);
            float textCenterX = textX + textWidth / 2F;
            float textCenterY = textY + fontHeight / 2F;

            setShaderColor(context, 1F, 1F, 1F, 1F);

            if (textScale != 1F)
            {
                stack.method_22903();
                stack.method_46416(textCenterX, textCenterY, 0F);
                stack.method_22905(textScale, textScale, 1F);
                stack.method_46416(-textCenterX, -textCenterY, 0F);
            }

            batcher.text(bossBar.text, textX, textY, textColor, true);

            if (textScale != 1F)
            {
                stack.method_22909();
            }
        }

        setShaderColor(context, 1F, 1F, 1F, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();

        stack.method_22909();
        batcher.flush();
    }

    private static void setShaderColor(class_332 context, float red, float green, float blue, float alpha)
    {
        context.method_51422(red, green, blue, alpha);
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    private static float getResolutionScale(int width, int height)
    {
        if (width <= 0 || height <= 0)
        {
            return 1F;
        }

        return Math.max(0.05F, Math.min(width / REFERENCE_WIDTH, height / REFERENCE_HEIGHT));
    }

    private static int applyAlpha(int color, float alpha)
    {
        int a = class_3532.method_15340(Math.round(class_3532.method_15363(alpha, 0F, 1F) * 255F), 0, 255);

        return (a << 24) | (color & 0x00FFFFFF);
    }
}
