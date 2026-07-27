package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.importers.Importers;
import mchorse.bbs_mod.importers.types.IImporter;

public class RegisterImportersEvent
{
    public void register(IImporter importer)
    {
        Importers.register(importer);
    }
}
