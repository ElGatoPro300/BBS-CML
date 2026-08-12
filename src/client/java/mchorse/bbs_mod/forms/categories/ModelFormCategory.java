package mchorse.bbs_mod.forms.categories;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIModelFormCategory;

import java.util.ArrayList;
import java.util.List;

public class ModelFormCategory extends FormCategory
{
    public ModelFormCategory(IKey title, ValueBoolean visibility)
    {
        super(title, visibility);
    }

    @Override
    public UIFormCategory createUI(UIFormList list)
    {
        return new UIModelFormCategory(this, list);
    }

    public static class Folder extends ModelFormCategory
    {
        public final String path;
        public final String name;
        public Folder parent;
        public int depth;
        public final List<Folder> children = new ArrayList<>();

        public Folder(IKey title, ValueBoolean visibility, String path, String name)
        {
            super(title, visibility);

            this.path = path;
            this.name = name;
        }

        public Folder getParent()
        {
            return this.parent;
        }

        public int getDepth()
        {
            return this.depth;
        }

        public List<Folder> getChildren()
        {
            return this.children;
        }
    }
}
