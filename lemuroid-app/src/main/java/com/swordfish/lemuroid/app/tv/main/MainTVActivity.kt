package com.swordfish.lemuroid.app.tv.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import com.swordfish.lemuroid.R
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
        
        // 1. MATA O CACHE: Garante que o React atualizado sempre seja baixado
        myWebView.clearCache(true)
        myWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        myWebView.webViewClient = WebViewClient()

        // 2. INJETA A PONTE: Conecta o JS com o Kotlin
        myWebView.addJavascriptInterface(ArenaRetroNativeBridge(this), "ArenaRetroNative")
        myWebView.loadUrl("https://gorjetaplus.online/")
    }
}

// ==========================================
// A PONTE MÁGICA (BLINDADA E COM INSPETOR DE QUALIDADE)
// ==========================================
@Keep // Escudo 1: Impede o Android de apagar ou renomear a classe
class ArenaRetroNativeBridge(private val context: Context) {
    
    @Keep // Escudo 2: Impede o Android de renomear esta função
    @JavascriptInterface
    fun iniciarJogo(romUrl: String, console: String) {
        thread {
            try {
                // Aviso visual de início
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "Baixando jogo da nuvem...", Toast.LENGTH_SHORT).show()
                }

                // Inspetor de Download - Passo 1: Conectar
                val url = URL(romUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                // Inspetor de Download - Passo 2: O servidor deu erro? (ex: 404, 500)
                if (connection.responseCode != 200) {
                    throw Exception("Servidor negou o arquivo (Código ${connection.responseCode})")
                }

                // Inspetor de Download - Passo 3: Baixar os bytes
                val bytes = url.readBytes()
                
                // 🚀 A SUA IDEIA AQUI: Pega a extensão real do link (.sfc, .smc, .bin, etc)
                // Se não achar um ponto, ele usa "rom" como segurança.
                val extensaoOriginal = romUrl.substringAfterLast(".", "rom")
                val tempFile = File(context.cacheDir, "temp_game.$extensaoOriginal")
                
                tempFile.writeBytes(bytes)

                // Inspetor de Download - Passo 4: O arquivo é uma página HTML de erro quebrada? (menor que 10KB)
                if (tempFile.length() < 10000) {
                    throw Exception("Arquivo muito pequeno! O link da ROM pode estar quebrado.")
                }

                // Sucesso Absoluto: Abre o Lemuroid Nativo!
                (context as FragmentActivity).runOnUiThread {
                    val intent = Intent(context, TVGameActivity::class.java).apply {
                        data = Uri.fromFile(tempFile)
                        putExtra("core_name", console)
                    }
                    context.startActivity(intent)
                }

            } catch (e: Exception) {
                // Se qualquer coisa falhar (download, conexão, tamanho do arquivo), mostra o erro na TV
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "FALHA: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
