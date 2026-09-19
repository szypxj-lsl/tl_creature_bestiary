package com.szypxj.tlcreaturebestiary.api.profile;

import java.util.Objects;

public record BestiaryProfileField<T>(
        T value,
        BestiaryProfileSource source,
        BestiaryProfileConfidence confidence
) {
    public BestiaryProfileField {
        value = Objects.requireNonNull(value, "value");
        source = source == null ? BestiaryProfileSource.UNKNOWN : source;
        confidence = confidence == null ? BestiaryProfileConfidence.UNKNOWN : confidence;
    }
}
