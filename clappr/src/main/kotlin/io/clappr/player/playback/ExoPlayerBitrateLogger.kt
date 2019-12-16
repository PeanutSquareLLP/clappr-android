package io.clappr.player.playback

import android.os.Bundle
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.MediaSourceEventListener
import io.clappr.player.base.Event
import io.clappr.player.base.EventData
import io.clappr.player.bitrate.BitrateHistory
import io.clappr.player.components.Playback
import io.clappr.player.log.Logger

class ExoPlayerBitrateLogger(private val playback: Playback,
                             private val bitrateHistory: BitrateHistory = BitrateHistory { System.nanoTime() }) :
    AnalyticsListener {

    private var audio = 0L
    private var video = 0L

    var lastBitrate: Long? = null
        set(value) {

            val oldValue = field

            field = value

            try {
                bitrateHistory.addBitrate(field)
            } catch (e: BitrateHistory.BitrateLog.WrongTimeIntervalException) {
                Logger.error(ExoPlayerBitrateLogger::class.java.simpleName, e.message
                    ?: "Can not add bitrate on history")
            }

            if (oldValue != field) {
                playback.trigger(Event.DID_UPDATE_BITRATE.value, Bundle().apply {
                    putLong(EventData.BITRATE.value, field ?: 0)
                })
            }
        }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime?,
        loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
        mediaLoadData: MediaSourceEventListener.MediaLoadData?
    ) {
        mediaLoadData?.let { data ->
            when (data.trackType) {
                C.TRACK_TYPE_DEFAULT, C.TRACK_TYPE_VIDEO -> {
                    data.trackFormat?.bitrate?.let {
                        video = it.toLong()
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    data.trackFormat?.bitrate
                        ?.takeIf { it > 0 }
                        ?.let { audio = it.toLong() }
                }
            }
            lastBitrate = if ((video + audio) > 0) video + audio else null
        }
    }

    fun averageBitrate() = bitrateHistory.averageBitrate()
    fun clearHistory() = bitrateHistory.clear()
}