package com.spotify.recycler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.spotify.model.TopTracksUser;
import com.spotify.R;

import java.util.List;

public class TopTrackAdapter extends RecyclerView.Adapter<TopTrackAdapter.TrackViewHolder> {

    private Context context;
    private List<TopTracksUser.Item> trackList;
    private OnTrackClickListener onTrackClickListener;
    public interface OnTrackClickListener {
        void onTrackClick(String trackUri);
    }

    public TopTrackAdapter(Context context, List<TopTracksUser.Item> trackList, OnTrackClickListener listener) {
        this.context = context;
        this.trackList = trackList;
        this.onTrackClickListener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_top_tracks, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        TopTracksUser.Item track = trackList.get(position);
        holder.trackName.setText(track.getName());
        holder.trackArtist.setText(track.getArtists().get(0).getName());

        int durationInSeconds = track.getDuration_ms() / 1000;
        int minutes = durationInSeconds / 60;
        int seconds = durationInSeconds % 60;

        holder.trackDuration.setText(String.format("%d:%02d", minutes, seconds));
        holder.trackPopularity.setText("Popularity: " + track.getPopularity());

        Glide.with(context)
                .load(track.getAlbum().getImages().get(0).getUrl())
                .into(holder.trackImage);

        holder.itemView.setOnClickListener(v -> {
            if (onTrackClickListener != null) {
                onTrackClickListener.onTrackClick(track.getUri());
            }
        });
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView trackImage;
        TextView trackName;
        TextView trackArtist;
        TextView trackDuration;
        TextView trackPopularity;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            trackImage = itemView.findViewById(R.id.track_image);
            trackName = itemView.findViewById(R.id.track_name);
            trackArtist = itemView.findViewById(R.id.track_artist);
            trackDuration = itemView.findViewById(R.id.track_duration);
            trackPopularity = itemView.findViewById(R.id.track_popularity);
        }
    }
}
