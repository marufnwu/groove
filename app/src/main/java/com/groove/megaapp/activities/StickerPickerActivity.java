package com.groove.megaapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Sticker;
import com.groove.megaapp.data.models.StickerSection;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.fragments.StickerPickerFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StickerPickerActivity extends AppCompatActivity {

    public static final String EXTRA_STICKER = "sticker";

    private static final String TAG = "StickerPickerActivity";

    private StickerPickerActivityViewModel mModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_picker);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.sticker_label);
        findViewById(R.id.header_more).setVisibility(View.GONE);
        View empty = findViewById(R.id.empty);
        View loading = findViewById(R.id.loading);
        mModel = new ViewModelProvider(this).get(StickerPickerActivityViewModel.class);
        mModel.sections.observe(this, this::setupTabs);
        mModel.state.observe(this, state -> {
            List<StickerSection> list = mModel.sections.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<StickerSection> list = mModel.sections.getValue();
        LoadingState state = mModel.state.getValue();
        if ((list == null || list.isEmpty()) && state != LoadingState.LOADING) {
            fetchSections();
        }
    }

    public void closeWithSelection(Sticker sticker) {
        Intent data = new Intent();
        data.putExtra(EXTRA_STICKER, sticker);
        setResult(RESULT_OK, data);
        finish();
    }
    
    private void fetchSections() {
        mModel.state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.stickerSectionsIndex(null, 1)
                .enqueue(new Callback<Wrappers.Paginated<StickerSection>>() {
                    
                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<StickerSection>> call,
                            @Nullable Response<Wrappers.Paginated<StickerSection>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Loading sticker sections from server returned " + code + ".");
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            List<StickerSection> sections = response.body().data;
                            mModel.sections.postValue(sections);
                            mModel.state.postValue(LoadingState.LOADED);
                        } else {
                            mModel.state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<StickerSection>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to load sticker sections from server.", t);
                        mModel.state.postValue(LoadingState.ERROR);
                    }
                });
    }

    private void setupTabs(List<StickerSection> sections) {
        StickerPagerAdapter adapter = new StickerPagerAdapter(this);
        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);
        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            StickerSection section = sections.get(position);
            tab.setText(section.name);
        }).attach();
    }

    private class StickerPagerAdapter extends FragmentStateAdapter {

        public StickerPagerAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            List<StickerSection> sections = mModel.sections.getValue();
            //noinspection ConstantConditions
            StickerSection section = sections.get(position);
            return StickerPickerFragment.newInstance(section.id);
        }

        @Override
        public int getItemCount() {
            List<StickerSection> sections = mModel.sections.getValue();
            //noinspection ConstantConditions
            return sections.size();
        }
    }

    public static class StickerPickerActivityViewModel extends ViewModel {

        public MutableLiveData<List<StickerSection>> sections = new MutableLiveData<>();
        public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);
    }
}
