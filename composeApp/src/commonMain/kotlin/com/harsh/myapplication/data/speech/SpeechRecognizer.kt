package com.harsh.myapplication.data.speech

sealed class SpeechRecognizerState {
    object Idle : SpeechRecognizerState()
    object Listening : SpeechRecognizerState()
    data class Result(val text: String) : SpeechRecognizerState()
    data class Error(val message: String) : SpeechRecognizerState()
}

expect class SpeechRecognizer() {
    fun startListening(onResult: (SpeechRecognizerState) -> Unit)
    fun stopListening()
    fun checkPermission(): Boolean
}