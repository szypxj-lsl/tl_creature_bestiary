package com.szypxj.tlcreaturebestiary.profile.lore;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public final class ModJarTextResource {
    private static final String DEFAULT_LANGUAGE = "en_us";

    private ModJarTextResource() {
    }

    public static Optional<String> readUtf8(String modId, String resourcePath) {
        if (modId == null || modId.isBlank() || resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        ModList modList = ModList.get();
        if (modList == null) {
            return Optional.empty();
        }
        IModFileInfo modFile = modList.getModFileById(modId);
        if (modFile == null) {
            return Optional.empty();
        }
        try {
            Path path = modFile.getFile().findResource(resourcePath);
            if (path == null || !Files.isRegularFile(path)) {
                return Optional.empty();
            }
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException error) {
            return Optional.empty();
        }
    }

    public static Optional<String> readLocalized(
            String modId,
            String requestedLanguage,
            Function<String, String> pathFactory
    ) {
        if (pathFactory == null) {
            return Optional.empty();
        }
        for (String language : languageCandidates(requestedLanguage)) {
            Optional<String> value = readUtf8(modId, pathFactory.apply(language));
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    public static List<String> languageCandidates(String requestedLanguage) {
        String requested = normalizeLanguage(requestedLanguage);
        List<String> result = new ArrayList<>(2);
        result.add(requested);
        if (!DEFAULT_LANGUAGE.equals(requested)) {
            result.add(DEFAULT_LANGUAGE);
        }
        return List.copyOf(result);
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.matches("[a-z0-9_]+") ? normalized : DEFAULT_LANGUAGE;
    }
}
