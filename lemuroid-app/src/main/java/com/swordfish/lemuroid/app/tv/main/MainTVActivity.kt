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
// A PONTE MÁGICA (COM O HACK DO OBJETO GAME)
// ==========================================
@Keep
class ArenaRetroNativeBridge(private val context: Context) {
    
    @Keep
    @JavascriptInterface
    fun iniciarJogo(romUrl: String, console: String) {
        thread {
            try {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "Baixando jogo da nuvem...", Toast.LENGTH_SHORT).show()
                }

                val url = URL(romUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != 200) {
                    throw Exception("Servidor negou o arquivo (Código ${connection.responseCode})")
                }

                val bytes = url.readBytes()
                val extensaoOriginal = romUrl.substringAfterLast(".", "rom")
                val tempFile = File(context.cacheDir, "temp_game.$extensaoOriginal")
                tempFile.writeBytes(bytes)

                if (tempFile.length() < 10000) {
                    throw Exception("Arquivo muito pequeno! O link da ROM pode estar quebrado.")
                }

                (context as FragmentActivity).runOnUiThread {
                    // 1. Cria o Intent para abrir o emulador
                    val intent = Intent(context, TVGameActivity::class.java).apply {
                        data = Uri.fromFile(tempFile)
                        putExtra("core_name", console)
                    }

                    // 2. O HACK: Cria um jogo falso para enganar o banco de dados do Lemuroid
                    val mockGame = com.swordfish.lemuroid.lib.library.db.entity.Game(
                        id = -1, // ID Falso
                        title = "Gorjeta Plus Game",
                        path = tempFile.absolutePath, // Onde o jogo está salvo
                        systemId = console,
                        isStarred = false,
                        coverUrl = null
                    )
                    
                    // 3. Envia o Jogo Falso como "bagagem" junto com a intenção
                    intent.putExtra("game", mockGame)

                    // 4. Inicia!
                    context.startActivity(intent)
                }

            } catch (e: Exception) {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "FALHA: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
