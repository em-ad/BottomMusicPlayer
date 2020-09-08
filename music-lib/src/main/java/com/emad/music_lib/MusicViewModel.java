package com.emad.music_lib;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class MusicViewModel extends ViewModel {

    private int currentIndex = -1;
    private MutableLiveData<ArrayList<Song>> allSongs = new MutableLiveData<>();
    private MutableLiveData<Song> playingSong = new MutableLiveData<>();

    public MutableLiveData<ArrayList<Song>> getAllSongs() {
        return allSongs;
    }

    public MutableLiveData<Song> getPlayingSong() {
        return playingSong;
    }

    public void repeatSong(){
        playingSong.postValue(playingSong.getValue());
    }

    public void nextSong(){
        if(allSongs.getValue() == null)
            return;
        if(allSongs.getValue().size() > currentIndex + 1)
            playingSong.postValue(allSongs.getValue().get(++currentIndex));
        else {
            currentIndex = 0;
            playingSong.postValue(allSongs.getValue().get(currentIndex));
        }
    }

    public void prevSong(){
        if(allSongs.getValue() == null)
            return;
        if(0 < currentIndex)
            playingSong.postValue(allSongs.getValue().get(--currentIndex));
        else {
            currentIndex = allSongs.getValue().size() - 1;
            playingSong.postValue(allSongs.getValue().get(currentIndex));
        }
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
