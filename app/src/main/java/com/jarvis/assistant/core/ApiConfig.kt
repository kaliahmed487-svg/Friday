package com.jarvis.assistant.core

/**
 * ⚠️ EDIT THIS BEFORE BUILDING ⚠️
 *
 * Paste your free Gemini API key here. Get one at https://aistudio.google.com/apikey
 * (sign in with any Google account, no credit card required for the free tier).
 *
 * This is the only file that needs a personal secret in it. Leaving it as
 * the placeholder is safe — the app will simply skip online mode and use
 * the offline model for everything.
 */
object ApiConfig {
    const val GEMINI_API_KEY = "AQ.Ab8RN6LA_eKRbTOkXafxii-74iWBsNNOU5XdtxkIZnSq909JRA"

    fun isConfigured(): Boolean =
        GEMINI_API_KEY.isNotBlank() && GEMINI_API_KEY != "PASTE_YOUR_GEMINI_API_KEY_HERE"
}
