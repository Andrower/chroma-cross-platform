# Chroma Native

Android project skeleton for a single APK with two modes:

- Control mode: discovers and manages receiver devices on the LAN.
- Receiver mode: auto-discovers the control device and connects to it.

This workspace does not include a Gradle wrapper, so the project is meant to be opened in Android Studio and synced there.

Notes:

- Discovery uses LAN multicast, so control and receiver need to be on the same Wi-Fi/LAN.
- Each receiver gets its own device entry on the control screen.
- Changing the selected device updates only that screen.
- The receiver stays on a pure-color full-screen canvas and waits for the control side to push state.
