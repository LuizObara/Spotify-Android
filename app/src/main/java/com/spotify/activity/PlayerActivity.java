package com.spotify.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;

import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.spotify.R;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.callback.LongCallback;
import com.spotify.service.PlayerService;
import com.spotify.service.PlayerServiceImpl;

import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerActivity extends AppCompatActivity {
    private boolean isPlaying = true;
    private PlayerService playerService;
    private ImageView imageTrack;
    private TextView textTrack, textArtist, textDurationState, textDurationMax;
    private SeekBar barTrack;
    private FloatingActionButton buttonPrevious, buttonPlayPause, buttonSkip, buttonRandom, buttonRepeat, buttonHome;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String trackUri = getIntent().getStringExtra("TRACK_URI");

        buttonSkip      = findViewById(R.id.buttonSkip     );
        buttonRandom    = findViewById(R.id.buttonRandom   );
        buttonRepeat    = findViewById(R.id.buttonRepeat   );
        buttonPrevious  = findViewById(R.id.buttonPrevious );
        buttonPlayPause = findViewById(R.id.buttonPlayPause);

        buttonHome      = findViewById(R.id.btnHome);

        barTrack = findViewById(R.id.seekBarMusic);

        textTrack         = findViewById(R.id.textTrack        );
        textArtist        = findViewById(R.id.textArtist       );
        textDurationMax   = findViewById(R.id.textDurationMax  );
        textDurationState = findViewById(R.id.textDurationState);

        imageTrack = findViewById(R.id.imageTrack);

        buttonHome.setOnClickListener(v -> {
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
            finish();
        });

        start(trackUri);
    }

    protected void onStart() {
        super.onStart();
    }

    public void start(String uriToPlay) {
        SpotifyAppRemote.connect(this, LoginActivity.connectionParams,
            new Connector.ConnectionListener() {
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {

                    playerService = new PlayerServiceImpl(spotifyAppRemote);

                    playerService.play(uriToPlay);

                    urlToImage(imageTrack);
                    trackToTextViewTrack(textTrack);
                    trackToTextViewArtist(textArtist);
                    switchImageButton(isPlaying);
                    trackDuration(textDurationMax);
                    updateCurrentPosition(textDurationState);
                    seekBarProgress(barTrack);

                    buttonSkip.setOnClickListener(v -> {
                        playerService.skipToNext();
                        switchImageButton(isPlaying = true);
                    });

                    buttonPrevious.setOnClickListener(v -> {
                        playerService.skipToPrevious();
                        switchImageButton(isPlaying = true);
                    });

                    buttonPlayPause.setOnClickListener(v -> switchImageButton(isPlaying = playerService.playPause(isPlaying)));

                    buttonRandom.setOnClickListener(v -> playerService.shuffle());

                    buttonRepeat.setOnClickListener(v -> playerService.repeat());

                    playerService.getDuration(duration -> barTrack.setMax(duration.intValue()));

                    barTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser){
                                updateCurrentPosition(textDurationState, progress);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            int progress = seekBar.getProgress();
                            playerService.seekTo((long) progress);
                            seekBar.setProgress(progress);
                        }
                    });

                    new Timer().scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            seekBarProgress(barTrack);
                        }
                    }, 0, 900);

                }
                public void onFailure(Throwable throwable) {
                    Log.e("MyActivity", throwable.getMessage(), throwable);
                }
            });
    }

    private void trackToTextViewTrack(TextView textTrack) {
        playerService.getNameTrack(nameTrack -> textTrack.setText(nameTrack));
    }

    private void trackToTextViewArtist(TextView textArtist) {
        playerService.getNameArtist(nameTrack -> textArtist.setText(nameTrack));
    }

    private void urlToImage(ImageView imageView) {
        playerService.getImage(imageUri -> {
            String image = extractImageUrl(imageUri);
            downloadImage(image, imageView);
        });
    }

    private String extractImageUrl(String imageUri) {
        String imageId = imageUri.substring(imageUri.lastIndexOf(":") + 1, imageUri.length() - 2);
        return "https://i.scdn.co/image/" + imageId;
    }

    private void downloadImage(String imageUri, ImageView imageTrack) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUri);
                final Bitmap bmp;
                bmp = BitmapFactory
                        .decodeStream(url.openConnection()
                                .getInputStream());
                new Handler(Looper.getMainLooper()).post(() -> {
                    imageTrack.setImageBitmap(bmp);
                });
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void switchImageButton(boolean isPlaying) {
        if (isPlaying){
            buttonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        }else {
            buttonPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void trackDuration(TextView duration) {
        playerService.getDuration(l -> {
            long totalSeconds = l / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            String formattedTime = String.format("%02d:%02d", minutes, seconds);
            duration.setText(formattedTime);
        });
    }

    private void updateCurrentPosition(TextView position) {
        playerService.getCurrentPlaybackPosition(new LongCallback() {
            @Override
            public void onLongReceived(Long playbackPosition) {
                long currentPositionSeconds = playbackPosition / 1000;
                long minutes = currentPositionSeconds / 60;
                long seconds = currentPositionSeconds % 60;
                String formattedTime = String.format("%02d:%02d", minutes, seconds);
                position.setText(formattedTime);
            }
        });
    }

    private void seekBarProgress(SeekBar barTrack) {
        playerService.getCurrentPlaybackPosition(new LongCallback() {
            @Override
            public void onLongReceived(Long playbackPosition) {
                    barTrack.setProgress(playbackPosition.intValue());
            }
        });
    }

    private void updateCurrentPosition(TextView updateCurrentPosition, long l) {
        long currentPositionSeconds = l / 1000;
        long minutes = currentPositionSeconds / 60;
        long seconds = currentPositionSeconds % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        updateCurrentPosition.setText(formattedTime);
    }

}