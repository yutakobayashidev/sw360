package org.eclipse.sw360.users.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
    private static final String EXTENSION = ".log";
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileUtil() {
    }

    public static void writeErrorToFile(String typeLog, String method, String messageError, String folder) {
        BufferedWriter writer = null;
        FileWriter fileWriter = null;
        try {
            String error = LocalDateTime.now().format(format) + " " + typeLog + " " + " " + method + " - " + messageError;
            String path = folder + LocalDate.now() + EXTENSION;
            File file = new File(path);
            if (file.exists()) {
                fileWriter = new FileWriter(file, true);
                writer = new BufferedWriter(fileWriter);
                writer.append(error);
            } else {
                File files = new File(path);
                if (files.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                fileWriter = new FileWriter(files);
                writer = new BufferedWriter(fileWriter);
                writer.write(error);
            }
            writer.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static File getFileLastModified(String directoryFilePath) {
        File directory = new File(directoryFilePath);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }
        return chosenFile;
    }

    public static List<String> readFileError(String filePath) {
        List<String> errors = new ArrayList<>();
        Path path = Paths.get(filePath);
        try {
            errors = Files.readAllLines(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return errors;
    }

    public static Set<String> listFilesUsingFileWalk(String dir) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

}
