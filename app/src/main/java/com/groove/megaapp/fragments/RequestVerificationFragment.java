package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.utils.IntentUtil;
import com.groove.megaapp.utils.TempUtil;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("LongLogTag")
public class RequestVerificationFragment extends Fragment {

    private RequestVerificationFragmentViewModel mModel;
    private static final String TAG = "RequestVerificationFragment";

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.v(TAG, "Received request: " + requestCode + ", result: " + resultCode + ".");
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_DOCUMENT && resultCode == Activity.RESULT_OK && data != null) {
            Uri selection = data.getData();
            if (selection != null) {
                selectFile(selection);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(this).get(RequestVerificationFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_request_verification, container, false);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.header_back)
                .setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.verification_label);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        TextInputLayout file = view.findViewById(R.id.file);
        if (mModel.document != null) {
            String extension = FilenameUtils.getExtension(mModel.document.getAbsolutePath());
            file.getEditText().setText(
                    getString(R.string.verification_file_selected, extension.toUpperCase(Locale.US))
            );
        } else {
            file.getEditText().setText(R.string.verification_file_none);
        }

        Button browse = view.findViewById(R.id.browse);
        browse.setOnClickListener(v -> IntentUtil.startChooser(
                this,
                SharedConstants.REQUEST_CODE_PICK_DOCUMENT,
                "application/pdf", "image/png", "image/jpeg", "image/jpg"));
        Button submit = view.findViewById(R.id.submit);
        submit.setOnClickListener(v -> {
            if (mModel.document != null) {
                submitToServer();
            } else {
                Toast.makeText(requireContext(), R.string.error_choose_file, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static Fragment newInstance() {
        return new RequestVerificationFragment();
    }

    @SuppressWarnings("ConstantConditions")
    private void selectFile(Uri uri) {
        String mime = requireContext().getContentResolver().getType(uri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (TextUtils.isEmpty(extension)) {
            extension = "txt";
        }

        File copy = TempUtil.createCopy(requireContext(), uri, "." + extension);
        if (mModel.document != null) {
            //noinspection ResultOfMethodCallIgnored
            mModel.document.delete();
        }

        mModel.document = copy;
        TextInputLayout file = getView().findViewById(R.id.file);
        file.getEditText().setText(
                getString(R.string.verification_file_selected, extension.toUpperCase(Locale.US))
        );
    }

    private void submitToServer() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        RequestBody body = RequestBody.create(mModel.document, null);
        MultipartBody.Part document =
                MultipartBody.Part.createFormData("document", mModel.document.getName(), body);
        Call<ResponseBody> call = rest.verificationsCreate(document);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response
            ) {
                progress.dismiss();
                if (response != null && response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.verification_requested, Toast.LENGTH_SHORT).show();
                    ((MainActivity)requireActivity()).popBackStack();
                } else if (response != null && response.code() == 422) {
                    try {
                        //noinspection ConstantConditions
                        String content = response.errorBody().string();
                        JSONObject json = new JSONObject(content);
                        JSONObject errors = json.optJSONObject("errors");
                        if (errors != null) {
                            JSONArray messages = errors.optJSONArray("document");
                            if (messages != null) {
                                String error = messages.getString(0);
                                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception ignore) {
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when trying to request verification.", t);
                Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                progress.dismiss();
            }
        });
    }

    public static class RequestVerificationFragmentViewModel extends ViewModel {

        public File document;
    }
}
