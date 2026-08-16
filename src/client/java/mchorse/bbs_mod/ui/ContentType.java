package mchorse.bbs_mod.ui;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.cubic.model.ModelRepository;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.model.UIModelPanel;
import mchorse.bbs_mod.ui.particles.UIParticleSchemePanel;
import mchorse.bbs_mod.utils.repos.FilmRepository;
import mchorse.bbs_mod.utils.repos.FolderManagerRepository;
import mchorse.bbs_mod.utils.repos.IRepository;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

public class ContentType
{
    private static final IRepository<ParticleScheme> PARTICLE_REPOSITORY = new FolderManagerRepository<>(BBSModClient.getParticles());
    private static final IRepository<Film> FILMS_REPOSITORY = new FolderManagerRepository<>(BBSMod.getFilms());
    private static final IRepository<Film> FILMS_LOCAL_REPOSITORY = new FolderManagerRepository<>(new FilmManager(() -> new File(BBSMod.getAssetsFolder().getParentFile(), "data/films")));
    private static final IRepository<Film> FILMS_REMOTE_REPOSITORY = new FilmRepository();

    public static final ContentType PARTICLES = new ContentType("particles", () -> PARTICLE_REPOSITORY, (dashboard) -> dashboard.getPanel(UIParticleSchemePanel.class));
    public static final ContentType MODELS = new ContentType("models", () -> new ModelRepository(BBSModClient.getModels()), (dashboard) -> dashboard.getPanel(UIModelPanel.class));
    public static final ContentType FILMS = new ContentType("films", ContentType::getFilmsRepository, (dashboard) -> dashboard.getPanel(UIFilmPanel.class));
    public static final ContentType SOUNDS = new ContentType("sounds", () -> null, (dashboard) -> null);
    public static final ContentType GRAPH = new ContentType("graph", () -> null, (dashboard) -> null);

    private static IRepository<? extends ValueGroup> getFilmsRepository()
    {
        if (MinecraftClient.getInstance().isIntegratedServerRunning())
        {
            return FILMS_REPOSITORY;
        }

        return ClientNetwork.isIsBBSModOnServer() ? FILMS_REMOTE_REPOSITORY : FILMS_LOCAL_REPOSITORY;
    }

    private final String id;
    private Supplier<IRepository<? extends ValueGroup>> manager;
    private Function<UIDashboard, UIDataDashboardPanel> dashboardPanel;

    public ContentType(String id, Supplier<IRepository<? extends ValueGroup>> manager, Function<UIDashboard, UIDataDashboardPanel> dashboardPanel)
    {
        this.id = id;
        this.manager = manager;
        this.dashboardPanel = dashboardPanel;
    }

    public static ContentType fromId(String id)
    {
        if ("particles".equals(id)) return PARTICLES;
        if ("models".equals(id)) return MODELS;
        if ("films".equals(id)) return FILMS;
        if ("sounds".equals(id)) return SOUNDS;
        if ("graph".equals(id)) return GRAPH;

        return null;
    }

    public String getId()
    {
        return this.id;
    }

    public IRepository<? extends ValueGroup> getRepository()
    {
        return this.manager.get();
    }

    public UIDataDashboardPanel get(UIDashboard dashboard)
    {
        return this.dashboardPanel.apply(dashboard);
    }
}