package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.function.Consumer;

public class UIBlockStateEditor extends UIElement
{
    private final Consumer<BlockState> callback;
    private BlockState blockState;
    private boolean opened;

    public UIBlockStateEditor(Consumer<BlockState> callback)
    {
        this.callback = callback;
        this.blockState = Blocks.AIR.getDefaultState();

        this.context((menu) ->
        {
            Item item = this.blockState.getBlock().asItem();

            if (item != Items.AIR)
            {
                menu.action(Icons.PLAYER, UIKeys.ITEM_STACK_CONTEXT_GIVE, () -> UIItemStack.giveToPlayer(new ItemStack(item)));
            }
        });

        this.h(UIConstants.CONTROL_HEIGHT);
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            this.opened = true;

            UIUnifiedPickOverlayPanel panel = UIUnifiedPickOverlayPanel.forBlock((state) ->
            {
                this.acceptBlockState(state);
            }, this.blockState);

            panel.onClose((a) -> this.opened = false);
            UIOverlay.addOverlay(this.getContext(), panel, 0.5F, 0.75F);
            UIUtils.playClick();

            return true;
        }

        return super.subMouseClicked(context);
    }

    public void setBlockState(BlockState blockState)
    {
        this.blockState = blockState == null ? Blocks.AIR.getDefaultState() : blockState;
    }

    private void acceptBlockState(BlockState blockState)
    {
        this.blockState = blockState == null ? Blocks.AIR.getDefaultState() : blockState;

        if (this.callback != null)
        {
            this.callback.accept(this.blockState);
        }
    }

    @Override
    public void render(UIContext context)
    {
        int border = this.opened ? Colors.A100 | BBSSettings.primaryColor.get() : Colors.WHITE;

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), border);
        context.batcher.box(this.area.x + 1, this.area.y + 1, this.area.ex() - 1, this.area.ey() - 1, -3750202);

        ItemStack stack = new ItemStack(this.blockState.getBlock().asItem());

        if (!stack.isEmpty())
        {
            MatrixStack matrices = context.batcher.getContext().getMatrices();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            matrices.push();
            consumers.setUI(true);
            context.batcher.getContext().drawItem(stack, this.area.mx() - 8, this.area.my() - 8);
            context.batcher.getContext().drawItemInSlot(context.batcher.getFont().getRenderer(), stack, this.area.mx() - 8, this.area.my() - 8);
            consumers.setUI(false);
            matrices.pop();
        }

        super.render(context);
    }
}
