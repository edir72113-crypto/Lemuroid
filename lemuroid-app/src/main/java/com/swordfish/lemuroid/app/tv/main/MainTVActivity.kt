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
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import java.io.File
import java.net.URL
import java.util.HashMap
import java.util.ArrayList
import kotlin.concurrent.thread

class MainTVActivity : FragmentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)

        val myWebView: WebView = findViewById(R.id.arena_retro_webview)
        
        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        myWebView.setInitialScale(1)
        myWebView.clearCache(true)

        myWebView.webViewClient = WebViewClient()
        myWebView.addJavascriptInterface(ArenaRetroNativeBridge(this), "ArenaRetroNative")
        myWebView.loadUrl("https://gorjetaplus.online/")
    }
}

@Keep
class ArenaRetroNativeBridge(private val context: Context) {
    
    @Keep
    @JavascriptInterface
    fun iniciarJogo(romUrl: String, console: String) {
        thread {
            try {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "A preparar o jogo...", Toast.LENGTH_SHORT).show()
                }

                val url = URL(romUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != 200) {
                    throw Exception("Erro no servidor: ${connection.responseCode}")
                }

                val bytes = url.readBytes()
                val extensaoOriginal = romUrl.substringAfterLast(".", "rom")
                val tempFile = File(context.cacheDir, "temp_game.$extensaoOriginal")
                tempFile.writeBytes(bytes)

                (context as FragmentActivity).runOnUiThread {
                    val intent = Intent(context, TVGameActivity::class.java).apply {
                        data = Uri.fromFile(tempFile)
                    }

                    val mockGame = Game(
                        id = -1,
                        title = "Arena Retrô Play",
                        systemId = console,
                        fileName = tempFile.name,
                        fileUri = Uri.fromFile(tempFile).toString(),
                        developer = "Gorjeta Plus",
                        coverFrontUrl = "",
                        lastIndexedAt = System.currentTimeMillis()
                    )

                    val mockConfigsMap = HashMap<Int, ArrayList<ControllerConfig>>()

                    // 🚀 CORREÇÃO DEFINITIVA: Em vez de CoreID(""), pegamos o primeiro valor do Enum
                    val defaultCore = CoreID.values().first()

                    val mockConfig = SystemCoreConfig(
                        coreID = defaultCore, 
                        controllerConfigs = mockConfigsMap,
                        exposedSettings = listOf(),
                        exposedAdvancedSettings = listOf()
                    )
                    
                    intent.putExtra("GAME", mockGame)
                    intent.putExtra("EXTRA_SYSTEM_CORE_CONFIG", mockConfig)
                    intent.putExtra("core_name", console)

                    context.startActivity(intent)
                }

            } catch (e: Exception) {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "ERRO: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Keep
    @JavascriptInterface
    fun fecharEmulador() {
        (context as FragmentActivity).runOnUiThread {
            val intent = Intent(context, MainTVActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        }
    }
}
