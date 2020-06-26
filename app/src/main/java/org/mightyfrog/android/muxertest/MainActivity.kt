package org.mightyfrog.android.muxertest

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.fab
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.textView
import java.io.File
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    // https://v.redd.it/d9j4xznvk6841
    // https://v.redd.it/hvnoj4ivz9841
    // https://v.redd.it/3yjp8q0fn8841
    // https://v.redd.it/7zns22u189841
    // https://v.redd.it/33p1vmtq92841 // says 41 min long

    private val id = "d9j4xznvk6841"
    private val audioUrl = "https://v.redd.it/$id/audio"
    private val videoUrl = "https://v.redd.it/$id/DASH_480"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            mux() // blocks UI
        }
    }

    private fun mux() {
        val outputFile = File(Environment.getExternalStorageDirectory(), "$id.mp4")

        val muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(audioUrl)
        val audioIndexMap = hashMapOf<Int, Int>()
        for (i in 0 until audioExtractor.trackCount) {
            audioExtractor.selectTrack(i)
            val audioFormat = audioExtractor.getTrackFormat(i)
            audioIndexMap[i] = muxer.addTrack(audioFormat)
        }

        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoUrl)
        val videoIndexMap = hashMapOf<Int, Int>()
        for (i in 0 until videoExtractor.trackCount) {
            videoExtractor.selectTrack(i)
            val videoFormat = videoExtractor.getTrackFormat(i)
            videoIndexMap[i] = muxer.addTrack(videoFormat)
        }

        var finished = false
        val bufferSize: Int = 1024 * 1024 // increase buffer size if readSampleData fails
        val offset = 0
        val audioBuffer = ByteBuffer.allocate(bufferSize)
        val videoBuffer = ByteBuffer.allocate(bufferSize)
        val audioBufferInfo = MediaCodec.BufferInfo()
        val videoBufferInfo = MediaCodec.BufferInfo()

        muxer.start()
        while (!finished) {
            audioBufferInfo.offset = offset
            audioBufferInfo.size = audioExtractor.readSampleData(audioBuffer, offset)
            audioBufferInfo.flags = audioExtractor.sampleFlags
            audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime

            videoBufferInfo.offset = offset
            videoBufferInfo.size = videoExtractor.readSampleData(videoBuffer, offset)
            videoBufferInfo.flags = videoExtractor.sampleFlags
            videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
            when {
                audioBufferInfo.size < 0 && videoBufferInfo.size < 0 -> {
                    finished = true
                    audioBufferInfo.size = 0
                    videoBufferInfo.size = 0
                }
                else -> {
                    audioIndexMap[audioExtractor.sampleTrackIndex]?.apply {
                        muxer.writeSampleData(
                            this,
                            audioBuffer,
                            audioBufferInfo
                        )
                    }
                    audioExtractor.advance()

                    videoIndexMap[videoExtractor.sampleTrackIndex]?.apply {
                        muxer.writeSampleData(
                            this,
                            videoBuffer,
                            videoBufferInfo
                        )
                    }
                    videoExtractor.advance()
                }
            }
        }
        muxer.stop()
        muxer.release()

        textView.text = getString(R.string.download_complete, "$id.mp4")
    }
}
