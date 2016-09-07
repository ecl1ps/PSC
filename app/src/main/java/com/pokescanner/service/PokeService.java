package com.pokescanner.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.TimeUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pokescanner.MapsActivity;
import com.pokescanner.R;
import com.pokescanner.helper.PokeDistanceSorter;
import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.SettingsUtil;
import com.pokescanner.utils.UiUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;

import static com.pokescanner.helper.Generation.makeHexScanMap;

/**
 * Created by Brian on 7/26/2016.
 */
public class PokeService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    List<LatLng> scanMap = new ArrayList<>();
    static LatLng location;
    static Realm realm;


    public PokeService() {
        super("PokeScanner");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        scanForPoke();
    }

    private void scanForPoke() {
        Fabric.with(this, new Crashlytics());
        Settings currentSettings = SettingsUtil.getSettings();
        int SERVER_REFRESH_RATE = currentSettings.getServerRefresh();
        int scanValue = currentSettings.getScanValue();
        PokeNotifications.ongiongNotification(getString(R.string.scan_running) + " " + UiUtils.getSearchTimeString(SettingsUtil.getSettings().getScanValue(), this), this);

        realm = Realm.getDefaultInstance();

        //get our saved or current position
        if (currentSettings.isCustomLocationEnabled()) {
            if (currentSettings.getCustomLocation() != null) {
                location = currentSettings.getCustomLocation();
            } else {
                PokeNotifications.ongiongNotification(getString(R.string.custom_location_invalid), this);
                location = getCurrentLocation();
            }
        } else {
            location = getCurrentLocation();
        }

        if (location != null) {
            scanMap = makeHexScanMap(location, scanValue, 1, new ArrayList<LatLng>());

            if (scanMap != null) {
                //Pull our users from the realm
                ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));

                MultiAccountLoader.setSleepTime(UiUtils.BASE_DELAY * SERVER_REFRESH_RATE);
                //Set our map
                MultiAccountLoader.setScanMap(scanMap);
                //Set our users
                MultiAccountLoader.setUsers(users);
                MultiAccountLoader.setContext(this);
                MultiAccountLoader.setIsBackground(true);
                //Begin our threads???
                MultiAccountLoader.startThreads();
            } else {
                PokeNotifications.ongiongNotification("Scan failed", this);
            }
        } else {
            PokeNotifications.ongiongNotification("Scan failed", this);
        }
        realm.close();
    }


    @SuppressWarnings({"MissingPermission"})
    public LatLng getCurrentLocation() {
        if (PermissionUtils.doWeHaveGPSandLOC(this)) {
            if (mGoogleApiClient.isConnected()) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location != null) {
                    return new LatLng(location.getLatitude(), location.getLongitude());
                }
                return null;
            }
            return null;
        }
        return null;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    static private ArrayList<Pokemons> pokemonRecycler = new ArrayList<>();

    public static void pokeNotification(Context context) {
        String content = String.format(context.getString(R.string.scan_complete), TimeUnit.MILLISECONDS.toMinutes(SettingsUtil.getSettings().getServiceRefresh()));
        PokeNotifications.ongiongNotification(content, context);

        realm = Realm.getDefaultInstance();
        //Create an arraylist to hold our new pokemon
        ArrayList<Pokemons> pokemon = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
        //Clear our current list
        pokemonRecycler.clear();

        //Alright does our incomming list contain any pokemon that have expired?
        for (int i = 0; i < pokemon.size(); i++) {
            //Put our pokemon inside an object
            Pokemons temp = pokemon.get(i);
            //Now we check has it expired
            if (!temp.isExpired()) {
                //If it has lets removed the pokemon
                realm.beginTransaction();
                realm.where(Pokemons.class).equalTo("encounterid", temp.getEncounterid()).findAll().deleteAllFromRealm();
                pokemon.remove(i);
                realm.commitTransaction();
            }
        }
        realm.close();

        //If the value isnt null then lets continue
        if (location != null) {
            Location tempLocation = new Location("");
            tempLocation.setLatitude(location.latitude);
            tempLocation.setLongitude(location.longitude);
            //Write distance to pokemons
            for (int i = 0; i < pokemon.size(); i++) {
                Pokemons pokemons = pokemon.get(i);

                if (PokemonListLoader.getNotificationList().contains(new NotificationItem(pokemons.getNumber()))) {
                    //DO MATH
                    Location temp = new Location("");

                    temp.setLatitude(pokemons.getLatitude());
                    temp.setLongitude(pokemons.getLongitude());

                    double distance = tempLocation.distanceTo(temp);
                    pokemons.setDistance((int) Math.round(distance));
                    pokemons.setBearing(getBearing(location, temp));

                    //ADD OUR POKEMANS TO OUR OUT LIST
                    pokemonRecycler.add(pokemons);
                }
            }
        }

        if (pokemonRecycler.size() > 0) {
            Collections.sort(pokemonRecycler, new PokeDistanceSorter());
            removeAlreadyNotified(pokemonRecycler);
            PokeNotifications.pokeNotification(context, pokemonRecycler);
        }

        Log.d("POKE", "Found " + pokemonRecycler.size() + " pokemon");
    }

    private static void removeAlreadyNotified(ArrayList<Pokemons> pokemonRecycler) {
        Settings settings = SettingsUtil.getSettings();
        ArrayList<Long> notifiedEncounters = settings.getNotifiedEncounters();
        ArrayList<Pokemons> templist = new ArrayList<>();
        for (Pokemons pokemon : pokemonRecycler) {
            if (notifiedEncounters.contains(pokemon.getEncounterid())) {
                templist.add(pokemon);
            } else {
                notifiedEncounters.add(pokemon.getEncounterid());
            }
        }
        pokemonRecycler.removeAll(templist);
        settings.setNotifiedEncounters(notifiedEncounters);
        SettingsUtil.saveSettings(settings);
    }

    protected static String getBearing(LatLng user, Location pokemon) {
        double longitude1 = user.longitude;
        double longitude2 = pokemon.getLongitude();
        double latitude1 = Math.toRadians(user.latitude);
        double latitude2 = Math.toRadians(pokemon.getLatitude());
        double longDiff = Math.toRadians(longitude2 - longitude1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        double resultDegree = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        String coordNames[] = {"North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest", "North"};
        double directionid = Math.round(resultDegree / 45);
        if (directionid < 0) {
            directionid = directionid + 9;
        }

        return coordNames[(int) directionid];
    }


}