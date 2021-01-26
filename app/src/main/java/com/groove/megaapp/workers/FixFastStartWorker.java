package com.groove.megaapp.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileOutputStream;

public class FixFastStartWorker extends Worker {

    public static final String KEY_INPUT = "input";
    public static final String KEY_OUTPUT = "output";
    private static final String TAG = "FixFastStartWorker";

    public FixFastStartWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        File input = new File(getInputData().getString(KEY_INPUT));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        boolean success = doActualWork(input, output);
        if (!success && !input.delete()) {
            Log.w(TAG, "Could not delete input file: " + input);
        }

        if (!success && !output.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + input);
        }

        return success ? Result.success() : Result.failure();
    }

    private boolean doActualWork(File input, File output) {
        FileOutputStream os = null;
        try {
            Movie source = MovieCreator.build(input.getAbsolutePath());
            Container mp4 = new DefaultMp4Builder().build(source);
            os = new FileOutputStream(output);
            mp4.writeContainer(os.getChannel());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to output at " + output, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {
                }
            }
        }

        return false;
    }
}
