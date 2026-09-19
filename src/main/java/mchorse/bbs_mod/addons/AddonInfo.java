package mchorse.bbs_mod.addons;

import mchorse.bbs_mod.resources.Link;

import java.util.List;

public class AddonInfo
{
    public final String id;
    public final String name;
    public final String version;
    public final String description;
    public final List<String> authors;
    public final Link icon;
    public final String website;
    public final String issues;
    public final String source;

    public AddonInfo(String id, String name, String version, String description, List<String> authors, Link icon, String website, String issues, String source)
    {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.authors = authors;
        this.icon = icon;
        this.website = website;
        this.issues = issues;
        this.source = source;
    }
}
