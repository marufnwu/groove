package com.groove.megaapp.utils;

import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final public class CacheUtil {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);

    private static final String TAG = "CacheUtil";

    public static void prefetch(HttpProxyCacheServer proxy, String url) {
        EXECUTOR.submit(() -> prefetchOrTimeout(proxy, url));
    }

    private static void prefetchContents(HttpProxyCacheServer proxy, String url) {
        try (InputStream is = new URL(proxy.getProxyUrl(url)).openStream()) {
            byte[] buffer = new byte[1024];
            //noinspection StatementWithEmptyBody
            while (is.read(buffer) != -1) {
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed while pre-caching " + url + ".", e);
        }
    }

    private static void prefetchOrTimeout(HttpProxyCacheServer proxy, String url) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable task = () -> prefetchContents(proxy, url);
        Future<?> future = executor.submit(task);
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException ignore) {
        } finally {
            future.cancel(true);
        }
    }
}
