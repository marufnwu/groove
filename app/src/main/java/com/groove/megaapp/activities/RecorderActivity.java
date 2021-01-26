package com.groove.megaapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.segmentedprogressbar.SegmentedProgressBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.munon.turboimageview.TurboImageView;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.filter.Filters;
import com.otaliastudios.cameraview.filters.BrightnessFilter;
import com.otaliastudios.cameraview.filters.GammaFilter;
import com.otaliastudios.cameraview.filters.SharpnessFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.hoang8f.android.segmented.SegmentedGroup;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.common.FilterAdapter;
import com.groove.megaapp.common.VideoFilter;
import com.groove.megaapp.data.models.Song;
import com.groove.megaapp.data.models.Sticker;
import com.groove.megaapp.filters.ExposureFilter;
import com.groove.megaapp.filters.HazeFilter;
import com.groove.megaapp.filters.MonochromeFilter;
import com.groove.megaapp.filters.PixelatedFilter;
import com.groove.megaapp.filters.SolarizeFilter;
import com.groove.megaapp.utils.AnimationUtil;
import com.groove.megaapp.utils.BitmapUtil;
import com.groove.megaapp.utils.IntentUtil;
import com.groove.megaapp.utils.TempUtil;
import com.groove.megaapp.utils.TextFormatUtil;
import com.groove.megaapp.utils.VideoUtil;
import com.groove.megaapp.workers.FileDownloadWorker;
import com.groove.megaapp.workers.MergeVideosWorker2;
import com.groove.megaapp.workers.VideoSpeedWorker2;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class RecorderActivity extends AppCompatActivity {

    public static final String EXTRA_AUDIO = "audio";
    public static final String EXTRA_SONG = "song";
    private static final String TAG = "RecorderActivity";

    private CameraView mCamera;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer mMediaPlayer;
    private RecorderActivityViewModel mModel;
    private KProgressHUD mProgress;
    private YoYo.YoYoString mPulse;
    private ImageButton mRecordButton;
    private final Runnable mStopper = this::stopRecording;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.v(TAG, "Received request: " + requestCode + ", result: " + resultCode + ".");
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            submitUpload(data.getData());
        } else if (requestCode == SharedConstants.REQUEST_CODE_PICK_SONG && resultCode == RESULT_OK && data != null) {
            Song song = data.getParcelableExtra(EXTRA_SONG);
            Uri audio = data.getParcelableExtra(EXTRA_AUDIO);
            setupSong(song, audio);
        } else if (requestCode == SharedConstants.REQUEST_CODE_PICK_STICKER && resultCode == RESULT_OK && data != null) {
            Sticker sticker = data.getParcelableExtra(StickerPickerActivity.EXTRA_STICKER);
            downloadSticker(sticker);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        mModel = new ViewModelProvider(this).get(RecorderActivityViewModel.class);
        Song song = getIntent().getParcelableExtra(EXTRA_SONG);
        Uri audio = getIntent().getParcelableExtra(EXTRA_AUDIO);
        if (audio != null) {
            setupSong(song, audio);
        }

        mCamera = findViewById(R.id.camera);
        mCamera.setLifecycleOwner(this);
        mCamera.setMode(Mode.VIDEO);
        findViewById(R.id.close).setOnClickListener(view -> confirmClose());
        findViewById(R.id.done).setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                        .show();
            } else if (mModel.segments.isEmpty()) {
                Toast.makeText(this, R.string.recorder_error_no_clips, Toast.LENGTH_SHORT)
                        .show();
            } else {
                commitRecordings(mModel.segments, mModel.audio);
            }
        });
        findViewById(R.id.flip).setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mCamera.toggleFacing();
            }
        });
        findViewById(R.id.flash).setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mCamera.setFlash(mCamera.getFlash() == Flash.OFF ? Flash.TORCH : Flash.OFF);
            }
        });
        SegmentedGroup speeds = findViewById(R.id.speeds);
        View speed = findViewById(R.id.speed);
        speed.setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                        .show();
            } else {
                speeds.setVisibility(
                        speeds.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        speed.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? View.VISIBLE : View.GONE);
        RadioButton speed05x = findViewById(R.id.speed05x);
        RadioButton speed075x = findViewById(R.id.speed075x);
        RadioButton speed1x = findViewById(R.id.speed1x);
        RadioButton speed15x = findViewById(R.id.speed15x);
        RadioButton speed2x = findViewById(R.id.speed2x);
        speed05x.setChecked(mModel.speed == .5f);
        speed075x.setChecked(mModel.speed == .75f);
        speed1x.setChecked(mModel.speed == 1);
        speed15x.setChecked(mModel.speed == 1.5f);
        speed2x.setChecked(mModel.speed == 2);
        speeds.setOnCheckedChangeListener((group, checked) -> {
            float factor = 1;
            if (checked != R.id.speed05x) {
                speed05x.setChecked(false);
            } else {
                factor = .5f;
            }

            if (checked != R.id.speed075x) {
                speed075x.setChecked(false);
            } else {
                factor = .75f;
            }

            if (checked != R.id.speed1x) {
                speed1x.setChecked(false);
            }

            if (checked != R.id.speed15x) {
                speed15x.setChecked(false);
            } else {
                factor = 1.5f;
            }

            if (checked != R.id.speed2x) {
                speed2x.setChecked(false);
            } else {
                factor = 2;
            }

            mModel.speed = factor;
        });
        RecyclerView filters = findViewById(R.id.filters);
        findViewById(R.id.filter).setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                        .show();
            } else if (filters.getVisibility() == View.VISIBLE) {
                filters.setAdapter(null);
                filters.setVisibility(View.GONE);
            } else {
                mProgress = KProgressHUD.create(this)
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setLabel(getString(R.string.progress_title))
                        .setCancellable(false)
                        .show();
                mCamera.takePictureSnapshot();
            }
        });
        TurboImageView stickers = findViewById(R.id.stickers);
        mCamera.setOnTouchListener((v, event) -> stickers.dispatchTouchEvent(event));
        View remove = findViewById(R.id.remove);
        remove.setOnClickListener(v -> {
            stickers.removeSelectedObject();
            if (stickers.getObjectCount() <= 0) {
                remove.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.sticker).setOnClickListener(v -> {
            Intent intent = new Intent(this, StickerPickerActivity.class);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_STICKER);
        });
        View sticker = findViewById(R.id.sticker_parent);
        sticker.setVisibility(getResources().getBoolean(R.bool.stickers_enabled) ? View.VISIBLE : View.GONE);
        View sheet = findViewById(R.id.timer_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        ImageButton close = sheet.findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        TextView title = sheet.findViewById(R.id.header_title);
        title.setText(R.string.timer_label);
        ImageButton start = sheet.findViewById(R.id.header_more);
        start.setImageResource(R.drawable.ic_baseline_check_24);
        start.setOnClickListener(view -> {
            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            startTimer();
        });
        findViewById(R.id.timer).setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                        .show();
            } else {
                bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        TextView maximum = findViewById(R.id.maximum);
        View sound = findViewById(R.id.sound);
        sound.setOnClickListener(view -> {
            if (mModel.segments.isEmpty()) {
                Intent intent = new Intent(this, SongPickerActivity.class);
                startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_SONG);
            } else if (mModel.audio == null) {
                Toast.makeText(this, R.string.message_song_select, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.message_song_change, Toast.LENGTH_SHORT).show();
            }
        });
        Slider selection = findViewById(R.id.selection);
        selection.setLabelFormatter(value -> TextFormatUtil.toMMSS((long)value));
        View upload = findViewById(R.id.upload);
        upload.setOnClickListener(view -> {
            String[] permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
            if (EasyPermissions.hasPermissions(RecorderActivity.this, permissions)) {
                chooseVideoForUpload();
            } else {
                EasyPermissions.requestPermissions(
                        this,
                        getString(R.string.permission_rationale_upload),
                        SharedConstants.REQUEST_CODE_PERMISSIONS_UPLOAD,
                        permissions);
            }
        });
        bsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onSlide(@NonNull View v, float offset) { }

            @Override
            public void onStateChanged(@NonNull View v, int state) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    long max;
                    max = SharedConstants.MAX_DURATION - mModel.recorded();
                    max = TimeUnit.MILLISECONDS.toSeconds(max);
                    max = TimeUnit.SECONDS.toMillis(max);
                    selection.setValue(0);
                    selection.setValueTo(max);
                    selection.setValue(max);
                    maximum.setText(TextFormatUtil.toMMSS(max));
                }
            }
        });
        SegmentedProgressBar segments = findViewById(R.id.segments);
        segments.enableAutoProgressView(SharedConstants.MAX_DURATION);
        segments.setDividerColor(Color.BLACK);
        segments.setDividerEnabled(true);
        segments.setDividerWidth(2f);
        segments.setListener(l -> { /* eaten */ });
        segments.setShader(new int[]{
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.colorAccent),
        });
        mCamera.addCameraListener(new CameraListener() {

            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);
                result.toBitmap(bitmap -> {
                    if (bitmap != null) {
                        Bitmap square = BitmapUtil.getSquareThumbnail(bitmap, 250);
                        bitmap.recycle();
                        Bitmap rounded = BitmapUtil.addRoundCorners(square, 25);
                        square.recycle();
                        FilterAdapter adapter =
                                new FilterAdapter(RecorderActivity.this, rounded);
                        adapter.setListener(RecorderActivity.this::applyPreviewFilter);
                        RecyclerView filters = findViewById(R.id.filters);
                        filters.setAdapter(adapter);
                        filters.setVisibility(View.VISIBLE);
                    }

                    mProgress.dismiss();
                });
            }

            @Override
            public void onVideoRecordingEnd() {
                Log.v(TAG, "Video recording has ended.");
                segments.pause();
                segments.addDivider();
                mHandler.removeCallbacks(mStopper);
                mHandler.postDelayed(() -> processCurrentRecording(), 500);
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                }

                mPulse.stop();
                mRecordButton.setSelected(false);
                toggleVisibility(true);
            }

            @Override
            public void onVideoRecordingStart() {
                Log.v(TAG, "Video recording has started.");
                segments.resume();
                if (mMediaPlayer != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        float speed = 1f;
                        if (mModel.speed == .5f) {
                            speed = 2f;
                        } else if (mModel.speed == .75f) {
                            speed = 1.5f;
                        } else if (mModel.speed == 1.5f) {
                            speed = .75f;
                        } else if (mModel.speed == 2f) {
                            speed = .5f;
                        }

                        PlaybackParams params = new PlaybackParams();
                        params.setSpeed(speed);
                        mMediaPlayer.setPlaybackParams(params);
                    }

                    mMediaPlayer.start();
                }

                mPulse = YoYo.with(Techniques.Pulse)
                        .repeat(YoYo.INFINITE)
                        .playOn(mRecordButton);
                mRecordButton.setSelected(true);
                toggleVisibility(false);
            }
        });
        mRecordButton = findViewById(R.id.record);
        mRecordButton.setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                stopRecording();
            } else {
                filters.setVisibility(View.GONE);
                speeds.setVisibility(View.GONE);
                stickers.deselectAll();
                startRecording();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        for (RecordSegment segment : mModel.segments) {
            File file = new File(segment.file);
            if (!file.delete()) {
                Log.v(TAG, "Could not delete record segment file: " + file);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void addSticker(File file) {
        TurboImageView stickers = findViewById(R.id.stickers);
        stickers.addObject(this, BitmapFactory.decodeFile(file.getAbsolutePath()));
        View remove = findViewById(R.id.remove);
        remove.setVisibility(View.VISIBLE);
    }

    private void applyPreviewFilter(VideoFilter filter) {
        switch (filter) {
            case BRIGHTNESS: {
                BrightnessFilter glf = (BrightnessFilter) Filters.BRIGHTNESS.newInstance();
                glf.setBrightness(1.2f);
                mCamera.setFilter(glf);
                break;
            }
            case EXPOSURE:
                mCamera.setFilter(new ExposureFilter());
                break;
            case GAMMA: {
                GammaFilter glf = (GammaFilter) Filters.GAMMA.newInstance();
                glf.setGamma(2);
                mCamera.setFilter(glf);
                break;
            }
            case GRAYSCALE:
                mCamera.setFilter(Filters.GRAYSCALE.newInstance());
                break;
            case HAZE: {
                HazeFilter glf = new HazeFilter();
                glf.setSlope(-0.5f);
                mCamera.setFilter(glf);
                break;
            }
            case INVERT:
                mCamera.setFilter(Filters.INVERT_COLORS.newInstance());
                break;
            case MONOCHROME:
                mCamera.setFilter(new MonochromeFilter());
                break;
            case PIXELATED: {
                PixelatedFilter glf = new PixelatedFilter();
                glf.setPixel(5);
                mCamera.setFilter(glf);
                break;
            }
            case POSTERIZE:
                mCamera.setFilter(Filters.POSTERIZE.newInstance());
                break;
            case SEPIA:
                mCamera.setFilter(Filters.SEPIA.newInstance());
                break;
            case SHARP: {
                SharpnessFilter glf = (SharpnessFilter) Filters.SHARPNESS.newInstance();
                glf.setSharpness(0.25f);
                mCamera.setFilter(glf);
                break;
            }
            case SOLARIZE:
                mCamera.setFilter(new SolarizeFilter());
                break;
            case VIGNETTE:
                mCamera.setFilter(Filters.VIGNETTE.newInstance());
                break;
            default:
                mCamera.setFilter(Filters.NONE.newInstance());
                break;
        }
    }

    private void applyVideoSpeed(File file, float speed, long duration) {
        File output = TempUtil.createNewFile(this, ".mp4");
        mProgress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Data data = new Data.Builder()
                .putString(VideoSpeedWorker2.KEY_INPUT, file.getAbsolutePath())
                .putString(VideoSpeedWorker2.KEY_OUTPUT, output.getAbsolutePath())
                .putFloat(VideoSpeedWorker2.KEY_SPEED, speed)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(VideoSpeedWorker2.class)
                .setInputData(data)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        mProgress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        RecordSegment segment = new RecordSegment();
                        segment.file = output.getAbsolutePath();
                        segment.duration = duration;
                        mModel.segments.add(segment);
                    }
                });
    }

    @AfterPermissionGranted(SharedConstants.REQUEST_CODE_PERMISSIONS_UPLOAD)
    private void chooseVideoForUpload() {
        IntentUtil.startChooser(
                this,
                SharedConstants.REQUEST_CODE_PICK_VIDEO,
                "video/mp4");
    }

    private void commitRecordings(@NonNull List<RecordSegment> segments, @Nullable Uri audio) {
        mProgress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        List<String> videos = new ArrayList<>();
        for (RecordSegment segment : segments) {
            videos.add(segment.file);
        }

        File merged = TempUtil.createNewFile(this, ".mp4");
        Data data = new Data.Builder()
                .putStringArray(MergeVideosWorker2.KEY_INPUTS, videos.toArray(new String[0]))
                .putString(MergeVideosWorker2.KEY_OUTPUT, merged.getAbsolutePath())
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MergeVideosWorker2.class)
                .setInputData(data)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        mProgress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        if (audio != null) {
                            proceedToVolume(merged, new File(audio.getPath()));
                        } else {
                            proceedToFilter(merged);
                        }
                    }
                });
    }

    private void confirmClose() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirmation_close_recording)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.close_button, (dialog, i) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    private void downloadSticker(Sticker sticker) {
        File stickers = new File(getFilesDir(), "stickers");
        if (!stickers.exists() && !stickers.mkdirs()) {
            Log.w(TAG, "Could not create directory at " + stickers);
        }

        String extension = sticker.image.substring(sticker.image.lastIndexOf(".") + 1);
        File image = new File(stickers, sticker.id + extension);
        if (image.exists()) {
            addSticker(image);
            return;
        }

        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Data input = new Data.Builder()
                .putString(FileDownloadWorker.KEY_INPUT, sticker.image)
                .putString(FileDownloadWorker.KEY_OUTPUT, image.getAbsolutePath())
                .build();
        WorkRequest request = new OneTimeWorkRequest.Builder(FileDownloadWorker.class)
                .setInputData(input)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        addSticker(image);
                    }
                });
    }

    private void proceedToFilter(File video) {
        Log.v(TAG, "Proceeding to filter screen with " + video);
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra(FilterActivity.EXTRA_SONG, mModel.song);
        intent.putExtra(FilterActivity.EXTRA_VIDEO, video.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void proceedToVolume(File video, File audio) {
        Log.v(TAG, "Proceeding to volume screen with " + video + "; " + audio);
        Intent intent = new Intent(this, VolumeActivity.class);
        intent.putExtra(VolumeActivity.EXTRA_SONG, mModel.song);
        intent.putExtra(VolumeActivity.EXTRA_VIDEO, video.getAbsolutePath());
        intent.putExtra(VolumeActivity.EXTRA_AUDIO, audio.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void processCurrentRecording() {
        long duration = VideoUtil.getDuration(this, Uri.fromFile(mModel.video));
        if (mModel.speed != 1) {
            applyVideoSpeed(mModel.video, mModel.speed, duration);
        } else {
            RecordSegment segment = new RecordSegment();
            segment.file = mModel.video.getAbsolutePath();
            segment.duration = duration;
            mModel.segments.add(segment);
        }

        mModel.video = null;
    }

    private void setupSong(@Nullable Song song, Uri file) {
        mMediaPlayer = MediaPlayer.create(this, file);
        mMediaPlayer.setOnCompletionListener(mp -> mMediaPlayer = null);
        TextView sound = findViewById(R.id.sound);
        if (song != null) {
            sound.setText(song.title);
            mModel.song = song.id;
        } else {
            sound.setText(getString(R.string.audio_from_clip));
        }

        mModel.audio = file;
    }

    private void startRecording() {
        long recorded = mModel.recorded();
        if (recorded >= SharedConstants.MAX_DURATION) {
            Toast.makeText(RecorderActivity.this, R.string.recorder_error_maxed_out, Toast.LENGTH_SHORT).show();
        } else {
            mModel.video = TempUtil.createNewFile(this, ".mp4");
            mCamera.takeVideoSnapshot(
                    mModel.video, (int)(SharedConstants.MAX_DURATION - recorded));
        }
    }

    @SuppressLint("SetTextI18n")
    private void startTimer() {
        View countdown = findViewById(R.id.countdown);
        TextView count = findViewById(R.id.count);
        count.setText(null);
        Slider selection = findViewById(R.id.selection);
        long duration = (long)selection.getValue();
        CountDownTimer timer = new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long remaining) {
                mHandler.post(() -> count.setText(TimeUnit.MILLISECONDS.toSeconds(remaining) + 1 + ""));
            }

            @Override
            public void onFinish() {
                mHandler.post(() -> countdown.setVisibility(View.GONE));
                startRecording();
                mHandler.postDelayed(mStopper, duration);
            }
        };
        countdown.setOnClickListener(v -> {
            timer.cancel();
            countdown.setVisibility(View.GONE);
        });
        countdown.setVisibility(View.VISIBLE);
        timer.start();
    }

    private void stopRecording() {
        mCamera.stopVideo();
    }

    private void submitUpload(Uri uri) {
        File copy = TempUtil.createCopy(this, uri, ".mp4");
        Intent intent = new Intent(this, TrimmerActivity.class);
        if (mModel.audio != null) {
            intent.putExtra(TrimmerActivity.EXTRA_AUDIO, mModel.audio.getPath());
        }

        intent.putExtra(TrimmerActivity.EXTRA_SONG, mModel.song);
        intent.putExtra(TrimmerActivity.EXTRA_VIDEO, copy.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void toggleVisibility(boolean show) {
        if (!getResources().getBoolean(R.bool.clutter_free_recording_enabled)) {
            return;
        }

        View top = findViewById(R.id.top);
        AnimationUtil.toggleVisibilityToTop(top, show);
        View right = findViewById(R.id.right);
        AnimationUtil.toggleVisibilityToRight(right, show);
        View upload = findViewById(R.id.upload);
        AnimationUtil.toggleVisibilityToBottom(upload, show);
        View done = findViewById(R.id.done);
        AnimationUtil.toggleVisibilityToBottom(done, show);
        View left = findViewById(R.id.left);
        AnimationUtil.toggleVisibilityToLeft(left, show);
    }

    public static class RecorderActivityViewModel extends ViewModel {

        public Uri audio;
        public List<RecordSegment> segments = new ArrayList<>();
        public int song = 0;
        public float speed = 1;
        public File video;

        public long recorded() {
            long recorded = 0;
            for (RecordSegment segment : segments) {
                recorded += segment.duration;
            }

            return recorded;
        }
    }

    private static class RecordSegment {

        public String file;
        public long duration;
    }
}
