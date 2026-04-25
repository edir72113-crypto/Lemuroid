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
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
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
        
        // 1. LIMPEZA DE CACHE: Garante que o site React sempre carregue atualizado
        myWebView.clearCache(true)
        myWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        myWebView.webViewClient = WebViewClient()

        // 2. INJEÇÃO DA PONTE NATIVA: Conecta o site com o Android
        myWebView.addJavascriptInterface(ArenaRetroNativeBridge(this), "ArenaRetroNative")
        myWebView.loadUrl("https://gorjetaplus.online/")
    }
}

// ==========================================
// A PONTE MÁGICA (VERSÃO FINAL SNIPER)
// ==========================================
@Keep
class ArenaRetroNativeBridge(private val context: Context) {
    
    // FUNÇÃO 1: INICIAR O JOGO (COM TODAS AS MALAS NECESSÁRIAS)
    @Keep
    @JavascriptInterface
    fun iniciarJogo(romUrl: String, console: String) {
        thread {
            try {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "Baixando jogo da nuvem...", Toast.LENGTH_SHORT).show()
                }

                // Download da ROM para a pasta de Cache
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
                    throw Exception("Arquivo muito pequeno! Link quebrado.")
                }

                (context as FragmentActivity).runOnUiThread {
                    val intent = Intent(context, TVGameActivity::class.java).apply {
                        data = Uri.fromFile(tempFile)
                    }

                    // 🚀 O HACK DO JOGO: Identidade necessária (Chave "GAME")
                    val mockGame = Game(
                        id = -1,
                        title = "Fliperama Arena Retro",
                        systemId = console,
                        fileName = tempFile.name,
                        fileUri = Uri.fromFile(tempFile).toString(),
                        developer = "Gorjeta Plus",
                        coverFrontUrl = "",
                        lastIndexedAt = System.currentTimeMillis()
                    )

                    // 🚀 O HACK DA CONFIG: A "mala" obrigatória (Chave "EXTRA_SYSTEM_CORE_CONFIG")
                    val mockConfig = SystemCoreConfig(
                        systemId = console,
                        coreId = "", // O Lemuroid escolherá o core padrão
                        exposedSettings = listOf(),
                        exposedAdvancedSettings = listOf()
                    )
                    
                    // Injeta os dados usando as chaves exatas do BaseGameActivity.kt
                    intent.putExtra("GAME", mockGame)
                    intent.putExtra("EXTRA_SYSTEM_CORE_CONFIG", mockConfig)
                    intent.putExtra("core_name", console)

                    context.startActivity(intent)
                }

            } catch (e: Exception) {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "FALHA: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // FUNÇÃO 2: FECHAR O EMULADOR PELO CELULAR
    @Keep
    @JavascriptInterface
    fun fecharEmulador() {
        (context as FragmentActivity).runOnUiThread {
            // O comando CLEAR_TOP destrói o jogo e traz a WebView de volta sem recarregar
            val intent = Intent(context, MainTVActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        }
    }
}
