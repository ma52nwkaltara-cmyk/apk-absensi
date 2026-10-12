package id.nwkalutara.jadwalmadrasah

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AnnouncementService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingText: String? = null
    private var pendingLang = "id-ID"
    private var pendingRate = 1.0f
    private var pendingPitch = 1.0f

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Siap untuk pengumuman"))
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, null -> updateNotification("Aktif · menunggu pengumuman")
            ACTION_SPEAK -> {
                pendingText = intent.getStringExtra(EXTRA_TEXT)
                pendingLang = intent.getStringExtra(EXTRA_LANG) ?: "id-ID"
                pendingRate = intent.getFloatExtra(EXTRA_RATE, 1.0f).coerceIn(0.5f, 2.0f)
                pendingPitch = intent.getFloatExtra(EXTRA_PITCH, 1.0f).coerceIn(0.5f, 2.0f)
                speakPendingIfReady()
            }
            ACTION_STOP -> {
                tts?.stop()
                updateNotification("Pengumuman dihentikan")
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale("id", "ID")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { updateNotification("Sedang membacakan pengumuman") }
                override fun onDone(utteranceId: String?) { updateNotification("Aktif · menunggu pengumuman") }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { updateNotification("TTS gagal · periksa mesin suara Android") }
            })
            speakPendingIfReady()
        } else {
            updateNotification("Mesin TTS tidak tersedia. Aktifkan Google Text-to-Speech di Android.")
        }
    }

    private fun speakPendingIfReady() {
        val text = pendingText ?: return
        if (!ttsReady) return
        pendingText = null
        val langParts = pendingLang.split('-', '_')
        val locale = if (langParts.size >= 2) Locale(langParts[0], langParts[1]) else Locale(langParts[0])
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
            tts?.setLanguage(Locale("id", "ID"))
        }
        tts?.setSpeechRate(pendingRate)
        tts?.setPitch(pendingPitch)
        updateNotification("Membacakan pengumuman")
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "madrasah-${System.currentTimeMillis()}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Pengumuman Jadwal Madrasah", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Menjaga layanan suara pengumuman tetap aktif"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Jadwal Madrasah")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Jadwal Madrasah")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "id.nwkalutara.jadwalmadrasah.START"
        const val ACTION_SPEAK = "id.nwkalutara.jadwalmadrasah.SPEAK"
        const val ACTION_STOP = "id.nwkalutara.jadwalmadrasah.STOP"
        const val EXTRA_TEXT = "text"
        const val EXTRA_LANG = "lang"
        const val EXTRA_RATE = "rate"
        const val EXTRA_PITCH = "pitch"
        const val EXTRA_VOICE = "voice"
        private const val CHANNEL_ID = "madrasah_announcements"
        private const val NOTIFICATION_ID = 4107
    }
}
