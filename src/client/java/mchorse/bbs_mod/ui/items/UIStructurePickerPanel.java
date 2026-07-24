package mchorse.bbs_mod.ui.items;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.StructurePickerClient;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * Immersive Structure Picker: tools on left, mode list on right; slide in from each side.
 */
public class UIStructurePickerPanel extends UIOverlayPanel
{
    private static final int MODES_W = 100;
    private static final int TOOLS_W = 168;
    private static final int TOOLS_H = 204;
    private static final int COLOR_IMPORT_MODEL = 0xFF3B82F6;
    private static final int COLOR_IMPORT_FILM = 0xFF3B82F6;
    private static final int COLOR_REMOVE = 0xFFF59E0B;
    private static final int COLOR_BREAK = Colors.RED;
    public static final int COLOR_SIDEBAR = 0x66101018;

    private static UIStructurePickerPanel opened;

    private final UIElement modesPanel;
    private final UIElement toolsPanel;
    private final UITextbox nameBox;
    private final UIButton modelBlockButton;
    private final UIButton importFilmButton;
    private final UIButton removeSelectionButton;
    private final UIButton breakSelectionButton;
    private final UIStructurePickerModePicker modePicker;
    private final UIToggle clickOnAirToggle;
    private final UIToggle subtractToggle;

    public static void open()
    {
        if (UIStructurePickerPanel.opened != null)
        {
            return;
        }

        UIStructurePickerPanel panel = new UIStructurePickerPanel();

        UIStructurePickerPanel.opened = panel;

        MinecraftClient client = MinecraftClient.getInstance();
        Screen returnScreen = client.currentScreen;

        panel.onClose((event) ->
        {
            UIStructurePickerPanel.opened = null;

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
            public boolean preserveMinecraftGuiScale()
            {
                return true;
            }

            @Override
            public boolean showInputOverlay()
            {
                return false;
            }

            @Override
            protected void closeMenu()
            {
                panel.close();
            }

            @Override
            public void onOpen(UIBaseMenu oldMenu)
            {
                super.onOpen(oldMenu);

                UIOverlay.addOverlay(this.context, panel, 1F, 1F).noBackground();
            }
        });
    }

    public static boolean isOpened()
    {
        if (UIStructurePickerPanel.opened != null)
        {
            return true;
        }

        Screen currentScreen = MinecraftClient.getInstance().currentScreen;

        if (!(currentScreen instanceof UIScreen uiScreen))
        {
            return false;
        }

        List<UIStructurePickerPanel> panels = uiScreen.getMenu().getRoot().getChildren(UIStructurePickerPanel.class);

        return !panels.isEmpty();
    }

