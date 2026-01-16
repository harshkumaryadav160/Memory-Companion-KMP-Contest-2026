package com.harsh.myapplication.data.speech

actual class SpeechRecognizer actual constructor() {
    actual fun startListening(onResult: (SpeechRecognizerState) -> Unit) {
        onResult(SpeechRecognizerState.Error("Voice input is Android-only"))
    }

    actual fun stopListening() {}
    actual fun checkPermission(): Boolean = false
}