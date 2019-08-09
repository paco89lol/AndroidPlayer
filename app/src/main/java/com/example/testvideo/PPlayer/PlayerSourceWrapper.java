package com.example.testvideo.PPlayer;

import com.google.android.exoplayer2.source.hls.HlsMediaSource;

public class PlayerSourceWrapper {
    public String url;
    public String title;
    public HlsMediaSource source;

    public PlayerSourceWrapper(String url, String title, HlsMediaSource source) {
        this.url = url;
        this.title = title;
        this.source = source;
    }
}
