package com.spotify.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.spotify.R;
import com.spotify.callback.PlaylistCallback;
import com.spotify.callback.TopTracksUserCallback;
import com.spotify.callback.UserCallback;
import com.spotify.model.Playlist;
import com.spotify.model.TopTracksUser;
import com.spotify.model.User;
import com.spotify.recycler.PlaylistAdapter;
import com.spotify.recycler.TopTrackAdapter;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import com.spotify.service.SpotifyService;
import com.spotify.service.SpotifyServiceImpl;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView recyclerViewPlaylist, recyclerViewTopTracks;
    private PlaylistAdapter adapterPlaylist;
    private TopTrackAdapter adapterTopTrack;
    private FloatingActionButton buttonShare;
    User me = new User();
    Playlist playlists = new Playlist();
    TopTracksUser topTracksUser = new TopTracksUser();
    private SpotifyService spotifyService;
    String mAccessToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylist);
        recyclerViewTopTracks = findViewById(R.id.recyclerViewTopTracks);

        recyclerViewPlaylist.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerViewTopTracks.setLayoutManager(new GridLayoutManager(this, 1));

        recyclerViewPlaylist.setAdapter(adapterPlaylist);
        recyclerViewTopTracks.setAdapter(adapterTopTrack);

        buttonShare = findViewById(R.id.btnPlayer);

        buttonShare.setOnClickListener(v -> {
        });

        spotifyService = new SpotifyServiceImpl();

        start();
    }

    public void start() {
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(LoginActivity.CLIENT_ID, AuthorizationResponse.Type.TOKEN, LoginActivity.REDIRECT_URI);

        builder.setScopes(new String[]{"streaming", "user-top-read", "app-remote-control"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, LoginActivity.REQUEST_CODE, request);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == LoginActivity.REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                case TOKEN:
                    mAccessToken = response.getAccessToken();
                    spotifyService.getMe(mAccessToken, new UserCallback() {
                        @Override
                        public void onUserReceived(User user) {
                            me = user;
                            spotifyService.getPlaylistsByUserId(mAccessToken, user.getId(), new PlaylistCallback() {
                                @Override
                                public void onPlaylistReceived(Playlist playlist) {
                                    playlists = playlist;
                                    Log.e("LoginActivity", playlist.toString());
                                    runOnUiThread(HomeActivity.this::adapterPlaylist);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        }
                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                        }
                    });

                    spotifyService.getTopTrackUser(mAccessToken, new TopTracksUserCallback() {
                        @Override
                        public void onTopTracksUserReceived(TopTracksUser topTrackUser) {
                            topTracksUser = topTrackUser;
                            runOnUiThread(HomeActivity.this::adapterTopTracks);
                        }

                        @Override
                        public void onFailure(Exception e) {

                        }
                    });
                    break;
                case ERROR:
                    break;
                default:
            }
        }
    }

    private void adapterPlaylist() {
        adapterPlaylist = new PlaylistAdapter(this, playlists.getItems());

        recyclerViewPlaylist.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerViewPlaylist.setAdapter(adapterPlaylist);
    }

    private void adapterTopTracks() {
        adapterTopTrack = new TopTrackAdapter(this, topTracksUser.getItems(), new TopTrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(String trackUri) {
                Log.d("TopTrackAdapter", "AAAAAAAAAAAA " + trackUri);
                Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);
                intent.putExtra("TRACK_URI", trackUri);
                startActivity(intent);
            }
        });

        recyclerViewTopTracks.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerViewTopTracks.setAdapter(adapterTopTrack);
    }
}