    public UIStructurePickerPanel()
    {
        super(UIKeys.STRUCTURE_PICKER_CONFIRM_TITLE);

        this.title.setVisible(false);
        this.close.setVisible(false);
        this.icons.setVisible(false);
        this.content.relative(this).xy(0, 0).w(1F).h(1F);

        this.modesPanel = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                float t = UIStructurePickerPanel.this.getOpenAmount();
                float slide = (1.0F - t) * -(MODES_W + 24);

                context.render.batcher.getContext().getMatrices().push();
                context.render.batcher.getContext().getMatrices().translate(slide, 0.0F, 0.0F);
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), COLOR_SIDEBAR);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0x55FFFFFF, 1);
                super.render(context);
                context.render.batcher.getContext().getMatrices().pop();
            }
        };
        this.toolsPanel = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                float t = UIStructurePickerPanel.this.getOpenAmount();
                float slide = (1.0F - t) * (TOOLS_W + 24);

                context.render.batcher.getContext().getMatrices().push();
                context.render.batcher.getContext().getMatrices().translate(slide, 0.0F, 0.0F);
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), COLOR_SIDEBAR);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0x55FFFFFF, 1);
                super.render(context);
                context.render.batcher.getContext().getMatrices().pop();
            }
        };

        this.nameBox = new UITextbox(64, null);
        this.nameBox.filename();
        this.nameBox.placeholder(UIKeys.STRUCTURE_PICKER_NAME_PLACEHOLDER);

        this.modelBlockButton = new UIButton(UIKeys.STRUCTURE_PICKER_MAKE_MODEL_BLOCK, (b) -> this.importSelection(true));
        this.modelBlockButton.color(COLOR_IMPORT_MODEL);
        this.importFilmButton = new UIButton(UIKeys.STRUCTURE_PICKER_IMPORT_FILM, (b) -> this.importSelection(false));
        this.importFilmButton.color(COLOR_IMPORT_FILM);
        this.removeSelectionButton = new UIButton(UIKeys.STRUCTURE_PICKER_REMOVE_SELECTION, (b) -> this.removeSelection());
        this.removeSelectionButton.color(COLOR_REMOVE);
        this.breakSelectionButton = new UIButton(UIKeys.STRUCTURE_PICKER_BREAK_SELECTION, (b) -> this.breakSelection());
        this.breakSelectionButton.color(COLOR_BREAK);

        this.modePicker = new UIStructurePickerModePicker();

        this.clickOnAirToggle = new UIToggle(UIKeys.STRUCTURE_PICKER_CLICK_ON_AIR, StructurePickerClient.isClickOnAir(), (b) ->
        {
            StructurePickerClient.setClickOnAir(b.getValue());
        });
        this.clickOnAirToggle.color(Colors.WHITE, true);

        this.subtractToggle = new UIToggle(UIKeys.STRUCTURE_PICKER_SUBTRACT_SELECTION, StructurePickerClient.isSubtractMode(), (b) ->
        {
            StructurePickerClient.setSubtractMode(b.getValue());
        });
        this.subtractToggle.color(Colors.WHITE, true);

        int panelH = Math.max(this.modePicker.preferredHeight(), TOOLS_H);

        /* Mode list flush left, tools flush right — matched height. */
        this.modesPanel.relative(this.content).x(0).y(0.5F).w(MODES_W).h(panelH).anchorY(0.5F);
        this.toolsPanel.relative(this.content).x(1F).y(0.5F).w(TOOLS_W).h(panelH).anchor(1F, 0.5F);

        this.modePicker.relative(this.modesPanel).xy(0, 0).w(1F).h(1F);

        this.nameBox.relative(this.toolsPanel).x(10).y(10).w(1F, -20).h(18);
        this.modelBlockButton.relative(this.toolsPanel).x(10).y(38).w(1F, -20).h(22);
        this.importFilmButton.relative(this.toolsPanel).x(10).y(66).w(1F, -20).h(22);
        this.removeSelectionButton.relative(this.toolsPanel).x(10).y(94).w(1F, -20).h(22);
        this.breakSelectionButton.relative(this.toolsPanel).x(10).y(122).w(1F, -20).h(22);
        this.clickOnAirToggle.relative(this.toolsPanel).x(10).y(156).w(1F, -20).h(16);
        this.subtractToggle.relative(this.toolsPanel).x(10).y(178).w(1F, -20).h(16);

        this.modesPanel.add(this.modePicker);
        this.toolsPanel.add(
            this.nameBox,
            this.modelBlockButton,
            this.importFilmButton,
            this.removeSelectionButton,
            this.breakSelectionButton,
            this.clickOnAirToggle,
            this.subtractToggle
        );
        this.content.add(this.modesPanel, this.toolsPanel);

        this.updateImportButtons();
    }

    private float getOpenAmount()
    {
        UIElement parent = this.getParent();

        if (parent instanceof UIOverlay)
        {
            return ((UIOverlay) parent).getOpenTransition();
        }

        return 1.0F;
    }

    @Override
    protected void beginOpenTransition(UIContext context, float transition)
    {
        /* Per-panel slides are applied in tools/modes render. */
    }

    @Override
    protected void endOpenTransition(UIContext context, float transition)
    {
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        /* Transparent shell — side panels paint their own fill. */
    }

    private void updateImportButtons()
    {
        UIFilmPanel filmPanel = BBSModClient.getDashboard().getPanel(UIFilmPanel.class);
        boolean canImport = filmPanel != null && filmPanel.getData() != null;

        this.importFilmButton.setEnabled(canImport);

        if (canImport)
        {
            this.importFilmButton.removeTooltip();
        }
        else
        {
            this.importFilmButton.tooltip(UIKeys.STRUCTURE_PICKER_IMPORT_FILM_DISABLED, Direction.LEFT);
        }
    }

    private String getStructureName()
    {
        return this.nameBox.getText() == null ? "" : this.nameBox.getText().trim();
    }

    private void importSelection(boolean toModelBlock)
    {
        if (!toModelBlock)
        {
            UIFilmPanel filmPanel = BBSModClient.getDashboard().getPanel(UIFilmPanel.class);

            if (filmPanel == null || filmPanel.getData() == null)
            {
                return;
            }
        }

        StructurePickerClient.importSelection(toModelBlock, this.getStructureName());
    }

    private void removeSelection()
    {
        if (Window.isShiftPressed())
        {
            StructurePickerClient.clearSelection();
            this.close();

            return;
        }

        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
            UIKeys.STRUCTURE_PICKER_REMOVE_SELECTION,
            UIKeys.STRUCTURE_PICKER_REMOVE_CONFIRM,
            (confirmed) ->
            {
                if (confirmed)
                {
                    StructurePickerClient.clearSelection();
                    this.close();
                }
            }
        );

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void breakSelection()
    {
        if (Window.isShiftPressed())
        {
            StructurePickerClient.breakSelection();
            this.close();

            return;
        }

        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
            UIKeys.STRUCTURE_PICKER_BREAK_SELECTION,
            UIKeys.STRUCTURE_PICKER_BREAK_CONFIRM,
            (confirmed) ->
            {
                if (confirmed)
                {
                    StructurePickerClient.breakSelection();
                    this.close();
                }
            }
        );

        UIOverlay.addOverlay(this.getContext(), panel);
    }
}
