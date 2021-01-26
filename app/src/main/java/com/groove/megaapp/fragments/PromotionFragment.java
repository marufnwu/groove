package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.facebook.drawee.view.SimpleDraweeView;

import com.groove.megaapp.R;
import com.groove.megaapp.data.models.Promotion;

public class PromotionFragment extends Fragment {

    private static final String ARG_PROMOTION = "promotion";

    private Promotion mPromotion;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPromotion = requireArguments().getParcelable(ARG_PROMOTION);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_promotion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView title = view.findViewById(R.id.title);
        TextView description = view.findViewById(R.id.description);
        SimpleDraweeView image = view.findViewById(R.id.image);
        title.setText(mPromotion.title);
        description.setText(mPromotion.description);
        image.setImageURI(mPromotion.image);
    }

    public static PromotionFragment newInstance(Promotion promotion) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_PROMOTION, promotion);
        PromotionFragment fragment = new PromotionFragment();
        fragment.setArguments(arguments);
        return fragment;
    }
}
