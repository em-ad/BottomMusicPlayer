package com.emad.music_lib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.MyViewHolder>   {

    private List<Song> mSongList;
    private List<Song> mSongListFiltered;
    private Context mContext;
    private SongAdapterListener listener;


    public SongAdapter(Context context, List<Song> songList, SongAdapterListener listener) {
        this.mContext = context;
        this.listener = listener;
        this.mSongList = songList;
        this.mSongListFiltered = songList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Song song = mSongListFiltered.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        Glide.with(mContext).load(song.getThumbnail()).placeholder(R.drawable.play).error(R.drawable.play).centerCrop().into(holder.thumbnail);
    }

    @Override
    public int getItemCount() {
        return mSongListFiltered == null ? 0 : mSongListFiltered.size();
    }

    public void setData(List<Song> mSongList) {
        this.mSongList = mSongList;
        this.mSongListFiltered = mSongList;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        mSongList.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(Song item, int position) {
        mSongList.add(position, item);
        // notify item added by position
        notifyItemInserted(position);
    }



    public interface SongAdapterListener {
        void onSongSelected(Song song);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title, artist;
        public RelativeLayout viewBackground, viewForeground;
        public ImageView thumbnail;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.song_title);
            artist = view.findViewById(R.id.song_artist);
            viewBackground = view.findViewById(R.id.view_background);
            viewForeground = view.findViewById(R.id.view_foreground);
            thumbnail = view.findViewById(R.id.thumbnail);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // send selected contact in callback
                    listener.onSongSelected(mSongListFiltered.get(getAdapterPosition()));

                }
            });
        }
    }

}
