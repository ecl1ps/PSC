package com.pokescanner.utils;

import com.pokescanner.settings.Settings;

import io.realm.Realm;


public class SettingsUtil {

    public static final String ENABLE_UPDATES = "updatesEnabled";
    public static final String KEY_BOUNDING_BOX = "boundingBoxEnabled";
    public static final String ENABLE_LOW_MEMORY = "enableLowMemory";
    public static final String SHOW_LURED_POKEMON = "showLuredPokemon";
    public static final String KEY_OLD_MARKER = "useOldMapMarker";
    public static final String DRIVING_MODE = "drivingModeEnabled";
    public static final String FORCE_ENGLISH_NAMES = "forceEnglishNames";
    public static final String SERVER_REFRESH_RATE = "serverRefreshRate";
    public static final String MAP_REFRESH_RATE = "mapRefreshRate";
    public static final String POKEMON_ICON_SCALE = "pokemonIconScale";
    public static final String SCAN_VALUE = "scanValue";
    public static final String LAST_USERNAME = "lastUsername";
    public static final String SHOW_NEUTRAL_GYMS = "showNeutralGyms";
    public static final String SHOW_YELLOW_GYMS = "showYellowGyms";
    public static final String SHOW_BLUE_GYMS = "showBlueGyms";
    public static final String SHOW_RED_GYMS = "showRedGyms";
    public static final String GUARD_MIN_CP = "guardPokemonMinCp";
    public static final String GUARD_MAX_CP = "guardPokemonMaxCp";
    public static final String SHUFFLE_ICONS = "shuffleIcons";
    public static final String SHOW_LURED_POKESTOPS = "showLuredPokestops";
    public static final String SHOW_NORMAL_POKESTOPS = "showNormalPokestops";

    public static Settings getSettings()
    {
        Realm realm = Realm.getDefaultInstance();
        if (realm.where(Settings.class).findFirst() == null)
        {
            realm.close();
            return new Settings();
        }
        Settings currentSettings = realm.copyFromRealm(realm.where(Settings.class).findFirst());
        realm.close();
        return new Settings(currentSettings);
    }

    public static void saveSettings(final Settings settings)
    {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(settings);
            }
        });
        realm.close();
    }
}
