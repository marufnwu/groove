package com.groove.megaapp.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.groove.megaapp.R;

public class InterstitialAdActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE = "image";
    public static final String EXTRA_LINK = "link";
    private static final String TAG = "InterstitialAdActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interstitial_ad);
        String image = getIntent().getStringExtra(EXTRA_IMAGE);
        String link = getIntent().getStringExtra(EXTRA_LINK);
        findViewById(R.id.close)
                .setOnClickListener(v -> finish());
        ImageView view = findViewById(R.id.image);
        View loading = findViewById(R.id.loading);
        if (image.endsWith(".gif")) {
            Glide.with(this)
                    .asGif()
                    .load(image)
                    .listener(new RequestListener<GifDrawable>() {

                        @Override
                        public boolean onLoadFailed(
                                @Nullable GlideException e,
                                Object model,
                                Target<GifDrawable> target,
                                boolean first
                        ) {
                            Log.e(TAG, "Failed to load ad image: " + image, e);
                            finish();
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(
                                GifDrawable resource,
                                Object model,
                                Target<GifDrawable> target,
                                DataSource source,
                                boolean first
                        ) {
                            loading.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(view);
        } else {
            Glide.with(this)
                    .load(image)
                    .listener(new RequestListener<Drawable>() {

                        @Override
                        public boolean onLoadFailed(
                                @Nullable GlideException e,
                                Object model,
                                Target<Drawable> target,
                                boolean first
                        ) {
                            Log.e(TAG, "Failed to load ad image: " + image, e);
                            finish();
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(
                                Drawable resource,
                                Object model,
                                Target<Drawable> target,
                                DataSource source,
                                boolean first
                        ) {
                            loading.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(view);
        }

        view.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))));
    }
}
