package com.swordfish.lemuroid.app.tv.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import com.swordfish.lemuroid.app.R

class MainTVActivity : FragmentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Carrega o ecrã (ficheiro XML) que esvaziou no passo anterior
        setContentView(R.layout.activity_tv_main)

        // 2. Encontra a sua WebView pelo ID que demos lá no XML
        val myWebView: WebView = findViewById(R.id.arena_retro_webview)

        // 3. Hack essencial: Ativa o Javascript e o LocalStorage para o seu React rodar perfeitamente
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true

        // 4. Trava a navegação dentro da aplicação (impede que a TV abra o navegador externo)
        myWebView.webViewClient = WebViewClient()

        // 5. O Grande Momento: Carrega o seu sistema de Fliperama Digital!
        myWebView.loadUrl("https://gorjetaplus.online/")
    }
}
