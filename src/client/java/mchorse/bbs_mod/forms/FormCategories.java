package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.sections.ExtraFormSection;
import mchorse.bbs_mod.forms.sections.FormSection;
import mchorse.bbs_mod.forms.sections.ModelFormSection;
import mchorse.bbs_mod.forms.sections.ParticleFormSection;
import mchorse.bbs_mod.forms.sections.RecentFormSection;
import mchorse.bbs_mod.forms.sections.UserFormSection;
import mchorse.bbs_mod.utils.watchdog.IWatchDogListener;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FormCategories implements IWatchDogListener
{
    private static final List<Function<FormCategories, FormSection>> EXTRA_SECTIONS = new ArrayList<>();

    public final VisibilityManager visibility = new VisibilityManager();

    private List<FormSection> sections = new ArrayList<>();
    private RecentFormSection recentForms = new RecentFormSection(this);
    private UserFormSection userForms = new UserFormSection(this);
    private ExtraFormSection extraForms = new ExtraFormSection(this);

    private long lastUpdate;

    public static void registerSection(Function<FormCategories, FormSection> factory)
    {
        EXTRA_SECTIONS.add(factory);
    }

    /* Setup */

    public void setup()
    {
        this.sections.clear();
        this.sections.add(this.recentForms);
        this.sections.add(this.userForms);
        this.sections.add(new ModelFormSection(this));
        this.sections.add(new ParticleFormSection(this));
        this.sections.add(this.extraForms);

        for (Function<FormCategories, FormSection> factory : EXTRA_SECTIONS)
        {
            this.sections.add(factory.apply(this));
        }

        for (FormSection section : this.sections)
        {
            section.initiate();
        }

        this.markDirty();
        this.visibility.read();
    }

    public long getLastUpdate()
    {
        return lastUpdate;
    }

    public void markDirty()
    {
        this.lastUpdate = System.currentTimeMillis();
    }

    public RecentFormSection getRecentForms()
    {
        return this.recentForms;
    }

    public UserFormSection getUserForms()
    {
        return this.userForms;
    }

    public ExtraFormSection getExtraForms()
    {
        return this.extraForms;
    }

    public List<FormCategory> getAllCategories()
    {
        List<FormCategory> formCategories = new ArrayList<>();

        for (FormSection section : this.sections)
        {
            formCategories.addAll(section.getCategories());
        }

        return formCategories;
    }

    @Override
    public void accept(Path path, WatchDogEvent event)
    {
        for (FormSection section : this.sections)
        {
            section.accept(path, event);
        }
    }
}
