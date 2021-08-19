package com.linkedin.soundpool

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

enum class SOUND_STATE {
    INIT,
    PLAYING,
    PAUSED
}
data class SoundFile(val soundId:Int, var state: SOUND_STATE = SOUND_STATE.INIT)

class MainActivity : AppCompatActivity() {
    var soundMap = HashMap<Int, Int>()
    lateinit var soundPool: SoundPool
    lateinit var soundFile1: SoundFile
    lateinit var soundFile2: SoundFile
    lateinit var soundFile3: SoundFile
    lateinit var soundFile4: SoundFile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        soundPool = SoundPool.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setMaxStreams(4)
            .build()
        loadSounds()

        sound1.setOnClickListener {
            toggleState(soundFile1, SOUND1)
        }
        sound2.setOnClickListener {
            toggleState(soundFile2, SOUND2)
        }
        sound3.setOnClickListener {
            toggleState(soundFile3, SOUND3)
        }
        sound4.setOnClickListener {
            toggleState(soundFile4, SOUND4)
        }
    }

    fun playSound(sound: Int) {
        soundPool.play(soundMap[sound]!!,
            1.0f, 1.0f,
            1, -1, 1.0f)
    }

    fun pauseSound(sound: Int) {
        soundPool.pause(soundMap[sound]!!)
    }

    fun resumeSound(sound: Int) {
        soundPool.resume(soundMap[sound]!!)
    }

    fun toggleState(soundFile: SoundFile, sound: Int) {
        when(soundFile.state) {

            SOUND_STATE.INIT -> {
                soundFile.state = SOUND_STATE.PLAYING
                playSound(sound)
            }
            SOUND_STATE.PLAYING -> {
                soundFile.state = SOUND_STATE.PAUSED
                pauseSound(sound)
            }
            SOUND_STATE.PAUSED -> {
                soundFile.state = SOUND_STATE.PLAYING
                resumeSound(sound)
            }
        }
    }

    fun loadSounds() {
        soundMap.put(SOUND1,
            soundPool.load(assets.openFd("flash_beep.mp3")
                , 1))
        soundFile1 = SoundFile(soundMap[SOUND1]!!)

        soundMap.put(SOUND2,
            soundPool.load(assets.openFd("giraffe_energy.mp3")
                , 1))
        soundFile2 = SoundFile(soundMap[SOUND2]!!)

        soundMap.put(SOUND3,
            soundPool.load(assets.openFd("jungle_loop.mp3")
                , 1))
        soundFile3 = SoundFile(soundMap[SOUND3]!!)

        soundMap.put(SOUND4,
            soundPool.load(assets.openFd("monkey_chirp.mp3")
                , 1))
        soundFile4 = SoundFile(soundMap[SOUND4]!!)
    }

    companion object {
        const val SOUND1 = 1
        const val SOUND2 = 2
        const val SOUND3 = 3
        const val SOUND4 = 4
    }
}
