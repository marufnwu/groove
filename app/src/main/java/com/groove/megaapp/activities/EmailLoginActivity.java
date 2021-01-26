package com.groove.megaapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
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

public class EmailLoginActivity extends AppCompatActivity {

    public static final String EXTRA_TOKEN = "token";
    private static final String TAG = "EmailLoginActivity";

    private EmailLoginActivityViewModel mModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.login_label);
        findViewById(R.id.header_more).setVisibility(View.GONE);
        mModel = new ViewModelProvider(this).get(EmailLoginActivityViewModel.class);
        TextInputLayout name = findViewById(R.id.name);
        name.getEditText().setText(mModel.name);
        name.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.name = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout email = findViewById(R.id.email);
        email.getEditText().setText(mModel.email);
        email.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.email = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout otp = findViewById(R.id.otp);
        otp.getEditText().setText(mModel.otp);
        otp.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.otp = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        View generate = findViewById(R.id.generate);
        generate.setOnClickListener(v -> generateOtp());
        View verify = findViewById(R.id.verify);
        verify.setOnClickListener(v -> verifyOtp());
        mModel.doesExist.observe(this, exists -> {
            Boolean sent = mModel.isSent.getValue();
            name.setVisibility(sent && !exists ? View.VISIBLE : View.GONE);
        });
        mModel.isSent.observe(this, sent -> {
            Boolean exists = mModel.doesExist.getValue();
            name.setVisibility(sent && !exists ? View.VISIBLE : View.GONE);
            otp.setVisibility(sent ? View.VISIBLE : View.GONE);
            if (sent) {
                otp.requestFocus();
            }

            verify.setEnabled(sent);
        });
        mModel.errors.observe(this, errors -> {
            email.setError(null);
            otp.setError(null);
            name.setError(null);
            if (errors == null) {
                return;
            }
            if (errors.containsKey("email")) {
                email.setError(errors.get("email"));
            }
            if (errors.containsKey("otp")) {
                otp.setError(errors.get("otp"));
            }
            if (errors.containsKey("name")) {
                name.setError(errors.get("name"));
            }
        });
        email.getEditText().requestFocus();
    }

    private void generateOtp() {
        mModel.errors.postValue(null);
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.loginEmailOtp(mModel.email)
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
                            message = R.string.login_otp_sent_email;
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
                            Toast.makeText(EmailLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Exists> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to generate OTP.", t);
                        Toast.makeText(EmailLoginActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        progress.dismiss();
                    }
                });
    }

    private void showErrors(JSONObject json) throws Exception {
        JSONObject errors = json.getJSONObject("errors");
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{"name", "email", "otp"};
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
        rest.loginEmail(mModel.email, mModel.otp, mModel.name)
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
                                Toast.makeText(EmailLoginActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Token> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to verify OTP.", t);
                        Toast.makeText(EmailLoginActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        progress.dismiss();
                    }
                });
    }

    public static class EmailLoginActivityViewModel extends ViewModel {

        public String name = "";
        public String email = "";
        public String otp = "";
        public MutableLiveData<Boolean> doesExist = new MutableLiveData<>(false);
        public MutableLiveData<Boolean> isSent = new MutableLiveData<>(false);

        public MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();
    }
}
