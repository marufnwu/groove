package com.groove.megaapp.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import com.groove.megaapp.BuildConfig;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.SplashActivity;
import com.groove.megaapp.events.MessageEvent;
import com.groove.megaapp.workers.DeviceTokenWorker;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FirebaseMessageService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessageService";
    private static final String TOPIC_DEFAULT = "default";

    @Override
    public void onMessageReceived(@NotNull RemoteMessage message) {
        Log.d(TAG, "Received message from " + message.getFrom());
        Map<String, String> data = message.getData();
        if (data.containsKey("thread")) {
            int thread = Integer.parseInt(data.get("thread"));
            Log.v(TAG, "Thread #" + thread + " found in notification.");
            if (EventBus.getDefault().hasSubscriberForEvent(MessageEvent.class)) {
                EventBus.getDefault().post(new MessageEvent(thread));
                return;
            }
        }

        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null) {
            String id = getString(R.string.notification_channel_id);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, id)
                    .setAutoCancel(true)
                    .setContentText(notification.getBody())
                    .setContentTitle(notification.getTitle())
                    .setSmallIcon(R.mipmap.ic_launcher);
            if (notification.getImageUrl() != null) {
                builder.setLargeIcon(downloadImage(notification.getImageUrl()));
            }

            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Uri link = notification.getLink();
            if (link != null) {
                intent.setData(link);
            } else if (data.containsKey("clip")) {
                Uri base = Uri.parse(getString(R.string.server_url));
                Uri uri = base.buildUpon()
                        .path("links/clips")
                        .appendQueryParameter("first", data.get("clip"))
                        .appendQueryParameter("package", BuildConfig.APPLICATION_ID)
                        .build();
                intent.setData(uri);
            } else if (data.containsKey("user")) {
                Uri base = Uri.parse(getString(R.string.server_url));
                Uri uri = base.buildUpon()
                        .path("links/users")
                        .appendQueryParameter("user", data.get("user"))
                        .appendQueryParameter("package", BuildConfig.APPLICATION_ID)
                        .build();
                intent.setData(uri);
            }

            builder.setContentIntent(
                    PendingIntent.getActivity(
                            this, 0, intent, PendingIntent.FLAG_ONE_SHOT));
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel =
                        new NotificationChannel(
                                id, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(channel);
            }

            nm.notify(0, builder.build());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token is " + token);
        Prefs.putString(SharedConstants.PREF_FCM_TOKEN, token);
        WorkRequest request = OneTimeWorkRequest.from(DeviceTokenWorker.class);
        WorkManager.getInstance(this).enqueue(request);
        FirebaseMessaging.getInstance()
                .subscribeToTopic(TOPIC_DEFAULT)
                .addOnCompleteListener(task ->
                        Log.v(TAG, "Subscription to " + TOPIC_DEFAULT + " is " + task.isSuccessful() + "."));
    }

    public Bitmap downloadImage(Uri url) {
        try {
            Request request = new Request.Builder().url(url.toString()).get().build();
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return BitmapFactory.decodeStream(response.body().byteStream());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download notification image.", e);
        }

        return null;
    }
}
