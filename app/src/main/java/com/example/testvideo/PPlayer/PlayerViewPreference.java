package com.example.testvideo.PPlayer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

public class PlayerViewPreference {

    private int minVolume;
    private int maxVolume;
    private int volumeBeforeMuted;

    private float minBrightness;
    private float maxBrightness;

    private Context context;

    public PlayerViewPreference(Context context) {
        this.context = context;
        minBrightness = 0.1f;
        maxBrightness = 1.0f;
        minVolume = 0;
        maxVolume = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    //Volume
    public boolean isMinVolume() {
        return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
    }

    public void mute(boolean mute) {
        if (mute) {
            volumeBeforeMuted = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
            setVolume(0);
        } else {
            setVolume(volumeBeforeMuted);
        }
    }

    public int getCurrentVolume() {
        int current = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
        if (current < 0) {
            setVolume(0);
        }
        return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public void setVolume(int volume) {
        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    public void turnDownVolumeBy(int down) {
        int current = getCurrentVolume();
        if (current - down < minVolume) {
            current = minVolume;
        }
        setVolume(current);
    }

    public void turnUpVolumeBy(int up) {
        int current = getCurrentVolume();
        if (current + up > maxVolume) {
            current = maxVolume;
        }
        setVolume(current);
    }

    // Brightness

    public float getCurrentBrightness() {
        return ((Activity) context).getWindow().getAttributes().screenBrightness;
    }

    public void setBrightness(float brightness) {
        ((Activity) context).getWindow().getAttributes().screenBrightness = brightness;
    }

    public void turnDownBrightnessBy(float down) {
        float current = getCurrentBrightness();
        if (current - down < minBrightness) {
            current = minBrightness;
        }
        setBrightness(current);
    }

    public void turnUpBrightnessBy(float up) {
        float current = getCurrentBrightness();
        if (current + up > maxBrightness) {
            current = maxBrightness;
        }
        setBrightness(current);
    }
}
