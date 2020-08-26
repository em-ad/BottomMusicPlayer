package com.emad.music_lib;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MusicActivity extends AppCompatActivity implements
        SongAdapter.SongAdapterListener,
        View.OnClickListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener, SeekBar.OnSeekBarChangeListener {

    private static Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    private List<Song> mSongList = new ArrayList<>();
    private RecyclerView mRecyclerViewSongs;
    private SongAdapter mAdapter;
    private CoordinatorLayout mCoordinatorLayout;
    private LinearLayout mMediaLayout;
    private TextView mTvTitle;

    private ImageView mIvArtwork;
    private ImageView mIvPlay;
    private ImageView mIvPrevious;
    private ImageView mIvNext;

    private ImageView img_repeat;
    private ImageView img_shuffle;

    private SeekBar songProgressBar;
    private MediaPlayer mMediaPlayer;
    private TextView mTvCurrentDuration;
    private TextView mTvTotalDuration;
    private TimeUtil timeUtil;
    private int currentSongIndex;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();
    private AudioManager mAudioManager;

    // repeat
    int count = 0; // initialise outside listener to prevent looping
    boolean repeat = false;
    boolean shuffle = false;



    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (mMediaPlayer == null) return;
            long totalDuration = mMediaPlayer.getDuration();
            long currentDuration = mMediaPlayer.getCurrentPosition();
            mTvTotalDuration.setText(String.format("%s", timeUtil.milliSecondsToTimer(totalDuration)));
            mTvCurrentDuration.setText(String.format("%s", timeUtil.milliSecondsToTimer(currentDuration)));
            int progress = (timeUtil.getProgressPercentage(currentDuration, totalDuration));
            songProgressBar.setProgress(progress);
            mHandler.postDelayed(this, 100);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        init();
        setUpAdapter();
        setUpListeners();
        getPermission();
        checkIncomingCalls();

        img_repeat.setAlpha(.5f);
        img_shuffle.setAlpha(.5f);

        img_repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (repeat) {
                    repeat = false;
                    img_repeat.setAlpha(.5f);
                } else {
                    repeat = true;
                    img_repeat.setAlpha(1f);
                    shuffle = false;
                    img_shuffle.setAlpha(.5f);
                }

            }
        });

        img_shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffle) {

                    shuffle = false;

                    img_shuffle.setAlpha(.5f);


                } else {
                    shuffle = true;
                    img_shuffle.setAlpha(1f);
                    repeat = false;
                    img_repeat.setAlpha(.5f);
                }
            }
        });


        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (repeat) {

                 /*   playSong(globalSong);
                    currentSongIndex = mSongList.indexOf(globalSong);*/
                    playSong(mSongList.get(currentSongIndex));
                } else if (shuffle) {
                    // shuffle
                    Random random = new Random();
                   currentSongIndex = random.nextInt((mSongList.size()-1)+1);
              //      currentSongIndex = random.nextInt(mSongList.size());
                    playSong(mSongList.get(currentSongIndex));

                } else {
                    // no repeat no shuffle
                    if (currentSongIndex < (mSongList.size() - 1)) {
                        playSong(mSongList.get(currentSongIndex + 1));
                        currentSongIndex = currentSongIndex + 1;
                    } else {
                        playSong(mSongList.get(0));
                        currentSongIndex = 0;
                    }
                }
            }
        });


        //  img_repeat.setAlpha(.5f);
      /*  img_repeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    img_repeat.setAlpha(1f);

                  *//*  mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            count++;
                            mMediaPlayer.seekTo(0);
                            mMediaPlayer.start();
                        }
                    });*//*

                } else {
                    img_repeat.setAlpha(.5f);

                }
            }
        });*/

    }


    private void checkIncomingCalls() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

    }

    void getPermission() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            getSongList();
                        } else
                            ActivityCompat.requestPermissions(MusicActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE},
                                    230);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode == 230){
            getPermission();
        }
    }

    private void init() {
        mMediaPlayer = new MediaPlayer();
        timeUtil = new TimeUtil();
        mRecyclerViewSongs = findViewById(R.id.recycler_view);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mMediaLayout = findViewById(R.id.layout_media);
        mIvArtwork = findViewById(R.id.iv_artwork);
        mIvPlay = findViewById(R.id.iv_play);
        mIvPrevious = findViewById(R.id.iv_previous);
        mIvNext = findViewById(R.id.iv_next);
        mTvTitle = findViewById(R.id.tv_title);
        mTvCurrentDuration = findViewById(R.id.songCurrentDurationLabel);
        mTvTotalDuration = findViewById(R.id.songTotalDurationLabel);
        songProgressBar = findViewById(R.id.songProgressBar);


        img_shuffle = findViewById(R.id.img_shuffle);
        img_repeat = findViewById(R.id.img_repeat);


    }


    private void setUpAdapter() {
        mAdapter = new SongAdapter(getApplicationContext(), mSongList, this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerViewSongs.setLayoutManager(mLayoutManager);
        mRecyclerViewSongs.setItemAnimator(new DefaultItemAnimator());
        mRecyclerViewSongs.setAdapter(mAdapter);
    }

    private void setUpListeners() {
        //       ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        //  new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerViewSongs);
        mIvPlay.setOnClickListener(this);
        mIvPrevious.setOnClickListener(this);
        mIvNext.setOnClickListener(this);
        songProgressBar.setOnSeekBarChangeListener(this);
        mMediaPlayer.setOnCompletionListener(this);
    }

    public void getSongList() {
        //retrieve item_song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int albumID = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songLink = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                Uri thisSongLink = Uri.parse(musicCursor.getString(songLink));
                long some = musicCursor.getLong(albumID);
                Uri uri = ContentUris.withAppendedId(sArtworkUri, some);
                mSongList.add(new Song(thisId, thisTitle, thisArtist, uri.toString(), thisSongLink.toString()));
            }
            while (musicCursor.moveToNext());
        }
        assert musicCursor != null;
        musicCursor.close();

        // Sort music alphabetically
        Collections.sort(mSongList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        mAdapter.notifyDataSetChanged();
    }


    public boolean deleteMusic(final File file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{file.getAbsolutePath()};
        final ContentResolver contentResolver = MusicActivity.this.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        contentResolver.delete(filesUri, where, selectionArgs);
        if (file.exists()) {
            contentResolver.delete(filesUri, where, selectionArgs);
        }
        return !file.exists();
    }


    @Override
    public void onSongSelected(Song song) {
        playSong(song);
        currentSongIndex = mSongList.indexOf(song);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_play) {
            playMusic();
        } else if (id == R.id.iv_previous) {
            playPreviousSong();
        } else if (id == R.id.iv_next) {
            playNextSong();
        }
    }

    private void playMusic() {
        if (!mMediaPlayer.isPlaying()) {
            mIvPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            mMediaPlayer.start();
        } else {
            mIvPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
            mMediaPlayer.pause();
        }
    }

    public void playSong(Song song) {
        try {
            mMediaPlayer.reset();
      /*      @Deprecated
      * use this
      *            mp.setAudioAttributes(new AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            */

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(song.getSongLink()));
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            // Displaying Song title
            //      isPlaying = true;
            mIvPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            mMediaLayout.setVisibility(View.VISIBLE);
            mTvTitle.setText(song.getTitle());
            Glide.with(this).load(song.getThumbnail()).placeholder(R.drawable.play).error(R.drawable.play).centerCrop().into(mIvArtwork);
            // set Progress bar values
            songProgressBar.setProgress(0);
            songProgressBar.setMax(100);

            songProgressBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            songProgressBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

            // Updating progress bar
            updateProgressBar();
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    private void playNextSong() {
        if (currentSongIndex < (mSongList.size() - 1)) {
            Song song = mSongList.get(currentSongIndex + 1);
            playSong(song);
            currentSongIndex = currentSongIndex + 1;
        } else {
            playSong(mSongList.get(0));
            currentSongIndex = 0;
        }
    }

    private void playPreviousSong() {
        if (currentSongIndex > 0) {
            Song song = mSongList.get(currentSongIndex - 1);
            playSong(song);
            currentSongIndex = currentSongIndex - 1;
        } else {
            Song song = mSongList.get(mSongList.size() - 1);
            playSong(song);
            currentSongIndex = mSongList.size() - 1;
        }
    }


    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        // handling calls
        if (focusChange <= 0) {
            //LOSS -> PAUSE
            mMediaPlayer.pause();
        } else {
            //GAIN -> PLAY
            mMediaPlayer.start();
        }
    }


    // auto go to the next song
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (currentSongIndex < (mSongList.size() - 1)) {
            playSong(mSongList.get(currentSongIndex + 1));
            currentSongIndex = currentSongIndex + 1;
        } else {
            playSong(mSongList.get(0));
            currentSongIndex = 0;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mMediaPlayer.getDuration();
        int currentPosition = timeUtil.progressToTimer(seekBar.getProgress(), totalDuration);
        mMediaPlayer.seekTo(currentPosition);
        updateProgressBar();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mAudioManager.abandonAudioFocus(this);
        /**AudioManager.abandonAudioFocus(this);
         * Added in API level 8
         * Deprecated in API level 26
         * use abandonAudioFocusRequest(android.media.AudioFocusRequest)
         */
        if (mMediaPlayer.isPlaying())
            mMediaPlayer.pause();

    }


}
