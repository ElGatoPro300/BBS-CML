package mchorse.bbs_mod.ui.utils.keys;

import mchorse.bbs_mod.l10n.keys.IKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyCombo
{
    private static Set<String> categoryKeys = new HashSet<>();

    public String id = "";
    public IKey label;
    public IKey category = IKey.EMPTY;
    public String categoryKey;
    public boolean repeatable;
    public List<Integer> keys = new ArrayList<>();

    public static Set<String> getCategoryKeys()
    {
        return categoryKeys;
    }

    public KeyCombo(String id, IKey label, int... keys)
    {
        this(label, keys);

        this.id = id;

        this.categoryKey("all");
    }

    public KeyCombo(IKey label, int... keys)
    {
        this.label = label;

        this.set(keys);
    }

    private void set(int... keys)
    {
        this.keys.clear();

        for (int key : keys)
        {
            this.keys.add(key);
        }
    }

    public KeyCombo repeatable()
    {
        this.repeatable = true;

        return this;
    }

    public KeyCombo category(IKey category)
    {
        this.category = category;

        return this;
    }

    public KeyCombo categoryKey(String categoryKey)
    {
        this.categoryKey = categoryKey;

        categoryKeys.add(categoryKey);

        return this;
    }

    public int getMainKey()
    {
        return this.keys.isEmpty() ? -1 : this.keys.get(0);
    }

    public String getKeyCombo()
    {
        StringBuilder label = new StringBuilder(KeyCodes.getName(this.getMainKey()));

        for (int i = 1; i < this.keys.size(); i++)
        {
            label.insert(0, KeyCodes.getName(this.keys.get(i)) + " + ");
        }

        return label.toString();
    }

    public void copy(KeyCombo combo)
    {
        this.keys.clear();
        this.keys.addAll(combo.keys);
    }
}