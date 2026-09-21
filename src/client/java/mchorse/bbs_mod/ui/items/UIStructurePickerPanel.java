package mchorse.bbs_mod.ui.items;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.StructurePickerClient;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.items.StructurePickerBrushShape;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

public class UIStructurePickerPanel extends UIOverlayPanel
{
    private static final int BODY_WIDTH = 300;
    private static final int BODY_HEIGHT = 8 + StructurePickerMode.values().length * 22 + 8;
    private static final int DEFAULT_WIDTH = 340;
    private static final int DEFAULT_HEIGHT = 20 + Math.min(BODY_HEIGHT + 16, 320);

    private static UIStructurePickerPanel opened;

    private final List<UIButton> shapeButtons = new ArrayList<>();
    private final UIButton modelBlockButton;
    private final UIButton importFilmButton;
    private final UIButton removeSelectionButton;
    private final UIToggle subtractToggle;
    private final UIButton breakSelectionButton;
    private final UIToggle clickOnAirToggle;
    private final UITrackpad sameBlockLimit;
    private final UIElement brushControls;
    private final UICirculate brushShape;
    private final UITrackpad brushRadius;
    private final UITrackpad brushDepth;

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
            protected void closeMenu()
            {
                panel.close();
            }

            @Override
            public void onOpen(UIBaseMenu oldMenu)
            {
                super.onOpen(oldMenu);

                UIOverlay.addOverlay(this.context, panel, DEFAULT_WIDTH, DEFAULT_HEIGHT);
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

        this.minSize(280, 200);

        UIElement body = new UIElement();
        StructurePickerMode[] modes = StructurePickerMode.values();

        for (int i = 0; i < modes.length; i++)
        {
            StructurePickerMode mode = modes[i];
            UIButton button = new UIButton(UIKeys.STRUCTURE_PICKER_MODE_LABELS[mode.index], (b) -> this.selectMode(mode));

            button.relative(body).x(8).y(8 + i * 22).w(110).h(20);
            this.shapeButtons.add(button);
            body.add(button);
        }

        this.modelBlockButton = new UIButton(UIKeys.STRUCTURE_PICKER_MAKE_MODEL_BLOCK, (b) -> this.importSelection(true));
        this.importFilmButton = new UIButton(UIKeys.STRUCTURE_PICKER_IMPORT_FILM, (b) -> this.importSelection(false));
        this.removeSelectionButton = new UIButton(UIKeys.STRUCTURE_PICKER_REMOVE_SELECTION, (b) -> this.removeSelection());
        this.subtractToggle = new UIToggle(UIKeys.STRUCTURE_PICKER_SUBTRACT_SELECTION, StructurePickerClient.isSubtractMode(), (b) ->
        {
            StructurePickerClient.setSubtractMode(b.getValue());
        });
        this.breakSelectionButton = new UIButton(UIKeys.STRUCTURE_PICKER_BREAK_SELECTION, (b) -> this.breakSelection());
        this.clickOnAirToggle = new UIToggle(UIKeys.STRUCTURE_PICKER_CLICK_ON_AIR, StructurePickerClient.isClickOnAir(), (b) ->
        {
            StructurePickerClient.setClickOnAir(b.getValue());
        });
        this.sameBlockLimit = new UITrackpad((v) -> StructurePickerClient.setSameBlockLimit(v.intValue()));
        this.sameBlockLimit.tooltip(UIKeys.STRUCTURE_PICKER_SAME_LIMIT);
        this.sameBlockLimit.integer().limit(1, 500);
        this.sameBlockLimit.setValue(StructurePickerClient.getSameBlockLimit());

        this.brushShape = new UICirculate((b) ->
            StructurePickerClient.setBrushShape(StructurePickerBrushShape.fromIndex(b.getValue())));
        this.brushShape.addLabel(UIKeys.STRUCTURE_PICKER_BRUSH_SPHERE);
        this.brushShape.addLabel(UIKeys.STRUCTURE_PICKER_BRUSH_CUBE);
        this.brushShape.setValue(StructurePickerClient.getBrushShape().index);
        this.brushShape.tooltip(UIKeys.STRUCTURE_PICKER_BRUSH_SHAPE);

        this.brushRadius = new UITrackpad((v) -> StructurePickerClient.setBrushRadius(v.intValue()));
        this.brushRadius.tooltip(UIKeys.STRUCTURE_PICKER_BRUSH_RADIUS);
        this.brushRadius.integer().limit(0, 32);
        this.brushRadius.setValue(StructurePickerClient.getBrushRadius());

        this.brushDepth = new UITrackpad((v) -> StructurePickerClient.setBrushDepth(v.intValue()));
        this.brushDepth.tooltip(UIKeys.STRUCTURE_PICKER_BRUSH_DEPTH);
        this.brushDepth.integer().limit(1, 32);
        this.brushDepth.setValue(StructurePickerClient.getBrushDepth());

        this.brushControls = new UIElement();
        this.brushShape.relative(this.brushControls).x(0).y(0).w(1F).h(20);
        this.brushRadius.relative(this.brushControls).x(0).y(24).w(1F).h(20);
        this.brushDepth.relative(this.brushControls).x(0).y(48).w(1F).h(20);
        this.brushControls.add(this.brushShape, this.brushRadius, this.brushDepth);

        this.modelBlockButton.relative(body).x(126).y(8).w(1F, -134).h(20);
        this.importFilmButton.relative(body).x(126).y(32).w(1F, -134).h(20);
        this.subtractToggle.relative(body).x(126).y(56).w(1F, -134).h(14);
        this.removeSelectionButton.relative(body).x(126).y(74).w(1F, -134).h(20);
        this.breakSelectionButton.relative(body).x(126).y(98).w(1F, -134).h(20);
        this.clickOnAirToggle.relative(body).x(126).y(122).w(1F, -134).h(14);
        this.sameBlockLimit.relative(body).x(126).y(146).w(1F, -134).h(20);
        this.brushControls.relative(body).x(126).y(146).w(1F, -134).h(68);
        this.breakSelectionButton.color(Colors.RED);

        body.add(this.modelBlockButton, this.importFilmButton, this.subtractToggle, this.removeSelectionButton, this.breakSelectionButton, this.clickOnAirToggle, this.sameBlockLimit, this.brushControls);
        body.w(BODY_WIDTH).h(BODY_HEIGHT);

        UIScrollView horizontalScroll = new UIScrollView(ScrollDirection.HORIZONTAL);

        horizontalScroll.h(BODY_HEIGHT);
        horizontalScroll.column(0).vertical(false).scroll().width(BODY_WIDTH).padding(0);
        horizontalScroll.add(body);

        UIScrollView verticalScroll = new UIScrollView();

        verticalScroll.full(this.content);
        verticalScroll.column(0).vertical().stretch().scroll().padding(0);
        verticalScroll.add(horizontalScroll);
        this.content.add(verticalScroll);

        this.keys().register(Keys.UNDO, StructurePickerClient::undo).active(StructurePickerClient::canUndo);
        this.keys().register(Keys.REDO, StructurePickerClient::redo).active(StructurePickerClient::canRedo);

        this.updateShapeButtons();
        this.updateImportButtons();
        this.syncModeExtras();
    }

