package com.ktg.mes.ai.service;

import com.ktg.common.config.RuoYiConfig;
import com.ktg.mes.ai.domain.KbDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class KbOriginalStorage {
    private final Path configuredRoot;

    public KbOriginalStorage(@Value("${mes.ai.kb.storage-root:./kb-storage}") String root) {
        configuredRoot = Paths.get(root).toAbsolutePath().normalize();
    }

    Path store(KbDocument.Version version, MultipartFile file) throws IOException {
        Path root = privateRoot();
        Path relative = relativePath(version);
        Path directory = Files.createDirectories(root.resolve(relative.getParent())).toRealPath();
        if (!directory.startsWith(root)) {
            throw new IOException("Invalid private document directory");
        }
        Path original = directory.resolve(relative.getFileName());
        MessageDigest digest = sha256();
        try (InputStream input = file.getInputStream(); DigestInputStream content = new DigestInputStream(input, digest)) {
            Files.copy(content, original);
        } catch (IOException e) {
            Files.deleteIfExists(original);
            throw e;
        }
        StringBuilder checksum = new StringBuilder(64);
        for (byte value : digest.digest()) {
            checksum.append(Character.forDigit((value & 0xff) >>> 4, 16));
            checksum.append(Character.forDigit(value & 0xf, 16));
        }
        version.setStoragePath(relative.toString().replace('\\', '/'));
        version.setChecksumSha256(checksum.toString());
        return original;
    }

    Path original(KbDocument.Version version) throws IOException {
        Path root = privateRoot();
        Path relative = relativePath(version);
        if (!relative.toString().replace('\\', '/').equals(version.getStoragePath())) {
            throw new IOException("Invalid original path");
        }
        Path location = root.resolve(relative);
        if (!Files.exists(location, LinkOption.NOFOLLOW_LINKS)) {
            return location;
        }
        Path resolved = location.toRealPath();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
            throw new IOException("Invalid original location");
        }
        return resolved;
    }

    private Path privateRoot() throws IOException {
        Path publicPath = Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
        if (configuredRoot.startsWith(publicPath)) {
            throw new IOException("Knowledge base requires private storage");
        }
        Path root = Files.createDirectories(configuredRoot).toRealPath();
        Path publicRoot = Files.exists(publicPath) ? publicPath.toRealPath() : publicPath;
        if (root.startsWith(publicRoot) || publicRoot.startsWith(root)) {
            throw new IOException("Knowledge base requires a separate storage directory");
        }
        return root;
    }

    private static Path relativePath(KbDocument.Version version) {
        return Paths.get(version.getDeptId().toString(), version.getLibraryId().toString(),
            version.getVersionId().toString(), "original");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
