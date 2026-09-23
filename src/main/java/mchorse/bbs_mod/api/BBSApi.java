package mchorse.bbs_mod.api;

/**
 * BBS Addon API contract metadata and runtime verification.
 */
public class BBSApi
{
    /**
     * Current version of the BBS API.
     */
    public static final int VERSION = 1;

    /**
     * Requires the running BBS instance to support at least the specified API version.
     *
     * @param addonId The mod ID of the calling addon.
     * @param requiredVersion The minimum API version required.
     * @throws IllegalStateException If the running BBS API version is lower than required.
     */
    public static void requireVersion(String addonId, int requiredVersion)
    {
        if (VERSION < requiredVersion)
        {
            throw new IllegalStateException(
                "Addon '" + addonId + "' requires BBS API version " + requiredVersion +
                ", but running BBS provides API version " + VERSION + "."
            );
        }
    }
}
