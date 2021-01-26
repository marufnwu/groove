package com.groove.megaapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.daasuu.gpuv.egl.filter.GlBrightnessFilter;
import com.daasuu.gpuv.egl.filter.GlExposureFilter;
import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.egl.filter.GlGammaFilter;
import com.daasuu.gpuv.egl.filter.GlGrayScaleFilter;
import com.daasuu.gpuv.egl.filter.GlHazeFilter;
import com.daasuu.gpuv.egl.filter.GlInvertFilter;
import com.daasuu.gpuv.egl.filter.GlMonochromeFilter;
import com.daasuu.gpuv.egl.filter.GlPixelationFilter;
import com.daasuu.gpuv.egl.filter.GlPosterizeFilter;
import com.daasuu.gpuv.egl.filter.GlSepiaFilter;
import com.daasuu.gpuv.egl.filter.GlSharpenFilter;
import com.daasuu.gpuv.egl.filter.GlSolarizeFilter;
import com.daasuu.gpuv.egl.filter.GlVignetteFilter;
import com.daasuu.gpuv.player.GPUPlayerView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.groove.megaapp.R;
import com.groove.megaapp.common.VideoFilter;
import com.groove.megaapp.common.FilterAdapter;
import com.groove.megaapp.utils.BitmapUtil;
import com.groove.megaapp.utils.TempUtil;
import com.groove.megaapp.utils.VideoUtil;
import com.groove.megaapp.workers.VideoFilterWorker;

public class FilterActivity extends AppCompatActivity {

    public static final String EXTRA_SONG = "song";
    public static final String EXTRA_VIDEO = "video";
    private static final String TAG = "FilterActivity";

    private FilterActivityViewModel mModel;
    private SimpleExoPlayer mPlayer;
    private GPUPlayerView mPlayerView;
    private int mSong;
    private String mVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        mModel = new ViewModelProvider(this).get(FilterActivityViewModel.class);
        mSong = getIntent().getIntExtra(EXTRA_SONG, 0);
        mVideo = getIntent().getStringExtra(EXTRA_VIDEO);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.filters_label);
        ImageButton done = findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(view -> submitForFilter());
        Bitmap frame = VideoUtil.getFrameAtTime(mVideo, TimeUnit.SECONDS.toMicros(3));
        Bitmap square = BitmapUtil.getSquareThumbnail(frame, 250);
        //noinspection ConstantConditions
        frame.recycle();
        Bitmap rounded = BitmapUtil.addRoundCorners(square, 25);
        square.recycle();
        FilterAdapter adapter = new FilterAdapter(this, rounded);
        adapter.setListener(this::applyFilter);
        RecyclerView filters = findViewById(R.id.filters);
        filters.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        File video = new File(mVideo);
        if (!video.delete()) {
            Log.w(TAG, "Could not delete input video: " + video);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayerView.onPause();
        mPlayer.setPlayWhenReady(false);
        mPlayer.stop(true);
        mPlayer.release();
        mPlayer = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer = new SimpleExoPlayer.Builder(this).build();
        mPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(this, getString(R.string.app_name));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(Uri.fromFile(new File(mVideo)));
        mPlayer.setPlayWhenReady(true);
        mPlayer.prepare(source);
        mPlayerView = findViewById(R.id.player);
        mPlayerView.setSimpleExoPlayer(mPlayer);
        mPlayerView.onResume();
    }

    public void applyFilter(VideoFilter filter) {
        Log.v(TAG, "User wants to apply " + filter.name() + " filter.");
        GPUPlayerView player = findViewById(R.id.player);
        switch (mModel.filter = filter) {
            case BRIGHTNESS: {
                GlBrightnessFilter glf = new GlBrightnessFilter();
                glf.setBrightness(0.2f);
                player.setGlFilter(glf);
                break;
            }
            case EXPOSURE:
                player.setGlFilter(new GlExposureFilter());
                break;
            case GAMMA: {
                GlGammaFilter glf = new GlGammaFilter();
                glf.setGamma(2f);
                player.setGlFilter(glf);
                break;
            }
            case GRAYSCALE:
                player.setGlFilter(new GlGrayScaleFilter());
                break;
            case HAZE: {
                GlHazeFilter glf = new GlHazeFilter();
                glf.setSlope(-0.5f);
                player.setGlFilter(glf);
                break;
            }
            case INVERT:
                player.setGlFilter(new GlInvertFilter());
                break;
            case MONOCHROME:
                player.setGlFilter(new GlMonochromeFilter());
                break;
            case PIXELATED: {
                GlPixelationFilter glf = new GlPixelationFilter();
                glf.setPixel(5);
                player.setGlFilter(glf);
                break;
            }
            case POSTERIZE:
                player.setGlFilter(new GlPosterizeFilter());
                break;
            case SEPIA:
                player.setGlFilter(new GlSepiaFilter());
                break;
            case SHARP: {
                GlSharpenFilter glf = new GlSharpenFilter();
                glf.setSharpness(1f);
                player.setGlFilter(glf);
                break;
            }
            case SOLARIZE:
                player.setGlFilter(new GlSolarizeFilter());
                break;
            case VIGNETTE:
                player.setGlFilter(new GlVignetteFilter());
                break;
            default:
                player.setGlFilter(new GlFilter());
                break;
        }
    }

    private void closeFinally(File clip) {
        Log.v(TAG, "Filter was successfully applied to " + clip);
        Intent intent = new Intent(this, UploadActivity.class);
        intent.putExtra(UploadActivity.EXTRA_SONG, mSong);
        intent.putExtra(UploadActivity.EXTRA_VIDEO, clip.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void submitForFilter() {
        mPlayer.setPlayWhenReady(false);
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        File filtered = TempUtil.createNewFile(this, ".mp4");
        Data data = new Data.Builder()
                .putString(VideoFilterWorker.KEY_INPUT, mVideo)
                .putString(VideoFilterWorker.KEY_OUTPUT, filtered.getAbsolutePath())
                .putString(VideoFilterWorker.KEY_FILTER, mModel.filter.name())
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(VideoFilterWorker.class)
                .setInputData(data)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED;
                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        progress.dismiss();
                        closeFinally(filtered);
                    } else if (ended) {
                        progress.dismiss();
                    }
                });
    }

    public static class FilterActivityViewModel extends ViewModel {

        public VideoFilter filter = VideoFilter.NONE;
    }
}
