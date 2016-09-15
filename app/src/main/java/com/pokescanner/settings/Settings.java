package com.pokescanner.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class Settings {

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
    public static final String ENABLE_SERVICE = "enableService";
    public static final String SERVICE_REFRESH = "serviceRefresh";
    public static final String ENABLE_SERVICE_ON_BOOT = "enableServiceOnBoot";
    public static final String GROUP_POKEMON = "groupPokemon";
    public static final String NOTIFICATION_RINGTONE = "notificationRingtone";
    public static final String NOTIFICATION_VIBRATE = "notificationVibrate";
    public static final String ENABLE_CUSTOM_LOCATION = "customLocationEnabled";
    public static final String CUSTOM_LOCATION = "customLocation";
    public static final String NOTIFIED_ENCOUNTERS = "notifiedEncounters";
    public static final String HIDE_TIMER = "hideTimer";
    public static final String PROFILE = "profile";

    private static final ArrayList<String> stringPrefs = new ArrayList<>(Arrays.asList(
            SERVER_REFRESH_RATE,
            MAP_REFRESH_RATE,
            POKEMON_ICON_SCALE,
            LAST_USERNAME,
            SERVICE_REFRESH,
            NOTIFICATION_RINGTONE,
            CUSTOM_LOCATION,
            NOTIFIED_ENCOUNTERS
    ));

    private static final ArrayList<String> intPrefs = new ArrayList<>(Arrays.asList(
            SCAN_VALUE,
            GUARD_MIN_CP,
            GUARD_MAX_CP
    ));

    // Settings default values
    public static final boolean DEFAULT_updatesEnabled = false;
    public static final boolean DEFAULT_boundingBoxEnabled = false;
    public static final boolean DEFAULT_enableLowMemory = true;
    public static final boolean DEFAULT_showLuredPokemon = true;
    public static final boolean DEFAULT_useOldMapMarker = false;
    public static final boolean DEFAULT_drivingModeEnabled = false;
    public static final boolean DEFAULT_forceEnglishNames = false;
    public static final String DEFAULT_serverRefreshRate = "10";
    public static final String DEFAULT_mapRefreshRate = "2";
    public static final String DEFAULT_pokemonIconScale = "1";
    public static final int DEFAULT_scanValue = 4;
    public static final String DEFAULT_lastUsername = "";
    public static final boolean DEFAULT_showNeutralGyms = false;
    public static final boolean DEFAULT_showYellowGyms = false;
    public static final boolean DEFAULT_showBlueGyms = false;
    public static final boolean DEFAULT_showRedGyms = false;
    public static final int DEFAULT_guardPokemonMinCp = 1;
    public static final int DEFAULT_guardPokemonMaxCp = 1999;
    public static final boolean DEFAULT_shuffleIcons = false;
    public static final boolean DEFAULT_showLuredPokestops = false;
    public static final boolean DEFAULT_showNormalPokestops = false;
    public static final boolean DEFAULT_enableService = false;
    public static final String DEFAULT_serviceRefresh = "1800000";
    public static final boolean DEFAULT_enableServiceOnBoot = false;
    public static final boolean DEFAULT_groupPokemon = false;
    public static final String DEFAULT_notificationRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();
    public static final boolean DEFAULT_notificationVibrate = true;
    public static final boolean DEFAULT_customLocationEnabled = false;
    public static final String DEFAULT_customLocation = null;
    public static final String DEFAULT_notifiedEncounters = null;
    public static final boolean DEFAULT_hideTimer = false;
    public static final String DEFAULT_profile = "Default";


    public static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static JSONObject toJSONObject(Context context) throws JSONException {
        JSONObject result = new JSONObject();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> keys = prefs.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    public static void loadSharedPreferencesFromFile(Context context, Map<String, ?> input) {
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefEdit.clear();
        for (Map.Entry<String, ?> entry : input.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            if (stringPrefs.contains(key)) {
                if (v instanceof Integer) {
                    prefEdit.putString(key, String.valueOf((Integer) v));
                } else {
                    prefEdit.putString(key, (String) v);
                }
            } else if (intPrefs.contains(key)) {
                if (v instanceof String) {
                    prefEdit.putInt(key, Integer.valueOf((String) v));
                } else {
                    prefEdit.putInt(key, (Integer) v);
                }
            } else {
                if (v instanceof String) {
                    prefEdit.putBoolean(key, Boolean.valueOf((String) v));
                } else {
                    prefEdit.putBoolean(key, (Boolean) v);
                }
            }
        }
        prefEdit.apply();
    }

    public static ArrayList<Long> getNotifiedEncounters(Context context) {
        ArrayList<Long> encounters = new ArrayList<>();
        String notifiedEncounters = getPrefs(context).getString(NOTIFIED_ENCOUNTERS, DEFAULT_notifiedEncounters);
        if (notifiedEncounters != null) {
            String[] encountersString = notifiedEncounters.split(",");
            for (String encounter : encountersString) {
                encounters.add(Long.parseLong(encounter));
            }
        }
        return encounters;
    }

    public static void setNotifiedEncounters(Context context, ArrayList<Long> encounters) {
        StringBuilder sb = new StringBuilder();
        for (long encounter : encounters) {
            sb.append(encounter).append(",");
        }
        getPrefs(context).edit().putString(NOTIFIED_ENCOUNTERS, sb.toString()).apply();
    }

    public static LatLng getCustomLocation(Context context) {
        String customLocation = getPrefs(context).getString(CUSTOM_LOCATION, DEFAULT_customLocation);
        if (customLocation != null) {
            String[] loc = customLocation.split(",");
            double lat = Double.parseDouble(loc[0]);
            double lon = Double.parseDouble(loc[1]);
            return new LatLng(lat, lon);
        }
        return null;
    }

    public static String getCustomLocationString(Context context) {
        return getPrefs(context).getString(CUSTOM_LOCATION, DEFAULT_customLocation);
    }

    public static void setCustomLocation(Context context, LatLng location) {
        getPrefs(context).edit().putString(CUSTOM_LOCATION, location.latitude + "," + location.longitude).apply();
    }


    public static void setPreference(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).apply();
    }

    public static void setPreference(Context context, String key, int value) {
        getPrefs(context).edit().putInt(key, value).apply();

    }

    public static void setPreference(Context context, String key, boolean value) {
        getPrefs(context).edit().putBoolean(key, value).apply();

    }

    public static String getPreferenceString(Context context, String key) {
        Field field = null;
        try {
            field = Settings.class.getField("DEFAULT_" + key);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        Settings settings = new Settings();
        String json = null;
        try {
            json = getPrefs(context).getString(key, (String) field.get(settings));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        return json;
    }

    public static int getPreferenceInt(Context context, String key) {
        //Handle int values that are stored as Strings
        if (stringPrefs.contains(key)) {
            return Integer.parseInt(getPreferenceString(context, key));
        }

        Field field = null;
        try {
            field = Settings.class.getField("DEFAULT_" + key);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        Settings settings = new Settings();
        try {
            return getPrefs(context).getInt(key, (int) field.get(settings));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean getPreferenceBoolean(Context context, String key) {
        Field field = null;
        try {
            field = Settings.class.getField("DEFAULT_" + key);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        Settings settings = new Settings();
        try {
            return getPrefs(context).getBoolean(key, (boolean) field.get(settings));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
