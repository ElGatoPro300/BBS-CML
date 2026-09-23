package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.l10n.L10n;

/**
 * Posted on the client when language files are reloaded.
 */
public class L10nReloadEvent
{
    public final L10n l10n;

    public L10nReloadEvent(L10n l10n)
    {
        this.l10n = l10n;
    }
}
