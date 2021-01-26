package com.groove.megaapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("LongLogTag")
public class PhoneLoginFirebaseActivity extends PhoneLoginBaseActivity {

    private static final String TAG = "PhoneLoginFirebaseActivity";

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private KProgressHUD mProgress;
    private String mVerificationId;
    private boolean mVerificationInProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View generate = findViewById(R.id.generate);
        generate.setOnClickListener(v -> generateOtp());
        View verify = findViewById(R.id.verify);
        verify.setOnClickListener(v -> verifyOtp());
        mAuth = FirebaseAuth.getInstance();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(@NotNull PhoneAuthCredential credential) {
                Log.d(TAG, "Phone verification successfully completed, credential:" + credential);
                mVerificationInProgress = false;
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }

                loginWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NotNull FirebaseException e) {
                Log.d(TAG, "Phone verification failed with Firebase.", e);
                mVerificationInProgress = false;
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("phone", getString(R.string.error_invalid_phone));
                    mModel.errors.postValue(errors);
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.error_internet,
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "Phone verification code was sent: " + verificationId);
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                mVerificationId = verificationId;
                mModel.doesExist.postValue(true);
                mModel.isSent.postValue(true);
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mVerificationInProgress) {
            generateOtp();
        }
    }

    private void generateOtp() {
        mProgress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel.errors.postValue(null);
        Map<String, String> errors = new HashMap<>();
        if (TextUtils.isEmpty(mModel.cc + "")) {
            errors.put("cc", getString(R.string.error_field_required));
        } else if (TextUtils.isEmpty(mModel.phone)) {
            errors.put("phone", getString(R.string.error_field_required));
        }

        if (errors.isEmpty()) {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    "+" + mModel.cc + mModel.phone,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    mCallbacks);
            mVerificationInProgress = true;
        } else {
            mModel.errors.postValue(errors);
        }
    }

    private void verifyOtp() {
        mModel.errors.postValue(null);
        Map<String, String> errors = new HashMap<>();
        if (TextUtils.isEmpty(mModel.otp)) {
            errors.put("otp", getString(R.string.error_field_required));
        }

        if (errors.isEmpty()) {
            PhoneAuthCredential credential =
                    PhoneAuthProvider.getCredential(mVerificationId, mModel.otp);
            loginWithCredential(credential);
        } else {
            mModel.errors.postValue(errors);
        }
    }

    private void loginWithCredential(PhoneAuthCredential credential) {
        mProgress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel.errors.postValue(null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        user.getIdToken(false).addOnCompleteListener(this, task1 -> {
                            if (task.isSuccessful()) {
                                String token = task1.getResult().getToken();
                                loginWithServer(token);
                            } else if (mProgress != null && mProgress.isShowing()) {
                                mProgress.dismiss();
                            }
                        });
                    } else {
                        if (mProgress != null && mProgress.isShowing()) {
                            mProgress.dismiss();
                        }

                        Log.e(TAG, "Signin with phone credential failed.", task.getException());
                        Map<String, String> errors = new HashMap<>();
                        errors.put("otp", getString(R.string.error_invalid_otp));
                        mModel.errors.postValue(errors);
                    }
                });
    }

    private void loginWithServer(String token) {
        Log.v(TAG, "Transmitting Firebase ID token to server: " + token);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.loginFirebase(token)
                .enqueue(new Callback<Token>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Token> call,
                            @Nullable Response<Token> response
                    ) {
                        if (mProgress != null && mProgress.isShowing()) {
                            mProgress.dismiss();
                        }

                        if (response != null && response.isSuccessful()) {
                            Intent data = new Intent();
                            data.putExtra(EXTRA_TOKEN, response.body());
                            setResult(RESULT_OK, data);
                            finish();
                        } else {
                            Toast.makeText(PhoneLoginFirebaseActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Token> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Login with Firebase phone auth has failed.", t);
                        if (mProgress != null && mProgress.isShowing()) {
                            mProgress.dismiss();
                        }

                        Toast.makeText(PhoneLoginFirebaseActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
