package com.zorroa.archivist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String extension(String path) {
        try {
            return path.substring(path.lastIndexOf('.')+1).toLowerCase();
        }
        catch (IndexOutOfBoundsException ignore) {
            //
        }
        return "";
    }

    public static String extension(Path path) {
        return FileUtils.extension(path.toString());
    }

    /**
     * Return the basename of a file file in a path.  The basename is the file name without
     * the file extension.
     *
     * In this example "file" is the base name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String basename(String path) {
        String filename = filename(path);
        return filename.substring(0, filename.lastIndexOf("."));
    }

    /**
     * Return the filenane of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String filename(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * Return the directory name portion of a path.
     *
     * In this example /test/example is the directory name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String dirname(String path) {
        return path.substring(0, path.lastIndexOf("/"));
    }

    /**
     * Make all the given directories in the given path. If they already exist, return.
     *
     * @param path
     */
    public static void makedirs(String path) {
        logger.info("mkdir: {}", path);
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * Join string elements into file path.
     * @param e
     * @return
     */
    public static String pathjoin(String ... e) {
        return StringUtils.arrayToDelimitedString(e, "/");
    }
}
