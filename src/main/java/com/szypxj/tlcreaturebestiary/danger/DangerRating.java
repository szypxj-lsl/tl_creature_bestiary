package com.szypxj.tlcreaturebestiary.danger;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassRadarBaseline;

public final class DangerRating {
    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 9;

    private static final double POWER_WEIGHT = 0.50D;
    private static final double LIFE_WEIGHT = 0.35D;
    private static final double SPEED_WEIGHT = 0.15D;

    private DangerRating() {
    }

    public static int fromBaseStats(BaseStats stats) {
        if (stats == null) {
            return MIN_STARS;
        }
        return fromRadarScores(
                SpyglassRadarBaseline.powerPercentile(stats.attackDamage()),
                SpyglassRadarBaseline.lifePercentile(stats.maxHealth()),
                SpyglassRadarBaseline.speedPercentile(stats.movementSpeed())
        );
    }

    public static int fromRadarScores(int power, int life, int speed) {
        double score = clamp100(power) * POWER_WEIGHT
                + clamp100(life) * LIFE_WEIGHT
                + clamp100(speed) * SPEED_WEIGHT;
        if (score < 20.0D) {
            return MIN_STARS;
        }
        int stars = (int) Math.floor(score / 10.0D);
        return Math.max(MIN_STARS, Math.min(MAX_STARS, stars));
    }

    private static double clamp100(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
