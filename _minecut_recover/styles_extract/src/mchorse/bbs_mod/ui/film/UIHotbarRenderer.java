package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.camera.clips.misc.HotbarState;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;
import java.util.Random;

public class UIHotbarRenderer
{
    private static final float REFERENCE_WIDTH = 1920F;
    private static final float REFERENCE_HEIGHT = 1080F;
    private static final int HUD_GREEN = 8453920;
    private static final int BAR_ICON_Y = -17;
    private static final int EXPERIENCE_BAR_Y = -7;
    private static final int EXPERIENCE_TEXT_Y = -13;
    private static final float SCALE_PIVOT_X = 91F;
    private static final float SCALE_PIVOT_Y = 0.5F;
    private static final int MAX_HEALTH_ROWS = 60;
    private static final float MAX_HEALTH_CONTAINER = MAX_HEALTH_ROWS * 10F * 2F;
    private static final class_2960 HOTBAR = class_2960.method_60655("minecraft", "hud/hotbar");
    private static final class_2960 HOTBAR_SELECTION = class_2960.method_60655("minecraft", "hud/hotbar_selection");
    private static final class_2960 HOTBAR_OFFHAND_LEFT = class_2960.method_60655("minecraft", "hud/hotbar_offhand_left");
    private static final class_2960 HEART_CONTAINER = class_2960.method_60655("minecraft", "hud/heart/container");
    private static final class_2960 HEART_HARDCORE_CONTAINER = class_2960.method_60655("minecraft", "hud/heart/container_hardcore");
    private static final class_2960[][] HEART_HALVES = {
        {class_2960.method_60655("minecraft", "hud/heart/half"), class_2960.method_60655("minecraft", "hud/heart/hardcore_half")},
        {class_2960.method_60655("minecraft", "hud/heart/poisoned_half"), class_2960.method_60655("minecraft", "hud/heart/poisoned_hardcore_half")},
        {class_2960.method_60655("minecraft", "hud/heart/withered_half"), class_2960.method_60655("minecraft", "hud/heart/withered_hardcore_half")},
        {class_2960.method_60655("minecraft", "hud/heart/absorbing_half"), class_2960.method_60655("minecraft", "hud/heart/absorbing_hardcore_half")},
        {class_2960.method_60655("minecraft", "hud/heart/frozen_half"), class_2960.method_60655("minecraft", "hud/heart/frozen_hardcore_half")}
    };
    private static final class_2960[][] HEART_FULLS = {
        {class_2960.method_60655("minecraft", "hud/heart/full"), class_2960.method_60655("minecraft", "hud/heart/hardcore_full")},
        {class_2960.method_60655("minecraft", "hud/heart/poisoned_full"), class_2960.method_60655("minecraft", "hud/heart/poisoned_hardcore_full")},
        {class_2960.method_60655("minecraft", "hud/heart/withered_full"), class_2960.method_60655("minecraft", "hud/heart/withered_hardcore_full")},
        {class_2960.method_60655("minecraft", "hud/heart/absorbing_full"), class_2960.method_60655("minecraft", "hud/heart/absorbing_hardcore_full")},
        {class_2960.method_60655("minecraft", "hud/heart/frozen_full"), class_2960.method_60655("minecraft", "hud/heart/frozen_hardcore_full")}
    };
    private static final class_2960 ARMOR_EMPTY = class_2960.method_60655("minecraft", "hud/armor_empty");
    private static final class_2960 ARMOR_FULL = class_2960.method_60655("minecraft", "hud/armor_full");
    private static final class_2960 ARMOR_HALF = class_2960.method_60655("minecraft", "hud/armor_half");
    private static final class_2960 FOOD_EMPTY = class_2960.method_60655("minecraft", "hud/food_empty");
    private static final class_2960 FOOD_FULL = class_2960.method_60655("minecraft", "hud/food_full");
    private static final class_2960 FOOD_HALF = class_2960.method_60655("minecraft", "hud/food_half");
    private static final class_2960 FOOD_EMPTY_HUNGER = class_2960.method_60655("minecraft", "hud/food_empty_hunger");
    private static final class_2960 FOOD_FULL_HUNGER = class_2960.method_60655("minecraft", "hud/food_full_hunger");
    private static final class_2960 FOOD_HALF_HUNGER = class_2960.method_60655("minecraft", "hud/food_half_hunger");
    private static final class_2960 AIR = class_2960.method_60655("minecraft", "hud/air");
    private static final class_2960 AIR_BURSTING = class_2960.method_60655("minecraft", "hud/air_bursting");
    private static final class_2960 EXPERIENCE_BAR_BACKGROUND_TEXTURE = class_2960.method_60655("minecraft", "textures/gui/sprites/hud/experience_bar_background.png");
    private static final class_2960 EXPERIENCE_BAR_PROGRESS_TEXTURE = class_2960.method_60655("minecraft", "textures/gui/sprites/hud/experience_bar_progress.png");
    private static boolean wasHeartRegenerationEnabled;
    private static long heartRegenerationStartTick;

