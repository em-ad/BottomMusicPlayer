package com.emad.music_lib;

import java.io.Serializable;

public class Song implements Serializable {

    private long id;
    private String title;
    private String artist;
    private String thumbnail;
    private String songLink;

    public Song() {
    }

    public Song(long songID, String songTitle, String songArtist, String thumbNail, String link) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        thumbnail = thumbNail;
        songLink = link;
    }

    public long getID() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getSongLink() {
        return songLink;
    }


}
