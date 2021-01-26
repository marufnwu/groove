package com.groove.megaapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.data.models.User;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class LanguageActivity extends AppCompatActivity {

    public static final String EXTRA_SPLASH = "splash";
    public static final String EXTRA_USER = "user";

    private boolean mFirstLaunch;
    private LanguageActivityViewModel mModel;
    private User mUser;

    @Override
    public void onBackPressed() {
        if (mFirstLaunch) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_USER, mUser);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        super.onBackPressed();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        findViewById(R.id.header_back).setVisibility(View.GONE);
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.language_label);
        ImageButton done = findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(view -> commitSelection());
        mFirstLaunch = getIntent().getBooleanExtra(EXTRA_SPLASH, false);
        mUser = getIntent().getParcelableExtra(EXTRA_USER);
        mModel = new ViewModelProvider(this).get(LanguageActivityViewModel.class);
        String[] codes = getResources().getStringArray(R.array.language_codes);
        int[] colors = getResources().getIntArray(R.array.language_colors);
        String[] names = getResources().getStringArray(R.array.language_names);
        List<Language> options = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            Language language = new Language();
            language.code = codes[i];
            language.color = colors[i];
            language.name = names[i];
            language.selected = mModel.languages.contains(codes[i]);
            options.add(language);
        }

        LanguageAdapter adapter = new LanguageAdapter(options);
        RecyclerView languages = findViewById(R.id.languages);
        languages.setAdapter(adapter);
        languages.setLayoutManager(new GridLayoutManager(this, 3));
        OverScrollDecoratorHelper.setUpOverScroll(
                languages, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
    }

    private void commitSelection() {
        Prefs.putStringSet(SharedConstants.PREF_PREFERRED_LANGUAGES, mModel.languages);
        if (mFirstLaunch) {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }

    private static class Language {

        public int color;
        public String code;
        public String name;

        public boolean selected;
    }

    public static class LanguageActivityViewModel extends ViewModel {

        public LanguageActivityViewModel() {
            Set<String> selected = Prefs.getStringSet(SharedConstants.PREF_PREFERRED_LANGUAGES, null);
            if (selected == null) {
                selected = new HashSet<>();
            }

            languages = selected;
        }

        public final Set<String> languages;
    }

    private class LanguageAdapter extends RecyclerView.Adapter<LanguageViewHolder> {

        private final List<Language> mLanguages;

        public LanguageAdapter(List<Language> languages) {
            mLanguages = languages;
        }

        @Override
        public int getItemCount() {
            return mLanguages.size();
        }

        @Override
        public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
            Language language = mLanguages.get(position);
            holder.card.setCardBackgroundColor(language.color);
            holder.name.setText(language.name);
            holder.check.setVisibility(
                    mModel.languages.contains(language.code) ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (mModel.languages.contains(language.code)) {
                    mModel.languages.remove(language.code);
                    holder.check.setVisibility(View.GONE);
                } else {
                    mModel.languages.add(language.code);
                    holder.check.setVisibility(View.VISIBLE);
                }
            });
        }

        @NonNull
        @Override
        public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(LanguageActivity.this)
                    .inflate(R.layout.item_language, parent, false);
            return new LanguageViewHolder(root);
        }
    }

    private static class LanguageViewHolder extends RecyclerView.ViewHolder {

        public MaterialCardView card;
        public TextView name;
        public View check;

        public LanguageViewHolder(@NonNull View root) {
            super(root);
            card = (MaterialCardView)root;
            name = root.findViewById(R.id.name);
            check = root.findViewById(R.id.check);
        }
    }
}
