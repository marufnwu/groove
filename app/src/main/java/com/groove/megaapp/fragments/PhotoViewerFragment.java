package com.groove.megaapp.fragments;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.kaopiz.kprogresshud.KProgressHUD;

import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;

public class PhotoViewerFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_URL = "url";

    private String mTitle;
    private Uri mUrl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = requireArguments().getString(ARG_TITLE);
        mUrl = requireArguments().getParcelable(ARG_URL);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(mTitle);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        PhotoView photo = view.findViewById(R.id.photo);
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Glide.with(requireContext())
                .load(mUrl)
                .addListener(new RequestListener<Drawable>() {

                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        progress.dismiss();
                        Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progress.dismiss();
                        return false;
                    }
                })
                .into(photo);
    }

    public static PhotoViewerFragment newInstance(String title, Uri url) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TITLE, title);
        bundle.putParcelable(ARG_URL, url);
        PhotoViewerFragment fragment = new PhotoViewerFragment();
        fragment.setArguments(bundle);
        return fragment;
    }
}
