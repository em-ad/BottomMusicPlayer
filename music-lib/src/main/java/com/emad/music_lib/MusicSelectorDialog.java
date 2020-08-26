package com.emad.music_lib;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class MusicSelectorDialog extends Dialog
implements SongAdapter.SongAdapterListener {

    private RecyclerView mRecyclerViewSongs;
    private SongAdapter mAdapter;
    private MusicViewModel viewModel;

    public MusicSelectorDialog(@NonNull Context context, MusicViewModel viewModel) {
        super(context);
        this.viewModel = viewModel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_selector);
        mRecyclerViewSongs = findViewById(R.id.recycler_view);
        setUpAdapter(getContext());
        viewModel.getAllSongs().observeForever(new Observer<ArrayList<Song>>() {
            @Override
            public void onChanged(ArrayList<Song> songs) {
                mAdapter.setData(songs);
            }
        });
    }

    private void setUpAdapter(Context context) {
        mAdapter = new SongAdapter(context, viewModel.getAllSongs().getValue(), this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        mRecyclerViewSongs.setLayoutManager(mLayoutManager);
        mRecyclerViewSongs.setItemAnimator(new DefaultItemAnimator());
        mRecyclerViewSongs.setAdapter(mAdapter);
    }

    @Override
    public void onSongSelected(Song song) {
        viewModel.getPlayingSong().postValue(song);
        dismiss();
    }
}
