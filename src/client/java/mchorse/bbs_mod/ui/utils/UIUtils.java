package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.utils.OS;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class UIUtils
{
    /**
     * Open web link (in default web browser)
     */
    public static boolean openWebLink(String address)
    {
        if (OS.CURRENT == OS.WINDOWS)
        {
            return runSysCommand("rundll32", "url.dll,FileProtocolHandler", address);
        }
        else if (OS.CURRENT == OS.MACOS)
        {
            return runSysCommand("open", address);
        }

        return runSysCommand("kde-open", address)
            || runSysCommand("gnome-open", address)
            || runSysCommand("xdg-open", address);
    }

    /**
     * Open native OS folder chooser dialog using modern File Explorer.
     */
    public static String selectFolder(String title, String defaultPath)
    {
        if (OS.CURRENT == OS.WINDOWS)
        {
            try
            {
                String initial = (defaultPath != null && !defaultPath.isEmpty() && new File(defaultPath).exists())
                    ? defaultPath.replace("'", "''")
                    : "";

                String desc = (title == null ? "Select BBS Folder" : title).replace("'", "''");

                String script = "[void][System.Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms');"
                    + "$f=New-Object System.Windows.Forms.FolderBrowserDialog;"
                    + "$f.AutoUpgradeEnabled=$true;"
                    + "$f.Description='" + desc + "';"
                    + (initial.isEmpty() ? "" : "$f.SelectedPath='" + initial + "';")
                    + "if($f.ShowDialog()-eq[System.Windows.Forms.DialogResult]::OK){[Console]::Out.Write($f.SelectedPath)}";

                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-STA", "-Command", script);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8")))
                {
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(System.lineSeparator());
                        }

                        sb.append(line);
                    }

                    process.waitFor();
                    String result = sb.toString().trim();

                    if (!result.isEmpty())
                    {
                        return result;
                    }
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }

        try
        {
            return TinyFileDialogs.tinyfd_selectFolderDialog(title, defaultPath == null ? "" : defaultPath);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return null;
        }
    }

    public static boolean openFolder(File folder)
    {
        try
        {
            String path = folder.getAbsolutePath();

            if (OS.CURRENT == OS.WINDOWS)
            {
                return runSysCommand("explorer", path);
            }
            else if (OS.CURRENT == OS.MACOS)
            {
                return runSysCommand("open", path);
            }

            return runSysCommand("kde-open", path)
                || runSysCommand("gnome-open", path)
                || runSysCommand("xdg-open", path);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean runSysCommand(String... command)
    {
        try
        {
            Process p = Runtime.getRuntime().exec(command);

            if (p == null)
            {
                return false;
            }

            try
            {
                return p.exitValue() == 0;
            }
            catch (IllegalThreadStateException e)
            {
                return true;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return false;
        }
    }

    public static void playClick()
    {
        playClick(1F);
    }

    public static void playClick(float pitch)
    {
        if (BBSSettings.clickSound.get())
        {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(BBSMod.CLICK, pitch));
        }
        else
        {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }
}