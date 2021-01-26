package com.groove.megaapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.kaopiz.kprogresshud.KProgressHUD;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Exists;
import com.groove.megaapp.data.models.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("LongLogTag")
public class PhoneLoginServerActivity extends PhoneLoginBaseActivity {

    private static final String TAG = "PhoneLoginServerActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View generate = findViewById(R.id.generate);
        generate.setOnClickListener(v -> generateOtp());
        View verify = findViewById(R.id.verify);
        verify.setOnClickListener(v -> verifyOtp());
    }

    private void generateOtp() {
        mModel.errors.postValue(null);
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.loginPhoneOtp(mModel.cc + "", mModel.phone)
                .enqueue(new Callback<Exists>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Exists> call,
                            @Nullable Response<Exists> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        int message = -1;
                        if (code == 200) {
                            boolean exists = response.body().exists;
                            mModel.doesExist.postValue(exists);
                            mModel.isSent.postValue(true);
                            message = R.string.login_otp_sent;
                        } else if (code == 422) {
                            try {
                                String content = response.errorBody().string();
                                showErrors(new JSONObject(content));
                            } catch (Exception ignore) {
                            }
                        } else {
                            message = R.string.error_internet;
                        }

                        if (message != -1) {
                            Toast.makeText(PhoneLoginServerActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Exists> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to generate OTP.", t);
                        Toast.makeText(PhoneLoginServerActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        progress.dismiss();
                    }
                });
    }

    private void showErrors(JSONObject json) throws Exception {
        JSONObject errors = json.getJSONObject("errors");
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{"cc", "phone", "otp", "name"};
        for (String key : keys) {
            JSONArray fields = errors.optJSONArray(key);
            if (fields != null) {
                messages.put(key, fields.getString(0));
            }
        }

        mModel.errors.postValue(messages);
    }

    private void verifyOtp() {
        mModel.errors.postValue(null);
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.loginPhone(mModel.cc + "", mModel.phone, mModel.otp, mModel.name)
                .enqueue(new Callback<Token>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Token> call,
                            @Nullable Response<Token> response
                    ) {
                        progress.dismiss();
                        if (response != null) {
                            if (response.isSuccessful()) {
                                Intent data = new Intent();
                                data.putExtra(EXTRA_TOKEN, response.body());
                                setResult(RESULT_OK, data);
                                finish();
                            } else if (response.code() == 422) {
                                try {
                                    String content = response.errorBody().string();
                                    showErrors(new JSONObject(content));
                                } catch (Exception ignore) {
                                }
                            } else {
                                Toast.makeText(PhoneLoginServerActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Token> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to verify OTP.", t);
                        Toast.makeText(PhoneLoginServerActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        progress.dismiss();
                    }
                });
    }
}
