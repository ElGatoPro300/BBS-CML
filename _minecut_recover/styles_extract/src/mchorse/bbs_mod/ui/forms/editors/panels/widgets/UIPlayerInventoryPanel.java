package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.class_1304;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_746;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Consumer;

public class UIPlayerInventoryPanel extends UIOverlayPanel
{
    public static final int PANEL_WIDTH       = 232;
    public static final int PANEL_HEIGHT      = 290;

    private static final int PADDING_X        = 16;
    private static final int PADDING_Y        = 16;
    private static final int SECTION_GAP_Y    = 10;
    private static final int TITLE_HEIGHT     = 14;
    private static final int BUTTON_HEIGHT    = 20;

    private static final int SLOT_SIZE        = 18;
    private static final int SLOT_GAP         = 2;
    private static final int SLOT_SPACING     = SLOT_SIZE + SLOT_GAP;
    private static final int GRID_COLS        = 9;

    private static final int SLOT_BG_EMPTY    = 0x66000000;
    private static final int SLOT_BG_FILLED   = 0x66333333;
    private static final int SLOT_BG_HOVER    = 0x88555555;
    private static final int SLOT_BORDER      = 0xFF666666;
    private static final int SLOT_HOVER_TINT  = 0x33FFFFFF;

    private final Consumer<class_1799> callback;
    private final class_746 player;
    private final class_1661 playerInventory;

    public UIPlayerInventoryPanel(Consumer<class_1799> callback)
    {
        super(UIKeys.INVENTORY_TITLE);

        this.callback = callback;

        class_310 mc = class_310.method_1551();
        this.player = mc.field_1724;
        this.playerInventory = (this.player != null) ? this.player.method_31548() : null;

        this.setupUI();
    }

    private void setupUI()
    {
        this.content.w(PANEL_WIDTH);

        int cursorY = PADDING_Y;

        UILabel emptyTitle = sectionTitle(UIKeys.INVENTORY_SELECTION.get());
        centerHorizontally(emptyTitle, TITLE_HEIGHT, cursorY);
        this.content.add(emptyTitle);
        cursorY += TITLE_HEIGHT + 4;

        UIButton clear = new UIButton(UIKeys.INVENTORY_EMPTY, (b) ->
        {
            if (this.callback != null)
            {
                this.callback.accept(class_1799.field_8037);
            }

            this.close();
            UIUtils.playClick();
        });

        clear.relative(this.content).x(0.5F, -72).y(cursorY).w(144).h(BUTTON_HEIGHT);
        this.content.add(clear);
        cursorY += BUTTON_HEIGHT + SECTION_GAP_Y;

        if (this.playerInventory == null || this.player == null)
        {
            this.content.h(cursorY + PADDING_Y);
            return;
        }

        UILabel equipmentTitle = sectionTitle(UIKeys.MODELS_ARMOR.get());
        centerHorizontally(equipmentTitle, TITLE_HEIGHT, cursorY);
        this.content.add(equipmentTitle);
        cursorY += TITLE_HEIGHT + 4;

        int equipmentStartX = centerSlotsX(PANEL_WIDTH, 5);

        this.content.add(new UIEquipmentSlot(class_1304.field_6169).relative(this.content).x(equipmentStartX).y(cursorY).w(SLOT_SIZE).h(SLOT_SIZE));
        this.content.add(new UIEquipmentSlot(class_1304.field_6174).relative(this.content).x(equipmentStartX + SLOT_SPACING).y(cursorY).w(SLOT_SIZE).h(SLOT_SIZE));
        this.content.add(new UIEquipmentSlot(class_1304.field_6172).relative(this.content).x(equipmentStartX + SLOT_SPACING * 2).y(cursorY).w(SLOT_SIZE).h(SLOT_SIZE));
        this.content.add(new UIEquipmentSlot(class_1304.field_6166).relative(this.content).x(equipmentStartX + SLOT_SPACING * 3).y(cursorY).w(SLOT_SIZE).h(SLOT_SIZE));
        this.content.add(new UIEquipmentSlot(class_1304.field_6171).relative(this.content).x(equipmentStartX + SLOT_SPACING * 4).y(cursorY).w(SLOT_SIZE).h(SLOT_SIZE));
        cursorY += SLOT_SIZE + SECTION_GAP_Y;

        UILabel hotbarTitle = sectionTitle(UIKeys.INVENTORY_HOTBAR.get());
        centerHorizontally(hotbarTitle, TITLE_HEIGHT, cursorY);
        this.content.add(hotbarTitle);
        cursorY += TITLE_HEIGHT + 4;

        int gridStartX = gridStartX(PANEL_WIDTH, GRID_COLS, SLOT_SPACING, SLOT_SIZE);
        cursorY = placeGridRow(cursorY, gridStartX, 0, 9);

        cursorY += SECTION_GAP_Y;

        UILabel mainTitle = sectionTitle(UIKeys.INVENTORY_FULL.get());
        centerHorizontally(mainTitle, TITLE_HEIGHT, cursorY);
        this.content.add(mainTitle);
        cursorY += TITLE_HEIGHT + 4;

        int startIndex = 9;
        for (int row = 0; row < 3; row++)
        {
            cursorY = placeGridRow(cursorY, gridStartX, startIndex + row * 9, 9);
        }

        this.content.h(cursorY + PADDING_Y);
    }

