package com.groove.megaapp.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.List;

import com.groove.megaapp.R;
import com.groove.megaapp.data.models.Promotion;

public class PromotionsDialogFragment extends DialogFragment {

    private static final String ARG_PROMOTIONS = "promotions";

    private List<Promotion> mPromotions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPromotions = requireArguments().getParcelableArrayList(ARG_PROMOTIONS);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //noinspection ConstantConditions
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return inflater.inflate(R.layout.fragment_promotions_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.close).setOnClickListener(v -> dismiss());
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new PromotionPagerAdapter(this));
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + position);
                if (fragment != null) {
                    updatePagerHeight(fragment.requireView(), pager);
                }
            }
        });
        WormDotsIndicator indicator = view.findViewById(R.id.indicator);
        indicator.setViewPager2(pager);
    }

    public static PromotionsDialogFragment newInstance(List<Promotion> promotions) {
        Bundle arguments = new Bundle();
        arguments.putParcelableArrayList(ARG_PROMOTIONS, new ArrayList<>(promotions));
        PromotionsDialogFragment fragment = new PromotionsDialogFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private static void updatePagerHeight(View view, ViewPager2 pager) {
        view.post(() -> {
            int specW = View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY);
            int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(specW, specH);
            ViewGroup.LayoutParams params = pager.getLayoutParams();
            if (params.height != view.getMeasuredHeight()) {
                params.height = view.getMeasuredHeight();
                pager.setLayoutParams(params);
            }
        });
    }

    private class PromotionPagerAdapter extends FragmentStateAdapter {

        public PromotionPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return PromotionFragment.newInstance(mPromotions.get(position));
        }

        @Override
        public int getItemCount() {
            return mPromotions.size();
        }
    }
}
