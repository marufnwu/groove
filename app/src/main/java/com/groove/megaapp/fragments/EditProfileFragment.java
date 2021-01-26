package com.groove.megaapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding4.widget.RxTextView;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.disposables.Disposable;
import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.utils.AutocompleteUtil;
import com.groove.megaapp.utils.SocialSpanUtil;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private final List<Disposable> mDisposables = new ArrayList<>();
    private ProfileEditFragmentModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri uri = result.getUri();
            Log.v(TAG, "Copped image as saved to " + uri);
            mModel1.photo = uri.getPath();
            refreshPhoto();
        } else if (requestCode == SharedConstants.REQUEST_CODE_PICK_LOCATION && resultCode == Activity.RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            setLocation(place);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel1 = new ViewModelProvider(this).get(ProfileEditFragmentModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
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
    public void onResume() {
        super.onResume();
        LoadingState state = mModel1.state.getValue();
        User user = mModel1.user.getValue();
        if (user == null && state != LoadingState.LOADING) {
            loadUser();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton close = view.findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.edit_label);
        ImageButton done = view.findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(v -> saveProfile());
        SimpleDraweeView photo = view.findViewById(R.id.photo);
        photo.setOnClickListener(v -> choosePhotoAction());
        Disposable disposable;
        TextInputLayout location = view.findViewById(R.id.location);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(location.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    if (TextUtils.isEmpty(e.getEditable())) {
                        mModel1.location = null;
                        mModel1.latitude = null;
                        mModel1.longitude = null;
                    }
                });
        mDisposables.add(disposable);
        if (!getResources().getBoolean(R.bool.locations_enabled)) {
            location.setVisibility(View.GONE);
        }

        location.setEndIconOnClickListener(v -> pickLocation());
        TextInputLayout name = view.findViewById(R.id.name);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(name.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.name = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout username = view.findViewById(R.id.username);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(username.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.username = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout email = view.findViewById(R.id.email);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(email.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.email = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout phone = view.findViewById(R.id.phone);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(phone.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.phone = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout bio = view.findViewById(R.id.bio);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(bio.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.bio = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout facebook = view.findViewById(R.id.facebook);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(facebook.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.facebook = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout instagram = view.findViewById(R.id.instagram);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(instagram.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.instagram = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout linkedin = view.findViewById(R.id.linkedin);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(linkedin.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.linkedin = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout snapchat = view.findViewById(R.id.snapchat);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(snapchat.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.snapchat = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout tiktok = view.findViewById(R.id.tiktok);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(tiktok.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.tiktok = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout twitter = view.findViewById(R.id.twitter);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(twitter.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.twitter = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        TextInputLayout youtube = view.findViewById(R.id.youtube);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(youtube.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel1.youtube = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        View content = view.findViewById(R.id.content);
        View loading = view.findViewById(R.id.loading);
        mModel1.errors.observe(getViewLifecycleOwner(), errors -> {
            location.setError(null);
            name.setError(null);
            username.setError(null);
            email.setError(null);
            phone.setError(null);
            bio.setError(null);
            if (errors == null) {
                return;
            }
            if (errors.containsKey("location")) {
                location.setError(errors.get("location"));
            }
            if (errors.containsKey("name")) {
                name.setError(errors.get("name"));
            }
            if (errors.containsKey("username")) {
                username.setError(errors.get("username"));
            }
            if (errors.containsKey("email")) {
                email.setError(errors.get("email"));
            }
            if (errors.containsKey("phone")) {
                phone.setError(errors.get("phone"));
            }
            if (errors.containsKey("bio")) {
                bio.setError(errors.get("bio"));
            }
        });
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
            content.setVisibility(state == LoadingState.LOADED ? View.VISIBLE : View.GONE);
        });
        mModel1.user.observe(getViewLifecycleOwner(), user -> {
            location.getEditText().setText(mModel1.location = user.location);
            mModel1.latitude = user.latitude;
            mModel1.longitude = user.longitude;
            name.getEditText().setText(mModel1.name = user.name);
            username.getEditText().setText(mModel1.username = user.username);
            email.getEditText().setText(mModel1.email = user.email);
            phone.getEditText().setText(mModel1.phone = user.phone);
            bio.getEditText().setText(mModel1.bio = user.bio);
            User.UserLink facebook2 = findLink("facebook", user.links);
            if (facebook2 != null) {
                facebook.getEditText().setText(mModel1.facebook = facebook2.url);
            } else {
                facebook.getEditText().setText(mModel1.facebook = null);
            }

            User.UserLink instagram2 = findLink("instagram", user.links);
            if (instagram2 != null) {
                instagram.getEditText().setText(mModel1.instagram = instagram2.url);
            } else {
                instagram.getEditText().setText(mModel1.instagram = null);
            }

            User.UserLink linkedin2 = findLink("linkedin", user.links);
            if (linkedin2 != null) {
                linkedin.getEditText().setText(mModel1.linkedin = linkedin2.url);
            } else {
                linkedin.getEditText().setText(mModel1.linkedin = null);
            }

            User.UserLink snapchat2 = findLink("snapchat", user.links);
            if (snapchat2 != null) {
                snapchat.getEditText().setText(mModel1.snapchat = snapchat2.url);
            } else {
                snapchat.getEditText().setText(mModel1.snapchat = null);
            }

            User.UserLink tiktok2 = findLink("tiktok", user.links);
            if (tiktok2 != null) {
                tiktok.getEditText().setText(mModel1.tiktok = tiktok2.url);
            } else {
                tiktok.getEditText().setText(mModel1.tiktok = null);
            }

            User.UserLink twitter2 = findLink("twitter", user.links);
            if (twitter2 != null) {
                twitter.getEditText().setText(mModel1.twitter = twitter2.url);
            } else {
                twitter.getEditText().setText(mModel1.twitter = null);
            }

            User.UserLink youtube2 = findLink("youtube", user.links);
            if (youtube2 != null) {
                youtube.getEditText().setText(mModel1.youtube = youtube2.url);
            } else {
                youtube.getEditText().setText(mModel1.youtube = null);
            }

            refreshPhoto();
        });
        facebook.setVisibility(
                getResources().getBoolean(R.bool.profile_link_facebook)
                ? View.VISIBLE : View.GONE
        );
        instagram.setVisibility(
                getResources().getBoolean(R.bool.profile_link_instagram)
                ? View.VISIBLE : View.GONE
        );
        linkedin.setVisibility(
                getResources().getBoolean(R.bool.profile_link_linkedin)
                ? View.VISIBLE : View.GONE
        );
        snapchat.setVisibility(
                getResources().getBoolean(R.bool.profile_link_snapchat)
                ? View.VISIBLE : View.GONE
        );
        tiktok.setVisibility(
                getResources().getBoolean(R.bool.profile_link_tiktok)
                ? View.VISIBLE : View.GONE
        );
        twitter.setVisibility(
                getResources().getBoolean(R.bool.profile_link_twitter)
                ? View.VISIBLE : View.GONE
        );
        youtube.setVisibility(
                getResources().getBoolean(R.bool.profile_link_youtube)
                ? View.VISIBLE : View.GONE
        );
        EditText input = bio.getEditText();
        SocialSpanUtil.apply(input, mModel1.bio, null);
        if (getResources().getBoolean(R.bool.autocomplete_enabled)) {
            AutocompleteUtil.setupForHashtags(requireContext(), input);
            AutocompleteUtil.setupForUsers(requireContext(), input);
        }
    }

    private void choosePhotoAction() {
        new MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.photo_options, (dialogInterface, i) -> {
                    if (i == 0) {
                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setMinCropResultSize(256,256)
                                .start(requireContext(), EditProfileFragment.this);
                    } else {
                        removePhoto();
                    }
                })
                .show();
    }

    @Nullable
    private static User.UserLink findLink(String type, List<User.UserLink> links) {
        if (links != null) {
            for (User.UserLink link : links) {
                if (TextUtils.equals(link.type, type)) {
                    return link;
                }
            }
        }

        return null;
    }

    private void loadUser() {
        mModel1.state.setValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.profileShow()
                .enqueue(new Callback<Wrappers.Single<User>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Response<Wrappers.Single<User>> response
                    ) {
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            User user = response.body().data;
                            mModel1.user.setValue(user);
                            mModel1.state.setValue(LoadingState.LOADED);
                        } else {
                            mModel1.state.setValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to fetch profile.", t);
                        mModel1.state.setValue(LoadingState.ERROR);
                    }
                });
    }

    public static EditProfileFragment newInstance() {
        return new EditProfileFragment();
    }

    private void pickLocation() {
        List<Place.Field> fields =
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setTypeFilter(TypeFilter.CITIES)
                .build(requireContext());
        startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_LOCATION);
    }

    private void removePhoto() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.profilePhotoDelete()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        if (response != null && response.isSuccessful()) {
                            mModel1.photo = null;
                            refreshPhoto();
                        }
                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        progress.dismiss();
                    }
                });
    }

    private void saveProfile() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel1.errors.postValue(null);
        REST rest = MainApplication.getContainer().get(REST.class);
        MultipartBody.Part photo = null;
        if (!TextUtils.isEmpty(mModel1.photo)) {
            RequestBody body = RequestBody.create(new File(mModel1.photo), null);
            photo = MultipartBody.Part.createFormData("photo", "photo.png", body);
        }

        List<User.UserLink> links = new ArrayList<>();
        if (!TextUtils.isEmpty(mModel1.facebook)) {
            User.UserLink link = new User.UserLink();
            link.type = "facebook";
            link.url = mModel1.facebook;
            links.add(link);
        }

        if (!TextUtils.isEmpty(mModel1.instagram)) {
            User.UserLink link = new User.UserLink();
            link.type = "instagram";
            link.url = mModel1.instagram;
            links.add(link);
        }

        if (!TextUtils.isEmpty(mModel1.linkedin)) {
            User.UserLink link = new User.UserLink();
            link.type = "linkedin";
            link.url = mModel1.linkedin;
            links.add(link);
        }

        if (!TextUtils.isEmpty(mModel1.snapchat)) {
            User.UserLink link = new User.UserLink();
            link.type = "snapchat";
            link.url = mModel1.snapchat;
            links.add(link);
        }

        if (!TextUtils.isEmpty(mModel1.tiktok)) {
            User.UserLink link = new User.UserLink();
            link.type = "tiktok";
            link.url = mModel1.tiktok;
            links.add(link);
        }

        if (!TextUtils.isEmpty(mModel1.twitter)) {
            User.UserLink link = new User.UserLink();
            link.type = "twitter";
            link.url = mModel1.twitter;
            links.add(link);
        }

        if (!TextUtils.isEmpty(mModel1.youtube)) {
            User.UserLink link = new User.UserLink();
            link.type = "youtube";
            link.url = mModel1.youtube;
            links.add(link);
        }

        Map<String, RequestBody> extras = new HashMap<>();
        if (links.isEmpty()) {
            extras.put("links", RequestBody.create("", null));
        } else {
            for (int i = 0; i < links.size(); i++) {
                extras.put("links[" + i + "][type]", RequestBody.create(links.get(i).type, null));
                extras.put("links[" + i + "][url]", RequestBody.create(links.get(i).url, null));
            }
        }

        Call<ResponseBody> call = rest.profileUpdate(
                photo,
                RequestBody.create(mModel1.username, null),
                mModel1.bio != null ? RequestBody.create(mModel1.bio, null) : null,
                RequestBody.create(mModel1.name, null),
                mModel1.email != null ? RequestBody.create(mModel1.email, null) : null,
                RequestBody.create(mModel1.phone, null),
                mModel1.location != null ? RequestBody.create(mModel1.location, null) : null,
                mModel1.latitude != null ? RequestBody.create(mModel1.latitude + "", null) : null,
                mModel1.longitude != null ? RequestBody.create(mModel1.longitude + "", null) : null,
                extras
        );
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response
            ) {
                if (response != null) {
                    if (response.isSuccessful()) {
                        mModel2.isProfileInvalid = true;
                        ((MainActivity)requireActivity()).popBackStack();
                    } else if (response.code() == 422) {
                        try {
                            //noinspection ConstantConditions
                            String content = response.errorBody().string();
                            showErrors(new JSONObject(content));
                        } catch (Exception ignore) {
                        }
                    }
                }
                progress.dismiss();
            }

            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when trying to update profile.", t);
                progress.dismiss();
            }
        });
    }

    private void refreshPhoto() {
        User user = mModel1.user.getValue();
        //noinspection ConstantConditions
        SimpleDraweeView photo = getView().findViewById(R.id.photo);
        if (!TextUtils.isEmpty(mModel1.photo)) {
            photo.setImageURI(Uri.fromFile(new File(mModel1.photo)));
        } else if (user != null && !TextUtils.isEmpty(user.photo)) {
            photo.setImageURI(user.photo);
        } else {
            photo.setActualImageResource(R.drawable.photo_placeholder);
        }
    }

    private void setLocation(Place place) {
        Log.v(TAG, "User chose " + place.getId() + " place.");
        mModel1.location = place.getName();
        mModel1.latitude = place.getLatLng().latitude;
        mModel1.longitude = place.getLatLng().longitude;
        TextInputLayout location = getView().findViewById(R.id.location);
        location.getEditText().setText(mModel1.location);
    }

    private void showErrors(JSONObject json) throws Exception {
        JSONObject errors = json.getJSONObject("errors");
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{"username", "bio", "name", "email", "phone", "location", "latitude", "longitude"};
        for (String key : keys) {
            JSONArray fields = errors.optJSONArray(key);
            if (fields != null) {
                messages.put(key, fields.getString(0));
            }
        }

        mModel1.errors.postValue(messages);
    }

    public static class ProfileEditFragmentModel extends ViewModel {

        public String photo;
        public String username;
        public String bio;
        public String name;
        public String email;
        public String phone;

        public String facebook;
        public String instagram;
        public String linkedin;
        public String snapchat;
        public String tiktok;
        public String twitter;
        public String youtube;

        public String location;
        public Double latitude;
        public Double longitude;

        public final MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();
        public final MutableLiveData<LoadingState> state = new MutableLiveData<>();
        public final MutableLiveData<User> user = new MutableLiveData<>();
    }
}
