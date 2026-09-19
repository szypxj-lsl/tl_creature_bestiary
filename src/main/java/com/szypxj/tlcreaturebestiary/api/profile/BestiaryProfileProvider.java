package com.szypxj.tlcreaturebestiary.api.profile;

public interface BestiaryProfileProvider {
    String id();

    int priority();

    boolean supports(BestiaryProfileContext context);

    BestiaryProfileContribution provide(BestiaryProfileContext context);
}
