package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.R

/**
 * Reproduz o efeito sonoro clássico de caixa registradora antiga com moedas
 * na inicialização do aplicativo.
 */
object StartupSoundPlayer {

  private const val TAG = "StartupSoundPlayer"

  fun playCashRegisterCoins(context: Context) {
    try {
      val appContext = context.applicationContext
      val player = MediaPlayer()
      val afd = appContext.resources.openRawResourceFd(R.raw.sfx_cash_register_coins) ?: return
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
      afd.close()

      player.setOnPreparedListener { mp ->
        try {
          mp.start()
        } catch (e: Exception) {
          Log.w(TAG, "Falha ao iniciar áudio de inicialização", e)
          mp.release()
        }
      }
      player.setOnCompletionListener { mp ->
        mp.release()
      }
      player.setOnErrorListener { mp, _, _ ->
        mp.release()
        true
      }
      player.prepareAsync()
    } catch (e: Exception) {
      Log.w(TAG, "Não foi possível reproduzir o som de moedas na inicialização", e)
    }
  }
}
