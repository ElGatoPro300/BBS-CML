package mchorse.bbs_mod.ui.items;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.StructurePickerClient;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.items.StructurePickerBrushShape;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

public class UIStructurePickerPanel extends UIOverlayPanel
{
    private static final int MODE_COLUMN_HEIGHT = 8 + StructurePickerMode.values().length * 22 + 8;
    /* Right column through reachExtended + bottom pad (same/brush slots added dynamically). */
    private static final int RIGHT_COLUMN_CORE = 8 + 32 + 4 + 6 + 4 + 20 + 24 + 14 + 20 + 24 + 14 + 20 + 24 + 20 + 24 + 8;
    private static final int SLOT_SAME = 24;
    private static final int SLOT_BRUSH = 72;
    private static final int BODY_WIDTH = 360;
    private static final int BODY_HEIGHT_MAX = Math.max(MODE_COLUMN_HEIGHT, RIGHT_COLUMN_CORE + Math.max(SLOT_SAME, SLOT_BRUSH));
    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 20 + Math.min(BODY_HEIGHT_MAX + 16, 320);
    private static final float MODE_INACTIVE_TINT = 0.78F;

    /* Distinct accents per mode (not rainbow-ordered). */
    private static final int[] MODE_TINTS = new int[]
    {
        0xFF14B8A6, /* Block - teal */
        0xFFF97316, /* Rectangle - orange */
        0xFFA78BFA, /* Cube - light purple */
        0xFF84CC16, /* Circle - lime */
        0xFFE11D48, /* Sphere - crimson */
        0xFF0EA5E9, /* Triangle - sky */
        0xFFEAB308, /* Cone - gold */
        0xFFC026D3, /* Cylinder - fuchsia */
        0xFF6366F1, /* Same - indigo */
        0xFFEC4899, /* Brush - pink */
        0xFFEF4444  /* Erase - red */
    };

    private static final Icon[] MODE_ICONS = new Icon[]
    {
        Icons.SP_BLOCK,
        Icons.SP_RECTANGLE,
        Icons.SP_CUBE,
        Icons.SP_CIRCLE,
        Icons.SP_SPHERE,
        Icons.SP_TRIANGLE,
        Icons.SP_CONE,
        Icons.SP_CYLINDER,
        Icons.BUCKET,
        Icons.BRUSH,
        Icons.ERASER
    };

    private static UIStructurePickerPanel opened;

    private final List<UIButton> shapeButtons = new ArrayList<>();
    private final UICirculate applyScope;
    private final UIButton modelBlockButton;
    private final UIButton importFilmButton;
    private final UIButton removeSelectionButton;
    private final UIToggle subtractToggle;
    private final UIButton breakSelectionButton;
    private final UIToggle clickOnAirToggle;
    private final UITrackpad reach;
    private final UITrackpad reachExtended;
    private final UITrackpad sameBlockLimit;
    private final UIElement brushControls;
    private final UICirculate brushShape;
    private final UITrackpad brushRadius;
    private final UITrackpad brushDepth;
    private final UIElement body;
    private final UIScrollView horizontalScroll;
    private final UIIcon helpIcon;

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

        this.body = new UIElement();
        StructurePickerMode[] modes = StructurePickerMode.values();

        for (int i = 0; i < modes.length; i++)
        {
            StructurePickerMode mode = modes[i];
            UIButton button = new UIButton(UIKeys.STRUCTURE_PICKER_MODE_LABELS[mode.index], (b) -> this.selectMode(mode));

            button.leadingIcon(MODE_ICONS[mode.index]);
            button.relative(this.body).x(8).y(8 + i * 22).w(110).h(20);
            this.shapeButtons.add(button);
            this.body.add(button);
        }

        this.applyScope = new UICirculate((b) ->
        {
            StructurePickerClient.setApplyToSelectedOnly(b.getValue() == 0);
            this.updateActionButtons();
        });
        this.applyScope.addLabel(UIKeys.STRUCTURE_PICKER_APPLY_SELECTED);
        this.applyScope.addLabel(UIKeys.STRUCTURE_PICKER_APPLY_ALL);
        this.applyScope.wrapping();
        this.applyScope.setValue(StructurePickerClient.isApplyToSelectedOnly() ? 0 : 1);

        UIElement scopeSeparator = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                int y = this.area.my();

