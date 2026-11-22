# YESMIN AI (Android, Kotlin + Compose + Gemini)

## Termux build
pkg update && pkg install openjdk-17 gradle aapt2 unzip wget
# Android SDK chahi (cmdline-tools): platforms;android-34 + build-tools;34.0.0
export ANDROID_HOME=$HOME/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle assembleDebug        # Gradle 8.7-8.9 use koro (AGP 8.5.2 er jonno)
# APK: app/build/outputs/apk/debug/app-debug.apk

## Tips
- aapt2 path gradle.properties e set ache (Termux). Na chale: `which aapt2` diye path milao.
- Prothom run: Settings > AI BRAIN SETTINGS e Gemini key dao > Test connection > model select.
- Voice commands: "open youtube", "play <song> on youtube", "battery", "time", "flashlight on/off",
  "volume up/down", "wifi settings", "pause music".

## Not yet implemented (future)
AccessibilityService touch automation, wake-word, weather/calendar/notes, image/doc upload,
syntax highlighting, persistent history/projects, camera recognition, widgets, smart home.
