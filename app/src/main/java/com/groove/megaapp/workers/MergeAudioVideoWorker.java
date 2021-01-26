package com.groove.megaapp.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class MergeAudioVideoWorker extends Worker {

    public static final String KEY_AUDIO = "audio";
    public static final String KEY_OUTPUT = "output";
    public static final String KEY_VIDEO = "video";
    private static final String TAG = "MergeAudioVideoWorker";

    public MergeAudioVideoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        File audio = new File(getInputData().getString(KEY_AUDIO));
        File video = new File(getInputData().getString(KEY_VIDEO));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        boolean success = doActualWork(audio, video, output);
        if (!success && !output.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + output);
        }

        return success ? Result.success() : Result.failure();
    }

    private boolean doActualWork(File audio, File video, File output) {
        try {
            Movie temp = MovieCreator.build(video.getAbsolutePath());
            Movie movie = new Movie();
            for (Track track : temp.getTracks()) {
                if (TextUtils.equals(track.getHandler(), "vide")) {
                    movie.addTrack(track);
                    break;
                }
            }

            Track sound = null;
            if (audio.getAbsolutePath().endsWith(".mp4")) {
                for (Track track : temp.getTracks()) {
                    if (TextUtils.equals(track.getHandler(), "soun")) {
                        sound = crop(video.getAbsolutePath(), track);
                        break;
                    }
                }

            } else {
                sound = crop(
                        video.getAbsolutePath(),
                        new AACTrackImpl(new FileDataSourceImpl(audio)));
            }

            if (sound != null) {
                movie.addTrack(sound);
            }

            Container mp4 = new DefaultMp4Builder().build(movie);
            WritableByteChannel wbc = new FileOutputStream(output).getChannel();
            mp4.writeContainer(wbc);
            wbc.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save merged output to " + output, e);
        }

        return false;
    }

    private static CroppedTrack crop(String clip, Track audio) throws IOException {
        IsoFile iso = new IsoFile(clip);
        MovieHeaderBox header = iso.getMovieBox().getMovieHeaderBox();
        double duration = (double) header.getDuration() / header.getTimescale();
        double time_c = 0; // current time
        double time_p = -1; // previous time
        long sample_c = 0; // current sample
        long sample_s = -1; // start sample
        long sample_e = -1; // end sample
        for (int i = 0; i < audio.getSampleDurations().length; i++) {
            long delta = audio.getSampleDurations()[i];
            if (time_c > time_p && time_c <= 0) {
                sample_s = sample_c;
            }

            if (time_c > time_p && time_c <= duration) {
                sample_e = sample_c;
            }

            time_p = time_c;
            time_c += (double) delta / (double) audio.getTrackMetaData().getTimescale();
            sample_c++;
        }

        return new CroppedTrack(audio, sample_s, sample_e);
    }
}
