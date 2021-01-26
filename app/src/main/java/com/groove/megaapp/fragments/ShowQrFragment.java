package com.groove.megaapp.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.sumimakito.awesomeqr.AwesomeQrRenderer;
import com.github.sumimakito.awesomeqr.RenderResult;
import com.github.sumimakito.awesomeqr.option.RenderOption;
import com.github.sumimakito.awesomeqr.option.color.Color;

import com.groove.megaapp.BuildConfig;
import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.data.models.User;

public class ShowQrFragment extends Fragment {

    private static final String TAG = "ShowQrFragment";

    private MainActivity.MainActivityViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.qr_label);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        User user = mModel.user.getValue();
        TextView name = view.findViewById(R.id.name);
        name.setText(user.name);
        TextView username = view.findViewById(R.id.username);
        username.setText(user.username);
        Uri base = Uri.parse(getString(R.string.server_url));
        Uri link = base.buildUpon()
                .path("links/users")
                .appendQueryParameter("user", user.id + "")
                .appendQueryParameter("package", BuildConfig.APPLICATION_ID)
                .build();
        RenderOption option = new RenderOption();
        Color color = new Color();
        color.setBackground(android.graphics.Color.WHITE);
        color.setDark(android.graphics.Color.BLACK);
        color.setLight(android.graphics.Color.WHITE);
        option.setColor(color);
        option.setContent(link.toString());
        option.setPatternScale(1f);
        option.setSize(1024);
        RenderResult result = null;
        try {
            result = AwesomeQrRenderer.render(option);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate a QR code.", e);
        }

        if (result != null) {
            ImageView image = view.findViewById(R.id.image);
            image.setImageBitmap(result.getBitmap());
        }
    }

    public static ShowQrFragment newInstance() {
        return new ShowQrFragment();
    }
}
