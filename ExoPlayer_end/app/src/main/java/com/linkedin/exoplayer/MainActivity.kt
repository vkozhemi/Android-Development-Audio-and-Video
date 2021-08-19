package com.linkedin.exoplayer

import android.net.Uri
import android.os.Bundle
import android.support.annotation.RawRes
import android.support.v7.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var videoExoPlayer: SimpleExoPlayer
    lateinit var audioExoPlayer: SimpleExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createVideoPlayer()
        createAudioPlayer()
        setupVideoPlayer()
        setupAudioPlayer(URL)
        play.setOnClickListener {
            audioExoPlayer.playWhenReady = true
        }
        pause.setOnClickListener {
            audioExoPlayer.playWhenReady = false
        }
        playerView.player = videoExoPlayer
    }

    fun setupAudioPlayer(url: String) {
        audioExoPlayer.prepare(createUrlMediaSource(url))
    }

    fun setupVideoPlayer() {
        videoExoPlayer.prepare(createRawMediaSource(R.raw.sports))
    }
    fun createVideoPlayer() {
        val trackSelector = DefaultTrackSelector()
        val loadControl = DefaultLoadControl()
        val renderersFactory = DefaultRenderersFactory(this)

        videoExoPlayer = ExoPlayerFactory.newSimpleInstance(
            this, renderersFactory, trackSelector, loadControl
        )

        videoExoPlayer.videoScalingMode =
                C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
    }
    fun createAudioPlayer() {
        val trackSelector = DefaultTrackSelector()
        val loadControl = DefaultLoadControl()
        val renderersFactory = DefaultRenderersFactory(this)

        audioExoPlayer = ExoPlayerFactory.newSimpleInstance(
            this, renderersFactory, trackSelector, loadControl
        )


    }
    fun createUrlMediaSource(url: String): MediaSource {
        val userAgent = Util.getUserAgent(this,
            getString(R.string.app_name))
        return ExtractorMediaSource
            .Factory(DefaultDataSourceFactory(this, userAgent))
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(url))
    }

    fun createRawMediaSource(@RawRes rawId: Int): MediaSource {
        val rawResourceDataSource = RawResourceDataSource(this)
        val dataSpec = DataSpec(RawResourceDataSource.buildRawResourceUri(rawId))
        rawResourceDataSource.open(dataSpec)
        return ExtractorMediaSource.Factory(DataSource.Factory {
            rawResourceDataSource
        }).createMediaSource(rawResourceDataSource.uri)
    }

    companion object {
        const val URL = "https://791353.youcanlearnit.net/EC_podcast.mp3"

    }
}
