package com.szypxj.tlcreaturebestiary.profile.provider;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

public final class VanillaBestiaryProfileProvider implements BestiaryProfileProvider {
    private static final Set<EntityType<?>> CONSTRUCTS = set(
            EntityType.IRON_GOLEM,
            EntityType.SNOW_GOLEM
    );
    private static final Set<EntityType<?>> UNDEAD = set(
            EntityType.ZOMBIE,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.PHANTOM,
            EntityType.WITHER
    );
    private static final Set<EntityType<?>> ARTHROPODS = set(
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.SILVERFISH,
            EntityType.ENDERMITE,
            EntityType.BEE
    );
    private static final Set<EntityType<?>> ILLAGERS = set(
            EntityType.PILLAGER,
            EntityType.VINDICATOR,
            EntityType.EVOKER,
            EntityType.ILLUSIONER,
            EntityType.RAVAGER
    );
    private static final Set<EntityType<?>> HERBIVORES = set(
            EntityType.COW,
            EntityType.MOOSHROOM,
            EntityType.SHEEP,
            EntityType.GOAT,
            EntityType.HORSE,
            EntityType.DONKEY,
            EntityType.MULE,
            EntityType.LLAMA,
            EntityType.TRADER_LLAMA,
            EntityType.RABBIT,
            EntityType.CAMEL,
            EntityType.PANDA,
            EntityType.TURTLE,
            EntityType.PARROT
    );
    private static final Set<EntityType<?>> CARNIVORES = set(
            EntityType.WOLF,
            EntityType.CAT,
            EntityType.OCELOT,
            EntityType.POLAR_BEAR
    );
    private static final Set<EntityType<?>> OMNIVORES = set(
            EntityType.PIG,
            EntityType.CHICKEN,
            EntityType.FOX
    );

    @Override
    public String id() {
        return "tl_creature_bestiary:vanilla";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        return context != null && "minecraft".equals(context.entityTypeId().getNamespace());
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        EntityType<?> type = context.type();
        BestiaryProfileField<Component> species = species(type);
        BestiaryProfileField<BestiaryDiet> diet = diet(type);
        return new BestiaryProfileContribution(species, diet, null);
    }

    private static BestiaryProfileField<Component> species(EntityType<?> type) {
        String key = null;
        if (CONSTRUCTS.contains(type)) {
            key = "gui.tl_creature_bestiary.profile.species.construct";
        } else if (UNDEAD.contains(type)) {
            key = "gui.tl_creature_bestiary.profile.species.undead";
        } else if (ARTHROPODS.contains(type)) {
            key = "gui.tl_creature_bestiary.profile.species.arthropod";
        } else if (ILLAGERS.contains(type)) {
            key = "gui.tl_creature_bestiary.profile.species.illager";
        }
        if (key == null) {
            return null;
        }
        return new BestiaryProfileField<>(
                Component.translatable(key),
                BestiaryProfileSource.VANILLA,
                BestiaryProfileConfidence.HIGH
        );
    }

    private static BestiaryProfileField<BestiaryDiet> diet(EntityType<?> type) {
        BestiaryDiet value = null;
        if (HERBIVORES.contains(type)) {
            value = BestiaryDiet.HERBIVORE;
        } else if (CARNIVORES.contains(type)) {
            value = BestiaryDiet.CARNIVORE;
        } else if (OMNIVORES.contains(type)) {
            value = BestiaryDiet.OMNIVORE;
        }
        if (value == null) {
            return null;
        }
        return new BestiaryProfileField<>(
                value,
                BestiaryProfileSource.VANILLA,
                BestiaryProfileConfidence.MEDIUM
        );
    }

    private static Set<EntityType<?>> set(EntityType<?>... types) {
        return Set.of(types);
    }
}
