package com.szypxj.tlcreaturebestiary.danger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DangerRatingTest {
    @Test
    void radarScoresDriveFixedSpeciesDangerRating() {
        assertEquals(8, DangerRating.fromRadarScores(99, 81, 27));
        assertEquals(1, DangerRating.fromRadarScores(0, 0, 0));
        assertEquals(1, DangerRating.fromRadarScores(19, 19, 19));
        assertEquals(2, DangerRating.fromRadarScores(20, 20, 20));
        assertEquals(5, DangerRating.fromRadarScores(100, 0, 0));
        assertEquals(9, DangerRating.fromRadarScores(90, 90, 90));
        assertEquals(9, DangerRating.fromRadarScores(100, 100, 100));
    }
}
