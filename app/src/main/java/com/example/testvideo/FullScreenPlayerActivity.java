package com.example.testvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.testvideo.PPlayer.ActivitySource;
import com.example.testvideo.PPlayer.PlayerSource;
import com.example.testvideo.PPlayer.PlayerSourceWrapper;
import com.example.testvideo.PPlayer.PlayerViewController;
import com.google.android.exoplayer2.Player;

public class FullScreenPlayerActivity extends Activity {

    private PlayerViewController playerViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_player);

        // hide status bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // end: hide status bar

        Intent intent = getIntent();
        boolean playWhenReady = intent.getBooleanExtra("playWhenReady", true);
        Player normalScreenSizePlayer = ActivitySource.getInstance().normalScreenSizePlayer;
        playerViewController = new PlayerViewController(this);
        playerViewController.setBackButtonOnClickListener(backButton -> {
            ActivitySource.getInstance().normalScreenSizePlayer = null;
            boolean _playWhenReady = playerViewController.getPlayer().getPlayWhenReady();
            if (_playWhenReady) {
                setResult(1);
            } else {
                setResult(0);
            }
            finish();
        });

//        playerViewController.setShareButtonOnClickListener(shareButton -> {
//
//        });

        playerViewController.setScreenSizeButtonOnClickListener(screenSizeButton -> {
            ActivitySource.getInstance().normalScreenSizePlayer = null;
            boolean _playWhenReady = playerViewController.getPlayer().getPlayWhenReady();
            if (_playWhenReady) {
                setResult(1);
            } else {
                setResult(0);
            }
            finish();
        });

        playerViewController.setPlayer(normalScreenSizePlayer);
        playerViewController.getPlayer().setPlayWhenReady(playWhenReady);
        // temporary fix
        String title = "";
        if (PlayerSource.getInstance().source.values().size() > 0) {
            title = ((PlayerSourceWrapper) PlayerSource.getInstance().source.values().toArray()[0]).title;
            if (title == null) {
                title = "";
            }
        }
        // end: temporary fix
        playerViewController.setTitle(title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerViewController.onActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerViewController.onActivityPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // do not destroy the player because the player is passed by previous activity
        playerViewController.onActivityDestroy(false);
    }
}
