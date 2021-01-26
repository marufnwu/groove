package com.groove.megaapp.workers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;

public class SaveToGalleryWorker extends Worker {

    public static final String KEY_FILE = "file";
    public static final String KEY_NAME = "name";
    public static final String KEY_NOTIFICATION = "notification";

    private static final String TAG = "SaveToGalleryWorker";

    public SaveToGalleryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        File file = new File(getInputData().getString(KEY_FILE));
        String name = getInputData().getString(KEY_NAME);
        boolean success = doActualWork(file, name);
        if (success && !file.delete()) {
            Log.w(TAG, "Could not delete downloaded file: " + file);
        }

        return success ? Result.success() : Result.failure();
    }

    public boolean doActualWork(File file, String name) {
        File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        Context context = getApplicationContext();
        File app = new File(movies, context.getString(R.string.app_name));
        if (!app.isDirectory() && !app.mkdirs()) {
            Log.v(TAG, "Could not create app directory at " + app);
        }

        File mp4 = new File(app, name);
        InputStream is = null;
        OutputStream os = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            is = new FileInputStream(file);
            os = new FileOutputStream(mp4);
            IOUtils.copy(is, os);
        } catch (Exception e) {
            Log.e(TAG, "Could not save video to " + mp4, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ignore) {
            }

            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception ignore) {
            }
        }

        ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, mp4.getAbsolutePath());
        Uri uri = context.getContentResolver()
                .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        if (getInputData().getBoolean(KEY_NOTIFICATION, false)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            PendingIntent pi = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            Notification notification =
                    new NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                            .setAutoCancel(true)
                            .setContentIntent(pi)
                            .setContentText(context.getString(R.string.notification_saved_description))
                            .setContentTitle(context.getString(R.string.notification_saved_title))
                            .setSmallIcon(R.drawable.ic_baseline_local_movies_24)
                            .setTicker(context.getString(R.string.notification_saved_title))
                            .build();
            NotificationManagerCompat nm = NotificationManagerCompat.from(context);
            nm.notify(SharedConstants.NOTIFICATION_SAVE_TO_GALLERY, notification);
        }

        return true;
    }
}
