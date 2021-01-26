package com.groove.megaapp.fragments;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ScanQrFragment extends Fragment {

    private static final String TAG = "ScanQrFragment";

    private CodeScanner mCodeScanner;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan_qr, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCodeScanner.releaseResources();
    }

    @Override
    public void onResume() {
        super.onResume();
        String[] permissions = new String[]{Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(requireContext(), permissions)) {
            mCodeScanner.startPreview();
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.permission_rationale_scan),
                    SharedConstants.REQUEST_CODE_PERMISSIONS_SCAN,
                    permissions);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.scan_label);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        CodeScannerView scanner = view.findViewById(R.id.scanner);
        mCodeScanner = new CodeScanner(requireActivity(), scanner);
        mCodeScanner.setDecodeCallback(result -> {
            Log.v(TAG, "Found a valid QR code maybe.");
            boolean valid = false;
            String content = result.getText();
            if (URLUtil.isValidUrl(content)) {
                Uri base = Uri.parse(getString(R.string.server_url));
                Uri encoded = Uri.parse(content);
                if (TextUtils.equals(base.getHost(), encoded.getHost())) {
                    mHandler.post(() -> handleUrl(encoded));
                    valid = true;
                }
            }

            if (!valid) {
                Toast.makeText(requireContext(), R.string.message_qr_invalid, Toast.LENGTH_SHORT).show();
                mCodeScanner.startPreview();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void handleUrl(Uri uri) {
        NavController controller = ((MainActivity)requireActivity()).findNavController();
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setData(uri);
        controller.handleDeepLink(intent);
    }

    public static ScanQrFragment newInstance() {
        return new ScanQrFragment();
    }

    @AfterPermissionGranted(SharedConstants.REQUEST_CODE_PERMISSIONS_SCAN)
    private void startScanner() {
        mCodeScanner.startPreview();
    }
}
