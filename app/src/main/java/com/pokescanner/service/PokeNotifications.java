package com.pokescanner.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
    public static final String INDIVIDUAL_GROUP = "individual_group";
    public static final String GROUPED_GROUP = "grouped_group";
    public static final String ONGOING_GROUP = "ongoing_group";


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
                .setPriority(Notification.PRIORITY_MIN)
                .setGroup(ONGOING_GROUP);

        notify(context, ONGOING_ID, builder);
    }

    public static void pokeNotification(Context context, ArrayList<Pokemons> pokemonRecycler) {
        if (SettingsUtil.getSettings().isNotificationGrouped()) {
            groupPokeNotification(context, pokemonRecycler);
        } else {
            singlePokeNotification(context, pokemonRecycler);
        }
    }

    private static void singlePokeNotification(Context context, ArrayList<Pokemons> pokemonRecycler) {
        NotificationCompat.Builder builder = setupNotification(context);
        for (Pokemons pokemon : pokemonRecycler) {
            builder.setContentTitle(pokemon.getFormalName(context))
                    .setContentText(String.format(Locale.getDefault(), "%3dm %-9s expires in %s",
                            pokemon.getDistance(),
                            pokemon.getBearing(),
                            DrawableUtils.getExpireTime(pokemon.getExpires())))
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), pokemon.getResourceID(context)))
                    .setGroup(INDIVIDUAL_GROUP);
            notify(context, (int) pokemon.getEncounterid(), builder);
            builder = setupSilentNotification(context);
        }
    }

    private static void groupPokeNotification(Context context, ArrayList<Pokemons> pokemonRecycler) {
        NotificationCompat.Builder builder = setupNotification(context);
        for (Pokemons pokemon : pokemonRecycler) {
            builder.setContentTitle(pokemon.getFormalName(context))
                    .setContentText(String.format(Locale.getDefault(), "%3dm %-9s expires in %s",
                            pokemon.getDistance(),
                            pokemon.getBearing(),
                            DrawableUtils.getExpireTime(pokemon.getExpires())))
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), pokemon.getResourceID(context)))
                    .setGroup(GROUPED_GROUP);
            notify(context, (int) pokemon.getEncounterid(), builder);
            builder = setupSilentNotification(context);
        }
        if (pokemonRecycler.size() > 1) {
            builder.setContentTitle(pokemonRecycler.size() + " Nearby Pokemon")
                    .setStyle(generateNotificationText(pokemonRecycler, context))
                    .setGroup(GROUPED_GROUP)
                    .setGroupSummary(true);
            notify(context, GROUPED_ID, builder);
        }
    }

    //Really ugly way of customizing grouped notification
    private static NotificationCompat.BigTextStyle generateNotificationText(ArrayList<Pokemons> pokemonRecycler, Context context) {
        NotificationCompat.BigTextStyle inbox = new NotificationCompat.BigTextStyle();
        StringBuilder sb = new StringBuilder();
        String newline = "";
        for (Pokemons pokemon : pokemonRecycler) {
            sb.append(newline)
                    .append(String.format("%s %dm  %s  expires in %s",
                            pokemon.getFormalName(context),
                            pokemon.getDistance(),
                            pokemon.getBearing(),
                            DrawableUtils.getExpireTime(pokemon.getExpires())));
            newline = "\n";
        }
        inbox.bigText(sb.toString());

        return inbox;
    }

    private static NotificationCompat.Builder setupNotification(Context context) {
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(context, 0, new Intent(context, MapsActivity.class), 0);
        Settings settings = SettingsUtil.getSettings();
        long[] vibrate = {0, 400};
        Log.d("POKE", settings.getNotificationRingtone());
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntentOpenApp)
                .setSound(Uri.parse(settings.getNotificationRingtone()))
                .setVibrate(settings.isNotificationVibrate() ? vibrate : new long[]{0});
    }

    private static NotificationCompat.Builder setupSilentNotification(Context context) {
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(context, 0, new Intent(context, MapsActivity.class), 0);
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntentOpenApp);
    }

    private static void notify(Context context, int id, NotificationCompat.Builder builder) {
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(id, builder.build());
    }
}
