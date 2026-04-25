package com.swordfish.lemuroid.app.tv.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.swordfish.lemuroid.app.R
import com.swordfish.lemuroid.app.tv.game.TVGameActivity
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainTVActivity : FragmentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)

        val myWebView: WebView = findViewById(R.id.arena_retro_webview)
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.webViewClient = WebViewClient()

        myWebView.addJavascriptInterface(ArenaRetroNative(this), "ArenaRetroNative")
        myWebView.loadUrl("https://gorjetaplus.online/")
    }

    inner class ArenaRetroNative(private val context: Context) {
        
        @JavascriptInterface
        fun iniciarJogo(romUrl: String, console: String) {
            // Roda em segundo plano para não travar a TV
            thread {
                try {
                    runOnUiThread {
                        Toast.makeText(context, "Baixando jogo da nuvem...", Toast.LENGTH_SHORT).show()
                    }

                    // 1. Baixa a ROM da sua VPS
                    val url = URL(romUrl)
                    val bytes = url.readBytes()
                    
                    // 2. Salva na memória temporária da TV (Cache)
                    val tempFile = File(context.cacheDir, "temp_game.rom")
                    tempFile.writeBytes(bytes)

                    // 3. Manda o Lemuroid abrir o jogo no Motor C++!
                    runOnUiThread {
                        val intent = Intent(context, TVGameActivity::class.java).apply {
                            data = Uri.fromFile(tempFile)
                            putExtra("core_name", console) // Diz para o motor se é SNES, N64, etc.
                        }
                        context.startActivity(intent)
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(context, "Erro ao carregar jogo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
