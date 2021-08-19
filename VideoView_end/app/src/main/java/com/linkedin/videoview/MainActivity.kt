package com.linkedin.videoview

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val path = "android.resource://" + packageName + "/" +
                R.raw.sample_video
        videoView.setVideoURI(Uri.parse(path))
        videoView.setOnClickListener {
            when (playing) {
                true -> {
                    videoView.pause()
                    playing = false
                }
                else -> {
                    videoView.start()
                    playing = true
                }
            }
        }
    }
}
