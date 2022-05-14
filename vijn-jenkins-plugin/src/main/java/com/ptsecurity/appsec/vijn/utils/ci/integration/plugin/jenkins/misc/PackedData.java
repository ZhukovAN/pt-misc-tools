package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.Deflater;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PackedData {
    public enum Type {
        VIJN_RESULT_V1
    }

    protected Type type;

    protected String data;

    private static final String DATA_FILE_NAME = "data.json";

    @SneakyThrows
    @NonNull
    public static String packData(@NonNull final Object data) {
        log.debug("Creating temporal file to serialize and pack data");
        try (
                TempFile jsonFile = TempFile.createFile();
                TempFile packedFile = TempFile.createFile();
                ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(packedFile.toFile())) {
            outputStream.setLevel(Deflater.BEST_COMPRESSION);
            log.debug("Storing data as JSON to file {}", jsonFile.toPath());
            new ObjectMapper().writeValue(jsonFile.toFile(), data);

            log.debug("Data will be packed to {}", packedFile.toPath());
            log.debug("Adding packed file entry {}", DATA_FILE_NAME);
                ZipArchiveEntry entry = new ZipArchiveEntry(jsonFile.toFile(), DATA_FILE_NAME);
                outputStream.putArchiveEntry(entry);
                FileInputStream jsonFileStream = new FileInputStream(jsonFile.toFile());
                IOUtils.copy(jsonFileStream, outputStream);
                jsonFileStream.close();
                outputStream.closeArchiveEntry();
                outputStream.finish();
            outputStream.close();
            byte[] binaryData = FileUtils.readFileToByteArray(packedFile.toFile());
            return Base64.getEncoder().encodeToString(binaryData);
        } catch (IOException e) {
            log.error("Packed file initialization failed", e);
            throw e;
        }
    }

    @SneakyThrows
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> T unpackData(@NonNull final String data, @NonNull final Class<?> clazz) {
        byte[] binary = Base64.getDecoder().decode(data);
        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(binary);
                ZipArchiveInputStream inputStream = new ZipArchiveInputStream(byteArrayInputStream)) {
            do {
                ZipArchiveEntry entry = inputStream.getNextZipEntry();
                if (null == entry) break;
                if (entry.isDirectory()) continue;
                if (!DATA_FILE_NAME.equals(entry.getName())) continue;
                log.debug("Allocating {}-byte array to read data", entry.getSize());
                byte[] jsonData = new byte[(int) entry.getSize()];
                log.debug("Reading packed data");
                IOUtils.read(inputStream, jsonData);
                ObjectMapper mapper = BaseJsonHelper.createObjectMapper();
                return (T) mapper.readValue(jsonData, clazz);
            } while (true);
            throw new IllegalArgumentException("No packed data found");
        } catch (IOException e) {
            log.error("Packed file initialization failed", e);
            throw e;
        }
    }

    public <T> T unpackData(@NonNull final Class<?> clazz) {
        return unpackData(data, clazz);
    }
}
