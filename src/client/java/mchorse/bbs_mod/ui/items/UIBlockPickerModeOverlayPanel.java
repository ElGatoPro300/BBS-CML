package mchorse.bbs_mod.ui.items;

import mchorse.bbs_mod.client.BlockPickerClient;
import mchorse.bbs_mod.items.BlockPickerMode;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

public class UIBlockPickerModeOverlayPanel extends UIOverlayPanel
{
    private static UIBlockPickerModeOverlayPanel opened;

    private UIButton modelBlockButton;
    private UIButton importFilmButton;

    public static void open(UIBlockPickerModeOverlayPanel panel)
    {
        if (UIBlockPickerModeOverlayPanel.isOpened())
        {
            return;
        }

        UIBlockPickerModeOverlayPanel.opened = panel;
        MinecraftClient client = MinecraftClient.getInstance();
        Screen returnScreen = client.currentScreen;

        panel.onClose((event) ->
        {
            UIBlockPickerModeOverlayPanel.opened = null;

            if (returnScreen != null)
            {
                client.setScreen(returnScreen);
            }
            else
            {
                client.setScreen(null);
            }
        });

        UIScreen.open(new UIBaseMenu()
        {
            @Override
            public boolean needsWorldRender()
            {
                return true;
            }

            @Override
            public boolean canHideHUD()
            {
                return false;
            }

            @Override
            public boolean canPause()
            {
                return false;
            }

            @Override
            public void onOpen(UIBaseMenu oldMenu)
            {
                super.onOpen(oldMenu);

                UIOverlay.addOverlay(this.context, panel, 240, 96);
            }
        });
    }

    public static boolean isOpened()
    {
        if (UIBlockPickerModeOverlayPanel.opened != null)
        {
            return true;
        }

        Screen currentScreen = MinecraftClient.getInstance().currentScreen;

        if (!(currentScreen instanceof UIScreen uiScreen))
        {
            return false;
        }

        List<UIBlockPickerModeOverlayPanel> panels = uiScreen.getMenu().getRoot().getChildren(UIBlockPickerModeOverlayPanel.class);

        return !panels.isEmpty();
    }

    public UIBlockPickerModeOverlayPanel()
    {
        super(UIKeys.BLOCK_PICKER_MODE_TITLE);

        this.modelBlockButton = new UIButton(UIKeys.BLOCK_PICKER_MODE_MODEL_BLOCK, (b) -> this.selectMode(BlockPickerMode.MODEL_BLOCK));
        this.importFilmButton = new UIButton(UIKeys.BLOCK_PICKER_MODE_IMPORT_FILM, (b) -> this.selectMode(BlockPickerMode.IMPORT_TO_FILM));

        this.modelBlockButton.relative(this.content).y(8).w(1F, -16).h(20);
        this.importFilmButton.relative(this.content).y(36).w(1F, -16).h(20);

        this.content.add(this.modelBlockButton, this.importFilmButton);
        this.updateSelection();
    }

    private void selectMode(BlockPickerMode mode)
    {
        BlockPickerClient.setMode(mode);
        this.updateSelection();
    }

    private void updateSelection()
    {
        BlockPickerMode mode = BlockPickerClient.getMode();
        int activeColor = Colors.ACTIVE;
        int idleColor = Colors.GRAY;

        this.modelBlockButton.color(mode == BlockPickerMode.MODEL_BLOCK ? activeColor : idleColor);
        this.importFilmButton.color(mode == BlockPickerMode.IMPORT_TO_FILM ? activeColor : idleColor);
    }
}
