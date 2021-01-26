package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;

public class LoginRequiredFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login_required, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.login)
                .setOnClickListener(v -> ((MainActivity)requireActivity()).showLoginSheet());
    }

    public static LoginRequiredFragment newInstance() {
        return new LoginRequiredFragment();
    }
}
