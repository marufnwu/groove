package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.models.Challenge;

public class ChallengeFragment extends Fragment {

    public static String ARG_CHALLENGE = "challenge";

    private Challenge mChallenge;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChallenge = requireArguments().getParcelable(ARG_CHALLENGE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_challenge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SimpleDraweeView image = view.findViewById(R.id.image);
        image.setImageURI(mChallenge.image);
        image.setOnClickListener(v -> {
            ArrayList<String> hashtags = new ArrayList<>();
            hashtags.add(mChallenge.hashtag);
            Bundle params = new Bundle();
            params.putStringArrayList(ClipDataSource.PARAM_HASHTAGS, hashtags);
            ((MainActivity) requireActivity()).showClips("#" + mChallenge.hashtag, params);
        });
    }

    public static ChallengeFragment newInstance(Challenge challenge) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_CHALLENGE, challenge);
        ChallengeFragment fragment = new ChallengeFragment();
        fragment.setArguments(arguments);
        return fragment;
    }
}
