package com.groove.megaapp.workers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.UploadActivity;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.dbs.ClientDatabase;
import com.groove.megaapp.data.entities.Draft;
import com.groove.megaapp.data.models.Clip;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.events.ResetDraftsEvent;
import com.groove.megaapp.utils.VideoUtil;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public class UploadClipWorker extends Worker {

    public static final String KEY_COMMENTS = "comments";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_PREVIEW = "preview";
    public static final String KEY_PRIVATE = "private";
    public static final String KEY_SCREENSHOT = "screenshot";
    public static final String KEY_SONG = "song";
    public static final String KEY_VIDEO = "video";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    private static final String TAG = "UploadClipWorker";

    public UploadClipWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private ForegroundInfo createForegroundInfo(Context context) {
        String cancel = context.getString(R.string.cancel_button);
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());
        Notification notification =
                new NotificationCompat.Builder(
                        context, context.getString(R.string.notification_channel_id))
                        .setContentTitle(context.getString(R.string.notification_upload_title))
                        .setTicker(context.getString(R.string.notification_upload_title))
                        .setContentText(context.getString(R.string.notification_upload_description))
                        .setSmallIcon(R.drawable.ic_baseline_publish_24)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .addAction(R.drawable.ic_baseline_close_24, cancel, intent)
                        .build();
        return new ForegroundInfo(SharedConstants.NOTIFICATION_UPLOAD, notification);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        setForegroundAsync(createForegroundInfo(getApplicationContext()));
        File video = new File(getInputData().getString(KEY_VIDEO));
        File screenshot = new File(getInputData().getString(KEY_SCREENSHOT));
        File preview = new File(getInputData().getString(KEY_PREVIEW));
        Integer songId = getInputData().getInt(KEY_SONG, -1);
        if (songId <= 0) {
            songId = null;
        }

        String description = getInputData().getString(KEY_DESCRIPTION);
        String language = getInputData().getString(KEY_LANGUAGE);
        long duration = VideoUtil.getDuration(getApplicationContext(), Uri.fromFile(video));
        duration = TimeUnit.MILLISECONDS.toSeconds(duration);
        boolean isPrivate = getInputData().getBoolean(KEY_PRIVATE, false);
        boolean hasComments = getInputData().getBoolean(KEY_COMMENTS, false);
        String location = getInputData().getString(KEY_LOCATION);
        Double latitude = getInputData().getDouble(KEY_LATITUDE, 60600);
        if (latitude == 60600) {
            latitude = null;
        }

        Double longitude = getInputData().getDouble(KEY_LONGITUDE, 60600);
        if (longitude == 60600) {
            longitude = null;
        }

        boolean success = false;
        try {
            success = doActualWork(
                    video, screenshot, preview, songId, description, language, (int)duration,
                    isPrivate, hasComments, location, latitude, longitude);
        } catch (Exception e) {
            Log.e(TAG, "Failed to upload clip to server.", e);
        }

        if (success && !video.delete()) {
            Log.w(TAG, "Could not delete uploaded video file.");
        }

        if (success && !screenshot.delete()) {
            Log.w(TAG, "Could not delete uploaded screenshot file.");
        }

        if (success && !preview.delete()) {
            Log.w(TAG, "Could not delete uploaded preview file.");
        }

        if (!success) {
            Draft draft = createDraft(
                    video, screenshot, preview, songId, description, language, isPrivate,
                    hasComments, location, latitude, longitude);
            Log.w(TAG, "Failed clip saved as draft with ID " + draft.id + ".");
            EventBus.getDefault().post(new ResetDraftsEvent());
            createDraftNotification(draft);
        }

        return success ? Result.success() : Result.failure();
    }

    private Draft createDraft(
            File video,
            File screenshot,
            File preview,
            Integer songId,
            String description,
            String language,
            boolean isPrivate,
            boolean hasComments,
            String location,
            Double latitude,
            Double longitude
    ) {
        ClientDatabase db = MainApplication.getContainer().get(ClientDatabase.class);
        Draft draft = new Draft();
        draft.video = video.getAbsolutePath();
        draft.screenshot = screenshot.getAbsolutePath();
        draft.preview = preview.getAbsolutePath();
        draft.songId = songId;
        draft.description = description;
        draft.language = language;
        draft.isPrivate = isPrivate;
        draft.hasComments = hasComments;
        draft.location = location;
        draft.latitude = latitude;
        draft.longitude = longitude;
        db.drafts().insert(draft);
        return draft;
    }

    private void createDraftNotification(Draft draft) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, UploadActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(UploadActivity.EXTRA_DRAFT, draft);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        Notification notification =
                new NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                        .setAutoCancel(true)
                        .setContentIntent(pi)
                        .setContentText(context.getString(R.string.notification_upload_failed_description))
                        .setContentTitle(context.getString(R.string.notification_upload_failed_title))
                        .setSmallIcon(R.drawable.ic_baseline_redo_24)
                        .setTicker(context.getString(R.string.notification_upload_failed_title))
                        .build();
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(SharedConstants.NOTIFICATION_UPLOAD_FAILED, notification);
    }

    private boolean doActualWork(
            File video,
            File screenshot,
            File preview,
            @Nullable Integer songId,
            String description,
            String language,
            int duration,
            boolean isPrivate,
            boolean hasComments,
            String location,
            Double latitude,
            Double longitude
    ) throws IOException {
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<Wrappers.Single<Clip>> call = rest.clipsCreate(
                MultipartBody.Part.createFormData("video", "video.mp4", RequestBody.create(video, null)),
                MultipartBody.Part.createFormData("screenshot", "screenshot.png", RequestBody.create(screenshot, null)),
                MultipartBody.Part.createFormData("preview", "preview.gif", RequestBody.create(preview, null)),
                songId != null ? RequestBody.create(songId + "", null) : null,
                description != null ? RequestBody.create(description, null) : null,
                RequestBody.create(language, null),
                RequestBody.create(isPrivate ? "1" : "0", null),
                RequestBody.create(hasComments ? "1" : "0", null),
                RequestBody.create(duration + "", null),
                location != null ? RequestBody.create(location, null) : null,
                latitude != null ? RequestBody.create(latitude + "", null) : null,
                longitude != null ? RequestBody.create(longitude + "", null) : null
        );
        Response<Wrappers.Single<Clip>> response = null;
        try {
            response = call.execute();
        } catch (Exception e) {
            Log.e(TAG, "Failed when uploading clip to server.", e);
        }

        if (response != null) {
            if (response.code() == 422) {
                Log.v(TAG, "Server returned validation errors:\n" + response.errorBody().string());
            }

            return response.isSuccessful();
        }

        return false;
    }
}
