package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.FontUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;
import java.util.List;

public class UILabelFormPanel extends UIFormPanel<LabelForm>
{
    public UITextbox text;
    public UIToggle billboard;
    public UIToggle nametag;
    public UIColor color;
    public UIColor glowingColor;
    public UITrackpad glowIntensity;
    public UIPoseSectionCollapse glowSection;
    public UITrackpad max;
    public UITrackpad anchorX;
    public UITrackpad anchorY;
    public UIToggle anchorLines;

    public UITrackpad shadowX;
    public UITrackpad shadowY;
    public UIColor shadowColor;

    public UIColor background;
    public UITrackpad offset;

    /* Advanced */
    public UICirculate font;
    public UIIcon openFontsFolder;
    private List<String> availableFonts;
    public UITrackpad fontSize;
    public UIToggle bold;
    public UICirculate fontStyle;
    public UITrackpad letterSpacing;
    public UITrackpad lineHeight;
    public UICirculate textAlign;
    public UITrackpad opacity;
    
    public UIToggle underline;
    public UIToggle strikethrough;
    public UITrackpad shadowBlur;
    public UIToggle outline;
    public UIColor outlineColor;
    public UITrackpad outlineWidth;
    public UIToggle gradient;
    public UIColor gradientEndColor;
    public UITrackpad gradientOffset;
    public UIButton resetGradient;

    public UIPoseSectionCollapse advancedSection;

