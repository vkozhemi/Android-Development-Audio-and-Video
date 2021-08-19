package com.linkedin.mediaplayerservice

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC

        play.setOnClickListener {
            val intent = Intent(this, MediaPlayerService::class.java)
            intent.action = MediaPlayerService.PLAY_ACTION
            startService(intent)
        }
        pause.setOnClickListener {
            val intent = Intent(this, MediaPlayerService::class.java)
            intent.action = MediaPlayerService.PAUSE_ACTION
            startService(intent)
        }
        stop.setOnClickListener {
            val intent = Intent(this, MediaPlayerService::class.java)
            intent.action = MediaPlayerService.STOP_ACTION
            startService(intent)
        }
    }
}
