package mchorse.bbs_mod.forms.sections;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.categories.ParticleFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParticleFormSection extends SubFormSection
{
    private final Map<String, ParticleFormCategory.Folder> folders = new LinkedHashMap<>();
    private final List<FormCategory> orderedCategories = new ArrayList<>();
    private boolean lastHierarchyEnabled;

    public ParticleFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public void initiate()
    {
        this.categories.clear();
        this.folders.clear();
        this.orderedCategories.clear();

        List<String> keys = new ArrayList<>(BBSModClient.getParticles().getKeys());

        keys.sort(String::compareToIgnoreCase);

        boolean hierarchyEnabled = this.isHierarchyEnabled();

        for (String key : keys)
        {
            if (hierarchyEnabled)
            {
                this.addParticleKey(key);
            }
            else
            {
                super.add(key);
            }
        }

        if (hierarchyEnabled)
        {
            this.rebuildOrder();
        }

        this.lastHierarchyEnabled = hierarchyEnabled;
    }

    @Override
    protected IKey getTitle()
    {
        return UIKeys.FORMS_CATEGORIES_PARTICLES;
    }

    @Override
    protected Form create(String key)
    {
        ParticleForm form = new ParticleForm();

        form.effect.set(key);

        return form;
    }

    @Override
    protected FormCategory createCategory(IKey uiKey, String id)
    {
        return new ParticleFormCategory(uiKey, this.parent.visibility.get("particles_" + id));
    }

    @Override
    protected boolean isEqual(Form form, String key)
    {
        ParticleForm particleForm = (ParticleForm) form;

        return Objects.equals(particleForm.effect.get(), key);
    }

    @Override
    public void accept(Path path, WatchDogEvent event)
    {
        File file = path.toFile();
        Link link = BBSMod.getProvider().getLink(file);

        if (file.isDirectory())
        {
            this.initiate();
            this.parent.markDirty();
            return;
        }

        if (link.path.startsWith("particles/") && link.path.endsWith(".json"))
        {
            String key = link.path.substring("particles/".length());

            key = key.substring(0, key.length() - ".json".length());

            if (event == WatchDogEvent.DELETED)
            {
                this.remove(key);
            }
            else
            {
                this.add(key);
            }

            this.parent.markDirty();
        }
    }

    @Override
    protected void add(String key)
    {
        if (!this.isHierarchyEnabled())
        {
            super.add(key);
            return;
        }

        this.addParticleKey(key);
        this.rebuildOrder();
    }

    @Override
    protected void remove(String key)
    {
        if (!this.isHierarchyEnabled())
        {
            super.remove(key);
            return;
        }

        String folderPath = this.getFolderPath(key);
        ParticleFormCategory.Folder folder = this.folders.get(folderPath);
        boolean removed = false;

        if (folder == null)
        {
            return;
        }

        for (int i = 0; i < folder.getDirectForms().size(); i++)
        {
            if (this.isEqual(folder.getDirectForms().get(i), key))
            {
                folder.getDirectForms().remove(i);
                removed = true;
                break;
            }
        }

        this.cleanupFolder(folder);
        this.rebuildOrder();

        if (removed)
        {
            this.parent.markDirty();
        }
    }

    @Override
    public List<FormCategory> getCategories()
    {
        boolean hierarchyEnabled = this.isHierarchyEnabled();

        if (this.lastHierarchyEnabled != hierarchyEnabled)
        {
            this.initiate();
            this.parent.markDirty();
        }

        if (!hierarchyEnabled)
        {
            return super.getCategories();
        }

        return new ArrayList<>(this.orderedCategories);
    }

    private void addParticleKey(String key)
    {
        String folderPath = this.getFolderPath(key);
        ParticleFormCategory.Folder folder = this.getOrCreateFolder(folderPath);

        for (Form form : folder.getForms())
        {
            if (this.isEqual(form, key))
            {
                return;
            }
        }

        folder.addForm(this.create(key));
    }

    private boolean isHierarchyEnabled()
    {
        return true;
    }

    private String getFolderPath(String key)
    {
        int slash = key.lastIndexOf('/');

        return slash >= 0 ? key.substring(0, slash) : "";
    }

    private String getParentPath(String path)
    {
        int slash = path.lastIndexOf('/');

        return slash >= 0 ? path.substring(0, slash) : null;
    }

    private ParticleFormCategory.Folder getOrCreateFolder(String path)
    {
        ParticleFormCategory.Folder existing = this.folders.get(path);

        if (existing != null)
        {
            return existing;
        }

        String name = path.isEmpty() ? "" : path.substring(path.lastIndexOf('/') + 1);
        IKey title;

        if (path.isEmpty())
        {
            title = UIKeys.FORMS_CATEGORIES_PARTICLES;
        }
        else if (!path.contains("/"))
        {
            title = IKey.comp(Arrays.asList(this.getTitle(), IKey.constant(" (" + name + ")")));
        }
        else
        {
            title = IKey.constant(name);
        }

        boolean defaultVisibility = path.isEmpty();
        ParticleFormCategory.Folder folder = new ParticleFormCategory.Folder(title, this.parent.visibility.get("particles_" + path, defaultVisibility), path, name);
        this.folders.put(path, folder);

        String parentPath = this.getParentPath(path);

        if (parentPath != null)
        {
            ParticleFormCategory.Folder parent = this.getOrCreateFolder(parentPath);
            folder.parent = parent;
            folder.depth = parent.depth + 1;

            if (!parent.children.contains(folder))
            {
                parent.children.add(folder);
            }
        }

        return folder;
    }

    private void cleanupFolder(ParticleFormCategory.Folder folder)
    {
        ParticleFormCategory.Folder current = folder;

        while (current != null && current.getForms().isEmpty() && current.getChildren().isEmpty())
        {
            this.folders.remove(current.path);

            ParticleFormCategory.Folder parent = current.getParent();

            if (parent != null)
            {
                parent.getChildren().remove(current);
            }

            current = parent;
        }
    }

    private void rebuildOrder()
    {
        this.orderedCategories.clear();

        ParticleFormCategory.Folder root = this.folders.get("");

        if (root != null && !root.getForms().isEmpty())
        {
            this.orderedCategories.add(root);
        }

        List<ParticleFormCategory.Folder> topLevel = new ArrayList<>();

        for (ParticleFormCategory.Folder folder : this.folders.values())
        {
            if (folder.getParent() == null && !folder.path.isEmpty())
            {
                topLevel.add(folder);
            }
        }

        topLevel.sort(Comparator.comparing((ParticleFormCategory.Folder folder) -> folder.name, String::compareToIgnoreCase));

        for (ParticleFormCategory.Folder folder : topLevel)
        {
            this.addFolderAndChildren(folder);
        }
    }

    private void addFolderAndChildren(ParticleFormCategory.Folder folder)
    {
        this.orderedCategories.add(folder);

        List<ParticleFormCategory.Folder> children = new ArrayList<>(folder.getChildren());
        children.sort(Comparator.comparing((ParticleFormCategory.Folder child) -> child.name, String::compareToIgnoreCase));

        for (ParticleFormCategory.Folder child : children)
        {
            this.addFolderAndChildren(child);
        }
    }
}
