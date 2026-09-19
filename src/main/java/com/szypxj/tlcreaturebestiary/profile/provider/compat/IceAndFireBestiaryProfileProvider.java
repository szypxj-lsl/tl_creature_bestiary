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
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class IceAndFireBestiaryProfileProvider implements BestiaryProfileProvider {
    private static final String MOD_ID = "iceandfire";
    private static final Set<String> DRAGONS = Set.of(
            "fire_dragon",
            "ice_dragon",
            "lightning_dragon",
            "black_frost_dragon"
    );
    private static final Map<String, String> BESTIARY_SLUGS = Map.ofEntries(
            Map.entry("fire_dragon", "firedragon"),
            Map.entry("ice_dragon", "icedragon"),
            Map.entry("lightning_dragon", "lightningdragon"),
            Map.entry("hippogryph", "hippogryph"),
            Map.entry("gorgon", "gorgon"),
            Map.entry("pixie", "pixie"),
            Map.entry("cyclops", "cyclops"),
            Map.entry("siren", "siren"),
            Map.entry("hippocampus", "hippocampus"),
            Map.entry("deathworm", "deathworm"),
            Map.entry("cockatrice", "cockatrice"),
            Map.entry("stymphalian_bird", "stymphalianbird"),
            Map.entry("troll", "troll"),
            Map.entry("myrmex_worker", "myrmex"),
            Map.entry("myrmex_soldier", "myrmex"),
            Map.entry("myrmex_queen", "myrmex"),
            Map.entry("myrmex_sentinel", "myrmex"),
            Map.entry("myrmex_royal", "myrmex"),
            Map.entry("myrmex_swarmer", "myrmex"),
            Map.entry("amphithere", "amphithere"),
            Map.entry("sea_serpent", "seaserpent"),
            Map.entry("dread_thrall", "dread_mobs"),
            Map.entry("dread_ghoul", "dread_mobs"),
            Map.entry("dread_beast", "dread_mobs"),
            Map.entry("dread_scuttler", "dread_mobs"),
            Map.entry("dread_lich", "dread_mobs"),
            Map.entry("dread_knight", "dread_mobs"),
            Map.entry("dread_horse", "dread_mobs"),
            Map.entry("dread_queen", "dread_mobs"),
            Map.entry("hydra", "hydra"),
            Map.entry("ghost", "ghost")
    );

    @Override
    public String id() {
        return "tl_creature_bestiary:compat/iceandfire";
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
        String path = context.entityTypeId().getPath();
        String bestiarySlug = BESTIARY_SLUGS.get(path);

        BestiaryProfileField<Component> species = null;
        if (DRAGONS.contains(path)) {
            species = new BestiaryProfileField<>(
                    Component.translatable("gui.tl_creature_bestiary.profile.species.dragon"),
                    BestiaryProfileSource.MOD_COMPAT,
                    BestiaryProfileConfidence.HIGH
            );
        } else if (bestiarySlug != null) {
            species = new BestiaryProfileField<>(
                    Component.translatable(context.type().getDescriptionId()),
                    BestiaryProfileSource.MOD_COMPAT,
                    BestiaryProfileConfidence.HIGH
            );
        }

        BestiaryProfileField<BestiaryDiet> diet = null;
        if (DRAGONS.contains(path)) {
            diet = new BestiaryProfileField<>(
                    BestiaryDiet.CARNIVORE,
                    BestiaryProfileSource.MOD_COMPAT,
                    BestiaryProfileConfidence.HIGH
            );
        }

        BestiaryProfileField<Component> description = officialLore(context, bestiarySlug)
                .map(text -> new BestiaryProfileField<Component>(
                        Component.literal(text),
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.AUTHORITATIVE
                ))
                .orElse(null);

        return new BestiaryProfileContribution(species, diet, description);
    }

    private static Optional<String> officialLore(BestiaryProfileContext context, String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return ModJarTextResource.readLocalized(
                        MOD_ID,
                        context.languageCode(),
                        language -> "assets/iceandfire/lang/bestiary/" + language + "_0/" + slug + "_0.txt"
                )
                .map(raw -> OfficialLoreSanitizer.sanitizeIceAndFire(raw, context.languageCode()))
                .filter(text -> !text.isBlank());
    }
}
