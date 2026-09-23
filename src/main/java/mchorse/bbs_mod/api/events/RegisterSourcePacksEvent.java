package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;

/**
 * Posted on both sides when source packs are being registered into the asset provider.
 */
public class RegisterSourcePacksEvent
{
    public final AssetProvider provider;

    public RegisterSourcePacksEvent(AssetProvider provider)
    {
        this.provider = provider;
    }

    public void register(ISourcePack sourcePack)
    {
        this.provider.register(sourcePack);
    }

    /**
     * Gives an addon's own assets a source of their own: they can then be addressed as
     * {@code <modId>:...} links anywhere BBS takes one, and are read out of
     * {@code assets/<modId>/assets} in the addon's jar.
     *
     * @param clazz any class of the addon — the source reads its files through that class.
     */
    public void registerAddon(String modId, Class<?> clazz)
    {
        this.provider.register(new InternalAssetsSourcePack(modId, "assets/" + modId + "/assets", clazz));
    }
}
