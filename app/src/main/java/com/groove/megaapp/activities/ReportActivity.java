package com.groove.megaapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.data.api.REST;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_SUBJECT_TYPE = "subject_type";
    public static final String EXTRA_REPORT_SUBJECT_ID = "subject_id";
    private static final String TAG = "ReportActivity";

    private ReportActivityViewModel mModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        mModel = new ViewModelProvider(this).get(ReportActivityViewModel.class);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.report_label);
        ImageButton done = findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(v -> submitReport());
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.reason_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner reason = findViewById(R.id.reason);
        reason.setAdapter(adapter);
        List<String> codes = Arrays.asList(
                getResources().getStringArray(R.array.reason_codes)
        );
        reason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mModel.reason = codes.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        if (TextUtils.isEmpty(mModel.reason)) {
            mModel.reason = codes.get(0);
        }
        reason.setSelection(codes.indexOf(mModel.reason));
        TextInputLayout message = findViewById(R.id.message);
        //noinspection ConstantConditions
        message.getEditText().setText(mModel.message);
        message.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.message = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        mModel.errors.observe(this, errors -> {
            message.setError(null);
            if (errors == null) {
                return;
            }
            if (errors.containsKey("message")) {
                message.setError(errors.get("message"));
            }
        });
    }

    private void showErrors(JSONObject json) throws Exception {
        JSONObject errors = json.getJSONObject("errors");
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{"message"};
        for (String key : keys) {
            JSONArray fields = errors.optJSONArray(key);
            if (fields != null) {
                messages.put(key, fields.getString(0));
            }
        }

        mModel.errors.postValue(messages);
    }

    private void submitReport() {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel.errors.postValue(null);
        String subjectType = getIntent().getStringExtra(EXTRA_REPORT_SUBJECT_TYPE);
        int subjectId = getIntent().getIntExtra(EXTRA_REPORT_SUBJECT_ID, 0);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.reportsCreate(subjectType, subjectId, mModel.reason, mModel.message)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Submitting report returned with " + code + " status.");
                        if (code == 200) {
                            Toast.makeText(ReportActivity.this, R.string.report_submitted, Toast.LENGTH_SHORT).show();
                            finish();
                        } else if (code == 422) {
                            try {
                                //noinspection ConstantConditions
                                String content = response.body().string();
                                showErrors(new JSONObject(content));
                            } catch (Exception ignore) {
                            }
                        } else {
                            Toast.makeText(ReportActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        }
                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Submitting report has failed.", t);
                        Toast.makeText(ReportActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        progress.dismiss();
                    }
                });
    }

    public static class ReportActivityViewModel extends ViewModel {

        private MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();
        public String reason = null;
        public String message = "";
    }
}
