package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;

import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UITexturePainter extends UIElement
{
    private static List<Document> documents = new ArrayList<>();
    private static int currentIndex = -1;

    public static final class Document
    {
        public Link link;
        public final Pixels pixels;
        public final UITextureEditor editor;

        public Document(Link link, Pixels pixels, UITextureEditor editor)
        {
            this.link = link;
            this.pixels = pixels;
            this.editor = editor;
        }
    }

    public UITrackpad brightness;
    public UITrackpad brushSize;
    public UIElement savebar;

    public UIColor primary;
    public UIColor secondary;

    public UIIcon undoIcon;
    public UIIcon redoIcon;
    public UIIcon saveIcon;
    public UIIcon resizeIcon;
    public UIIcon extractIcon;

    private UITextureTabs tabs;
    private UIElement content;
    private Consumer<Link> saveCallback;

    public UITexturePainter(Consumer<Link> saveCallback)
    {
        this.saveCallback = saveCallback;

        this.brightness = new UITrackpad();
        this.brightness.limit(0, 1).setValue(0.7);
        this.brightness.tooltip(UIKeys.TEXTURES_VIEWER_BRIGHTNESS, Direction.TOP);
        this.brightness.relative(this).x(1F, -10).y(1F, -10).w(130).anchor(1F, 1F);

        this.brushSize = new UITrackpad((v) -> this.setBrushSize(v.intValue()));
        this.brushSize.integer().limit(1, 64, true).setValue(1);
        this.brushSize.tooltip(UIKeys.TEXTURES_BRUSH_SIZE, Direction.TOP);
        this.brushSize.relative(this).x(1F, -10).y(1F, -40).w(130).anchor(1F, 1F);

        this.tabs = new UITextureTabs(this);
        this.tabs.relative(this).w(1F).h(UITextureTabs.TABS_HEIGHT_PX);

        this.content = new UIElement();
        this.content.relative(this.tabs).y(1F).w(1F).hTo(this.area, 1F);

        this.savebar = new UIElement();
        this.savebar.relative(this.content).h(UIConstants.CONTROL_HEIGHT + UIConstants.MARGIN * 2).row(UIConstants.MARGIN).resize().height(UIConstants.CONTROL_HEIGHT).padding(UIConstants.MARGIN);

        this.primary = new UIColor((c) -> {}).noLabel();
        this.primary.direction(Direction.RIGHT).w((int) (UIConstants.CONTROL_HEIGHT * 1.5F)).h(UIConstants.CONTROL_HEIGHT);
        this.primary.setColor(0);
        this.secondary = new UIColor((c) -> {}).noLabel();
        this.secondary.direction(Direction.RIGHT).w((int) (UIConstants.CONTROL_HEIGHT * 1.5F)).h(UIConstants.CONTROL_HEIGHT);
        this.secondary.setColor(Colors.WHITE);

        this.resizeIcon = new UIIcon(Icons.FULLSCREEN, (b) ->
        {
            UITextureEditor ed = this.getCurrentEditor();
            if (ed != null) ed.openResizeOverlay();
        });
        this.resizeIcon.tooltip(UIKeys.TEXTURES_RESIZE, Direction.BOTTOM);
        this.extractIcon = new UIIcon(Icons.UPLOAD, (b) ->
        {
            UITextureEditor ed = this.getCurrentEditor();
            if (ed != null) ed.openExtractOverlay();
        });
        this.extractIcon.tooltip(UIKeys.TEXTURES_EXTRACT_FRAMES_TITLE, Direction.BOTTOM);

        this.undoIcon = new UIIcon(Icons.UNDO, (b) ->
        {
            UITextureEditor ed = this.getCurrentEditor();
            if (ed != null) ed.undo();
        });
        this.undoIcon.tooltip(UIKeys.TEXTURES_KEYS_UNDO, Direction.BOTTOM);
        this.redoIcon = new UIIcon(Icons.REDO, (b) ->
        {
            UITextureEditor ed = this.getCurrentEditor();
            if (ed != null) ed.redo();
        });
        this.redoIcon.tooltip(UIKeys.TEXTURES_KEYS_REDO, Direction.BOTTOM);

        this.saveIcon = new UIIcon(() ->
        {
            UITextureEditor ed = this.getCurrentEditor();
            return ed != null && ed.isDirty() ? Icons.SAVE : Icons.SAVED;
        }, (b) ->
        {
            UITextureEditor ed = this.getCurrentEditor();
            if (ed != null) ed.openSaveOverlay();
        });

        this.savebar.add(this.primary, this.secondary, this.undoIcon, this.redoIcon, this.saveIcon, this.resizeIcon, this.extractIcon);
        this.content.add(this.savebar);
        this.add(this.tabs, this.content);
        this.add(this.brightness, this.brushSize);

        this.syncTabs();
        this.showCurrentEditor();

        IKey category = UIKeys.TEXTURES_KEYS_CATEGORY;

        this.keys().register(Keys.PIXEL_SWAP, this::swapColors).inside().category(category);
        this.keys().register(Keys.PIXEL_PICK, this::pickColor).inside().category(category);
        this.keys().register(Keys.PIXEL_FILL, this::fillColor).inside().category(category);
        this.keys().register(Keys.PIXEL_BRUSH_DEC, () -> this.adjustBrushSize(-1)).inside().category(category);
        this.keys().register(Keys.PIXEL_BRUSH_INC, () -> this.adjustBrushSize(1)).inside().category(category);
        this.keys().register(Keys.CYCLE_PANELS, this::cycleTabs).inside().category(category);
    }

    private void adjustBrushSize(int delta)
    {
        int n = MathUtils.clamp((int) this.brushSize.getValue() + delta, 1, 64);

        if (n == (int) this.brushSize.getValue())
        {
            return;
        }

        this.brushSize.setValue(n);
        this.setBrushSize(n);
    }

    private void cycleTabs()
    {
        if (documents.size() <= 1)
        {
            return;
        }

        this.switchTab(MathUtils.cycler(currentIndex + (Window.isShiftPressed() ? -1 : 1), documents));

        UIUtils.playClick();
    }

    public int getTabCount()
    {
        return documents.size();
    }

    public int getCurrentTabIndex()
    {
        return currentIndex;
    }

    public IKey getTabLabel(int index)
    {
        return IKey.raw(StringUtils.fileName(documents.get(index).link.path));
    }

    public IKey getTabTooltip(int index)
    {
        return IKey.raw(documents.get(index).link.path);
    }

    public Icon getTabIcon(int index)
    {
        return Icons.MATERIAL;
    }

    public boolean canCloseTab(int index)
    {
        return documents.size() > 1 && index >= 0 && index < documents.size();
    }

    public void switchTab(int index)
    {
        if (index < 0 || index >= documents.size() || currentIndex == index)
        {
            return;
        }

        currentIndex = index;
        this.showCurrentEditor();
        this.syncTabs();
    }

    private UITextureEditor createEditor(Link link, Pixels pixels)
    {
        UITextureEditor editor = new UITextureEditor().saveCallback(this.saveCallback);
        editor.renameCallback((newLink) -> this.renameDocument(editor, newLink));
        editor.colorSupplier(() -> this.primary.picker.color);
        editor.backgroundSupplier(() -> (float) this.brightness.getValue());
        editor.setBrushSize((int) this.brushSize.getValue());
        editor.setDocument(link, pixels);
        editor.full(this.content);
        return editor;
    }

    /**
     * Reassigns {@code editor}'s document to {@code newLink} after a Save As, closing any
     * pre-existing tab that already referenced {@code newLink} (its contents are discarded
     * since the file on disk was just overwritten).
     */
    private void renameDocument(UITextureEditor editor, Link newLink)
    {
        int editorIndex = -1;
        int duplicateIndex = -1;

        for (int i = 0; i < documents.size(); i++)
        {
            Document doc = documents.get(i);

            if (doc.editor == editor)
            {
                editorIndex = i;
            }
            else if (doc.link.equals(newLink))
            {
                duplicateIndex = i;
            }
        }

        if (editorIndex < 0)
        {
            return;
        }

        if (duplicateIndex >= 0)
        {
            this.removeTab(duplicateIndex);

            if (duplicateIndex < editorIndex)
            {
                editorIndex -= 1;
            }
        }

        documents.get(editorIndex).link = newLink;
        this.finishTabMutation();
    }

    private void showCurrentEditor()
    {
        if (documents.isEmpty())
        {
            return;
        }

        List<IUIElement> children = this.content.getChildren();

        if (!children.isEmpty() && children.get(0) instanceof UITextureEditor currentInContent)
        {
            this.content.remove(currentInContent);
        }

        UITextureEditor toShow = documents.get(currentIndex).editor;

        toShow.removeFromParent();
        this.content.prepend(toShow);
        toShow.full(this.content);
        toShow.setBrushSize((int) this.brushSize.getValue());

        this.resize();
    }

    private UITextureEditor getCurrentEditor()
    {
        return documents.isEmpty() || currentIndex < 0 || currentIndex >= documents.size()
            ? null
            : documents.get(currentIndex).editor;
    }

    private void addTabFromPath(String path)
    {
        Link link = Link.create(path);

        for (int i = 0; i < documents.size(); i++)
        {
            if (documents.get(i).link.toString().equals(path))
            {
                this.switchTab(i);
                return;
            }
        }

        Texture t = BBSModClient.getTextures().getTexture(link);
        Pixels pixels = Texture.pixelsFromTexture(t);

        if (pixels == null)
        {
            return;
        }

        UITextureEditor editor = this.createEditor(link, pixels);
        Document doc = new Document(link, pixels, editor);

        documents.add(doc);
        this.switchTab(documents.size() - 1);
    }

    public void closeTab(int index)
    {
        if (!this.canCloseTab(index))
        {
            return;
        }

        this.removeTab(index);
        this.finishTabMutation();
    }

    public void closeOtherTabs(int index)
    {
        if (index < 0 || index >= documents.size() || documents.size() <= 1)
        {
            return;
        }

        for (int i = documents.size() - 1; i >= 0; i--)
        {
            if (i != index)
            {
                this.removeTab(i);
            }
        }

        this.finishTabMutation();
    }

    public void closeTabsLeft(int index)
    {
        if (index <= 0 || index >= documents.size())
        {
            return;
        }

        for (int i = index - 1; i >= 0; i--)
        {
            this.removeTab(i);
        }

        this.finishTabMutation();
    }

    public void closeTabsRight(int index)
    {
        if (index < 0 || index >= documents.size() - 1)
        {
            return;
        }

        for (int i = documents.size() - 1; i > index; i--)
        {
            this.removeTab(i);
        }

        this.finishTabMutation();
    }

    public void openNewTab()
    {
        UITextureEditor current = this.getCurrentEditor();
        Link currentLink = current != null ? current.getTexture() : null;

        UITexturePicker.findAllTextures(this.getContext(), currentLink, this::addTabFromPath);
    }

    private void removeTab(int index)
    {
        if (index < 0 || index >= documents.size())
        {
            return;
        }

        Document doc = documents.remove(index);

        doc.editor.removeFromParent();
        doc.pixels.delete();
        doc.editor.deleteTexture();

        if (documents.isEmpty())
        {
            currentIndex = -1;
        }
        else if (index < currentIndex)
        {
            currentIndex -= 1;
        }
        else if (currentIndex >= documents.size())
        {
            currentIndex = documents.size() - 1;
        }
    }

    private void finishTabMutation()
    {
        if (currentIndex >= 0 && currentIndex < documents.size())
        {
            this.showCurrentEditor();
        }

        this.syncTabs();
    }

    private void syncTabs()
    {
        if (this.tabs != null)
        {
            this.tabs.sync();
        }
    }

    private void swapColors()
    {
        int swap = this.primary.picker.color.getRGBColor();

        this.primary.setColor(this.secondary.picker.color.getRGBColor());
        this.secondary.setColor(swap);
    }

    private void pickColor()
    {
        UITextureEditor editor = this.getCurrentEditor();
        if (editor == null)
        {
            return;
        }
        UIContext context = this.getContext();
        if (editor.area.isInside(context))
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            if (color != null)
            {
                this.primary.setColor(color.getRGBColor());
            }
        }
    }

    private void fillColor()
    {
        UITextureEditor editor = this.getCurrentEditor();
        if (editor == null)
        {
            return;
        }
        UIContext context = this.getContext();
        if (editor.area.isInside(context))
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            editor.fillColor(pixel, this.primary.picker.color, Window.isShiftPressed());
        }
    }

    private void setBrushSize(int size)
    {
        UITextureEditor editor = this.getCurrentEditor();
        if (editor != null)
        {
            editor.setBrushSize(size);
        }
    }

    public void fillTexture(Link current)
    {
        if (current == null)
        {
            return;
        }

        String path = current.toString();

        if (documents.isEmpty())
        {
            Texture t = BBSModClient.getTextures().getTexture(current);
            Pixels pixels = Texture.pixelsFromTexture(t);

            if (pixels != null)
            {
                UITextureEditor editor = this.createEditor(current, pixels);
                documents.add(new Document(current, pixels, editor));
                this.switchTab(0);
                editor.setEditing(true);
            }

            return;
        }

        for (int i = 0; i < documents.size(); i++)
        {
            if (documents.get(i).link.toString().equals(path))
            {
                this.switchTab(i);
                this.getCurrentEditor().setEditing(true);
                return;
            }
        }

        Texture t = BBSModClient.getTextures().getTexture(current);
        Pixels pixels = Texture.pixelsFromTexture(t);

        if (pixels != null)
        {
            UITextureEditor editor = this.createEditor(current, pixels);
            documents.add(new Document(current, pixels, editor));
            this.switchTab(documents.size() - 1);
            editor.setEditing(true);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        UITextureEditor editor = this.getCurrentEditor();
        if (editor != null && editor.area.isInside(context) && editor.getPixels() != null)
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
            };

            int x = this.area.x + 10;
            int y = this.area.ey() - context.batcher.getFont().getHeight() - 10 - (information.length - 1) * 14;

            for (String line : information)
            {
                context.batcher.textCard(line, x, y);

                y += 14;
            }
        }
    }
}
