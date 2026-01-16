package com.harsh.myapplication.data.speech

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import androidx.core.content.ContextCompat
import com.harsh.myapplication.data.local.getApplicationContext
import java.util.Locale
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer

actual class SpeechRecognizer actual constructor() {

    private val context = getApplicationContext()

    // Keep a single instance to avoid "Client Busy" errors
    private var recognizer: AndroidSpeechRecognizer? = null
    private var isListening = false

    private fun ensureRecognizer() {
        if (recognizer == null) {
            recognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    actual fun startListening(onResult: (SpeechRecognizerState) -> Unit) {
        if (!checkPermission()) {
            onResult(SpeechRecognizerState.Error("Microphone permission not granted"))
            return
        }

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                if (isListening) return@post // Don't start if already running

                ensureRecognizer()

                // Reset listener for this new session
                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onResult(SpeechRecognizerState.Listening)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        //  println("🎤 SPEECH DEBUG: Error Code = $error")

                        // Ignore trivial errors
                        if (error == AndroidSpeechRecognizer.ERROR_NO_MATCH) return
                        if (error == AndroidSpeechRecognizer.ERROR_CLIENT) return // Ignore busy errors

                        val msg = when (error) {
                            AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                            AndroidSpeechRecognizer.ERROR_NETWORK -> "Network error"
                            else -> "Error code: $error"
                        }
                        onResult(SpeechRecognizerState.Error(msg))
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches =
                            results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(SpeechRecognizerState.Result(matches[0]))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                recognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                onResult(SpeechRecognizerState.Error("Start failed: ${e.message}"))
            }
        }
    }

    actual fun stopListening() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                recognizer?.stopListening()
                isListening = false
            } catch (e: Exception) {
                // Ignore stop errors
            }
        }
    }

    actual fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}