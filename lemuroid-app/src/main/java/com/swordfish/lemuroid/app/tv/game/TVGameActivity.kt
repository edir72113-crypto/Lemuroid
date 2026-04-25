package com.swordfish.lemuroid.app.tv.game

import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.tv.gamemenu.TVGameMenuActivity
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.common.coroutines.safeCollect
import com.swordfish.lemuroid.common.displayToast
import kotlinx.coroutines.flow.filter

// Importações do Socket.io e JSON
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class TVGameActivity : BaseGameActivity() {
    override fun getDialogClass() = TVGameMenuActivity::class.java

    // 1. Declarar o nosso "Ouvido Biônico" (Socket)
    private var mSocket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFlows()
        
        // 2. Ligar o motor do Socket assim que a tela do jogo abrir
        conectarSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 3. Desligar o Socket quando fechar o jogo para economizar RAM
        mSocket?.disconnect()
        mSocket?.off("comando_controle")
    }

    @Composable
    override fun GameScreen(viewModel: BaseGameScreenViewModel) {
        TVGameScreen(viewModel)
    }

    private fun initializeFlows() {
        launchOnState(Lifecycle.State.CREATED) {
            initializeShortcutToastFlow()
        }
    }

    private suspend fun initializeShortcutToastFlow() {
        inputDeviceManager
            .getEnabledInputsObservable()
            .filter { it.isEmpty() }
            .safeCollect {
                // Comentei propositalmente o displayToast original abaixo.
                // displayToast(R.string.tv_game_message_missing_gamepad)
                // Motivo: A TV vai reclamar que não tem controle Bluetooth, 
                // mas ela não sabe que o controle dela é o celular via Socket!
            }
    }

    // ==========================================
    // A MÁGICA DO CONTROLE VIA SOCKET.IO
    // ==========================================
    private fun conectarSocket() {
        try {
            // Conecta na sua VPS! (Mantenha a URL raiz do seu servidor)
            mSocket = IO.socket("https://gorjetaplus.online") 
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }

        // Fica escutando as ordens do celular
        mSocket?.on("comando_controle") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val botao = data.optString("botao")
                val acao = data.optString("acao") // "keydown" (apertou) ou "keyup" (soltou)

                // Converte a palavra "up" para o código de botão interno do Android
                val androidKeyCode = mapearBotaoParaAndroid(botao)
                
                if (androidKeyCode != -1) {
                    val keyAction = if (acao == "keydown") KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
                    
                    // Dispara o botão falso na TV (O Lemuroid acha que é um controle Bluetooth)
                    runOnUiThread {
                        val event = KeyEvent(keyAction, androidKeyCode)
                        dispatchKeyEvent(event)
                    }
                }
            }
        }
        
        mSocket?.connect()
    }

    // Tradutor de Joystick (Celular -> Android Nativo)
    private fun mapearBotaoParaAndroid(botao: String): Int {
        return when (botao.lowercase()) {
            "up" -> KeyEvent.KEYCODE_DPAD_UP
            "down" -> KeyEvent.KEYCODE_DPAD_DOWN
            "left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            
            // O Lemuroid usa o layout oficial da Nintendo
            "a" -> KeyEvent.KEYCODE_BUTTON_A 
            "b" -> KeyEvent.KEYCODE_BUTTON_B
            "x" -> KeyEvent.KEYCODE_BUTTON_X
            "y" -> KeyEvent.KEYCODE_BUTTON_Y
            
            "start" -> KeyEvent.KEYCODE_BUTTON_START
            "select" -> KeyEvent.KEYCODE_BUTTON_SELECT
            "l" -> KeyEvent.KEYCODE_BUTTON_L1
            "r" -> KeyEvent.KEYCODE_BUTTON_R1
            else -> -1
        }
    }
}
