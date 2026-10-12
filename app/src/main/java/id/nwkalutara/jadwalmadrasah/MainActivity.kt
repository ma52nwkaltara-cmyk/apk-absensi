package id.nwkalutara.jadwalmadrasah

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val scheduleUrl = "https://absensi.rdmku.cloud/index.html"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        startAnnouncementService()

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                installNativeSpeechBridge()
            }
        }
        webView.addJavascriptInterface(NativeTtsBridge(), "MadrasahNative")
        setContentView(webView)
        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else webView.loadUrl(scheduleUrl)
    }

    private fun installNativeSpeechBridge() {
        // Redirect Web Speech API utterances from the schedule page to Android TextToSpeech.
        // Existing HTML remains hosted on the user's FastPanel server.
        val js = """
            (function() {
              if (window.__madrasahNativeTtsInstalled) return;
              window.__madrasahNativeTtsInstalled = true;
              try {
                const NativeUtterance = window.SpeechSynthesisUtterance;
                window.speechSynthesis.speak = function(u) {
                  if (!u) return;
                  const text = String(u.text || '');
                  if (!text.trim()) return;
                  const lang = u.lang || 'id-ID';
                  const rate = Number(u.rate || 1);
                  const pitch = Number(u.pitch || 1);
                  const voice = u.voice && u.voice.name ? u.voice.name : '';
                  window.MadrasahNative.speak(text, lang, rate, pitch, voice);
                  if (typeof u.onstart === 'function') setTimeout(() => u.onstart(new Event('start')), 0);
                  if (typeof u.onend === 'function') setTimeout(() => u.onend(new Event('end')), Math.max(500, text.length * 55));
                };
                window.speechSynthesis.cancel = function() { window.MadrasahNative.stop(); };
                window.speechSynthesis.pause = function() {};
                window.speechSynthesis.resume = function() {};
              } catch(e) { console.log('Native TTS bridge:', e); }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun startAnnouncementService() {
        val intent = Intent(this, AnnouncementService::class.java).apply {
            action = AnnouncementService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    inner class NativeTtsBridge {
        @JavascriptInterface
        fun speak(text: String, lang: String?, rate: Float, pitch: Float, voiceName: String?) {
            val intent = Intent(this@MainActivity, AnnouncementService::class.java).apply {
                action = AnnouncementService.ACTION_SPEAK
                putExtra(AnnouncementService.EXTRA_TEXT, text)
                putExtra(AnnouncementService.EXTRA_LANG, lang ?: "id-ID")
                putExtra(AnnouncementService.EXTRA_RATE, rate.coerceIn(0.5f, 2.0f))
                putExtra(AnnouncementService.EXTRA_PITCH, pitch.coerceIn(0.5f, 2.0f))
                putExtra(AnnouncementService.EXTRA_VOICE, voiceName ?: "")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        }

        @JavascriptInterface
        fun stop() {
            startService(Intent(this@MainActivity, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_STOP))
        }

        @JavascriptInterface
        fun status(): String = "Google TTS Android bridge aktif"
    }
}