    public static void renderHotbars(class_4587 stack, Batcher2D batcher, List<HotbarState> hotbars)
    {
        if (hotbars == null || hotbars.isEmpty())
        {
            return;
        }

        class_310 mc = class_310.method_1551();
        int width = mc.method_22683().method_4486();
        int height = mc.method_22683().method_4502();

        renderHotbars(stack, batcher, hotbars, 0, 0, width, height);
    }

    public static void renderHotbars(class_4587 stack, Batcher2D batcher, List<HotbarState> hotbars, int originX, int originY, int width, int height)
    {
        if (hotbars == null || hotbars.isEmpty())
        {
            return;
        }

        for (HotbarState hotbar : hotbars)
        {
            renderHotbar(stack, batcher, hotbar, originX, originY, width, height);
        }
    }

    public static void renderHotbar(class_4587 stack, Batcher2D batcher, HotbarState hotbar, int originX, int originY, int width, int height)
    {
        float alpha = class_3532.method_15363(hotbar.alpha, 0F, 1F);

        if (alpha <= 0F)
        {
            return;
        }

        float resolutionScale = getResolutionScale(width, height);
        float scale = Math.max(0.05F, hotbar.scale) * resolutionScale;
        int hotbarWidth = 182;
        int x = originX + Math.round(width / 2F + hotbar.x * resolutionScale - hotbarWidth / 2F);
        int y = originY + Math.round(height - (22 + 9) * resolutionScale + hotbar.y * resolutionScale);

        batcher.flush();
        stack.method_22903();
        stack.method_46416(x, y, 0);
        stack.method_46416(SCALE_PIVOT_X, SCALE_PIVOT_Y, 0F);
        stack.method_22905(scale, scale, 1F);
        stack.method_46416(-SCALE_PIVOT_X, -SCALE_PIVOT_Y, 0F);

        /* HUD layers must ignore world depth to avoid bottom clipping against terrain. */
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        batcher.getContext().method_51422(1F, 1F, 1F, alpha);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);

        batcher.getContext().method_52706(HOTBAR, 0, 0, 182, 22);

        boolean hasOffhandItem = hotbar.offhandItem != null && !hotbar.offhandItem.method_7960();

        if (hasOffhandItem)
        {
            batcher.getContext().method_52706(HOTBAR_OFFHAND_LEFT, -29, -1, 29, 24);
        }

        int selectedSlot = class_3532.method_15340(hotbar.selectedSlot, 0, 8);
        batcher.getContext().method_52706(HOTBAR_SELECTION, selectedSlot * 20 - 1, -1, 24, 23);

        int barsY = BAR_ICON_Y;
        int heartType = class_3532.method_15340(hotbar.heartType, HotbarState.HEART_NORMAL, HotbarState.HEART_FROZEN);
        int hardcore = hotbar.hardcore ? 1 : 0;
        class_2960 container = hotbar.hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
        class_2960 heartHalf = HEART_HALVES[heartType][hardcore];
        class_2960 heartFull = HEART_FULLS[heartType][hardcore];
        class_2960 absorptionHalf = HEART_HALVES[HotbarState.HEART_ABSORBING][hardcore];
        class_2960 absorptionFull = HEART_FULLS[HotbarState.HEART_ABSORBING][hardcore];
        int healthSlots = class_3532.method_15386(class_3532.method_15363(hotbar.healthContainer, 0F, MAX_HEALTH_CONTAINER) / 2F);
        healthSlots = class_3532.method_15340(healthSlots, 0, MAX_HEALTH_ROWS * 10);
        int healthRows = Math.max(1, Math.min(MAX_HEALTH_ROWS, (healthSlots + 9) / 10));
        int absorptionSlots = class_3532.method_15386(class_3532.method_15363(hotbar.absorptionContainer, 0F, MAX_HEALTH_CONTAINER) / 2F);
        absorptionSlots = class_3532.method_15340(absorptionSlots, 0, MAX_HEALTH_ROWS * 10);
        int absorptionRows = absorptionSlots <= 0 ? 0 : Math.max(1, Math.min(MAX_HEALTH_ROWS, (absorptionSlots + 9) / 10));
        Random heartShakeRandom = hotbar.health <= 4F ? new Random(thisTickSeed()) : null;
        Random hungerShakeRandom = hotbar.hunger <= 6F ? new Random(thisTickSeed() + 17L) : null;
        int regenerationHeartIndex = -1;
        long hudTick = currentHudTick();

