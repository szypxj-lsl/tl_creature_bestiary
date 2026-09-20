package com.szypxj.tlcreaturebestiary.profile;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProviderRegistry;
import com.szypxj.tlcreaturebestiary.profile.provider.GenericBestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.profile.provider.VanillaBestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.profile.provider.compat.ErsBestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.profile.provider.compat.FossilBestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.profile.provider.compat.IceAndFireBestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.profile.provider.compat.RorBestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.profile.provider.compat.SaintsDragonsBestiaryProfileProvider;

public final class BuiltinBestiaryProfiles {
    private static boolean registered;

    private BuiltinBestiaryProfiles() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BestiaryProfileProviderRegistry.register(new IceAndFireBestiaryProfileProvider());
        BestiaryProfileProviderRegistry.register(new ErsBestiaryProfileProvider());
        BestiaryProfileProviderRegistry.register(new RorBestiaryProfileProvider());
        BestiaryProfileProviderRegistry.register(new SaintsDragonsBestiaryProfileProvider());
        BestiaryProfileProviderRegistry.register(new FossilBestiaryProfileProvider());
        BestiaryProfileProviderRegistry.register(new VanillaBestiaryProfileProvider());
        BestiaryProfileProviderRegistry.register(new GenericBestiaryProfileProvider());
    }
}
