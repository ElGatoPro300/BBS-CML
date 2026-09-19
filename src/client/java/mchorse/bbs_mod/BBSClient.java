package mchorse.bbs_mod;

import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.graphics.FramebufferManager;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.items.GunZoom;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.particles.ParticleManager;
import mchorse.bbs_mod.selectors.EntitySelectors;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import mchorse.bbs_mod.utils.VideoRecorder;

/**
 * BBS utility class that provides easy access to BBS Mod's client components.
 */
public class BBSClient
{
    public static TextureManager getTextures()
    {
        return BBSModClient.getTextures();
    }

    public static FramebufferManager getFramebuffers()
    {
        return BBSModClient.getFramebuffers();
    }

    public static SoundManager getSounds()
    {
        return BBSModClient.getSounds();
    }

    public static L10n getL10n()
    {
        return BBSModClient.getL10n();
    }

    public static ModelManager getModels()
    {
        return BBSModClient.getModels();
    }

    public static FormCategories getFormCategories()
    {
        return BBSModClient.getFormCategories();
    }

    public static ScreenshotRecorder getScreenshotRecorder()
    {
        return BBSModClient.getScreenshotRecorder();
    }

    public static VideoRecorder getVideoRecorder()
    {
        return BBSModClient.getVideoRecorder();
    }

    public static EntitySelectors getSelectors()
    {
        return BBSModClient.getSelectors();
    }

    public static ParticleManager getParticles()
    {
        return BBSModClient.getParticles();
    }

    public static CameraController getCameraController()
    {
        return BBSModClient.getCameraController();
    }

    public static Films getFilms()
    {
        return BBSModClient.getFilms();
    }

    public static GunZoom getGunZoom()
    {
        return BBSModClient.getGunZoom();
    }

    public static UIDashboard getDashboard()
    {
        return BBSModClient.getDashboard();
    }
}