        if (hotbar.heartRegeneration && healthSlots > 0 && hotbar.health > 0F)
        {
            if (!wasHeartRegenerationEnabled)
            {
                heartRegenerationStartTick = hudTick;
            }

            wasHeartRegenerationEnabled = true;

            int cycleLength = healthSlots + 5; /* Vanilla-like pacing: one sweep plus idle tail. */
            int cycleIndex = cycleLength <= 0 ? 0 : (int) Math.floorMod(hudTick - heartRegenerationStartTick, cycleLength);

            regenerationHeartIndex = cycleIndex < healthSlots ? cycleIndex : -1;
        }
        else if (wasHeartRegenerationEnabled)
        {
            wasHeartRegenerationEnabled = false;
        }

        renderBar(batcher, hotbar.health, container, heartHalf, heartFull, 0, barsY, healthSlots, heartShakeRandom, regenerationHeartIndex);
        if (absorptionSlots > 0)
        {
            renderBar(batcher, hotbar.absorption, container, absorptionHalf, absorptionFull, 0, barsY - healthRows * 10, absorptionSlots, heartShakeRandom, -1);
        }
        if (hotbar.armor > 0F)
        {
            renderBar(batcher, hotbar.armor, ARMOR_EMPTY, ARMOR_HALF, ARMOR_FULL, 0, barsY - (healthRows + absorptionRows) * 10, 10, null, -1);
        }
        class_2960 foodEmpty = hotbar.hungerEffect ? FOOD_EMPTY_HUNGER : FOOD_EMPTY;
        class_2960 foodHalf = hotbar.hungerEffect ? FOOD_HALF_HUNGER : FOOD_HALF;
        class_2960 foodFull = hotbar.hungerEffect ? FOOD_FULL_HUNGER : FOOD_FULL;
        renderBarReverse(batcher, hotbar.hunger, foodEmpty, foodHalf, foodFull, 182 - 9, barsY, 10, hungerShakeRandom);
        renderAirBar(batcher, hotbar.air, 182 - 9, barsY - 10);

        float experience = class_3532.method_15363(hotbar.experience, 0F, 1F);
        int xpPixels = class_3532.method_15386(experience * 182F);
        batcher.getContext().method_25290(EXPERIENCE_BAR_BACKGROUND_TEXTURE, 0, EXPERIENCE_BAR_Y, 0F, 0F, 182, 5, 182, 5);
        if (xpPixels > 0)
        {
            batcher.getContext().method_25290(EXPERIENCE_BAR_PROGRESS_TEXTURE, 0, EXPERIENCE_BAR_Y, 0F, 0F, xpPixels, 5, 182, 5);
        }

        if (hotbar.experienceLevel > 0)
        {
            String level = Integer.toString(hotbar.experienceLevel);
            int levelX = (182 - batcher.getFont().getWidth(level)) / 2;
            int outlineColor = applyAlpha(0x000000, alpha);
            int levelColor = applyAlpha(HUD_GREEN, alpha);

            /* Vanilla-like outlined XP number: no drop shadow, solid contour around glyphs. */
            batcher.text(level, levelX - 1, EXPERIENCE_TEXT_Y, outlineColor, false);
            batcher.text(level, levelX + 1, EXPERIENCE_TEXT_Y, outlineColor, false);
            batcher.text(level, levelX, EXPERIENCE_TEXT_Y - 1, outlineColor, false);
            batcher.text(level, levelX, EXPERIENCE_TEXT_Y + 1, outlineColor, false);
            batcher.text(level, levelX, EXPERIENCE_TEXT_Y, levelColor, false);
        }

