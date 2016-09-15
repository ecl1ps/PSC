package com.pokescanner.helper;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokescanner.objects.FilterItem;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.settings.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by Brian on 7/21/2016.
 */
public class PokemonListLoader {

    public static final int FILTER = 0;
    public static final int NOTIFICATION = 1;

    public static ArrayList getPokelist(Context context, int type) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        if (type == FILTER) {
            populateFilterPokemonList(context);
        } else {
            populateNotificationPokemonList(context);
        }
        ArrayList<?> returnlist = new ArrayList<>(realm.copyFromRealm(
                realm.where(type == FILTER ? FilterItem.class : NotificationItem.class)
                        .findAll()
                        .sort("Number")));
        realm.close();
        return returnlist;
    }

    public static ArrayList<FilterItem> getFilteredList() {
        Realm realm = Realm.getDefaultInstance();
        ArrayList<FilterItem> returnArray = new ArrayList<>(realm.copyFromRealm(
                realm.where(FilterItem.class)
                        .equalTo("filtered", true)
                        .findAll()
                        .sort("Number")));
        realm.close();
        return returnArray;
    }

    public static ArrayList<NotificationItem> getNotificationList() {
        Realm realm = Realm.getDefaultInstance();
        ArrayList<NotificationItem> returnArray = new ArrayList<>(realm.copyFromRealm(
                realm.where(NotificationItem.class)
                        .findAll()
                        .sort("Number")));
        realm.close();
        return returnArray;
    }

    public static ArrayList<NotificationItem> getNotificationListForProfile(Context context) {
        Realm realm = Realm.getDefaultInstance();
        ArrayList<NotificationItem> returnArray = new ArrayList<>(realm.copyFromRealm(
                realm.where(NotificationItem.class)
                        .contains("profiles", Settings.getPreferenceString(context, Settings.PROFILE))
                        .findAll()
                        .sort("Number")));
        realm.close();
        return returnArray;
    }

    public static ArrayList<NotificationItem> getCatchableNotificationList() {
        Realm realm = Realm.getDefaultInstance();
        ArrayList<NotificationItem> returnArray = new ArrayList<>(realm.copyFromRealm(
                realm.where(NotificationItem.class)
                        .contains("profiles", "Catchable")
                        .findAll()
                        .sort("Number")));
        realm.close();
        return returnArray;
    }

    public static void savePokeList(final ArrayList<FilterItem> pokelist) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(pokelist);
            }
        });
        realm.close();
    }

    public static void populatePokemonList(Context context) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        if (realm.where(FilterItem.class).findAll().size() != 151 || realm.where(NotificationItem.class).findAll().size() != 151) {
            InputStream is = context.getAssets().open("pokemons.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String bufferString = new String(buffer);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<FilterItem>>() {
            }.getType();
            final ArrayList<FilterItem> filterItems = gson.fromJson(bufferString, listType);
            translateNamesIfNeeded(context, filterItems);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(filterItems);
                }
            });
            listType = new TypeToken<ArrayList<NotificationItem>>() {
            }.getType();
            final ArrayList<NotificationItem> notificationItem = gson.fromJson(bufferString, listType);
//            translateNamesIfNeeded(context, notificationItem);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(notificationItem);
                }
            });
        }
        realm.close();
    }

    public static void populateFilterPokemonList(Context context) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        if (realm.where(FilterItem.class).findAll().size() != 151) {
            InputStream is = context.getAssets().open("pokemons.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String bufferString = new String(buffer);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<FilterItem>>() {
            }.getType();
            final ArrayList<FilterItem> filterItems = gson.fromJson(bufferString, listType);
            translateNamesIfNeeded(context, filterItems);
            if (realm.where(FilterItem.class).findAll().size() < 151 && realm.where(FilterItem.class).findAll().size() > 0) {
                filterItems.removeAll(getFilteredList());
            }
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(filterItems);
                }
            });
        }
        realm.close();
    }

    public static void populateNotificationPokemonList(Context context) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        if (realm.where(NotificationItem.class).findAll().size() != 151) {
            InputStream is = context.getAssets().open("pokemons.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String bufferString = new String(buffer);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<NotificationItem>>() {
            }.getType();
            final ArrayList<NotificationItem> notificationItem = gson.fromJson(bufferString, listType);
            if (realm.where(NotificationItem.class).findAll().size() < 151 && realm.where(NotificationItem.class).findAll().size() > 0) {
                notificationItem.removeAll(getNotificationList());
            }
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(notificationItem);
                }
            });
        }
        realm.close();
    }

    private static void translateNamesIfNeeded(Context context, ArrayList<FilterItem> filterItems) {
        if (!Settings.getPreferenceBoolean(context, Settings.FORCE_ENGLISH_NAMES)) {
            for (FilterItem item : filterItems) {
                String identifierName = "p" + Integer.toString(item.getNumber());
                int resourceID = context.getResources().getIdentifier(identifierName, "string", context.getPackageName());
                if (resourceID != 0) {
                    item.setName(context.getResources().getString(resourceID));
                }
            }
        }
    }
}
