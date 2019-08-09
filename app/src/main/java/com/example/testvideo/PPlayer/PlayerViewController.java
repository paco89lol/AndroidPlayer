package com.example.testvideo.PPlayer;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.testvideo.R;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class PlayerViewController implements GestureDetector.OnGestureListener, View.OnTouchListener  {

    public PlayerViewHolder playerViewHolder;

    private Context context;

    private Handler handler;

    // Gesture
    private PlayerViewGesturePreference gesturePreference;
    private GestureDetector gestureDetector;

    // Sound and Volume
    private PlayerViewPreference preference;

    // tmp variables
    private Player player;
    private String title = "";
    private int playerViewHeaderHeight = 0;
    private int playerViewFooterHeight = 0;
    private long touchDownCurrentPlayTimeMS;
    private long previousTouchDownCurrentPlayTimeMS;
    private long previousPlayTimeMS = 0;

    // exoplayer2 Listener
    private PlayerControlView.VisibilityListener controllerVisibilityListener;
    private Player.EventListener playerEventListener;

    // listener
    private View.OnClickListener soundImageButtonOnClickListener;
    private View.OnClickListener screenSizeButtonOnClickListener;
    private View.OnClickListener backButtonOnClickListener;
    private View.OnClickListener shareButtonOnClickListener;
    private PlayerPeriodicPlayTimeListener playerPeriodicPlayTimeListener;

    // State
    private PlayerViewGestureState gestureState = PlayerViewGestureState.GESTURE_CLEAR;// 1,调节进度，2，调节音量,3.调节亮度
    private boolean isPlayerControllerShowing;
    private boolean onPan;

    // Runnable
    private Runnable miniTimeBarRunnable;

    // Timer
    private Runnable playerPeriodicPlayTimeRunnable;

    public PlayerViewController(@NonNull Context context) {
        this.context = context;
        playerViewHolder = new PlayerViewHolder();
        handler = new Handler();
        gesturePreference = new PlayerViewGesturePreference();
        preference = new PlayerViewPreference(context);
        gestureDetector = new GestureDetector(context, this);
        playerViewHolder = getViewReferenceFromContext(context);
        playerViewHolder.applyStyle();
        playerViewHeaderHeight = playerViewHolder.header.getHeight();
        playerViewFooterHeight = playerViewHolder.footer.getHeight();

        controllerVisibilityListener = createControllerVisibilityListener();
        playerEventListener = createPlayerEventListener();
        soundImageButtonOnClickListener = createSoundImageButtonOnClickListener();

        playerPeriodicPlayTimeRunnable = createPlayerPeriodicPlayTimeRunnable();

        setupPlayerView();
        setPlayer(ExoPlayerFactory.newSimpleInstance(context));
        initMiniTimeBarTimer();
    }

    public void setVideoPath(String url, String title) {
        this.title = title;
        setTitle(title);
        PlayerSourceWrapper wrapper = PlayerSource.getInstance().source.get(url);
        if (wrapper == null) {
            wrapper = PlayerSource.getInstance().createSource(url, title);
        } else {
            wrapper.title = title;
        }
        setVideoSource(wrapper);
    }

    public void setVideoSource(PlayerSourceWrapper playerSourceWrapper) {
        this.title = playerSourceWrapper.title;
        setTitle(title);
        if (playerSourceWrapper.source == null) {
            Uri uri = Uri.parse(playerSourceWrapper.url);
            DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "" + context.getApplicationInfo().packageName));
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).setAllowChunklessPreparation(true).createMediaSource(uri);
            playerSourceWrapper.source = hlsMediaSource;
        }
        playerViewHolder.hide(playerViewHolder.errorViewContainer, true);
        ((SimpleExoPlayer) getPlayer()).prepare(playerSourceWrapper.source, false, false);
    }

    @UiThread
    public void setTitle(@Nullable String title) {
        if (playerViewHolder.titleTextView == null) {
            return;
        }
        playerViewHolder.titleTextView.setText(title);
    }

    public void resetPlayerView() {
        invalidatePlayerViewIfNeeded();
        playerViewHolder.playerViewContainer.removeViewAt(0);
        PlayerView playerView = new PlayerView(context);
        playerView.setId(R.id.player_view);
        playerView.setBackgroundColor(context.getResources().getColor(android.R.color.black));
        playerViewHolder.playerViewContainer.addView(playerView);
        playerViewHolder = getViewReferenceFromContext(context);
        playerViewHolder.applyStyle();
        setupPlayerView();
        setupPlayer();
    }

    public long getCurrentPosition() {
        Player player = getPlayer();
        if (player == null) {
            return 0;
        }
        return player.getCurrentPosition();
    }

    public  long getDuration() {
        Player player = getPlayer();
        if (player == null) {
            return 0;
        }
        return player.getDuration();
    }

    public void stop() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        player.stop();
    }

    public  void onActivityResume() {
    }

    public void onActivityPause() {
        Player player = getPlayer();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    public void onActivityDestroy(boolean destroyPlayer) {
        invalidateAll();
        if (destroyPlayer) {
            Player player = getPlayer();
            if (player != null) {
                player.stop();
                player.release();
            }
        }

    }

    public void seekTo(long positionMS) {
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        if (positionMS < 0) {
            positionMS = 0;
        }
        player.seekTo(positionMS);
    }

    public void fakeReload() {
        if (getPlayer() == null) {
            return;
        }
        getPlayer().setPlayWhenReady(false);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                seekTo(getCurrentPosition() - 1000);
                getPlayer().setPlayWhenReady(true);
            }
        }, 1000);
    }

    public void invalidateAll() {
        invalidateMiniTimeBarTimer();
        invalidatePlayerViewIfNeeded();
        invalidatePlayerIfNeeded();
    }

    public void setBackButtonOnClickListener(@Nullable View.OnClickListener listener) {
        do {
            backButtonOnClickListener = listener;
            if (playerViewHolder.backButton == null || listener == null) {
                break;
            }
            playerViewHolder.backButton.setOnClickListener(listener);
        } while (false);
    }

    public void setShareButtonOnClickListener(@Nullable View.OnClickListener listener) {
        do {
            shareButtonOnClickListener = listener;
            if (playerViewHolder.shareButton == null || listener == null) {
                break;
            }
            playerViewHolder.shareButton.setOnClickListener(listener);
        } while (false);
    }

    public void setScreenSizeButtonOnClickListener(@Nullable View.OnClickListener listener) {
        do {
            screenSizeButtonOnClickListener = listener;
            if (playerViewHolder.screenSizeButton == null || listener == null) {
                break;
            }
            playerViewHolder.screenSizeButton.setOnClickListener(listener);
        } while (false);
    }

    public void setPlayerPeriodicPlayTimeListener(@Nullable PlayerPeriodicPlayTimeListener listener) {
        playerPeriodicPlayTimeListener = listener;
    }

    private void setupPlayerView() {
        PlayerView playerView = playerViewHolder.playerView;
        isPlayerControllerShowing = false;
        playerView.setControllerVisibilityListener(controllerVisibilityListener);
        playerView.setControllerAutoShow(false);
        playerView.setLongClickable(true);
        playerView.setOnTouchListener(this);

        setTitle(title);

        if (preference.isMinVolume()) {
            playerViewHolder.soundImageButton.setImageResource(R.drawable.icon_player_sound_muted);
        } else {
            playerViewHolder.soundImageButton.setImageResource(R.drawable.icon_player_sound);
        }

        playerViewHolder.soundImageButton.setOnClickListener(soundImageButtonOnClickListener);
        playerViewHolder.screenSizeButton.setOnClickListener(screenSizeButtonOnClickListener);
        playerViewHolder.backButton.setOnClickListener(backButtonOnClickListener);
        playerViewHolder.shareButton.setOnClickListener(shareButtonOnClickListener);

    }

    private void invalidatePlayerViewIfNeeded() {
        PlayerView playerView = playerViewHolder.playerView;
        if (playerView == null) {
            return;
        }
        playerView.setControllerVisibilityListener(null);
    }

    public void setPlayer(@Nullable Player player) {
        invalidatePlayerIfNeeded();
        this.player = player;
        setupPlayer();
        handler.postDelayed(playerPeriodicPlayTimeRunnable, 1000);
    }

    private Runnable createPlayerPeriodicPlayTimeRunnable() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (playerPeriodicPlayTimeRunnable == null) {

                } else {
                    long currentPlayTimeMS = getCurrentPosition();
                    if( playerPeriodicPlayTimeListener != null) {
                        playerPeriodicPlayTimeListener.onPeriodicPlayTime(previousPlayTimeMS, currentPlayTimeMS);
                    }
                    previousPlayTimeMS = currentPlayTimeMS;
                    handler.postDelayed(playerPeriodicPlayTimeRunnable, 1000);
                }
            }
        };
        return runnable;
    }

    private void invalidatePlayerPeriodicPlayTime() {
        if (handler == null || playerPeriodicPlayTimeRunnable == null) {
            return;
        }
        handler.removeCallbacks(playerPeriodicPlayTimeRunnable);
    }

    private void setupPlayer() {
        player.addListener(playerEventListener);
        player.setPlayWhenReady(true);
        playerViewHolder.playerView.setPlayer(player);
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    private void invalidatePlayerIfNeeded() {
        Player player = this.player;
        if (player != null) {
            player.removeListener(playerEventListener);
        }

        invalidatePlayerPeriodicPlayTime();
    }

    private Player.EventListener createPlayerEventListener() {
        Player.EventListener playerEventListener = new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                PlayerViewHolder viewHolder = PlayerViewController.this.playerViewHolder;
                Player player = getPlayer();
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        viewHolder.hide(viewHolder.loadingViewContainer, true);
                        break;
                    case Player.STATE_BUFFERING:
                        viewHolder.hide(viewHolder.controlButtonContainer, true);
                        viewHolder.hide(viewHolder.loadingViewContainer, false);
                        break;
                    case Player.STATE_READY:
                        viewHolder.hide(viewHolder.loadingViewContainer, true);
                        viewHolder.hide(viewHolder.controlButtonContainer, false);
                        break;
                    case Player.STATE_ENDED:
                        viewHolder.hide(viewHolder.loadingViewContainer, true);
                        if (player == null) {
                            break;
                        }
                        player.seekTo(0);
                        player.stop();
                        break;
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                PlayerViewHolder viewHolder = PlayerViewController.this.playerViewHolder;
                viewHolder.hide(viewHolder.loadingViewContainer, true);
                viewHolder.hide(viewHolder.controlButtonContainer, true);
                viewHolder.hide(viewHolder.errorViewContainer, false);

                if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                    IOException cause = error.getSourceException();
                    if (cause instanceof HttpDataSource.HttpDataSourceException) {
                        // An HTTP error occurred.
                        HttpDataSource.HttpDataSourceException httpError = (HttpDataSource.HttpDataSourceException) cause;
                        // This is the request for which the error occurred.
                        DataSpec requestDataSpec = httpError.dataSpec;
                        // It's possible to find out more about the error both by casting and by
                        // querying the cause.
                        if (httpError instanceof HttpDataSource.InvalidResponseCodeException) {
                            // Cast to InvalidResponseCodeException and retrieve the response code,
                            // message and headers.
                        } else {
                            // Try calling httpError.getCause() to retrieve the underlying cause,
                            // although note that it may be null.
                        }
                    }
                }

            }
        };
        return playerEventListener;
    }

    private PlayerControlView.VisibilityListener createControllerVisibilityListener() {
        PlayerControlView.VisibilityListener controllerVisibilityListener = new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    isPlayerControllerShowing = true;
                    playerViewHolder.hide(playerViewHolder.miniTimeBar, true);
                } else {
                    isPlayerControllerShowing = false;
                    playerViewHolder.hide(playerViewHolder.miniTimeBar, false);
                }
            }
        };
        return controllerVisibilityListener;
    }

    private View.OnClickListener createSoundImageButtonOnClickListener() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (preference.isMinVolume() == true) {
                    preference.mute(false);
                    playerViewHolder.soundImageButton.setImageResource(R.drawable.icon_player_sound);
                } else {
                    preference.mute(true);
                    playerViewHolder.soundImageButton.setImageResource(R.drawable.icon_player_sound_muted);
                }
            }
        };
        return listener;
    }

    @UiThread
    private void hidePlayerPanel(boolean hide) {
        if (hide) {
            playerViewHolder.playerView.hideController();
        } else {
            playerViewHolder.playerView.showController();
        }
    }

    private void initMiniTimeBarTimer() {
        miniTimeBarRunnable = new Runnable() {
            @Override
            public void run() {
                Player player = getPlayer();
                if (player == null) {

                } else {
                    playerViewHolder.miniTimeBar.setBufferedPosition(player.getBufferedPosition());
                    playerViewHolder.miniTimeBar.setPosition(getCurrentPosition());
                    playerViewHolder.miniTimeBar.setDuration(getDuration());
                }
                handler.postDelayed(miniTimeBarRunnable, 1000);
            }
        };
        handler.postDelayed(miniTimeBarRunnable, 1000);
    }

    private void invalidateMiniTimeBarTimer() {
        if (miniTimeBarRunnable == null) {
            return;
        }
        handler.removeCallbacks(miniTimeBarRunnable);
    }

    private boolean shouldTriggerGesture(@NonNull View playerView, @NonNull MotionEvent motionEvent) {
        float y = playerView.getY();
        float gestureY = motionEvent.getY();
        return (gestureY > (y + playerViewHeaderHeight) && gestureY < (y + playerView.getHeight() - playerViewFooterHeight));
    }

    private PanDirection getPanDirection(@NonNull float distanceX,@NonNull float distanceY) {
        if (Math.abs(distanceX) >= Math.abs(distanceY)) {
            return PanDirection.HORIZONTAL;
        } else {
            return PanDirection.VERTICAL;
        }
    }

    // GestureDetector.OnGestureListener AND View.OnTouchListener

    @Override
    public boolean onDown(MotionEvent e) {
        onPan = true;
        previousTouchDownCurrentPlayTimeMS = getCurrentPosition();
        touchDownCurrentPlayTimeMS = getCurrentPosition();
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) { }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (!isPlayerControllerShowing) {
            hidePlayerPanel(false);
        } else {
            hidePlayerPanel(true);
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent oldMotionEvent, MotionEvent newMotionEvent, float distanceX, float distanceY) {
        hidePlayerPanel(true);

        do {
            if (onPan != true) {
                break;
            }

            switch (getPanDirection(distanceX, distanceY)) {
                case VERTICAL:
                    if (oldMotionEvent.getX() > playerViewHolder.playerView.getWidth() / 2.0) {
                        gestureState = PlayerViewGestureState.GESTURE_MODIFY_VOLUME;
                    } else if (oldMotionEvent.getX() < playerViewHolder.playerView.getWidth() / 2.0) {
                        gestureState = PlayerViewGestureState.GESTURE_MODIFY_BRIGHT;
                    }
                    break;
                case HORIZONTAL:
                    gestureState = PlayerViewGestureState.GESTURE_MODIFY_PROGRESS;
                    break;
            }

        } while (false);

        switch (gestureState) {
            case GESTURE_MODIFY_BRIGHT:
                if (Math.abs(distanceY) < DensityUtil.dip2px(context, gesturePreference.BRIGHTNESS_SCROLL_DRATION)) {
                    break;
                }
                if (distanceY > 0) {
                    preference.turnUpBrightnessBy(0.05f);
                } else {
                    preference.turnDownBrightnessBy(0.05f);
                }
                break;
            case GESTURE_MODIFY_VOLUME:
                if (Math.abs(distanceY) < DensityUtil.dip2px(context, gesturePreference.VOLUME_SCROLL_DRATION)) {
                    break;
                }
                if (distanceY > 0) {
                    preference.turnUpVolumeBy(1);
                } else {
                    preference.turnDownVolumeBy(1);
                }
                if (preference.isMinVolume()) {
                    playerViewHolder.soundImageButton.setImageResource(R.drawable.icon_player_sound_muted);
                } else {
                    playerViewHolder.soundImageButton.setImageResource(R.drawable.icon_player_sound);
                }
                break;
            case GESTURE_MODIFY_PROGRESS:
                if (Math.abs(distanceX) < DensityUtil.dip2px(context, gesturePreference.PROGRESS_SCROLL_DRATION)) {
                    break;
                }
                if (getPlayer() == null) {
                    break;
                }
                if (getCurrentPosition() < 0 || getDuration() <= 0) {
                    break;
                }
                playerViewHolder.hide(playerViewHolder.progressModificationViewContainer, false);
                if (distanceX > 0) {
                    playerViewHolder.progressModificationImageView.setImageResource(R.drawable.icon_player_rewind);
                    touchDownCurrentPlayTimeMS -= 15 * 1000;
                    if (touchDownCurrentPlayTimeMS < 0) {
                        touchDownCurrentPlayTimeMS = 0;
                    }
                } else {
                    playerViewHolder.progressModificationImageView.setImageResource(R.drawable.icon_player_forward);
                    touchDownCurrentPlayTimeMS += 15 * 1000;
                    if (touchDownCurrentPlayTimeMS > getDuration()) {
                        touchDownCurrentPlayTimeMS = getDuration();
                    }
                }
                SimpleDateFormat currentStrFormat = new SimpleDateFormat("HH:mm:ss");
                currentStrFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String currentStr = currentStrFormat.format(new Date((touchDownCurrentPlayTimeMS)));

                SimpleDateFormat durationStrFormat = new SimpleDateFormat("HH:mm:ss");
                durationStrFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String durationStr = durationStrFormat.format(new Date(getDuration()));
                playerViewHolder.progressModificationTextView.setText(currentStr + " / " + durationStr);
//                seekTo(touchDownCurrentPlayTimeMS);
                break;

        }

        onPan = false;
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) { }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            playerViewHolder.hide(playerViewHolder.progressModificationViewContainer, true);
            if (touchDownCurrentPlayTimeMS != previousTouchDownCurrentPlayTimeMS) {
                seekTo(touchDownCurrentPlayTimeMS);
            }
        }

        if (shouldTriggerGesture(view, motionEvent)) {
            gestureDetector.onTouchEvent(motionEvent);
        }

        return true;
    }

    // end: GestureDetector.OnGestureListener AND View.OnTouchListener

    // Inner class

    private PlayerViewHolder getViewReferenceFromContext(Context context) {
        PlayerViewHolder viewHolder = new PlayerViewHolder();
        viewHolder.playerViewContainer = ((Activity) context).findViewById(R.id.player_view_container);
        // header
        viewHolder.header = ((Activity) context).findViewById(R.id.player_view_header);
        // footer
        viewHolder.footer = ((Activity) context).findViewById(R.id.player_view_footer);
        // player
        viewHolder.playerView = ((Activity) context).findViewById(R.id.player_view);
        // title view
        viewHolder.titleTextView = ((Activity) context).findViewById(R.id.player_video_title);
        // error view
        viewHolder.errorViewContainer = ((Activity) context).findViewById(R.id.player_error_view_container);
        viewHolder.errorImageView = ((Activity) context).findViewById(R.id.player_error_iv);
        // loading view
        viewHolder.loadingViewContainer = ((Activity) context).findViewById(R.id.player_loading_view_container);
        viewHolder.loadingSpinner = ((Activity) context).findViewById(R.id.player_loading_spinner);
        viewHolder.soundImageButton = ((Activity) context).findViewById(R.id.player_sound);
        viewHolder.screenSizeButton = ((Activity) context).findViewById(R.id.player_screen_size_button);
        viewHolder.backButton = ((Activity) context).findViewById(R.id.player_back_button);
        viewHolder.shareButton = ((Activity) context).findViewById(R.id.player_share_button);
        // progress modification view
        viewHolder.progressModificationViewContainer = ((Activity) context).findViewById(R.id.gesture_progress_layout);
        viewHolder.progressModificationImageView = ((Activity) context).findViewById(R.id.gesture_iv_progress);
        viewHolder.progressModificationTextView = ((Activity) context).findViewById(R.id.gesture_tv_progress_time);
        // time bar
        viewHolder.timeBar = ((Activity) context).findViewById(R.id.exo_progress);
        viewHolder.miniTimeBar = ((Activity) context).findViewById(R.id.player_view_mini_time_bar);
        //
        viewHolder.controlButtonContainer = ((Activity) context).findViewById(R.id.player_control_button_container);
        viewHolder.rewindImageButton = ((Activity) context).findViewById(R.id.exo_rew);
        viewHolder.playImageButton = ((Activity) context).findViewById(R.id.exo_play);
        viewHolder.pauseImageButton = ((Activity) context).findViewById(R.id.exo_pause);
        viewHolder.forwardImageButton = ((Activity) context).findViewById(R.id.exo_ffwd);

        return viewHolder;
    }

    public class PlayerViewHolder {
        // the root view of player
        public FrameLayout playerViewContainer;
        // header
        public LinearLayout header;
        // footer
        public LinearLayout footer;
        // player view
        public PlayerView playerView;
        // the title of player view
        public TextView titleTextView;
        // the loading view of player
        public FrameLayout loadingViewContainer;
        public ProgressBar loadingSpinner;
        // the error view of player
        public FrameLayout errorViewContainer;
        public ImageView errorImageView;
        // operation button of player view
        public ImageButton soundImageButton;
        public ImageButton screenSizeButton;
        public ImageButton backButton;
        public ImageButton shareButton;
        // the root view of preference setting
        public RelativeLayout progressModificationViewContainer;
        // sup view of preference setting
        public TextView progressModificationTextView;
        public ImageView progressModificationImageView;
        // play view time bar
        public DefaultTimeBar timeBar;
        public DefaultTimeBar miniTimeBar;

        //
        public FrameLayout controlButtonContainer;
        public ImageButton rewindImageButton;
        public ImageButton playImageButton;
        public ImageButton pauseImageButton;
        public ImageButton forwardImageButton;

        public void hide(View view, boolean isHidden) {
            if (view == null) {
                return;
            }
            if (isHidden == true) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        }

        public void hide(View view, int visibility) {
            view.setVisibility(visibility);
        }

        public boolean isHide(View view) {
            if (view.getVisibility() == View.VISIBLE) {
                return false;
            } else {
                return true;
            }
        }

        public void applyStyle() {
            playerView.setBackgroundColor(Color.parseColor("#000000"));
            rewindImageButton.setColorFilter(Color.parseColor("#E985AE"));
            playImageButton.setColorFilter(Color.parseColor("#E985AE"));
            pauseImageButton.setColorFilter(Color.parseColor("#E985AE"));
            forwardImageButton.setColorFilter(Color.parseColor("#E985AE"));

            progressModificationImageView.setColorFilter(Color.parseColor("#E985AE"));

            timeBar.setScrubberColor(Color.parseColor("#E985AE"));
            timeBar.setPlayedColor(Color.parseColor("#E985AE"));
            miniTimeBar.setPlayedColor(Color.parseColor("#E985AE"));
            miniTimeBar.setScrubberColor(Color.parseColor("#00000000"));

            loadingSpinner.getIndeterminateDrawable().setColorFilter(Color.parseColor("#E985AE"), PorterDuff.Mode.MULTIPLY);
            errorImageView.setColorFilter(Color.parseColor("#E985AE"));
        }
    }

    // end: Inner class

    // Interface

    public interface PlayerPeriodicPlayTimeListener {

        void onPeriodicPlayTime(long previousPosition, long currentPosition);
    }

    // end: Interface
}
