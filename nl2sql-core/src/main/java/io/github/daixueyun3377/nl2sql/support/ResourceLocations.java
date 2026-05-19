package io.github.daixueyun3377.nl2sql.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 从 classpath 或本地文件路径加载文本（应用方本地维护 MD 时使用 file 路径）。
 */
public final class ResourceLocations {

    private ResourceLocations() {
    }

    public static String loadText(String classpathResource, String filePath) {
        if (filePath != null && !filePath.trim().isEmpty()) {
            return loadFromFile(filePath.trim());
        }
        if (classpathResource == null || classpathResource.trim().isEmpty()) {
            throw new IllegalArgumentException("classpath resource or file path is required");
        }
        return loadFromClasspath(classpathResource.trim());
    }

    public static String loadFromClasspath(String resourcePath) {
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        InputStream in = ResourceLocations.class.getClassLoader().getResourceAsStream(normalized);
        if (in == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
        }
        try (InputStream stream = in) {
            byte[] bytes = readAllBytes(stream);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read classpath resource: " + resourcePath, e);
        }
    }

    public static String loadFromFile(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + filePath, e);
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