    public UILabelFormPanel(UIForm editor)
    {
        super(editor);

        this.text = new UITextbox(10000, (t) -> this.form.text.set(t));
        this.billboard = new UIToggle(UIKeys.FORMS_EDITORS_BILLBOARD_TITLE, (b) -> this.form.billboard.set(b.getValue()));
        this.nametag = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_NAMETAG, (b) -> this.form.nametag.set(b.getValue()));
        this.nametag.tooltip(UIKeys.FORMS_EDITORS_LABEL_NAMETAG_HINT);
        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();
        this.color.tooltip(UIKeys.FORMS_EDITORS_BLEND_COLOR);
        this.glowingColor = new UIColor((c) ->
        {
            Color color = Color.rgba(c);

            color.a = 1F;
            this.form.glowingColor.set(color);

            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            this.form.glowSettings.set(settings);
        });
        this.glowingColor.tooltip(UIKeys.FORMS_EDITORS_GLOW);
        this.glowIntensity = new UITrackpad((value) ->
        {
            GlowSettings settings = this.form.glowSettings.get().copy();

            settings.intensity = value.floatValue();
            this.form.glowSettings.set(settings);
        });
        this.glowIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.glowIntensity.tooltip(UIKeys.FORMS_EDITORS_GLOW_INTENSITY);
        this.glowSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_GLOW,
            Colors.ORANGE,
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_GLOWING_COLOR).marginTop(4),
                this.glowingColor,
                UI.label(UIKeys.FORMS_EDITORS_GLOW_INTENSITY),
                this.glowIntensity
            )
        );
        this.max = new UITrackpad((value) -> this.form.max.set(value.intValue()));
        this.max.limit(-1, Integer.MAX_VALUE, true).increment(10);
        this.anchorX = new UITrackpad((value) -> this.form.anchorX.set(value.floatValue()));
        this.anchorX.values(0.01F);
        this.anchorY = new UITrackpad((value) -> this.form.anchorY.set(value.floatValue()));
        this.anchorY.values(0.01F);
        this.anchorLines = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_ANCHOR_LINES, (value) -> this.form.anchorLines.set(value.getValue()));

        this.shadowX = new UITrackpad((value) -> this.form.shadowX.set(value.floatValue()));
        this.shadowX.limit(-100, 100).values(0.1F, 0.01F, 0.5F).increment(0.1F);
        this.shadowY = new UITrackpad((value) -> this.form.shadowY.set(value.floatValue()));
        this.shadowY.limit(-100, 100).values(0.1F, 0.01F, 0.5F).increment(0.1F);
        this.shadowColor = new UIColor((value) -> this.form.shadowColor.set(Color.rgba(value))).withAlpha();

        this.background = new UIColor((value) -> this.form.background.set(Color.rgba(value))).withAlpha();
        this.offset = new UITrackpad((value) -> this.form.offset.set(value.floatValue()));

        /* Advanced Inits */
        this.availableFonts = FontUtils.getAvailableFonts();
        this.font = new UICirculate((b) ->
        {
            int v = b.getValue();
            if (v == 0) this.form.font.set("");
            else if (v - 1 < this.availableFonts.size()) this.form.font.set(this.availableFonts.get(v - 1));
        });
        
        this.font.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_DEFAULT);
        for (String fontName : this.availableFonts)
        {
            this.font.addLabel(IKey.raw(fontName));
        }

        this.openFontsFolder = new UIIcon(Icons.FOLDER, (b) -> {
            File fontsFolder = new File(BBSMod.getAssetsFolder(), "fonts");
            fontsFolder.mkdirs();
            UIUtils.openFolder(fontsFolder);
        })
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                int color = Colors.A100 + BBSSettings.primaryColor.get();

                if (this.hover)
                {
                    color = Colors.mulRGB(color, 0.85F);
                }

                this.area.render(context.batcher, color);

                super.renderSkin(context);
            }
        };
        this.openFontsFolder.tooltip(UIKeys.FORMS_EDITORS_LABEL_OPEN_FONTS_FOLDER);

        this.fontSize = new UITrackpad((v) -> this.form.fontSize.set(v.floatValue()));
        this.fontSize.limit(0.1F, 100F).values(0.1F, 0.1F, 2F);
        
        this.bold = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_BOLD, (b) -> this.form.fontWeight.set(b.getValue() ? 700 : 400));
        
        this.fontStyle = new UICirculate((b) -> this.form.fontStyle.set(b.getValue()));
        this.fontStyle.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_STYLE_NORMAL);
        this.fontStyle.addLabel(UIKeys.FORMS_EDITORS_LABEL_FONT_STYLE_ITALIC);
        
        this.textAlign = new UICirculate((b) -> this.form.textAlign.set(b.getValue()));
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_LEFT);
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_CENTER);
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_RIGHT);
        this.textAlign.addLabel(UIKeys.FORMS_EDITORS_LABEL_TEXT_ALIGN_JUSTIFY);

        this.letterSpacing = new UITrackpad((v) -> this.form.letterSpacing.set(v.floatValue()));
        this.letterSpacing.limit(-10F, 50F).values(0.1F);
        
        this.lineHeight = new UITrackpad((v) -> this.form.lineHeight.set(v.floatValue()));
        this.lineHeight.limit(0F, 100F).values(0.1F);
        
        this.opacity = new UITrackpad((v) -> this.form.opacity.set(v.floatValue()));
        this.opacity.limit(0F, 1F).values(0.05F);

        this.underline = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_UNDERLINE, (b) -> this.form.underline.set(b.getValue()));
        this.strikethrough = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_STRIKETHROUGH, (b) -> this.form.strikethrough.set(b.getValue()));
        
        this.shadowBlur = new UITrackpad((v) -> this.form.shadowBlur.set(v.floatValue()));
        this.shadowBlur.limit(0F, 20F).values(0.1F);
        
        this.outline = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_OUTLINE, (b) -> this.form.outline.set(b.getValue()));
        this.outlineColor = new UIColor((c) -> this.form.outlineColor.set(Color.rgba(c))).withAlpha();
        this.outlineWidth = new UITrackpad((v) -> this.form.outlineWidth.set(v.floatValue()));
        this.outlineWidth.limit(0F, 10F).values(0.1F);
        
        this.gradient = new UIToggle(UIKeys.FORMS_EDITORS_LABEL_GRADIENT, (b) -> this.form.gradient.set(b.getValue()));
        this.gradientEndColor = new UIColor((c) -> this.form.gradientEndColor.set(Color.rgba(c))).withAlpha();
        this.gradientOffset = new UITrackpad((v) -> this.form.gradientOffset.set(v.floatValue()));
        this.gradientOffset.limit(0F, 1F).values(0.01F);
        this.gradientOffset.tooltip(UIKeys.RAW_GRADIENT_OFFSET);

        this.resetGradient = new UIButton(UIKeys.RAW_RESET_GRADIENT, (b) ->
        {
            this.form.gradient.set(false);
            this.form.gradientEndColor.set(Color.white());
            this.form.gradientOffset.set(0.5F);

            this.gradient.setValue(false);
            this.gradientEndColor.setColor(Colors.WHITE);
            this.gradientOffset.setValue(0.5F);
        });

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_LABEL), this.text, this.billboard, this.nametag, this.color, this.glowSection, this.max);

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_ANCHOR).marginTop(8), UI.row(this.anchorX, this.anchorY), this.anchorLines);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_SHADOW_OFFSET).marginTop(8), this.shadowX, this.shadowY);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_SHADOW_COLOR).marginTop(8), this.shadowColor);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_BACKGROUND).marginTop(8), this.background, this.offset);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_LABEL_COLOR_FORMAT_GUIDE).marginTop(8), new UIMinecraftColorGuide());

        /* Advanced text — same animated colored disclosure as Glow / Color. */
        this.advancedSection = new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_LABEL_ADVANCED_TEXT,
            Colors.ACTIVE,
            UI.column(
                UI.label(UIKeys.FORMS_EDITORS_LABEL_FONT), this.font, this.openFontsFolder,
                UI.row(this.fontSize, this.bold),
                UI.row(this.fontStyle, this.textAlign),
                UI.row(this.letterSpacing, this.lineHeight),
                UI.label(UIKeys.FORMS_EDITORS_LABEL_OPACITY), this.opacity,
                UI.row(this.underline, this.strikethrough),
                UI.label(UIKeys.FORMS_EDITORS_LABEL_EFFECTS).marginTop(8),
                UI.label(UIKeys.FORMS_EDITORS_LABEL_SHADOW_BLUR), this.shadowBlur,
                this.outline, this.outlineColor, this.outlineWidth,
                this.gradient, this.gradientEndColor, this.gradientOffset, this.resetGradient
            )
        );
        this.advancedSection.marginTop(12);
        this.options.add(this.advancedSection);
    }

    @Override
    public void startEdit(LabelForm form)
    {
        super.startEdit(form);

        this.text.setText(form.text.get());
        this.billboard.setValue(form.billboard.get());
        this.nametag.setValue(form.nametag.get());
        this.color.setColor(form.color.get().getARGBColor());
        GlowSettings glow = form.glowSettings.get();
        Color glowDisplay = new Color();

        glow.resolveColor(form.glowingColor.get(), glowDisplay);
        this.glowingColor.setColor(glowDisplay.getRGBColor());

        this.glowIntensity.setValue(glow.intensity);
        this.max.setValue(form.max.get());
        this.anchorX.setValue(form.anchorX.get());
        this.anchorY.setValue(form.anchorY.get());
        this.anchorLines.setValue(form.anchorLines.get());

        this.shadowX.setValue(form.shadowX.get());
        this.shadowY.setValue(form.shadowY.get());
        this.shadowColor.setColor(form.shadowColor.get().getARGBColor());

        this.background.setColor(form.background.get().getARGBColor());
        this.offset.setValue(form.offset.get());

        /* Advanced Sync */
        String currentFont = form.font.get();
        int fontIndex = this.availableFonts.indexOf(currentFont);
        this.font.setValue(fontIndex == -1 ? 0 : fontIndex + 1);

        this.fontSize.setValue(form.fontSize.get());
        this.bold.setValue(form.fontWeight.get() >= 700);
        this.fontStyle.setValue(form.fontStyle.get());
        this.textAlign.setValue(form.textAlign.get());
        this.letterSpacing.setValue(form.letterSpacing.get());
        this.lineHeight.setValue(form.lineHeight.get());
        this.opacity.setValue(form.opacity.get());
        this.underline.setValue(form.underline.get());
        this.strikethrough.setValue(form.strikethrough.get());
        this.shadowBlur.setValue(form.shadowBlur.get());
        this.outline.setValue(form.outline.get());
        this.outlineColor.setColor(form.outlineColor.get().getARGBColor());
        this.outlineWidth.setValue(form.outlineWidth.get());
        this.gradient.setValue(form.gradient.get());
        this.gradientEndColor.setColor(form.gradientEndColor.get().getARGBColor());
        this.gradientOffset.setValue(form.gradientOffset.get());
    }

    @Override
    public void finishEdit()
    {
        super.finishEdit();

        this.color.picker.removeFromParent();
        this.shadowColor.picker.removeFromParent();
        this.background.picker.removeFromParent();
        
        this.outlineColor.picker.removeFromParent();
        this.gradientEndColor.picker.removeFromParent();
    }

    private static class UIMinecraftColorGuide extends UIElement
    {
        private static final char[] COLOR_CODES = new char[]
        {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };

        private static final int[] COLOR_VALUES = new int[]
        {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };

        private static final IKey PREVIEW_TEXT = UIKeys.FORMS_EDITORS_LABEL_COLOR_EXAMPLE;
        private static final IKey FORMAT_HEADER = UIKeys.FORMS_EDITORS_LABEL_FORMAT_HEADER;
        private static final String PREVIEW_FORMAT_TEXT = "Minecraft";
        private static final char[] FORMAT_CODES = new char[]
        {
            'k', 'l', 'm', 'n', 'o', 'r'
        };
        private static final IKey[] FORMAT_LABELS = new IKey[]
        {
            UIKeys.FORMS_EDITORS_LABEL_FORMAT_GLITCH,
            UIKeys.FORMS_EDITORS_LABEL_FORMAT_BOLD,
            UIKeys.FORMS_EDITORS_LABEL_FORMAT_STRIKETHROUGH,
            UIKeys.FORMS_EDITORS_LABEL_FORMAT_UNDERLINE,
            UIKeys.FORMS_EDITORS_LABEL_FORMAT_ITALIC,
            UIKeys.FORMS_EDITORS_LABEL_FORMAT_RESET
        };

        private final int lineHeight;
        private final int leftPadding = 2;

        public UIMinecraftColorGuide()
        {
            super();

            int baseLine = Batcher2D.getDefaultTextRenderer().getHeight();
            this.lineHeight = baseLine + 2;
            this.h(this.lineHeight * (COLOR_CODES.length + FORMAT_CODES.length + 2) + 4);
        }

        @Override
        public void render(UIContext context)
        {
            FontRenderer font = context.batcher.getFont();
            int x = this.area.x + this.leftPadding;
            int y = this.area.y + 1;
            int codeWidth = font.getWidth("[f]");
            int previewX = x + codeWidth + 6;

            for (int i = 0; i < COLOR_CODES.length; i++)
            {
                String code = "[" + COLOR_CODES[i] + "]";
                int color = Colors.A100 | COLOR_VALUES[i];

                context.batcher.text(code, x, y, Colors.LIGHTER_GRAY, true);
                String previewLabel = PREVIEW_TEXT.get();

                if (COLOR_CODES[i] == '0')
                {
                    int w = font.getWidth(previewLabel);
                    context.batcher.box(previewX - 2, y - 1, previewX + w + 2, y + font.getHeight() + 1, Colors.WHITE);
                }
                context.batcher.text(previewLabel, previewX, y, color, true);

                y += this.lineHeight;
            }

            y += 2;
            context.batcher.text(FORMAT_HEADER.get(), x, y, Colors.GRAY, false);
            y += this.lineHeight;

            for (int i = 0; i < FORMAT_CODES.length; i++)
            {
                String code = "[" + FORMAT_CODES[i] + "]";
                String preview = StringUtils.processColoredText("[" + FORMAT_CODES[i] + PREVIEW_FORMAT_TEXT);

                context.batcher.text(code, x, y, Colors.LIGHTER_GRAY, true);
                context.batcher.text(preview, previewX, y, Colors.WHITE, true);

                if (i < FORMAT_LABELS.length)
                {
                    int labelX = previewX + font.getWidth(PREVIEW_FORMAT_TEXT) + 8;
                    context.batcher.text(FORMAT_LABELS[i].get(), labelX, y, Colors.GRAY, false);
                }

                y += this.lineHeight;
            }

            super.render(context);
        }
    }
}
