package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.l10n.L10n;

/**
 * Posted on the client before the first localization load so addons can register their language files.
 */
public class RegisterL10nEvent
{
    public final L10n l10n;

    public RegisterL10nEvent(L10n l10n)
    {
        this.l10n = l10n;
    }
}