                context.batcher.box(this.area.x, y, this.area.ex(), y + 1, 0x66FFFFFF);
                super.render(context);
            }
        };

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

        this.reach = new UITrackpad((v) -> BBSSettings.structurePickerReach.set(v.intValue()));
        this.reach.tooltip(UIKeys.STRUCTURE_PICKER_REACH);
        this.reach.integer().limit(1, 300);
        this.reach.setValue(BBSSettings.structurePickerReach.get());

        this.reachExtended = new UITrackpad((v) -> BBSSettings.structurePickerReachExtended.set(v.intValue()));
        this.reachExtended.tooltip(UIKeys.STRUCTURE_PICKER_REACH_EXTENDED);
        this.reachExtended.integer().limit(1, 300);
        this.reachExtended.setValue(BBSSettings.structurePickerReachExtended.get());

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

        this.applyScope.relative(this.body).x(126).y(8).w(1F, -134).h(20);
        scopeSeparator.relative(this.applyScope).x(0).y(1F, 4).w(1F).h(6);
        this.modelBlockButton.relative(scopeSeparator).x(0).y(1F, 4).w(1F).h(20);
        this.importFilmButton.relative(this.modelBlockButton).x(0).y(1F, 4).w(1F).h(20);
        this.subtractToggle.relative(this.importFilmButton).x(0).y(1F, 4).w(1F).h(14);
        this.removeSelectionButton.relative(this.subtractToggle).x(0).y(1F, 4).w(1F).h(20);
        this.breakSelectionButton.relative(this.removeSelectionButton).x(0).y(1F, 4).w(1F).h(20);
        this.clickOnAirToggle.relative(this.breakSelectionButton).x(0).y(1F, 4).w(1F).h(14);
        this.reach.relative(this.clickOnAirToggle).x(0).y(1F, 4).w(1F).h(20);
        this.reachExtended.relative(this.reach).x(0).y(1F, 4).w(1F).h(20);
        this.sameBlockLimit.relative(this.reachExtended).x(0).y(1F, 4).w(1F).h(20);
        this.brushControls.relative(this.reachExtended).x(0).y(1F, 4).w(1F).h(68);
        this.breakSelectionButton.color(Colors.RED);

        this.body.add(this.applyScope, scopeSeparator, this.modelBlockButton, this.importFilmButton, this.subtractToggle, this.removeSelectionButton, this.breakSelectionButton, this.clickOnAirToggle, this.reach, this.reachExtended, this.sameBlockLimit, this.brushControls);
        this.body.w(BODY_WIDTH).h(this.computeBodyHeight());

        this.horizontalScroll = new UIScrollView(ScrollDirection.HORIZONTAL);

        this.horizontalScroll.h(this.computeBodyHeight());
        this.horizontalScroll.column(0).vertical(false).scroll().width(BODY_WIDTH).padding(0);
        this.horizontalScroll.add(this.body);

        UIScrollView verticalScroll = new UIScrollView();

        verticalScroll.full(this.content);
        verticalScroll.column(0).vertical().stretch().scroll().padding(0);
        verticalScroll.add(this.horizontalScroll);
        this.content.add(verticalScroll);

        this.helpIcon = new UIIcon(Icons.HELP, (b) -> {});
        this.helpIcon.tooltip(UIStructurePickerPanel::buildKeysHelpTip, 220, Direction.LEFT);
        this.helpIcon.relative(this).x(1F, -24).y(1F, -24).wh(20, 20);
        this.add(this.helpIcon);

        this.keys().register(Keys.UNDO, StructurePickerClient::undo).active(StructurePickerClient::canUndo);
        this.keys().register(Keys.REDO, StructurePickerClient::redo).active(StructurePickerClient::canRedo);

        this.updateShapeButtons();
        this.updateImportButtons();
        this.updateActionButtons();
        this.syncReachControls();
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
            StructurePickerMode mode = modes[i];
            int tint = MODE_TINTS[mode.index];
            boolean selected = mode == current;

            this.shapeButtons.get(i).color(selected ? tint : Colors.mulRGB(tint, MODE_INACTIVE_TINT));
            this.shapeButtons.get(i).outlined(selected);
        }
    }

    private void syncReachControls()
    {
        if (!this.reach.isFocused() && !this.reach.isDragging())
        {
            int value = BBSSettings.structurePickerReach.get();

            if ((int) this.reach.getValue() != value)
            {
                this.reach.setValue(value);
            }
        }

        if (!this.reachExtended.isFocused() && !this.reachExtended.isDragging())
        {
            int value = BBSSettings.structurePickerReachExtended.get();

            if ((int) this.reachExtended.getValue() != value)
            {
                this.reachExtended.setValue(value);
            }
        }
    }

    private static String buildKeysHelpTip()
    {
        return UIKeys.STRUCTURE_PICKER_KEYS_HELP_TIP.format(
            Keys.UNDO.getKeyCombo(),
            Keys.UNDO.label.get(),
            Keys.REDO.getKeyCombo(),
            Keys.REDO.label.get(),
            Keys.STRUCTURE_PICKER_CYCLE_PLANE.getKeyCombo(),
            Keys.STRUCTURE_PICKER_CYCLE_PLANE.label.get()
        ).get();
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

        this.applyBodyHeight();
    }

    private int computeBodyHeight()
    {
        int right = RIGHT_COLUMN_CORE;

        if (StructurePickerClient.getMode() == StructurePickerMode.SAME)
        {
            right += SLOT_SAME;
        }
        else if (StructurePickerClient.getMode() == StructurePickerMode.BRUSH)
        {
            right += SLOT_BRUSH;
        }

        return Math.max(MODE_COLUMN_HEIGHT, right);
    }

    private void applyBodyHeight()
    {
        int height = this.computeBodyHeight();

        this.body.h(height);
        this.horizontalScroll.h(height);
        this.content.resize();
    }

    private void updateImportButtons()
    {
        this.updateActionButtons();
    }

    private void updateActionButtons()
    {
        UIFilmPanel filmPanel = BBSModClient.getDashboard().getPanel(UIFilmPanel.class);
        boolean canImportFilm = filmPanel != null && filmPanel.getData() != null;
        boolean canApply = StructurePickerClient.canApplyScopedActions();

        this.modelBlockButton.setEnabled(canApply);
        this.importFilmButton.setEnabled(canApply && canImportFilm);
        this.removeSelectionButton.setEnabled(canApply);
        this.breakSelectionButton.setEnabled(canApply);
    }

    private void importSelection(boolean toModelBlock)
    {
        if (!StructurePickerClient.canApplyScopedActions())
        {
            return;
        }

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
        if (!StructurePickerClient.canApplyScopedActions())
        {
            return;
        }

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
        if (!StructurePickerClient.canApplyScopedActions())
        {
            return;
        }

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
