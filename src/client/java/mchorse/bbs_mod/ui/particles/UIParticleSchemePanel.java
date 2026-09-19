package mchorse.bbs_mod.ui.particles;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.renderers.ParticleFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.tabs.DataTab;
import mchorse.bbs_mod.ui.dashboard.panels.tabs.UIDataTabs;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextEditor;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeAppearanceSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeCollisionSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeCurvesSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeExpirationSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeGeneralSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeInitializationSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeLifetimeSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeLightingSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeMotionSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeRateSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeShapeSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeSpaceSection;
import mchorse.bbs_mod.ui.particles.utils.MolangSyntaxHighlighter;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class UIParticleSchemePanel extends UIDataDashboardPanel<ParticleScheme>
{
    /**
     * Default particle placeholder that comes with the engine.
     */
    public static final Link PARTICLE_PLACEHOLDER = Link.assets("particles/default_placeholder.json");

    public UITextEditor textEditor;
    public UIParticleSchemeRenderer renderer;
    public UIScrollView sectionsView;
    public UIParticleSelectionPanel selectionPanel;

    public List<UIParticleSchemeSection> sections = new ArrayList<>();

    private String molangId;

    public UIParticleSchemePanel(UIDashboard dashboard)
    {
        super(dashboard);
        this.enableTabs();

        this.renderer = new UIParticleSchemeRenderer();
        this.renderer.relative(this).wTo(this.iconBar.getFlex()).h(1F);

        this.textEditor = new UITextEditor(null).highlighter(new MolangSyntaxHighlighter());
        this.textEditor.background().relative(this.editor).y(1F, -60).w(1F).h(60);
        this.sectionsView = UI.scrollView(20, 10);
        this.sectionsView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.sectionsView.relative(this.editor).w(200).hTo(this.textEditor.area);

        this.prepend(new UIRenderable(this::drawOverlay));
        this.prepend(this.renderer);
        this.editor.add(this.textEditor, this.sectionsView);

        this.selectionPanel = new UIParticleSelectionPanel(this);
        this.selectionPanel.relative(this).y(UIDataTabs.TABS_HEIGHT_PX).wTo(this.iconBar.area).h(1F, -UIDataTabs.TABS_HEIGHT_PX);
        this.add(this.selectionPanel);

        UIIcon close = new UIIcon(Icons.CLOSE, (b) -> this.editMoLang(null, null, null));

        close.relative(this.textEditor).x(1F, -20);
        this.textEditor.add(close);
        this.overlay.namesList.setFileIcon(Icons.PARTICLE);

        UIIcon restart = new UIIcon(Icons.REFRESH, (b) ->
        {
            this.renderer.setScheme(this.data);
        });
        restart.tooltip(UIKeys.SNOWSTORM_RESTART_EMITTER, Direction.LEFT);

        this.iconBar.add(restart);

        this.addSection(new UIParticleSchemeGeneralSection(this));
        this.addSection(new UIParticleSchemeCurvesSection(this));
        this.addSection(new UIParticleSchemeSpaceSection(this));
        this.addSection(new UIParticleSchemeInitializationSection(this));
        this.addSection(new UIParticleSchemeRateSection(this));
        this.addSection(new UIParticleSchemeLifetimeSection(this));
        this.addSection(new UIParticleSchemeShapeSection(this));
        this.addSection(new UIParticleSchemeMotionSection(this));
        this.addSection(new UIParticleSchemeExpirationSection(this));
        this.addSection(new UIParticleSchemeAppearanceSection(this));
        this.addSection(new UIParticleSchemeLightingSection(this));
        this.addSection(new UIParticleSchemeCollisionSection(this));

        this.fill(null);
    }

    public void editMoLang(String id, Consumer<String> callback, MolangExpression expression)
    {
        this.molangId = id;
        this.textEditor.callback = callback;
        this.textEditor.setText(expression == null ? "" : expression.toString());
        this.textEditor.setVisible(callback != null);

        if (callback != null)
        {
            this.sectionsView.hTo(this.textEditor.area);
        }
        else
        {
            this.sectionsView.h(1F);
        }

        this.sectionsView.resize();
    }

    @Override
    protected IKey getTitle()
    {
        return UIKeys.SNOWSTORM_TITLE;
    }

    @Override
    public ContentType getType()
    {
        return ContentType.PARTICLES;
    }

    @Override
    public Icon getTabIcon(DataTab tab)
    {
        return tab != null && tab.dataId == null ? Icons.SEARCH : Icons.PARTICLE;
    }

    public void dirty()
    {
        this.renderer.emitter.setupVariables();
    }

    private void addSection(UIParticleSchemeSection section)
    {
        this.sections.add(section);
        this.sectionsView.add(section);
    }

    @Override
    protected void fillData(ParticleScheme data)
    {
        this.editMoLang(null, null, null);

        this.renderer.setVisible(data != null);
        this.selectionPanel.setVisible(data == null);

        if (this.data != null)
        {
            this.renderer.setScheme(this.data);

            for (UIParticleSchemeSection section : this.sections)
            {
                section.setScheme(this.data);
            }

            this.sectionsView.resize();
        }
        else
        {
            this.renderer.setScheme(null);
        }
    }

    @Override
    public void fillNames(Collection<String> names)
    {
        super.fillNames(names);

        if (this.selectionPanel != null)
        {
            this.selectionPanel.fillNames(names);
        }
    }

    @Override
    public void forceSave()
    {
        super.forceSave();

        ParticleFormRenderer.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void fillDefaultData(ParticleScheme data)
    {
        super.fillDefaultData(data);

        try (InputStream asset = BBSMod.getProvider().getAsset(PARTICLE_PLACEHOLDER))
        {
            MapType map = DataToString.mapFromString(IOUtils.readText(asset));

            ParticleScheme.PARSER.fromData(data, map);
        }
        catch (Exception e)
        {}
    }

    @Override
    public void appear()
    {
        super.appear();

        this.textEditor.updateHighlighter();
    }

    @Override
    protected boolean shouldAutoOpenListOnFirstResize()
    {
        return false;
    }

    @Override
    public void close()
    {
        if (this.renderer.emitter != null)
        {
            this.renderer.emitter.particles.clear();
        }
    }

    @Override
    public void resize()
    {
        super.resize();

        /* Renderer needs to be resized again because iconBar is in front, and wTo() doesn't
         * work earlier for some reason... */
        this.renderer.resize();
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        if (this.iconBar.isVisible())
        {
            int bg = this.selectionPanel != null && this.selectionPanel.isVisible() ? Colors.A100 : Colors.A50;

            this.iconBar.area.render(context.batcher, bg);
            context.batcher.gradientHBox(this.iconBar.area.x - 6, this.iconBar.area.y, this.iconBar.area.x, this.iconBar.area.ey(), 0, 0x29000000);
        }
    }

    private void drawOverlay(UIContext context)
    {
        /* Draw debug info */
        if (this.editor.isVisible())
        {
            ParticleEmitter emitter = this.renderer.emitter;
            String label = emitter.particles.size() + "P - " + emitter.age + "A";

            int y = (this.textEditor.isVisible() ? this.textEditor.area.y : this.area.ey()) - 12;

            context.batcher.textShadow(label, this.editor.area.ex() - 4 - context.batcher.getFont().getWidth(label), y);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (this.molangId != null)
        {
            FontRenderer font = context.batcher.getFont();
            int w = font.getWidth(this.molangId);

            context.batcher.textCard(this.molangId, this.textEditor.area.ex() - 6 - w, this.textEditor.area.ey() - 6 - font.getHeight());
        }
    }
}
