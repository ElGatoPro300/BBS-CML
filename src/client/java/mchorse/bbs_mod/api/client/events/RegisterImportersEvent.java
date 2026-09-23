package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.importers.Importers;
import mchorse.bbs_mod.importers.types.IImporter;

/**
 * Posted on the client once BBS has registered its built-in file drop importers.
 */
public class RegisterImportersEvent
{
    public void register(IImporter importer)
    {
        Importers.register(importer);
    }
}
