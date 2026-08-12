package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UIDebugPanel extends UIDashboardPanel
{
    private enum ElementType
    {
        BUTTON,
        TOGGLE,
        ICON,
        IMAGE
    }

    private static class ElementConfig
    {
        public ElementType type;
        public String variableName;
        public String key;
        public String icon;
        public String imagePath;
        public float x;
        public float y;
    }

    public UIElement preview;
    public UIElement editor;

    public UIButton addButton;
    public UIButton addToggle;
    public UIButton exportButton;
    public UIButton exportFileButton;

    public UITextbox variableInput;
    public UITextbox keyInput;
    public UITextbox iconInput;
    public UITextbox imageInput;
    public UITextarea<TextLine> exportArea;

    private final List<ElementConfig> elements = new ArrayList<>();
    private ElementConfig selected;

    public UIDebugPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.preview = new UIElement();
        this.preview.relative(this).x(10).y(10).w(0.5F, -15).h(1F, -20).markContainer();

        this.editor = new UIElement();
        this.editor.relative(this).x(0.5F, 5).y(10).w(0.5F, -15).h(1F, -20).markContainer();

        this.addButton = new UIButton(UIKeys.RAW_ADD_ELEMENT, (b) -> this.openAddElementOverlay());
        this.addToggle = new UIButton(UIKeys.RAW_IMPORT_PANEL, (b) -> this.openImportPanelOverlay());
        this.exportButton = new UIButton(UIKeys.RAW_EXPORT_UI_CLIPBOARD, (b) -> this.exportLayout());
        this.exportFileButton = new UIButton(UIKeys.RAW_EXPORT_UI_FILE, (b) -> this.exportLayoutToFile());

        UIElement palette = UI.row(this.addButton, this.addToggle, this.exportButton, this.exportFileButton);
        palette.relative(this.editor).x(0).y(0).w(1F).h(20);

        this.variableInput = new UITextbox(128, (t) ->
        {
            if (this.selected != null)
            {
                this.selected.variableName = t;
            }
        });

        this.keyInput = new UITextbox(512, (t) ->
        {
            if (this.selected != null)
            {
                this.selected.key = t;
                this.updatePreview();
            }
        });

        this.iconInput = new UITextbox(128, (t) ->
        {
            if (this.selected != null)
            {
                this.selected.icon = t;
                this.updatePreview();
            }
        });

        this.imageInput = new UITextbox(512, (t) ->
        {
            if (this.selected != null)
            {
                this.selected.imagePath = t;
                this.updatePreview();
            }
        });

        this.variableInput.placeholder(UIKeys.RAW_VARIABLE_NAME).border();
        this.keyInput.placeholder(UIKeys.RAW_LOCALIZATION_KEY_BBS_UI_SOMETHING).border();
        this.iconInput.placeholder(UIKeys.RAW_ICON_NAME_E_G_HELP).border();
        this.imageInput.placeholder(UIKeys.RAW_IMAGE_LINK_E_G_BBS_TEXTURES_PNG).border();

        this.iconInput.setVisible(false);
        this.imageInput.setVisible(false);

        UIElement fields = UI.column(5, 5, this.variableInput, this.keyInput, this.iconInput, this.imageInput);
        fields.relative(this.editor).x(0).y(25).w(1F).h(90);

        this.exportArea = new UITextarea<>(null).background();
        this.exportArea.relative(this.editor).x(0).y(120).w(1F).h(1F, -125);

        this.editor.add(palette, fields, this.exportArea);
        this.add(this.preview, this.editor);
    }

    private void openAddElementOverlay()
    {
        List<String> types = new ArrayList<>();

        for (ElementType type : ElementType.values())
        {
            types.add(type.name().toLowerCase());
        }

        UIStringOverlayPanel panel = new UIStringOverlayPanel(UIKeys.RAW_PICK_ELEMENT_TYPE, types, (name) ->
        {
            if (name == null || name.isEmpty())
            {
                return;
            }

            ElementType type = null;

            for (ElementType value : ElementType.values())
            {
                if (value.name().equalsIgnoreCase(name))
                {
                    type = value;
                    break;
                }
            }

            if (type != null)
            {
                this.addElement(type);
            }
        });

        UIOverlay.addOverlay(this.getContext(), panel, 260, 0.5F);
    }

    private void openImportPanelOverlay()
    {
        UIDashboardPanels panels = this.getParent(UIDashboardPanels.class);

        if (panels == null || panels.panels.isEmpty())
        {
            return;
        }

        List<String> names = new ArrayList<>();
        Map<String, UIDashboardPanel> map = new LinkedHashMap<>();

        for (UIDashboardPanel panel : panels.panels)
        {
            if (panel == this)
            {
                continue;
            }

            String name = panel.getClass().getSimpleName();

            if (!map.containsKey(name))
            {
                map.put(name, panel);
                names.add(name);
            }
        }

        if (names.isEmpty())
        {
            return;
        }

        UIStringOverlayPanel overlay = new UIStringOverlayPanel(UIKeys.RAW_IMPORT_FROM_PANEL, names, (name) ->
        {
            UIDashboardPanel panel = map.get(name);

            if (panel != null)
            {
                this.importPanel(panel);
            }
        });

        UIOverlay.addOverlay(this.getContext(), overlay, 260, 0.5F);
    }

    private void addElement(ElementType type)
    {
        ElementConfig config = new ElementConfig();
        config.type = type;
        config.variableName = this.createDefaultVariableName(type);
        config.key = "";
        config.icon = type == ElementType.ICON ? "HELP" : "";
        config.imagePath = "";

        float offset = 0.1F * (this.elements.size() + 1);

        config.x = offset;
        config.y = offset;

        this.elements.add(config);
        this.selectElement(config);
        this.updatePreview();
    }

    private void importPanel(UIDashboardPanel panel)
    {
        panel.visitChildren(UIElement.class, true, (element) ->
        {
            if (element instanceof UIButton)
            {
                UIButton button = (UIButton) element;
                ElementConfig config = new ElementConfig();

                config.type = ElementType.BUTTON;
                config.variableName = this.createDefaultVariableName(ElementType.BUTTON);
                config.key = button.label == null ? "" : button.label.get();
                config.icon = "";
                config.imagePath = "";

                float offset = 0.1F * (this.elements.size() + 1);

                config.x = offset;
                config.y = offset;

                this.elements.add(config);
            }
            else if (element instanceof UIToggle)
            {
                UIToggle toggle = (UIToggle) element;
                ElementConfig config = new ElementConfig();

                config.type = ElementType.TOGGLE;
                config.variableName = this.createDefaultVariableName(ElementType.TOGGLE);
                config.key = toggle.label == null ? "" : toggle.label.get();
                config.icon = "";
                config.imagePath = "";

                float offset = 0.1F * (this.elements.size() + 1);

                config.x = offset;
                config.y = offset;

                this.elements.add(config);
            }
            else if (element instanceof UIIcon)
            {
                UIIcon iconElement = (UIIcon) element;
                ElementConfig config = new ElementConfig();

                config.type = ElementType.ICON;
                config.variableName = this.createDefaultVariableName(ElementType.ICON);
                config.key = "";
                config.icon = resolveIconName(iconElement.getIcon());

                if (config.icon == null || config.icon.isEmpty())
                {
                    config.icon = "HELP";
                }
                config.imagePath = "";

                float offset = 0.1F * (this.elements.size() + 1);

                config.x = offset;
                config.y = offset;

                this.elements.add(config);
            }
        });

        this.selected = null;
        this.updatePreview();
    }

    private String createDefaultVariableName(ElementType type)
    {
        String base;

        if (type == ElementType.BUTTON)
        {
            base = "button";
        }
        else if (type == ElementType.TOGGLE)
        {
            base = "toggle";
        }
        else if (type == ElementType.ICON)
        {
            base = "icon";
        }
        else
        {
            base = "image";
        }

        int index = 1;

        while (true)
        {
            String name = base + index;
            boolean exists = false;

            for (ElementConfig element : this.elements)
            {
                if (name.equals(element.variableName))
                {
                    exists = true;
                    break;
                }
            }

            if (!exists)
            {
                return name;
            }

            index += 1;
        }
    }

    private static Icon resolveIcon(String name)
    {
        if (name == null || name.isEmpty())
        {
            return Icons.MORE;
        }

        try
        {
            Field field = Icons.class.getField(name);
            Object value = field.get(null);

            if (value instanceof Icon)
            {
                return (Icon) value;
            }
        }
        catch (Exception e)
        {}

        return Icons.MORE;
    }

    private static String resolveIconName(Icon icon)
    {
        if (icon == null)
        {
            return "";
        }

        for (Field field : Icons.class.getFields())
        {
            if (field.getType() == Icon.class)
            {
                try
                {
                    Object value = field.get(null);

                    if (value == icon)
                    {
                        return field.getName();
                    }
                }
                catch (Exception e)
                {}
            }
        }

        return "";
    }

    private void selectElement(ElementConfig config)
    {
        this.selected = config;

        if (config != null)
        {
            this.variableInput.setText(config.variableName == null ? "" : config.variableName);
            this.keyInput.setText(config.key == null ? "" : config.key);

            if (config.type == ElementType.ICON)
            {
                this.iconInput.setVisible(true);
                this.imageInput.setVisible(false);
                this.iconInput.setText(config.icon == null ? "" : config.icon);
            }
            else if (config.type == ElementType.IMAGE)
            {
                this.iconInput.setVisible(false);
                this.imageInput.setVisible(true);
                this.imageInput.setText(config.imagePath == null ? "" : config.imagePath);
            }
            else
            {
                this.iconInput.setVisible(false);
                this.imageInput.setVisible(false);
            }
        }
        else
        {
            this.variableInput.setText("");
            this.keyInput.setText("");
            this.iconInput.setText("");
            this.imageInput.setText("");
            this.iconInput.setVisible(false);
            this.imageInput.setVisible(false);
        }
    }

    private UIElement createPreviewElement(ElementConfig config)
    {
        String labelText = config.key == null || config.key.isEmpty() ? config.variableName : config.key;
        IKey label = IKey.raw(labelText == null ? "" : labelText);

        if (config.type == ElementType.BUTTON)
        {
            UIButton button = new UIButton(label, (b) -> this.selectElement(config));

            button.w(0.25F).h(20);

            return button;
        }
        else if (config.type == ElementType.TOGGLE)
        {
            UIToggle toggle = new UIToggle(label, (t) -> this.selectElement(config));

            toggle.w(0.25F).h(20);

            return toggle;
        }
        else if (config.type == ElementType.ICON)
        {
            UIIcon icon = new UIIcon(() -> resolveIcon(config.icon), (b) -> this.selectElement(config));

            icon.wh(20, 20);

            return icon;
        }
        else if (config.type == ElementType.IMAGE)
        {
            UIElement image = new UIElement()
            {
                @Override
                protected boolean subMouseClicked(UIContext context)
                {
                    if (this.area.isInside(context))
                    {
                        UIDebugPanel.this.selectElement(config);

                        return true;
                    }

                    return super.subMouseClicked(context);
                }

                @Override
                public void render(UIContext context)
                {
                    if (config.imagePath != null && !config.imagePath.isEmpty())
                    {
                        try
                        {
                            Link link = Link.create(config.imagePath);
                            Texture texture = BBSModClient.getTextures().getTexture(link);

                            if (texture != null)
                            {
                                context.batcher.fullTexturedBox(texture, this.area.x, this.area.y, this.area.w, this.area.h);
                            }
                        }
                        catch (Exception e)
                        {}
                    }

                    super.render(context);
                }
            };

            image.w(0.25F).h(0.25F);

            return image;
        }

        UIElement element = new UIElement();

        element.w(0.25F).h(20);

        return element;
    }

    private void updatePreview()
    {
        this.preview.removeAll();

        if (this.elements.isEmpty())
        {
            return;
        }

        for (ElementConfig config : this.elements)
        {
            UIElement element = this.createPreviewElement(config);

            element.relative(this.preview).x(config.x).y(config.y);

            UIDraggable draggable = new UIDraggable((context) ->
            {
                if (this.preview.area.w <= 0 || this.preview.area.h <= 0)
                {
                    return;
                }

                float rx = (context.mouseX - this.preview.area.x) / (float) this.preview.area.w;
                float ry = (context.mouseY - this.preview.area.y) / (float) this.preview.area.h;

                if (rx < 0F) rx = 0F;
                if (ry < 0F) ry = 0F;
                if (rx > 1F) rx = 1F;
                if (ry > 1F) ry = 1F;

                config.x = rx;
                config.y = ry;

                this.updatePreview();
            });

            draggable.hoverOnly().relative(element).w(1F).h(1F);

            this.preview.add(element, draggable);
        }

        this.preview.resize();
    }

    private String buildExportCode()
    {
        StringBuilder builder = new StringBuilder();

        for (ElementConfig config : this.elements)
        {
            String varName = config.variableName == null || config.variableName.isEmpty() ? this.createDefaultVariableName(config.type) : config.variableName;
            String key = config.key == null ? "" : config.key;
            String escapedKey = key.replace("\\", "\\\\").replace("\"", "\\\"");

            if (config.type == ElementType.BUTTON)
            {
                builder.append("UIButton ").append(varName).append(" = new UIButton(L10n.lang(\"").append(escapedKey).append("\"), (b) -> {});\n");
            }
            else if (config.type == ElementType.TOGGLE)
            {
                builder.append("UIToggle ").append(varName).append(" = new UIToggle(L10n.lang(\"").append(escapedKey).append("\"), (t) -> {});\n");
            }
            else if (config.type == ElementType.ICON)
            {
                String iconName = config.icon == null || config.icon.isEmpty() ? "HELP" : config.icon;

                builder.append("UIIcon ").append(varName).append(" = new UIIcon(Icons.").append(iconName).append(", (b) -> {});\n");
            }
            else if (config.type == ElementType.IMAGE)
            {
                builder.append("UIElement ").append(varName).append(" = new UIElement();\n");
            }
        }

        builder.append("\nUIElement layout = UI.column(");

        for (int i = 0; i < this.elements.size(); i++)
        {
            ElementConfig config = this.elements.get(i);
            String varName = config.variableName == null || config.variableName.isEmpty() ? this.createDefaultVariableName(config.type) : config.variableName;

            builder.append(varName);

            if (i < this.elements.size() - 1)
            {
                builder.append(", ");
            }
        }

        builder.append(");\n");
        builder.append("this.add(layout);\n");

        return builder.toString();
    }

    private void exportLayout()
    {
        if (this.elements.isEmpty())
        {
            this.exportArea.setText("");
            Window.setClipboard("");

            return;
        }

        String code = this.buildExportCode();

        this.exportArea.setText(code);
        Window.setClipboard(code);
    }

    private void exportLayoutToFile()
    {
        if (this.elements.isEmpty())
        {
            this.exportArea.setText("");
            Window.setClipboard("");

            return;
        }

        String code = this.buildExportCode();

        this.exportArea.setText(code);
        Window.setClipboard(code);

        File folder = new File(BBSMod.getExportFolder(), "ui");

        if (!folder.exists())
        {
            folder.mkdirs();
        }

        File file = IOUtils.findNonExistingFile(new File(folder, "ui_builder.java"));

        try
        {
            IOUtils.writeText(file, code);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}