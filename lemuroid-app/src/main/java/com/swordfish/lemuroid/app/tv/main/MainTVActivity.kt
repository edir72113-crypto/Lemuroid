package com.swordfish.lemuroid.app.tv.main

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.swordfish.lemuroid.app.R

class MainTVActivity : FragmentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)

        val myWebView: WebView = findViewById(R.id.arena_retro_webview)
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.webViewClient = WebViewClient()

        // 1. A PONTE MÁGICA: Conecta o seu site React ao hardware nativo da TV
        myWebView.addJavascriptInterface(ArenaRetroNative(this), "ArenaRetroNative")

        myWebView.loadUrl("https://gorjetaplus.online/")
    }

    // 2. Esta classe fica "escutando" os comandos que vêm do seu React
    inner class ArenaRetroNative(private val context: Context) {
        
        @JavascriptInterface
        fun iniciarJogo(romUrl: String, console: String) {
            // Quando o React chamar essa função, o Kotlin fará o download da ROM
            // e vai disparar o motor C++ do Lemuroid. (Faremos essa lógica no próximo passo)
            
            // Um aviso só para termos certeza que a ponte funcionou quando testarmos:
            Toast.makeText(context, "Kotlin recebeu: \$console", Toast.LENGTH_SHORT).show()
        }
    }
}
