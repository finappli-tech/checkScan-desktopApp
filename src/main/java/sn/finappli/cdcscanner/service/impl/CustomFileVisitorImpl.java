package sn.finappli.cdcscanner.service.impl;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom file visitor implementation that processes files with specific extensions.
 * This file visitor is designed to traverse directories, read files, and filter them based on a specific
 * set of criteria. It identifies and collects files with extensions matching a predefined pattern and
 * validates their format during the visit. The processed files and relevant statistics are stored for further analysis.
 *
 * <p>The visit includes logging information about the start and end of directory traversal, as well as
 * details about each visited file. Additionally, in case of any exceptions or failures during the visit,
 * relevant error messages and details are logged for troubleshooting purposes.</p>
 *
 * <p>The main criteria for processing files include skipping directories, continuing with non-regular files,
 * and collecting files with valid extensions. The validity of an extension is determined by both a specific pattern
 * and a predefined list of valid extensions.</p>
 *
 * @see FileVisitResult
 * @see Path
 * @see BasicFileAttributes
 * @see Files
 * @see LinkOption
 * @see Logger
 * @see LoggerFactory
 * @see List
 */
public final class CustomFileVisitorImpl implements FileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(CustomFileVisitorImpl.class);
    private static final List<String> validExtensions = List.of("R", "V", "D");

    /**
     * The list of valid files discovered during the file visit.
     */
    @Getter
    private final List<Path> validFiles = new ArrayList<>();

    /**
     * The count of files processed during the file visit.
     */
    private Integer count = 0;

    /**
     * Invoked before visiting a directory. Logs the start of walking through the directory.
     *
     * @param dir   The directory being visited.
     * @param attrs The attributes of the directory.
     * @return {@link FileVisitResult#CONTINUE} to continue the visit.
     */
    @Override
    public FileVisitResult preVisitDirectory(@NotNull Path dir, BasicFileAttributes attrs) {
        logger.info("Start walking through directory {}", dir);
        return FileVisitResult.CONTINUE;
    }

    /**
     * Invoked for visiting a file. Logs the reading of the file and processes it if it has a valid extension.
     *
     * @param file  The file being visited.
     * @param attrs The attributes of the file.
     * @return {@link FileVisitResult#CONTINUE} to continue the visit.
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        logger.info("Reading file {}", file);

        // Skip if it's a directory
        if (Files.isDirectory(file))
            return FileVisitResult.SKIP_SUBTREE;

        // Continue if it's not a regular file
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            return FileVisitResult.CONTINUE;

        // Process the file if it has a valid extension
        if (isValidExtension(FilenameUtils.getExtension(file.toString()))) {
            count++;
            validFiles.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Checks if the given file extension is valid based on a specific pattern and a list of valid extensions.
     *
     * @param extension The file extension to check.
     * @return {@code true} if the extension is valid, otherwise {@code false}.
     */
    private boolean isValidExtension(@NotNull String extension) {
        return extension.matches("^\\d{1,9}[RVD]$") && validExtensions.contains(extension.substring(extension.length() - 1));
    }

    /**
     * Invoked when a file visit fails. Logs the failure to read the file and associated details.
     *
     * @param file The file for which the visit failed.
     * @param exc  The IOException that occurred during the visit.
     * @return {@link FileVisitResult#CONTINUE} to continue the visit.
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        logger.error("Failed to read file {}", file);
        logger.error(exc.getMessage(), exc);
        return FileVisitResult.CONTINUE;
    }

    /**
     * Invoked after visiting a directory. Logs the end of walking through the directory and the count of processed files.
     *
     * @param dir The directory that has been visited.
     * @param exc The IOException that occurred during the visit, or {@code null} if no exception occurred.
     * @return {@link FileVisitResult#CONTINUE} to continue the visit.
     */
    @Override
    public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) {
        logger.info("End walking through directory {}. {} files has been processed", dir, count);
        if (exc != null) logger.error(exc.getMessage(), exc);
        return FileVisitResult.CONTINUE;
    }
}
