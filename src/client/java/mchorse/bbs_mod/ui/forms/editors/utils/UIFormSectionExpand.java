package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.ui.ValueUILayoutPreferences;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.input.UIFormDisclosureCollapse;

/**
 * Persists form-editor section / disclosure open-closed state in
 * {@link ValueUILayoutPreferences} across panel switches and game restarts.
 */
public final class UIFormSectionExpand
{
    public static final String GENERAL_DISPLAY = "forms.general.display";
    public static final String GENERAL_TRANSFORM = "forms.general.transform";
    public static final String GENERAL_ILLUSION = "forms.general.illusion";
    public static final String GENERAL_HITBOX = "forms.general.hitbox";
    public static final String GENERAL_STATS = "forms.general.stats";

    public static final String MODEL_PICK = "forms.model.pick";
    public static final String MODEL_SHADING = "forms.model.shading";
    public static final String MODEL_POSE = "forms.model.pose";

    public static final String BODY_FORM = "forms.body.form";
    public static final String BODY_BONE = "forms.body.bone";
    public static final String BODY_TRANSFORM = "forms.body.transform";

    /** Shared Extra disclosure across form type panels. */
    public static final String SHARED_EXTRA = "forms.shared.extra";
    /** Shared Color grade disclosure nested under Extra. */
    public static final String SHARED_COLOR_GRADE = "forms.shared.color_grade";
    /** Outline {@link UISection} from {@code UIFormOutlineControls.createSection()}. */
    public static final String SHARED_OUTLINE = "forms.shared.outline";

    private UIFormSectionExpand()
    {
    }

    public static boolean has(String sectionId)
    {
        ValueUILayoutPreferences prefs = BBSSettings.uiLayoutPreferences;

        return prefs != null && prefs.hasFormSectionExpanded(sectionId);
    }

    public static boolean get(String sectionId, boolean defaultExpanded)
    {
        ValueUILayoutPreferences prefs = BBSSettings.uiLayoutPreferences;

        if (prefs == null)
        {
            return defaultExpanded;
        }

        return prefs.getFormSectionExpanded(sectionId, defaultExpanded);
    }

    public static void set(String sectionId, boolean expanded)
    {
        ValueUILayoutPreferences prefs = BBSSettings.uiLayoutPreferences;

        if (prefs != null)
        {
            prefs.setFormSectionExpanded(sectionId, expanded);
        }
    }

    /**
     * Restores saved expand state (no animation) and saves on later toggles.
     */
    public static UISection bind(UISection section, String sectionId)
    {
        if (section == null || sectionId == null || sectionId.isEmpty())
        {
            return section;
        }

        boolean expanded = get(sectionId, section.isExpanded());

        section.onToggle((open) -> set(sectionId, open));
        section.setExpanded(expanded, false);

        return section;
    }

    /**
     * Same as {@link #bind(UISection, String)} for form disclosures (Extra / Color grade).
     */
    public static UIFormDisclosureCollapse bind(UIFormDisclosureCollapse section, String sectionId)
    {
        if (section == null || sectionId == null || sectionId.isEmpty())
        {
            return section;
        }

        boolean expanded = get(sectionId, section.isExpanded());

        section.onToggle((open) -> set(sectionId, open));
        section.setExpanded(expanded, false);

        return section;
    }
}
