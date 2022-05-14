package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@AllArgsConstructor
public class TempFile implements AutoCloseable {
    public static final String PREFIX = "vijn-";
    public static final String SUFFIX = "-file";

    private final Path path;

    public Path toPath() {
        return path;
    }

    public File toFile() {
        return path.toFile();
    }

    public static TempFile createFile() {
        return createFile(null);
    }

    public static TempFile createFolder() {
        return createFolder(null);
    }

    @SneakyThrows
    public static TempFile createFile(final Path folder) {
        return (null == folder)
                ? new TempFile(Files.createTempFile(PREFIX, SUFFIX))
                : new TempFile(Files.createTempFile(folder, PREFIX, SUFFIX));
    }

    @SneakyThrows
    public static TempFile createFolder(final Path folder) {
        return (null == folder)
                ? new TempFile(Files.createTempDirectory(PREFIX))
                : new TempFile(Files.createTempDirectory(folder, PREFIX));
    }

    @SneakyThrows
    @Override
    public void close() {
        if (path.toFile().isDirectory())
            FileUtils.deleteDirectory(path.toFile());
        else
            FileUtils.forceDelete(path.toFile());
    }
}
