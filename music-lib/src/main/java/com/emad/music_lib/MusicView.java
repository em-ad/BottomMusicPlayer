package com.emad.music_lib;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

class MusicView extends FrameLayout implements
        View.OnClickListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener, SeekBar.OnSeekBarChangeListener {

    private static Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

    private LinearLayout mMediaLayout;
    private TextView mTvTitle;
    private ImageView mIvArtwork;
    private ImageView mIvPlay;
    private ImageView mIvPrevious;
    private ImageView mIvNext;
    private ImageView img_repeat;
    private ImageView img_shuffle;
    private SeekBar songProgressBar;
    private static MediaPlayer mMediaPlayer;
    private TextView mTvCurrentDuration;
    private TextView mTvTotalDuration;
    private TimeUtil timeUtil;
    private Handler mHandler = new Handler();
    private AudioManager mAudioManager;
    private MusicSelectorDialog dialog;

    private PermissionCallback callback;
    private Context context;
    private int currentSongIndex = -1;
    private int count = 0; // initialise outside listener to prevent looping
    private boolean repeat = false;
    private boolean shuffle = false;
    private static MusicViewModel viewModel;

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

    public MusicView(@NonNull Context context, PermissionCallback callback) {
        super(context);
        this.context = context;
        this.callback = callback;
        init(context);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context);
    }

    private void init(final Context context) {
        final ViewGroup viewConversation = (ViewGroup) inflate(context, R.layout.frame_music, this);
        viewConversation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = new MusicSelectorDialog(context, viewModel);
                dialog.show();
            }
        });
        try {
            viewModel = new ViewModelProvider(((AppCompatActivity) context)).get(MusicViewModel.class);
        } catch (Exception e) {
            Log.e("tag", "init: " + e.getMessage());
            return;
        }
        initViews(viewConversation);
        setUpListeners();
        if (checkPermission(context))
            getSongList(context);
        checkIncomingCalls(context);

        viewModel.getPlayingSong().observeForever(new Observer<Song>() {
            @Override
            public void onChanged(Song song) {
                playSong(song);
                if (viewModel.getAllSongs().getValue() != null)
                    viewModel.setCurrentIndex(viewModel.getAllSongs().getValue().indexOf(song));
            }
        });

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
                    viewModel.repeatSong();
                } else if (shuffle) {
//                    Random random = new Random();
//                    currentSongIndex = random.nextInt((mSongList.size() - 1) + 1);
//                    playSong(mSongList.get(currentSongIndex));
                    viewModel.nextSong();
                } else {
                    viewModel.nextSong();
                }
            }
        });
    }

    private void checkIncomingCalls(Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    boolean checkPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void initViews(ViewGroup viewConversation) {
        mMediaPlayer = new MediaPlayer();
        try{
        mMediaPlayer.stop();}
        catch (Exception e){
            Log.e("tag", "initViews: " + e.getMessage() );
        }
        timeUtil = new TimeUtil();

        mMediaLayout = viewConversation.findViewById(R.id.layout_media);
        mIvArtwork = viewConversation.findViewById(R.id.iv_artwork);
        mIvPlay = viewConversation.findViewById(R.id.iv_play);
        mIvPrevious = viewConversation.findViewById(R.id.iv_previous);
        mIvNext = viewConversation.findViewById(R.id.iv_next);
        mTvTitle = viewConversation.findViewById(R.id.tv_title);
        mTvCurrentDuration = viewConversation.findViewById(R.id.songCurrentDurationLabel);
        mTvTotalDuration = viewConversation.findViewById(R.id.songTotalDurationLabel);
        songProgressBar = viewConversation.findViewById(R.id.songProgressBar);
        img_shuffle = viewConversation.findViewById(R.id.img_shuffle);
        img_repeat = viewConversation.findViewById(R.id.img_repeat);


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

    public void getSongList(Context context) {
        ArrayList<Song> mSongList = new ArrayList<>();
        //retrieve item_song info
        ContentResolver musicResolver = context.getContentResolver();
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
        viewModel.getAllSongs().postValue(mSongList);
    }


    public boolean deleteMusic(final File file, Context context) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{file.getAbsolutePath()};
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        contentResolver.delete(filesUri, where, selectionArgs);
        if (file.exists()) {
            contentResolver.delete(filesUri, where, selectionArgs);
        }
        return !file.exists();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_play) {
            if (viewModel.getPlayingSong().getValue() != null)
                playMusic();
        } else if (id == R.id.iv_previous) {
            viewModel.prevSong();
        } else if (id == R.id.iv_next) {
            viewModel.nextSong();
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
        mIvPlay.setVisibility(VISIBLE);
        mIvNext.setVisibility(VISIBLE);
        mIvPrevious.setVisibility(VISIBLE);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(context, Uri.parse(song.getSongLink()));
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            // Displaying Song title
            //      isPlaying = true;
            mIvPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            mMediaLayout.setVisibility(View.VISIBLE);
            mTvTitle.setText(song.getTitle());
            Glide.with(this).load(song.getThumbnail()).circleCrop().placeholder(R.drawable.play).error(R.drawable.play).into(mIvArtwork);
            // set Progress bar values
            songProgressBar.setProgress(0);
            songProgressBar.setMax(100);

            songProgressBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            songProgressBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

            // Updating progress bar
            updateProgressBar();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("tag", "Play ERROR: " + e.getMessage());
        }
    }

    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (currentSongIndex == -1)
            return;
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
        viewModel.nextSong();
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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMediaPlayer != null && mMediaPlayer.isPlaying())
            mMediaPlayer.stop();
    }
}
