package com.linkedin.mediaplayerservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import java.lang.Exception

enum class STATE {
    INIT,
    PREPARING,
    PLAYING,
    PAUSED,
    STOPPED
}

class MediaPlayerService : MediaBrowserServiceCompat(), MediaPlayer.OnPreparedListener {
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var channel: NotificationChannel
    private var state = STATE.INIT

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        //  Browsing not allowed
        if (EMPTY_MEDIA_ROOT_ID == parentMediaId) {
            result.sendResult(null)
            return
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (MediaButtonReceiver.handleIntent(mediaSession, intent) == null) {
            when (intent?.action) {
                MediaPlayerService.PLAY_ACTION -> play()
                MediaPlayerService.PAUSE_ACTION -> pause()
                MediaPlayerService.STOP_ACTION -> stop()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                try {
                    setWakeMode(this@MediaPlayerService,PowerManager.PARTIAL_WAKE_LOCK)
                    state = STATE.PREPARING
                    setDataSource(PODCAST_URL)
                    setOnPreparedListener(this@MediaPlayerService)
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Problems loading audio", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initChannel()
        startInForeGround()
    }

    override fun onDestroy() {
        stop()
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onPrepared(mp: MediaPlayer) {
        mediaSession?.isActive = true
        state = STATE.PLAYING
        mp.start()
        showNotification()
        setPlaybackState()
    }

    private fun initChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            getString(R.string.default_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = getString(R.string.default_channel_description)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }

    private fun stop() {
        state = STATE.STOPPED
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        mediaSession?.isActive = false
        mediaSession = null
        stopForeground(true)
        stopSelf()
    }

    private fun pause() {
        mediaPlayer?.pause()
        state = STATE.PAUSED
        val stateBuilder = getPlaybackBuilder()
        stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
            0, 1.0f, SystemClock.elapsedRealtime())
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun play() {
        createMediaPlayer()
        setupMediaSession()
        if (state == STATE.PAUSED) {
            state = STATE.PLAYING
            mediaPlayer?.start()
            setPlaybackState()
        }
    }

    private fun setPlaybackState() {
        val stateBuilder = getPlaybackBuilder()
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f, SystemClock.elapsedRealtime())
        mediaSession?.setPlaybackState(stateBuilder.build())
        mediaSession?.isActive = true
    }

    fun createNotification(): Notification {
        return NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID).apply {
            // Add the metadata for the currently playing track
            setContentTitle("Podcast")
            setContentText("SubTitle")

            // Enable launching the player by clicking the notification
            setContentIntent(
                PendingIntent.getActivity(
                    this@MediaPlayerService, 0,
                    Intent(this@MediaPlayerService, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            // Stop the service when the notification is swiped away
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@MediaPlayerService,
                    PlaybackStateCompat.ACTION_STOP
                )
            )


            // Make the transport controls visible on the lockscreen
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add an app icon and set its accent color
            // Be careful about the color
            setSmallIcon(R.mipmap.ic_launcher_round)
            color = ContextCompat.getColor(this@MediaPlayerService, R.color.primary_dark_material_dark)

            // Add a pause button
            var action = NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@MediaPlayerService,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
            addAction(action)

            // Play Button
            action = NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@MediaPlayerService,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )

            addAction(action)

            // Stop/Exit button
            action = NotificationCompat.Action(
                android.R.drawable.ic_delete,
                getString(R.string.stop),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@MediaPlayerService,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            addAction(action)

            // Take advantage of MediaStyle features
            setStyle(
                android.support.v4.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)

                    // Add a cancel button
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this@MediaPlayerService,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        }.build()
    }

    fun startInForeGround() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    fun showNotification() {
        val notification = createNotification()
        val mgr = NotificationManagerCompat.from(this)
        mgr.cancelAll()
        mgr.notify(NOTIFICATION_ID, notification)
    }

    private fun setupMediaSession() {
        if (mediaSession == null) {
            val mediaButtonReceiver = ComponentName(this, MediaButtonReceiver::class.java)
            mediaSession = MediaSessionCompat(this, "MediaPlayer", mediaButtonReceiver, null).apply {

                // Enable callbacks from MediaButtons and TransportControls

                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )


                // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
                val stateBuilder = getPlaybackBuilder()

                isActive = true
                setPlaybackState(stateBuilder.build())


                // MySessionCallback() has methods that handle callbacks from a media controller
                setCallback(SessionCallback())

                // Set the session's token so that client activities can communicate with it.
                setSessionToken(sessionToken)

            }
        }
    }

    private fun getPlaybackBuilder(): PlaybackStateCompat.Builder {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_STOP
            )
        return stateBuilder
    }

    inner class SessionCallback : MediaSessionCompat.Callback() {

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent =
                mediaButtonEvent?.getParcelableExtra<Parcelable>("android.intent.extra.KEY_EVENT") as KeyEvent?
            keyEvent?.let {
                if (keyEvent.action == 0) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            onPlay()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            onPlay()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            onPause()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            onStop()
                            return true
                        }
                    }
                }

            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlay() {
            if (state == STATE.PLAYING) {
                pause()
            } else {
                play()
            }
        }

        override fun onPause() {
            pause()
        }

        override fun onStop() {
            stop()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        val DEFAULT_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".media_playback_channel"
        const val PODCAST_URL = "https://791353.youcanlearnit.net/EC_podcast.mp3"
        const val PLAY_ACTION = "play_action"
        const val STOP_ACTION = "stop_action"
        const val PAUSE_ACTION = "pause_action"
        const val EMPTY_MEDIA_ROOT_ID = "EMPTY_MEDIA_ROOT_ID"
        val TAG = "MediaPlayerService"
    }

}
