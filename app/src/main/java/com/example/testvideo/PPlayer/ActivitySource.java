package com.example.testvideo.PPlayer;

import com.google.android.exoplayer2.Player;

public class ActivitySource {

    public static ActivitySource instance;

    public Player normalScreenSizePlayer;

    public static ActivitySource getInstance() {
        if (instance == null) {
            instance = new ActivitySource();
        }
        return instance;
    }
}
