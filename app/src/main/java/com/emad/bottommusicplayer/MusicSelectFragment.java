package com.emad.bottommusicplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.emad.music_lib.MusicSelectorDialog;
import com.emad.music_lib.MusicViewModel;


public class MusicSelectFragment extends Fragment {
    public MusicSelectFragment() {
        // Required empty public constructor
    }

    public static MusicSelectFragment newInstance() {
        MusicSelectFragment fragment = new MusicSelectFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_music_selector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.tvSelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicSelectorDialog dialog = new MusicSelectorDialog(getActivity(), new ViewModelProvider(getActivity()).get(MusicViewModel.class));
                dialog.show();
            }
        });
        view.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getChildFragmentManager().beginTransaction().add(R.id.root, new MusicTestFragment(), null).commit();
            }
        });
    }
}