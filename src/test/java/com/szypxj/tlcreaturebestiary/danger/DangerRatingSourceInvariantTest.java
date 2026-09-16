package com.szypxj.tlcreaturebestiary.danger;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DangerRatingSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String rating = Files.readString(root.resolve("src/main/java/com/szypxj/tlcreaturebestiary/danger/DangerRating.java"));
        String screen = Files.readString(root.resolve("src/main/java/com/szypxj/tlcreaturebestiary/client/screen/BestiaryScreen.java"));
        String title = Files.readString(root.resolve("src/main/java/com/szypxj/tlcreaturebestiary/client/BestiarySpyglassTitleExtension.java"));

        assertContains(rating, "fromRadarScores");
        assertContains(screen, "DangerRating.fromBaseStats(displayStats)");
        assertContains(title, "DangerRating.fromBaseStats(context.baseStats())");
        assertNotContains(screen, "DangerRating.fromBaseHealth");
        assertNotContains(title, "DangerRating.fromBaseHealth");

        System.out.println("TCB_DANGER_RATING_SOURCE_PASS");
    }

    private static void assertContains(String text, String needle) {
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle);
        }
    }

    private static void assertNotContains(String text, String needle) {
        if (text.contains(needle)) {
            throw new AssertionError("Unexpected source invariant: " + needle);
        }
    }
}
