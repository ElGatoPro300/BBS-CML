package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.manager.storage.CompressedDataStorage;
import mchorse.bbs_mod.utils.resources.FilteredLink;
import mchorse.bbs_mod.utils.resources.MultiLink;

import net.minecraft.client.MinecraftClient;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FilmProjectHandler
{
    public static void exportProject(UIFilmPanel filmPanel)
    {
        Film film = filmPanel.getData();

        if (film == null)
        {
            return;
        }

        filmPanel.save();

        File exportFolder = new File(BBSMod.getAssetsFolder().getParentFile(), "export");
        exportFolder.mkdirs();

        File destZip = new File(exportFolder, film.getId() + "_project.bbsproject");

        Set<Link> customLinks = new HashSet<>();
        Set<String> customModels = new HashSet<>();
        collectFilmAssets(film, customLinks, customModels);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZip)))
        {
            Set<String> addedEntries = new HashSet<String>();

            /* 1. Add film.dat */
            File filmFile = new File(BBSMod.getWorldFolder(), "bbs/films/" + film.getId() + ".dat");

            if (filmFile.exists())
            {
                String entryName = "film.dat";
                addedEntries.add(entryName);
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(filmFile.toPath(), zos);
                zos.closeEntry();
            }

            /* 2. Add custom links */
            for (Link link : customLinks)
            {
                File file = new File(BBSMod.getAssetsFolder(), link.path);

                if (file.exists() && file.isFile())
                {
                    String entryName = "assets/" + link.path;
                    entryName = entryName.replace('\\', '/');
                    if (addedEntries.add(entryName))
                    {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file.toPath(), zos);
                        zos.closeEntry();
                    }
                }
            }

            /* 3. Add custom models folders recursively */
            for (String modelId : customModels)
            {
                File modelDir = new File(BBSMod.getAssetsFolder(), "models/" + modelId);

                if (modelDir.exists() && modelDir.isDirectory())
                {
                    addFolderToZip(modelDir, "assets/models/" + modelId + "/", zos, addedEntries);
                }
            }

            filmPanel.getContext().notifySuccess(UIKeys.FILM_EXPORT_SUCCESS.format(destZip.getName()));
            UIUtils.openFolder(exportFolder);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            filmPanel.getContext().notifyError(UIKeys.FILM_EXPORT_ERROR.format(e.getMessage()));
        }
    }

    private static void addFolderToZip(File folder, String zipPath, ZipOutputStream zos, Set<String> addedEntries) throws Exception
    {
        zipPath = zipPath.replace('\\', '/');
        File[] files = folder.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                addFolderToZip(file, zipPath + file.getName() + "/", zos, addedEntries);
            }
            else
            {
                String entryName = zipPath + file.getName();
                entryName = entryName.replace('\\', '/');
                if (addedEntries.add(entryName))
                {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private static void collectFilmAssets(BaseValueGroup group, Set<Link> links, Set<String> models)
    {
        for (BaseValue value : group.getAll())
        {
            if (value instanceof ValueLink valueLink)
            {
                Link link = valueLink.get();

                if (link != null)
                {
                    if (link instanceof MultiLink multiLink)
                    {
                        for (FilteredLink child : multiLink.children)
                        {
                            if (child.path != null && Link.isAssets(child.path))
                            {
                                links.add(child.path);
                            }
                        }
                    }
                    else if (Link.isAssets(link))
                    {
                        links.add(link);
                    }
                }
            }
            else if (value instanceof ValueString valueString)
            {
                String id = valueString.getId();
                String strVal = valueString.get();

                if (id.equals("model"))
                {
                    if (!strVal.isEmpty() && !strVal.startsWith("imported_projects/"))
                    {
                        File modelDir = new File(BBSMod.getAssetsFolder(), "models/" + strVal);

                        if (modelDir.exists() && modelDir.isDirectory())
                        {
                            models.add(strVal);
                        }
                    }
                }
                else if (id.equals("effect"))
                {
                    if (!strVal.isEmpty() && !strVal.startsWith("imported_projects/"))
                    {
                        links.add(Link.assets("particles/" + strVal + ".json"));
                    }
                }
                else if (id.equals("structure_file"))
                {
                    if (!strVal.isEmpty() && !strVal.startsWith("imported_projects/"))
                    {
                        links.add(Link.assets(strVal));
                    }
                }
            }
            else if (value instanceof ValueForm valueForm)
            {
                Form form = valueForm.get();

                if (form != null)
                {
                    collectFilmAssets(form, links, models);
                }
            }
            else if (value instanceof BaseValueGroup childGroup)
            {
                collectFilmAssets(childGroup, links, models);
            }
        }
    }

    public static void importProjectDirect(UIFilmPanel filmPanel, File selectedZip)
    {
        String projectName = selectedZip.getName().replaceAll("(?i)\\.bbsproject$", "").replaceAll("[^a-zA-Z0-9_\\-]", "_");

        try
        {
            boolean safeMode = BBSSettings.editorImportMode.get() == 0;
            File exportFolder = new File(BBSMod.getAssetsFolder().getParentFile(), "export");
            exportFolder.mkdirs();

            File tempFile = new File(exportFolder, "temp_film_" + System.currentTimeMillis() + ".dat");

            try (ZipFile zip = new ZipFile(selectedZip))
            {
                ZipEntry filmEntry = zip.getEntry("film.dat");

                if (filmEntry == null)
                {
                    MinecraftClient.getInstance().execute(() ->
                        filmPanel.getContext().notifyError(UIKeys.FILM_IMPORT_ERROR_INVALID)
                    );

                    return;
                }

                try (InputStream is = zip.getInputStream(filmEntry);
                     FileOutputStream fos = new FileOutputStream(tempFile))
                {
                    is.transferTo(fos);
                }

                Set<String> customModelsInZip = new HashSet<>();
                Set<String> customParticlesInZip = new HashSet<>();
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements())
                {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith("assets/models/") && !entry.isDirectory())
                    {
                        String rest = name.substring("assets/models/".length());
                        int slash = rest.indexOf('/');

                        if (slash > 0)
                        {
                            customModelsInZip.add(rest.substring(0, slash));
                        }
                    }
                    else if (name.startsWith("assets/particles/") && !entry.isDirectory() && name.endsWith(".json"))
                    {
                        String rel = name.substring("assets/particles/".length());
                        String particleId = rel.substring(0, rel.length() - ".json".length());
                        customParticlesInZip.add(particleId);
                    }
                }

                MapType data = new CompressedDataStorage().load(tempFile);

                if (safeMode)
                {
                    remapNBT(data, projectName, customModelsInZip, customParticlesInZip);
                }

                File finalFilmFile = new File(BBSMod.getWorldFolder(), "bbs/films/" + projectName + ".dat");
                finalFilmFile.getParentFile().mkdirs();
                new CompressedDataStorage().save(finalFilmFile, data);

                entries = zip.entries();

                while (entries.hasMoreElements())
                {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith("assets/") && !entry.isDirectory())
                    {
                        String relativePath = name.substring("assets/".length());
                        String targetPath = safeMode ? getSafePath(relativePath, projectName) : relativePath;

                        File destFile = new File(BBSMod.getAssetsFolder(), targetPath);
                        destFile.getParentFile().mkdirs();

                        try (InputStream is = zip.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(destFile))
                        {
                            is.transferTo(fos);
                        }
                    }
                }
            }
            finally
            {
                if (tempFile.exists())
                {
                    tempFile.delete();
                }
            }

            MinecraftClient.getInstance().execute(() ->
            {
                filmPanel.requestNames();
                filmPanel.getContext().notifySuccess(UIKeys.FILM_IMPORT_SUCCESS.format(projectName));
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MinecraftClient.getInstance().execute(() ->
                filmPanel.getContext().notifyError(UIKeys.FILM_IMPORT_ERROR.format(e.getMessage()))
            );
        }
    }

    private static String getSafePath(String originalPath, String projectName)
    {
        int slash = originalPath.indexOf('/');

        if (slash < 0)
        {
            return originalPath;
        }

        String category = originalPath.substring(0, slash);
        String rest = originalPath.substring(slash + 1);

        return category + "/imported_projects/" + projectName + "/" + rest;
    }

    private static void remapNBT(MapType map, String projectName, Set<String> customModels, Set<String> customParticles)
    {
        if (map == null)
        {
            return;
        }

        if (map.has("model", BaseType.TYPE_STRING))
        {
            String modelVal = map.getString("model");

            if (!modelVal.isEmpty() && customModels.contains(modelVal))
            {
                if (!modelVal.startsWith("imported_projects/"))
                {
                    map.putString("model", "imported_projects/" + projectName + "/" + modelVal);
                }
            }
        }

        if (map.has("effect", BaseType.TYPE_STRING))
        {
            String effectVal = map.getString("effect");

            if (!effectVal.isEmpty() && customParticles.contains(effectVal))
            {
                if (!effectVal.startsWith("imported_projects/"))
                {
                    map.putString("effect", "imported_projects/" + projectName + "/" + effectVal);
                }
            }
        }

        if (map.has("structure_file", BaseType.TYPE_STRING))
        {
            String structVal = map.getString("structure_file");

            if (!structVal.isEmpty() && !structVal.startsWith("imported_projects/"))
            {
                String safePath = getSafePath(structVal, projectName);
                map.putString("structure_file", safePath);
            }
        }

        for (String key : map.keys())
        {
            BaseType val = map.get(key);

            if (val != null)
            {
                if (val.getTypeId() == BaseType.TYPE_STRING)
                {
                    String strVal = ((StringType) val).value;

                    if (strVal.startsWith("assets:"))
                    {
                        String originalPath = strVal.substring("assets:".length());

                        if (!originalPath.contains("/imported_projects/"))
                        {
                            String safePath = getSafePath(originalPath, projectName);
                            map.putString(key, "assets:" + safePath);
                        }
                    }
                }
                else if (val.getTypeId() == BaseType.TYPE_MAP)
                {
                    remapNBT((MapType) val, projectName, customModels, customParticles);
                }
                else if (val.getTypeId() == BaseType.TYPE_LIST)
                {
                    remapList((ListType) val, projectName, customModels, customParticles);
                }
            }
        }
    }

    private static void remapList(ListType list, String projectName, Set<String> customModels, Set<String> customParticles)
    {
        for (int i = 0; i < list.size(); i++)
        {
            BaseType val = list.get(i);

            if (val != null)
            {
                if (val.getTypeId() == BaseType.TYPE_STRING)
                {
                    String strVal = ((StringType) val).value;

                    if (strVal.startsWith("assets:"))
                    {
                        String originalPath = strVal.substring("assets:".length());

                        if (!originalPath.contains("/imported_projects/"))
                        {
                            String safePath = getSafePath(originalPath, projectName);
                            list.elements.set(i, new StringType("assets:" + safePath));
                        }
                    }
                }
                else if (val.getTypeId() == BaseType.TYPE_MAP)
                {
                    remapNBT((MapType) val, projectName, customModels, customParticles);
                }
                else if (val.getTypeId() == BaseType.TYPE_LIST)
                {
                    remapList((ListType) val, projectName, customModels, customParticles);
                }
            }
        }
    }
}