package com.szypxj.tlcreaturebestiary.profile.provider.compat;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

public final class RorBestiaryProfileProvider implements BestiaryProfileProvider {
    private static final TagKey<EntityType<?>> SAUROPOD = tag("ror:sauropod");
    private static final TagKey<EntityType<?>> PREDATOR = tag("ror:predator");
    private static final TagKey<EntityType<?>> APEXES = tag("ror:apexes");
    private static final TagKey<EntityType<?>> CONTROLABLE_DINOSAUR = tag("ror:controlable_dinosaur");

    @Override
    public String id() {
        return "tl_creature_bestiary:compat/ror";
    }

    @Override
    public int priority() {
        return 700;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        return context != null && "ror".equals(context.entityTypeId().getNamespace());
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        BestiaryProfileField<Component> species = null;
        if (isTagged(context, SAUROPOD)) {
            species = new BestiaryProfileField<>(
                    Component.translatable("gui.tl_creature_bestiary.profile.species.sauropod"),
                    BestiaryProfileSource.MOD_COMPAT,
                    BestiaryProfileConfidence.HIGH
            );
        }

        BestiaryProfileField<BestiaryDiet> diet = null;
        if (isTagged(context, PREDATOR)) {
            diet = new BestiaryProfileField<>(
                    BestiaryDiet.CARNIVORE,
                    BestiaryProfileSource.MOD_COMPAT,
                    BestiaryProfileConfidence.HIGH
            );
        }

        // ROR 0.7 already ships APEXES and CONTROLABLE_DINOSAUR tags, but its RORpedia is incomplete.
        // Keep those tags discoverable here for future facts without fabricating missing official lore.
        boolean apex = isTagged(context, APEXES);
        boolean controllable = isTagged(context, CONTROLABLE_DINOSAUR);
        if (apex && controllable) {
            // Intentionally no description override: the generic provider remains the safe fallback.
        }

        return new BestiaryProfileContribution(species, diet, null);
    }

    @SuppressWarnings("deprecation")
    private static boolean isTagged(BestiaryProfileContext context, TagKey<EntityType<?>> tag) {
        return context.type().builtInRegistryHolder().is(tag);
    }

    private static TagKey<EntityType<?>> tag(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, Objects.requireNonNull(ResourceLocation.tryParse(id)));
    }
}
