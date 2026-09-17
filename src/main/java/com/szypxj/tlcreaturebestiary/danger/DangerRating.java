package com.szypxj.tlcreaturebestiary.danger;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;

public final class DangerRating {
    public static final int MIN_STARS = CreatureInfoApi.DANGER_MIN_STARS;
    public static final int MAX_STARS = CreatureInfoApi.DANGER_MAX_STARS;

    private DangerRating() {
    }

    public static int fromBaseStats(BaseStats stats) {
        return CreatureInfoApi.dangerStars(stats);
    }

    public static int fromBaseStats(BaseStats stats, boolean elite) {
        return CreatureInfoApi.dangerStars(stats, elite);
    }

    public static int fromRadarScores(int power, int life, int speed) {
        return CreatureInfoApi.dangerStarsFromRadarScores(power, life, speed);
    }

    public static int fromRadarScores(int power, int life, int speed, boolean elite) {
        return CreatureInfoApi.dangerStarsFromRadarScores(power, life, speed, elite);
    }
}
