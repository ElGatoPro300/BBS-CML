package mchorse.bbs_mod.forms.renderers.utils;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MatrixCache
{
    private static final MatrixCacheEntry EMPTY = new MatrixCacheEntry(null, null);

    private Map<String, MatrixCacheEntry> entries = new HashMap<>();

    public void clear()
    {
        this.entries.clear();
    }

    public boolean has(String path)
    {
        return this.entries.containsKey(path);
    }

    public MatrixCacheEntry get(String path)
    {
        return this.entries.getOrDefault(path, EMPTY);
    }

    public void put(String path, Matrix4f matrix, Matrix4f origin)
    {
        MatrixCacheEntry existing = this.entries.get(path);

        if (existing == null)
        {
            this.entries.put(path, new MatrixCacheEntry(new Matrix4f(matrix), new Matrix4f(origin)));
        }
        else
        {
            existing.matrix().set(matrix);
            existing.origin().set(origin);
        }
    }

    public Set<String> keySet()
    {
        return this.entries.keySet();
    }

    public Iterable<Map.Entry<String, MatrixCacheEntry>> entrySet()
    {
        return this.entries.entrySet();
    }
}