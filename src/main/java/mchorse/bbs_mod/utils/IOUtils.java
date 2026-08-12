package mchorse.bbs_mod.utils;

import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IOUtils
{
    public static String readText(File file) throws FileNotFoundException
    {
        return readText(new FileInputStream(file));
    }

    /**
     * Read a text file from current jar's resources
     */
    public static String readText(String path)
    {
        try
        {
            InputStream in = IOUtils.class.getResourceAsStream(path);

            return readText(in);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read file: " + path);
        }
    }

    /**
     * Read a text file from {@link InputStream} 
     */
    public static String readText(InputStream in)
    {
        try (Scanner scanner = new Scanner(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            scanner.useDelimiter("\\A");

            /* Empty/corrupt config files must not crash startup via next(). */
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public static void writeText(File file, String string) throws IOException
    {
        if (file == null)
        {
            return;
        }

        File parent = file.getParentFile();

        if (parent != null && !parent.isDirectory())
        {
            parent.mkdirs();
        }

        /* Write to a temp file first. Opening the real path with FileOutputStream
         * truncates immediately — a crash mid-write left bbs.json at 0 bytes and
         * the next launch overwrote user settings with defaults. */
        File temp = new File(file.getAbsolutePath() + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)))
        {
            writer.write(string);
        }

        if (file.isFile() && file.length() > 0)
        {
            File backup = new File(file.getAbsolutePath() + ".bak");

            try
            {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Read all lines from a file (needs a text file) 
     */
    public static List<String> readLines(String fileName) throws Exception
    {
        return readLines(IOUtils.class.getClass().getResourceAsStream(fileName));
    }

    /**
     * Read all lines from a file (needs a text file) 
     */
    public static List<String> readLines(InputStream stream) throws Exception
    {
        List<String> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;

            while ((line = br.readLine()) != null)
            {
                list.add(line);
            }
        }

        return list;
    }

    /**
     * <b>IMPORTANT</b>: don't forget to free the memory using {@link MemoryUtil#memFree(Buffer)}
     * after using the byte buffer!
     */
    public static ByteBuffer readByteBuffer(InputStream stream, int bufferSize) throws IOException
    {
        byte[] bytes = IOUtils.readBytes(stream, bufferSize);
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);

        buffer.put(bytes);
        buffer.flip();

        return buffer;
    }

    public static byte[] readBytes(InputStream stream) throws IOException
    {
        return readBytes(stream, 4 * 1024);
    }

    public static byte[] readBytes(InputStream stream, int bufferSize) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int bytesRead;
        byte[] data = new byte[bufferSize];

        while ((bytesRead = stream.read(data, 0, data.length)) != -1)
        {
            buffer.write(data, 0, bytesRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public static void copyFolder(File source, File destination) throws IOException
    {
        if (source.isDirectory())
        {
            if (!destination.exists())
            {
                destination.mkdirs();
            }

            String[] files = source.list();

            if (files != null)
            {
                for (String file : files)
                {
                    File sourceFile = new File(source, file);
                    File destinationFile = new File(destination, file);

                    copyFolder(sourceFile, destinationFile);
                }
            }
        }
        else
        {
            try (FileInputStream in = new FileInputStream(source);
                 FileOutputStream out = new FileOutputStream(destination))
            {
                byte[] buffer = new byte[1024];
                int length;

                while ((length = in.read(buffer)) > 0)
                {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    public static void deleteFolder(File folder)
    {
        if (!folder.isDirectory())
        {
            return;
        }

        for (File file : folder.listFiles())
        {
            if (file.isDirectory())
            {
                deleteFolder(file);
            }
            else
            {
                file.delete();
            }
        }

        folder.delete();
    }

    public static File findNonExistingFile(File file)
    {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        String baseName = index == -1 ? name : name.substring(0, index);
        String extension = index == -1 ? "" : name.substring(index);

        int i = 1;

        while (file.exists())
        {
            file = new File(file.getParentFile().getAbsolutePath(), baseName + "_" + i + extension);

            i += 1;
        }

        return file;
    }
}