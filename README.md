# Friday — Premium Edition (Android / Kotlin)

Same voice-only, pitch-black concept, but:
- **Single premium orb**, perfectly still at idle (no breathing/rotation) — motion only ever means something
- **Deep violet / warm gold** palette instead of primary cyan/green
- **Custom adaptive app icon** matching the in-app orb
- **Hybrid online/offline brain**: uses Google's Gemini API when the phone has internet and a key is configured, silently falls back to the fully offline on-device model (MediaPipe/Gemma) otherwise — switches automatically per request, no user action needed

## The one thing you must edit before building

Open `app/src/main/java/com/jarvis/assistant/core/ApiConfig.kt` and paste your
Gemini API key:

```kotlin
const val GEMINI_API_KEY = "PASTE_YOUR_GEMINI_API_KEY_HERE"
```

Get a free key at **https://aistudio.google.com/apikey** — sign in with any
Google account, click "Create API key". No credit card needed for the free
tier (generous daily request quota).

**If you leave the placeholder as-is**, the app still works perfectly — it
just always uses the offline model, since `ApiConfig.isConfigured()` returns
false and the online path is skipped entirely.

## Everything else is identical to the practical build

Same setup steps: bundle the Vosk model + Gemma `.task` file into
`app/src/main/assets/` before building (see the previous build's README for
exact download links) — the offline model is still there as the fallback,
so it's still required even with a Gemini key configured.

## How the switching actually works

Every time Jarvis needs to answer a general (non-file-command) question,
`JarvisService.generateReply()`:
1. Checks `ApiConfig.isConfigured()` (is a real key pasted in) AND
   `NetworkMonitor.isOnline()` (is there an active, validated internet connection)
2. If both true → calls Gemini. If that call fails for any reason (timeout,
   quota exceeded, bad key) → falls through to the offline model automatically
3. If either is false → goes straight to the offline model

No settings screen, no toggle — it's decided fresh on every single request,
so a phone that loses signal mid-use just keeps working offline without
missing a beat.

## Files new/changed vs. the practical build

```
core/ApiConfig.kt              ← edit this with your key
core/NetworkMonitor.kt         ← connectivity check
ai/GeminiApiEngine.kt          ← Gemini REST API client (no extra dependency)
ui/AudioVisualizerView.kt      ← premium palette + static idle state
res/drawable/ic_launcher_*.xml ← new adaptive app icon
res/mipmap-anydpi-v26/*        ← adaptive icon wiring
AndroidManifest.xml            ← added INTERNET + ACCESS_NETWORK_STATE permissions
```
