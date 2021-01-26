package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding4.widget.RxTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.ads.BannerAdProvider;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.utils.AdsUtil;

public class SearchFragment extends Fragment {

    private BannerAdProvider mAd;
    private final List<Disposable> mDisposables = new ArrayList<>();
    private MainActivity.MainActivityViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Advertisement ad = AdsUtil.findByLocationAndType("search", "banner");
        if (ad != null) {
            mAd = new BannerAdProvider(ad);
        }
        mModel = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Disposable disposable : mDisposables) {
            disposable.dispose();
        }

        mDisposables.clear();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.search_label);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        ViewPager2 pager = view.findViewById(R.id.pager);
        SearchPagerAdapter adapter = new SearchPagerAdapter(this);
        pager.setAdapter(adapter);
        TabLayout tabs = view.findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) ->
                tab.setText(position == 0 ? R.string.users_label : R.string.hashtags_label)).attach();
        TextInputLayout q = view.findViewById(R.id.q);
        q.getEditText().setText(mModel.searchTerm.getValue());
        Disposable disposable = RxTextView.afterTextChangeEvents(q.getEditText())
                .skipInitialValue()
                .debounce(250, TimeUnit.MILLISECONDS)
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel.searchTerm.postValue(editable != null ? editable.toString() : null);
                });
        mDisposables.add(disposable);
        if (mAd != null) {
            View ad = mAd.create(requireContext());
            if (ad != null) {
                LinearLayout banner = view.findViewById(R.id.banner);
                banner.removeAllViews();
                banner.addView(ad);
            }
        }
    }

    private static class SearchPagerAdapter extends FragmentStateAdapter {

        public SearchPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 1) {
                return SearchHashtagsFragment.newInstance();
            }

            return SearchUsersFragment.newInstance();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
