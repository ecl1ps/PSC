package com.pokescanner.loaders;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.pokescanner.helper.MyPartition;
import com.pokescanner.objects.User;
import com.pokescanner.service.PokeService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Brian on 7/31/2016.
 */
public class MultiAccountLoader {
    static private List<LatLng> scanMap;
    static private List<List<LatLng>> scanMaps;
    static private ArrayList<User> users;
    static private int SLEEP_TIME;
    static private ArrayList<ObjectLoaderPTC> tasks;
    static private boolean isBackground = false;


    static public void startThreads(Context context) {
        scanMaps = new ArrayList<>();
        tasks = new ArrayList<>();

        int userSize = users.size();
        double dividedValue = (float) scanMap.size() / userSize;
        int scanMapSplitSize = (int) Math.ceil(dividedValue);

        System.out.println("Divided Value:" + dividedValue + "(scanMap.size() = " + scanMap.size()
                + "; userSize = " + userSize + ")");

        scanMaps = MyPartition.partition(scanMap, scanMapSplitSize);

        System.out.println("Scan Map Size: " + scanMaps.size());

        for (int i = 0; i < scanMaps.size(); i++) {
            List<LatLng> tempMap = scanMaps.get(i);
            User tempUser = users.get(i);
            ObjectLoaderPTC objectLoaderPTC = new ObjectLoaderPTC(context, tempUser, tempMap, SLEEP_TIME, i);
            objectLoaderPTC.executeOnExecutor(THREAD_POOL_EXECUTOR);
            tasks.add(objectLoaderPTC);
        }

    }

    static public void setSleepTime(int SLEEP_TIME) {
        MultiAccountLoader.SLEEP_TIME = SLEEP_TIME;
    }

    static public void setUsers(ArrayList<User> users) {
        MultiAccountLoader.users = users;
    }

    static public void setScanMap(List<LatLng> scanMap) {
        MultiAccountLoader.scanMap = scanMap;
    }

    public static void setIsBackground(boolean isBackground) {
        MultiAccountLoader.isBackground = isBackground;
    }

    static public void checkIfComplete(Context context) {
        int counter = 1;
        for (ObjectLoaderPTC objectLoaderPTC : tasks) {
            if (objectLoaderPTC.getStatus() == AsyncTask.Status.FINISHED) {
                counter++;
            }
        }
        if (counter == scanMaps.size()) {
            if (isBackground) {
                PokeService.pokeNotification(context);
            }
        }
    }


    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    private static final Executor THREAD_POOL_EXECUTOR;

    static {
        //Disregard CPU cores and run as many threads as we want! ...well 30
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                30, 30, 30, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    public static void cancelAllThreads() {
        if (tasks != null) {
            for (ObjectLoaderPTC objectLoaderPTC : tasks) {
                objectLoaderPTC.cancel(true);
            }
        }
    }

    public static boolean areThreadsRunning() {
        if (tasks != null && tasks.size() > 0) {
            for (ObjectLoaderPTC objectLoaderPTC : tasks) {
                if (objectLoaderPTC.getStatus() != AsyncTask.Status.FINISHED) {
                    return true;
                }
            }
        }
        return false;
    }

}
