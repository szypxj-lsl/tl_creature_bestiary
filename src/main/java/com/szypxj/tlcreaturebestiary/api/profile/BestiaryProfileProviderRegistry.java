package com.szypxj.tlcreaturebestiary.api.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class BestiaryProfileProviderRegistry {
    private static final List<BestiaryProfileProvider> PROVIDERS = new ArrayList<>();

    private BestiaryProfileProviderRegistry() {
    }

    public static synchronized void register(BestiaryProfileProvider provider) {
        Objects.requireNonNull(provider, "provider");
        PROVIDERS.removeIf(existing -> existing.id().equals(provider.id()));
        PROVIDERS.add(provider);
        PROVIDERS.sort(Comparator.comparingInt(BestiaryProfileProvider::priority).reversed());
    }

    public static synchronized List<BestiaryProfileProvider> providers() {
        return List.copyOf(PROVIDERS);
    }
}
