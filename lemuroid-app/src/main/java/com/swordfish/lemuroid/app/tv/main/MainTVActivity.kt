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
import com.swordfish.lemuroid.lib.library.GameSystem
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
        
        // 🚀 FIX GRÁFICO (Desativa o Wide Gamut que dava crash de OpenGL)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        }

        setContentView(R.layout.activity_tv_main)
        val myWebView: WebView = findViewById(R.id.arena_retro_webview)
        
        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
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
                val bytes = url.readBytes()
                val extensao = romUrl.substringAfterLast(".", "sfc").lowercase()
                
                val tempFile = File(context.filesDir, "game.$extensao")
                tempFile.writeBytes(bytes)

                (context as FragmentActivity).runOnUiThread {
                    val intent = Intent(context, TVGameActivity::class.java).apply {
                        data = Uri.fromFile(tempFile)
                    }

                    val systemIdUpper = console.uppercase()

                    // 🚀 O GRANDE TRUQUE: Procura o sistema real dentro do próprio Lemuroid
                    val gameSystem = GameSystem.all().find { it.id.name == systemIdUpper }
                        ?: GameSystem.findByUniqueFileExtension(extensao)
                        ?: GameSystem.all().first()

                    // Extrai o nome de banco de dados que o Lemuroid realmente exige ("Nintendo - Super Nintendo...")
                    val correctDbName = gameSystem.id.dbname

                    val mockGame = Game(
                        id = -1,
                        title = "Arena Retrô Play",
                        systemId = correctDbName, // AQUI É A CHAVE DE OURO QUE RESOLVE O ERRO
                        fileName = tempFile.name,
                        fileUri = Uri.fromFile(tempFile).toString(),
                        developer = "Gorjeta Plus",
                        coverFrontUrl = "",
                        lastIndexedAt = System.currentTimeMillis()
                    )

                    // Em vez de uma "mala" falsa, pegamos a configuração exata (controles e núcleo) do console selecionado!
                    val mockConfig = gameSystem.systemCoreConfigs.firstOrNull() ?: SystemCoreConfig(
                        coreID = CoreID.values().first(), 
                        controllerConfigs = HashMap<Int, ArrayList<ControllerConfig>>(),
                        exposedSettings = listOf(),
                        exposedAdvancedSettings = listOf()
                    )
                    
                    intent.putExtra("GAME", mockGame)
                    intent.putExtra("EXTRA_SYSTEM_CORE_CONFIG", mockConfig)
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                (context as FragmentActivity).runOnUiThread {
                    Toast.makeText(context, "FALHA: ${e.message}", Toast.LENGTH_LONG).show()
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
