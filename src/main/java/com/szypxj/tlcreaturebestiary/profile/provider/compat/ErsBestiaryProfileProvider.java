package com.szypxj.tlcreaturebestiary.profile.provider.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public final class ErsBestiaryProfileProvider implements BestiaryProfileProvider {
    private static final String MOD_ID = "ers";
    private static final TagKey<EntityType<?>> PISCIVOROUS = tag("ers:piscivorous");
    private static final TagKey<EntityType<?>> PREDATOR = tag("ers:predator");
    private static final TagKey<EntityType<?>> SCAVENGER = tag("ers:scavenger");
    private static final TagKey<EntityType<?>> ARMORED = tag("ers:armored");

    @Override
    public String id() {
        return "tl_creature_bestiary:compat/ers";
    }

    @Override
    public int priority() {
        return 700;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        if (context == null) {
            return false;
        }
        String namespace = context.entityTypeId().getNamespace();
        return "ers".equals(namespace) || "oasis".equals(namespace);
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        AtlasEntry entry = readAtlasEntry(context).orElse(null);
        BestiaryProfileField<Component> species = entry == null || entry.name().isBlank()
                ? null
                : new BestiaryProfileField<>(
                        Component.literal(entry.name()),
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.AUTHORITATIVE
                );
        BestiaryProfileField<Component> description = entry == null || entry.description().isBlank()
                ? null
                : new BestiaryProfileField<>(
                        Component.literal(entry.description()),
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.AUTHORITATIVE
                );
        BestiaryProfileField<BestiaryDiet> diet = diet(context);
        return new BestiaryProfileContribution(species, diet, description);
    }

    private static BestiaryProfileField<BestiaryDiet> diet(BestiaryProfileContext context) {
        BestiaryDiet diet = null;
        if (isTagged(context, PISCIVOROUS) || isTagged(context, PREDATOR)) {
            diet = BestiaryDiet.CARNIVORE;
        } else if (isTagged(context, SCAVENGER)) {
            diet = BestiaryDiet.SCAVENGER;
        }
        if (diet == null) {
            return null;
        }
        return new BestiaryProfileField<>(
                diet,
                BestiaryProfileSource.MOD_COMPAT,
                BestiaryProfileConfidence.HIGH
        );
    }

    private static Optional<AtlasEntry> readAtlasEntry(BestiaryProfileContext context) {
        String namespace = context.entityTypeId().getNamespace();
        String entityPath = context.entityTypeId().getPath();
        return ModJarTextResource.readLocalized(
                        MOD_ID,
                        context.languageCode(),
                        language -> "assets/" + namespace + "/patchouli_books/manual/" + language
                                + "/entries/atlas/" + entityPath + ".json"
                )
                .flatMap(raw -> parseAtlasEntry(raw, context));
    }

    private static Optional<AtlasEntry> parseAtlasEntry(String raw, BestiaryProfileContext context) {
        try {
            String json = raw != null && !raw.isEmpty() && raw.charAt(0) == '\uFEFF' ? raw.substring(1) : raw;
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String name = stringValue(root, "name");
            String declaredEntity = "";
            String description = "";
            JsonElement pagesElement = root.get("pages");
            if (pagesElement != null && pagesElement.isJsonArray()) {
                for (JsonElement pageElement : pagesElement.getAsJsonArray()) {
                    if (!pageElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject page = pageElement.getAsJsonObject();
                    if (declaredEntity.isBlank() && page.has("entity")) {
                        declaredEntity = stringValue(page, "entity");
                    }
                    String type = stringValue(page, "type");
                    if (description.isBlank() && ("text".equals(type) || type.endsWith(":text")) && page.has("text")) {
                        description = OfficialLoreSanitizer.sanitizePatchouli(
                                stringValue(page, "text"),
                                context.languageCode()
                        );
                    }
                }
            }
            if (!declaredEntity.isBlank() && !context.entityTypeId().toString().equals(declaredEntity)) {
                return Optional.empty();
            }
            if (name.isBlank() && description.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AtlasEntry(name, description));
        } catch (RuntimeException error) {
            return Optional.empty();
        }
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    @SuppressWarnings("deprecation")
    private static boolean isTagged(BestiaryProfileContext context, TagKey<EntityType<?>> tag) {
        return context.type().builtInRegistryHolder().is(tag);
    }

    private static TagKey<EntityType<?>> tag(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, Objects.requireNonNull(ResourceLocation.tryParse(id)));
    }

    private record AtlasEntry(String name, String description) {
    }
}
