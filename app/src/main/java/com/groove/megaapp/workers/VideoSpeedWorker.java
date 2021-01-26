package com.groove.megaapp.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.daasuu.mp4compose.VideoFormatMimeType;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class VideoSpeedWorker extends ListenableWorker {

    public static final String KEY_INPUT = "input";
    public static final String KEY_OUTPUT = "output";
    public static final String KEY_SPEED = "speed";
    private static final String TAG = "VideoSpeedWorker";

    public VideoSpeedWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public ListenableFuture<Result> startWork() {
        File input = new File(getInputData().getString(KEY_INPUT));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        float speed = getInputData().getFloat(KEY_SPEED, 1f);
        return CallbackToFutureAdapter.getFuture(completer -> {
            doActualWork(input, output, speed, completer);
            return null;
        });
    }

    private void doActualWork(File input, File output, float speed, CallbackToFutureAdapter.Completer<Result> completer) {
        Mp4Composer composer = new Mp4Composer(input.getAbsolutePath(), output.getAbsolutePath());
        composer.timeScale(speed);
        composer.listener(new Mp4Composer.Listener() {

            @Override
            public void onProgress(double progress) { }

            @Override
            public void onCompleted() {
                Log.d(TAG, "MP4 composition has finished.");
                completer.set(Result.success());
                if (!input.delete()) {
                    Log.w(TAG, "Could not delete input file: " + input);
                }
            }

            @Override
            public void onCanceled() {
                Log.d(TAG, "MP4 composition was cancelled.");
                completer.setCancelled();
                if (!input.delete()) {
                    Log.w(TAG, "Could not delete input file: " + input);
                }

                if (!output.delete()) {
                    Log.w(TAG, "Could not delete failed output file: " + output);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.d(TAG, "MP4 composition failed with error.", e);
                completer.setException(e);
                if (!input.delete()) {
                    Log.w(TAG, "Could not delete input file: " + input);
                }

                if (!output.delete()) {
                    Log.w(TAG, "Could not delete failed output file: " + output);
                }
            }
        });
        composer.videoFormatMimeType(VideoFormatMimeType.AVC);
        composer.start();
    }
}
