package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.data.ClipDataSource;

public class PlayerTabsFragment extends Fragment {

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
        return inflater.inflate(R.layout.fragment_player_tabs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new PlayerTabPagerAdapter(this));
        pager.setCurrentItem(1, false);
        pager.setUserInputEnabled(false);
        TabLayout tabs = view.findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            int text = position == 0
                    ? R.string.following_label
                    : R.string.for_you_label;
            tab.setText(text);
        }).attach();
    }

    public static PlayerTabsFragment newInstance() {
        return new PlayerTabsFragment();
    }

    private class PlayerTabPagerAdapter extends FragmentStateAdapter {

        public PlayerTabPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                if (mModel.isLoggedIn()) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(ClipDataSource.PARAM_FOLLOWING, true);
                    return PlayerSliderFragment.newInstance(bundle);
                }

                return LoginRequiredFragment.newInstance();
            }

            return PlayerSliderFragment.newInstance(null);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
