package com.example.testvideo.PPlayer;

import java.util.HashMap;

public class PlayerSource {

    public static PlayerSource instance;

    public HashMap<String, PlayerSourceWrapper> source;

    public static PlayerSource getInstance() {
        if (instance == null) {
            instance = new PlayerSource();
        }
        return instance;
    }

    private PlayerSource() {
        source = new HashMap<>();
    }

    public PlayerSourceWrapper createSource(String url, String title) {
        PlayerSourceWrapper wrapper = source.get(url);
        if (wrapper != null) {
            wrapper.title = title;
        } else {
            source.clear(); // temporary only allow one source wrapper in source due to the unclear lifecycle to source wrapper
            wrapper = new PlayerSourceWrapper(url, title, null);
            source.put(url, wrapper);
        }
        return wrapper;
    }
}