    private UILabel sectionTitle(String text)
    {
        UILabel label = UI.label(IKey.constant(text));
        label.w(PANEL_WIDTH).h(TITLE_HEIGHT);
        return label;
    }

    private void centerHorizontally(UIElement element, int h, int y)
    {
        element.relative(this.content).x(0.5F, -PANEL_WIDTH / 2 + PADDING_X).y(y).w(PANEL_WIDTH - PADDING_X * 2).h(h);
    }

    private int gridStartX(int panelW, int cols, int spacing, int slotSize)
    {
        int gridWidth = cols * slotSize + (cols - 1) * SLOT_GAP;

        return (panelW - gridWidth) / 2;
    }

    private int centerSlotsX(int panelW, int slots)
    {
        int width = slots * SLOT_SIZE + (slots - 1) * SLOT_GAP;

        return (panelW - width) / 2;
    }

    private int placeGridRow(int cursorY, int startX, int indexStart, int count)
    {
        for (int i = 0; i < count; i++)
        {
            UIInventorySlot slot = new UIInventorySlot(indexStart + i);
            slot.relative(this.content).x(startX + i * SLOT_SPACING).y(cursorY).w(SLOT_SIZE).h(SLOT_SIZE);
            this.content.add(slot);
        }
        return cursorY + SLOT_SIZE + 2;
    }

    private class UIInventorySlot extends UIElement
    {
        private final int slotIndex;

        public UIInventorySlot(int slotIndex)
        {
            this.slotIndex = slotIndex;
        }

        private class_1799 getStack()
        {
            if (playerInventory == null || slotIndex < 0 || slotIndex >= playerInventory.method_5439())
            {
                return class_1799.field_8037;
            }
            return playerInventory.method_5438(slotIndex);
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.area.isInside(context) && context.mouseButton == 0)
            {
                class_1799 stack = this.getStack();
                if (!stack.method_7960() && callback != null)
                {
                    callback.accept(stack.method_7972());
                    UIPlayerInventoryPanel.this.close();
                    UIUtils.playClick();
                    return true;
                }
            }
            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            class_1799 stack = this.getStack();
            boolean isEmpty = stack.method_7960();
            boolean hovered = this.area.isInside(context);

            int bg = isEmpty ? SLOT_BG_EMPTY : SLOT_BG_FILLED;
            if (hovered && !isEmpty) bg = SLOT_BG_HOVER;

            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), bg);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), SLOT_BORDER);

            if (!isEmpty)
            {
                int itemX = this.area.x + 1;
                int itemY = this.area.y + 1;

                Vector3f light0 = new Vector3f(0.85F, 0.85F, -1.0F).normalize();
                Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1.0F).normalize();
                RenderSystem.setupGui3DDiffuseLighting(light0, light1);

                context.batcher.getContext().method_51427(stack, itemX, itemY);
                context.batcher.getContext().method_51431(context.batcher.getFont().getRenderer(), stack, itemX, itemY);

                context.batcher.getContext().method_51452();

                class_308.method_24210();

                if (hovered)
                {
                    context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), SLOT_HOVER_TINT);
                }
            }

            super.render(context);
        }
    }

    private class UIEquipmentSlot extends UIElement
    {
        private final class_1304 slot;

        public UIEquipmentSlot(class_1304 slot)
        {
            this.slot = slot;
        }

        private class_1799 getStack()
        {
            if (player == null)
            {
                return class_1799.field_8037;
            }

            return player.method_6118(this.slot);
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.area.isInside(context) && context.mouseButton == 0)
            {
                class_1799 stack = this.getStack();

                if (!stack.method_7960() && callback != null)
                {
                    callback.accept(stack.method_7972());
                    UIPlayerInventoryPanel.this.close();
                    UIUtils.playClick();

                    return true;
                }
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            class_1799 stack = this.getStack();
            boolean isEmpty = stack.method_7960();
            boolean hovered = this.area.isInside(context);

            int bg = isEmpty ? SLOT_BG_EMPTY : SLOT_BG_FILLED;

            if (hovered && !isEmpty)
            {
                bg = SLOT_BG_HOVER;
            }

            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), bg);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), SLOT_BORDER);

            if (!isEmpty)
            {
                int itemX = this.area.x + 1;
                int itemY = this.area.y + 1;

                Vector3f light0 = new Vector3f(0.85F, 0.85F, -1.0F).normalize();
                Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1.0F).normalize();
                RenderSystem.setupGui3DDiffuseLighting(light0, light1);

                context.batcher.getContext().method_51427(stack, itemX, itemY);
                context.batcher.getContext().method_51431(context.batcher.getFont().getRenderer(), stack, itemX, itemY);

                context.batcher.getContext().method_51452();

                class_308.method_24210();

                if (hovered)
                {
                    context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), SLOT_HOVER_TINT);
                }
            }

            super.render(context);
        }
    }
}