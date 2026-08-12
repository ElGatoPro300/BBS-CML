package mchorse.bbs_mod.importers.types;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.ImporterUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.utils.FilmProjectHandler;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import java.io.File;

public class BBSProjectImporter implements IImporter
{
    @Override
    public IKey getName()
    {
        return IKey.constant("BBS Project");
    }

    @Override
    public File getDefaultFolder()
    {
        return new File(BBSMod.getAssetsFolder().getParentFile(), "export");
    }

    @Override
    public boolean canImport(ImporterContext context)
    {
        UIBaseMenu currentMenu = UIScreen.getCurrentMenu();

        if (currentMenu instanceof UIDashboard dashboard)
        {
            if (dashboard.panels.panel instanceof UIFilmPanel)
            {
                return ImporterUtils.checkFileExtension(context.files, ".bbsproject");
            }
        }

        return false;
    }

    @Override
    public void importFiles(ImporterContext context)
    {
        UIBaseMenu currentMenu = UIScreen.getCurrentMenu();

        if (currentMenu instanceof UIDashboard dashboard)
        {
            if (dashboard.panels.panel instanceof UIFilmPanel filmPanel)
            {
                for (File file : context.files)
                {
                    if (file.getName().endsWith(".bbsproject"))
                    {
                        FilmProjectHandler.importProjectDirect(filmPanel, file);
                    }
                }
            }
        }
    }
}
