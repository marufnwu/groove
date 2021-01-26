package com.groove.megaapp.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoTrimmerWorker2 extends Worker {

    public static final String KEY_INPUT = "input";
    public static final String KEY_OUTPUT = "output";
    public static final String KEY_START = "start";
    public static final String KEY_END = "end";
    private static final String TAG = "VideoTrimmerWorker2";

    public VideoTrimmerWorker2(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        File input = new File(getInputData().getString(KEY_INPUT));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        long start = getInputData().getLong(KEY_START, 0);
        long end = getInputData().getLong(KEY_END, 0);
        boolean success = false;
        try {
            success = doActualWork(input, output, start, end, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Encountered error when trimming " + input, e);
        }

        if (!success && !output.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + output);
        }

        return success ? Result.success() : Result.failure();
    }

    private static double correct(Track track, double cut, boolean next) {
        double[] times = new double[track.getSyncSamples().length];
        long sample = 0;
        double time = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];
            if (Arrays.binarySearch(track.getSyncSamples(), sample + 1) >= 0) {
                times[Arrays.binarySearch(track.getSyncSamples(), sample + 1)] = time;
            }

            time += (double) delta / (double) track.getTrackMetaData().getTimescale();
            sample++;
        }

        double previous = 0;
        for (double i : times) {
            if (i > cut) {
                if (next) {
                    return i;
                } else {
                    return previous;
                }
            }
            previous = i;
        }

        return times[times.length - 1];
    }

    public static boolean doActualWork(File input, File output, long start, long end, TimeUnit unit) throws IOException {
        Movie movie = MovieCreator.build(new FileDataSourceImpl(input));
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<>());
        double from = unit.toSeconds(start);
        double until = unit.toSeconds(end);
        boolean corrected = false;
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (corrected) {
                    throw new RuntimeException("Time correction has already been performed.");
                }
                from = correct(track, from, false);
                until = correct(track, until, true);
                corrected = true;
            }
        }

        for (Track track : tracks) {
            long sample = 0;
            double time = 0;
            long first = -1, last = -1;
            for (int i = 0; i < track.getSampleDurations().length; i++) {
                if (time <= from) {
                    first = sample;
                }

                if (time <= until) {
                    last = sample;
                } else {
                    break;
                }

                time += (double) track.getSampleDurations()[i] / (double) track.getTrackMetaData().getTimescale();
                sample++;
            }
            movie.addTrack(new CroppedTrack(track, first, last));
        }

        Container mp4 = new DefaultMp4Builder().build(movie);
        WritableByteChannel out = new FileOutputStream(output).getChannel();
        mp4.writeContainer(out);
        out.close();
        return true;
    }
}
