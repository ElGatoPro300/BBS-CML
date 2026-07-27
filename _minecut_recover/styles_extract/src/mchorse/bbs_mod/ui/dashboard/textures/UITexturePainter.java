package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.utils.undo.UndoManager;

import org.joml.Vector2i;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UITexturePainter extends UIElement
{
    private static final int SIDE_PANEL_WIDTH = 186;
    private static final int MODEL_PREVIEW_LEFT_WIDTH = 220;
    private static final int MODEL_PREVIEW_GAP = 6;

    public UITrackpad brightness;
    public UITrackpad brush;
    public UIElement headerToolbar;
    public UIElement sidePanel;
    public UIElement colorTabContent;
    public UIElement paletteTabContent;
    public UIElement mediaTabContent;
    public UIButton tabColor;
    public UIButton tabPalette;
    public UIButton tabImages;
    public UIButton tabLayers;
    public UIButton primarySlot;
    public UIButton secondarySlot;
    public UIElement layerRow;
    public UIElement imageRow;
    public UITrackpad layerOpacity;
    public UIButton selectTextureButton;
    public UIIcon addLayerButton;
    public UIScrollView imageRows;
    public UIScrollView layerRows;

    public UIColor primary;
    public UIColor secondary;
    public UITextureInlineColorPicker fixedColorPicker;

    public UITextureEditor main;
    public UITextureEditor reference;
    public UIElement modelPreviewArea;
    public UIFormRenderer modelPreview;
    public UIIcon toolBrush;
    public UIIcon toolEraser;
    public UIIcon toolPick;
    public UIIcon toolFill;
    public UIIcon toolSquare;
    public UIIcon toolCircle;

    private Supplier<Form> formPreviewSupplier;
    private final Set<Link> touchedPreviewTextures = new HashSet<>();
    private UIPixelsEditor.Tool activeTool = UIPixelsEditor.Tool.BRUSH;
    private UIPixelsEditor.BrushShape activeBrushShape = UIPixelsEditor.BrushShape.SQUARE;
    private boolean editingPrimary = true;
    private boolean topTabColor = true;
    private boolean bottomTabLayers = true;
    private final List<Link> imageTextures = new ArrayList<>();
    private final List<TextureLayer> layers = new ArrayList<>();
    private final List<Texture> layerPreviewTextures = new ArrayList<>();
    private final Map<Link, List<TextureLayer>> layersByTexture = new HashMap<>();
    private final Map<Link, Integer> selectedLayerByTexture = new HashMap<>();
    private int selectedImageIndex = -1;
    private int selectedLayerIndex = -1;
    private Texture layersCompositeTexture;
    private Pixels layersCompositePixels;
    private UIElement texturePickerPopup;

    private static class TextureLayer
    {
        public String name;
        public float opacity;
        public boolean visible;
        public Pixels pixels;
        public UndoManager<Pixels> undoManager;

        public TextureLayer(String name, float opacity, boolean visible, Pixels pixels, UndoManager<Pixels> undoManager)
        {
            this.name = name;
            this.opacity = opacity;
            this.visible = visible;
            this.pixels = pixels;
            this.undoManager = undoManager;
        }
    }

    private List<TextureLayer> copyLayers(List<TextureLayer> source)
    {
        List<TextureLayer> copy = new ArrayList<>();

        for (TextureLayer layer : source)
        {
            Pixels pixels = this.copyPixels(layer.pixels);

            if (pixels == null && layer.pixels != null && layer.pixels.width > 0 && layer.pixels.height > 0)
            {
                pixels = Pixels.fromSize(layer.pixels.width, layer.pixels.height);
            }

            copy.add(new TextureLayer(layer.name, layer.opacity, layer.visible, pixels, layer.undoManager));
        }

        return copy;
    }

    private Pixels copyPixels(Pixels pixels)
    {
        if (pixels == null || pixels.getBuffer() == null || pixels.width <= 0 || pixels.height <= 0)
        {
            return null;
        }

        Pixels copy = Pixels.fromSize(pixels.width, pixels.height);

        copy.draw(pixels, 0, 0, copy.width, copy.height);

        return copy;
    }

    private void clearLayerPreviewTextures()
    {
        for (Texture texture : this.layerPreviewTextures)
        {
            if (texture != null && texture.isValid())
            {
                texture.delete();
            }
        }

        this.layerPreviewTextures.clear();
    }

    private Texture createLayerPreviewTexture(TextureLayer layer)
    {
        if (layer == null || layer.pixels == null || layer.pixels.getBuffer() == null)
        {
            return null;
        }

        Texture previewTexture = new Texture();
        previewTexture.setFilter(GL11.GL_NEAREST);

        layer.pixels.rewindBuffer();
        previewTexture.bind();
        previewTexture.updateTexture(layer.pixels);
        this.layerPreviewTextures.add(previewTexture);

        return previewTexture;
    }

    private void saveCurrentTextureLayers()
    {
        Link texture = this.main.getTexture();

        if (texture == null)
        {
            return;
        }

        this.storeActiveLayerPixels();
        this.layersByTexture.put(texture, this.copyLayers(this.layers));

        if (this.selectedLayerIndex >= 0)
        {
            this.selectedLayerByTexture.put(texture, this.selectedLayerIndex);
        }
        else
        {
            this.selectedLayerByTexture.remove(texture);
        }
    }

    private void loadTextureLayers(Link texture)
    {
        this.layers.clear();
        this.selectedLayerIndex = -1;

        if (texture == null)
        {
            this.ensureDefaultLayer();

            return;
        }

        List<TextureLayer> storedLayers = this.layersByTexture.get(texture);

        if (storedLayers == null || storedLayers.isEmpty())
        {
            this.ensureDefaultLayer();
            this.layersByTexture.put(texture, this.copyLayers(this.layers));

            return;
        }

        this.layers.addAll(this.copyLayers(storedLayers));

        int selected = this.selectedLayerByTexture.getOrDefault(texture, this.layers.size() - 1);
        this.selectedLayerIndex = Math.max(0, Math.min(selected, this.layers.size() - 1));
        this.layerOpacity.setValue(Math.round(this.layers.get(this.selectedLayerIndex).opacity * 100F));
    }

    public UITexturePainter(Consumer<Link> saveCallback)
    {
        this.brightness = new UITrackpad();
        this.brightness.limit(0, 1).setValue(0.7);
        this.brightness.tooltip(UIKeys.TEXTURES_VIEWER_BRIGHTNESS, Direction.BOTTOM);
        this.brightness.w(52).maxW(52);

        this.brush = new UITrackpad((v) ->
        {
            int brushSize = Math.max(1, v.intValue());

            this.main.setBrushSize(brushSize);

            if (this.reference != null)
            {
                this.reference.setBrushSize(brushSize);
            }
        });
        this.brush.integer().limit(1, 32, true).setValue(1);
        this.brush.tooltip(UIKeys.TEXTURES_BRUSH_SIZE, Direction.BOTTOM);
        this.brush.w(40).maxW(40);

        this.primary = new UIColor((c) -> {}).noLabel();
        this.primary.direction(Direction.BOTTOM).h(20);
        this.secondary = new UIColor((c) -> {}).noLabel();
        this.secondary.direction(Direction.BOTTOM).wh(20, 20);

        this.primary.setColor(Colors.WHITE);
        this.secondary.setColor(0);

        this.toolBrush = new UIIcon(Icons.BRUSH, (b) -> this.setActiveTool(UIPixelsEditor.Tool.BRUSH))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolEraser = new UIIcon(Icons.ERASER, (b) -> this.setActiveTool(UIPixelsEditor.Tool.ERASER))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolPick = new UIIcon(Icons.DROPPER, (b) -> this.setActiveTool(UIPixelsEditor.Tool.PICK))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolFill = new UIIcon(Icons.DROP, (b) -> this.setActiveTool(UIPixelsEditor.Tool.FILL))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolSquare = new UIIcon(Icons.SQUARE, (b) -> this.setBrushShape(UIPixelsEditor.BrushShape.SQUARE))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolCircle = new UIIcon(Icons.CIRCLE, (b) -> this.setBrushShape(UIPixelsEditor.BrushShape.CIRCLE))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };

        this.toolBrush.tooltip(UIKeys.GENERAL_EDIT, Direction.BOTTOM);
        this.toolEraser.tooltip(UIKeys.TEXTURE_EDITOR_ERASE, Direction.BOTTOM);
        this.toolPick.tooltip(UIKeys.TEXTURES_KEYS_PICK, Direction.BOTTOM);
        this.toolFill.tooltip(UIKeys.TEXTURES_KEYS_FILL, Direction.BOTTOM);
        this.toolSquare.tooltip(UIKeys.KEYFRAMES_SHAPES_SQUARE, Direction.BOTTOM);
        this.toolCircle.tooltip(UIKeys.KEYFRAMES_SHAPES_CIRCLE, Direction.BOTTOM);

        this.main = new UITextureEditor().saveCallback(saveCallback);
        this.main.renderTextureSupplier(this::getComposedEditorTexture);
        this.main.savePixelsSupplier(this::getComposedSavePixels);
        this.configureEditor(this.main);
        this.main.full(this);
        this.main.undo.removeFromParent();
        this.main.redo.removeFromParent();
        this.main.resize.removeFromParent();
        this.main.extract.removeFromParent();
        this.main.save.removeFromParent();
        this.main.resize.tooltip(UIKeys.TEXTURES_RESIZE, Direction.BOTTOM);
        this.main.extract.tooltip(UIKeys.TEXTURES_EXTRACT_FRAMES_TITLE, Direction.BOTTOM);
        this.main.save.tooltip(UIKeys.TEXTURES_SAVE, Direction.BOTTOM);
        this.toolBrush.wh(20, 20).minW(20).maxW(20);
        this.toolEraser.wh(20, 20).minW(20).maxW(20);
        this.toolPick.wh(20, 20).minW(20).maxW(20);
        this.toolFill.wh(20, 20).minW(20).maxW(20);
        this.toolSquare.wh(20, 20).minW(20).maxW(20);
        this.toolCircle.wh(20, 20).minW(20).maxW(20);
        this.main.undo.wh(20, 20).minW(20).maxW(20);
        this.main.redo.wh(20, 20).minW(20).maxW(20);
        this.main.resize.wh(20, 20).minW(20).maxW(20);
        this.main.extract.wh(20, 20).minW(20).maxW(20);
        this.main.save.wh(20, 20).minW(20).maxW(20);
        this.headerToolbar = new UIElement();
        UIElement toolsGroup = UI.row(
            0,
            this.toolBrush,
            this.toolEraser,
            this.toolPick,
            this.toolFill.marginRight(8),
            this.toolSquare,
            this.toolCircle.marginRight(8),
            this.main.undo,
            this.main.redo,
            this.main.resize,
            this.main.extract,
            this.main.save
        );
        toolsGroup.row(0).width(20);
        toolsGroup.relative(this.headerToolbar).xy(0, 0).h(20).w(1F, -108);

        UIElement controlsGroup = UI.row(0, this.brush.marginRight(4), this.brightness);
        controlsGroup.relative(this.headerToolbar).x(1F, -96).y(0).wh(96, 20);

        this.headerToolbar.add(toolsGroup, controlsGroup);
        this.updateToolButtons();

        this.add(this.main);
        this.setupSidePanel();

        this.modelPreviewArea = new UIElement();
        this.modelPreview = new UIFormRenderer();
        this.modelPreview.grid = false;
        this.modelPreview.setDistance(14);
        this.modelPreview.setPosition(0F, 1F, 0F);
        this.modelPreview.setRotation(34F, 8F);
        this.modelPreview.relative(this.modelPreviewArea).full(this.modelPreviewArea);
        this.modelPreviewArea.add(this.modelPreview);
        this.modelPreviewArea.setVisible(false);
        this.add(this.modelPreviewArea);

        IKey category = UIKeys.TEXTURES_KEYS_CATEGORY;

        this.keys().register(Keys.PIXEL_SWAP, this::swapColors).inside().category(category);
        this.keys().register(Keys.PIXEL_PICK, this::pickColor).inside().category(category);
        this.keys().register(Keys.PIXEL_FILL, this::fillColor).inside().category(category);
    }

    private void setupSidePanel()
    {
        this.sidePanel = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A25);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50);
                super.render(context);
            }
        };
        this.sidePanel.relative(this).x(1F, -SIDE_PANEL_WIDTH).y(0).w(SIDE_PANEL_WIDTH).h(1F);

        this.fixedColorPicker = new UITextureInlineColorPicker((color) ->
        {
            if (this.editingPrimary)
            {
                this.primary.setColor(color);
            }
            else
            {
                this.secondary.setColor(color);
            }

            this.updateColorSlots();
        });
        this.fixedColorPicker.setup(0, 0);
        this.fixedColorPicker.relative(this.sidePanel).xy(8, 34).w(1F, -16);
        this.fixedColorPicker.h(164);

        this.tabColor = new UIButton(UIKeys.TEXTURE_PAINTER_TAB_COLOR, (b) -> this.setTopTab(true));
        this.tabPalette = new UIButton(UIKeys.TEXTURE_PAINTER_TAB_PALETTE, (b) -> this.setTopTab(false));
        this.tabColor.relative(this.sidePanel).xy(8, 8).w(0.5F, -10).h(20);
        this.tabPalette.relative(this.sidePanel).x(0.5F, 2).y(8).w(0.5F, -10).h(20);

        this.colorTabContent = new UIElement();
        this.colorTabContent.relative(this.sidePanel).xy(8, 34).w(1F, -16).h(164);
        this.fixedColorPicker.relative(this.colorTabContent).x(0).y(0).w(1F).h(1F);

        this.primarySlot = new UIButton(IKey.EMPTY, (b) -> this.setEditingPrimary(true))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (UITexturePainter.this.editingPrimary)
                {
                    int outline = 0xff000000 | BBSSettings.primaryColor.get();
                    context.batcher.outline(this.area.x - 1, this.area.y - 1, this.area.ex() + 1, this.area.ey() + 1, outline);
                }
            }
        };
        this.primarySlot.wh(12, 12).tooltip(UIKeys.TEXTURE_PAINTER_PRIMARY_COLOR, Direction.TOP);
        this.primarySlot.relative(this.colorTabContent).xy(6, 6);

        this.secondarySlot = new UIButton(IKey.EMPTY, (b) -> this.setEditingPrimary(false))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (!UITexturePainter.this.editingPrimary)
                {
                    int outline = 0xff000000 | BBSSettings.primaryColor.get();
                    context.batcher.outline(this.area.x - 1, this.area.y - 1, this.area.ex() + 1, this.area.ey() + 1, outline);
                }
            }
        };
        this.secondarySlot.wh(12, 12).tooltip(UIKeys.TEXTURE_PAINTER_SECONDARY_COLOR, Direction.TOP);
        this.secondarySlot.relative(this.colorTabContent).xy(12, 12);

        this.colorTabContent.add(this.fixedColorPicker, this.secondarySlot, this.primarySlot);

        this.paletteTabContent = new UIElement();
        this.paletteTabContent.relative(this.sidePanel).xy(8, 34).w(1F, -16).h(56);

        UIElement paletteRowOne = new UIElement();
        UIElement paletteRowTwo = new UIElement();
        paletteRowOne.relative(this.paletteTabContent).xy(0, 0).w(1F).h(18).row(2).resize();
        paletteRowTwo.relative(this.paletteTabContent).xy(0, 20).w(1F).h(18).row(2).resize();

        int[] swatches = new int[] {
            0x000000, 0xffffff, 0x8f3f20, 0xd87f33, 0xff0000, 0xff55ff,
            0x00aa00, 0x55ffff, 0x3c44aa, 0x8932b8, 0xa0a0a0, 0x5a5a5a
        };

        for (int i = 0; i < swatches.length; i++)
        {
            final int color = swatches[i];
            UIButton swatch = new UIButton(IKey.EMPTY, (b) ->
            {
                if (this.editingPrimary)
                {
                    this.primary.setColor(color);
                }
                else
                {
                    this.secondary.setColor(color);
                }

                this.fixedColorPicker.setColor(this.getActiveColor());
                this.updateColorSlots();
            });

            swatch.color(color).background(true).wh(18, 18).tooltip(IKey.constant(String.format("#%06X", color)), Direction.TOP);

            if (i < 6)
            {
                paletteRowOne.add(swatch);
            }
            else
            {
                paletteRowTwo.add(swatch);
            }
        }

        this.paletteTabContent.add(paletteRowOne, paletteRowTwo);

        this.tabImages = new UIButton(UIKeys.TEXTURE_PAINTER_TAB_IMAGES, (b) -> this.setBottomTab(false));
        this.tabLayers = new UIButton(UIKeys.TEXTURE_PAINTER_TAB_LAYERS, (b) -> this.setBottomTab(true));
        this.tabImages.relative(this.sidePanel).x(8).y(206).w(0.5F, -10).h(18);
        this.tabLayers.relative(this.sidePanel).x(0.5F, 2).y(206).w(0.5F, -10).h(18);

        this.mediaTabContent = new UIElement();
        this.mediaTabContent.relative(this.sidePanel).x(8).y(226).w(1F, -16).h(1F, -234);

        this.imageRow = new UIElement();
        this.imageRow.relative(this.mediaTabContent).full(this.mediaTabContent);

        this.selectTextureButton = new UIButton(UIKeys.TEXTURE_PICK_TEXTURE, (b) -> this.openTextureSelector());
        this.selectTextureButton.relative(this.imageRow).xy(0, 0).w(1F).h(20).tooltip(UIKeys.TEXTURE_PAINTER_OPEN_TEXTURE_PICKER, Direction.BOTTOM);

        this.imageRows = UI.scrollView(2, 0);
        this.imageRows.relative(this.imageRow).xy(0, 24).w(1F).h(1F, -24);

        this.imageRow.add(this.selectTextureButton, this.imageRows);

        this.layerRow = new UIElement();
        this.layerRow.relative(this.mediaTabContent).full(this.mediaTabContent);

        this.addLayerButton = new UIIcon(Icons.ADD, (b) -> this.addLayer());
        this.addLayerButton.wh(20, 20).tooltip(UIKeys.TEXTURE_PAINTER_ADD_LAYER, Direction.BOTTOM);
        this.addLayerButton.relative(this.layerRow).xy(0, 0);

        this.layerOpacity = new UITrackpad((v) ->
        {
            if (this.selectedLayerIndex >= 0 && this.selectedLayerIndex < this.layers.size())
            {
                float opacity = Math.max(0F, Math.min(1F, v.floatValue() / 100F));
                this.layers.get(this.selectedLayerIndex).opacity = opacity;
                this.refreshLayerRows();
            }
        });
        this.layerOpacity.integer().limit(0, 100, true);
        this.layerOpacity.setValue(100);
        this.layerOpacity.tooltip(UIKeys.TEXTURE_PAINTER_LAYER_OPACITY, Direction.BOTTOM);
        this.layerOpacity.relative(this.layerRow).x(1F, -58).y(0).w(58).h(20);

        this.layerRows = UI.scrollView(2, 0);
        this.layerRows.relative(this.layerRow).xy(0, 24).w(1F).h(1F, -24);

        this.layerRow.add(this.addLayerButton, this.layerOpacity, this.layerRows);
        this.mediaTabContent.add(this.imageRow, this.layerRow);
        this.sidePanel.add(
            this.tabColor,
            this.tabPalette,
            this.colorTabContent,
            this.paletteTabContent,
            this.tabImages,
            this.tabLayers,
            this.mediaTabContent
        );
        this.fixedColorPicker.setColor(this.primary.picker.color.getRGBColor());
        this.setTopTab(true);
        this.setBottomTab(true);
        this.ensureDefaultLayer();
        this.refreshLayerRows();
        this.updateColorSlots();
        this.add(this.sidePanel);
    }

    public UITexturePainter withFormPreview(Supplier<Form> supplier)
    {
        this.formPreviewSupplier = supplier;
        this.modelPreviewArea.setVisible(supplier != null);
        this.refreshModelPreview();
        this.updateEditorsLayout();
        this.resize();

        return this;
    }

    private void swapColors()
    {
        int swap = this.primary.picker.color.getRGBColor();

        this.primary.setColor(this.secondary.picker.color.getRGBColor());
        this.secondary.setColor(swap);
        this.fixedColorPicker.setColor(this.getActiveColor());
        this.updateColorSlots();
    }

    private int getActiveColor()
    {
        return this.editingPrimary ? this.primary.picker.color.getRGBColor() : this.secondary.picker.color.getRGBColor();
    }

    private Color getActiveBrushColor()
    {
        return this.editingPrimary ? this.primary.picker.color : this.secondary.picker.color;
    }

    private void setEditingPrimary(boolean editingPrimary)
    {
        this.editingPrimary = editingPrimary;
        this.fixedColorPicker.setColor(this.getActiveColor());
        this.updateColorSlots();
    }

    private void updateColorSlots()
    {
        this.primarySlot.color(this.primary.picker.color.getRGBColor()).background(true);
        this.secondarySlot.color(this.secondary.picker.color.getRGBColor()).background(true);
    }

    private void setTopTab(boolean color)
    {
        this.topTabColor = color;
        this.colorTabContent.setVisible(color);
        this.paletteTabContent.setVisible(!color);

        this.tabColor.background(color).textColor(color ? Colors.WHITE : 0xb0b0b0, false);
        this.tabPalette.background(!color).textColor(color ? 0xb0b0b0 : Colors.WHITE, false);
    }

    private void setBottomTab(boolean layers)
    {
        this.bottomTabLayers = layers;
        this.layerRow.setVisible(layers);
        this.imageRow.setVisible(!layers);

        this.tabLayers.background(layers).textColor(layers ? Colors.WHITE : 0xb0b0b0, false);
        this.tabImages.background(!layers).textColor(layers ? 0xb0b0b0 : Colors.WHITE, false);
    }

    private void openTextureSelector()
    {
        if (this.texturePickerPopup != null && this.texturePickerPopup.hasParent())
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null || context.menu == null || context.menu.overlay == null)
        {
            return;
        }

        UIElement overlay = context.menu.overlay;
        UIElement popup = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A50);
                super.render(context);
            }
        };
        popup.full(overlay);
        popup.markContainer().eventPropagataion(EventPropagation.BLOCK);

        UIElement content = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A25);
                context.batcher.outline(this.area.x - 1, this.area.y - 1, this.area.ex() + 1, this.area.ey() + 1, Colors.A100);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                super.render(context);
            }
        };
        content.relative(popup).set(20, 20, 0, 0).w(1F, -40).h(1F, -40);

        UITexturePicker picker = new UITexturePicker((link) ->
        {
            this.closeTextureSelectorPopup();

            if (link == null)
            {
                return;
            }

            this.addImageTexture(link, true);
            this.fillTexture(link);
        });
        picker.disablePixelEditor();
        picker.disableMultiSkin();
        content.add(picker);
        popup.add(content);
        overlay.add(popup);
        popup.resize();
        content.resize();
        picker.full(content);
        picker.resize();
        picker.fill(this.main.getTexture());
        this.texturePickerPopup = popup;
    }

    private void closeTextureSelectorPopup()
    {
        if (this.texturePickerPopup != null)
        {
            this.texturePickerPopup.removeFromParent();
            this.texturePickerPopup = null;
        }
    }

    private void ensureDefaultLayer()
    {
        if (this.layers.isEmpty())
        {
            this.layers.add(new TextureLayer("layer", 1F, true, this.main.getPixels(), this.main.exportUndoManager()));
            this.selectedLayerIndex = 0;
            this.layerOpacity.setValue(100);
        }
    }

    private void addLayer()
    {
        this.storeActiveLayerPixels();

        String name = this.layers.isEmpty() ? "layer" : "layer_" + this.layers.size();
        Pixels pixels = this.createTransparentLayerPixels();
        this.layers.add(new TextureLayer(name, 1F, true, pixels, null));
        this.selectedLayerIndex = this.layers.size() - 1;
        this.layerOpacity.setValue(100);
        this.loadSelectedLayerPixels();
        this.refreshLayerRows();
    }

    private Pixels createTransparentLayerPixels()
    {
        Pixels current = this.main.getPixels();

        if (current == null)
        {
            return null;
        }

        return Pixels.fromSize(current.width, current.height);
    }

    private void storeActiveLayerPixels()
    {
        if (this.selectedLayerIndex < 0 || this.selectedLayerIndex >= this.layers.size())
        {
            return;
        }

        TextureLayer activeLayer = this.layers.get(this.selectedLayerIndex);

        activeLayer.pixels = this.main.getPixels();
        activeLayer.undoManager = this.main.exportUndoManager();
    }

    private void loadSelectedLayerPixels()
    {
        if (this.selectedLayerIndex < 0 || this.selectedLayerIndex >= this.layers.size())
        {
            return;
        }

        TextureLayer layer = this.layers.get(this.selectedLayerIndex);

        if (layer.pixels == null)
        {
            layer.pixels = this.createTransparentLayerPixels();
        }

        if (layer.pixels != null)
        {
            this.main.fillPixels(layer.pixels, true);
            this.main.setEditing(true);
            this.main.importUndoManager(layer.undoManager);
            layer.undoManager = this.main.exportUndoManager();
        }
    }

    private void selectLayer(int index)
    {
        if (index < 0 || index >= this.layers.size())
        {
            return;
        }

        this.storeActiveLayerPixels();
        this.selectedLayerIndex = index;
        this.layerOpacity.setValue(Math.round(this.layers.get(index).opacity * 100F));
        this.loadSelectedLayerPixels();
        this.refreshLayerRows();
    }

    private void toggleLayerVisibility(int index)
    {
        if (index < 0 || index >= this.layers.size())
        {
            return;
        }

        TextureLayer layer = this.layers.get(index);
        layer.visible = !layer.visible;

        if (!layer.visible && this.selectedLayerIndex == index)
        {
            int next = this.findVisibleLayerIndex();

            if (next >= 0)
            {
                this.selectLayer(next);

                return;
            }
        }

        this.refreshLayerRows();
    }

    private int findVisibleLayerIndex()
    {
        for (int i = this.layers.size() - 1; i >= 0; i--)
        {
            if (this.layers.get(i).visible)
            {
                return i;
            }
        }

        return -1;
    }

    private void addImageTexture(Link texture, boolean select)
    {
        if (texture == null)
        {
            return;
        }

        for (int i = 0; i < this.imageTextures.size(); i++)
        {
            if (texture.equals(this.imageTextures.get(i)))
            {
                if (select)
                {
                    this.selectedImageIndex = i;
                }

                this.refreshImageRows();

                return;
            }
        }

        this.imageTextures.add(texture);

        if (select)
        {
            this.selectedImageIndex = this.imageTextures.size() - 1;
        }

        this.refreshImageRows();
    }

    private void refreshImageRows()
    {
        if (this.imageRows == null)
        {
            return;
        }

        this.imageRows.removeAll();

        for (int i = 0; i < this.imageTextures.size(); i++)
        {
            final int index = i;
            Link texture = this.imageTextures.get(i);
            String name = StringUtils.fileName(texture.path);

            if (name == null || name.isEmpty())
            {
                name = texture.toString();
            }

            UIElement row = new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    boolean selected = index == UITexturePainter.this.selectedImageIndex;
                    int color = selected ? (Colors.A50 | BBSSettings.primaryColor.get()) : Colors.A25;

                    this.area.render(context.batcher, color);
                    super.render(context);
                }
            };
            row.relative(this.imageRows).x(0).y(i * 22).w(1F, -8).h(20);

            UIElement preview = new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    super.render(context);

                    context.batcher.iconArea(Icons.CHECKBOARD, Colors.A50, this.area.x, this.area.y, this.area.w, this.area.h);
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A100);

                    Texture thumbnail = BBSModClient.getTextures().getTexture(texture);

                    if (thumbnail != null && thumbnail.isValid())
                    {
                        context.batcher.fullTexturedBox(thumbnail, this.area.x, this.area.y, this.area.w, this.area.h);
                    }
                }
            };
            preview.relative(row).xy(2, 2).wh(16, 16);

            UIButton select = new UIButton(IKey.constant(name), (b) ->
            {
                this.selectedImageIndex = index;
                this.fillTexture(this.imageTextures.get(index));
            });
            select.relative(row).x(20).y(0).w(1F, -20).h(20);
            select.background(false).textColor(index == this.selectedImageIndex ? Colors.WHITE : 0xd0d0d0, false);
            select.tooltip(IKey.constant(texture.toString()), Direction.BOTTOM);

            row.add(preview, select);
            this.imageRows.add(row);
        }

        this.imageRows.resize();
    }

    private void refreshLayerRows()
    {
        if (this.layerRows == null)
        {
            return;
        }

        this.clearLayerPreviewTextures();
        this.layerRows.removeAll();

        int count = this.layers.size();

        for (int rowIndex = 0; rowIndex < count; rowIndex++)
        {
            final int index = count - 1 - rowIndex;
            TextureLayer layer = this.layers.get(index);
            int opacity = Math.round(layer.opacity * 100F);
            String text = (rowIndex + 1) + ". " + layer.name + " (" + opacity + "%)";

            UIElement row = new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    boolean selected = index == UITexturePainter.this.selectedLayerIndex;
                    int color = selected ? (Colors.A50 | BBSSettings.primaryColor.get()) : Colors.A25;

                    this.area.render(context.batcher, color);
                    super.render(context);
                }
            };
            row.relative(this.layerRows).x(0).y(rowIndex * 26).w(1F, -8).h(24);
            Texture previewTexture = this.createLayerPreviewTexture(layer);

            UIElement preview = new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    super.render(context);

                    context.batcher.iconArea(Icons.CHECKBOARD, Colors.A50, this.area.x, this.area.y, this.area.w, this.area.h);
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A100);

                    if (previewTexture != null && previewTexture.isValid())
                    {
                        int alpha = Math.max(0, Math.min(255, Math.round(layer.opacity * 255F)));

                        context.batcher.fullTexturedBox(previewTexture, (alpha << 24) | 0x00ffffff, this.area.x, this.area.y, this.area.w, this.area.h);
                    }
                }
            };
            preview.relative(row).xy(2, 2).wh(20, 20);

            UIButton select = new UIButton(IKey.constant(text), (b) -> this.selectLayer(index));
            select.relative(row).x(24).y(0).w(1F, -44).h(24);
            select.background(false).textColor(index == this.selectedLayerIndex ? Colors.WHITE : 0xd0d0d0, false);

            UIIcon visibility = new UIIcon(() -> layer.visible ? Icons.VISIBLE : Icons.INVISIBLE, (b) -> this.toggleLayerVisibility(index));
            visibility.relative(row).x(1F, -20).y(0).wh(20, 20);
            visibility.tooltip(layer.visible ? UIKeys.TEXTURE_PAINTER_HIDE_LAYER : UIKeys.TEXTURE_PAINTER_SHOW_LAYER, Direction.LEFT);

            row.add(preview, select, visibility);
            this.layerRows.add(row);
        }

        this.layerRows.resize();
    }

    private Pixels composeVisibleLayers()
    {
        Pixels base = this.main.getPixels();

        if (base == null)
        {
            return null;
        }

        if (this.layersCompositePixels == null || this.layersCompositePixels.width != base.width || this.layersCompositePixels.height != base.height)
        {
            if (this.layersCompositePixels != null)
            {
                this.layersCompositePixels.delete();
            }

            this.layersCompositePixels = Pixels.fromSize(base.width, base.height);
        }

        Pixels composed = this.layersCompositePixels;
        composed.drawRect(0, 0, composed.width, composed.height, 0);
        Color output = new Color();

        for (int i = 0; i < this.layers.size(); i++)
        {
            TextureLayer layer = this.layers.get(i);

            if (!layer.visible || layer.opacity <= 0F || layer.pixels == null)
            {
                continue;
            }

            Pixels source = layer.pixels;

            if (source.getBuffer() == null)
            {
                continue;
            }

            for (int x = 0; x < composed.width; x++)
            {
                for (int y = 0; y < composed.height; y++)
                {
                    Color src = source.getColor(x, y);

                    if (src == null)
                    {
                        continue;
                    }

                    float alpha = src.a * layer.opacity;

                    if (alpha <= 0F)
                    {
                        continue;
                    }

                    Color dst = composed.getColor(x, y);
                    float outA = alpha + dst.a * (1F - alpha);

                    if (outA <= 0F)
                    {
                        continue;
                    }

                    output.a = outA;
                    output.r = (src.r * alpha + dst.r * dst.a * (1F - alpha)) / outA;
                    output.g = (src.g * alpha + dst.g * dst.a * (1F - alpha)) / outA;
                    output.b = (src.b * alpha + dst.b * dst.a * (1F - alpha)) / outA;
                    composed.setColor(x, y, output);
                }
            }
        }

        return composed;
    }

    private Texture getComposedEditorTexture()
    {
        this.storeActiveLayerPixels();
        Pixels composed = this.composeVisibleLayers();

        if (composed == null)
        {
            return null;
        }

        if (this.layersCompositeTexture == null || !this.layersCompositeTexture.isValid())
        {
            this.layersCompositeTexture = new Texture();
            this.layersCompositeTexture.setFilter(GL11.GL_NEAREST);
        }

        composed.rewindBuffer();
        this.layersCompositeTexture.bind();
        this.layersCompositeTexture.updateTexture(composed);

        return this.layersCompositeTexture;
    }

    private Pixels getComposedSavePixels()
    {
        this.storeActiveLayerPixels();

        return this.composeVisibleLayers();
    }

    private UITextureEditor getHoverEditor(UIContext context)
    {
        return this.main.area.isInside(context) ? this.main : (this.reference != null && this.reference.area.isInside(context) ? this.reference : null);
    }

    private void pickColor()
    {
        UIContext context = this.getContext();
        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            if (color != null)
            {
                if (this.editingPrimary)
                {
                    this.primary.setColor(color.getRGBColor());
                }
                else
                {
                    this.secondary.setColor(color.getRGBColor());
                }

                this.fixedColorPicker.setColor(this.getActiveColor());
                this.updateColorSlots();
            }
        }
    }

    private void fillColor()
    {
        UIContext context = this.getContext();
        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);

            editor.fillColor(pixel, this.getActiveBrushColor(), Window.isShiftPressed());
        }
    }

    private void configureEditor(UITextureEditor editor)
    {
        editor
            .colorSupplier(this::getActiveBrushColor)
            .backgroundSupplier(() -> (float) this.brightness.getValue())
            .onPickColor((color) ->
            {
                if (this.editingPrimary)
                {
                    this.primary.setColor(color.getRGBColor());
                }
                else
                {
                    this.secondary.setColor(color.getRGBColor());
                }

                this.fixedColorPicker.setColor(this.getActiveColor());
                this.updateColorSlots();
            })
            .onFillColor((pixel, replace) -> editor.fillColor(pixel, this.getActiveBrushColor(), replace))
            .setTool(this.activeTool)
            .setBrushShape(this.activeBrushShape)
            .useExternalToolbar();
        editor.setBrushSize((int) this.brush.getValue());
    }

    private void setActiveTool(UIPixelsEditor.Tool tool)
    {
        this.activeTool = tool == null ? UIPixelsEditor.Tool.BRUSH : tool;

        this.main.setTool(this.activeTool);

        if (this.reference != null)
        {
            this.reference.setTool(this.activeTool);
        }

        this.updateToolButtons();
    }

    private void setBrushShape(UIPixelsEditor.BrushShape brushShape)
    {
        this.activeBrushShape = brushShape == null ? UIPixelsEditor.BrushShape.SQUARE : brushShape;

        this.main.setBrushShape(this.activeBrushShape);

        if (this.reference != null)
        {
            this.reference.setBrushShape(this.activeBrushShape);
        }

        this.updateToolButtons();
    }

    private void updateToolButtons()
    {
        this.toolBrush.active(this.activeTool == UIPixelsEditor.Tool.BRUSH);
        this.toolEraser.active(this.activeTool == UIPixelsEditor.Tool.ERASER);
        this.toolPick.active(this.activeTool == UIPixelsEditor.Tool.PICK);
        this.toolFill.active(this.activeTool == UIPixelsEditor.Tool.FILL);
        this.toolSquare.active(this.activeBrushShape == UIPixelsEditor.BrushShape.SQUARE);
        this.toolCircle.active(this.activeBrushShape == UIPixelsEditor.BrushShape.CIRCLE);
    }

    public void fillTexture(Link current)
    {
        this.saveCurrentTextureLayers();
        this.main.fillTexture(current);
        this.main.setEditing(true);
        this.addImageTexture(current, true);
        this.loadTextureLayers(current);
        this.loadSelectedLayerPixels();
        this.refreshLayerRows();
        this.saveCurrentTextureLayers();
        this.refreshModelPreview();
    }

    private void refreshModelPreview()
    {
        if (this.formPreviewSupplier == null)
        {
            this.modelPreview.form = null;

            return;
        }

        Form source = this.formPreviewSupplier.get();
        this.modelPreview.form = source == null ? null : FormUtils.copy(source);
    }

    private void updateEditorsLayout()
    {
        boolean sidePanelVisible = this.reference == null || this.modelPreviewArea.isVisible();
        this.sidePanel.setVisible(sidePanelVisible);

        if (this.modelPreviewArea.isVisible())
        {
            this.modelPreviewArea.relative(this).x(0).y(6).w(MODEL_PREVIEW_LEFT_WIDTH).h(1F, -12);
            this.sidePanel.relative(this).x(1F, -SIDE_PANEL_WIDTH).y(0).w(SIDE_PANEL_WIDTH).h(1F);
            this.main.relative(this)
                .xy(MODEL_PREVIEW_LEFT_WIDTH + MODEL_PREVIEW_GAP, 0)
                .w(1F, -(SIDE_PANEL_WIDTH + MODEL_PREVIEW_LEFT_WIDTH + MODEL_PREVIEW_GAP + 4))
                .h(1F);

            if (this.reference != null)
            {
                this.reference.setVisible(false);
            }

            return;
        }

        if (this.reference == null)
        {
            if (sidePanelVisible)
            {
                this.main.relative(this).xy(0, 0).w(1F, -(SIDE_PANEL_WIDTH + 4)).h(1F);
                this.sidePanel.relative(this).x(1F, -SIDE_PANEL_WIDTH).y(0).w(SIDE_PANEL_WIDTH).h(1F);
            }
            else
            {
                this.main.relative(this).xy(0, 0).w(1F).h(1F);
            }
        }
        else
        {
            this.main.relative(this).xy(0, 0).w(0.5F).h(1F);
            this.reference.relative(this).xy(0.5F, 0).w(0.5F).h(1F);
            this.reference.setVisible(true);
        }
    }

    public UIElement getHeaderToolbar()
    {
        return this.headerToolbar;
    }

    private boolean updatePreviewTexture(TextureManager manager, Link textureLink, Pixels pixels)
    {
        if (manager == null)
        {
            return false;
        }

        Texture texture = manager.getTexture(textureLink, GL11.GL_NEAREST, true);

        if (texture == null || texture == manager.getError())
        {
            return false;
        }

        pixels.rewindBuffer();
        texture.bind();
        texture.updateTexture(pixels);
        this.touchedPreviewTextures.add(textureLink);

        return true;
    }

    private void updateLiveModelPreviewTexture(UIContext context)
    {
        if (!this.modelPreviewArea.isVisible() || this.formPreviewSupplier == null)
        {
            return;
        }

        Link textureLink = this.main.getTexture();
        this.storeActiveLayerPixels();
        Pixels pixels = this.composeVisibleLayers();

        if (textureLink == null || pixels == null)
        {
            return;
        }

        if (this.modelPreview.form instanceof ModelForm modelForm)
        {
            modelForm.texture.set(textureLink);
        }

        boolean updated = this.updatePreviewTexture(context == null ? null : context.render.getTextures(), textureLink, pixels);

        if (context == null || context.render.getTextures() != BBSModClient.getTextures())
        {
            updated = this.updatePreviewTexture(BBSModClient.getTextures(), textureLink, pixels) || updated;
        }

        if (!updated)
        {
            this.updatePreviewTexture(BBSModClient.getTextures(), textureLink, pixels);
        }
    }

    public void discardPreviewTextureChanges()
    {
        for (Link link : this.touchedPreviewTextures)
        {
            BBSModClient.getTextures().delete(link);
        }

        this.touchedPreviewTextures.clear();
    }

    @Override
    public void render(UIContext context)
    {
        this.updateEditorsLayout();
        this.updateLiveModelPreviewTexture(context);

        if (this.modelPreviewArea.isVisible())
        {
            this.modelPreviewArea.area.render(context.batcher, Colors.A25);
            context.batcher.outline(this.modelPreviewArea.area.x, this.modelPreviewArea.area.y, this.modelPreviewArea.area.ex(), this.modelPreviewArea.area.ey(), Colors.A50);
        }

        super.render(context);

        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            int r = 0;
            int g = 0;
            int b = 0;
            int a = 0;

            if (color != null)
            {
                r = (int) Math.floor(color.r * 255);
                g = (int) Math.floor(color.g * 255);
                b = (int) Math.floor(color.b * 255);
                a = (int) Math.floor(color.a * 255);
            }

            String[] information = {
                editor.getPixels().width + "x" + editor.getPixels().height + " (" + pixel.x + ", " + pixel.y + ")",
                "\u00A7cR\u00A7aG\u00A79B\u00A7rA (" + r + ", " + g + ", " + b + ", " + a + ")",
                "Brush " + editor.getBrushSize() + "x" + editor.getBrushSize() + " " + (editor.getBrushShape() == UIPixelsEditor.BrushShape.CIRCLE ? "Circle" : "Square"),
            };

            int x = this.area.x + 10;
            int y = this.area.ey() - context.batcher.getFont().getHeight() - 10 - (information.length - 1)* 14;

            for (String line : information)
            {
                context.batcher.textCard(line, x, y);

                y += 14;
            }
        }
    }
}
