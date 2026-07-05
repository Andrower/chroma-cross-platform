# Chroma Cross Electron Client

This folder is the Windows desktop shell source for building a portable `.exe`.

It keeps the current web control system, but wraps it in Electron so display lock can also use:

- borderless fullscreen/kiosk mode
- always-on-top projection window
- blocked `Alt+F4`, `Esc`, `F11`, `Ctrl+W`, `Ctrl+R` and similar shortcuts while locked
- unlock hotkey: `Ctrl + Alt + Shift + L`

Build on Windows:

```bat
cd electron-client
npm install
npm run build:win
```

The generated portable exe will be in `dist`.

System-level Windows shortcuts such as `Ctrl + Alt + Delete`, the power key, and Task Manager force-ending the process cannot be fully blocked by a normal app.
