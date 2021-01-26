package com.groove.megaapp.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.multidex.BuildConfig;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.utils.TempUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private SplashActivityViewModel mModel;

    private final RequestListener<GifDrawable> mRequestListener = new RequestListener<GifDrawable>() {

        @Override
        public boolean onLoadFailed(
                @Nullable GlideException e,
                Object model,
                Target<GifDrawable> target,
                boolean first
        ) {
            return false;
        }

        @Override
        public boolean onResourceReady(
                GifDrawable resource,
                Object model,
                Target<GifDrawable> target,
                DataSource source,
                boolean first
        ) {
            resource.setLoopCount(1);
            resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {

                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    mModel.animated = true;
                    afterAnimation();
                }
            });
            return false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        // Add code to print out the key hash
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.groove.megaapp",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                //Toast.makeText(this, Base64.encodeToString(md.digest(), Base64.DEFAULT), Toast.LENGTH_LONG).show();
                Log.e("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
        mModel = new ViewModelProvider(this).get(SplashActivityViewModel.class);
        ImageView logo = findViewById(R.id.logo);
        if (logo != null) {
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.logo_splash)
                    .listener(mRequestListener)
                    .into(logo);
        } else {
            mModel.animated = true;
        }

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && TextUtils.equals(intent.getType(), "video/mp4")) {
            mModel.stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mModel.animated && !mModel.continued) {
            afterAnimation();
        }
    }

    private void afterAnimation() {
        boolean intro = Prefs.getBoolean(SharedConstants.PREF_INTRO_SHOWN, false);
        if (getResources().getBoolean(R.bool.skip_intro_screen) || intro) {
            mModel.continued = true;
            continueWith();
        } else {
            Prefs.putBoolean(SharedConstants.PREF_INTRO_SHOWN, true);
            startIntroActivity();
        }
    }

    private void continueWith() {
        String token = Prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
        if (TextUtils.isEmpty(token)) {
            continueWithProfile(null);
        } else {
            REST rest = MainApplication.getContainer().get(REST.class);
            rest.profileShow()
                    .enqueue(new Callback<Wrappers.Single<User>>() {

                        @Override
                        public void onResponse(
                                @Nullable Call<Wrappers.Single<User>> call,
                                @Nullable Response<Wrappers.Single<User>> response
                        ) {
                            int code = response != null ? response.code() : -1;
                            Log.w(TAG, "Checking token validity with server returned " + code + ".");
                            if (response != null && response.isSuccessful()) {
                                continueWithProfile(response.body().data);
                            } else {
                                continueWithProfile(null);
                            }
                        }

                        @Override
                        public void onFailure(
                                @Nullable Call<Wrappers.Single<User>> call,
                                @Nullable Throwable t
                        ) {
                            Log.e(TAG, "Failed to validate token status.", t);
                            continueWithProfile(null);
                        }
                    });
        }
    }

    private void continueWithAds(@Nullable User user) {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnCompleteListener(result -> {
                    Uri uri = getIntent().getData();
                    if (result.isSuccessful()) {
                        PendingDynamicLinkData data = result.getResult();
                        if (data != null) {
                            Log.v(TAG, "Found dynamic link " + data.getLink());
                            uri = data.getLink();
                        }
                    }

                    continueWithDeepLink(user, uri);
                });
    }

    private void continueWithDeepLink(@Nullable User user, @Nullable Uri data) {
        boolean languages = Prefs.getBoolean(SharedConstants.PREF_LANGUAGES_OFFERED, false);
        if (getResources().getBoolean(R.bool.skip_language_screen) || languages) {
            startMainActivity(user, data);
        } else {
            Prefs.putBoolean(SharedConstants.PREF_LANGUAGES_OFFERED, true);
            startLanguageActivity(user);
        }

        finish();
    }

    private void continueWithProfile(@Nullable User user) {
        if (user != null && mModel.stream != null) {
            startSharing(mModel.stream);
            return;
        }

        long ads = Prefs.getLong(SharedConstants.PREF_ADS_SYNCED_AT, 0);
        if (BuildConfig.DEBUG || ads < System.currentTimeMillis() - SharedConstants.SYNC_ADS_INTERVAL) {
            Prefs.putLong(SharedConstants.PREF_ADS_SYNCED_AT, System.currentTimeMillis());
            REST rest = MainApplication.getContainer().get(REST.class);
            rest.advertisementsIndex(1)
                    .enqueue(new Callback<Wrappers.Paginated<Advertisement>>() {

                        @Override
                        public void onFailure(
                                @Nullable Call<Wrappers.Paginated<Advertisement>> call,
                                @Nullable Throwable t
                        ) {
                            Log.e(TAG, "Could not fetch advertisements from server.", t);
                            continueWithAds(user);
                        }

                        @Override
                        public void onResponse(
                                @Nullable Call<Wrappers.Paginated<Advertisement>> call,
                                @Nullable Response<Wrappers.Paginated<Advertisement>> response
                        ) {
                            int code = response != null ? response.code() : -1;
                            Log.w(TAG, "Fetching advertisements from server returned " + code + ".");
                            if (code == 200) {
                                List<Advertisement> ads = response.body().data;
                                ObjectMapper om = MainApplication.getContainer().get(ObjectMapper.class);
                                try {
                                    String json = om.writeValueAsString(ads);
                                    Prefs.putString(SharedConstants.PREF_ADS_CONFIG, json);
                                } catch (JsonProcessingException e) {
                                    Log.e(TAG, "Error in saving ads as JSON.", e);
                                }
                            }

                            continueWithAds(user);
                        }
                    });
        } else {
            continueWithAds(user);
        }
    }

    private void startIntroActivity() {
        startActivity(new Intent(this, IntroActivity.class));
    }

    private void startLanguageActivity(@Nullable User user) {
        Intent intent = new Intent(this, LanguageActivity.class);
        intent.putExtra(LanguageActivity.EXTRA_SPLASH, true);
        intent.putExtra(LanguageActivity.EXTRA_USER, user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void startMainActivity(@Nullable User user, @Nullable Uri data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USER, user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (data != null) {
            intent.setData(data);
        }

        startActivity(intent);
    }

    private void startSharing(Uri uri) {
        File copy = TempUtil.createCopy(this, uri, ".mp4");
        Intent intent = new Intent(this, TrimmerActivity.class);
        intent.putExtra(TrimmerActivity.EXTRA_VIDEO, copy.getAbsolutePath());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static class SplashActivityViewModel extends ViewModel {

        public boolean animated = false;
        public boolean continued = false;
        public Uri stream;
    }
}
