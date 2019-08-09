package com.example.testvideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.testvideo.PPlayer.ActivitySource;
import com.example.testvideo.PPlayer.PlayerViewController;

public class MainActivity extends AppCompatActivity {

    private PlayerViewController mPlayerViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mPlayerViewController == null) {
            setupPlayerView();
        }

    }

    public void setupPlayerView() {
        mPlayerViewController = new PlayerViewController(this);
        String url = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8";
        //url = M3U8Test.media.getLocalServerUrlForPlayingVideoOnline(url);
        mPlayerViewController.setVideoPath(url, "title one");
        mPlayerViewController.setBackButtonOnClickListener(backButton -> {
        });

//        mPlayerViewController.setShareButtonOnClickListener(shareButton -> {
//
//        });

        mPlayerViewController.setScreenSizeButtonOnClickListener(screenSizeButton -> {
            ActivitySource.getInstance().normalScreenSizePlayer = mPlayerViewController.getPlayer();
            Intent intent = new Intent(this, FullScreenPlayerActivity.class);
            intent.putExtra("playWhenReady", mPlayerViewController.getPlayer().getPlayWhenReady());
            startActivityForResult(intent, /*not using*/0);
        });

        mPlayerViewController.setPlayerPeriodicPlayTimeListener(new PlayerViewController.PlayerPeriodicPlayTimeListener() {
            @Override
            public void onPeriodicPlayTime(long previousPosition, long currentPosition) {
                StringBuilder sb = new StringBuilder();
                sb.append("normal screen size - previous: ");
                sb.append(Long.toString(previousPosition));
                sb.append(", current: ");
                sb.append(Long.toString(currentPosition));
                Log.i("onPeriodicPlayTime",sb.toString());

                if (previousPosition != currentPosition) {
                    // playing
                } else {
                    // not playing / pause / stop
                }
            }
        });

        //mPlayerViewController.fakeReload();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPlayerViewController.resetPlayerView();
        if (resultCode == 1) {
            mPlayerViewController.getPlayer().setPlayWhenReady(true);
        } else if (resultCode == 0) {
            mPlayerViewController.getPlayer().setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayerViewController.onActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayerViewController.onActivityPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayerViewController.onActivityDestroy(true);
    }

}
