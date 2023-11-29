package com.google.ar.core.examples.java.Utils;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IO {

    private final File ANDROID_DIR;
    public static final String APP_FOLDER = ".Saved_Model";


    public IO(Context context) {
        ANDROID_DIR = context.getExternalFilesDir(null);
        ANDROID_DIR.mkdirs();
        getRootDir().mkdirs();
    }

    public File getRootDir() {
        return joint(ANDROID_DIR, APP_FOLDER);
    }


    /**
     * Get absolute stickers directory
     *
     * @return "${ANDROID_ROOT}/${APP_FOLDER}/stickers"
     */
    public File getCustomFolder(String name) {
        return joint(getRootDir(), name);
    }


    /**
     * Save data to file
     *
     * @param data byte array
     * @param file file to saved
     * @throws IOException exception
     */
    public static void write(byte[] data, File file) throws IOException {
        if (data == null) throw new NullPointerException();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write(data);
        bos.flush();
        bos.close();
    }

    /**
     * Convert stream to byte arrays
     *
     * @param is input stream
     * @return byte array
     */
    public static byte[] streamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024 * 4];
        int len;
        while ((len = is.read(buffer)) >= 0) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    /**
     * Write stream data to file
     *
     * @param stream stream data
     * @param file   file
     */
    public static void write(InputStream stream, File file) throws IOException {
        if (stream == null) throw new NullPointerException();
        byte[] data = streamToBytes(stream);
        write(data, file);
    }



    /**
     *
     * @param name : must have extension, etc : '.obj'
     * @return customfile in root folder forder
     */
    public File getCustomFile(String name) {
        return new File(getRootDir(), name);
    }


    /**
     * Join file path
     *
     * @param root  root file path
     * @param paths sub file paths
     * @return joined path by "/" symbol
     */
    public static File joint(File root, String... paths) {
        return joint(root.getPath(), paths);
    }

    /**
     * Join file paths
     *
     * @param rootPath root path
     * @param paths    sub paths
     * @return joined path by "/" symbol
     */
    public static File joint(String rootPath, String... paths) {
        File root = new File(rootPath);
        for (String path : paths) {
            root = new File(root.getPath(), path);
        }
        return root;
    }

}
