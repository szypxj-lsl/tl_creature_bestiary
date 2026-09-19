package com.szypxj.tlcreaturebestiary.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TCBConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue HIGH_RISK_COMPLETION_REWARD_POINTS = BUILDER
            .comment("Unspent TDMC attribute points granted once when a high-risk investigation reaches 100%. Set to 0 to disable the reward.")
            .defineInRange("highRiskCompletionRewardPoints", 5, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private TCBConfig() {
    }
}
