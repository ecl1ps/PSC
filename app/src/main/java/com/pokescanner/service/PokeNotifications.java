package com.pokescanner.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.pokescanner.MapsActivity;
import com.pokescanner.R;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.DrawableUtils;
import com.pokescanner.utils.SettingsUtil;

import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Jason on 9/1/2016.
 */

public class PokeNotifications {
    public static final int ONGOING_ID = 213;
    public static final int GROUPED_ID = 312;
    public static final String STOP_SERVICE = "stop_service";
    public static final String RESCAN = "rescan";


    public static void ongiongNotification(String content, Context context) {
        Intent stopIntent = new Intent(context, PokeReceiver.class);
        stopIntent.setAction(STOP_SERVICE);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(context, 0, stopIntent, 0);

        Intent rescanIntent = new Intent(context, PokeReceiver.class);
        rescanIntent.setAction(RESCAN);
        PendingIntent pendingIntentRescan = PendingIntent.getBroadcast(context, 0, rescanIntent, 0);


        NotificationCompat.Builder builder = setupSilentNotification(context)
                .addAction(new NotificationCompat.Action(R.drawable.ic_close_white_24dp, "Cancel Service", pendingIntentStop))
                .addAction(new NotificationCompat.Action(R.drawable.ic_refresh_white_36dp, "Rescan", pendingIntentRescan))
                .setContentTitle("Poke Scanner")
                .setContentText(content)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN);

        notify(context, ONGOING_ID, builder);
    }

    public static void pokeNotification(Context context, ArrayList<Pokemons> pokemonRecycler){
        if (SettingsUtil.getSettings().isNotificationGrouped()){
            groupPokeNotification(context, pokemonRecycler);
        } else {
            singlePokeNotification(context, pokemonRecycler);
        }
    }

    private static void singlePokeNotification(Context context, ArrayList<Pokemons> pokemonRecycler) {
        NotificationCompat.Builder builder = setupNotification(context);

        for (Pokemons pokemon : pokemonRecycler){
            builder.setContentTitle(pokemon.getFormalName(context))
                    .setContentText(String.format(Locale.getDefault(), "%3dm %-9s expires in %s",
                            pokemon.getDistance(),
                            pokemon.getBearing(),
                            DrawableUtils.getExpireTime(pokemon.getExpires())))
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), pokemon.getResourceID(context)));
            notify(context, (int) pokemon.getEncounterid(), builder);

            builder = setupSilentNotification(context);

        }
    }

    public static void groupPokeNotification(Context context, ArrayList<Pokemons> pokemonRecycler) {

        NotificationCompat.Builder builder = setupNotification(context)
                .setContentTitle(pokemonRecycler.size() + " Nearby Pokemon")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(generateNotificationText(pokemonRecycler, context)))
                .setContentText(generateNotificationText(pokemonRecycler, context));

        notify(context, GROUPED_ID, builder);
    }

    private static String generateNotificationText(ArrayList<Pokemons> pokemonRecycler, Context context) {
        String newline = "";
        StringBuilder sb = new StringBuilder();
        for (Pokemons pokemon : pokemonRecycler) {
            sb.append(newline)
                    .append(String.format(Locale.getDefault(), "%-20s %3dm %-9s expires in %s",
                            pokemon.getFormalName(context),
                            pokemon.getDistance(),
                            pokemon.getBearing(),
                            DrawableUtils.getExpireTime(pokemon.getExpires())));
            newline = "\n";
        }
        return sb.toString();
    }

    private static NotificationCompat.Builder setupNotification(Context context) {
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(context, 0, new Intent(context, MapsActivity.class), 0);
        Settings settings = SettingsUtil.getSettings();
        long[] vibrate = {0, 400};
        return  new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntentOpenApp)
                .setSound(Uri.parse(settings.getNotificationRingtone()))
                .setVibrate(settings.isNotificationVibrate() ? vibrate : new long[]{0});
    }

    private static NotificationCompat.Builder setupSilentNotification(Context context) {
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(context, 0, new Intent(context, MapsActivity.class), 0);
        return  new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntentOpenApp);
    }

    private static void notify(Context context, int id, NotificationCompat.Builder builder){
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(id, builder.build());
    }
}
