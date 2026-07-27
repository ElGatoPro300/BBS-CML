package mchorse.bbs_mod.ui.supporters;

import mchorse.bbs_mod.resources.Link;

import java.util.Calendar;
import java.util.Date;

public class Supporter
{
    public final String name;
    public final String link;
    public final Link banner;
    public Date date = new Date();

    public Supporter(String name, String link, Link banner)
    {
        this.name = name;
        this.link = link;
        this.banner = banner;
    }

    public Supporter withDate(int month, int date)
    {
        return this.withDate(2024, month, date);
    }

    public Supporter withDate(int year, int month, int date)
    {
        Calendar instance = Calendar.getInstance();

        instance.set(year, month - 1, date, 0, 0);

        return this.withDate(instance.getTime());
    }

    public Supporter withDate(Date date)
    {
        this.date = date;

        return this;
    }

    public boolean hasOnlyName()
    {
        return this.link.isEmpty() && this.banner == null;
    }

    public boolean hasNoBanner()
    {
        return !this.link.isEmpty() && this.banner == null;
    }

    public boolean hasBanner()
    {
        return this.banner != null;
    }
}