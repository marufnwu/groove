package com.groove.megaapp.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.File;

import com.groove.megaapp.R;

public class PreviewActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO = "video";

    private PreviewActivityViewModel mModel;
    private SimpleExoPlayer mPlayer;
    private String mVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.volume_label);
        findViewById(R.id.header_more).setVisibility(View.GONE);
        mModel = new ViewModelProvider(this).get(PreviewActivityViewModel.class);
        mPlayer = new SimpleExoPlayer.Builder(this).build();
        mVideo = getIntent().getStringExtra(EXTRA_VIDEO);
        PlayerView player = findViewById(R.id.player);
        player.setPlayer(mPlayer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayer();
        mPlayer.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlayer.getPlaybackState() == Player.STATE_IDLE) {
            startPlayer();
        }
    }

    private void startPlayer() {
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(this, getString(R.string.app_name));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(Uri.fromFile(new File(mVideo)));
        mPlayer.setPlayWhenReady(true);
        mPlayer.seekTo(mModel.window, mModel.position);
        mPlayer.prepare(new LoopingMediaSource(source), false, false);
    }

    private void stopPlayer() {
        mModel.position = mPlayer.getCurrentPosition();
        mModel.window = mPlayer.getCurrentWindowIndex();
        mPlayer.setPlayWhenReady(false);
        mPlayer.stop();
    }

    public static class PreviewActivityViewModel extends ViewModel {

        public long position;
        public int window;
    }
}
