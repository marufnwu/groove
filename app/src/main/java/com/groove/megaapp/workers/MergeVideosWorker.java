package com.groove.megaapp.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MergeVideosWorker extends Worker {

    public static final String KEY_INPUTS = "inputs";
    public static final String KEY_OUTPUT = "output";
    private static final String TAG = "MergeVideosWorker";

    public MergeVideosWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        String[] paths = getInputData().getStringArray(KEY_INPUTS);
        File output = new File(getInputData().getString(KEY_OUTPUT));
        File[] files = new File[paths.length];
        for (int i = 0; i < paths.length; i++) {
            files[i] = new File(paths[i]);
        }

        boolean success = doActualWork(files, output);
        if (!success && !output.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + output);
        }

        return success ? Result.success() : Result.failure();
    }

    private boolean doActualWork(File[] inputs, File output) {
        FileOutputStream os = null;
        try {
            List<Movie> movies = new ArrayList<>();
            for (File input : inputs) {
                Log.v(TAG, "Merging " + input);
                movies.add(MovieCreator.build(input.getAbsolutePath()));
            }

            List<Track> audios = new ArrayList<>();
            List<Track> videos = new ArrayList<>();
            for (Movie movie : movies) {
                for (Track track : movie.getTracks()) {
                    if (TextUtils.equals(track.getHandler(), "soun")) {
                        audios.add(track);
                    }

                    if (TextUtils.equals(track.getHandler(), "vide")) {
                        videos.add(track);
                    }
                }
            }

            Movie merged = new Movie();
            if (!audios.isEmpty()) {
                merged.addTrack(new AppendTrack(audios.toArray(new Track[0])));
            }

            if (!videos.isEmpty()) {
                merged.addTrack(new AppendTrack(videos.toArray(new Track[0])));
            }

            Container mp4 = new DefaultMp4Builder().build(merged);
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