    private void selectMode(StructurePickerMode mode)
    {
        StructurePickerClient.setMode(mode);
        this.updateShapeButtons();
        this.syncModeExtras();
    }

    private void updateShapeButtons()
    {
        StructurePickerMode current = StructurePickerClient.getMode();
        StructurePickerMode[] modes = StructurePickerMode.values();

        for (int i = 0; i < this.shapeButtons.size(); i++)
        {
            this.shapeButtons.get(i).color(modes[i] == current ? Colors.ACTIVE : Colors.GRAY);
        }
    }

    private void syncModeExtras()
    {
        StructurePickerMode mode = StructurePickerClient.getMode();
        boolean showSame = mode == StructurePickerMode.SAME;
        boolean showBrush = mode == StructurePickerMode.BRUSH;

        this.sameBlockLimit.setVisible(showSame);
        this.brushControls.setVisible(showBrush);

        if (showSame && !this.sameBlockLimit.isFocused() && !this.sameBlockLimit.isDragging())
        {
            int limit = StructurePickerClient.getSameBlockLimit();

            if ((int) this.sameBlockLimit.getValue() != limit)
            {
                this.sameBlockLimit.setValue(limit);
            }
        }

        if (showBrush)
        {
            int shape = StructurePickerClient.getBrushShape().index;

            if (this.brushShape.getValue() != shape)
            {
                this.brushShape.setValue(shape);
            }

            if (!this.brushRadius.isFocused() && !this.brushRadius.isDragging())
            {
                int radius = StructurePickerClient.getBrushRadius();

                if ((int) this.brushRadius.getValue() != radius)
                {
                    this.brushRadius.setValue(radius);
                }
            }

            if (!this.brushDepth.isFocused() && !this.brushDepth.isDragging())
            {
                int depth = StructurePickerClient.getBrushDepth();

                if ((int) this.brushDepth.getValue() != depth)
                {
                    this.brushDepth.setValue(depth);
                }
            }
        }
    }

    private void updateImportButtons()
    {
        UIFilmPanel filmPanel = BBSModClient.getDashboard().getPanel(UIFilmPanel.class);
        boolean canImport = filmPanel != null && filmPanel.getData() != null;

        this.importFilmButton.setEnabled(canImport);
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

        StructurePickerClient.importSelection(toModelBlock);
    }

    private void removeSelection()
    {
        if (Window.isShiftPressed())
        {
            StructurePickerClient.removeSelection();
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
                    StructurePickerClient.removeSelection();
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
