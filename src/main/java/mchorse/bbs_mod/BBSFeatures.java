package mchorse.bbs_mod;

/**
 * Compile-time feature gates for unfinished or optional UI/surfaces.
 */
public final class BBSFeatures
{
    /**
     * Model editor IK panel and Geometry "Add IK Locator" action.
     *
     * <p>On since the IK port landed: the panel now edits a solver that exists, so
     * there is nothing left to defer. It registers second in the icon bar, right
     * after the default panel — the slot BBS-FS gives it, immediately after Pose.
     */
    public static final boolean MODEL_IK_UI = true;

    /**
     * Model editor procedural "Look at limb" picker (config.lookAtHead).
     * Gecko animation settings use UIModelGeckoAnimationsSection and stay visible.
     */
    public static final boolean MODEL_PROCEDURAL_LOOK_AT_UI = false;

    private BBSFeatures()
    {}
}
