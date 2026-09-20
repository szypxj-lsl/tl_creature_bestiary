package com.szypxj.tlcreaturebestiary.profile.provider.compat;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import com.szypxj.tlcreaturebestiary.profile.lore.ModJarTextResource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Method;
import java.util.Optional;

/** Reads Fossils & Archeology's own synchronized creature data and localized Dinopedia text. */
public final class FossilBestiaryProfileProvider implements BestiaryProfileProvider {
    private static final String MOD_ID = "fossil";
    private static final String INFO_CLASS =
            "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricEntityInfo";

    @Override
    public String id() {
        return "tl_creature_bestiary:compat/fossil";
    }

    @Override
    public int priority() {
        return 760;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        return context != null && MOD_ID.equals(context.entityTypeId().getNamespace());
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        Object info = findInfo(context.type());
        BestiaryProfileField<Component> species = species(info);
        BestiaryProfileField<BestiaryDiet> diet = diet(info);
        BestiaryProfileField<Component> description = officialLore(context)
                .map(text -> new BestiaryProfileField<>(
                        Component.literal(text),
                        BestiaryProfileSource.MOD_COMPAT,
                        BestiaryProfileConfidence.AUTHORITATIVE
                ))
                .orElse(null);
        return new BestiaryProfileContribution(species, diet, description);
    }

    private static Object findInfo(EntityType<?> type) {
        if (type == null) {
            return null;
        }
        try {
            Class<?> infoClass = Class.forName(INFO_CLASS);
            Method entityType = infoClass.getMethod("entityType");
            Object[] values = infoClass.getEnumConstants();
            if (values == null) {
                return null;
            }
            for (Object value : values) {
                if (entityType.invoke(value) == type) {
                    return value;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }

    private static BestiaryProfileField<Component> species(Object info) {
        String type = enumName(invoke(info, "mobType"));
        String key = switch (type) {
            case "DINOSAUR", "DINOSAUR_AQUATIC", "DINOSAUR_FISH" ->
                    "gui.tl_creature_bestiary.profile.species.dinosaur";
            case "ARTHROPOD" -> "gui.tl_creature_bestiary.profile.species.arthropod";
            case "FISH" -> "gui.tl_creature_bestiary.profile.species.aquatic";
            case "MAMMAL", "BIRD", "VANILLA_BIRD", "TERRORBIRD" ->
                    "gui.tl_creature_bestiary.profile.species.land_animal";
            default -> null;
        };
        return key == null ? null : new BestiaryProfileField<>(
                Component.translatable(key),
                BestiaryProfileSource.MOD_COMPAT,
                BestiaryProfileConfidence.HIGH
        );
    }

    private static BestiaryProfileField<BestiaryDiet> diet(Object info) {
        Object data = invoke(info, "data");
        String name = enumName(invoke(data, "diet"));
        BestiaryDiet value = switch (name) {
            case "HERBIVORE" -> BestiaryDiet.HERBIVORE;
            case "OMNIVORE" -> BestiaryDiet.OMNIVORE;
            case "CARNIVORE", "PISCIVORE", "CARNIVORE_EGG", "INSECTIVORE", "PISCI_CARNIVORE" ->
                    BestiaryDiet.CARNIVORE;
            default -> null;
        };
        return value == null ? null : new BestiaryProfileField<>(
                value,
                BestiaryProfileSource.MOD_COMPAT,
                BestiaryProfileConfidence.AUTHORITATIVE
        );
    }

    private static Optional<String> officialLore(BestiaryProfileContext context) {
        return ModJarTextResource.readLocalized(
                        MOD_ID,
                        context.languageCode(),
                        language -> "assets/fossil/dinopedia/" + language + "/"
                                + context.entityTypeId().getPath() + ".txt"
                )
                .map(String::trim)
                .filter(text -> !text.isBlank());
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> enumValue ? enumValue.name() : "";
    }
}
