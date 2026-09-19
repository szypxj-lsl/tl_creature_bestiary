package com.szypxj.tlcreaturebestiary.profile.lore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class OfficialLoreSanitizer {
    private static final int MAX_CHARS = 4096;
    private static final Pattern ANGLE_MARKUP = Pattern.compile("<[^>]+>");
    private static final Pattern PATCHOULI_BREAK = Pattern.compile("\\$\\(br\\)");
    private static final Pattern PATCHOULI_MARKUP = Pattern.compile("\\$\\([^)]*\\)");
    private static final Pattern INLINE_WHITESPACE = Pattern.compile("[\\t \\x0B\\f\\r]+");
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile("\\n{3,}");

    private OfficialLoreSanitizer() {
    }

    public static String sanitizeIceAndFire(String raw, String languageCode) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = stripBom(raw);
        text = ANGLE_MARKUP.matcher(text).replaceAll("");
        return normalizeParagraphs(text, languageCode);
    }

    public static String sanitizePatchouli(String raw, String languageCode) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = stripBom(raw);
        text = PATCHOULI_BREAK.matcher(text).replaceAll("\n");
        text = PATCHOULI_MARKUP.matcher(text).replaceAll("");
        return normalizeParagraphs(text, languageCode);
    }

    public static String sanitizePlainText(String raw, String languageCode) {
        return normalizeParagraphs(stripBom(raw), languageCode);
    }

    private static String normalizeParagraphs(String raw, String languageCode) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        normalized = EXCESS_BLANK_LINES.matcher(normalized).replaceAll("\n\n");
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        List<String> cleanParagraphs = new ArrayList<>();
        boolean compactCjk = isCjkLanguage(languageCode);
        for (String paragraph : paragraphs) {
            String[] lines = paragraph.split("\\n");
            List<String> cleanLines = new ArrayList<>();
            for (String line : lines) {
                String clean = INLINE_WHITESPACE.matcher(line).replaceAll(" ").trim();
                if (!clean.isEmpty()) {
                    cleanLines.add(clean);
                }
            }
            if (!cleanLines.isEmpty()) {
                cleanParagraphs.add(String.join(compactCjk ? "" : " ", cleanLines));
            }
        }
        String result = String.join("\n\n", cleanParagraphs).trim();
        if (result.length() <= MAX_CHARS) {
            return result;
        }
        return result.substring(0, MAX_CHARS).trim();
    }

    private static boolean isCjkLanguage(String languageCode) {
        if (languageCode == null) {
            return false;
        }
        String language = languageCode.toLowerCase(Locale.ROOT);
        return language.startsWith("zh_") || language.startsWith("ja_") || language.startsWith("ko_");
    }

    private static String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }
}
