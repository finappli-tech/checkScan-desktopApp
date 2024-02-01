package sn.finappli.cdcscanner.service.impl;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.FolderReaderResult;
import sn.finappli.cdcscanner.service.FolderReaderService;
import sn.finappli.cdcscanner.utility.ConfigHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Implementation of the {@link FolderReaderService} interface that reads files from a specified folder,
 * groups them, and builds {@link FolderReaderResult} objects based on certain naming conventions.
 */
public class FolderReaderServiceImpl implements FolderReaderService {

    private static final Logger log = LoggerFactory.getLogger(FolderReaderServiceImpl.class);

    /**
     * Checks if all files in a {@link FolderReaderResult} object are present (non-null).
     *
     */
    private final Predicate<FolderReaderResult> areAllFilesPresent = result ->
            Stream.of(result.getFileD(), result.getFileR(), result.getFileV()).noneMatch(Objects::isNull);

    /**
     * Reads files from the configured scan storage folder, groups them, and processes them to generate a list
     * of {@link FolderReaderResult} objects with valid data.
     *
     * @return A list of {@link FolderReaderResult} objects with valid data.
     */
    @Override
    public List<FolderReaderResult> readScanFolder() throws IOException {
        var scanStoragePath = ConfigHolder.getContext().getScanStoragePath();
        var files = fetchFiles(Paths.get(scanStoragePath));

        var groupedNames = getDistinctFileGroup(files);
        return groupedNames.parallelStream()
                .map(name -> buildDataFromFiles(files, name))
                .filter(areAllFilesPresent)
                .map(this::processFolderReaderResult)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Processes a {@link FolderReaderResult} object by retrieving additional information and
     * ensuring the validity of the data.
     *
     * @param result The {@link FolderReaderResult} object to be processed.
     * @return The processed {@link FolderReaderResult} object or {@code null} if not valid.
     */
    private @Nullable FolderReaderResult processFolderReaderResult(FolderReaderResult result) {
        try {
            result.setCmc(Files.readString(result.getFileD()));
            if (isBlank(result.getCmc())) return null;
        } catch (Exception e) {
            return null;
        }
        result.setScanDate(parseDateFromName(result.getName()));
        return result;
    }

    /**
     * Parses the date from the specified name using a custom date and time format.
     *
     * @param name The name containing the date to parse.
     * @return The parsed {@link LocalDateTime} or the current date and time if parsing fails.
     */
    @Contract("_ -> new")
    private LocalDateTime parseDateFromName(String name) {
        try {
            return LocalDateTime.parse(name, DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return LocalDateTime.now();
        }
    }

    /**
     * Builds a {@link FolderReaderResult} object based on the provided group name and files.
     *
     * @param files The list of files to build the result from.
     * @param group The group name for which to build the result.
     * @return The constructed {@link FolderReaderResult} object.
     */
    private FolderReaderResult buildDataFromFiles(@NotNull List<Path> files, String group) {
        var builder = FolderReaderResult.builder();
        files.stream().filter(file -> {
            var name = FilenameUtils.getBaseName(file.toString());
            var result = name != null && name.equals(group);
            if (result) builder.name(name);
            return result;
        }).forEach(file -> {
            switch (file.getFileName().toString().substring(file.getFileName().toString().length() - 1)) {
                case "D" -> builder.fileD(file);
                case "R" -> builder.fileR(file);
                case "V" -> builder.fileV(file);
                default -> log.info("some pair are missing for file group {}", file);
            }
        });
        return builder.build();
    }

    /**
     * Retrieves the distinct file groups from the list of files.
     *
     * @param files The list of files to extract distinct file groups from.
     * @return The list of distinct file groups.
     */
    private List<String> getDistinctFileGroup(@NotNull List<Path> files) {
        return files.stream()
                .map(file -> FilenameUtils.getBaseName(file.toString()))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Fetches a list of valid files from the specified path using a custom file visitor.
     *
     * @param path The {@link Path} representing the directory to start the file tree walk from.
     * @return The list of valid files obtained from the file visitor.
     * @throws IOException If an I/O error occurs during the file tree walk.
     */
    private List<Path> fetchFiles(Path path) throws IOException {
        var fileVisitor = new CustomFileVisitorImpl();
        Files.walkFileTree(path, Set.of(), 1, fileVisitor);
        return fileVisitor.getValidFiles();
    }
}