        /* Item glint (enchants) requires depth test in GUI item renderer. */
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1.0F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1.0F).normalize();
        RenderSystem.setupGui3DDiffuseLighting(light0, light1);

        for (int i = 0; i < 9; i++)
        {
            class_1799 stackItem = hotbar.items[i];

            if (stackItem == null || stackItem.method_7960())
            {
                continue;
            }

            int itemX = 3 + i * 20;
            int itemY = 3;

            batcher.getContext().method_51427(stackItem, itemX, itemY);
            batcher.getContext().method_51431(batcher.getFont().getRenderer(), stackItem, itemX, itemY);
        }

        if (hasOffhandItem)
        {
            int offhandX = -26;
            int offhandY = 3;

            batcher.getContext().method_51427(hotbar.offhandItem, offhandX, offhandY);
            batcher.getContext().method_51431(batcher.getFont().getRenderer(), hotbar.offhandItem, offhandX, offhandY);
        }

        batcher.getContext().method_51452();

        class_308.method_24210();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();

        batcher.getContext().method_51422(1F, 1F, 1F, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        stack.method_22909();
        batcher.flush();
    }

    private static float getResolutionScale(int width, int height)
    {
        if (width <= 0 || height <= 0)
        {
            return 1F;
        }

        return Math.max(0.05F, Math.min(width / REFERENCE_WIDTH, height / REFERENCE_HEIGHT));
    }

    private static void renderBar(Batcher2D batcher, float value, class_2960 empty, class_2960 half, class_2960 full, int x, int y, int slots, Random lowHealthShakeRandom, int regenerationHeartIndex)
    {
        if (slots <= 0)
        {
            return;
        }

        float normalized = class_3532.method_15363(value, 0F, slots * 2F) / 2F;

        for (int i = 0; i < slots; i++)
        {
            int row = i / 10;
            int col = i % 10;
            int iconX = x + col * 8;
            int iconY = y - row * 10;

            if (lowHealthShakeRandom != null)
            {
                iconY += lowHealthShakeRandom.nextInt(2);
            }

            if (i == regenerationHeartIndex)
            {
                iconY -= 2;
            }

            batcher.getContext().method_52706(empty, iconX, iconY, 9, 9);

            float current = normalized - i;

            if (current >= 1F)
            {
                batcher.getContext().method_52706(full, iconX, iconY, 9, 9);
            }
            else if (current >= 0.5F)
            {
                batcher.getContext().method_52706(half, iconX, iconY, 9, 9);
            }
        }
    }

    private static long thisTickSeed()
    {
        return currentHudTick() * 312871L;
    }

    private static long currentHudTick()
    {
        class_310 mc = class_310.method_1551();

        return mc.field_1687 != null ? mc.field_1687.method_8510() : System.currentTimeMillis() / 50L;
    }

    private static void renderBarReverse(Batcher2D batcher, float value, class_2960 empty, class_2960 half, class_2960 full, int x, int y, int slots, Random lowHungerShakeRandom)
    {
        if (slots <= 0)
        {
            return;
        }

        float normalized = class_3532.method_15363(value, 0F, slots * 2F) / 2F;

        for (int i = 0; i < slots; i++)
        {
            int row = i / 10;
            int col = i % 10;
            int iconX = x - col * 8;
            int iconY = y - row * 10;

            if (lowHungerShakeRandom != null)
            {
                iconY += lowHungerShakeRandom.nextInt(2);
            }

            batcher.getContext().method_52706(empty, iconX, iconY, 9, 9);

            float current = normalized - i;

            if (current >= 1F)
            {
                batcher.getContext().method_52706(full, iconX, iconY, 9, 9);
            }
            else if (current >= 0.5F)
            {
                batcher.getContext().method_52706(half, iconX, iconY, 9, 9);
            }
        }
    }

    private static int applyAlpha(int color, float alpha)
    {
        int a = class_3532.method_15340(Math.round(class_3532.method_15363(alpha, 0F, 1F) * 255F), 0, 255);

        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static void renderAirBar(Batcher2D batcher, float air, int x, int y)
    {
        if (air >= 300F)
        {
            return;
        }

        int full = class_3532.method_15386((air - 2F) * 10F / 300F);
        int popping = class_3532.method_15386(air * 10F / 300F) - full;

        full = class_3532.method_15340(full, 0, 10);
        popping = class_3532.method_15340(popping, 0, 10 - full);

        for (int i = 0; i < full + popping; i++)
        {
            int iconX = x - i * 8;
            class_2960 icon = i < full ? AIR : AIR_BURSTING;

            batcher.getContext().method_52706(icon, iconX, y, 9, 9);
        }
    }
}
