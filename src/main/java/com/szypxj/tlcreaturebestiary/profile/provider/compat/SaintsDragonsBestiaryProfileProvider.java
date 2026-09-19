package com.szypxj.tlcreaturebestiary.profile.provider.compat;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import com.szypxj.tlcreaturebestiary.profile.lore.ModJarTextResource;
import com.szypxj.tlcreaturebestiary.profile.lore.OfficialLoreSanitizer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SaintsDragonsBestiaryProfileProvider implements BestiaryProfileProvider {
    private static final String MOD_ID = "saintsdragons";
    private static final TagKey<EntityType<?>> DRAGONS = tag("saintsdragons:dragons");
    private static final TagKey<EntityType<?>> FLYING_DRAGONS = tag("saintsdragons:flying_dragons");
    private static final TagKey<EntityType<?>> GROUNDED_DRAGONS = tag("saintsdragons:grounded_dragons");
    private static final TagKey<EntityType<?>> SWIMMING_DRAGONS = tag("saintsdragons:swimming_dragons");
    private static final TagKey<EntityType<?>> TAMEABLE_DRAGONS = tag("saintsdragons:tameable_dragons");
    private static final TagKey<EntityType<?>> RIDEABLE_DRAGONS = tag("saintsdragons:rideable_dragons");
    private static final Set<String> AUDITED_CARNIVORES = Set.of(
            "cindervane",
            "ignivorus",
            "raevyx",
            "varasuchus",
            "volitans"
    );

    @Override
    public String id() {
        return "tl_creature_bestiary:compat/saintsdragons";
    }

    @Override
    public int priority() {
        return 700;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        return context != null && MOD_ID.equals(context.entityTypeId().getNamespace());
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        BestiaryProfileField<Component> species = isTagged(context, DRAGONS)
                ? new BestiaryProfileField<>(
                        Component.translatable("gui.tl_creature_bestiary.profile.species.dragon"),
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.AUTHORITATIVE
                )
                : null;

        BestiaryProfileField<BestiaryDiet> diet = AUDITED_CARNIVORES.contains(context.entityTypeId().getPath())
                ? new BestiaryProfileField<>(
                        BestiaryDiet.CARNIVORE,
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.HIGH
                )
                : null;

        BestiaryProfileField<Component> description = officialEcology(context)
                .map(text -> new BestiaryProfileField<Component>(
                        Component.literal(text),
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.AUTHORITATIVE
                ))
                .orElse(null);

        // Force tag resolution for the movement/taming facts shipped by the mod. They are intentionally not
        // rewritten into lore when an official ecology paragraph exists; the data remains available for future fields.
        isTagged(context, FLYING_DRAGONS);
        isTagged(context, GROUNDED_DRAGONS);
        isTagged(context, SWIMMING_DRAGONS);
        isTagged(context, TAMEABLE_DRAGONS);
        isTagged(context, RIDEABLE_DRAGONS);

        return new BestiaryProfileContribution(species, diet, description);
    }

    private static Optional<String> officialEcology(BestiaryProfileContext context) {
        String entityPath = context.entityTypeId().getPath();
        return ModJarTextResource.readLocalized(
                        MOD_ID,
                        context.languageCode(),
                        language -> "assets/saintsdragons/codex/" + language + "/ecology/" + entityPath + ".txt"
                )
                .map(raw -> OfficialLoreSanitizer.sanitizePlainText(raw, context.languageCode()))
                .filter(text -> !text.isBlank());
    }

    @SuppressWarnings("deprecation")
    private static boolean isTagged(BestiaryProfileContext context, TagKey<EntityType<?>> tag) {
        return context.type().builtInRegistryHolder().is(tag);
    }

    private static TagKey<EntityType<?>> tag(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, Objects.requireNonNull(ResourceLocation.tryParse(id)));
    }
}
